# Open Romme – Android App

---

## Deutsch

Open Romme ist eine quelloffene Android-App für das klassische Kartenspiel Rommé, gespielt über das lokale Netzwerk oder das Internet in Echtzeit mit 2–6 Spielern.

### Voraussetzungen

Die App ist ein reiner Client. Für den Betrieb ist ein laufender **romme-server** erforderlich, den du selbst hosten musst:

- **romme-server:** [https://github.com/StefanMacher/romme-server](https://github.com/StefanMacher/romme-server)
- Der Server stellt die Spiellogik, die Raumverwaltung und die Echtzeit-Kommunikation über Socket.IO bereit.

Ohne einen eigenen Server kann die App keine Verbindung herstellen.

### Anmeldung

Die Authentifizierung erfolgt über einen **Nextcloud-Account** (Benutzername + App-Passwort). Du benötigst daher zusätzlich eine Nextcloud-Instanz, die mit dem romme-server verbunden ist.

### Einrichtung

Beim ersten Start gibst du folgende Verbindungsdaten ein:

| Feld | Beschreibung |
|---|---|
| Server-URL | URL des romme-servers (z. B. `https://meinserver.de`) |
| Nextcloud-URL | URL deiner Nextcloud-Instanz |
| Nextcloud-Benutzername | Dein Nextcloud-Benutzername |
| App-Passwort | Ein in Nextcloud generiertes App-Passwort |
| Socket-Pfad | Pfad des Socket.IO-Endpunkts (Standard: `/romme/socket.io`) |

Die Einstellungen werden verschlüsselt auf dem Gerät gespeichert und beim nächsten Start automatisch wiederverwendet.

### Spielablauf

1. Einloggen → Lobby
2. Raum erstellen oder einem bestehenden Raum beitreten (2–6 Spieler)
3. Host startet das Spiel
4. Rommé spielen – Karten ziehen, Meldungen auslegen, Karten anlegen, Joker ersetzen, ablegen

Die vollständigen Spielregeln und alle implementierten Sonderfälle sind in [RULES.md](RULES.md) beschrieben.

### Datenschutzerklärung

Open Romme erhebt, speichert und überträgt **keine personenbezogenen Daten** an Dritte oder den App-Entwickler.

- Die App stellt ausschließlich eine Verbindung zu dem Server her, dessen URL du selbst eingibst.
- Dein Nextcloud-Benutzername und dein App-Passwort werden ausschließlich zur Authentifizierung gegenüber diesem Server verwendet und verschlüsselt lokal auf deinem Gerät gespeichert (Android EncryptedSharedPreferences).
- Es werden keine Nutzungsstatistiken, Absturzberichte, Werbe-IDs oder sonstige Tracking-Daten erfasst.
- Es werden keine Daten an Werbetreibende oder Analysedienste weitergegeben.
- Der Entwickler hat keinen Zugriff auf deine Verbindungsdaten oder Spielinhalte.

Für Fragen zum Datenschutz: **maker.stefan@googlemail.com**

---

## English

Open Romme is an open-source Android app for the classic card game Rummy, played in real time over a local network or the internet with 2–6 players.

### Requirements

The app is a client only. A running **romme-server** that you host yourself is required:

- **romme-server:** [https://github.com/StefanMacher/romme-server](https://github.com/StefanMacher/romme-server)
- The server provides game logic, room management, and real-time communication via Socket.IO.

Without a self-hosted server, the app cannot connect to anything.

### Authentication

Authentication is handled via a **Nextcloud account** (username + app password). You therefore also need a Nextcloud instance connected to the romme-server.

### Setup

On first launch, enter the following connection details:

| Field | Description |
|---|---|
| Server URL | URL of the romme-server (e.g. `https://myserver.com`) |
| Nextcloud URL | URL of your Nextcloud instance |
| Nextcloud username | Your Nextcloud username |
| App password | An app password generated in Nextcloud |
| Socket path | Path of the Socket.IO endpoint (default: `/romme/socket.io`) |

Settings are stored encrypted on the device and reused automatically on subsequent launches.

### Gameplay

1. Log in → Lobby
2. Create a room or join an existing one (2–6 players)
3. Host starts the game
4. Play Rummy – draw cards, lay down melds, append to melds, replace jokers, discard

The full rules and all implemented edge cases are described in [RULES.md](RULES.md).

### Privacy Policy

Open Romme does **not collect, store, or transmit any personal data** to third parties or the app developer.

- The app connects exclusively to the server whose URL you enter yourself.
- Your Nextcloud username and app password are used solely to authenticate with that server and are stored encrypted locally on your device (Android EncryptedSharedPreferences).
- No usage statistics, crash reports, advertising IDs, or any other tracking data are collected.
- No data is shared with advertisers or analytics services.
- The developer has no access to your connection details or game content.

For privacy-related questions: **maker.stefan@googlemail.com**
