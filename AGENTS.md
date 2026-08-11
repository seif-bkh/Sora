# AGENTS.md — Operating Instructions for Agentic Contributors

This file governs how any agentic coding tool (Claude Code or equivalent) must behave while working on this repository. It applies in addition to, not instead of, the project brief in `PROJECT_BRIEF.md` / the original spec document. If anything here conflicts with the project brief, the process rules in this file take precedence, since they govern *how* work happens rather than *what* is being built.

---

## 1. Plan confirmation is mandatory before execution

Before writing code, running a build, creating files, or making any repository change for a new phase, task, or non-trivial fix:

- Produce a short written plan: what you intend to do, which files/modules will be touched, and any assumptions or decisions you're making.
- Present this plan to the user and explicitly wait for confirmation before proceeding.
- Do not batch multiple phases or unrelated changes into a single plan. One plan per coherent unit of work (e.g., "Phase 3: local library scanning" is one plan; "Phase 3 and Phase 4" is two).
- If the user requests changes to the plan, revise and re-confirm before executing — do not partially execute an unconfirmed plan.
- Small, obviously reversible actions (reading files, searching, inspecting the repo state) do not require confirmation. Anything that writes, deletes, commits, pushes, or modifies CI/build configuration does.
- If a phase's scope shifts materially once you're inside it (e.g., you discover the matching engine needs a schema change), stop and re-confirm rather than silently expanding scope.

## 2. Continuous Integration setup

- Generate and maintain a `.github/workflows/ci.yml` GitHub Actions workflow for this Android project. At minimum it must:
  - Trigger on `push` and `pull_request` to the main branch.
  - Set up JDK (Temurin, version matching the project's Gradle/AGP requirements) and cache Gradle dependencies.
  - Run `./gradlew build` and `./gradlew test` (unit tests) for all modules.
  - Run lint (`./gradlew lint`) and fail the workflow on new lint errors introduced by the change.
  - Run the filename-parser and fuzzy-matching-scorer unit tests explicitly as a named step, since the project brief calls these out as highest-risk components.
  - Cache the Gradle wheel/build cache between runs to keep CI time reasonable.
- Any change to build configuration, module structure, or dependencies must be reflected in `ci.yml` in the same plan/commit — do not let CI config drift from the actual build.
- Present the CI workflow as part of a plan (per Section 1) before committing it, same as any other change.

## 3. Push, monitor, and iterate until green

After committing and pushing any change:

- Push to the remote branch.
- Poll/check the resulting GitHub Actions run status (via `gh run watch`, `gh run list`, or equivalent) until it reaches a terminal state (success or failure). Do not consider a task complete while a triggered workflow run is still pending or in progress.
- If the run fails:
  - Fetch the failure logs, diagnose the root cause, and produce a plan for the fix (per Section 1 — this still requires confirmation before executing, unless the user has explicitly pre-approved autonomous fix-iteration for this session).
  - Apply the fix, commit, push, and re-check the workflow again.
  - Repeat until the workflow succeeds.
- Never mark a phase or task as "done" while the corresponding CI run is red. If a fix isn't obvious after a reasonable number of iterations (e.g., 3 failed attempts on the same error), stop and surface the issue to the user with your diagnosis rather than continuing to guess.
- Do not disable, skip, or weaken a failing test/lint check as a means of making CI pass unless the user explicitly approves that as the fix.

## 4. Skills

- Before starting any new phase or non-trivial task, check whether the user has provided or referenced skills relevant to the work — treat this as part of the planning step in Section 1, not a separate optional check.
- Skills the user provides may cover: Android/Kotlin conventions, Compose UI patterns, Room/migration practices, AniList API usage, CI/CD conventions, git workflow conventions, or anything else relevant to this project. Do not assume the list is limited to what's been mentioned so far — check for newly provided skills each session.
- If a relevant skill exists, follow its guidance over your own default assumptions or general training knowledge, since skills encode the user's specific, hard-won preferences and environment constraints.
- If no relevant skill is found for a given task, proceed using the conventions established elsewhere in this file and the project brief, and note in the plan that no matching skill was found.
- If a skill and an instruction in this file or the project brief conflict, surface the conflict to the user as part of the plan rather than silently picking one.

---

## Summary of the loop for every unit of work

1. Check for relevant user-provided skills.
2. Write a plan (scope, files touched, assumptions).
3. Present the plan and wait for user confirmation.
4. Execute only after confirmation.
5. Commit and push.
6. Watch CI to a terminal state.
7. If red: diagnose, plan the fix (confirm per step 3 unless pre-approved), fix, push, re-check. Repeat until green.
8. Only then consider the unit of work complete.
