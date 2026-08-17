# Sora (空)

A native Android client that unifies anime watching and manga reading from
local storage and local-network servers into a single library, with metadata,
discovery and progress tracking backed by [AniList](https://anilist.co).

> **Status: Phase 1b complete.** Multi-module skeleton, branding, adaptive
> navigation shell, and the persistence layer (Room + Proto DataStore + DI).
> See [DECISIONS.md](DECISIONS.md) for the running log of design calls.

---

## Requirements

| Tool | Version |
|---|---|
| JDK | 17 |
| Android Gradle Plugin | 8.13.0 |
| Gradle | 8.13 (via the wrapper) |
| Kotlin | 2.2.21 |
| Android SDK | compileSdk / targetSdk 36, minSdk 26 |

## Getting started

### With Docker (no local JDK or Android SDK needed)

```bash
git clone https://github.com/seif-bkh/Sora.git
cd Sora
USER_ID=$(id -u) GROUP_ID=$(id -g) docker compose build   # once
docker compose run --rm build                             # -> debug APK
```

The APK appears at `app/build/outputs/apk/debug/app-debug.apk`. See
[docker/README.md](docker/README.md) for the other services (`test`, `lint`,
`check`, `shell`) and troubleshooting.

### With a local toolchain

```bash
git clone https://github.com/seif-bkh/Sora.git
cd Sora
./gradlew assembleDebug
```

### Supplying an AniList client ID

The app talks to the AniList GraphQL API using OAuth2. **No client ID is
committed to this repository** - you supply your own.

1. Sign in to AniList and open
   [Settings → Developer](https://anilist.co/settings/developer).
2. Create a new client:
   - **Name**: anything (e.g. `Sora (dev)`)
   - **Redirect URL**: `sora://auth`
3. Copy the numeric **Client ID**.
4. Add it to `local.properties` in the project root (git-ignored):

   ```properties
   anilist.clientId=12345
   ```

The build reads this into `BuildConfig.ANILIST_CLIENT_ID`. If it is absent the
build still succeeds with a placeholder of `0`, so the project compiles
out of the box - but login will fail until a real ID is provided. CI supplies
the value from the optional `ANILIST_CLIENT_ID` repository secret, falling back
to the same placeholder.

AniList's implicit OAuth grant is used (mobile app, no backend), so there is
**no client secret** to configure.

---

## Module structure

```
app/                    Application, MainActivity, NavHost, DI graph root
build-logic/            Gradle convention plugins (shared module config)
core/
  core-common/          Result types, dispatchers, logging, shared utils
  core-model/           Plain data classes; the MediaSource interface
  core-database/        Room database, entities, DAOs, migrations
  core-datastore/       DataStore: settings and encrypted auth tokens
  core-datastore-proto/ Protobuf schemas + generated classes (KSP-free)
  core-network/         Apollo (AniList GraphQL), OkHttp config
feature/
  feature-auth/         AniList OAuth2 login
  feature-library/      Library scan, unmatched review, grid UI
  feature-discovery/    Trending / seasonal / recommendations
  feature-details/      Series detail: metadata + unit list
  feature-player/       Video playback (Media3/ExoPlayer)
  feature-reader/       Manga reader (paged + webtoon)
  feature-settings/     Settings, server config, account
sources/
  source-local/         SAF/MediaStore local scanning
  source-server/        WebDAV/HTTP MediaSource implementation
tools/svg2vd/           Branding SVG -> VectorDrawable generator
branding/               Brand SVGs (design source of truth)
```

### Module boundary rules

- Feature modules **never** depend on each other.
- Feature modules see `core-model` (which owns `MediaSource`) and
  `core-common`; they **never** depend on `source-local` or `source-server`
  directly. Concrete sources are bound into the Hilt graph by `:app`.
- `:app` is the composition root and the only module allowed to see everything.

These rules are enforced by the `sora.android.feature` convention plugin, which
gives every feature module a fixed dependency set.

---

## Branding assets

`branding/*.svg` is the design source of truth. Android cannot consume those
SVGs directly - VectorDrawable has no `<text>`, `<line>` or `rx` clip support -
so the drawables in `app/src/main/res/drawable/` are generated:

```bash
# One-time: fetch the font used for the 空 glyph outlines
mkdir -p /tmp/fontprobe && cd /tmp/fontprobe \
  && npm init -y && npm i @fontsource/noto-serif-jp

# Regenerate the VectorDrawables
pip install -r tools/svg2vd/requirements.txt
python3 tools/svg2vd/build_icons.py
```

Noto Serif JP (SIL OFL 1.1) is used at asset-generation time only; the font is
not bundled in the APK.

---

## Testing

```bash
./gradlew testDebugUnitTest   # JVM unit tests, all modules
./gradlew lintDebug           # Android Lint
```

Room migration and DAO tests run on the JVM via Robolectric, so no emulator is
needed. Exported schemas under `core/core-database/schemas/` are committed
because `MigrationTestHelper` reads them at test time - see that folder's
README before touching them.

CI runs assemble, unit tests and lint on every push. The workflow lives at
`.github/workflows/ci.yml` and is maintained manually - see
[AGENTS.md](AGENTS.md).

---

## Out of scope

Torrent-based streaming or downloading is **explicitly not implemented**. The
`MediaSource` abstraction is designed so an additional source could be added
later without touching the player, reader or UI layers.
