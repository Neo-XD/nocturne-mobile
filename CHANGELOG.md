# Changelog

All notable changes to Nocturne Mobile are documented in this file.

## [v0.2.1-m] - 2026-09-06

### ✨ New Features
- **Queue Reordering & Disk Persistence**: Interactive drag handles are now unlocked and visible by default. Reordered queue sequences automatically persist to disk across timeline changes, app swipes, and task termination.
- **Dynamic Album Art Warping Background Theme**: Added a dynamic blurred liquid-mesh background theme with pulsing breath scale, swaying rotation, wave distortion, and boosted saturation adapting to the currently playing song's artwork.
- **Convx-Style Player Theme Customizer**: Added a granular theme customizer with real-time interactive preview and sliders for blur radius, overlay dim opacity, saturation, motion speed, and specular glass border toggles.
- **Nocturne Frosted Glass (Acrylic Styling)**: Implemented high-performance acrylic frosted glass modifiers and container cards with specular highlight edge reflections across navigation pills, search buttons, and player cards.
- **Dedicated Monochrome Theme**: Added a one-click minimal achromatic grayscale and AMOLED pure black theme option.
- **Quick Access Nocturne Sync**: Added direct Nocturne Sync entry to the top popup menu for fast pairing with Nocturne Desktop.

### ⚡ Improvements
- **Categorized Settings Screen**: Reorganized all settings into clean, structured sections (Appearance & Playback, Connectivity & Sync, Content & Features, Storage & Privacy, Updates & About).
- **Robust Queue Deserialization**: Added MediaMetadata fallback so media items without full tags maintain metadata integrity when restoring playback from disk.
- **Floating Navigation Styling**: Translucent acrylic styling with specular border highlights applied to floating navigation pills.
- **Theme Color Schemes**: Fully integrated `NocturneTheme` composable with dynamic `materialKolor` monochrome palette generator.

### 🐛 Bug Fixes
- Fixed queue reordering lost when swiping away the app or closing playback.
- Fixed restored media items occasionally dropping artist or album information on disk reload.
- Cleaned up legacy Vivimusic email and external URL links across dialogs and menus.

### ⚠️ Known Bugs
- Remote playback sync on Android is in beta and may experience latency when recovering from background suspension.

---

## [v0.2m] - 2026-09-03

### ✨ New Features
- **The Nocturne Rebrand**: Complete transition to Nocturne Music with new brand identity, UI, and `com.nocturne.music` package namespace.
- **Zero-PIN Remote Device Sync**: Automated UDP discovery and WebSocket pairing with Nocturne Desktop PC.
- **Full Remote Controller Mode**: Route song clicks, queue dispatches, play/pause, seek, and volume directly to Desktop PC.
- **Bidirectional Playback Handoff**: 1-tap seamless playback session transfer between Phone and Desktop with live seek position.
- **Dynamic Ambient Themes**: Real-time dynamic Gradient, Blur, Apple Music fluid blur, and Live Mesh backgrounds.

### ⚡ Improvements
- Live Now Playing & high-resolution album artwork synchronized across full player, mini player, and carousel.
- Flexible release build configuration with automatic fallback to debug certificate for local builds.

### 🐛 Bug Fixes
- Settings branding cleanup and full removal of legacy identifiers.
- Database migration schemas aligned with `com.nocturne.music.db.InternalDatabase`.

### ⚠️ Known Bugs
- Remote playback sync on Android is in beta and may experience latency when recovering from background suspension.
