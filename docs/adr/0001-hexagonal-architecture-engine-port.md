# 1. Hexagonal Decoupling of Core Arcade Engines from Platform Renderers

Date: 2024-11-04
Status: Accepted

## Context
Video game logic tightly coupled to specific GUI toolkits (e.g., Android `SurfaceView` or desktop `JavaFX Canvas`) cannot be cross-compiled or tested headlessly in CI/CD without spinning up full display servers or hardware emulators.

## Decision
We implemented a Hexagonal Ports & Adapters architecture:
1. **Core Domain (`source/core/arcade/`):** Game loops (`Snake.java`, `Slots.java`, `Mines.java`, `TicTacToe.java`) depend exclusively on the abstract `Gfx` interface and pure math/data structures.
2. **Platform Adapters:**
   - **Android:** `AndroidGfx.java` wraps native `android.graphics.Canvas` and `Paint`.
   - **Desktop:** `JavaFxGfx.java` wraps `javafx.scene.canvas.GraphicsContext`.
3. **Headless Testing:** Unit tests instantiate mock or headless implementations to run 75 test suites in under 1 second without GUI dependencies.

## Consequences
- Guaranteed 100% logic and math parity between desktop and Android builds.
- Ultra-fast Maven testing in headless Linux CI environments.
