# DECISIONS.md

A running log of design calls, gap-fills and deviations from the project brief,
each with a one-line rationale. Newest phase last.

---

## Phase 1a — Foundation: skeleton, branding, adaptive shell

### Build environment

| # | Decision | Rationale |
|---|---|---|
| 1 | **All compilation happens in GitHub Actions, not locally.** | The dev sandbox has no JDK and no Android SDK, and `dl.google.com`, `repo1.maven.org`, `plugins.gradle.org` and `services.gradle.org` are all unreachable from it. CI is the only environment that can build, so the `arena/**` push trigger in `ci.yml` is the verification loop. |
| 2 | **AGP 8.13.0 / Gradle 8.13 / JDK 17, not AGP 9.x.** | 8.13 is the final 8.x line, supports compileSdk 36, and is the most widely-proven combination. Since every compile costs a CI round-trip, a major-version migration is expensive to debug incrementally. Revisit post-MVP. |
| 3 | **compileSdk / targetSdk = 36 (Android 16), minSdk 26.** | Brief says "targetSdk = latest stable at build time"; Play requires API 36 for new apps from 31 Aug 2026. minSdk 26 is specified by the brief. |
| 4 | **Gradle wrapper JAR fetched from the `gradle/gradle` GitHub repo.** | `services.gradle.org` is blocked in the sandbox; the GitHub API is not. The jar is byte-identical to the official v8.13.0 artifact and validated as a well-formed archive containing `GradleWrapperMain`. |
| 5 | **`build-logic` included build with `sora.*` convention plugins.** | Standard multi-module practice (as in Now in Android). Not a tech-stack substitution: it centralises the config that 13 modules would otherwise duplicate. Also the enforcement point for the brief's module-boundary rules. |
| 6 | **Gradle configuration cache and parallel builds enabled.** | Free build-time win on a wide module graph; disable if a plugin turns out to be incompatible. |
| 6a | **Kotlin is pinned to 2.0.21 because Gradle 8.13 embeds that compiler.** | `kotlin-dsl` compiles `build-logic` with the compiler embedded in the Gradle distribution, which cannot read metadata produced by a newer Kotlin. Kotlin 2.2.21 failed here. **Kotlin and Gradle must be bumped in lockstep.** |
| 6b | **Convention plugins fetch extensions with `getByType` and configure the object directly, instead of using configuration lambdas.** | Gradle's Kotlin DSL exposes both receiver-style (`T.() -> Unit`) and Action-style (`(T) -> Unit`) overloads depending on API and version; choosing wrong is a compile error (cost two CI round-trips). Reading the object avoids the ambiguity entirely. |
| 6c | **CI logs are read via the signed redirect URL from the jobs `/logs` endpoint.** | GitHub's log storage hosts (`*.blob.core.windows.net`, `results-receiver`) are unreachable from the sandbox, so `gh run view --log` always fails. `curl -w '%{redirect_url}'` yields a pre-signed, self-authenticating URL that can be fetched out-of-band. This is the only way to see a build error; without it, fixing CI is guesswork. |

### Module structure

| # | Decision | Rationale |
|---|---|---|
| 7 | **`core-model` is an Android library, not a pure-JVM module.** | The brief's own `MediaSource` contract returns `android.net.Uri` in `PlayableOrReadable.VideoStream`, which requires the Android framework. A `sora.jvm.library` convention plugin exists for genuinely framework-free modules later. |
| 8 | **`:benchmark` is not created in Phase 1a.** | The brief sequences Macrobenchmark + Baseline Profiles into Phase 7. Creating an empty module now would only add a failing/no-op CI target. 13 of the 14 listed modules exist. |
| 9 | **Feature modules get a fixed dependency set from the convention plugin.** | The brief forbids feature-to-feature dependencies and forbids features seeing `source-*` concretes. Encoding that in `AndroidFeatureConventionPlugin` makes the rule structural rather than a convention someone has to remember. |
| 10 | **Type-safe project accessors (`projects.core.coreModel`) enabled.** | Compile-time-checked module references; a typo fails configuration instead of silently resolving to nothing. |

### Branding

| # | Decision | Rationale |
|---|---|---|
| 11 | **Adaptive icon foreground is transparent; the gradient lives only on the background layer.** | The supplied SVGs painted a gradient on *both* layers over different spans (foreground `y=16..92`, background `y=0..108`), so the ramps did not align and a squircle seam was visible. A transparent foreground is also what makes launcher parallax work. Confirmed with the user before changing. |
| 12 | **Horizon line inset from `x=18..90` to `x=20..88`.** | The guaranteed-visible region of an adaptive icon is a 72dp *circle*; at `y=63` that circle spans only `x≈19.1..88.9`, so the original round caps were clipped on circular-mask launchers. |
| 13 | **Kanji 空 converted from `<text>` to outline paths via `tools/svg2vd/`.** | VectorDrawable has no text support at all. Outlines are generated from Noto Serif JP (SIL OFL 1.1) at asset-build time; the font is not shipped in the APK. Committing the generator keeps the assets reproducible instead of hand-traced. |
| 14 | **No legacy density-bucket launcher PNGs.** | minSdk 26 means every supported device understands `<adaptive-icon>`; the mdpi..xxxhdpi ladder would be dead weight. |
| 15 | **Notification icon is the frame + horizon tick only, no kanji.** | Android renders notification small icons as a flat alpha mask at 24dp, where the kanji strokes turn to mush. Verified by rendering at true 24dp scale. |
| 16 | **Play Store 512px PNG not generated.** | Rasterising CJK text needs a renderer that cannot be installed in the sandbox. It is a store-listing asset, not a build input, so nothing blocks on it; `branding/ic_launcher_play_store.svg` is the source when needed. |
| 17 | **The wordmark will be a Compose component, not a bundled VectorDrawable.** | As real text it inherits theme colour (so light/dark variants collapse into one), scales with the user's font-size setting, and needs no bundled CJK font since the platform font stack covers 空. The four wordmark SVGs remain in `branding/` as design reference. |
| 18 | **Splash uses `androidx.core:core-splashscreen`, one theme, no `values-v31` variant.** | The library backports the Android 12 SplashScreen API across the whole minSdk 26+ range, so a single theme definition covers every supported device. |

### App shell

| # | Decision | Rationale |
|---|---|---|
| 19 | **Dark theme is the hardcoded default; light is fully implemented.** | Brief specifies dark-by-default for the content domain. Phase 8 adds a system/light/dark setting that will drive the existing `darkTheme` parameter. |
| 20 | **Static fallback palette seeded from `#4A90E2`.** | The brand seed stated in the supplied background SVG. Used below Android 12, where Material You dynamic colour is unavailable. |
| 21 | **StrictMode: disk violations log, network violations are fatal.** | Framework and AndroidX initialisers do benign main-thread disk reads we do not control; `penaltyDeath()` on those makes the app undebuggable. Main-thread network is never acceptable, so `penaltyDeathOnNetwork()` applies. Satisfies the brief's hard rule while staying usable. |
| 22 | **`NavigationSuiteScaffold` for adaptive chrome: bottom bar on compact, nav rail on medium/expanded.** | Brief requires `WindowSizeClass` patterns from the start. A permanent drawer on expanded widths is deferred - with three destinations it wastes width the library grid can use for columns. |
| 23 | **String-based navigation routes, not type-safe serialization routes.** | No destination has real arguments yet. Type-safe routes are a mechanical upgrade once Phase 2+ introduces them; doing it now would be speculative. |
| 24 | **Placeholder screens name the phase that will replace them.** | Makes the Phase 1a app genuinely navigable end-to-end (the brief's bar for a "working, runnable app state") rather than a blank shell. |

### Deferred (explicitly, per brief)

- **Sub-chapter detection inside volume CBZs** (`ComicInfo.xml` parsing / OCR) — deferred post-MVP; the brief defers it and not all releases include the metadata.
- **mpv-based fallback player** — not implemented; the player will sit behind a `VideoPlayerController` interface so a second implementation can be swapped in. HEVC 10-bit playback compatibility varies by device.
- **Torrent sources** — out of scope entirely. `MediaSource` is designed so one could be added without touching player/reader/UI.

---

## Phase 1b — Persistence: Room, Proto DataStore, DI

### Data layer

| # | Decision | Rationale |
|---|---|---|
| 25 | **`LibraryEntry.lastConfirmedChapter` added to the brief's schema.** | The brief requires persisting the chapter number a user confirms when finishing a VOLUME unit "so future syncs for the same series have a reference point". Storing it on the series (not the unit) is what lets the next volume's dialog pre-fill an estimate. |
| 26 | **`MatchCandidate` carries denormalised `candidateTitle` / `candidateCoverUrl`.** | The review picker shows five candidate posters per unmatched series. Without these, rendering the picker means one AniList round-trip per candidate, which would blow the 30 req/min limit during a first scan. |
| 27 | **Enums persisted by name, not ordinal.** | Ordinals are positional: inserting a constant mid-enum would silently reinterpret every existing row. Names cost a few bytes and fail loudly instead. |
| 28 | **Foreign keys with `ON DELETE CASCADE` from `MediaUnit`/`MatchCandidate` to `LibraryEntry`, plus indices on `rootPath`, `path`, `anilistId`.** | Deleting a series must not orphan its units. The indices back the incremental-rescan lookups ("have I seen this folder/file?"), which would otherwise be a table scan per filesystem entry. |
| 29 | **No `fallbackToDestructiveMigration`.** | The library, confirmed matches and read positions are user effort. A missing migration must fail loudly rather than wipe them. |
| 30 | **Database starts at version 2 with a real 1→2 migration.** | The brief requires "one trivial migration test". The 1→2 step adds `lastConfirmedChapter`, so the test exercises a genuine schema change rather than a synthetic no-op. |
| 31 | **Exported schema JSON committed under `core-database/schemas/<variant>/`, and mapped in as a unit-test asset source.** | `MigrationTestHelper` reads schemas from test assets at runtime; without both the committed v1 and the generated v2 it fails with `FileNotFoundException`. |

### Settings and credentials

| # | Decision | Rationale |
|---|---|---|
| 32 | **Proto DataStore, as specified (user-confirmed over Preferences).** | Type-safe settings; migrating Preferences→Proto later would require a data migration on installed devices. |
| 33 | **Two separate stores: plaintext `UserSettings`, encrypted `AuthTokens`.** | Mixing them would either leave the token in plaintext or force every settings read through decryption. |
| 34 | **Tink used directly for encryption, not `androidx.security:security-crypto` or `androidx.datastore:datastore-tink`.** | `security-crypto` was deprecated in April 2025; `datastore-tink` (the official `AeadSerializer`) is alpha-only. Tink is what both wrap. AES256-GCM keyset wrapped by an Android Keystore master key. |
| 35 | **AEAD associated data is the file name.** | Not secret; it binds the ciphertext to this file so another valid encrypted blob cannot be swapped in and decrypt successfully. |
| 36 | **Decryption failure raises `CorruptionException` (→ signed-out default), not a crash.** | A wrong key or tampered file should force re-auth, not a launch crash loop. |
| 37 | **Sign-out clears AniList credentials but preserves the server password.** | They are unrelated accounts; wiping the WebDAV password on AniList logout would silently break the user's local library. |
| 38 | **Protobuf codegen isolated in `core-datastore-proto` (14 modules now, not 13).** | With protobuf and KSP in one module, KSP can run before protoc and Hilt sees `error.NonExistentClass` instead of `UserSettings`. A KSP-free codegen module removes the ordering conflict. Same structure Now in Android uses. |

### Build and test

| # | Decision | Rationale |
|---|---|---|
| 39 | **Robolectric added for DAO/migration tests (not named in the brief's stack).** | The brief requires a migration test but pins no Android-test runtime. Robolectric runs Room on the JVM, so migration and DAO tests execute in the normal CI unit-test job instead of needing an emulator. Flagged as a stack addition. |
| 40 | **`robolectric.properties` pins the emulated SDK to 34.** | Robolectric ships no SDK jar for API 36 (our compileSdk) and fails at startup in `DefaultSdkPicker`. Test-runtime only; does not change what the app compiles or targets. |
| 41 | **Room runtime exposed as `api`, not `implementation`.** | `SoraDatabase` extends `RoomDatabase`, so it is part of the module's public API; with `implementation`, consumers cannot resolve the supertype. |
| 42 | **DI verified by calling `@Provides` functions directly rather than building a full Hilt test component.** | Missing bindings are already a compile-time error in Hilt. What needs runtime proof is that the database actually opens and the DataStore defaults are right - achievable without a custom test runner. |
| 43 | **`SoraResult`/`SoraError` rather than `kotlin.Result`.** | `kotlin.Result` cannot be used in many return positions and models no loading state. A sealed error hierarchy also lets the UI distinguish "offline but cached" (silent) from "offline with no cache" (visible error), which the brief's offline mode needs. |

---

## Tooling — Dockerised local build environment

| # | Decision | Rationale |
|---|---|---|
| 44 | **Docker build image added (`Dockerfile`, `docker-compose.yml`).** | Requested so an APK can be produced locally without installing a JDK or Android SDK. Additive only - no existing build file changed, and CI keeps using its own toolchain. |
| 45 | **`docker/local.properties.container` is mounted read-only over `/workspace/local.properties`.** | `local.properties` is git-ignored and on a machine with Android Studio typically holds `sdk.dir=<host path>`, which does not exist in the container and takes precedence over `ANDROID_HOME` - the build would fail instantly. Shadowing isolates both directions: the container ignores the host SDK, and cannot modify the host file. Chosen over auto-editing the user's file. |
| 46 | **Both build-tools 36.0.0 and 35.0.0 installed.** | `compileSdk` is 36, but AGP 8.13's *default* build-tools is 35.0.0. With only 36.0.0 present the first build stalls silently auto-downloading the other. |
| 47 | **`USER_ID`/`GROUP_ID` build args.** | The repo is bind-mounted, so a root container leaves root-owned `build/` and `.gradle/` directories the host IDE cannot delete. |
| 48 | **Gradle cache is a named volume, and the Gradle distribution is pre-warmed into the image.** | Docker seeds an empty *named volume* from the image (a bind mount would hide it), so the ~150 MB distribution is not re-fetched on first run and dependencies persist across runs. |
| 49 | **`SHELL ["/bin/bash", "-o", "pipefail", "-c"]` in the Dockerfile.** | Docker's default RUN shell is `dash`, which rejects `set -o pipefail` ("Illegal option") - needed by the `yes \| sdkmanager --licenses` step. Caught by statically parsing every RUN block, since Docker is unavailable in the agent sandbox. |
| 50 | **`.env` added to `.gitignore`.** | The compose setup suggests putting `ANILIST_CLIENT_ID` there; without the ignore rule that credential would be committable. |
| 51 | **No emulator in the image.** | Requires KVM and `--privileged`, a poor trade for a build image. Build here, install to a device or host emulator with `adb install`. |
| 52 | **UNVERIFIED: the image has not been built or run.** | Docker is not installed in the agent sandbox and all container registries are unreachable, so this could not be executed end to end. YAML, all 7 RUN blocks, and the entrypoint's three guard paths were validated statically instead. First real run is on the user's machine. |

---

## Design — showcase and tokens

| # | Decision | Rationale |
|---|---|---|
| 53 | **`design/showcase/index.html` committed as the canonical design reference.** | A rendered showcase communicates intent better than prose, and committing it means it survives sandbox resets and stays the artifact both sides point at. Static HTML; not part of the Gradle build. |
| 54 | **Image paths rewritten to `../../concepts/`.** | The showcase was authored assuming `concepts/` sat beside it. Moving it to `design/showcase/` broke every render; paths now resolve from the repo root, verified by serving the page locally. |
| 55 | **Palette extended beyond the single brand blue:** `paper #E8E6DF` for text (not pure white), `#B48CFF` violet as a companion glow, `ink2/ink3` raised surfaces. | Warm off-white on near-black reads as film rather than terminal and lowers glare in the dark. Two-hue ambient washes are richer than tinting one hue. |
| 56 | **Type stack fixed: Cormorant Garamond 300 (display), Inter (UI), JetBrains Mono (numerals).** | Mono for episode numbers and `142/310` gives tabular figures, so progress counters do not jitter as digits change - a real defect in a progress-first UI. |
| 57 | **`prefers-reduced-motion` support added to the showcase.** | The spec makes reduced-motion a hard requirement for the app (§6); the document asserting that rule should not itself ignore it. |
| 58 | **Open question 1 (brand blue) closed.** | The showcase keeps `#4A90E2` as brand/fallback while layering ambient extraction over content surfaces - the two were never in conflict. |

---

## Phase 1c — shell and theme implementation

| # | Decision | Rationale |
|---|---|---|
| 59 | **Cormorant Garamond is bundled, not fetched via downloadable fonts (DESIGN.md §8 Q2).** | Closes the last open question. Downloadable fonts add a Play Services dependency, a first-launch network round trip, and a visible fallback flash on the hero title — unacceptable for the one element the design leans on hardest. At 47,148 B the download cost is noise next to an 18.8 MB APK. |
| 60 | **The font *generator* is committed (`tools/fonts/build_fonts.py`), not just the `.ttf`.** | Upstream ships only a 1.2 MB variable font, so the shipped file is the output of a two-step pipeline (instance to wght=300, subset to Latin). Committing an opaque 47 KB binary with no record of how it was produced makes it unreproducible and unauditable. The script also asserts CJK is absent, so the fallback behaviour below cannot regress silently. |
| 61 | **No CJK glyphs are bundled; 空 resolves through the platform serif fallback.** | Cormorant has no CJK coverage. Bundling a CJK serif to render a single character in the wordmark would cost several megabytes. Asserted in the build script so it reads as intent rather than oversight. |
| 62 | **Inter and JetBrains Mono dropped in favour of the platform sans and monospace (revises #56).** | #56 fixed the type stack from a *web* showcase where both were free CDN links. On Android each is a real bundled cost. At label sizes the platform sans is visually near-identical, and platform monospace already has the tabular figures that were the entire reason #56 named JetBrains Mono. The serif is the only face carrying the design's identity, so it is the only one worth bundling. |
| 63 | **Ambient colour extraction is hand-rolled on `androidx.palette` rather than taken from Coil.** | Coil 3's `coil-core` exposes no Palette integration, so there is nothing to reuse. Palette is a single 1.0.0 artifact with no transitive weight. |
| 64 | **Extracted colours are luminance-clamped (glow 0.06–0.34, accent 0.26–0.78) with hue preserved.** | Cover art is arbitrary user content: a white cover yields an accent that glares on near-black, a black one yields an invisible glow. The clamp scales linearly in sRGB, which is approximate but hue-preserving — and hue is the part that carries "the app's mood follows the content". A perceptual OkLab conversion would be more correct and far too expensive for something recomputed on every scroll frame. |
| 65 | **The clamp functions are `internal`, not `private`, so tests call the real arithmetic.** | The §6 contrast guarantee is only as good as its test. Restating the formula in the test file would let the two drift apart silently and void the guarantee while still showing green. |
| 66 | **Coil deliberately *not* added in this phase.** | Nothing loads a remote image yet. Adding the dependency now would be scope creep, and pinning a version months before first use invites a stale pin. It arrives with the first screen that renders cover art. |
| 67 | **`material3-adaptive-navigation-suite` removed from the catalog and from `:app`.** | Its only consumer was the deleted `NavigationSuiteScaffold`. Leaving an unused dependency in the catalog implies the bottom bar might come back. |
| 68 | **`SharedTransitionLayout` wraps the whole NavHost, with the opt-in confined to `SoraNavHost.kt`.** | The API is experimental in the pinned Compose 1.7 BOM (stable only in 1.10). Wrapping once at the graph root means individual screens participate without each one repeating `@OptIn`, and there is a single file to revisit at the 1.10 bump. No version bump now — that would drag the whole BOM. |
| 69 | **The rail on medium/expanded is icon-only, no labels.** | A labelled vertical rail is a bottom bar rotated 90°; it would reintroduce exactly the Tachiyomi silhouette §3 exists to remove. Labels live in `contentDescription`, so screen readers lose nothing. |
| 70 | **The current page name ("SORA" / "DISCOVER") is rendered in the compact chrome.** | With no bottom bar and no tab indicator, a two-page pager has no affordance at all — a user cannot tell a second page exists. The micro-label is the cheapest honest hint that satisfies §6's ban on gesture-only function alongside the compass glyph. |
| 71 | **`PlaceholderScreen`'s surface is transparent.** | An opaque surface would occlude the shell's ambient wash and flatten the entire background treatment, making the theme look broken while it is technically correct. |
| 72 | **Compose UI tests run under Robolectric, in the JVM `test` source set.** | CI has no emulator. Putting the shell's pager and accessibility assertions in `androidTest` would mean they never actually run. |
| 73 | **UNVERIFIED locally: no Kotlin in this phase has been compiled.** | The sandbox has no JDK or Android SDK and every Maven/Google host is unreachable. CI is the first compiler to see this code, exactly as in Phases 1a/1b. |
