package com.example.tabler;

import android.net.Uri;

/**
 * Model for a sheet music / tablature item (in-memory for now).
 */
public class SpartitoItem {
    private final String id;
    private final String titolo;
    private final Uri imageUri;
    private final String musicXmlPath; // optional, null if OMR not done yet
    private final long dataTimestamp;

    public SpartitoItem(String id, String titolo, Uri imageUri, String musicXmlPath, long dataTimestamp) {
        this.id = id;
        this.titolo = titolo;
        this.imageUri = imageUri;
        this.musicXmlPath = musicXmlPath;
        this.dataTimestamp = dataTimestamp;
    }

    public String getId() { return id; }
    public String getTitolo() { return titolo; }
    public Uri getImageUri() { return imageUri; }
    public String getMusicXmlPath() { return musicXmlPath; }
    public long getDataTimestamp() { return dataTimestamp; }

    /** "Solo immagine" if no MusicXML, "OMR completato" otherwise. */
    public String getStato() {
        return musicXmlPath != null && !musicXmlPath.isEmpty() ? "OMR completato" : "Solo immagine";
    }
}
