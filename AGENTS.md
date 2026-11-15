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
- A mixin that names a missing target aborts startup: gate it with `IMixinConfigPlugin#shouldApplyMixin`, because
  `gfbs_gltf` 1.6 classes (`GltfUploadedTexture`) do not exist on 1.5.x.
- The mixin config only loads through `MixinConfigs` in the jar manifest, and without MixinGradle a dev run never
  applies it — judge the mixin from the built jar, not from `runClient`.
- `AuralisBackend` / `GltfCompat` resolve every method in one all-or-nothing `try`: one renamed method silently drops
  the whole feature (it falls back to the vanilla path). Check the target jar with `javap -p` before trusting it.
- Auralis bypasses the vanilla `BLOCKS` mix, so its volume must be `base * BLOCKS * MASTER`; vanilla only needs `base`.
- An `ItemStack` cannot be created in a unit test on this machine: `Bootstrap.bootStrap()` dies inside Forge's
  `NetworkHooks.init`. Keep the logic worth testing in pure helpers, and gate item-level tests behind
  `assumeTrue(TestBootstrap.itemsAvailable())`.

## Runtime signals

The game already writes the evidence, just outside this repo:

    python tools/pull_runtime.py      # newest crash report + latest.log tail into .scratch/runtime/

The instance path comes from `--instance`, `$GFBS_INSTANCE`, or `.scratch/runtime.env`; set it once.

## When `git push` cannot reach GitHub

On this machine git's schannel backend fails the TLS revocation check (`CRYPT_E_NO_REVOCATION_CHECK`) and
`http.schannelCheckRevoke=false` does not help; `curl --ssl-no-revoke` and `gh api` both still work. Run
`python tools/push_gitdata.py` (add `--dry-run` to see the plan): it uploads only the objects in
`git rev-list --objects <remote>..HEAD`, rebuilds the trees child-first, replays each commit with its original
identity and dates, then fast-forwards the ref. Object ids are content addressed, so a correct run reproduces
the local SHAs exactly and the script aborts if any object comes back different.

Two things that will bite if you edit it: `POST /git/trees` rejects a reference to a tree that does not exist
yet, and the root tree's path is the empty string, so sorting trees by slash count alone puts it first by
mistake — `depth()` pushes it to the end. Afterwards confirm `git rev-parse HEAD` equals
`gh api repos/<slug>/git/ref/heads/master --jq .object.sha`; the ref is only moved once every object landed.

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
