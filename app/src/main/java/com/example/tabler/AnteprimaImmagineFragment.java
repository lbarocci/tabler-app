package com.example.tabler;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.tabler.databinding.FragmentAnteprimaImmagineBinding;

public class AnteprimaImmagineFragment extends Fragment {

    private FragmentAnteprimaImmagineBinding binding;
    private String imageUriString;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imageUriString = getArguments().getString("imageUri");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAnteprimaImmagineBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (imageUriString != null) {
            binding.anteprimaImage.setImageURI(Uri.parse(imageUriString));
        }
        binding.btnRiconosciOmr.setOnClickListener(v -> launchOmr());
    }

    private void launchOmr() {
        if (imageUriString == null) return;
        Bundle args = new Bundle();
        args.putString("imageUri", imageUriString);
        NavController nav = Navigation.findNavController(requireView());
        nav.navigate(R.id.risultatoOmrFragment, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
