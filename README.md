# AndroidProjectTestAgent

GitHub Actions "agent" workflow for Android projects that runs:

- Unit tests (`testDebugUnitTest`)
- UI/instrumentation tests (`connectedDebugAndroidTest`) on an emulator

## Usage

Copy or reuse `.github/workflows/android-test-agent.yml` in your Android project.

It runs automatically on:

- `push`
- `pull_request`
- Manual `workflow_dispatch`

### Requirements

- Android project with Gradle wrapper (`./gradlew`)
- Unit tests configured for the `testDebugUnitTest` task
- Instrumentation/UI tests configured for the `connectedDebugAndroidTest` task
