package com.example.animalcatch;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.animalcatch.api.ApiClient;
import com.example.animalcatch.api.ApiService;
import com.example.animalcatch.api.IdentifyResponse;
import com.example.animalcatch.api.StatsResponse;
import com.example.animalcatch.db.AnimalDao;
import com.example.animalcatch.db.AnimalEntity;
import com.example.animalcatch.db.AppDatabase;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1002;

    private PreviewView viewFinder;
    private ImageCapture imageCapture;
    private MaterialButton btnCapture;

    private ApiService apiService;
    private AnimalDao animalDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewFinder = findViewById(R.id.viewFinder);
        btnCapture = findViewById(R.id.btn_capture);
        MaterialButton btnInventory = findViewById(R.id.btn_inventory);
        MaterialButton btnBattle    = findViewById(R.id.btn_battle);

        apiService = ApiClient.getApiService();
        animalDao  = AppDatabase.getInstance(this).animalDao();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        }

        btnCapture.setOnClickListener(v -> takePhoto());

        btnInventory.setOnClickListener(v ->
                startActivity(new Intent(this, InventoryActivity.class)));

        btnBattle.setOnClickListener(v ->
                startActivity(new Intent(this, BattleActivity.class)));
    }

    // ─── Camera ──────────────────────────────────────────────────────────────

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try { bindPreview(future.get()); }
            catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        imageCapture   = new ImageCapture.Builder().build();
        preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                    this,
                    new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build(),
                    preview, imageCapture);
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    // ─── Capture + identify ───────────────────────────────────────────────────

    private void takePhoto() {
        if (imageCapture == null) return;

        File photoFile = new File(
                getExternalFilesDir(null),
                new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                        .format(System.currentTimeMillis()) + ".jpg");

        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(photoFile).build(),
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults r) {
                        setBusy(true);
                        identifyAnimal(photoFile);
                    }
                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        Toast.makeText(MainActivity.this,
                                "Capture failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void identifyAnimal(File photoFile) {
        RequestBody body = RequestBody.create(photoFile, MediaType.parse("image/jpeg"));
        MultipartBody.Part part =
                MultipartBody.Part.createFormData("image", photoFile.getName(), body);

        apiService.identifyAnimal(part).enqueue(new Callback<IdentifyResponse>() {
            @Override
            public void onResponse(@NonNull Call<IdentifyResponse> call,
                                   @NonNull Response<IdentifyResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    fetchStats(response.body().getName(), photoFile.getAbsolutePath());
                } else {
                    setBusy(false);
                    Toast.makeText(MainActivity.this,
                            "Couldn't identify the animal. Try again!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<IdentifyResponse> call, @NonNull Throwable t) {
                setBusy(false);
                Toast.makeText(MainActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchStats(String animalName, String photoPath) {
        apiService.getStats(animalName).enqueue(new Callback<StatsResponse>() {
            @Override
            public void onResponse(@NonNull Call<StatsResponse> call,
                                   @NonNull Response<StatsResponse> response) {
                setBusy(false);
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    StatsResponse stats = response.body();
                    saveToDatabase(stats, photoPath);
                    showStatsDialog(stats, true);
                } else {
                    Toast.makeText(MainActivity.this,
                            "Couldn't load stats for " + animalName, Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<StatsResponse> call, @NonNull Throwable t) {
                setBusy(false);
                Toast.makeText(MainActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─── Room ────────────────────────────────────────────────────────────────

    private void saveToDatabase(StatsResponse stats, String photoPath) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AnimalEntity existing = animalDao.getByName(stats.getName().toLowerCase());
            if (existing == null) {
                animalDao.insert(new AnimalEntity(
                        stats.getName().toLowerCase(),
                        stats.getHp(), stats.getAtk(), stats.getDef(), stats.getSpd(),
                        1, photoPath,
                        0,  // xp
                        1   // level
                ));
            } else {
                animalDao.incrementCountAndUpdatePhoto(
                        stats.getName().toLowerCase(), photoPath);
            }
        });
    }

    // ─── Dialog ──────────────────────────────────────────────────────────────

    private void showStatsDialog(StatsResponse stats, boolean isNewCatch) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_animal_stats);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        String displayName = stats.getName().substring(0, 1).toUpperCase()
                + stats.getName().substring(1);

        ((TextView) dialog.findViewById(R.id.tv_stat_name)).setText(displayName);
        ((TextView) dialog.findViewById(R.id.tv_stat_hp)).setText("HP: "  + stats.getHp());
        ((TextView) dialog.findViewById(R.id.tv_stat_atk)).setText("ATK: " + stats.getAtk());
        ((TextView) dialog.findViewById(R.id.tv_stat_def)).setText("DEF: " + stats.getDef());
        ((TextView) dialog.findViewById(R.id.tv_stat_spd)).setText("SPD: " + stats.getSpd());

        TextView tvCatch = dialog.findViewById(R.id.tv_catch_label);
        if (tvCatch != null) {
            tvCatch.setVisibility(isNewCatch ? View.VISIBLE : View.GONE);
            tvCatch.setText("🎉 Added to inventory!");
        }

        ((MaterialButton) dialog.findViewById(R.id.btn_close_stats))
                .setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void setBusy(boolean busy) {
        btnCapture.setEnabled(!busy);
        btnCapture.setAlpha(busy ? 0.5f : 1f);
        btnCapture.setText(busy ? "Identifying…" : "");
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && allPermissionsGranted())
            startCamera();
        else
            Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show();
    }
}