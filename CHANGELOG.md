# Changelog

## v0.9.4

- Fix CI Android SDK setup by selecting the tested command-line tools version and installing platform-tools without the retired `tools` package.
- Includes all Android stability fixes from v0.9.3 below. Backend compatibility remains v0.1.0.

## v0.9.3

Compatible with `doodleMaster-backend` tag `v0.1.0`.

- Preserve event order for strokes, undo/clear, chat, and turn transitions; retain repeated chat messages.
- Preserve the drawer's canvas across activity rotation and reset it at each turn.
- Cancel queued reconnects when leaving or changing rooms; ignore stale socket callbacks and stop retrying unavailable rooms.
- Encode WebSocket query parameters correctly, including usernames containing spaces, ampersands, or emoji.
- Check room admission before navigation and show joining progress, cancellation, and errors.
- Read identity and username reliably before joining; create player IDs atomically and reject blank profile names.
- Render game-over scores without requiring a last turn-end message and retain names for disconnected participants.
- Disable starting with fewer than two players, and prevent drawing or guessing outside an active connected turn.
- Fix guess text overlapping its word-mask hint.
- Protect JSON models from release shrinking and restrict cleartext connections to debug builds.
- Default debug builds to the Android emulator's host address; override with `-PserverHost=HOST:PORT` for another development server.
- Add unit tests, tests against the real Go backend, and CI checks before publishing a tagged APK.

Validation: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease -PbackendUrl=http://localhost:3001/` with backend `v0.1.0` running on port 3001. Release APKs are unsigned locally unless the signing environment variables are provided; tagged releases use the existing GitHub signing configuration.
