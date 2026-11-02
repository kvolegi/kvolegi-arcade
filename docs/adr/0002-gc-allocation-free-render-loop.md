# ADR 0002: Elimination of Runtime Garbage Collection Pauses in 60 FPS Render Loop

## Status
Accepted

## Context
In real-time 2D games running on the Java Virtual Machine (JVM) or Android Runtime (ART), instantiating short-lived objects inside the frame update loop (`frame(Gfx g, float dt, float w, float h)`) rapidly fills Young Generation memory.
This triggers frequent minor Garbage Collection (GC) pauses. Even a 16-millisecond GC pause causes a visible dropped frame, creating noticeable visual stutter.

## Decision Drivers
1. **Rock-solid 60 FPS**: Maintain a strict 16.6ms frame budget without jitter.
2. **Smooth Mobile Operation**: Android mobile devices have limited memory bandwidth and aggressive GC pause penalties.
3. **Low Latency Input**: Input polling and response must not be blocked behind memory compaction.

## Considered Options
1. **Idiomatic Functional Object Creation**: Allocating new `Vector2D`, `Point`, or `Color` objects every tick.
   - *Result*: Hundreds of thousands of allocations per minute; periodic micro-stutters.
2. **Object Pooling and Packed Primitives (Chosen)**:
   - All colors are represented as 32-bit packed integers (`0xAARRGGBB`) manipulated via bitwise math in `C.java`.
   - Particle systems recycle fixed-capacity primitive arrays (`float[] parts = {x, y, vx, vy, life}`).
   - Game grids use fixed 2D primitive arrays rather than nested collections.

## Consequences
- **Positive**:
  - Near-zero allocations during active gameplay frames.
  - Zero dropped frames due to GC pauses across macOS, Linux, Windows, and Android.
  - Memory footprint remains completely stable over hours of continuous play.
- **Negative / Mitigations**:
  - Code uses primitive arrays rather than modern Java records/objects inside hot loops.
  - *Mitigation*: The mathematical operations are strictly encapsulated inside `C.java` with comprehensive test coverage.
