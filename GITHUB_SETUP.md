# Come mettere il progetto su GitHub e collaborare con Cursor

Questa guida ti spiega come caricare l’app Android su GitHub e lavorarci insieme al tuo amico usando Cursor.

---

## 1. Installa Git (se non l’hai già)

- Scarica Git per Windows: https://git-scm.com/download/win  
- Installa e **riavvia Cursor** (o il terminale) dopo l’installazione.  
- Verifica: apri un nuovo terminale in Cursor e scrivi `git --version`. Se vedi un numero di versione, Git è installato.

---

## 2. Crea un account GitHub (se non ce l’hai)

- Vai su https://github.com e crea un account (gratuito).

---

## 3. Crea il repository su GitHub

1. Accedi a GitHub e clicca **“+”** in alto a destra → **“New repository”**.
2. **Repository name:** scegli un nome (es. `tabler-app` o `nuovo-test-app`).
3. **Description:** opzionale (es. “App Android – progetto condiviso”).
4. Lascia **Public**.
5. **Non** spuntare “Add a README” (il progetto ce l’hai già in locale).
6. Clicca **“Create repository”**.

Dopo la creazione GitHub ti mostrerà una pagina con i comandi; tienila aperta.

---

## 4. Inizializza Git nel progetto e carica su GitHub

Apri il **terminale in Cursor** (Terminal → New Terminal) e vai nella cartella del progetto, poi esegui i comandi **uno alla volta**:

```powershell
cd "z:\Nuovo test app"
```

```powershell
git init
```

```powershell
git add .
```

```powershell
git commit -m "Primo commit: app Android iniziale"
```

Ora collega il progetto al repository GitHub (sostituisci `TUO-USERNAME` e `NOME-REPO` con i tuoi):

```powershell
git remote add origin https://github.com/TUO-USERNAME/NOME-REPO.git
```

```powershell
git branch -M main
```

```powershell
git push -u origin main
```

Ti verrà chiesto di accedere a GitHub (browser o token). Se usi HTTPS e non hai ancora le credenziali, GitHub può chiederti un **Personal Access Token** invece della password: lo crei da GitHub → Settings → Developer settings → Personal access tokens.

Dopo il primo `git push` il codice sarà su GitHub.

---

## 5. Invitare il tuo amico a collaborare

1. Su GitHub apri il repository.
2. Vai in **Settings** → **Collaborators** (o **Manage access**).
3. Clicca **“Add people”** e inserisci l’username o email di GitHub del tuo amico.
4. Lui accetta l’invito dalla mail o dalla notifica su GitHub.

Da quel momento può **clonare** il repo e lavorarci con Cursor.

---

## 6. Come lavora il tuo amico con Cursor

Il tuo amico deve:

1. Avere **Git** e **Cursor** installati.
2. Clonare il repository:
   - In Cursor: **File → Clone Repository** (oppure dal terminale: `git clone https://github.com/TUO-USERNAME/NOME-REPO.git`).
3. Aprire la cartella clonata in Cursor e sviluppare come di solito.
4. Per inviare modifiche: **commit** + **push**.  
   Per prendere le tue modifiche: **pull** (o “Sync” in Cursor).

---

## Comandi utili per la collaborazione

| Azione | Comando |
|--------|--------|
| Scaricare le ultime modifiche (es. del tuo amico) | `git pull` |
| Inviare i tuoi commit su GitHub | `git push` |
| Vedere lo stato dei file | `git status` |
| Aggiungere modifiche e fare commit | `git add .` poi `git commit -m "Descrizione"` |

---

## Riepilogo veloce

1. Installa Git e riavvia Cursor.  
2. Crea un nuovo repository su GitHub (senza README).  
3. Nel terminale: `cd "z:\Nuovo test app"` → `git init` → `git add .` → `git commit -m "Primo commit"`.  
4. `git remote add origin https://github.com/TUO-USERNAME/NOME-REPO.git`  
5. `git branch -M main` → `git push -u origin main`.  
6. Aggiungi il tuo amico come collaboratore nelle impostazioni del repo.  
7. Lui clona il repo con Cursor e lavora con `git pull` / `git push`.

Se un comando dà errore, copia il messaggio e cercalo su Google o chiedi qui: spesso riguarda autenticazione (token/SSH) o percorso della cartella.
