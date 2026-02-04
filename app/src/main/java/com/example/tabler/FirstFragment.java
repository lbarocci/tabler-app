package com.example.tabler;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.tabler.databinding.FragmentFirstBinding;

import java.util.ArrayList;
import java.util.List;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private SpartitoAdapter adapter;
    private final List<SpartitoItem> items = new ArrayList<>();

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new SpartitoAdapter();
        adapter.setItems(items);
        binding.recyclerSpartiti.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerSpartiti.setAdapter(adapter);
        updateEmptyState(); // show "Nessuno spartito" when list is empty
    }

    void updateEmptyState() {
        boolean empty = items.isEmpty();
        binding.homeEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recyclerSpartiti.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    /** Call when a new item is added (e.g. from Anteprima/OMR flow) to refresh the list. */
    public void addSpartitoItem(SpartitoItem item) {
        items.add(0, item);
        if (adapter != null) {
            adapter.addItem(item);
        }
        updateEmptyState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        adapter = null;
    }
}
