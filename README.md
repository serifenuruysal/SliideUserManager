# Sliide User Manager KMP 

A production-grade **Kotlin Multiplatform** User Management System targeting Android & iOS, built with 100% shared Compose Multiplatform UI.

---

## Getting Started

1. **Get a GoRest token** — free at [gorest.co.in](https://gorest.co.in) (sign in with Google)
2. Add it to your local `gradle.properties`:
   ```
   goRestToken=your_token_here
   ```
3. Open in Android Studio Meerkat (2024.3+) or IntelliJ with the KMP plugin
4. Run `./gradlew :androidApp:installDebug` or open `iosApp/iosApp.xcodeproj` in Xcode

---

## Architecture

### Clean Architecture with MVI

```
┌────────────────────────────────────────────────────┐
│                   shared module                     │
│                                                     │
│  ui/          → Compose Multiplatform screens       │
│  presentation/→ ViewModel (MVI: State/Intent/Effect)│
│  domain/      → Use cases + Repository interface    │
│  data/        → GoRestApi (Ktor) + SQLDelight cache │
│  di/          → Koin modules                        │
└────────────────────────────────────────────────────┘
       ↑                                  ↑
 androidApp/                           iosApp/
 (MainActivity,                    (ComposeUIViewController,
  DatabaseFactory.android)          DatabaseFactory.ios)
```

The domain layer is completely framework-free. The ViewModel depends only on use cases. The repository interface lives in domain; the implementation in data. This makes the entire shared logic unit-testable without a device.

### MVI Flow

```
UI → Intent → ViewModel → State (StateFlow)
                       ↘ Effect (SharedFlow, one-shot)
```

- **State** — everything the UI needs to render; held in `StateFlow`
- **Intent** — sealed interface describing every possible user action
- **Effect** — fire-and-forget side effects (snackbar, scroll-to-top) via `SharedFlow`

---

## Key Feature Implementation Notes

### Offline-First with SQLDelight

`UserRepositoryImpl` uses SQLDelight's `asFlow()` + `mapToList()` to expose a reactive stream from the local DB. The UI always observes the cache; `syncUsers()` fetches the remote last-page and upserts, triggering the Flow automatically. GoRest doesn't return `created_at` for users, so we store `System.now()` on first insert.

**Last-page fetch strategy:**
```
GET /users?page=1 → read X-Pagination-Pages header → GET /users?page=<last>
```

### Optimistic Delete with Undo

The ViewModel implements a 4-second undo window:

1. User long-presses → confirmation dialog
2. On confirm: item is instantly removed from `state.users` and stored as `pendingDelete`
3. A `DeleteSnackbar` effect fires → Snackbar with "Undo" appears
4. If Undo is tapped: `deleteJob` is cancelled, user is re-inserted into list
5. If no Undo: `deleteJob` completes → remote DELETE is committed

The remote API is only called *after* the undo window expires, so UX is instant and network latency is hidden entirely.

### Adaptive Layout

Uses `BoxWithConstraints` to detect available width at runtime:
- **< 600dp** → `LazyColumn` (single list, portrait)
- **≥ 600dp** → `LazyVerticalGrid(columns = Fixed(2))` (landscape / tablet)

No `WindowSizeClass` dependency needed; works identically on both platforms.

### Shimmer Loading

Custom shimmer uses `rememberInfiniteTransition` + `Brush.linearGradient` with an animated X-offset. Completely self-contained in `ShimmerEffect.kt`, no third-party library required.

### Real-Time Validation

Validation logic lives in `shared/.../ui/util/Utils.kt` — pure Kotlin, tested independently:
- `isValidEmail()` — RFC-compatible regex
- `isValidName()` — min 2 non-whitespace chars

`OutlinedTextField` shows `isError` + `supportingText` reactively as the user types. Submit button is gated on both fields being valid.

---

## Tech Stack

| Concern | Library |
|---|---|
| Networking | Ktor 3.x (OkHttp/Darwin engines) |
| Local cache | SQLDelight 2.x |
| DI | Koin 4.x |
| ViewModel | `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel` (KMP) |
| UI | Compose Multiplatform 1.7.x / Material 3 |
| Date/Time | `kotlinx-datetime` |
| Testing | kotlin.test + Turbine + coroutines-test |

---

## Testing

```bash
./gradlew :shared:allTests
```

Test coverage includes:
- `UserListViewModelTest` — all intents, optimistic delete, undo, error handling, effect emission (Turbine)
- `RelativeTimeTest` — all timestamp bands + validation functions

Tests use a fake `UserRepository` backed by a `MutableStateFlow`, a `StandardTestDispatcher`, and Turbine for Effect stream assertions. No mocking frameworks; no Android dependencies.

---

## How AI Was Used

This project was built using Claude as an AI pair-programmer. Here's how the collaboration was directed:

| Task | AI contribution | Human curation |
|---|---|---|
| Architecture skeleton | Generated layer structure and interfaces | Enforced strict separation (no domain→data leaks) |
| MVI contract | Drafted State/Intent/Effect | Refined Effect to SharedFlow with buffer capacity |
| Optimistic delete | Drafted basic delete flow | Redesigned to use `Job` cancellation for undo window |
| Shimmer | Generated animated brush | Tuned alpha values for dark/light parity |
| Unit tests | Generated test cases for happy paths | Added edge cases: negative time, network failure restore |
| Regex | Generated email validation regex | Tested against RFC edge cases |

The AI accelerated boilerplate significantly (DTO mapping, DI wiring, SQLDelight queries). All architectural decisions — offline-first strategy, undo window approach, adaptive layout mechanism — were directed and reviewed by the engineer.

---

## Project Structure

```
.
├── gradle/libs.versions.toml          # Version catalog
├── shared/
│   ├── build.gradle.kts
│   ├── src/
│   │   ├── commonMain/
│   │   │   ├── kotlin/com/sliide/usermanager/
│   │   │   │   ├── di/AppModule.kt
│   │   │   │   ├── domain/
│   │   │   │   │   ├── model/User.kt
│   │   │   │   │   ├── repository/UserRepository.kt
│   │   │   │   │   └── usecase/UserUseCases.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── remote/GoRestApi.kt + dto/UserDto.kt
│   │   │   │   │   ├── local/DatabaseFactory.kt (expect)
│   │   │   │   │   └── repository/UserRepositoryImpl.kt
│   │   │   │   ├── presentation/
│   │   │   │   │   ├── UserListContract.kt  (State/Intent/Effect)
│   │   │   │   │   └── UserListViewModel.kt
│   │   │   │   └── ui/
│   │   │   │       ├── UserListScreen.kt
│   │   │   │       ├── AddUserSheet.kt
│   │   │   │       ├── components/ (UserCard, Shimmer, DeleteDialog)
│   │   │   │       ├── theme/SliideTheme.kt
│   │   │   │       └── util/Utils.kt
│   │   │   └── sqldelight/.../User.sq
│   │   ├── androidMain/  → DatabaseFactory.android.kt
│   │   ├── iosMain/      → DatabaseFactory.ios.kt
│   │   └── commonTest/   → ViewModelTest, RelativeTimeTest
└── androidApp/
    └── MainActivity.kt
```
# SliideUserManager
