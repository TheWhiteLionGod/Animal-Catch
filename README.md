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

Animal Catch combines real-world exploration with turn-based RPG mechanics. Players photograph animals they find outside — a dog in the park, a bird on a fence, a squirrel in a tree — and a Vision Transformer model automatically identifies the species. A language model then generates balanced RPG stats for that animal based on its real-world traits. The caught animal joins the player's roster and can be used in battles against wild opponents whose stats are also LLM-generated and scaled to the player's current level.

The core loop is: **go outside → photograph → AI identifies and stats the animal → battle → level up → repeat**.

---

## Features

- Catch real animals by photographing them with your device camera
- AI-powered animal identification using a Vision Transformer model trained on iNaturalist 21
- LLM-generated RPG stats (HP, ATK, DEF, SPD) tailored to each animal species
- Turn-based battles with Attack, Defend, and Flee actions
- Rule-based enemy AI that adapts strategy based on remaining HP
- XP and leveling system with stat growth on level-up
- Wild enemy stats scaled dynamically to the player's level
- Enemy sprite images fetched live from the Wikipedia API
- Local animal collection persisted with Room (SQLite)
- Entrance animations and VS badge for battle start
- HP bars that shift color from green to amber to red as health drops

---

## AI Architecture

The app uses two separate AI models running on the Python backend, plus a rule-based decision engine on the client side.

### Pipeline 1 — Animal Identification (Catching)

**Input:** A photo taken by the player on their device camera, sent as a POST request to `/identify`.

**AI capability:** Image classification. The server runs a Vision Transformer model (`timm/vit_large_patch14_clip_336.laion2b_ft_augreg_inat21`) via Hugging Face Transformers, which was trained on the iNaturalist 21 wildlife dataset and classifies images into animal species by scientific name.

**Processing:**
1. The image is decoded and converted to RGB on the server.
2. The ViT model runs inference and returns a ranked list of species predictions with confidence scores.
3. The top prediction (highest confidence score) is selected — this is a scientific name (e.g. `Canis lupus familiaris`).
4. The scientific name is resolved to an English common name by querying the GBIF Species API (`/species/match` then `/species/{id}/vernacularNames`).
5. The common name is returned to the Android client.

**Output:** The common English name of the identified animal (e.g. `wolf`, `golden eagle`), which becomes the name of the caught animal stored in Room.

---

### Pipeline 2 — Stat Generation (Battle Prep)

**Input:** An animal name string sent to `GET /generatestats/{animalName}`.

**AI capability:** Text generation / structured data generation. The server runs Qwen 2.5 1.5B Instruct, a small language model, prompted to act as an RPG game balancing engine.

**Processing:**
1. The server constructs a two-message prompt: a system message instructing the model to output only a JSON object with no extra text, and a user message asking for RPG stats (1–100) for the given animal name.
2. The model generates a response with `temperature=0.1` and `do_sample=False` to keep output deterministic and well-formed.
3. Any markdown code fences are stripped from the output.
4. The raw text is parsed as JSON into `{name, hp, atk, def, spd}`.
5. The stats are returned to the Android client.
6. `EnemyScaler` on the client multiplies the base stats by a factor derived from the player's current level before the battle begins.

**Output:** Scaled HP, ATK, DEF, and SPD values displayed in the enemy HUD and used for all damage calculations during the battle.

---

### Pipeline 3 — Battle AI (Enemy Decision-Making)

**Input:** The enemy's current HP as a fraction of its maximum, read each turn by `BattleAI.decideAction()`.

**AI capability:** Rule-based game agent. No machine learning is involved — this is an explicit probability decision tree running entirely on the Android client.

**Processing:**
```
HP > 60%  ->  Attack 80% of the time, Defend 20%
HP 30-60% ->  Attack 60% of the time, Defend 40%
HP < 30%  ->  Attack 90% of the time, Defend 10%
```

**Output:** An ATTACK or DEFEND action applied each enemy turn, described in the battle log.

---

### Summary of Inputs and Outputs

| Stage | Input | Output |
|---|---|---|
| Catch | Player camera photo | Common animal name |
| Stat generation | Animal name string | HP, ATK, DEF, SPD (1-100) |
| Enemy scaling | Base stats + player level | Scaled stats for the battle |
| Battle AI | Enemy HP fraction | ATTACK or DEFEND action |
| Enemy sprite | Animal name | Wikipedia thumbnail URL, displayed via Glide |

---

## Responsible AI

### Risk: Language model generating unbalanced or malformed stats

The Qwen model could produce stats that are wildly unbalanced (e.g. a rabbit with 100 ATK and 1 HP) or fail to return valid JSON entirely, which would either break the battle or make it trivially easy or impossible. This is mitigated in three ways: the system prompt strictly instructs the model to output only a valid JSON object with no extra text; `temperature=0.1` with `do_sample=False` keeps the output near-deterministic and well-structured; and the server wraps JSON parsing in a try/except that returns a clean error response if parsing fails, so the client can handle it gracefully rather than crash.

### Risk: Image classifier misidentifying an animal

The ViT model may confidently misclassify an animal — labeling a dog as a wolf, for example — which would give the caught animal an incorrect name and potentially misleading stats. Since the classification result directly determines what gets stored in the player's collection, a wrong label sticks permanently. This is partially mitigated by using a large, high-accuracy model trained on iNaturalist 21 (a specialist wildlife dataset), and by the fact that the game consequence of a misidentification is minor — a wrongly named animal still functions identically in battle.

### Risk: Exclusion of users without access to outdoor spaces

The app's core loop requires going outside to photograph real animals. Users in dense urban environments, users with mobility restrictions, or users in regions with limited accessible wildlife may find the catching mechanic difficult to engage with. Future versions could broaden the definition of a valid catch to include pets, zoo visits, or nature documentaries to reduce this barrier.

---

## AI Tools Used

| Tool | Model / Version | Purpose | Cost |
|---|---|---|---|
| Hugging Face Transformers — image classification | `timm/vit_large_patch14_clip_336.laion2b_ft_augreg_inat21` | Identifies animal species from player photos | Free |
| Hugging Face Transformers — text generation | `Qwen/Qwen2.5-1.5B-Instruct` | Generates RPG stats for any animal name | Free |
| GBIF Species API | REST API | Converts scientific species names to English common names | Free |
| Wikipedia REST API | `/page/summary/{title}` | Fetches real animal photos for enemy sprites | Free |
| Rule-based BattleAI | Custom Java (no model) | Enemy turn decision-making during battle | Free |
| Claude (Anthropic) | Claude Sonnet | AI coding assistance during development | Free tier / Paid |

---

## Data Sources

### Image Classification Training Data — iNaturalist 21

The ViT model used for animal identification was pre-trained on iNaturalist 21, a large public dataset of wildlife photographs labeled with scientific species names. It contains over 2.7 million images across 10,000 species, curated by the citizen science platform iNaturalist. The model was not fine-tuned for this project — the pre-trained checkpoint is used directly via Hugging Face.

### Animal Base Stats — Synthetic (LLM-Generated)

Stats are not hardcoded. Each time a battle begins, the server prompts the Qwen 2.5 1.5B model to reason about the animal's real-world traits and produce balanced RPG numbers between 1 and 100. The stats are synthetic in the sense that they are generated on-demand rather than pulled from a database, but they are grounded in the model's knowledge of each species. A wolf will consistently receive higher ATK and SPD than a tortoise, for example, because the model encodes that knowledge from its training data.

### Species Name Resolution — GBIF Species API

The Global Biodiversity Information Facility (GBIF) API is used to convert scientific species names (output by the ViT classifier) into English common names. GBIF is a free, open-access biodiversity data platform maintained by an international network of institutions. No API key is required.

### Animal Photos — Wikipedia REST API

Enemy sprite images are fetched at runtime from the Wikipedia page summary endpoint:

```
https://en.wikipedia.org/api/rest_v1/page/summary/{AnimalName}
```

The `thumbnail.source` field is used as the image URL. Wikipedia content is freely licensed under Creative Commons. No images are stored server-side or in the app's local database — they are fetched fresh each battle.

### Player Animal Photos — Device Camera (User-Generated)

Photos taken by the player are stored locally on the device. The file path is persisted in Room (SQLite). No photos are uploaded to any server — all player data stays on-device.

### Wild Animal Pool — Hardcoded

The 12 wild species available as battle opponents are hardcoded in `BattleActivity.java`:

```
wolf, bear, eagle, shark, lion, tiger,
crocodile, gorilla, cheetah, panther, rhino, hyena
```

These were chosen for recognizability and variety of combat archetypes. Because stats are LLM-generated rather than hardcoded, any animal name could in principle be added to this pool without any backend changes.

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
├── server/                     # Python backend (Flask)
│   ├── main.py                 # Flask app, route handlers
│   ├── animalclassifier.py     # ViT image classification pipeline
│   ├── statgenerator.py        # Qwen LLM stat generation pipeline
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

The backend exposes two AI-powered endpoints: one that identifies an animal from a photo, and one that generates RPG stats for any animal name. Stats are scaled client-side by `EnemyScaler` after being received.

### Endpoints

#### POST /identify

Accepts an image file and returns the identified animal's common English name.

**Request:** `multipart/form-data` with an `image` field (PNG, JPG, or JPEG).

**Example response:**
```json
{
  "success": true,
  "animalName": "wolf"
}
```

#### GET /generatestats/{animalName}

Prompts the Qwen LLM to generate balanced RPG stats for the given animal name.

**Example request:**
```
GET https://animal-catch.onrender.com/generatestats/wolf
```

**Example response:**
```json
{
  "success": true,
  "name": "Wolf",
  "hp": 78,
  "atk": 24,
  "def": 15,
  "spd": 21
}
```

**Error response:**
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

The API will start at `http://localhost:5000`. Update the base URL in `ApiClient.java` to point to your local server if you do not want to use the hosted Render instance. Note that the ViT classifier and Qwen model will be downloaded from Hugging Face on first run — this may take several minutes depending on your connection. GPU is used automatically if available, with a CPU fallback.

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
