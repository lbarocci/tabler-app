# Backend OMR: Python Flask + Java + Audiveris + gunicorn
# Per PaaS (Render, Fly.io, ecc.)

FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

# Dipendenze di sistema: Java (per Audiveris), Python, wget
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jre-headless \
    wget \
    ca-certificates \
    python3 \
    python3-pip \
    python3-venv \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Download e installazione Audiveris (.deb per Ubuntu 22.04)
# URL da https://github.com/Audiveris/audiveris/releases
ARG AUDIVERIS_VERSION=5.9.0
ARG AUDIVERIS_DEB=Audiveris-${AUDIVERIS_VERSION}-ubuntu22.04-x86_64.deb
RUN wget -q "https://github.com/Audiveris/audiveris/releases/download/${AUDIVERIS_VERSION}/${AUDIVERIS_DEB}" -O /tmp/audiveris.deb \
    && apt-get update \
    && apt-get install -y --no-install-recommends /tmp/audiveris.deb || apt-get install -f -y \
    && rm /tmp/audiveris.deb \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# L'eseguibile Audiveris è in PATH dopo l'installazione del .deb
ENV AUDIVERIS_CMD=audiveris

WORKDIR /app

COPY requirements.txt omr_server.py ./

RUN pip3 install --no-cache-dir -r requirements.txt gunicorn

EXPOSE 8080

# Timeout alto perché Audiveris può impiegare tempo
CMD ["gunicorn", "-w", "1", "-b", "0.0.0.0:8080", "--timeout", "120", "omr_server:app"]
