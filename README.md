# Silent Heist — Android MVP v0.1.0

A native Android proof-of-concept for an ASMR stealth game where **sound is the core gameplay mechanic**.

## Included in this MVP
- Mission 01: **The Quiet Vault**
- Touch-controlled safe dial
- Three hidden lock points with alternating rotation directions
- Audio proximity feedback: clicks sharpen as the player gets closer
- Movement-speed-based **Noise Meter**
- Guard states: Undetected → Suspicious → Searching → Danger
- Alarm / fail state at 100% noise
- Haptic feedback near hidden lock positions
- Original generated ASMR sound effects created automatically during build
- Win state with vault opening and diamond reveal
- Restart flow
- Portrait layout designed for Android phones
- No external runtime/game engine dependency

## Controls
1. Put on headphones.
2. Follow the direction shown under the mission title.
3. Drag around the safe dial slowly.
4. Listen to the click pitch. The click gets sharper near the hidden number.
5. Hit the hidden point while moving in the correct direction.
6. Crack all three locks without filling the Noise Meter.

## Open and build
1. Install a recent Android Studio.
2. Open this folder as a project.
3. Let Gradle sync.
4. Connect an Android phone or create an emulator (Android 8.0+).
5. Run the `app` configuration.

Package: `com.silentheist.game`
Minimum Android: API 26 (Android 8.0)
Target / compile SDK: 35

## Recommended next milestone (v0.2)
- Real safe door opening animation
- Glass cutter mini-game
- Wire-cutting / security panel mini-game
- Guard footsteps and positional audio
- Mission scoring: Noise / Time / Precision
- Loot collection + Hideout room
- Procedural safe combinations
- Tutorial level
- Settings for haptics, audio and accessibility
- Analytics hooks for retention testing

## Commercial direction
The MVP intentionally proves one question first: **Is “play quietly and use sound to solve the interaction” satisfying enough to make players replay?**
If that core loop tests well, the project can scale into banks, museums, jewelry stores, armored trains and other heist missions.

### Build note
GitHub Actions generates the WAV sound assets from `tools/generate_audio.py`, installs Android SDK 35, builds the debug APK with Gradle 8.9, and publishes the APK as a workflow artifact.
