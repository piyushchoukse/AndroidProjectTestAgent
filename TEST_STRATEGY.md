# Android Test Engineer Agent — Complete Test Strategy

## 1. Project Testing Context (PTC)

| Attribute           | Value                                              |
|---------------------|----------------------------------------------------|
| **Module**          | `:app` (single-module Android application)         |
| **Language**        | Kotlin 1.9                                         |
| **Architecture**    | Clean Architecture · MVVM presentation layer       |
| **DI**              | Hilt 2.51                                          |
| **Async**           | Kotlin Coroutines + `StateFlow`                    |
| **Network**         | Retrofit 2 + OkHttp 4                              |
| **Unit test stack** | JUnit 5 (Jupiter) · MockK · Coroutines Test · Turbine |
| **Coverage tool**   | Jacoco 0.8.11                                      |
| **Robolectric**     | Available for Hilt-instrumented edge cases only    |

### Layer Overview

```
┌─────────────────────────────────────┐
│  Presentation (ViewModel, UiState)  │  ← StateFlow, no Android types in API
├─────────────────────────────────────┤
│  Domain (UseCases)                  │  ← Pure Kotlin, orchestration & validation
├─────────────────────────────────────┤
│  Data (Repository, Remote DTO)      │  ← API + in-memory cache abstraction
└─────────────────────────────────────┘
```

---

## 2. Complexity & Risk Hotspots

| Class / File              | Cyclomatic Complexity | Risk Level | Notes                                                                 |
|---------------------------|-----------------------|------------|-----------------------------------------------------------------------|
| `LoginUseCase`            | Medium (5 paths)      | 🔴 HIGH    | Dual validation (email + password) + delegation; bugs affect all auth |
| `LoginViewModel`          | Medium (double-submit guard, 2 results) | 🔴 HIGH | State machine correctness critical for UX |
| `UserRepositoryImpl`      | Medium (cache-first reads) | 🟡 MED  | Cache invalidation bugs silent; network fallback tricky               |
| `GetUsersUseCase`         | Low-Medium (null guard + role filter) | 🟡 MED | Role-based visibility is a security boundary |
| `Result` sealed class     | Low (2 branches)      | 🟢 LOW     | Foundation type; regressions propagate everywhere                     |
| `User` data class         | Low (init validates)  | 🟢 LOW     | Invariants enforced at construction; edge: blank token                |
| `GetUserUseCase`          | Low (id guard + cache check) | 🟢 LOW | Cache-first read; straightforward                               |

### Key Risk Themes

1. **Auth validation logic** — email regex + password policy in `LoginUseCase` with many edge cases.
2. **State machine correctness** — `LoginViewModel` must guard against double-submit and emit exactly the right sequence of states.
3. **Cache-network duality** — `UserRepositoryImpl` must prefer cache; bugs lead to stale data or extra network calls.
4. **Role-based data leakage** — `GetUsersUseCase` filtering must ensure non-admins cannot access other users' data.
5. **Coroutine cancellation** — `viewModelScope` coroutines are cancelled when the ViewModel is cleared; tests must cover this via `advanceUntilIdle` + `TestDispatcher`.

---

## 3. Proposed Test Strategy

### Test Pyramid

```
           /\
          /  \          E2E / UI Tests (Espresso + Hilt)
         /----\         ← NOT in scope for this PR; add separately
        /      \
       /--------\       Integration Tests (Repository + fake server with MockWebServer)
      /          \      ← Covered by manual test plan; add as follow-up
     /------------\
    /              \    Unit Tests  ← PRIMARY FOCUS (this PR)
   /________________\
```

### Priority Order

| Priority | Layer          | Rationale                                       |
|----------|----------------|-------------------------------------------------|
| P0       | `LoginUseCase` | All auth flows depend on this                   |
| P0       | `LoginViewModel` | Most user-facing state machine                |
| P1       | `UserRepositoryImpl` | Cache correctness + network delegation    |
| P1       | `GetUsersUseCase` | Security boundary (role filtering)           |
| P2       | `GetUserUseCase` | Simpler; cache-first read                     |
| P2       | `Result` / `User` | Foundation types; regressions ripple up      |

### Test Patterns Used

| Pattern                  | Applied In                                            |
|--------------------------|-------------------------------------------------------|
| **AAA** (Arrange-Act-Assert) | All tests                                         |
| **Given-When-Then** (nested `inner class`) | Presentation & UseCase tests        |
| **Test Data Builder**    | `TestFixtures` object in `util/`                      |
| **Fake over Mock**       | `UserRepository` → MockK; `ApiService` → MockK (no real network) |
| **Coroutine Test**       | `runTest` + `StandardTestDispatcher` via `MainDispatcherExtension` |
| **Flow testing (Turbine)** | `LoginViewModelTest`, `UserListViewModelTest`       |
| **Parameterized Tests**  | Email/password validation in `LoginUseCaseTest`       |

---

## 4. Test Cases List (with Intent)

### `UserTest` (14 cases)
| # | Intent |
|---|--------|
| 1 | Valid construction with defaults |
| 2 | `isAuthenticated` false when token null |
| 3 | `isAuthenticated` true when token non-blank |
| 4 | `isAuthenticated` false when token is blank string |
| 5 | `canonicalEmail` lowercases and trims |
| 6 | ADMIN role stored correctly |
| 7 | Minimum id = 1 accepted |
| 8 | id = 0 throws |
| 9 | Negative id throws |
| 10 | Invalid email formats throw (parameterized) |
| 11 | Blank name throws |
| 12 | Empty name throws |
| 13 | Equal users with same fields |
| 14 | Copy with different token changes `isAuthenticated` |

### `ResultTest` (11 cases)
| # | Intent |
|---|--------|
| 1 | `Success.isSuccess` = true |
| 2 | `Success.isError` = false |
| 3 | `Success.getOrNull()` returns data |
| 4 | `Success.errorOrNull()` = null |
| 5 | `Error.isSuccess` = false |
| 6 | `Error.isError` = true |
| 7 | `Error.getOrNull()` = null |
| 8 | `Error.errorOrNull()` returns exception |
| 9 | `Error.message` defaults to exception message |
| 10 | `runCatching` wraps success |
| 11 | `runCatching` wraps exception; does NOT catch `Error` subclasses |

### `UserRepositoryImplTest` (15 cases)
| # | Intent |
|---|--------|
| 1 | `login` success returns authenticated User |
| 2 | `login` caches user after success |
| 3 | `login` network error returns `Error` |
| 4 | `login` fails when `getUser` throws after login |
| 5 | `getUser` cache hit skips API |
| 6 | `getUser` cache miss fetches from API and caches |
| 7 | `getUser` API error returned as `Error` |
| 8 | `getUsers` returns list |
| 9 | `getUsers` returns empty list |
| 10 | `getUsers` API failure |
| 11 | `getCachedUser` returns null before caching |
| 12 | `cacheUser` → `getCachedUser` round-trip |
| 13 | `clearCache` empties all entries |
| 14 | Caching same id overwrites |
| 15 | `getCachedUser` after `clearCache` returns null |

### `LoginUseCaseTest` (13 cases)
| # | Intent |
|---|--------|
| 1 | Valid credentials → `Success` |
| 2 | Trims email before delegation |
| 3 | Blank email → `Error`, repo not called |
| 4–8 | Invalid email formats → `Error` (parameterized) |
| 9 | Valid email formats → repo called (parameterized) |
| 10 | Short password → `Error` |
| 11 | No digit in password → `Error` |
| 12 | Empty password → `Error` |
| 13 | Valid passwords proceed → repo called |
| 14 | Repository error propagated unchanged |

### `GetUserUseCaseTest` (5 cases)
| # | Intent |
|---|--------|
| 1 | id = 0 → `Error`, no repo calls |
| 2 | Negative id → `Error` |
| 3 | Cache hit → returns user, no `getUser` call |
| 4 | Cache miss → falls back to `getUser` |
| 5 | `getUser` network error propagated |

### `GetUsersUseCaseTest` (5 cases)
| # | Intent |
|---|--------|
| 1 | Null requestingUser → `Error` |
| 2 | ADMIN sees full list |
| 3 | Regular USER sees only self |
| 4 | Regular USER not in list → empty result |
| 5 | Repository error propagated |

### `LoginViewModelTest` (7 cases)
| # | Intent |
|---|--------|
| 1 | Initial state is idle |
| 2 | State transitions: idle → loading → success |
| 3 | Delegates exact email/password to use case |
| 4 | State transitions: idle → loading → error |
| 5 | Double-submit guard (second call ignored) |
| 6 | `clearError()` removes errorMessage |
| 7 | Error state has no user |

### `UserListViewModelTest` (6 cases)
| # | Intent |
|---|--------|
| 1 | Initial state |
| 2 | Success: loading → list of users |
| 3 | Error: loading → error message |
| 4 | Empty list allowed |
| 5 | Null user → error state |
| 6 | `clearError()` works |

---

## 5. Generated Test Code

Test code lives in:

```
app/src/test/java/com/example/androidtestagent/
├── util/
│   ├── TestFixtures.kt           ← Test Data Builder (shared fixtures)
│   └── MainDispatcherExtension.kt ← JUnit 5 extension for coroutine dispatch
├── data/
│   ├── model/
│   │   ├── UserTest.kt
│   │   └── ResultTest.kt
│   └── repository/
│       └── UserRepositoryImplTest.kt
├── domain/usecase/
│   ├── LoginUseCaseTest.kt
│   ├── GetUserUseCaseTest.kt
│   └── GetUsersUseCaseTest.kt
└── presentation/viewmodel/
    ├── LoginViewModelTest.kt
    └── UserListViewModelTest.kt
```

### Key Decisions

- **No real network** — `ApiService` is mocked with MockK; `coEvery` controls responses.
- **No Robolectric** — `ViewModel` is tested without an Android environment. `viewModelScope` is driven by `MainDispatcherExtension`.
- **Turbine** — asserts on `StateFlow` emission sequences deterministically.
- **`@ParameterizedTest`** — email/password validation coverage without boilerplate.

---

## 6. Verification Checklist

### Anti-Pattern Checks
- [x] **No `Thread.sleep()`** — all timing controlled by `TestDispatcher`
- [x] **No real network calls** — `ApiService` fully mocked
- [x] **No real DB** — in-memory map used; no Room involved
- [x] **No `Random`** — all test data is deterministic via `TestFixtures`
- [x] **No `System.currentTimeMillis()`** — not used in production code under test
- [x] **No `GlobalScope`** — ViewModels use `viewModelScope` only
- [x] **Getters/setters NOT tested** — all tests target behaviour and contracts

### Flakiness Checks
- [x] `StandardTestDispatcher` ensures coroutines only advance when `advanceUntilIdle()` is called
- [x] Turbine's `cancelAndIgnoreRemainingEvents()` prevents hanging on unconsumed events
- [x] `@BeforeEach` resets `viewModel` and `mockk()` instances per test
- [x] MockK `coEvery` stubs are always set before `// When` step

### Coverage Goals (target after running Jacoco)
| Layer                  | Target Line Coverage |
|------------------------|----------------------|
| Domain UseCases        | ≥ 95%                |
| Data Repository        | ≥ 90%                |
| Presentation ViewModel | ≥ 90%                |
| Data Models            | ≥ 85%                |

---

## 7. How to Run & How to Generate Reports

### Prerequisites

```bash
# JDK 17+ required
java -version

# Android SDK required (for kapt/Hilt compilation)
echo $ANDROID_HOME
```

### Run All Unit Tests

```bash
# All unit tests
./gradlew :app:testDebugUnitTest

# Single test class
./gradlew :app:testDebugUnitTest --tests "com.example.androidtestagent.domain.usecase.LoginUseCaseTest"

# Single test method
./gradlew :app:testDebugUnitTest \
  --tests "com.example.androidtestagent.presentation.viewmodel.LoginViewModelTest.transitions idle*"
```

### Generate Jacoco Coverage Report

```bash
# Runs tests + generates HTML + XML report
./gradlew :app:jacocoTestReport

# Report locations:
#   HTML → app/build/reports/jacoco/jacocoTestReport/html/index.html
#   XML  → app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
```

### View Test Results (HTML)

```
app/build/reports/tests/testDebugUnitTest/index.html
```

Open this in a browser to see a full breakdown per package, class, and method.

### CI Integration (GitHub Actions example)

```yaml
- name: Run unit tests
  run: ./gradlew :app:testDebugUnitTest

- name: Generate coverage report
  run: ./gradlew :app:jacocoTestReport

- name: Upload coverage to Codecov
  uses: codecov/codecov-action@v4
  with:
    files: app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml

- name: Upload test results
  uses: actions/upload-artifact@v4
  if: always()
  with:
    name: test-results
    path: app/build/reports/tests/
```

### Enforce Coverage Gate (add to `app/build.gradle.kts`)

```kotlin
tasks.register<JacocoCoverageVerification>("jacocoCoverageVerification") {
    dependsOn("jacocoTestReport")
    violationRules {
        rule {
            limit {
                minimum = "0.85".toBigDecimal()   // 85% line coverage minimum
            }
        }
    }
}
```

```bash
./gradlew :app:jacocoCoverageVerification
```

---

*Generated by the Android Test Engineer Agent. All tests are deterministic, isolated, and follow the AAA / Given-When-Then patterns documented above.*
