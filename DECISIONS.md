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
