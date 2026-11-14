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

## Guardrail

`.githooks/pre-commit` runs the tests plus `tools/compat_check.py` and `tools/null_safety_check.py`.
Enable it once per clone: `git config core.hooksPath .githooks`.
