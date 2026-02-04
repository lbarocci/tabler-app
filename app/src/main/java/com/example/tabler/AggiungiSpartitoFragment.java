package com.example.tabler;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.tabler.databinding.FragmentAggiungiSpartitoBinding;

import java.io.File;
import java.io.IOException;

public class AggiungiSpartitoFragment extends Fragment {

    private FragmentAggiungiSpartitoBinding binding;
    private Uri cameraPhotoUri;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchCameraInternal();
                }
            });

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
                if (result && cameraPhotoUri != null) {
                    navigateToAnteprima(cameraPhotoUri.toString());
                }
            });

    private final ActivityResultLauncher<Intent> pickFileLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    navigateToAnteprima(result.getData().getData().toString());
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAggiungiSpartitoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.btnScattaFoto.setOnClickListener(v -> launchCamera());
        binding.btnScegliGalleria.setOnClickListener(v -> launchGallery());
    }

    private void launchCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }
        launchCameraInternal();
    }

    private void launchCameraInternal() {
        try {
            File photoFile = File.createTempFile("spartito_", ".jpg", requireContext().getCacheDir());
            cameraPhotoUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile
            );
            takePictureLauncher.launch(cameraPhotoUri);
        } catch (IOException e) {
            cameraPhotoUri = null;
        }
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"});
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pickFileLauncher.launch(Intent.createChooser(intent, getString(R.string.scegli_immagine_o_pdf)));
    }

    private void navigateToAnteprima(String imageUriString) {
        Bundle args = new Bundle();
        args.putString("imageUri", imageUriString);
        NavController nav = Navigation.findNavController(requireView());
        nav.navigate(R.id.anteprimaImmagineFragment, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
