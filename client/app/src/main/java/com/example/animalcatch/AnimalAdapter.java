package com.example.animalcatch;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AnimalAdapter extends RecyclerView.Adapter<AnimalAdapter.ViewHolder> {
    private List<Animal> animalList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Animal animal);
    }

    public AnimalAdapter(List<Animal> animalList, OnItemClickListener listener) {
        this.animalList = animalList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_animal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Animal animal = animalList.get(position);
        holder.tvName.setText(animal.getName());
        holder.imgAnimal.setImageResource(animal.getImageResId());
        holder.itemView.setOnClickListener(v -> listener.onItemClick(animal));
    }

    @Override
    public int getItemCount() {
        return animalList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAnimal;
        TextView tvName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAnimal = itemView.findViewById(R.id.img_animal);
            tvName = itemView.findViewById(R.id.tv_animal_name);
        }
    }
}
