# Sliide User Manager KMP 

A production-grade **Kotlin Multiplatform** User Management System targeting Android & iOS, built with 100% shared Compose Multiplatform UI.

---

## Getting Started

1. **Prerequisites** — Ensure you have **Java 21** installed (required by AGP 8.13).
2. **Open Project** — Open the root folder in **Android Studio Meerkat** (2024.3+) or IntelliJ with the KMP plugin.
3. **Run Android** — Run `./gradlew :androidApp:installDebug` or use the IDE Run configuration.
4. **Run iOS** — Open `iosApp/iosApp.xcodeproj` in Xcode and press **Run**. 
   * *Note: The build script handles Kotlin compilation and framework linking automatically.*

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
│  data/        → DummyJsonApi (Ktor) + SQLDelight    │
│  di/          → Koin modules (explicit KMP setup)   │
└────────────────────────────────────────────────────┘
       ↑                                  ↑
 androidApp/                           iosApp/
 (MainActivity,                    (ComposeUIViewController,
  DatabaseFactory.android)          DatabaseFactory.ios)
```

The domain layer is completely framework-free. The ViewModel depends only on use cases. The repository interface lives in domain; the implementation in data. 

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

`UserRepositoryImpl` uses SQLDelight's `asFlow()` + `mapToList()` to expose a reactive stream from the local DB. The UI always observes the cache; `syncUsers()` fetches the remote last-page and upserts, triggering the Flow automatically.

**Mock Backend Strategy:**
The app uses `dummyjson.com`. While `POST` and `DELETE` requests return success, the server is stateless and does not persist changes. Local changes are managed in the SQLite cache to provide a consistent user experience.

### Optimistic Delete with Undo

The ViewModel implements a 4-second undo window using reactive state:

1. User long-presses → confirmation dialog.
2. On confirm: item is reactively filtered from the UI via a `pendingDeleteUser` StateFlow.
3. A `DeleteSnackbar` effect fires → Snackbar with "Undo" appears.
4. If Undo is tapped: The pending state is cleared, and the user instantly reappears.
5. If no Undo: After the delay, the remote `DELETE` is committed.

### Adaptive Layout

Uses `BoxWithConstraints` to detect available width at runtime:
- **< 600dp** → `LazyColumn` (single list, portrait)
- **≥ 600dp** → `LazyVerticalGrid(columns = Fixed(2))` (landscape / tablet)

---

## Tech Stack

| Concern | Library |
|---|---|
| Networking | Ktor 3.x (OkHttp/Darwin engines) |
| Local cache | SQLDelight 2.x |
| DI | Koin 4.x (Manual factory definitions for iOS stability) |
| ViewModel | `androidx.lifecycle:lifecycle-viewmodel` (KMP version) |
| UI | Compose Multiplatform 1.7.x / Material 3 |
| Date/Time | `kotlinx-datetime` |
| Toolchain | Foojay Resolver (Auto-provisioning JDK 21) |

---

## Testing

```bash
./gradlew :shared:allTests
```

Test coverage includes:
- `UserListViewModelTest` — all intents, optimistic delete, undo, error handling.
- `RelativeTimeTest` — timestamp formatting and validation logic.

---

## Project Structure

```
.
├── gradle/libs.versions.toml          # Version catalog
├── shared/
│   ├── build.gradle.kts               # Dynamic framework & sqlite3 linking
│   ├── src/
│   │   ├── commonMain/
│   │   │   ├── kotlin/com/sliide/usermanager/
│   │   │   │   ├── di/AppModule.kt    # Unified DI config
│   │   │   │   ├── domain/            # Models & Use Cases
│   │   │   │   ├── data/              # Repository & DummyJSON API
│   │   │   │   ├── presentation/      # MVI ViewModel & State
│   │   │   │   └── ui/                # Shared Compose UI
│   │   ├── androidMain/               # Android SQLite setup
│   │   └── iosMain/                   # iOS UIViewController & SQLite setup
└── androidApp/                        # Android entry point
```
