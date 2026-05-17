# Rommé – Spielregeln / Game Rules

---

## Deutsch

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

---

## English

# Rommé – Game Rules (as implemented)

This document describes all game rules as implemented in the server code (`rommeGame.js`, `meldValidator.js`, `deck.js`) and the Android app, including all edge cases.

---

## 1. Cards & Deck

| Property | Value |
|---|---|
| Decks | 2 × 52 cards |
| Jokers | 6 (3 per deck) |
| Total | **110 cards** |
| Suits | Clubs ♣, Spades ♠, Hearts ♥, Diamonds ♦ |
| Ranks | 2, 3, 4, 5, 6, 7, 8, 9, 10, Jack, Queen, King, Ace |

### Card Values (Points)

| Card | Points |
|---|---|
| 2–9 | Face value (2–9) |
| 10, Jack, Queen, King | 10 |
| Ace | 11 |
| Joker | 20 |

> **Ace in wrap-around sequences:** In a sequence that includes the Ace together with low cards (2–10) *without* a King (e.g. A-2-3), the Ace counts only **1 point** when evaluating the initial lay-down (40-point minimum). At end-of-round scoring (hand cards), the Ace always counts 11 points.

---

## 2. Setup

- **Number of players:** 2–6.
- Each player receives **13 cards**.
- The first card of the remaining deck is placed face-up on the **discard pile**.
- All remaining cards form the face-down **draw pile**.
- The first player in the room (the host) goes first.

---

## 3. Turn Structure

Each turn consists of three phases in this order:

### Phase 1 – Draw (`draw`)
The player **must** draw exactly one card:
- from the **draw pile** (face-down), or
- from the **top of the discard pile** (face-up).

### Phase 2 – Play (`play`)
The player **may** perform any number of the following actions (in any order and any quantity):
- Lay down a new **meld** (set or sequence).
- **Append** a card to an existing meld.
- **Replace** a joker.

All three actions are optional. The phase ends with the discard.

### Phase 3 – Discard
The player **must** place exactly one card on the discard pile to end their turn.

> **Exception:** If the player has no cards left in hand after laying down or appending, the round ends immediately — no discard is required.

---

## 4. Melds

### 4.1 Set (`satz`)

- Exactly **3 or 4 cards**.
- All cards have the **same rank**.
- All cards have **different suits**.
- Jokers may substitute for a missing suit.

**Valid examples:**
- ♣8 ♠8 ♥8
- ♣K ♠K ♥K ♦K
- ♣7 ♠7 JOKER (Joker stands for ♥7 or ♦7)

**Invalid:**
- ♣8 ♠8 ♠8 (two spades – same suit)
- ♣8 ♠8 (only 2 cards)
- ♣8 ♠8 ♥8 ♦8 JOKER (5 cards – more than 4)

### 4.2 Sequence (`folge`)

- At least **3 cards**.
- All cards the **same suit**.
- **Consecutive ranks** (no gaps except those filled by a joker).
- A joker fills exactly one gap.
- **No duplicate ranks** within a sequence.
- No explicit maximum — theoretically up to 13+ cards possible.

**Wrap-around sequences:**
The Ace can be both high (after King) and low (before 2):
- `K-A-2` is valid (Ace between King and 2).
- `A-2-3` is valid (Ace before 2).
- `Q-K-A` is valid (Ace after King).

**Valid examples:**
- ♥5 ♥6 ♥7
- ♥5 JOKER ♥7 (Joker = ♥6)
- JOKER ♥6 ♥7 (Joker = ♥5 as edge joker)
- ♥5 ♥6 JOKER (Joker = ♥7 as edge joker)

**Invalid:**
- ♥5 ♥6 ♠7 (different suits)
- ♥5 JOKER ♥8 (Joker would have to fill two gaps: ♥6 and ♥7)
- ♥5 ♥6 ♥6 (duplicate rank)

### 4.3 Card Order When Laying Down a Sequence

When laying down a sequence, the server validates the **card order chosen by the player**:
- Normal cards must be in strictly ascending order.
- A joker must be placed **exactly** at the gap position between two normal cards.
- Edge jokers (at the start or end) are allowed.

**Examples:**
- `[♥3, JOKER, ♥5]` → valid (Joker at the gap between 3 and 5).
- `[JOKER, ♥3, ♥5]` → **invalid** (Joker is left of 3, but the gap is between 3 and 5).
- `[JOKER, ♥4, ♥5]` → valid (Joker as edge joker for ♥3).

---

## 5. Initial Lay-Down

- Each player must complete an **initial lay-down** before they can append cards to existing melds or replace jokers.
- The first meld a player lays down must be worth at least **40 points**.
- Points are calculated using card values (including 20 points per joker; observe the Ace wrap-around rule).
- Once the initial lay-down is complete, the 40-point threshold no longer applies — further melds in the same turn have no minimum point requirement.

---

## 6. Appending to a Meld (`appendToMeld`)

- Only possible **after the player's own initial lay-down**.
- Cards may be appended to **any meld** on the table, including melds laid down by other players.
- The appended card must keep the meld valid.
- **Joker as edge card:** A joker from hand may be appended to a meld if the resulting meld remains valid (the joker is positioned as an edge joker).

### Redundant Joker

If an appended card takes the position previously held by a joker in the meld (i.e. the joker is now redundant because the meld is valid without it), the joker is **automatically removed from the meld and added to the player's hand**.

**Example:**
- Meld on the table: `[♥5, JOKER, ♥7]` (Joker = ♥6)
- Player appends ♥6.
- Result: meld becomes `[♥5, ♥6, ♥7]`, the **joker goes to the player's hand**.

---

## 7. Replacing a Joker (`replaceJoker`)

- Only possible **after the player's own initial lay-down**.
- The player taps a joker in any meld.
- The server automatically finds the **first valid card** in the player's hand (in hand order) that can replace the joker.
- That card is placed in the meld at the joker's position.
- The joker goes to the **player's hand**.
- If no matching card exists, the action fails.

> **Note:** The player **cannot choose** which hand card is used — the server picks the first valid one.

---

## 8. Winning

The round ends immediately when a player has **no cards left in hand**. This can happen by:

1. Laying down a meld with the last cards.
2. Appending the last card to an existing meld.
3. Discarding the last card onto the discard pile.

> Replacing a joker alone cannot win the round (one card leaves the hand, one joker enters — net change: zero).

---

## 9. Scoring

| Player | Points |
|---|---|
| Winner | 0 |
| Losers | Negative sum of remaining hand card values |

Hand card values use standard values (Ace = 11, regardless of game context).

---

## 10. Lobby & Rooms

- **Minimum** to start a game: 2 players.
- **Maximum** per room: 6 players.
- Only the **room creator (host)** can start the game.
- If the host leaves the room before the game starts, the next player automatically becomes host.
- **After the game ends**, the room is automatically deleted and removed from the room list.
- Empty rooms are automatically deleted.
- If a player disconnects during the game, they are removed from the room; the game object on the server remains (the connection to other players continues).

---

## 11. Edge Cases & Implementation Details

### Drawing from the Discard Pile in the PLAY Phase
Drawing from the discard pile is not restricted to the DRAW phase — it is also possible during the **PLAY phase**. This means a player could theoretically draw from the draw pile first (→ PLAY phase), and then draw again from the discard pile in the same turn. The game does not enforce a one-card-per-turn limit.

### DISCARD Phase Exists in Code but Is Never Set
The code defines three phases: `draw`, `play`, `discard`. However, no game action ever sets the `discard` phase — `_nextTurn()` always sets `draw`, and drawing always sets `play`. Discarding is possible from the `play` phase (not from `discard`). The `discard` phase is dead code.

### Two Identical Cards (2 Decks)
Since two full decks are used, there are **two copies** of every card (distinguished by an internal deck ID). Both copies can be on the table at the same time — but **not in the same meld** (set: same suit forbidden; sequence: same rank forbidden).

### No Multi-Round Support
The `round` field exists in the code (initialised to `1`) but is never incremented. The current implementation supports only **one round** per game.

### Joker Order When Laying Down
The card order of a meld is **stored exactly as chosen by the player** when laying down (no automatic sorting during `layDownMeld`). The server only validates that the chosen order is rule-compliant.

### Automatic Sorting When Appending and Replacing Jokers
After `appendToMeld` and `replaceJoker`, the meld is **automatically sorted** (sequence: ascending by rank; set: ascending by suit). Jokers are placed in the correct gap positions.

### Maximum Set Size: 4 Cards
A set is limited to **4 cards** (one per suit). With a joker, the maximum is still 4 cards total — a joker cannot be appended to a complete 4-card set.

### Empty Draw Pile
When the draw pile is empty, the discard pile (except for the top card) is **reshuffled** and becomes the new draw pile. If the discard pile is also too small (≤ 1 card), no card can be drawn and the action fails.

### Joker in the Initial Lay-Down
A joker counts **20 points** toward the initial lay-down evaluation (40-point minimum). For example, a meld of ♥5, JOKER, ♥7 has a value of 5 + 20 + 7 = 32 points (< 40 → insufficient for the initial lay-down).

### Meld Order on the Table
Melds are numbered with sequential IDs (0, 1, 2, …). They are never removed or reordered. All players see all melds from all players.

### Hand Card Visibility
Each player sees only their own hand cards in full. From other players, only the **number of hand cards** is visible, plus whether they have already completed their initial lay-down.
