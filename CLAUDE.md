# CLAUDE.md

Guidance for Claude Code when working in this repo. **Read [ARCHITECTURE.md](ARCHITECTURE.md) before adding features** — it explains the offline-first sync model and where each piece lives.

## Project Context

Android client for the life-log diary system. Companion projects under `../`:

- `../life-log-api/` — Fastify + TypeORM + SQLite backend
- `../life-log-web/` — React + Vite web frontend

The Android app is **offline-first and write-only (MVP)**: every entry is queued locally and synced via WorkManager when the network returns. Reading old entries is done on the web for now.

## Stack

- **Kotlin** 2.0.21, **AGP** 8.12.3, **JVM target** 11, **KSP** for Room codegen
- **Jetpack Compose** (BOM 2024.09.00) + Material 3 + Navigation Compose
- **Room** 2.7.0 for local queue
- **WorkManager** 2.10.0 for background sync
- **Ktor Client** 3.0.3 (OkHttp engine) + **kotlinx.serialization** for HTTP/JSON
- **DataStore (Preferences)** for settings
- **Coil 3** for image loading
- `minSdk` 26, `compileSdk`/`targetSdk` 36
- `applicationId` / `namespace`: `com.rainbowcockroach.lifelog`
- Single module: `:app`. Dependency versions live in `gradle/libs.versions.toml`.

## Build & Run

```bash
./gradlew :app:assembleDebug         # debug build
./gradlew :app:installDebug          # install on device/emulator
./gradlew :app:testDebugUnitTest     # JVM unit tests
./gradlew :app:connectedDebugAndroidTest  # instrumented tests
./gradlew :app:lintDebug             # lint
```

`local.properties` holds the SDK path; never commit it.

## Configuring the app (first run)

The app launches into the Editor. Tap the gear icon → **Settings**:

- **API base URL** — your server origin, no trailing slash.
  - Emulator → `http://10.0.2.2:3000`
  - Real device on same Wi-Fi as dev box → `http://<your-LAN-ip>:3000`
  - Production → `https://your-real-api.example.com`
  - If your server is mounted under `/api`, include it: `https://example.com/api`
- **API key** — same key as `API_KEY` in `life-log-api/.env`.

Cleartext HTTP only works for `10.0.2.2`, `localhost`, `127.0.0.1` (see `res/xml/network_security_config.xml`). Everything else must be HTTPS.

## Repo map

```
app/src/main/java/com/rainbowcockroach/lifelog/
├── LifeLogApp.kt              # Application — builds AppContainer, kicks off TagSyncWorker
├── MainActivity.kt            # Compose entry + NavHost (editor ⇄ settings)
├── di/AppContainer.kt         # Manual DI (no Hilt)
├── data/
│   ├── EntryRepository.kt     # enqueue() + syncOne()
│   ├── TagRepository.kt       # local tag search + refresh-from-server + online-only create
│   ├── local/
│   │   ├── AppDatabase.kt     # v2, with MIGRATION_1_2
│   │   ├── PendingEntry.kt    # offline entry queue (now carries locationId + tagIdsJson)
│   │   ├── PendingEntryDao.kt
│   │   ├── CachedTag.kt       # local mirror of server tags + locations
│   │   ├── CachedTagDao.kt    # ranked search (exact > prefix > contains, then lastUsed)
│   │   └── SettingsStore.kt   # DataStore: base URL + API key + lastUsedLocationId
│   └── remote/
│       ├── ApiClient.kt       # Ktor — entries, media, tags (GET /tags, POST /tags)
│       └── Dto.kt             # Serializable request/response shapes
├── sync/
│   ├── SyncWorker.kt          # CoroutineWorker — drains the entry queue
│   ├── TagSyncWorker.kt       # CoroutineWorker — refreshes the tag cache
│   └── SyncScheduler.kt       # enqueueUniqueWork helpers (entries + tags)
├── ui/
│   ├── editor/                # EditorScreen + EditorViewModel + TagPickerSheet
│   └── settings/              # SettingsScreen + SettingsViewModel
└── util/ImageStorage.kt       # Copy + downscale picked images into filesDir
```

## Key rules (read before changing things)

- **The id IS the local timestamp at save time.** `EntryRepository.enqueue` stamps `id = System.currentTimeMillis()` and that value is both the Room PK and the server entry id (sent in `CreateEntryRequest.id`). This keeps the server-side list (sorted by `id DESC`) in the order the user actually wrote things, even when entries sync hours or days later. See ARCHITECTURE.md → "Why we use the local timestamp as the id".
- **All HTTP goes through `ApiClient`.** It reads the base URL and API key from `SettingsStore` on every call — never cache them.
- **Saving an entry must not require network.** `EntryRepository.enqueue` writes to Room and returns; only `SyncScheduler.schedule` touches WorkManager (and WorkManager itself waits for network).
- **Don't add Hilt yet.** Use `AppContainer`; it's a few lines and covers the whole app. Revisit if the graph grows past ~10 singletons.
- **No markdown editor library.** The editor is a `BasicTextField`/`OutlinedTextField` plus toolbar buttons that splice raw markdown (`![image](pending://<uuid>.jpg)`, `[🔗](url)`). The `pending://` token is rewritten to the server filename by `SyncWorker` — the server only ever sees the web-compatible `![image](filename.jpg)` form. Matches the web behavior in `life-log-web/src/page-editor/MarkdownEditor.tsx`.
- **No markdown renderer yet** — MVP is write-only. If/when a viewer is added, use `multiplatform-markdown-renderer` (mikepenz) and render the QR for `[🔗](url)` links via zxing-core.
- **Location is required, tags are optional.** Enforced in `EditorViewModel.save()`. The tag/location picker reads from the local Room cache (`cached_tags`) only — never the network — so it works offline. Cache is refreshed by `TagSyncWorker`. Inline tag creation is online-only; failures surface in the picker. See ARCHITECTURE.md → "Tags & location" for the data flow and the migration that adds the `cached_tags` table + `locationId`/`tagIdsJson` columns.

## API contract recap

Mirrors what `life-log-web/src/services/api.ts` does:

- Auth: `x-api-key: <key>` on every request.
- `POST {baseUrl}/entries` — body matches `CreateEntryRequest` (DTO). Server assigns id. Includes `locationId` + `tagIds`.
- `POST {baseUrl}/media/upload` — multipart, field name `file`. Returns `{ filename, path, url }`.
- `GET  {baseUrl}/tags` — full tag list (both `type=tag` and `type=location`). Used by `TagSyncWorker` to refresh the local cache.
- `POST {baseUrl}/tags` — create a new tag/location. Used only by the inline "Create '<x>'" row in the picker.
- IDs are timestamp-based `Long` server-side. We never assign them.
