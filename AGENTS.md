# AGENTS.md

Guidance for coding agents working in this repository.

The full instructions live in [CLAUDE.md](CLAUDE.md) — build and test commands, the localization architecture, the ritual and data flow, and which screenshot failures are real. That file is the single source of truth; this one exists so agents that look for `AGENTS.md` by convention find their way there. Keep the content in `CLAUDE.md` and leave this file as a pointer, so the two cannot drift apart.

The three things most likely to trip an agent up, in short:

1. **Do not override `LocalContext` to localize strings.** `AppLocalization` provides `LocalResources` on purpose; overriding the context detaches the Activity and silently breaks activity-result launchers and fold detection. See CLAUDE.md → *Localization architecture*.
2. **Reach resources only through static `R.string.*` references.** The release build shrinks resources, so dynamic name lookup would be stripped and crash at runtime.
3. **A green `testDebugUnitTest` says nothing about screenshots**, and several screenshot failures are known-stale or flaky rather than regressions. See CLAUDE.md → *Screenshot baselines*.
