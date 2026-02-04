"""
OMR Backend: accepts image upload and runs Audiveris (if available) to produce MusicXML.
Run: python omr_server.py
Then from the app use http://10.0.2.2:8080 (emulator) or http://<your-pc-ip>:8080 (device).
"""
import json
import os
import subprocess
import tempfile
from pathlib import Path

from flask import Flask, request, jsonify

app = Flask(__name__)

# Set to Audiveris executable path if not in PATH, or None to use "audiveris"
AUDIVERIS_CMD = os.environ.get("AUDIVERIS_CMD", "audiveris")


def run_audiveris(image_path: str, output_dir: str) -> str | None:
    """Run Audiveris CLI on image; return path to generated MusicXML file or None."""
    try:
        subprocess.run(
            [
                AUDIVERIS_CMD,
                "-batch",
                "-transcribe",
                "-export",
                "-output", output_dir,
                image_path,
            ],
            check=True,
            capture_output=True,
            timeout=120,
        )
    except (subprocess.CalledProcessError, FileNotFoundError, subprocess.TimeoutExpired) as e:
        print("Audiveris error:", e)
        return None

    # Audiveris writes .mxl or .xml in output_dir (same base name as input)
    base = Path(image_path).stem
    for ext in (".mxl", ".xml"):
        candidate = Path(output_dir) / (base + ext)
        if candidate.exists():
            return str(candidate)
    return None


@app.route("/omr", methods=["POST"])
def omr():
    if "image" not in request.files:
        return jsonify({"error": "Missing 'image' file"}), 400
    file = request.files["image"]
    if file.filename == "":
        return jsonify({"error": "No file selected"}), 400

    with tempfile.TemporaryDirectory() as tmp:
        image_path = os.path.join(tmp, "input.png")
        file.save(image_path)
        output_dir = tmp
        music_xml_path = run_audiveris(image_path, output_dir)

        if music_xml_path and os.path.exists(music_xml_path):
            with open(music_xml_path, encoding="utf-8", errors="replace") as f:
                music_xml = f.read()
            return jsonify({"musicXml": music_xml})
        else:
            # Audiveris not available or failed: return placeholder so app can show "result"
            placeholder = '<?xml version="1.0"?>\n<!-- OMR non disponibile: installa Audiveris e avvia il server con AUDIVERIS_CMD impostato -->\n<placeholder/>'
            return jsonify({"musicXml": placeholder, "note": "Audiveris non eseguito (non in PATH o errore). Installa Audiveris e imposta AUDIVERIS_CMD."})


@app.route("/health")
def health():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080, debug=True)
