---
name: wrap-up
description: >
  End-of-session checklist for committing, documenting, and persisting progress.
  Use when ending a session or when asked to wrap up.
---

# Session Wrap-Up

## Phase 1: Ship It

1. **Check compilation:**
   - If Java files were modified, run `ant` to verify clean compilation
   - Fix any errors before committing

2. **Commit changes:**
   - Run `git status` to see all uncommitted changes
   - Run `git diff` to review changes
   - Stage and commit with descriptive message
   - Ask user before pushing

## Phase 2: Persist It

3. **Write HANDOFF.md:**
   - Resumption prompt (one paragraph)
   - Completed work (specific files)
   - In progress / blockers
   - Next priorities

4. **Write Diary entry:**
   - Create `Diary/YYYY-MM-DD.md` (or append session number if one exists)
   - What we did, what we talked about, texture of the session, for next time

## Phase 3: Remember It

5. **Update memory if needed:**
   - New patterns discovered → update MEMORY.md
   - New gotchas → update MEMORY.md
   - New H&H resource names learned → add to STANDARDS.md resource table

## Report

```
## Wrap-Up Report
- Compiled: PASS/FAIL
- Committed: [hash] — [message]
- HANDOFF.md: Written
- Diary: Written to [path]
- Memory updates: [list or "none"]
```
