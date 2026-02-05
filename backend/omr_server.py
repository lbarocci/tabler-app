"""
OMR Backend: accepts image upload and runs Audiveris (if available) to produce MusicXML.
Run: python omr_server.py
Then from the app use http://10.0.2.2:8080 (emulator) or http://<your-pc-ip>:8080 (device).
"""
import json
import os
import re
import subprocess
import tempfile
import zipfile
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
JAVA_OPTS = os.environ.get("JAVA_TOOL_OPTIONS", "-Xmx192m -XX:+UseSerialGC -XX:MaxMetaspaceSize=64m")


def _extract_xml_from_mxl(mxl_path: str) -> str | None:
    """Estrae il contenuto XML da un file .mxl (ZIP). Supporta container.xml, nomi .xml/.musicxml, o qualsiasi file con contenuto XML."""
    with zipfile.ZipFile(mxl_path, "r") as z:
        # Standard MXL: META-INF/container.xml indica il rootfile (es. .musicxml)
        container_path = "META-INF/container.xml"
        if container_path in z.namelist():
            data = z.read(container_path).decode("utf-8", errors="replace")
            match = re.search(r'rootfile\s+[^>]*full-path=["\']([^"\']+)["\']', data)
            if match:
                root_path = match.group(1).strip()
                if root_path in z.namelist():
                    return z.read(root_path).decode("utf-8", errors="replace")
        # Fallback 1: primo file che termina con .xml o .musicxml (escluso container)
        for name in z.namelist():
            if name.endswith("/"):
                continue
            n = name.lower()
            if n.endswith(".musicxml") or (n.endswith(".xml") and "container" not in n):
                return z.read(name).decode("utf-8", errors="replace")
        # Fallback 2: qualsiasi file il cui contenuto sembri MusicXML (per output Audiveris atipici)
        for name in z.namelist():
            if name.endswith("/"):
                continue
            try:
                raw = z.read(name)
                text = raw.decode("utf-8", errors="replace").strip()
                if not text or len(text) < 50:
                    continue
                head = text[:500].lower()
                if (text.startswith("<?xml") or text.startswith("<score") or "<score " in head or
                        "<score>" in head or "musicxml" in head or "<part-list" in head):
                    return text
            except Exception:
                continue
    return None


def _find_music_xml(output_dir: str, base: str) -> str | None:
    """Cerca .mxl o .xml in output_dir e nelle sottocartelle (Audiveris crea book folder)."""
    out = Path(output_dir)
    # Prima: stesso livello (input.mxl)
    for ext in (".mxl", ".xml"):
        candidate = out / (base + ext)
        if candidate.exists():
            return str(candidate)
    # Poi: ricorsivo (es. output_dir/input/input.mxl per PDF/libri)
    for ext in (".mxl", ".xml"):
        for path in out.rglob("*" + ext):
            if path.is_file():
                return str(path)
    return None


def run_audiveris(image_path: str, output_dir: str):
    # Returns (path to MusicXML or None, error note for client)
    """Run Audiveris CLI on image; return (path to MusicXML or None, error note for client)."""
    env = {**os.environ, "JAVA_TOOL_OPTIONS": JAVA_OPTS}
    stderr_out = ""
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
        stderr_out = (result.stderr or "").strip()
    except FileNotFoundError:
        return None, "Audiveris non trovato (comando non in PATH)."
    except subprocess.TimeoutExpired:
        return None, "Audiveris in timeout dopo 120 secondi."
    except subprocess.CalledProcessError as e:
        err = (e.stderr or e.stdout or str(e))[:500]
        return None, f"Audiveris errore: {err}"
    except Exception as e:
        return None, f"Errore: {e!s}"

    base = Path(image_path).stem
    music_xml_path = _find_music_xml(output_dir, base)
    if music_xml_path:
        return music_xml_path, ""
    # Nessun file: messaggio utile, eventualmente con ultime righe di stderr
    note = "Audiveris non ha prodotto file MusicXML."
    if stderr_out:
        last_lines = "\n".join(stderr_out.splitlines()[-3:]).strip()[:300]
        if last_lines:
            note += " Dettaglio: " + last_lines
    return None, note


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
            if music_xml_path.lower().endswith(".mxl"):
                music_xml = _extract_xml_from_mxl(music_xml_path)
                if not music_xml:
                    error_note = (error_note or "Estrazione XML da .mxl fallita.") + " "
                    try:
                        with zipfile.ZipFile(music_xml_path, "r") as z:
                            error_note += "[Contenuto .mxl: " + ", ".join(z.namelist()[:15]) + "]"
                    except Exception:
                        error_note += "[Impossibile aprire .mxl]"
            else:
                with open(music_xml_path, encoding="utf-8", errors="replace") as f:
                    music_xml = f.read()
            if music_xml:
                return jsonify({"musicXml": music_xml})
        # Diagnostica: elenco file creati da Audiveris
        try:
            files_in_out = [str(p.relative_to(tmp)) for p in Path(tmp).rglob("*") if p.is_file()]
            if files_in_out:
                error_note = (error_note or "") + " [File in output: " + ", ".join(files_in_out[:20]) + "]"
        except Exception:
            pass
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
