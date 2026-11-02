# ADR 0003: Multi-Platform Fat-JAR Distribution with Bundled Native Graphics

## Status
Accepted

## Context
JavaFX requires native operating system binaries (`.dylib` on macOS, `.so` on Linux, `.dll` on Windows). Standard Maven/Gradle builds compile a JAR that only includes natives for the current build host.
When a user on Windows or Linux runs a JAR compiled on macOS, the JVM crashes with `UnsatisfiedLinkError: no prism_es2 in java.library.path`.

Distributing 4 separate JARs creates user confusion, fractured download links, and increased release friction.

## Decision Drivers
1. **Zero-Configuration User Experience**: One single file (`kvolegi-arcade-universal.jar`) must run out-of-the-box on macOS (Intel + Apple Silicon), Windows (x64), and Linux (x64) using a simple `java -jar`.
2. **Deterministic Native Extraction**: The JavaFX runtime must automatically load the correct architecture binary at runtime.

## Considered Options
1. **OS-Specific Installers (MSI, DEB, DMG)**: High packaging overhead and platform-specific code-signing requirements.
2. **Host-Specific JARs**: Requires user to select their exact CPU architecture and OS.
3. **Universal Multi-Platform Fat-JAR (Chosen)**:
   - Extract and bundle the JavaFX native shared libraries for `mac-aarch64`, `mac-x64`, `win-x64`, and `linux-x64` into `kvolegi-arcade-universal.jar`.
   - Provide a dynamic `Launcher.java` entry point that loads the JavaFX application class after runtime initialization.

## Consequences
- **Positive**:
  - One download link works on any desktop computer with Java 17+.
  - Simplified CI/CD: The build pipeline generates one canonical cross-platform binary artifact.
- **Negative / Mitigations**:
  - JAR artifact size increases to ~20MB because it bundles multiple OS natives.
  - *Mitigation*: 20MB is considered negligible for a desktop application containing 4 full games, fonts, and graphical assets.
