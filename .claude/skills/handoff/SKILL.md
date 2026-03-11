---
name: handoff
description: "Write HANDOFF.md for session continuity before clearing context"
disable-model-invocation: true
---

# Handoff Protocol

Write `HANDOFF.md` at project root with this structure:

## Template

```markdown
# HANDOFF — Session N

## Resumption Prompt
(One paragraph: what happened, current state, what to do next)

## Goal
(What we were working on this session)

## Completed
- File changes with paths
- Features implemented
- Bugs fixed

## In Progress
- Partial work and current state

## Blockers / Open Issues
- Anything preventing progress

## Next Priorities
1. Most important next task
2. Second priority
3. etc.

## Key Decisions
| Topic | Decision | Rationale |
|-------|----------|-----------|

## Key Files
| File | What |
|------|------|
```

Write it IMMEDIATELY — do not update other files first.
