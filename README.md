# Animal Catch

A mobile Android app that encourages users to go outside, photograph real animals they encounter, and build a collection of creatures to train and battle. Inspired by creature-collecting games, Animal Catch grounds the experience in the real world — every animal in your roster is one you actually found and photographed yourself.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [AI Architecture](#ai-architecture)
- [Responsible AI](#responsible-ai)
- [AI Tools Used](#ai-tools-used)
- [Data Sources](#data-sources)
- [Project Structure](#project-structure)
- [Android Client](#android-client)
- [Backend API](#backend-api)
- [Battle System](#battle-system)
- [XP and Leveling](#xp-and-leveling)
- [Getting Started](#getting-started)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

Animal Catch combines real-world exploration with turn-based RPG mechanics. Players photograph animals they find outside — a dog in the park, a bird on a fence, a squirrel in a tree — and those animals become their fighters. The app fetches live stats for wild opponents from a custom Python backend, scales them to match the player's level, and pulls real photos from Wikipedia so every enemy looks like the actual species.

The core loop is: **go outside → catch → battle → level up → repeat**.

---

## Features

- Catch real animals by photographing them with your device camera
- Turn-based battles with Attack, Defend, and Flee actions
- Rule-based enemy AI that adapts strategy based on remaining HP
- XP and leveling system with stat growth on level-up
- Wild enemy stats scaled dynamically to the player's level
- Enemy sprite images fetched live from the Wikipedia API
- Local animal collection persisted with Room (SQLite)
- Live animal base stats served from a hosted Python backend
- Entrance animations and VS badge for battle start
- HP bars that shift color from green to amber to red as health drops

---

## AI Architecture

### Inputs

| Input | Source | Description |
|---|---|---|
| Animal photo | Device camera | Player photographs a real animal to catch it |
| Animal name | Player text entry | Used to look up base stats from the backend |
| Wild animal name | Randomly selected in-app | One of 12 species picked per battle |
| Player level | Room database | Used to scale enemy stats appropriately |

### AI Capability Used

Animal Catch uses a **rule-based AI system** (not a machine learning model) to drive enemy decision-making during battle. This is an explicit, deterministic decision tree rather than a trained neural network.

The enemy AI observes a single input — its own current HP percentage — and selects an action according to fixed probability thresholds:

```
HP > 60%  ->  Attack 80% of the time, Defend 20%
HP 30-60% ->  Attack 60% of the time, Defend 40%
HP < 30%  ->  Attack 90% of the time, Defend 10% (desperate all-out mode)
```

This gives the enemy a coherent, readable personality: it plays cautiously at mid-health, and throws caution to the wind when it is nearly defeated.

### Processing

1. The Android client randomly selects a wild animal species from a hardcoded pool of 12.
2. It calls the Python backend (`GET /stats/{animal}`) to retrieve base HP, ATK, DEF, and SPD.
3. The `EnemyScaler` class multiplies those base stats by a factor derived from the player's current level, so encounters stay challenging as the player progresses.
4. On each enemy turn, `BattleAI.decideAction()` reads the enemy's `getHpFraction()` value and rolls a random double to select ATTACK or DEFEND according to the probability table above.
5. A Wikipedia REST API call (`/page/summary/{title}`) fetches a real photo of the species to display as the enemy sprite.

### Outputs

| Output | Description |
|---|---|
| Enemy stats | Scaled HP, ATK, DEF, SPD shown in the enemy HUD |
| Enemy sprite | Real Wikipedia photo of the species, loaded via Glide |
| Battle log text | Turn-by-turn narrative of actions and damage |
| XP and level-up | Awarded to the player's animal after the battle ends |
| Stat increases | HP +10, ATK +3, DEF +2, SPD +2 applied on level-up |

---

## Responsible AI

### Risk: Over-reliance on a rule-based AI creating an unfair or frustrating experience

The enemy AI's low-HP behavior (90% attack rate) is intentionally aggressive, which could frustrate newer players if their animal is already weak. To mitigate this, the `EnemyScaler` caps how steeply stats grow relative to the player's level, ensuring a baseline level of fairness regardless of how the AI behaves in a given turn.

Additionally, because the AI logic is fully deterministic and readable (a simple if/else probability tree rather than a black-box model), its behavior can be inspected, tuned, and explained to users. There is no hidden or emergent behavior — if the enemy feels unfair, the exact line of code responsible can be found and adjusted.

### Risk: Misinformation from Wikipedia content

The app fetches enemy sprites and could theoretically fetch misleading or vandalized Wikipedia images. This is mitigated by only consuming the thumbnail field of the Wikipedia page summary endpoint, not any text content. Images are displayed as decorative sprites only — no Wikipedia text is ever shown to the user as factual information within the app.

### Risk: Exclusion of users without access to outdoor spaces

The app's core loop requires going outside to photograph animals. Users in urban environments with limited wildlife, or users with mobility restrictions, may find it harder to catch animals. This is a design-level accessibility consideration that future versions could address by allowing a broader definition of "animal" (pets, zoo visits, etc.) or by providing a small set of starter animals that do not require a photo.

---

## AI Tools Used

| Tool | Purpose | Cost |
|---|---|---|
| Rule-based AI (custom Java) | Enemy battle decision-making | Free — written from scratch |
| Wikipedia REST API | Fetching real animal photos for enemy sprites | Free |
| Claude (Anthropic) | AI coding assistance during development | Free tier / Paid |
| Custom Python backend | Serving animal base stats | Free (hosted on Render free tier) |

No machine learning models, trained classifiers, or LLM inference calls are made at runtime inside the app. All AI behavior in the live app is the rule-based `BattleAI` class.

---

## Data Sources

### Animal Base Stats — Custom Backend (Synthetic Data)

The Python backend at `animal-catch.onrender.com` serves base stat values (HP, ATK, DEF, SPD) for each wild animal species. These stats are **hand-authored / synthetic** — they were designed to feel balanced and thematically appropriate for each species rather than derived from any biological dataset. For example, a shark has high ATK and HP; a cheetah has high SPD; a rhino has high DEF.

### Animal Photos — Wikipedia REST API

Enemy sprite images are fetched at runtime from the Wikipedia page summary endpoint:

```
https://en.wikipedia.org/api/rest_v1/page/summary/{AnimalName}
```

The `thumbnail.source` field of the response is used directly as the image URL. Wikipedia content is freely licensed under Creative Commons. No images are stored server-side or in the app's local database — they are fetched fresh each battle.

### Player Animal Photos — Device Camera (User-Generated)

Photos taken by the player are stored locally on the device via the Android file system. The file path is persisted in Room (SQLite). No photos are uploaded to any server. All player data stays on-device.

### Wild Animal Pool — Hardcoded

The 12 wild species available as opponents are hardcoded in `BattleActivity.java`:

```
wolf, bear, eagle, shark, lion, tiger,
crocodile, gorilla, cheetah, panther, rhino, hyena
```

These were chosen for recognizability and variety of combat archetypes.

---

## Project Structure

```
Animal-Catch/
├── client/                     # Android application (Java)
│   └── app/src/main/java/
│       └── com/example/animalcatch/
│           ├── api/            # Retrofit service interfaces and response POJOs
│           ├── battle/         # BattleAnimal, BattleAI, EnemyScaler
│           ├── db/             # Room database, AnimalDao, AnimalEntity
│           ├── BattleActivity.java
│           └── ...
├── server/                     # Python backend API
│   ├── main.py
│   └── requirements.txt
└── README.md
```

---

## Android Client

**Language:** Java  
**Min SDK:** API 26  
**Target SDK:** API 35  

### Dependencies

| Library | Version | Purpose |
|---|---|---|
| Room | 2.x | Local SQLite database for caught animals |
| Retrofit 2 | 2.x | HTTP client for game API and Wikipedia API |
| Glide | 4.x | Image loading and caching (player photos + enemy sprites) |
| Material Components | 1.x | Buttons, progress bars, theming |
| ConstraintLayout | 2.x | Battle arena scene layout |

### Key Classes

**BattleActivity** — orchestrates the entire battle screen: picks a random player animal from Room, fetches enemy stats from the backend, runs the turn loop, awards XP, and persists results.

**BattleAnimal** — runtime model for a fighter. Holds current HP, max HP, ATK, DEF, SPD, and a defending flag. Exposes `receiveDamage()`, `defend()`, `isDefeated()`, and `getHpFraction()`.

**BattleAI** — stateless decision engine. Takes a `BattleAnimal` (the enemy) and returns `Action.ATTACK` or `Action.DEFEND` based on HP percentage thresholds and a random roll.

**EnemyScaler** — pure static utility. Scales raw base stats from the API up or down based on the player's current level so that battles remain challenging throughout progression.

**AnimalEntity / AnimalDao** — Room entity and DAO for the caught animal collection. Stores name, photo path, level, XP, HP, ATK, DEF, SPD.

**WikipediaApiClient / WikipediaSummaryResponse** — Retrofit interface and POJO for the Wikipedia page summary endpoint. Only the `thumbnail.source` field is used.

---

## Backend API

**Language:** Python  
**Hosting:** Render (free tier)  
**Base URL:** `https://animal-catch.onrender.com`

The backend serves one purpose: providing balanced base stats for each wild animal species so that the Android client does not need to hardcode numbers for every animal. Stats are then scaled client-side by `EnemyScaler` before the battle begins.

### Endpoints

#### GET /stats/{animal}

Returns base combat stats for a named animal species.

**Example request:**
```
GET https://animal-catch.onrender.com/stats/wolf
```

**Example response:**
```json
{
  "success": true,
  "name": "Wolf",
  "hp": 80,
  "atk": 22,
  "def": 14,
  "spd": 20
}
```

**Error response (unknown animal):**
```json
{
  "success": false
}
```

---

## Battle System

Each battle is between one of the player's caught animals and a randomly chosen wild opponent. The battle proceeds in turns until one fighter's HP reaches zero, or the player flees.

### Turn structure

1. Player chooses Attack, Defend, or Flee.
2. If Attack: damage is calculated and applied to the enemy. If Defend: the player's defending flag is set for this turn.
3. If the enemy is defeated, the battle ends immediately.
4. Otherwise, the enemy AI takes its turn (ATTACK or DEFEND).
5. If the player is defeated, the battle ends.
6. Repeat.

### Damage calculation

Damage dealt is based on the attacker's ATK stat versus the defender's DEF stat. If the defender chose Defend on their previous turn, incoming damage is reduced.

### Enemy AI behavior

| Enemy HP | Attack probability | Defend probability |
|---|---|---|
| Above 60% | 80% | 20% |
| 30% to 60% | 60% | 40% |
| Below 30% | 90% | 10% |

---

## XP and Leveling

XP is awarded at the end of every battle regardless of outcome.

| Outcome | XP gained |
|---|---|
| Win | 50 XP |
| Lose | 10 XP |

XP required to level up increases with each level (`AnimalEntity.xpForNextLevel(level)`). Multiple level-ups in a single battle are supported.

**Stat increases per level-up:**

| Stat | Increase |
|---|---|
| HP | +10 |
| ATK | +3 |
| DEF | +2 |
| SPD | +2 |

All changes are persisted to Room immediately after the battle ends.

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1) or newer
- Android device or emulator running API 26+
- Python 3.10+ (only needed to run the server locally)

### Running the server locally

```bash
cd server
pip install -r requirements.txt
python main.py
```

The API will start at `http://localhost:8000`. Update the base URL in `ApiClient.java` to point to your local server if you do not want to use the hosted Render instance.

### Running the Android app

1. Open the `client/` folder in Android Studio.
2. Let Gradle sync complete.
3. Connect a device or start an emulator.
4. Press Run.

On first launch, go catch some animals before attempting to battle — the battle screen will exit immediately if your collection is empty.

---

## Contributing

Pull requests are welcome. Please open an issue first to discuss what you would like to change. For bug reports, include your Android version, device model, and a description of what you expected versus what happened.

---

## License

[MIT](LICENSE)
