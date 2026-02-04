# Backend OMR: Python Flask + Java + Audiveris + gunicorn
# Per PaaS (Render, Fly.io, ecc.)

FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

# Dipendenze di sistema: Java (per Audiveris), Python, wget, X11 libs (richieste da Audiveris)
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jre-headless \
    wget \
    ca-certificates \
    python3 \
    python3-pip \
    python3-venv \
    libx11-6 \
    libxext6 \
    libxrender1 \
    libxi6 \
    libxtst6 \
    libxcb1 \
    libxdmcp6 \
    libxau6 \
    xdg-utils \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Download Audiveris .deb e estrazione senza script post-install (evita xdg-desktop-menu in container)
ARG AUDIVERIS_VERSION=5.9.0
ARG AUDIVERIS_DEB=Audiveris-${AUDIVERIS_VERSION}-ubuntu22.04-x86_64.deb
RUN wget -q "https://github.com/Audiveris/audiveris/releases/download/${AUDIVERIS_VERSION}/${AUDIVERIS_DEB}" -O /tmp/audiveris.deb \
    && dpkg -x /tmp/audiveris.deb /tmp/audiveris-extract \
    && cp -r /tmp/audiveris-extract/* / \
    && rm -rf /tmp/audiveris.deb /tmp/audiveris-extract

ENV AUDIVERIS_CMD=audiveris

WORKDIR /app

COPY requirements.txt omr_server.py ./

RUN pip3 install --no-cache-dir -r requirements.txt gunicorn

EXPOSE 8080

# Timeout alto perché Audiveris può impiegare tempo
CMD ["gunicorn", "-w", "1", "-b", "0.0.0.0:8080", "--timeout", "120", "omr_server:app"]
