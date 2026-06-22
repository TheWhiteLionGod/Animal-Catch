# Animal Catch

A mobile Android app that encourages users to go outside, photograph real animals they encounter, and build a collection of creatures to train and battle. Inspired by creature-collecting games, Animal Catch grounds the experience in the real world — every animal in your roster is one you actually found and photographed yourself.

---

## Table of Contents

* [Overview](#overview)
* [Features](#features)
* [AI Architecture](#ai-architecture)
* [Responsible AI](#responsible-ai)
* [AI Tools Used](#ai-tools-used)
* [Data Sources](#data-sources)
* [Project Structure](#project-structure)
* [Android Client](#android-client)
* [Backend API](#backend-api)
* [API Documentation](#api-documentation)
* [Battle System](#battle-system)
* [XP and Leveling](#xp-and-leveling)
* [Getting Started](#getting-started)
* [Contributing](#contributing)
* [License](#license)

---

# Overview

Animal Catch combines real-world exploration with turn-based RPG mechanics. Players photograph animals they find outside — a dog in the park, a bird on a fence, a squirrel in a tree — and a Vision Transformer model automatically identifies the species. A language model then generates balanced RPG stats for that animal based on its real-world traits.

The core gameplay loop is:

**Go outside → Photograph → Identify → Battle → Level Up → Repeat**

---

# Features

* Catch real animals by photographing them
* AI-powered species identification
* LLM-generated RPG statistics
* Turn-based battles
* Dynamic enemy scaling
* XP and leveling system
* Local collection database
* Enemy images fetched from Wikipedia
* Battle animations and health bars
* Rule-based enemy AI

---

# AI Architecture

## Pipeline 1 — Animal Identification

**Input:** Player photo

* Image uploaded to `/identify`
* Vision Transformer performs classification
* Scientific name produced
* GBIF converts scientific names into common names

**Output:** Common animal name

---

## Pipeline 2 — Stat Generation

**Input:** Animal name

* Prompt sent to Qwen 2.5 1.5B
* Model outputs JSON
* Server validates response
* Client scales stats

**Output:** HP, ATK, DEF, SPD

---

## Pipeline 3 — Battle AI

Rule-based enemy AI:

```
HP > 60%    Attack 80%
HP 30-60%   Attack 60%
HP < 30%    Attack 90%
```

---

# Responsible AI

## Incorrect Species Identification

Animal photographs may occasionally be classified incorrectly. Since gameplay effects are minor, incorrect labels primarily affect the animal name.

## Invalid Stat Generation

The language model may produce malformed or unbalanced stats. Low temperature settings and JSON validation reduce this risk.

## Accessibility

The game requires going outdoors, which may limit accessibility for some players. Future versions could support pets, zoos, or educational sources.

---

# AI Tools Used

| Tool                       | Purpose                |
| -------------------------- | ---------------------- |
| ViT Large (iNaturalist 21) | Animal identification  |
| Qwen 2.5 1.5B Instruct     | RPG stat generation    |
| GBIF API                   | Common species names   |
| Wikipedia API              | Enemy images           |
| Custom BattleAI            | Enemy decisions        |
| Claude Sonnet              | Development assistance |

---

# Data Sources

## iNaturalist 21

Over 2.7 million wildlife images used to train the ViT model.

## GBIF Species API

Converts scientific species names into English names.

## Wikipedia REST API

Provides enemy thumbnail images.

## User Photos

Stored locally on the device.

## Wild Enemy Pool

```
wolf
bear
eagle
shark
lion
tiger
crocodile
gorilla
cheetah
panther
rhino
hyena
```

---

# Project Structure

```text
Animal-Catch/
├── client/
│   └── app/src/main/java/
│       └── com/example/animalcatch/
│           ├── api/
│           ├── battle/
│           ├── db/
│           └── BattleActivity.java
├── server/
│   ├── main.py
│   ├── animalclassifier.py
│   ├── statgenerator.py
│   └── requirements.txt
└── README.md
```

---

# Android Client

**Language:** Java

**Min SDK:** 26

**Target SDK:** 35

## Dependencies

* Room
* Retrofit
* Glide
* Material Components
* ConstraintLayout

---

# Backend API

**Language:** Python

**Framework:** Flask

**Hosting:** Render

**Base URL:**

```text
https://animal-catch.onrender.com
```

---

# API Documentation

## Authentication

No authentication is currently required.

Future versions may support:

* Accounts
* Cloud saves
* Leaderboards
* Multiplayer

---

## POST /identify

Identifies an animal from an uploaded image.

### Request

**Content Type:**

```text
multipart/form-data
```

### Parameters

| Name  | Type | Required |
| ----- | ---- | -------- |
| image | File | Yes      |

### Example

```bash
curl -X POST \
  -F "image=@wolf.jpg" \
  https://animal-catch.onrender.com/identify
```

### Response

```json
{
  "success": true,
  "animalName": "wolf"
}
```

### Error

```json
{
  "success": false
}
```

---

## GET /generatestats/{animalName}

Generates RPG stats.

### Example

```http
GET /generatestats/wolf
```

### Response

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

---

## HTTP Status Codes

| Code | Meaning            |
| ---- | ------------------ |
| 200  | Success            |
| 400  | Invalid request    |
| 404  | Endpoint not found |
| 500  | Server error       |

---

## Example Retrofit Interface

```java
@Multipart
@POST("identify")
Call<IdentifyResponse> identifyAnimal(
    @Part MultipartBody.Part image
);

@GET("generatestats/{animal}")
Call<StatsResponse> generateStats(
    @Path("animal") String animal
);
```

---

## API Workflow

```text
Player takes photo
        ↓
POST /identify
        ↓
Animal identified
        ↓
Animal saved
        ↓
GET /generatestats
        ↓
Stats generated
        ↓
Battle begins
```

---

# Battle System

1. Player attacks, defends, or flees.
2. Enemy takes a turn.
3. HP updates.
4. Defeated animal loses.
5. XP awarded.

---

## Enemy AI

| HP        | Attack | Defend |
| --------- | ------ | ------ |
| Above 60% | 80%    | 20%    |
| 30-60%    | 60%    | 40%    |
| Below 30% | 90%    | 10%    |

---

# XP and Leveling

| Outcome | XP |
| ------- | -- |
| Win     | 50 |
| Lose    | 10 |

### Level-up bonuses

| Stat | Increase |
| ---- | -------- |
| HP   | +10      |
| ATK  | +3       |
| DEF  | +2       |
| SPD  | +2       |

---

# Getting Started

## Requirements

* Android Studio Hedgehog or newer
* Android API 26+
* Python 3.10+

---

## Running the Server

```bash
cd server
pip install -r requirements.txt
python main.py
```

Server starts at:

```text
http://localhost:5000
```

---

## Running the App

1. Open `client/`
2. Sync Gradle
3. Connect device or emulator
4. Press Run

Catch animals before entering battle mode.

---

# Future Improvements

* Multiplayer battles
* Online accounts
* Global leaderboards
* Trading animals
* Daily challenges
* Seasonal events
* Expanded animal datasets
* Cooperative battles

---

# Contributing

Pull requests are welcome.

Please open an issue before major changes.

Include:

* Android version
* Device model
* Expected behavior
* Actual behavior

---

# License

MIT License
