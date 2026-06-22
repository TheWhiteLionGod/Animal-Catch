#  Animal Catch

> A mobile app that encourages its users to go outside and explore the world.

Animal Catch is an Android game that blends real-world exploration with Pokémon-style creature collecting and turn-based battles. Go outside, photograph real animals you find, catch them, train them, and battle wild opponents pulled from a live backend API.

---

##  Features

- **Catch real animals** — photograph creatures you find outside and add them to your collection
- **Live battle system** — turn-based combat with Attack, Defend, and Flee actions
- **Rule-based enemy AI** — wild animals adapt their strategy based on remaining HP
- **XP & leveling** — earn XP from every battle win or loss; level up to boost your animal's stats
- **Scaled wild encounters** — enemy stats scale to your animal's level so fights stay challenging
- **Real enemy photos** — wild opponent sprites are fetched live from Wikipedia
- **Persistent collection** — your caught animals are stored locally with Room (SQLite)
- **Live animal stats** — base stats for wild animals are served from a Python backend API

---

##  Project Structure

```
Animal-Catch/
├── client/          # Android app (Java)
├── server/          # Python backend API (FastAPI / Flask)
├── api/             # API contract / shared types
└── .github/
    └── workflows/   # CI/CD pipelines
```

---

##  Android Client

**Language:** Java  
**Min SDK:** API 26+  
**Architecture:** Single-activity with multiple Activity screens

### Key libraries

| Library | Purpose |
|---|---|
| Room | Local animal database |
| Retrofit 2 | REST calls to the game API and Wikipedia |
| Glide | Image loading (player photos + enemy sprites) |
| Material Components | UI buttons, progress bars |
| ConstraintLayout | Arena battle scene layout |

### Screens

- **Main / Collection** — browse your caught animals
- **Camera / Catch** — photograph a real animal to add it to your roster
- **Battle** — pick a random animal from your collection and fight a wild opponent

---

## ⚔️ Battle System

Each battle round the player chooses an action:

| Action | Effect |
|---|---|
|  Attack | Deal damage based on ATK vs enemy DEF |
|  Defend | Reduce incoming damage this turn |
|  Flee | Exit the battle immediately |

**Enemy AI strategy:**

| Enemy HP | Attack chance | Defend chance |
|---|---|---|
| > 60% | 80% | 20% |
| 30–60% | 60% | 40% |
| < 30% | 90% | 10% |

**XP rewards:**

| Outcome | XP |
|---|---|
| Win | +50 XP |
| Lose | +10 XP |

On level-up: +10 HP, +3 ATK, +2 DEF, +2 SPD

---

##  Backend API

**Language:** Python  

The server exposes animal base stats that the Android client fetches at the start of each battle. Wild animal stats are then scaled server-side (or client-side) to match the player's current level.

### Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/stats/{animal}` | Returns base HP, ATK, DEF, SPD for a named animal |

---

##  Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- Android device or emulator running API 26+
- Python 3.12+ (to run the server locally)

### Run the server locally

```bash
cd server
pip install -r requirements.txt
python main.py
```

The API will be available at `http://localhost:8000`.

Update the base URL in `client/app/src/main/java/.../api/ApiClient.java` to point to your local server if needed.

### Run the Android app

1. Open the `client/` folder in Android Studio
2. Sync Gradle
3. Run on a device or emulator

---

##  Contributing

Pull requests are welcome! Please open an issue first to discuss what you'd like to change.

---

##  License

[MIT](LICENSE)
