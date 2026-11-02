# kvolegi arcade 🕹️✨

<p align="center">
  <img src="icons/logo_master.png" alt="kvolegi arcade logo" width="380"/>
</p>

<p align="center">
  <strong>Cross-Platform Retro-Neon Arcade Game Suite</strong><br>
  <em>Built by <strong>@kvolegi</strong> • 2023 – 2024</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17%20%7C%2021-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java"/>
  <img src="https://img.shields.io/badge/JavaFX-21-FF6F00?style=for-the-badge&logo=java&logoColor=white" alt="JavaFX"/>
  <img src="https://img.shields.io/badge/Android-API%2024--35-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android"/>
  <img src="https://img.shields.io/badge/CI-Passing-success?style=for-the-badge&logo=githubactions&logoColor=white" alt="CI"/>
  <img src="https://img.shields.io/badge/Tests-15%20Passed-brightgreen?style=for-the-badge" alt="Tests"/>
  <img src="https://img.shields.io/badge/Architecture-Hexagonal%20Ports%20%26%20Adapters-blue?style=for-the-badge" alt="Architecture"/>
</p>

---

## About the Project

**kvolegi arcade** is a standalone cross-platform arcade hub written in Java. The project was built from scratch between November 2023 and April 2024 to explore custom 2D rendering, cross-platform code sharing without third-party game engines, game mathematics, and real-time heuristics.

The suite bundles four complete arcade games with a shared Cyberpunk / Neon visual style and dynamic RGB bloom:
1. **Tic Tac Toe (Neon Edition)** — Dynamic expanding grid (from 5x5 up to 25x25), 5-in-a-row win condition, Time-Attack score mode, and heuristic threat-evaluation AI.
2. **Sigma Slots** — 4x5 reel matrix with 20 classic casino paylines, balanced volatility, Wild & Scatter bonus mechanics, and an interactive paytable.
3. **Beer Snake** — Classic arcade snake with 25 progression levels, switchable toroidal wrap-around vs. solid wall collision, and dynamic speed scaling.
4. **Bobr Minesweeper** — Beaver & Broccoli tactical minesweeper featuring guaranteed first-click safety, Sonar Radar pulse ability, and custom field densities.

---

## 📸 Screenshots

### Desktop (JavaFX) vs Mobile (Native Android Canvas)

| Desktop Edition (macOS / Linux / Windows) | Mobile Edition (Android APK) |
| :---: | :---: |
| ![PC Arcade Hub](docs/screenshots/pc_arcade_hub.png) <br> *Main Arcade Hub (JavaFX GPU Bloom)* | ![Mobile Arcade Hub](docs/screenshots/mobile_arcade_hub.png) <br> *Main Arcade Hub (Android Native Surface)* |
| ![PC Tic Tac Toe](docs/screenshots/pc_ttt_board.png) <br> *Infinite Memory Board (JavaFX)* | ![Mobile Tic Tac Toe](docs/screenshots/mobile_ttt_board.png) <br> *Infinite Memory Board (Android Canvas)* |
| ![PC Sigma Slots](docs/screenshots/pc_slots_game.png) <br> *20-Line Vegas Slots (JavaFX)* | ![Mobile Sigma Slots](docs/screenshots/mobile_slots_game.png) <br> *20-Line Vegas Slots (Android Surface)* |
| ![PC Beer Snake](docs/screenshots/pc_snake_game.png) <br> *Beer Snake Game Loop (JavaFX)* | ![Mobile Beer Snake](docs/screenshots/mobile_snake_game.png) <br> *Beer Snake Game Loop (Android Canvas)* |
| ![PC Minesweeper](docs/screenshots/pc_mines_game.png) <br> *Bobr Minesweeper (JavaFX)* | ![Mobile Minesweeper](docs/screenshots/mobile_mines_game.png) <br> *Bobr Minesweeper (Android Canvas)* |

---

## 🏗️ Architecture & Technical Decisions

For complete technical specifications, see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

### Architecture Decision Records (ADRs)
The project's architectural trade-offs are documented under [docs/adr/](docs/adr/):
- **[ADR-0001: Platform-Agnostic Core via Hexagonal Architecture](docs/adr/0001-platform-agnostic-core.md)** — Zero external runtime dependencies, 100% headless testability.
- **[ADR-0002: Elimination of Runtime GC Pauses in 60 FPS Render Loop](docs/adr/0002-gc-allocation-free-render-loop.md)** — Object reuse, packed ARGB integers, and zero-allocation frame loops.
- **[ADR-0003: Multi-Platform Fat-JAR Distribution](docs/adr/0003-multi-platform-fat-jar.md)** — Bundled multi-OS native bindings in a single executable artifact.

The codebase uses a clean three-tier structure that isolates core game rules from platform presentation:

```
kvolegi-arcade/
├── source/
│   ├── core/                  # Pure Java Standard Library (Zero dependencies)
│   │   └── arcade/            # Game logic, state machines, math, data structures
│   ├── desktop/               # Desktop Frontend (JavaFX 21 + Reactive Controls)
│   │   └── arcade/desktop/    # Hardware-accelerated UI, Window management, Launcher
│   └── android/               # Mobile Frontend (Android Native SDK)
│       └── src/main/java/     # SurfaceView, dedicated 60 FPS RenderThread, Touch dispatch
├── assets/                    # High-res PNG sprites, TTF fonts, audio FX
├── builds/                    # Distribution binaries
│   ├── kvolegi-arcade-universal.jar  # Universal cross-platform JAR (Windows / Linux / macOS)
│   ├── mac/                   # kvolegi arcade.app & macOS runner
│   └── android/               # kvolegi-arcade.apk (Signed Release APK)
└── docs/                      # Screenshots, technical documentation & specifications
```

### 1. Why JavaFX on Desktop?
* **Hardware Acceleration:** JavaFX hooks straight into native GPU backends (Metal/OpenGL on macOS, DirectX on Windows), enabling real-time CSS pseudo-class transitions, neon drop-shadow filters, and fluid window resizing without frame drops.
* **Responsive Layouts:** Hierarchical `BorderPane`, `VBox`, and `GridPane` containers adapt smoothly from 800x600 windowed mode up to 4K displays.
* **Precise Input Handling:** Desktop controls use `setPickOnBounds(false)` overrides so wide 50px Gaussian glow effects do not inflate clickable bounding boxes or cause misclicks.

### 2. Why Native Android SurfaceView instead of Unity or LibGDX?
* **Compact APK Size:** General-purpose game engines add 30–60 MB of overhead. Rendering directly to Android `SurfaceView` and `Canvas` kept the full suite down to **~5.6 MB**.
* **Isolated 60 FPS Loop:** Rendering runs on a dedicated background thread (`ArcadeView.java`) that locks the canvas via double-buffering (`surfaceHolder.lockCanvas()`), keeping heavy drawing separate from the main OS UI thread.
* **Low Memory Churn:** Object reuse and minimal allocations inside the draw loop prevent frequent garbage collection pauses.

---

## 🧠 Game Logic & Mechanics

### 1. Tic Tac Toe — Dynamic Centered Expansion
Rather than a fixed 3x3 grid, the board grows dynamically:
* **Dynamic Expansion:** The board starts at $5 \times 5$. When over 50% of the cells fill up, the matrix expands by 5 rows and columns up to $25 \times 25$.
* **Center Coordinate Remapping:** Existing moves transfer to the center of the expanded matrix using:
  $$\Delta = \frac{N_{\text{new}} - N_{\text{old}}}{2}, \quad (r', c') = (r + \Delta, c + \Delta)$$
* **Adaptive Scaling:** Cell sizes rescale dynamically to fit available screen space:
  $$\text{cellSize} = \max\left(18, \min\left(88, \frac{\min(W_{\text{avail}}, H_{\text{avail}}) - (N - 1) \cdot \text{gap}}{N}\right)\right)$$
* **Threat-Evaluation AI:**
  The AI checks open-ended lines across four vectors: horizontal $[0, 1]$, vertical $[1, 0]$, main diagonal $[1, 1]$, and anti-diagonal $[1, -1]$. It prioritizes immediate 5-in-a-row wins ($+50,000$ pts), blocks opponent 4-in-a-row threats ($+15,000$ pts), and looks for branching double forks.

### 2. Sigma Slots — Paylines & Probability
* **4 Rows $\times$ 5 Reels Grid:** 20 symbol positions evaluated on each spin across 20 distinct paylines (straight rows, V-shapes, zigzags, and waves).
* **Payout Tiers:**
  * Low tier: Grape, Watermelon, Lemon, Plum, Pineapple ($0.18\times$ to $2.5\times$ bet).
  * Mid tier: Dragonfruit, Banana ($0.35\times$ to $6.0\times$ bet).
  * High tier (WILD): `BIGBANANA` ($1.8\times$ to $12.0\times$ bet, substitutes for standard symbols).
  * Jackpot tier (SCATTER): `BIGWIN` ($4.0\times$ to $30.0\times$ bet).
* **Custom Bet Input:** Players can bump bets by 10 or type an exact wager using a direct numeric dialog.

### 3. Beer Snake — Dual Physics & Difficulty Scaling
* **Level Progression:** 25 stages requiring 4 collectibles each, scaling speed on each tier:
  $$\text{Delay}_{\text{tick}}(L) = \max\left(60\text{ms}, \text{Delay}_{\text{base}} - L \times 5\text{ms}\right)$$
* **Wall Physics:** Toggle between solid walls (fatal boundary collision) and toroidal wrap-around ($x' = (x + \text{cols}) \pmod{\text{cols}}$).

### 4. Bobr Minesweeper — Safe Start & Sonar Pulse
* **Guaranteed First-Click Safety:** The mine matrix generates only after the player clicks, ensuring the starting tile is always safe and opens adjacent blank space.
* **Sonar Pulse:** A tactical ability revealing hidden beaver positions in a $3 \times 3$ area without triggering detonation.

---

## 🚀 How to Run

### Option 1: Universal JAR (Windows & Linux)

The standalone executable JAR `builds/kvolegi-arcade-universal.jar` bundles all necessary multiplatform desktop runtime components and works on Windows, Linux, and macOS.

#### Prerequisites
Java 17 or Java 21+ is required.
* **Windows:**
  Install OpenJDK via winget:
  ```powershell
  winget install EclipseAdoptium.Temurin.21.JRE
  ```
  Or download the installer from [Adoptium](https://adoptium.net).
* **Linux (Ubuntu / Debian / Mint):**
  ```bash
  sudo apt update && sudo apt install openjdk-21-jre
  ```
* **Linux (Fedora / RHEL):**
  ```bash
  sudo dnf install java-21-openjdk
  ```
* **Linux (Arch Linux):**
  ```bash
  sudo pacman -S jre21-openjdk
  ```

#### Launch Command
Open your terminal (PowerShell or Bash) in the project directory:
```bash
java -jar builds/kvolegi-arcade-universal.jar
```

---

### Option 2: macOS Application (`kvolegi arcade.app`)

For macOS users, a pre-packaged `.app` bundle is available under `builds/mac/`:

1. Open `builds/mac/` in Finder.
2. Double-click **`kvolegi arcade.app`**.
3. **If macOS Gatekeeper blocks the app** with a warning stating the developer cannot be verified:
   * **Terminal fix:** Open Terminal and remove the quarantine attribute:
     ```bash
     xattr -cr "builds/mac/kvolegi arcade.app"
     ```
   * **GUI fix:** Right-click (or Control-click) `kvolegi arcade.app` in Finder, select **Open**, and confirm **Open** in the dialog.
4. Alternatively, launch directly via terminal script:
   ```bash
   ./builds/mac/run_mac.sh
   ```

---

### Option 3: Android Device (`.apk`)

1. Copy `builds/android/kvolegi-arcade.apk` to your phone or tablet (Android 7.0 / API 24 or newer).
2. Open your device's file manager, tap the APK file, and allow installation from this source.
3. Launch **kvolegi arcade** from your app drawer.

---

## 🛠️ Building from Source

### Prerequisites
* JDK 17 or JDK 21+
* Python 3.10+ (for automation scripts)
* Android SDK Platform 35 & Build-Tools 35.0.0 (only if rebuilding the Android APK)

### Run Build
```bash
python3 build_all.py
```

---

## 🧪 Automated Testing

The repository contains a standalone verification suite (`tests/ArcadeTestSuite.java`) covering core algorithms, matrix transformations, and payout rules:

```bash
# Run test suite via Python helper
python3 tests/run_tests.py

# Or directly through Java source runner
java tests/ArcadeTestSuite.java
```

### Test Coverage:
* **Matrix Expansion:** $5 \times 5 \to 10 \times 10$ growth at $>50\%$ occupancy, centered coordinate remapping $(\Delta = 2)$.
* **5-in-a-Row Detection:** Validates victory vectors across horizontal, vertical, and both diagonal directions.
* **Heuristic AI Evaluation:** Confirms threat weight hierarchy (win $50,000 >$ block $15,000 >$ fork $1,200$).
* **Slots Payline Alignment:** Full coordinate check across all 20 paylines.
* **Wild & Scatter Rules:** Substitution logic and multiplier triggers.
* **Snake Movement Physics:** Toroidal coordinate wrapping and solid wall collision boundaries.
* **Minesweeper First-Click Guarantee:** Ensures zero mines on the initial click with recursive cascading open.
* **Hit-Testing Bounds:** Checks `setPickOnBounds(false)` to prevent 50px glow shaders from intercepting clicks on adjacent buttons.

---

## ⚖️ License & Copyright

```
kvolegi arcade — Multiplatform Retro-Neon Arcade Suite
Copyright (c) 2023-2024 kvolegi (@kvolegi). All Rights Reserved.
```

Personal portfolio project. Provided for evaluation, educational demonstration, and code review.
