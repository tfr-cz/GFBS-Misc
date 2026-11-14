# AGENTS.md

Doors-only line of **GFBS : Misc** (Forge 1.20.1, `modid = gfbs_misc`): NTM-style doors plus a ported
T-51b power armour rendered through glTF. The older doors+elevators line lives in another repo; keep
their resources out of this one.

## Pointers

| Question | Read |
|---|---|
| build, test, dependency override | `README.md` |
| architecture, model sizes, render pipeline, known limits | `HANDOVER.md` |
| where the decompiled baseline came from, what was lost | `BASELINE.md` |
| how the baseline was reconstructed, step by step | `MIGRATION.md` |
| model and asset tooling, guardrail scripts | `tools/` |

## Traps that already cost a build

- `EntityRenderersEvent` is a **mod bus** event: subscribe with `bus = Bus.MOD`, or the layer never attaches.
- Every catalog name carries its namespace: `gfbs_doors:armor/step_metal`, never `armor/step_metal`.
- Footstep cadence reads `Entity#moveDist`; `Entity#xo` only moves on teleports.
- `head.visible = glassVisor`: the hazmat visor is glass and keeps the face visible, the closed T-51b helmet hides it.
- T-51b footsteps use `step.metal`; `powered1..3` belong to other suits.
- A key that can hold an empty slot is a `record`, never `List.of(...)` plus a `contains(null)` check.
- Vanilla armour visibility comes from `setPartVisibility`, not from the player model.

## Runtime signals

The game already writes the evidence, just outside this repo:

    python tools/pull_runtime.py      # newest crash report + latest.log tail into .scratch/runtime/

The instance path comes from `--instance`, `$GFBS_INSTANCE`, or `.scratch/runtime.env`; set it once.

## Inspecting a jar (NTM, Forge, glTF)

- `unzip -l <jar> | grep -i <pattern>` finds a path, `unzip -p <jar> <path>` reads that one file.
- Decompile single classes, never a whole jar: `java -jar vineflower.jar -dgs=1 <path/To/Class.class> out/`.
- Find which class mentions a symbol by byte-searching the entries: `needle in data` per entry.
- Scope searches to a directory you already know; a machine-wide `find` costs minutes and answers nothing.

## Session handoffs

Session logs are `.jsonl.zstd` and this machine has no zstd, so a retro on a past session only works from
plain text: leave one at `.scratch/handoff-<date>.md` before closing the session.

## Guardrail

`.githooks/pre-commit` runs the tests plus `tools/compat_check.py` and `tools/null_safety_check.py`.
Enable it once per clone: `git config core.hooksPath .githooks`.
