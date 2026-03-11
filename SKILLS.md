# Hurricane Skills Reference

Skills are specialized workflows invoked with `/skill-name`.

---

## When to Use Skills

| Situation | Skill to Use |
|-----------|--------------|
| Starting a new session | `/session-start` |
| Building/running the client | `/build` |
| Creating a new bot | `/bot-scaffold` |
| Ending a session | `/wrap-up` |
| Creating session handoff | `/handoff` |

---

## Skill Descriptions

### `/build`

**Build and optionally run the Hurricane client.**

```
/build         — compile and launch (ant run)
/build bin     — compile only (ant bin)
/build clean   — remove build artifacts (ant clean)
```

---

### `/bot-scaffold`

**Create a new bot class** from the established Window+Runnable template.

- Generates `src/haven/automated/<BotName>.java` following STANDARDS.md template
- Includes all required patterns: safety checks, cleanup, position save, thread management
- Shows GameUI registration instructions

```
/bot-scaffold FarmingBot
```

---

### `/session-start`

**Initialize a new session** by loading HANDOFF.md, recent diary, and checking git status.

---

### `/wrap-up`

**End-of-session checklist:** compile check, commit, write HANDOFF.md and diary entry.

---

### `/handoff`

**Write HANDOFF.md** for session continuity before clearing context.

---

## Combining Skills

**New Bot Feature:**
1. `/bot-scaffold` — Generate skeleton
2. Implement bot logic
3. `/build` — Verify compilation
4. `/wrap-up` — Commit and document

**New Session:**
1. `/session-start` — Load context
2. Work on priorities from HANDOFF.md
3. `/wrap-up` — Ship and persist
