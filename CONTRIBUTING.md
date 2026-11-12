# Contributing to Nocturne Mobile

Thank you for contributing to Nocturne Mobile. Follow these guidelines to ensure an efficient development and review process.

## Proposing Contributions
- Substantial features, architectural changes, or major UI overhauls should be discussed before implementation. Open an issue on GitHub to outline your proposal.
- Straightforward bug fixes, performance improvements, documentation enhancements, or minor corrections can be submitted directly as a Pull Request.

## Branch and Pull Request Workflow
- New contributions should normally branch from `main` or the latest active development branch.
- When contributing:
  - Fork the repository and create a descriptive branch (e.g., `fix/sync-reconnect`, `feat/lyrics-timing`, `docs/build-guide`).
  - Keep pull requests focused on a single change or fix. Avoid mixing unrelated refactors, cosmetic tweaks, or mass reformatting with functional changes.
  - Keep your branch synchronized with the target base branch.
  - Provide a clear PR description explaining what was changed and why. Mention testing steps and verified environments (e.g., "Tested on Pixel 8 Android 14 and Nothing Phone (2)").
  - Address review feedback promptly and keep discussions focused on technical merits.

## Development Setup
The project requires the following tools:
- **JDK**: Java Development Kit 17 (Eclipse Temurin 17 or OpenJDK 17 recommended).
- **Android SDK**: `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`.
- **C/C++ NDK & CMake**: CMake 3.22.1+ and Android NDK (for native audio DSP and Oboe components configured under `app/src/main/cpp`).
- **Listen Together Backend (Optional)**: Go 1.22+ if developing or testing the local party server (`backend/`).

## Build and Test Commands
Run Gradle commands from the repository root:

### Build
- **macOS / Linux**:
  ```bash
  ./gradlew assembleDevDebug
  ```
- **Windows**:
  ```powershell
  .\gradlew.bat assembleDevDebug
  ```

### Run Tests
- **macOS / Linux**:
  ```bash
  ./gradlew testDevDebugUnitTest
  ```
- **Windows**:
  ```powershell
  .\gradlew.bat testDevDebugUnitTest
  ```

To run a specific test:
```bash
./gradlew testDevDebugUnitTest --tests "com.nocturne.music.playback.audio.DirectAudioStreamingRegressionTest"
```

## Architecture & Code Quality
- **Idiomatic Kotlin & Compose**: Follow modern Kotlin idioms and Jetpack Compose best practices.
- **Separation of Concerns**: Maintain clean boundaries between UI (`app`, `sharedUi`), playback and audio services (`app/src/main/java/com/nocturne/music/playback`), and networking / InnerTube resolver logic (`shared/src/jvmSharedMain/kotlin/com/nocturne/music/data`).
- **Nocturne Design Language**: Respect Nocturne's visual identity—including fluid liquid glass navigation folding, Nothing dot-matrix typography and icons, and responsive theme tokens.
- **Nocturne Sync Compatibility**: Ensure state changes (play/pause, queue modifications, seek offsets) stay properly serialized across the bidirectional desktop sync protocol.
- **Diff Hygiene**: Keep PR diffs minimal and purposeful. Avoid committing unused imports or formatting files that were not part of your change.

## Audio Changes and Telemetry
For audio engine modifications: application telemetry must not be presented as proof of physical hardware behavior unless that hardware behavior was actually verified.

Distinguish clearly between source stream format, decoder output, internal DSP format, AudioTrack output, AudioFlinger/HAL state, advertised device capability, and external physical endpoint behavior (e.g., Bluetooth sink vs. USB DAC).

## Review Process
All pull requests require review and approval by maintainers before merging. Maintainers review changes for correctness, architecture fit, performance impact, and maintainability.

## Dependencies and Licensing
Nocturne Mobile is licensed under the **GNU General Public License v3.0 (GPLv3)**.

All contributed code and dependencies must be strictly compatible with GPLv3. Avoid introducing heavy external libraries unless strictly necessary; any new dependency must be evaluated for necessity, binary size, and licensing compliance.
