# 📋 Agile User Stories & Roadmap (kvolegi arcade)

This document tracks all user stories, epics, and acceptance gates across desktop and Android APK arcade builds.

---

## 🎮 Epic 1: Mobile Touch UX & Accessibility

### US-A101: Touch Target Scaling for Mobile Displays
- **As an** Android player,
- **I want** all game buttons, toggles, and modal controls to have minimum touch targets of 56–72px,
- **So that** I can comfortably play and spin reels with one thumb on modern smartphone screens.
- **Story Points:** 5 SP
- **Status:** `DONE` (Verified in v2.0.6)
- **Acceptance Criteria:**
  - [x] In Slots, the SPIN button height is enlarged to 132px.
  - [x] In Slots, `-10` / `+10` and `Set Bet` buttons are scaled up to 66–72px.
  - [x] In Snake Settings, chip heights and start buttons are scaled to 52–58px (~1.5x scaling).
  - [x] The top `:(` button is expanded to 110x56px for easy tapping.

### US-A102: Unified Rainbow Neon Synchronization
- **As an** arcade enthusiast,
- **I want** on-screen borders, buttons, and titles to cycle through rainbow neon hues in unified synchrony,
- **So that** the interface feels alive and polished rather than flashing disjointed random colors.
- **Story Points:** 3 SP
- **Status:** `DONE` (Verified in v2.0.6)
- **Acceptance Criteria:**
  - [x] Sound OFF button in settings cycles rainbow glow synchronously with Sound ON.
  - [x] Snake head and field outline cycle dynamic rainbow colors.
  - [x] Slots reel border and action buttons cycle rainbow glow.
  - [x] The `:(` money modal smoothly shifts through a single unified color sequence.

---

## 🎰 Epic 2: Multi-Platform Engine & Graphics Port

### US-B201: Hexagonal Portability Parity
- **As a** cross-platform gamer,
- **I want** identical game math and odds on both macOS/Windows desktop and Android,
- **So that** my high scores and gameplay feel consistent on any device.
- **Story Points:** 8 SP
- **Status:** `DONE` (Verified in v2.0.0)
- **Acceptance Criteria:**
  - [x] 75 unit tests verify engine math independently of platform renderers.
  - [x] Automated build script produces `.app`, `.jar`, `.exe`, and signed `.apk` in a single run.
