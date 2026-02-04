# Backend OMR (Audiveris)

Server Flask che riceve un'immagine dall'app e la processa con Audiveris (se installato) per ottenere MusicXML.

## Avvio

1. Installa le dipendenze: `pip install -r requirements.txt`
2. (Opzionale) Installa [Audiveris](https://github.com/Audiveris/audiveris) e assicurati che l'eseguibile `audiveris` sia nel PATH, oppure imposta `AUDIVERIS_CMD` con il percorso completo.
3. Avvia il server: `python omr_server.py`
4. L'app usa `http://10.0.2.2:8080` dall'emulatore; su dispositivo reale configura l'URL con l'IP del PC (es. `http://192.168.1.x:8080`).

## Endpoint

- `POST /omr`: body `multipart/form-data` con campo `image` (file immagine). Risposta JSON `{ "musicXml": "..." }` o `{ "error": "..." }`.
- `GET /health`: verifica che il server sia attivo.

## Senza Audiveris

Se Audiveris non è installato o non è nel PATH, il server risponde comunque con un messaggio placeholder così l'app può mostrare la schermata risultato.

---

## Deploy su Render (PaaS con Docker)

Per far funzionare l'OMR per tutti (anche da app pubblicata sul Play Store), puoi hostare il backend su [Render](https://render.com) usando Docker.

### 1. Prepara il repo

Assicurati che nel repository GitHub ci siano:

- `backend/Dockerfile` (Ubuntu, Java, Audiveris, Python, gunicorn)
- `backend/omr_server.py` e `backend/requirements.txt`
- `render.yaml` nella **root** del repo (descrive il servizio Docker per Render)

### 2. Crea il servizio su Render

1. Vai su [render.com](https://render.com), registrati e collegati con GitHub.
2. **New** → **Blueprint** (oppure **New** → **Web Service**).
3. Se usi Blueprint: collega il repo e seleziona il file `render.yaml`. Render creerà il servizio "tabler-omr" in automatico.
4. Se crei un Web Service a mano: scegli **Docker**, imposta **Root Directory** su `backend`, **Port** su `8080`, e avvia il deploy.

### 3. URL del backend

Dopo il primo deploy, Render assegna un URL tipo:

`https://tabler-omr.onrender.com`

(Il nome può variare se hai scelto un altro nome per il servizio.)

### 4. Configura l'app Android

Nell'app Android, sostituisci l'URL base del backend con l'URL pubblico di Render. In `app/src/main/java/com/example/tabler/RisultatoOmrFragment.java` la costante `OMR_BASE_URL` è attualmente `http://10.0.2.2:8080`. Per la versione release (o per test con dispositivo/emulatore verso il cloud), imposta:

`OMR_BASE_URL = "https://tabler-omr.onrender.com"`

(sostituisci con l'URL reale mostrato da Render).

**Nota**: Sul piano free di Render l'istanza va in sleep dopo inattività; la prima richiesta dopo un po' di tempo può richiedere 30–60 secondi (cold start).
