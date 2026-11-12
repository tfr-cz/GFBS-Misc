# Third-Party Assets

## HBM's Nuclear Tech Mod (NTM)

- Source: https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT
- Version referenced: 1.0.27
- License: GNU Lesser General Public License v3.0

The following assets were ported from NTM and remain subject to NTM's license:

- Door geometry (OBJ): `pheodoors/*.obj`, `doors/silo_hatch*.obj`
- Door textures: `textures/models/pheodoors/**`, `textures/models/doors/*.png`
- Door sounds: `sounds/block/door/*.ogg`, `sounds/block/vault*.ogg`
- Transition Seal model and animation (converted from `models/doors/seal.dae`)
- Cargo elevator geometry and texture

The PHEO door models and textures are the work of their original NTM contributors.
The Transition Seal Collada model is credited to its original author in NTM.

## HBM's Nuclear Tech Mod - Cursed Addon

- Source: https://github.com/ntmleafia/NTM-Cursed-Addon
- License: PolyForm Shield License 1.0.0
- Author: Leafia

The `gfbs_doors:reactor_door` port (model, textures, sounds) comes from the Cursed
Addon and is included with the author's explicit permission:

- Model: `decoration/doors/reactordoor/reactordoorfinal.obj`
- Textures: `decoration/doors/reactordoor/reactordoor*.png`
- Sounds: `sounds/external/reactordoor_{open,handle,close}.ogg`
- Behaviour/animation reference: `ReactorDoorRender`, `ReactorDoorTE`,
  `ReactorDoorBlock`, `AddonDoorDecl.REACTOR_DOOR`

## GFBS: glTF

- Source: https://github.com/LytharaLab/GFBS-glTF
- License: MIT
- Used as a runtime dependency for OBJ rendering. It is not bundled in this repository.
