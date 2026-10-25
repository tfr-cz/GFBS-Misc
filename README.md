# GFBS: Misc

A GFBS addon for Minecraft Forge 1.20.1 that bundles the animated industrial doors and cargo elevator ported from HBM's Nuclear Tech Mod together with a channel-based cabin elevator.

## Requirements

- Minecraft 1.20.1
- Forge 47.x
- GFBS: glTF 1.5.x (`gfbs_gltf`, required)
- GFBS: Main 2.x (`gfbs_main`, optional — only used to follow its gate commands)

## Included content

### Doors (ported from NTM)

| Block | Notes |
| --- | --- |
| Vault-Tec Blast Door, Sliding Blast Door, Fire Door, Transition Seal, Silo Hatches, Secure Access Door, Large Vehicle Door, QE doors, Round Airlock Door, Sliding Seal Door, Water Door, Cargo Door, legacy Blast Door | Animated multi-block doors; skins, sounds and collision follow the 1.7.10 originals |

Every door has a configurable redstone mode and manual-control switch, edited with the in-game **Door Configurator**:

- **No redstone** — ignores signals entirely.
- **Pulse** — every rising edge toggles the door.
- **Trigger** — open while powered, closes when the signal ends.
- **Inverted** — closed while powered, opens when the signal ends.
- **GFBS Closed / GFBS Open** — command-only modes; redstone and right-click are ignored. Both follow GFBS: Main's global gate state: `GFBS Closed` behaves like `/GFBsMain` `gate` (facility normally-closed blast doors) and `GFBS Open` like `check_point_gate` (checkpoint normally-open doors). Use the native `/GFBsMain gate open|close gate|check_point_gate|all` commands.

Legacy 1x7 blast doors placed in a contiguous row with the same facing toggle and follow redstone as one group; editing one member's configuration applies to the whole group, and newly placed members inherit it.

### Cargo elevator

3x3 lifting platform (2 blocks/second). Right-click toggles it; right-click with a cargo elevator item while not sneaking adds a layer (up to 64). Riders are carried by the platform; entities and players stand on it while it moves.

### Cabin elevator

- **Controller** — one controller per floor; controllers sharing a channel and dimension form one elevator. Creative mode opens the configuration GUI (range, channel, floor name, time per floor).
- **Caller** — hall call bound to one controller; can also be triggered by a redstone pulse.
- **Redstone interface** — fires sequenced redstone signals after arrival (priority order, per-interface duration/strength/output faces, next-stage delay). Interfaces of one priority may share that priority and fire together.
- **Linker / Range selector** — bind interfaces and callers, and pick the free-form teleport range from two corners (the controller does not need to be centred).

## Namespaces and compatibility

All registered blocks, items and assets keep the original namespaces (`gfbs_doors:` for doors, `cabin_elevator:` for the elevator), so worlds created with the separate mods keep their blocks. **Do not install this jar next to the old `gfbs_doors` or `cabin_elevator` jars** — duplicate registry ids crash startup.

## Building

1. Install JDK 17.
2. Run `./gradlew build --offline` (the Gradle caches on the development machine are populated; `tools/forge_proxy.py` documents the offline dependency workaround).

Notes:

- GFBS: glTF is declared through `fg.deobf` and resolved from the local Maven repository at `G:/qoder/work/gfbs/local-maven`; adjust `build.gradle` if your checkout lives elsewhere.
- `libs/gfbs_main_api_stub.jar` is a compile-only slice of GFBS: Main's public gate API used to compile the optional gate integration. The full GFBS: Main jar is not bundled.
- The built jar is `build/libs/gfbs-misc-0.1.0.jar`.

## License and credits

The code is licensed under the GNU Lesser General Public License v3.0. Door models, textures and sounds are ported from HBM's Nuclear Tech Mod and remain subject to its license. See `LICENSE`, `LICENSE.LESSER`, `NOTICE` and `THIRD_PARTY_ASSETS.md`.

GFBS: glTF is MIT licensed and is a runtime dependency, not bundled with this mod.
