# AGENTS.md

Doors-only line of **GFBS : Misc** (Forge 1.20.1, `modid = gfbs_misc`): NTM-style doors plus ported HBM gear -
the hazmat suit, the T-51b power armour (OBJ through `PowerArmorLayer`), the Euphemium "Ep" set (plain vanilla
armour layer) and the Crucible sword (OBJ through a `BlockEntityWithoutLevelRenderer`). The older doors+elevators
line lives in another repo; keep their resources out of this one.

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
- Merely *touching* an `Item` class needs those registries too (`Item` implements `FeatureElement`, which trips
  `Registries` and then `BuiltInRegistries`), so a test that calls a static helper on an armour item must bootstrap
  itself. JUnit's class order is not stable: adding one test class once flipped the order and stranded a test that
  had been living off its neighbour's bootstrap.
- An armour texture comes from `ForgeHooksClient.getArmorTexture`, which asks `Item#getArmorTexture` and only falls
  back to `textures/models/armor/<material>_layer_N.png` when that returns null. That is why the per-piece sheets
  (hazmat, Euphemium) work despite the vanilla default being one sheet per material.
- A mesh item needs `{"parent": "builtin/entity"}` in its item model plus `IClientItemExtensions#getCustomRenderer`;
  Forge then skips the baked quads and calls `BlockEntityWithoutLevelRenderer#renderByItem` instead. On that path
  the JSON `display` block is **not** applied and the pose arrives with the item cube's near corner at the origin,
  which is why every such renderer starts with `translate(0.5, 0.5, 0.5)` and owns the rest of the transform.
- glTF/OBJ meshes and their textures are imported by scripts under `tools/` and loaded through
  `ClientGltfApi.models().load(...)`; a missing `usemtl` in the source OBJ means no material, so the import script
  has to add one.

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
