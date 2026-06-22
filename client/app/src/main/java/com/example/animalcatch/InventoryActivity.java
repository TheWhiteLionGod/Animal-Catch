package com.example.animalcatch;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animalcatch.db.AppDatabase;
import com.google.android.material.button.MaterialButton;

public class InventoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        RecyclerView rvInventory = findViewById(R.id.rv_inventory);
        rvInventory.setLayoutManager(new GridLayoutManager(this, 2));

        AnimalAdapter adapter = new AnimalAdapter(animal -> showStatsDialog(animal));
        rvInventory.setAdapter(adapter);

        // Observe Room LiveData — updates automatically when new animals are caught
        AppDatabase.getInstance(this)
                .animalDao()
                .getAllLive()
                .observe(this, animals -> adapter.submitList(animals));

        MaterialButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
    }

    private void showStatsDialog(com.example.animalcatch.db.AnimalEntity animal) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_animal_stats);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvName  = dialog.findViewById(R.id.tv_stat_name);
        TextView tvHp    = dialog.findViewById(R.id.tv_stat_hp);
        TextView tvAtk   = dialog.findViewById(R.id.tv_stat_atk);
        TextView tvDef   = dialog.findViewById(R.id.tv_stat_def);
        TextView tvSpd   = dialog.findViewById(R.id.tv_stat_spd);
        TextView tvCatch = dialog.findViewById(R.id.tv_catch_label);
        MaterialButton btnClose = dialog.findViewById(R.id.btn_close_stats);

        String displayName = animal.getName().substring(0, 1).toUpperCase()
                + animal.getName().substring(1);

        tvName.setText(displayName);
        tvHp.setText("HP: "  + animal.getHp());
        tvAtk.setText("ATK: " + animal.getAtk());
        tvDef.setText("DEF: " + animal.getDef());
        tvSpd.setText("SPD: " + animal.getSpd());

        if (tvCatch != null) {
            tvCatch.setText("Caught × " + animal.getCount());
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}