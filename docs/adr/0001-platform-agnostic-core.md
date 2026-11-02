# ADR 0001: Platform-Agnostic Core via Hexagonal Architecture

## Status
Accepted

## Context
Cross-platform 2D games often tie their domain logic directly to heavyweight game frameworks (e.g., LibGDX, Unity, Godot) or desktop-only UI kits (JavaFX, Swing).
This coupling causes:
1. Inability to run headless simulation tests in continuous integration environments.
2. Inflexibility when porting to mobile (Android Canvas/SurfaceView) or embedded targets.
3. Heavy dependency weight (hundreds of megabytes) and slow cold-start times.

## Decision Drivers
1. **Zero External Runtime Dependencies**: The core game logic must run on stock JVM 17+ and standard Android ART without external native libraries.
2. **Headless Testability**: The game simulation, collision detection, and scoring must run in automated CI pipelines without requiring a display server or virtual framebuffer (`Xvfb`).
3. **Pluggable Renderers**: The same core games must render identically on desktop and mobile.

## Considered Options
1. **LibGDX**: Established cross-platform framework, but requires large native binaries (LWJGL) and complex native setup.
2. **Pure JavaFX**: Clean on desktop, but unsupported natively on modern Android without experimental GraalVM mobile tooling.
3. **Hexagonal Architecture with Custom Micro-Gfx Port (Chosen)**:
   - Core domain (`source/core`) contains zero UI imports and renders entirely to a lightweight `Gfx` interface.
   - Desktop and Android provide lightweight adapters that map `Gfx` calls to `GraphicsContext` and `android.graphics.Canvas` respectively.

## Consequences
- **Positive**:
  - Blazing fast cold-start: Desktop app boots in <200 milliseconds.
  - Headless test execution: Hundreds of simulated game frames can be verified in seconds in headless Linux CI.
  - 100% code reuse of game rules, scoring, and option persistence across desktop and mobile.
- **Negative / Mitigations**:
  - We had to write vector drawing primitives manually in `Gfx`.
  - *Mitigation*: The drawing primitives are standard 2D vector calls (`rect`, `roundRect`, `oval`, `line`, `text`), keeping the port minimal and robust.
