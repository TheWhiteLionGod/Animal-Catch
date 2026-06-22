package com.example.animalcatch;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.animalcatch.db.AnimalEntity;

import java.io.File;

public class AnimalAdapter extends ListAdapter<AnimalEntity, AnimalAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(AnimalEntity animal);
    }

    private final OnItemClickListener listener;

    public AnimalAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<AnimalEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AnimalEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull AnimalEntity a, @NonNull AnimalEntity b) {
                    return a.getName().equals(b.getName());
                }

                @Override
                public boolean areContentsTheSame(@NonNull AnimalEntity a, @NonNull AnimalEntity b) {
                    return a.getHp()    == b.getHp()
                            && a.getAtk()   == b.getAtk()
                            && a.getDef()   == b.getDef()
                            && a.getSpd()   == b.getSpd()
                            && a.getCount() == b.getCount();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_animal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AnimalEntity animal = getItem(position);

        // Capitalise first letter
        String displayName = animal.getName().substring(0, 1).toUpperCase()
                + animal.getName().substring(1);
        holder.tvName.setText(displayName);
        holder.tvCount.setText("×" + animal.getCount());

        // Load captured photo from disk, fall back to placeholder
        if (animal.getPhotoPath() != null && !animal.getPhotoPath().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(new File(animal.getPhotoPath()))
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(holder.imgAnimal);
        } else {
            holder.imgAnimal.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(animal));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAnimal;
        TextView tvName;
        TextView tvCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAnimal = itemView.findViewById(R.id.img_animal);
            tvName    = itemView.findViewById(R.id.tv_animal_name);
            tvCount   = itemView.findViewById(R.id.tv_animal_summary);
        }
    }
}