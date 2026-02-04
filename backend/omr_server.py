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
from werkzeug.exceptions import RequestEntityTooLarge

app = Flask(__name__)

# Limite upload 4 MB per evitare di superare la RAM del server (es. Render free tier)
MAX_UPLOAD_BYTES = 4 * 1024 * 1024
app.config["MAX_CONTENT_LENGTH"] = MAX_UPLOAD_BYTES

# Set to Audiveris executable path if not in PATH, or None to use "audiveris"
AUDIVERIS_CMD = os.environ.get("AUDIVERIS_CMD", "audiveris")
# Heap JVM per Audiveris (override da env se serve, altrimenti limite sicuro per 512MB RAM)
JAVA_OPTS = os.environ.get("JAVA_TOOL_OPTIONS", "-Xmx256m -XX:+UseSerialGC")


def run_audiveris(image_path: str, output_dir: str):
    # Returns (path to MusicXML or None, error note for client)
    """Run Audiveris CLI on image; return (path to MusicXML or None, error note for client)."""
    env = {**os.environ, "JAVA_TOOL_OPTIONS": JAVA_OPTS}
    try:
        result = subprocess.run(
            [
                AUDIVERIS_CMD,
                "-batch",
                "-transcribe",
                "-export",
                "-output", output_dir,
                image_path,
            ],
            env=env,
            check=True,
            capture_output=True,
            timeout=120,
            text=True,
        )
    except FileNotFoundError:
        return None, "Audiveris non trovato (comando non in PATH)."
    except subprocess.TimeoutExpired:
        return None, "Audiveris in timeout dopo 120 secondi."
    except subprocess.CalledProcessError as e:
        err = (e.stderr or e.stdout or str(e))[:500]
        return None, f"Audiveris errore: {err}"
    except Exception as e:
        return None, f"Errore: {e!s}"

    # Audiveris writes .mxl or .xml in output_dir (same base name as input)
    base = Path(image_path).stem
    for ext in (".mxl", ".xml"):
        candidate = Path(output_dir) / (base + ext)
        if candidate.exists():
            return str(candidate), ""
    return None, "Audiveris non ha prodotto file MusicXML."


@app.route("/omr", methods=["POST"])
def omr():
    if "image" not in request.files:
        return jsonify({"error": "Missing 'image' file"}), 400
    file = request.files["image"]
    if file.filename == "":
        return jsonify({"error": "No file selected"}), 400

    with tempfile.TemporaryDirectory() as tmp:
        # Preserve extension (PDF, JPG, PNG) so Audiveris gets the right format
        ext = os.path.splitext(file.filename or "")[1] or ".png"
        if ext.lower() not in (".pdf", ".jpg", ".jpeg", ".png"):
            ext = ".png"
        image_path = os.path.join(tmp, "input" + ext)
        file.save(image_path)
        output_dir = tmp
        music_xml_path, error_note = run_audiveris(image_path, output_dir)

        if music_xml_path and os.path.exists(music_xml_path):
            with open(music_xml_path, encoding="utf-8", errors="replace") as f:
                music_xml = f.read()
            return jsonify({"musicXml": music_xml})
        else:
            placeholder = '<?xml version="1.0"?>\n<!-- OMR non disponibile -->\n<placeholder/>'
            return jsonify({"musicXml": placeholder, "note": error_note or "Il server non ha prodotto un risultato."})


@app.errorhandler(RequestEntityTooLarge)
def handle_too_large(e):
    return jsonify({
        "error": "File troppo grande.",
        "note": f"Dimensioni massime: {MAX_UPLOAD_BYTES // (1024*1024)} MB. Riduci l'immagine o il PDF e riprova.",
    }), 413


@app.route("/health")
def health():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080, debug=True)
