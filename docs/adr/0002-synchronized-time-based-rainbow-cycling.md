# 2. Synchronized Time-Based Neon Glow without Memory Allocation

Date: 2024-11-04
Status: Accepted

## Context
Rendering pulsating neon glow and cycling rainbow animations on mobile devices typically suffers from GC (Garbage Collection) pauses if color instances or intermediate objects are allocated per frame ($60\text{--}120\text{ FPS}$).

## Decision
We implemented closed-form integer math in `Art.cyclingRainbow(float time)`:
1. **Zero-Allocation Color Math:** Computes ARGB hexadecimal integers using direct trigonometric and bit-shift operations without creating `java.awt.Color` or `javafx.scene.paint.Color` objects.
2. **Global Time Synchronization:** All UI components, borders, and buttons read from `ui.time`, guaranteeing that multiple on-screen elements (e.g. Settings toggles, Slots paylines, Snake body segments) cycle in perfect visual harmony rather than chaotic individual phases.

## Consequences
- Constant 60 FPS performance on mid-tier Android devices with zero garbage collection stutter.
- Completely unified visual aesthetic across all mini-games and dialogs.
