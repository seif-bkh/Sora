#!/usr/bin/env bash
#
# Entrypoint for the Sora build container.
#
# Verifies the environment before handing over to Gradle, so a misconfiguration
# produces a one-line explanation instead of a confusing Gradle stack trace
# several minutes into a build.

set -euo pipefail

log()  { printf '\033[0;36m[sora]\033[0m %s\n' "$*"; }
warn() { printf '\033[0;33m[sora]\033[0m %s\n' "$*" >&2; }
fail() { printf '\033[0;31m[sora]\033[0m %s\n' "$*" >&2; exit 1; }

# --- Sanity checks ----------------------------------------------------------

[ -f "./gradlew" ] || fail \
    "No gradlew in /workspace. The repository does not appear to be mounted -
       run this from the project root via 'docker compose run --rm build'."

[ -d "${ANDROID_HOME}/platforms" ] || fail \
    "Android SDK missing at ${ANDROID_HOME}. Rebuild the image: docker compose build"

# `sdk.dir` overrides ANDROID_HOME, so a host path leaking in here breaks the
# build. docker-compose.yml shadows the file to prevent this; if the guard is
# ever bypassed (running the image directly with a different mount), say so
# clearly rather than letting Gradle fail cryptically.
if [ -f "./local.properties" ] && grep -qE '^\s*sdk\.dir' ./local.properties; then
    SDK_DIR="$(grep -E '^\s*sdk\.dir' ./local.properties | head -1 | cut -d= -f2- | tr -d '[:space:]')"
    if [ ! -d "${SDK_DIR}" ]; then
        fail "local.properties sets sdk.dir=${SDK_DIR}, which does not exist in this
       container. That path is from your host machine.

       docker-compose.yml normally shadows local.properties with
       docker/local.properties.container to avoid exactly this. If you are
       running 'docker run' by hand, add:
           -v \"\$PWD/docker/local.properties.container:/workspace/local.properties:ro\""
    fi
fi

# --- AniList client id ------------------------------------------------------
# Read from the environment (app/build.gradle.kts falls back to it) so no
# credential is ever written into a committed file.
if [ -n "${ANILIST_CLIENT_ID:-}" ]; then
    log "AniList client id supplied via environment."
else
    warn "ANILIST_CLIENT_ID not set - building with placeholder '0'."
    warn "The app compiles and runs, but AniList login will not work."
    warn "Set it with: ANILIST_CLIENT_ID=12345 docker compose run --rm build"
fi

# Gradle needs a writable home. With the named volume this is a no-op, but it
# keeps a bare 'docker run' from failing on a missing directory.
mkdir -p "${GRADLE_USER_HOME:-/gradle-cache}"

# `|| true` so a missing/odd JDK cannot abort the script under `set -e` before
# the far more useful Gradle error has a chance to appear.
log "JDK      $(java -version 2>&1 | head -1 | tr -d '"' || true)"
log "SDK      ${ANDROID_HOME} (platforms: $(ls -1 "${ANDROID_HOME}/platforms" 2>/dev/null | tr '\n' ' '))"
log "Gradle   cache at ${GRADLE_USER_HOME:-/gradle-cache}"
log "Running  $*"
printf '\n'

exec "$@"
