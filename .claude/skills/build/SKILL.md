---
name: build
description: "Build and optionally run the Hurricane client using Apache Ant"
argument-hint: "[run|bin|clean]"
---

# Build Hurricane Client

## Steps

1. **Parse `$ARGUMENTS`:**
   - `run` (default if empty) — compile and launch the client
   - `bin` — compile and stage distribution (no launch)
   - `clean` — remove build artifacts

2. **Run the build:**
   ```bash
   cd C:/Users/why_t/Hurricane && ant <target>
   ```

3. **Check output** for compilation errors:
   - Java compilation errors show file:line format
   - Missing dependency errors indicate `lib/` issues
   - Report any errors with the affected file and line number

4. **Report result:**
   - Success: "Build succeeded — N files compiled"
   - Failure: list each error with file path and line number
