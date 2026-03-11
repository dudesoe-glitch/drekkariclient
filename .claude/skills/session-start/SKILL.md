---
name: session-start
description: Initialize a new session by loading project context and confirming priorities
---

# Session Start Protocol

## Steps

1. **Read continuity documents** (in parallel):
   - `HANDOFF.md` — what happened last session, next steps
   - The most recent file in `Diary/` (use Glob on `Diary/*.md`, pick last by name)
   - `MEMORY.md` is auto-loaded — do NOT re-read it

2. **Check git status:**
   - Run `git status --short` to surface any uncommitted work
   - If uncommitted file count > 5, warn: consider committing before starting new work

3. **Verify build:**
   - Check if `ant` is available
   - Optionally run a quick `ant` compile check if the user wants

4. **Summarize to user:**
   - What was completed last session
   - What the next priorities are (from HANDOFF.md)
   - Any uncommitted files
   - Any compilation issues

5. **Wait for user confirmation** before starting any work
