# AGENTS.md — Rules of Engagement

This is a binding contract for any agentic coding tool working on this repository (Sora — Android anime/manga client). It contains rules only. Follow every rule on every task, without exception, unless the user explicitly overrides a specific rule in a specific instance.

---

## 1. CI Workflow — Manual Install Only

1. The **first action** on this repository, before any other setup or planning, is to generate a complete `.github/workflows/ci.yml` file and present it to the user as a file for them to add manually.
2. Never attempt to create, push, or commit `.github/workflows/*` directly to the repository via git or any tool — GitHub App permissions restrict agents from writing workflow files, and attempting it will fail or be rejected. Always output the YAML content for the user to copy in themselves.
3. Any future change to CI configuration — new steps, new triggers, dependency/version bumps in the workflow, new jobs — must be delivered the same way: generate the updated YAML, present it, and instruct the user to apply it manually. Never assume a workflow file change has taken effect until the user confirms they've added it.
4. Do not modify, delete, or rename `.github/workflows/*` files directly under any circumstance, even if a task seems to require it. Surface the needed change as an instruction to the user instead.
5. Keep the CI workflow in sync conceptually with the actual build (module list, test targets, lint) — if the project structure changes in a way that would make the existing `ci.yml` stale, flag this immediately and produce the updated YAML for manual replacement, don't let it silently drift.

## 2. Planning & Review — Required Before Any Change

6. Before writing code, creating files, editing files, or running any build/test command, produce a short written plan: what will change, which files/modules are affected, and any assumptions being made.
7. Present the plan to the user and wait for explicit confirmation before executing it. Do not proceed on an unconfirmed plan.
8. One plan per coherent unit of work. Do not bundle unrelated changes or multiple phases into a single plan.
9. If scope shifts materially mid-execution (a discovered dependency, a needed schema change, an unplanned file touch), stop and get re-confirmation rather than expanding scope silently.
10. Read-only actions (viewing files, searching the repo, inspecting state) do not require a plan or confirmation. Anything that writes, deletes, or modifies state does.
11. If the user requests changes to a plan, revise and re-present it — do not partially execute the old version while incorporating new asks.

## 3. Skills

12. Before planning any new task, check whether the user has provided or referenced skills relevant to the work.
13. If a relevant skill exists, its guidance overrides default assumptions or general training knowledge.
14. If a skill conflicts with another rule in this file or with the project brief, surface the conflict to the user as part of the plan — do not silently resolve it.

## 4. Scope Discipline

15. Do not implement anything explicitly marked out-of-scope in the project brief (e.g., torrent streaming) even if it seems like a natural extension of a current task.
16. Do not introduce libraries, frameworks, or architectural patterns not specified in the project brief's tech stack without flagging the substitution and getting confirmation first.
17. Do not perform speculative refactors, "while I'm here" cleanups, or unrelated improvements inside a plan scoped to something else. Raise them as a separate, future plan instead.
18. When a decision is ambiguous but low-cost to reverse, make a reasonable default, document it in `DECISIONS.md`, and continue — don't block progress on trivial ambiguity. When a decision is expensive to reverse (schema shape, module boundaries, sync strategy), stop and ask.

## 5. Code Quality & Safety

19. All network and disk I/O must be off the main thread — no exceptions, enforced via StrictMode in debug builds.
20. No hardcoded secrets, tokens, or API keys in source at any point, including in scratch/debug code.
21. Every commit must leave the repository in a buildable state — do not commit partially-working intermediate states as if they were complete.
22. Write unit tests for the filename parser (including chapter/volume CBZ detection) and the fuzzy-matching scorer as part of the plan that implements them, not as a deferred follow-up.
23. Never weaken, skip, or delete a test or lint rule to make a build pass unless the user explicitly approves it as the fix.

## 6. Git Hygiene

24. Commit messages describe what changed and why, scoped to the same unit of work as the plan that produced them.
25. Do not force-push, rewrite shared history, or delete branches without explicit user instruction.
26. Push only after the user has confirmed the plan and the resulting change is complete and buildable.
27. Never attempt to bypass GitHub App / permission restrictions through alternate tooling, scripts, or workarounds — if an action is restricted, tell the user and let them perform it.

## 7. Communication

28. State assumptions explicitly wherever they're made, in the plan or in `DECISIONS.md` — never leave an undocumented judgment call.
29. If uncertain whether a rule applies to a given situation, default to asking rather than guessing.
30. Never mark a task, phase, or plan as complete while any part of it — CI status the user hasn't confirmed, an unconfirmed plan step, an unresolved ambiguity — is still open.
