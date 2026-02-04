package com.example.tabler;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tabler.databinding.ItemSpartitoBinding;

import java.util.ArrayList;
import java.util.List;

public class SpartitoAdapter extends RecyclerView.Adapter<SpartitoAdapter.ViewHolder> {

    private final List<SpartitoItem> items = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSpartitoBinding binding = ItemSpartitoBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SpartitoItem item = items.get(position);
        holder.titolo.setText(item.getTitolo());
        holder.stato.setText(item.getStato());
        Uri uri = item.getImageUri();
        if (uri != null) {
            holder.thumbnail.setImageURI(uri);
        } else {
            holder.thumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<SpartitoItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void addItem(SpartitoItem item) {
        items.add(0, item);
        notifyItemInserted(0);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView thumbnail;
        final TextView titolo;
        final TextView stato;

        ViewHolder(ItemSpartitoBinding binding) {
            super(binding.getRoot());
            thumbnail = binding.itemThumbnail;
            titolo = binding.itemTitolo;
            stato = binding.itemStato;
        }
    }
}
