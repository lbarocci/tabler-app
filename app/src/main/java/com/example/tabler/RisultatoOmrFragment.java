package com.example.tabler;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tabler.databinding.FragmentRisultatoOmrBinding;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RisultatoOmrFragment extends Fragment {

    private FragmentRisultatoOmrBinding binding;
    private String imageUriString;

    /** OMR backend URL (Render). For local testing use http://10.0.2.2:8080 */
    private static final String OMR_BASE_URL = "https://tabler-omr.onrender.com";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imageUriString = getArguments().getString("imageUri");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRisultatoOmrBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.progressOmr.setVisibility(View.VISIBLE);
        binding.scrollRisultato.setVisibility(View.GONE);
        binding.risultatoText.setText("");

        if (imageUriString == null || imageUriString.isEmpty()) {
            showResult(getString(R.string.riconoscimento_fallito) + " (nessuna immagine)");
            return;
        }

        executor.execute(() -> {
            try {
                Uri uri = Uri.parse(imageUriString);
                String mimeType = requireContext().getContentResolver().getType(uri);
                byte[] imageBytes = readUriToBytes(uri);
                if (imageBytes == null || imageBytes.length == 0) {
                    runOnUiThread(() -> showResult(getString(R.string.riconoscimento_fallito) + " (impossibile leggere file)"));
                    return;
                }
                String result = callOmrBackend(imageBytes, mimeType);
                runOnUiThread(() -> showResult(result));
            } catch (Exception e) {
                runOnUiThread(() -> showResult(getString(R.string.riconoscimento_fallito) + "\n\n" + e.getMessage()));
            }
        });
    }

    private byte[] readUriToBytes(Uri uri) throws IOException {
        try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n;
            while ((n = is.read(chunk)) != -1) {
                buffer.write(chunk, 0, n);
            }
            return buffer.toByteArray();
        }
    }

    private String callOmrBackend(byte[] imageBytes, String mimeType) {
        String filename = "application/pdf".equals(mimeType) ? "spartito.pdf" : "spartito.jpg";
        MediaType mediaType = (mimeType != null && !mimeType.isEmpty())
                ? MediaType.parse(mimeType) : MediaType.parse("image/jpeg");
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", filename, RequestBody.create(mediaType, imageBytes))
                .build();
        Request request = new Request.Builder()
                .url(OMR_BASE_URL + "/omr")
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                if (response.code() == 413 && body.contains("\"note\"")) {
                    String note = extractJsonNote(body);
                    if (note != null && !note.isEmpty()) {
                        return getString(R.string.riconoscimento_fallito) + "\n\nFile troppo grande.\n\n" + note;
                    }
                }
                return getString(R.string.riconoscimento_fallito) + " (HTTP " + response.code() + ")\n\n" + body;
            }
            if (response.body() == null) return getString(R.string.riconoscimento_completato);
            String bodyStr = response.body().string();
            String originalResponse = bodyStr;
            if (bodyStr.trim().startsWith("{")) {
                // JSON: e.g. {"musicXml": "..."} or {"error": "..."}
                if (bodyStr.contains("\"musicXml\"")) {
                    int start = bodyStr.indexOf("\"musicXml\"");
                    int valueStart = bodyStr.indexOf(":", start) + 1;
                    int quoteStart = bodyStr.indexOf("\"", valueStart) + 1;
                    int quoteEnd = bodyStr.indexOf("\"", quoteStart);
                    if (quoteEnd > quoteStart) {
                        bodyStr = bodyStr.substring(quoteStart, quoteEnd).replace("\\n", "\n").replace("\\\"", "\"");
                        if (isPlaceholderResponse(bodyStr)) {
                            String backendNote = extractJsonNote(originalResponse);
                            String msg = getString(R.string.riconoscimento_fallito) + "\n\n" + getString(R.string.omr_nessun_risultato);
                            if (backendNote != null && !backendNote.isEmpty()) {
                                msg += "\n\nDettaglio server: " + backendNote;
                            }
                            return msg;
                        }
                    }
                } else if (bodyStr.contains("\"error\"")) {
                    return getString(R.string.riconoscimento_fallito) + "\n\n" + bodyStr;
                }
            }
            return getString(R.string.riconoscimento_completato) + "\n\n" + bodyStr;
        } catch (IOException e) {
            return getString(R.string.riconoscimento_fallito) + "\n\n" + e.getMessage() +
                    "\n\n(Verifica che il backend OMR sia in esecuzione su " + OMR_BASE_URL + ")";
        }
    }

    private void runOnUiThread(Runnable r) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(r);
        }
    }

    /** True if the backend returned the placeholder (no real OMR result). */
    private boolean isPlaceholderResponse(String bodyStr) {
        if (bodyStr == null) return true;
        return bodyStr.contains("OMR non disponibile")
                || bodyStr.contains("<placeholder/>")
                || (bodyStr.length() < 100 && bodyStr.contains("<?xml"));
    }

    /** Extract the "note" field from JSON response, if present (backend explanation when OMR fails). */
    private String extractJsonNote(String json) {
        if (json == null || !json.contains("\"note\"")) return null;
        int start = json.indexOf("\"note\"");
        int valueStart = json.indexOf(":", start) + 1;
        int quoteStart = json.indexOf("\"", valueStart) + 1;
        if (quoteStart <= valueStart) return null;
        int quoteEnd = json.indexOf("\"", quoteStart);
        if (quoteEnd <= quoteStart) return null;
        return json.substring(quoteStart, quoteEnd).replace("\\n", "\n").replace("\\\"", "\"");
    }

    private void showResult(String text) {
        if (binding == null) return;
        binding.progressOmr.setVisibility(View.GONE);
        binding.scrollRisultato.setVisibility(View.VISIBLE);
        binding.risultatoText.setText(text);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
