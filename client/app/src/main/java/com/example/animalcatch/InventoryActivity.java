package com.example.animalcatch;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        RecyclerView rvInventory = findViewById(R.id.rv_inventory);
        rvInventory.setLayoutManager(new GridLayoutManager(this, 2));

        MaterialButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        List<Animal> animals = new ArrayList<>();
        // Adding example animals
        animals.add(new Animal("Fox", 100, 20, 15, 25, android.R.drawable.ic_menu_gallery));
        animals.add(new Animal("Wolf", 120, 25, 18, 22, android.R.drawable.ic_menu_gallery));
        animals.add(new Animal("Bear", 200, 30, 25, 10, android.R.drawable.ic_menu_gallery));

        AnimalAdapter adapter = new AnimalAdapter(animals, this::showAnimalStats);
        rvInventory.setAdapter(adapter);
    }

    private void showAnimalStats(Animal animal) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_animal_stats);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvName = dialog.findViewById(R.id.tv_stat_name);
        TextView tvHp = dialog.findViewById(R.id.tv_stat_hp);
        TextView tvAtk = dialog.findViewById(R.id.tv_stat_atk);
        TextView tvDef = dialog.findViewById(R.id.tv_stat_def);
        TextView tvSpd = dialog.findViewById(R.id.tv_stat_spd);
        MaterialButton btnClose = dialog.findViewById(R.id.btn_close_stats);

        tvName.setText(animal.getName());
        tvHp.setText("HP: " + animal.getHp());
        tvAtk.setText("ATK: " + animal.getAtk());
        tvDef.setText("DEF: " + animal.getDef());
        tvSpd.setText("SPD: " + animal.getSpd());

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
