# Sora — UI/UX Design Specification

> **Status: proposed, pending approval.** No Compose code implements this yet.
> Reference renders in `concepts/`.
>
> **Interactive version:** `design/showcase/index.html` — open it in a browser
> (or `python3 -m http.server` from the repo root). It is the same spec, and it
> pins down concrete values this document only described. Where the two differ,
> the showcase wins and this file should be corrected.

---

## 1. The problem being solved

The Phase 1a shell looked like Tachiyomi/Aniyomi. That was not a styling
accident — it was **information architecture**. The app shipped with:

- a bottom navigation bar,
- a top-level destination literally called "Library",
- whose content is a grid of covers sorted by title.

Every app built on that skeleton reads the same regardless of colour or
corner radius, because the skeleton *is* the identity. Restyling it does not
help. The fix is to change what the home screen is *about*.

### The core insight

Tachiyomi's grid answers **"what do I own?"** — a *browsing* question.

Sora's user already owns the content: they pointed the app at their own
folders. Their real question is **"what do I continue?"** — a *resuming*
question, and the app already knows the answer. `MediaUnit` stores
`progressPercent`, `currentPage`/`totalPages` and `lastPositionMs`; the
resume queue is a query over data that already exists.

**The design principle, stated once:**

> Sora opens on the single thing you are most likely to tap.
> Everything else is one gesture away.

---

## 2. Direction

Two complementary modes, chosen because they answer different questions:

| Surface | Mode | Question it answers |
|---|---|---|
| Home / your content | **Continuum** | "Where was I?" |
| AniList discovery | **Canvas** | "What's out there?" |

**Continuum** — a progress-first queue. Cover art is sized by *relevance*, not
by grid position. The most-resumable item is the largest element on screen.

**Canvas** — full-bleed, one title at a time, poster-gallery. Correct for
unfamiliar titles you are evaluating; deliberately *not* used for the library,
where swiping one-at-a-time through 300 series would be miserable.

**Shelf** (physical-media skeuomorphism, `concepts/02-shelf.jpg`) was
considered and rejected for MVP: highest novelty, but ages badly, fits manga
far better than anime, and depends on spine artwork AniList does not reliably
provide. Recorded here so the decision is not relitigated by accident.

---

## 3. Navigation model — no bottom bar

**The bottom navigation bar is removed.** It is the single strongest visual
signature of the Tachiyomi lineage, and removing it forces every other
decision to be genuinely different.

Replaced by:

| Surface | Reached by | Rationale |
|---|---|---|
| **Home (queue)** | app launch | The default and the point of the app |
| **Discover** | horizontal swipe left→right, or the compass glyph | A peer of home, not a tab |
| **Search** | pull down on home | Muscle-memory gesture; costs zero permanent chrome |
| **Filters** (anime / manga / unmatched) | horizontal chip row *inside* home | A filter, not a destination |
| **Settings** | avatar glyph, top-right | Infrequent; does not deserve a permanent slot |
| **Detail** | tap any card | Shared-element transition from cover |
| **Player / Reader** | tap resume | Full-screen, zero chrome |

Home and Discover are a two-page horizontal pager, so the primary switch is a
thumb swipe. Only two glyphs persist: avatar (top-left) and search (top-right).

### Adaptive behaviour (`WindowSizeClass`)

| Width | Layout |
|---|---|
| **Compact** (phone) | Single column. Hero card + horizontal rails. Pager for Home/Discover. |
| **Medium** (small tablet, unfolded, landscape phone) | Two-pane: queue list (⅓) + hero detail (⅔). Narrow **icon-only** rail, ~72dp, no labels. |
| **Expanded** (large tablet) | Same two-pane, wider hero, rails become a multi-column poster wall. |

The icon-only rail at medium/expanded is deliberate: a *labelled* rail is just
a bottom bar rotated 90°, and would reintroduce the look we are removing.

---

## 4. Screens

### 4.1 Home — the queue (`concepts/01-continuum.jpg`)

Vertical scroll, three bands:

1. **Hero — "Continue"** (~45% of viewport)
   The single highest-priority unit. Full-bleed artwork, rounded, with a thin
   progress line across the bottom edge and an ambient glow bleeding into the
   background. One tap resumes playback/reading directly — it does **not** go
   to the detail screen. This is the app's primary action.

2. **Rails — "Continue reading" / "Continue watching"**
   Horizontal `LazyRow`s of medium cards, each with a thin progress indicator.
   Split by media type so a manga reader is never forced past anime.

3. **Rail — "Jump back in"**
   Started but stale (untouched > 14 days). Separated because "resume episode
   4 of 12" and "you abandoned this in March" are different intents.

**Empty state** (no library yet): the hero becomes a single full-width
invitation to pick a folder — the SAF entry point — instead of an empty grid.

**Ordering (explicit, because it drives everything):**
`lastPositionMs`/`currentPage` recency, weighted by completion — a unit at 80%
outranks one at 5%. Fully-watched units drop out; the *next* unit in that
series takes their place.

### 4.2 Detail (`concepts/04-continuum-detail.jpg`)

- Top ~45%: full-bleed key art fading to black, arriving via **shared-element
  transition** from the tapped cover.
- Large thin-serif title; one compact metadata row (year · type · rating ·
  genres). AniList synopsis is **collapsed to 3 lines** behind "more" — it is
  reference material, not the reason you opened the screen.
- One wide **Resume** pill, labelled with the actual next unit
  ("Resume · Episode 4"), never a generic "Play".
- Unit list: number, title, thin progress line. Watched rows dim rather than
  vanish. The in-progress row is visually distinct.
- **Volume units** show `142/310` prominently (brief requirement), since a
  volume spans multiple sessions.

### 4.3 Discover — Canvas (`concepts/03-canvas.jpg`)

Full-bleed poster per title, vertical swipe between titles, horizontal swipe
between shelves (Trending / Seasonal / Recommended). Metadata sits over a
bottom scrim. One action: **Add to library**.

Rationale: for unfamiliar titles the artwork *is* the information. This is
also where AniList's rate limit is friendliest — one title on screen means one
prefetch ahead, not a grid of 30 requests.

### 4.4 Player and Reader

Unchanged in intent from the brief; both are zero-chrome and full-screen. The
design system applies only to their overlay controls: same type scale, same
accent, controls fade after 3s. Detailed in their own phases.

---

## 5. Visual language

### Colour

- **Ambient extraction.** The dominant colour of the current cover, desaturated
  and darkened, tints the background glow and the accent. The app's mood
  follows the content. This is the strongest single differentiator and cannot
  be retrofitted onto a static-palette grid app.
- **Base is near-black** (`#08090C`), not Material's elevated greys. True black
  makes artwork the only light source and is cheaper on OLED.
- **Static fallback** stays seeded from the brand `#4A90E2` for pre-Android-12
  devices and any cover whose extraction fails.
- Dynamic colour (Material You) applies to *system* surfaces only; content
  surfaces use extraction so the app is not at the mercy of the wallpaper.

> **Open question for the user:** ambient extraction largely overrides the
> `#4A90E2` sky-blue in day-to-day use. Flagged in §8.

### Design tokens (from `design/showcase/index.html`)

The showcase fixes values that were previously adjectives. These map directly
onto Compose `ColorScheme`, `Typography` and `Shapes`:

| Token | Value | Notes |
|---|---|---|
| `ink` (base) | `#08090C` | Near-black app background |
| `ink2` / `ink3` | `#0B0D12` / `#11141B` | Raised surfaces, used sparingly |
| `paper` | `#E8E6DF` | Primary text. **Warm off-white, not pure `#FFFFFF`** |
| `paper-muted` | `#A8A69F` | Secondary text |
| `accent` | `#4A90E2` | Brand / static fallback |
| `accent-soft` | `#7FB0EE` | Lighter accent, code and links |
| secondary glow | `#B48CFF` | Violet, paired with accent in ambient washes |
| Card radius | 16dp | |
| Hero radius | 24dp | |
| Hairline borders | white @ 4–10% | The dominant separator, instead of elevation |

Two things the showcase establishes that the prose did not:

- **Text is `#E8E6DF`, not white.** A warm off-white against near-black reads
  as film rather than terminal, and lowers glare in a dark room.
- **Violet `#B48CFF` as a companion to the blue.** Ambient washes interpolate
  between the two rather than tinting a single hue.

### Type

Deliberate two-family contrast, since single-family Material type is part of
the generic look:

- **Display/titles:** **Cormorant Garamond Light (300)** — a thin
  high-contrast serif. Editorial, unexpected in this category, and the reason
  the renders read as "premium" rather than "utility". **Bundled at 47 KB**
  (see §8 Q2).
- **Body/labels/UI:** the **platform sans** (`FontFamily.SansSerif`), small
  and quiet. The showcase used Inter over a web font link; on Android the
  platform sans is close enough at label sizes that bundling a second family
  is not worth the download, and it inherits the user's font-scale settings.
- **Monospace:** `FontFamily.Monospace` for numerals and technical labels —
  episode numbers, `142/310`, timestamps. This is functional, not decorative:
  proportional digits make a progress counter visibly jitter as the number
  changes. The platform monospace has tabular figures by definition, so
  bundling JetBrains Mono would buy nothing but megabytes.
- Small labels are uppercase with wide tracking (~0.22em); this is a
  signature of the showcase and should carry into the app.
- Titles are large. A series title at 32sp+ is a deliberate statement that
  content outranks chrome.

### Shape, depth, motion

- Cards: 16dp radius. Hero: 24dp.
- Depth from **glow and scrim**, not Material elevation shadows.
- Motion: shared-element cover→detail; hero parallax on scroll; content
  cross-fades, chrome never slides. Respect
  `Settings.Global.ANIMATOR_DURATION_SCALE` for reduced-motion users.
- Predictive back (Android 14+) hooked into the shared-element return.

---

## 6. Accessibility (non-negotiable, easy to lose in a dark cinematic design)

- Text over artwork always sits on a scrim guaranteeing **≥ 4.5:1**; the scrim
  is computed against the extracted colour, not assumed.
- Ambient accent is **luminance-clamped** so a low-contrast cover cannot
  produce unreadable accents.
- Every glyph-only control (avatar, search, compass) carries a
  `contentDescription`.
- Touch targets ≥ 48dp even where the visual is smaller.
- Gesture navigation has a non-gesture equivalent: swipe-to-Discover is also
  the compass glyph; pull-to-search is also the search glyph. **No function is
  gesture-only** — this is the most common failure mode of chrome-less designs.

---

## 7. What this changes in existing code

| File | Change |
|---|---|
| `app/ui/SoraApp.kt` | Replace `NavigationSuiteScaffold` with a custom adaptive shell |
| `app/navigation/SoraDestination.kt` | `TopLevelDestination` enum is deleted; Home/Discover become pager pages |
| `app/navigation/SoraNavHost.kt` | Routes stay; hosting changes |
| `app/ui/theme/*` | Add ambient extraction, near-black base, serif display family |
| `core-database` | **No schema change** — the queue is a new DAO query over existing columns |

`MediaUnit`/`LibraryEntry` need no migration, which is the main reason this
redesign is affordable now rather than after Phase 5.

---

## 8. Open questions

1. **Brand blue** — ~~open~~ **answered by the showcase**: `#4A90E2` is kept as
   brand and static fallback, with ambient extraction layered over content
   surfaces and `#B48CFF` as its companion. Not in conflict.
2. **Serif display face** — ~~open~~ **resolved: bundle it.** The showcase
   specifies **Cormorant Garamond**, and upstream `google/fonts` ships *only*
   a variable TTF (1,195,560 B, wght 300–700). `tools/fonts/build_fonts.py`
   instances it to wght=300 (773,016 B) and subsets to Latin + punctuation,
   producing **47,148 B / 236 glyphs** — an order of magnitude smaller than
   the ~200 KB this document originally guessed, which settles the question.
   The generator is committed rather than just the binary, so the artefact is
   reproducible and auditable.

   Cormorant has **no CJK**, so 空 in the wordmark resolves through Android's
   platform-serif fallback chain. That is intended, and the build script
   asserts the absence so nobody "fixes" it by bundling a CJK face.
   SIL OFL 1.1 requires the licence to ship with the font:
   `app/src/main/assets/licenses/cormorant_garamond_OFL.txt`.
3. **Discover as a pager page** vs. reachable only from a glyph — ~~open~~
   **resolved: pager page.** Two pages, swipe or compass glyph.
4. **Phasing** — ~~open~~ **resolved: land the shell first.** Building Phase 2
   screens against a shell that is about to be deleted would mean writing them
   twice; the shell is cheap now and expensive later.
