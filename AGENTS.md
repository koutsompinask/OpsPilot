# AGENTS.md

## Purpose
Defines how Codex agents should operate in this repository for implementation and review work.

## Session Startup (Always)
1. Read `plan.md` first for project scope, architecture, and current target phase.
2. Read this `AGENTS.md` before making changes.
3. If present, read `tasks.md` to continue work without losing context.

## Plan-First Workflow
1. Use a plan for any non-trivial task (3+ steps, cross-service changes, or architectural decisions).
2. If a task derails, stop and re-plan before continuing.
3. Include verification steps in the plan (tests, builds, smoke checks), not only implementation steps.

## Task Tracking (`tasks.md`)
Maintain a root-level `tasks.md` during execution.

Required sections:
- `## Pending` (identified work not started yet)
- `## To-Do` (active plan for the current task/session)
- `## Done` (completed and verified items)

Rules:
1. Update `tasks.md` at start, during progress, and before final handoff.
2. Move items between sections instead of duplicating them.
3. Mark an item `Done` only after verification evidence (test/build/log output).
4. Keep entries short, actionable, and checkable (`- [ ]` / `- [x]`).

## Execution Principles
1. Simplicity first: choose the smallest change that solves the root cause.
2. Minimal impact: touch only required files and preserve existing behavior unless change is intentional.
3. No temporary hacks: prefer durable fixes over quick patches.
4. Autonomous bug fixing: when given a bug, diagnose and fix end-to-end without unnecessary user hand-holding.

## Verification Before Completion
1. Never mark work complete without proving it works.
2. Run relevant tests and/or build checks for changed components.
3. Validate behavior against expected outcomes from `plan.md` and task requirements.
4. Document what was verified in `tasks.md` (brief review note under `Done` or a short `Review` subsection).

## Quality Bar
1. For non-trivial changes, quickly challenge the approach for a cleaner alternative before finalizing.
2. Ask: "Would a staff engineer approve this change?"
3. If the answer is no, iterate once more before handoff.

## Continuous Improvement
1. After user corrections, update `tasks/lessons.md` with:
   - what went wrong
   - preventive rule
   - how to detect it earlier next time
2. Review relevant lessons at the start of similar future tasks.
