# Dockerised build environment

Builds Sora without installing a JDK or the Android SDK on your machine. The
only prerequisite is Docker with Compose v2 (`docker compose`, not the older
`docker-compose`).

> **Not a substitute for CI.** GitHub Actions still builds with its own
> toolchain. This exists so you can produce and iterate on an APK locally.

---

## Quick start

```bash
# 1. Build the image (once, and after any Dockerfile change).
#    The USER_ID/GROUP_ID args make build outputs owned by you rather than root.
USER_ID=$(id -u) GROUP_ID=$(id -g) docker compose build

# 2. Produce a debug APK.
docker compose run --rm build
```

The APK is written through the bind mount to:

```
app/build/outputs/apk/debug/app-debug.apk
```

Install it on a connected device from the **host**:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Available services

| Command | What it does |
|---|---|
| `docker compose run --rm build` | `assembleDebug` — the APK |
| `docker compose run --rm test` | `testDebugUnitTest` — all JVM unit tests |
| `docker compose run --rm lint` | `lintDebug` |
| `docker compose run --rm check` | all three, matching CI |
| `docker compose run --rm shell` | interactive shell for ad-hoc Gradle tasks |

Any Gradle task can be run directly:

```bash
docker compose run --rm build ./gradlew :core:core-database:testDebugUnitTest
docker compose run --rm build ./gradlew tasks
```

---

## Supplying an AniList client ID

The build succeeds without one, using the placeholder `0` — only the AniList
login flow will not work. To supply yours (see the root README for how to
register a client), either export it per-run:

```bash
ANILIST_CLIENT_ID=12345 docker compose run --rm build
```

or create a git-ignored `.env` next to `docker-compose.yml`:

```bash
echo 'ANILIST_CLIENT_ID=12345' > .env
```

It is passed as an environment variable and never written into the image or a
committed file.

---

## How this avoids clobbering your host setup

**Your `local.properties` is never read or modified.** If you also use Android
Studio, that file almost certainly contains something like
`sdk.dir=/Users/you/Library/Android/sdk` — a path that does not exist inside
the container. Because `sdk.dir` takes precedence over the `ANDROID_HOME`
environment variable, the container build would fail immediately.

Compose therefore mounts `docker/local.properties.container` **read-only** over
`/workspace/local.properties`. The container sees a file with no `sdk.dir` and
falls back to `ANDROID_HOME=/opt/android-sdk`; your host file is untouched, and
the container cannot write to it either.

**Build outputs are yours, not root's.** Passing `USER_ID`/`GROUP_ID` at image
build time creates a container user matching your account, so `build/` and
`.gradle/` directories stay deletable from the host. (On Docker Desktop for
macOS and Windows, file ownership is virtualised and this is a no-op — harmless
either way.)

**The Gradle cache persists** in the named volume `gradle-cache`. The first
build downloads several hundred MB of dependencies; later builds reuse them.
The Gradle distribution itself is pre-warmed into the image, so it is not
re-downloaded on first run.

---

## What is pinned, and why

| Component | Version | Reason |
|---|---|---|
| JDK | 17 | Hard requirement of AGP 8.13 — newer JDKs are not a drop-in |
| cmdline-tools | `15859902` | Pinned so image rebuilds are reproducible |
| Platform | `android-36` | Matches `compileSdk`/`targetSdk` |
| Build-tools | `36.0.0` **and** `35.0.0` | AGP 8.13's *default* is 35.0.0; omitting it makes the first build stall auto-downloading it |

Change any of these in the `ARG` lines at the top of the `Dockerfile`, then
rebuild.

---

## Troubleshooting

**`no such service: build`** — you are using Compose v1. Use `docker compose`
(space, v2), not `docker-compose` (hyphen).

**Permission-denied on `build/` from the host, or root-owned files** — the
image was built without the user args. Rebuild with
`USER_ID=$(id -u) GROUP_ID=$(id -g) docker compose build`.

**Out-of-memory / "Daemon stopped unexpectedly"** — Gradle is configured for a
4 GB heap. Give Docker Desktop at least 6 GB (Settings → Resources), or lower
`-Xmx` in the `GRADLE_OPTS` line of the `Dockerfile`.

**First build is very slow** — expected. It downloads the full dependency
graph. Subsequent runs hit the `gradle-cache` volume.

**Want a completely clean slate**:

```bash
docker compose down -v          # drops the Gradle cache volume
docker compose build --no-cache
```

**Emulator?** Not included. Running one in Docker needs KVM and
`--privileged`, which is a poor trade for a build image. Build the APK here and
install it on a device or a host-side emulator with `adb install`.
