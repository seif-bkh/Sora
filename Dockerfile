# syntax=docker/dockerfile:1
#
# ---------------------------------------------------------------------------
# Sora — local Android build environment
#
# Produces a debug APK without installing a JDK or the Android SDK on the host.
#
#   docker compose build
#   docker compose run --rm build
#   -> app/build/outputs/apk/debug/app-debug.apk
#
# See docker/README.md for the full walkthrough.
# ---------------------------------------------------------------------------

# JDK 17 is a hard requirement of AGP 8.13 (see gradle/libs.versions.toml).
# Newer JDKs are NOT a drop-in: AGP validates the JDK version and Kotlin's
# jvmTarget is pinned to 17 by the convention plugins.
FROM eclipse-temurin:17-jdk AS base

# --- Android SDK ------------------------------------------------------------
# Pinned rather than "latest" so the image is reproducible: an unpinned SDK
# would silently change under you between rebuilds.
ARG ANDROID_CMDLINE_TOOLS_VERSION=15859902
ARG ANDROID_PLATFORM=36
# AGP 8.13's DEFAULT build-tools is 35.0.0, while compileSdk is 36. Both are
# installed: if only 36.0.0 were present, the first build would stall silently
# auto-downloading 35.0.0.
ARG ANDROID_BUILD_TOOLS=36.0.0
ARG ANDROID_BUILD_TOOLS_FALLBACK=35.0.0

ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    DEBIAN_FRONTEND=noninteractive

# Docker's default RUN shell is /bin/sh (dash), which rejects `set -o pipefail`
# with "Illegal option". The sdkmanager --licenses step below needs it, so use
# bash for every RUN.
SHELL ["/bin/bash", "-o", "pipefail", "-c"]

# unzip/curl are needed to fetch the SDK; git is included because Gradle and
# some plugins shell out to it for version metadata.
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        curl \
        unzip \
        git \
        ca-certificates \
    && rm -rf /var/lib/apt/lists/*

RUN set -eux; \
    mkdir -p "${ANDROID_HOME}/cmdline-tools"; \
    curl -fsSL -o /tmp/cmdline-tools.zip \
        "https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_CMDLINE_TOOLS_VERSION}_latest.zip"; \
    unzip -q /tmp/cmdline-tools.zip -d "${ANDROID_HOME}/cmdline-tools"; \
    # The archive unpacks to `cmdline-tools/`; sdkmanager requires it to sit at
    # `cmdline-tools/latest/` or it cannot locate its own SDK root.
    mv "${ANDROID_HOME}/cmdline-tools/cmdline-tools" "${ANDROID_HOME}/cmdline-tools/latest"; \
    rm /tmp/cmdline-tools.zip

ENV PATH="${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools"

# Accepting licences at image-build time keeps `docker compose run` from
# failing on an interactive prompt. `yes |` breaks the pipe once sdkmanager
# stops reading, so pipefail is disabled for this line only.
RUN set -eux; \
    set +o pipefail; \
    yes | sdkmanager --licenses > /dev/null; \
    set -o pipefail; \
    sdkmanager --install \
        "platform-tools" \
        "platforms;android-${ANDROID_PLATFORM}" \
        "build-tools;${ANDROID_BUILD_TOOLS}" \
        "build-tools;${ANDROID_BUILD_TOOLS_FALLBACK}" \
        > /dev/null

# --- Non-root user ----------------------------------------------------------
# The repository is bind-mounted, so anything the build writes (build/,
# .gradle/) lands on the host with the container user's ownership. Running as
# root would leave root-owned directories your IDE cannot delete. Override
# these to match your own account:
#   docker compose build --build-arg USER_ID=$(id -u) --build-arg GROUP_ID=$(id -g)
ARG USER_ID=1000
ARG GROUP_ID=1000
ARG USERNAME=builder

RUN set -eux; \
    # Reuse the group/user if the id already exists in the base image.
    if ! getent group "${GROUP_ID}" > /dev/null; then \
        groupadd -g "${GROUP_ID}" "${USERNAME}"; \
    fi; \
    if ! getent passwd "${USER_ID}" > /dev/null; then \
        useradd -m -u "${USER_ID}" -g "${GROUP_ID}" -s /bin/bash "${USERNAME}"; \
    fi; \
    HOME_DIR="$(getent passwd "${USER_ID}" | cut -d: -f6)"; \
    mkdir -p "${HOME_DIR}/.gradle"; \
    chown -R "${USER_ID}:${GROUP_ID}" "${HOME_DIR}" "${ANDROID_HOME}"

USER ${USER_ID}:${GROUP_ID}

ENV GRADLE_USER_HOME=/gradle-cache
# Daemon off: the container is short-lived, so a daemon only costs startup
# memory. Configuration cache and workers still apply.
ENV GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx4g -Dorg.gradle.daemon=false"

# --- Pre-warm the Gradle distribution ---------------------------------------
# Copying just the wrapper and running it downloads the ~150 MB Gradle
# distribution into the image, so the first `docker compose run` does not stall
# on it. Docker seeds an EMPTY NAMED VOLUME from the image's directory
# contents, which is why this survives the /gradle-cache volume mount (it would
# NOT survive a bind mount there).
USER root
RUN mkdir -p /gradle-cache && chown -R "${USER_ID}:${GROUP_ID}" /gradle-cache
USER ${USER_ID}:${GROUP_ID}

WORKDIR /workspace
COPY --chown=${USER_ID}:${GROUP_ID} gradle/wrapper /workspace/gradle/wrapper
COPY --chown=${USER_ID}:${GROUP_ID} gradlew /workspace/gradlew
RUN ./gradlew --version > /dev/null

COPY --chown=${USER_ID}:${GROUP_ID} docker/entrypoint.sh /usr/local/bin/sora-entrypoint
# Belt and braces: git checkouts do not always preserve the executable bit.
USER root
RUN chmod +x /usr/local/bin/sora-entrypoint
USER ${USER_ID}:${GROUP_ID}

ENTRYPOINT ["/usr/local/bin/sora-entrypoint"]
CMD ["./gradlew", "assembleDebug"]
