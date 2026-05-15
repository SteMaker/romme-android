# Rommé – Spielregeln (implementiert)

Dieses Dokument beschreibt alle Spielregeln so, wie sie im Code des Servers (`rommeGame.js`, `meldValidator.js`, `deck.js`) und der Android-App implementiert sind, einschließlich aller Sonderfälle.

---

## 1. Karten & Deck

| Eigenschaft | Wert |
|---|---|
| Decks | 2 × 52 Karten |
| Joker | 6 (3 pro Deck) |
| Gesamt | **110 Karten** |
| Farben | Kreuz ♣, Pik ♠, Herz ♥, Karo ♦ |
| Ränge | 2, 3, 4, 5, 6, 7, 8, 9, 10, Bube, Dame, König, Ass |

### Kartenwerte (Punkte)

| Karte | Punkte |
|---|---|
| 2–9 | Nennwert (2–9) |
| 10, Bube, Dame, König | 10 |
| Ass | 11 |
| Joker | 20 |

> **Sonderfall Ass in Umbruch-Folgen:** In einer Folge, die das Ass zusammen mit niedrigen Karten (2–10) *ohne* König enthält (z. B. A-2-3), zählt das Ass bei der Meldungswertberechnung (Erstauslage) nur **1 Punkt** statt 11. Bei der Endabrechnung (Handkarten) zählt das Ass jedoch immer 11 Punkte.

---

## 2. Spielvorbereitung

- **Spieleranzahl:** 2–6 Spieler.
- Jeder Spieler erhält **13 Karten**.
- Die erste Karte des Restdecks wird offen auf den **Ablagestapel** gelegt.
- Alle übrigen Karten bilden den verdeckten **Nachziehstapel**.
- Der erste Spieler im Raum (Host) beginnt.

---

## 3. Ablauf eines Spielzugs

Jeder Zug besteht aus drei Phasen in dieser Reihenfolge:

### Phase 1 – Ziehen (`draw`)
Der Spieler **muss** genau eine Karte ziehen:
- vom **Nachziehstapel** (verdeckt), oder
- von der **Oberseite des Ablagestapels** (offen).

### Phase 2 – Spielen (`play`)
Der Spieler **kann** beliebig viele der folgenden Aktionen durchführen (in beliebiger Reihenfolge und Anzahl):
- Eine neue **Meldung auslegen** (Satz oder Folge).
- Eine Karte an eine **bestehende Meldung anlegen**.
- Einen **Joker ersetzen**.

Alle drei Aktionen sind optional. Die Phase endet mit dem Ablegen.

### Phase 3 – Ablegen
Der Spieler **muss** genau eine Karte auf den Ablagestapel legen, um den Zug zu beenden.

> **Ausnahme:** Hat der Spieler nach dem Auslegen oder Anlegen keine Karten mehr auf der Hand, endet die Runde sofort – kein Ablegen notwendig.

---

## 4. Meldungen (Auslagen)

### 4.1 Satz (`satz`)

- Genau **3 oder 4 Karten**.
- Alle Karten haben **denselben Rang**.
- Alle Karten haben **verschiedene Farben**.
- Joker dürfen die fehlende(n) Farbe(n) ersetzen.

**Gültige Beispiele:**
- ♣8 ♠8 ♥8
- ♣K ♠K ♥K ♦K
- ♣7 ♠7 JOKER (Joker steht für ♥7 oder ♦7)

**Ungültig:**
- ♣8 ♠8 ♠8 (zwei Pik – gleiche Farbe)
- ♣8 ♠8 (nur 2 Karten)
- ♣8 ♠8 ♥8 ♦8 JOKER (5 Karten – mehr als 4)

### 4.2 Folge (`folge`)

- Mindestens **3 Karten**.
- Alle Karten **gleiche Farbe**.
- **Aufeinanderfolgende Ränge** (keine Lücken außer durch Joker).
- Joker füllen genau eine Lücke.
- **Keine doppelten Ränge** innerhalb einer Folge.
- Keine explizite Maximalgrenze – theoretisch bis 13+ Karten möglich.

**Umbruch-Folgen (Wrap-Around):**
Das Ass kann sowohl hoch (nach König) als auch niedrig (vor 2) stehen:
- `K-A-2` ist gültig (Ass zwischen König und 2).
- `A-2-3` ist gültig (Ass vor 2).
- `Q-K-A` ist gültig (Ass nach König).

**Gültige Beispiele:**
- ♥5 ♥6 ♥7
- ♥5 JOKER ♥7 (Joker = ♥6)
- JOKER ♥6 ♥7 (Joker = ♥5 als Randjoker)
- ♥5 ♥6 JOKER (Joker = ♥7 als Randjoker)

**Ungültig:**
- ♥5 ♥6 ♠7 (verschiedene Farben)
- ♥5 JOKER ♥8 (Joker müsste zwei Lücken füllen: ♥6 und ♥7)
- ♥5 ♥6 ♥6 (doppelter Rang)

### 4.3 Reihenfolge beim Auslegen einer Folge

Beim Auslegen einer Folge prüft der Server die **vom Spieler gewählte Kartenreihenfolge**:
- Normale Karten müssen in streng aufsteigender Reihenfolge liegen.
- Ein Joker muss **exakt** an der Lückenstelle zwischen zwei normalen Karten platziert sein.
- Randjoker (Anfang oder Ende) sind erlaubt.

**Beispiel:**
- `[♥3, JOKER, ♥5]` → gültig (Joker an der Lücke zwischen 3 und 5).
- `[JOKER, ♥3, ♥5]` → **ungültig** (Joker steht links von 3, aber die Lücke liegt zwischen 3 und 5).
- `[JOKER, ♥4, ♥5]` → gültig (Joker als Randjoker für ♥3).

---

## 5. Erstauslage

- Jeder Spieler muss eine **Erstauslage** machen, bevor er Karten an bestehende Meldungen anlegen oder Joker ersetzen kann.
- Die erste Meldung eines Spielers muss mindestens **40 Punkte** wert sein.
- Die Wertberechnung erfolgt nach den Kartenwerten (inkl. 20 Punkte pro Joker, Ass-Sonderregel für Umbruch-Folgen beachten).
- Nach erfolgreicher Erstauslage gilt die 40-Punkte-Grenze nicht mehr – weitere Meldungen im selben Zug haben keine Mindestpunktzahl.

---

## 6. Karte anlegen (`appendToMeld`)

- Nur möglich **nach der eigenen Erstauslage**.
- Karten können an **beliebige Meldungen** auf dem Tisch angelegt werden, also auch an Meldungen anderer Spieler.
- Die anliegende Karte muss die Meldung weiterhin gültig halten.
- **Joker als Randkarte anlegen:** Eine Joker-Handkarte darf an eine Meldung angelegt werden, wenn die resultierende Meldung gültig bleibt (Joker wird als Randjoker positioniert).

### Redundanter Joker

Wenn eine angelegte Karte die Position übernimmt, die zuvor ein Joker in der Meldung einnahm (d. h. der Joker ist danach überflüssig, weil die Meldung ohne ihn gültig ist), wird der Joker **automatisch aus der Meldung entfernt und dem Spieler auf die Hand gegeben**.

**Beispiel:**
- Meldung auf dem Tisch: `[♥5, JOKER, ♥7]` (Joker = ♥6)
- Spieler legt ♥6 an.
- Ergebnis: Meldung wird `[♥5, ♥6, ♥7]`, der **Joker geht auf die Hand** des Spielers.

---

## 7. Joker ersetzen (`replaceJoker`)

- Nur möglich **nach der eigenen Erstauslage**.
- Der Spieler tippt auf einen Joker in einer beliebigen Meldung.
- Der Server sucht automatisch die **erste gültige Karte** in der Handdes Spielers (in Handkartenreihenfolge), die den Joker ersetzen kann.
- Die gefundene Karte wird in die Meldung an die Stelle des Jokers eingesetzt.
- Der Joker geht auf die **Hand des Spielers**.
- Hat der Spieler keine passende Karte, schlägt die Aktion fehl.

> **Hinweis:** Der Spieler kann **nicht selbst wählen**, welche Handkarte verwendet wird – der Server wählt die erste passende.

---

## 8. Gewinnen

Die Runde endet sofort, wenn ein Spieler **keine Handkarten mehr** hat. Das kann passieren durch:

1. Auslegen einer Meldung mit den letzten Karten.
2. Anlegen der letzten Karte an eine bestehende Meldung.
3. Ablegen der letzten Karte auf den Ablagestapel.

> Durch Joker-Ersetzen allein kann man nicht gewinnen (Karte geht raus, Joker kommt rein – Netto: keine Änderung der Handkartenanzahl).

---

## 9. Punkte­berechnung

| Spieler | Punkte |
|---|---|
| Gewinner | 0 |
| Verlierer | Negativer Summenwert der verbliebenen Handkarten |

Die Werte der Handkarten werden mit den Standardwerten berechnet (Ass = 11, unabhängig vom Spielkontext).

---

## 10. Lobby & Räume

- **Minimum** für Spielstart: 2 Spieler.
- **Maximum** pro Raum: 6 Spieler.
- Nur der **Raumersteller (Host)** kann das Spiel starten.
- Verlässt der Host den Raum vor Spielstart, wird der nächste Spieler automatisch Host.
- **Nach Spielende** wird der Raum automatisch gelöscht und aus der Raumliste entfernt.
- Leere Räume werden automatisch gelöscht.
- Trennt sich ein Spieler während des Spiels, wird er aus dem Raum entfernt; das Spielobjekt auf dem Server bleibt jedoch bestehen (laufende Verbindung zu anderen Spielern).

---

## 11. Sonderfälle & Implementierungs­details

### Ziehen vom Ablagestapel in der PLAY-Phase
Das Ziehen vom Ablagestapel ist nicht nur in der DRAW-Phase erlaubt, sondern auch noch in der **PLAY-Phase**. Das bedeutet, ein Spieler könnte theoretisch in einem Zug zuerst vom Nachziehstapel ziehen (→ PLAY-Phase), und danach noch einmal vom Ablagestapel ziehen. Das Spiel erzwingt keine Einschränkung auf eine Karte pro Zug.

### Phase DISCARD existiert im Code, wird aber nie gesetzt
Der Code definiert drei Phasen: `draw`, `play`, `discard`. Die `discard`-Phase wird jedoch durch keine Spielaktion gesetzt – `_nextTurn()` setzt immer auf `draw`, und Ziehen setzt immer auf `play`. Das Ablegen ist aus der `play`-Phase heraus möglich (nicht erst aus `discard`). Die `discard`-Phase ist toter Code.

### Zwei identische Karten (2 Decks)
Da zwei vollständige Decks verwendet werden, gibt es von jeder Karte **zwei Exemplare** (unterschieden durch eine interne Deck-ID). Beide Kopien können gleichzeitig auf dem Tisch liegen – aber **nicht in derselben Meldung** (Satz: gleiche Farbe verboten; Folge: gleicher Rang verboten).

### Keine Mehrrundenunterstützung
Das Feld `round` existiert im Code (initialisiert mit `1`), wird aber nie erhöht. Die aktuelle Implementierung unterstützt nur **eine Runde** pro Spiel.

### Joker-Reihenfolge beim Auslegen
Die Kartenreihenfolge einer Meldung wird beim Auslegen **so gespeichert, wie der Spieler sie gewählt hat** (kein automatisches Sortieren beim `layDownMeld`). Der Server validiert lediglich, dass die gewählte Reihenfolge regelkonform ist.

### Automatisches Sortieren bei Anlegen und Joker-Ersetzen
Bei `appendToMeld` und `replaceJoker` wird die Meldung nach der Aktion **automatisch sortiert** (Folge: aufsteigend nach Rang; Satz: aufsteigend nach Farbe). Joker werden dabei in die korrekten Lückenpositionen eingesetzt.

### Satz-Maximalgröße: 4 Karten
Ein Satz ist auf **maximal 4 Karten** begrenzt (eine pro Farbe). Mit Joker also maximal 4 Karten gesamt – man kann keinen Joker an einen vollständigen 4er-Satz anlegen.

### Nachziehstapel leer
Wenn der Nachziehstapel leer ist, wird der Ablagestapel (bis auf die oberste Karte) **neu gemischt** und als neuer Nachziehstapel verwendet. Ist auch der Ablagestapel zu klein (≤ 1 Karte), kann keine Karte gezogen werden, und der Zug schlägt fehl.

### Joker in der Erstauslage
Ein Joker zählt **20 Punkte** für die Erstaulagen-Bewertung (40-Punkte-Minimum). Eine Meldung aus ♥5, JOKER, ♥7 hat z. B. einen Wert von 5 + 20 + 7 = 32 Punkte (< 40 → nicht ausreichend als Erstauslage).

### Reihenfolge der Meldungen auf dem Tisch
Meldungen werden mit einer fortlaufenden ID (0, 1, 2, …) nummeriert. Sie werden nie entfernt oder umsortiert. Alle Spieler sehen alle Meldungen aller Spieler.

### Sichtbarkeit der Handkarten
Jeder Spieler sieht nur die eigenen Handkarten vollständig. Von Mitspielern ist nur die **Anzahl der Handkarten** sichtbar sowie ob sie bereits ihre Erstauslage gemacht haben.
