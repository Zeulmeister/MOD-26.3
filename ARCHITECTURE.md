# JCraft: Eyes of Ender — Superior 26.3 Edition Architecture

This document summarizes the engineering layout, runtime boundaries, data pipelines, and automated verification gates of **JCraft 26.3** (Fabric / Java 25 / Minecraft 26.3).

---

## 1. Module & Package Boundaries

The project uses a single Fabric Loom 1.19.4 module targeting Minecraft `26.3` (`release = 25`), strictly partitioned by environment boundary:

| Package Root | Environment | Responsibility |
| :--- | :---: | :--- |
| `net.arna.jcraft.api` | Common (`*`) | Public registries (`JStandTypeRegistry`, `JMoveTypeRegistry`, `JItemRegistry`, `JSoundRegistry`, `JMenuRegistry`), combat API (`Attacks`, `IAttacker`, `StandEntity`), server config primitives. |
| `net.arna.jcraft.common` | Common (`*`) | Entities, Stands, Specs, Moves, AI, directional gravity physics (`common.gravity`), `JServerConfig`, packets, events, recipes, and headless server runtime diagnostics (`common.debug`). |
| `net.arna.jcraft.fabric` | Common (`*`) | Fabric entrypoints (`JCraftFabric`), Cardinal Components implementations (`fabric.common.component`), TerraBlender region hook (`JTerraFabric`). |
| `net.arna.jcraft.client` / `net.arna.jcraft.fabric.client` | Client only | AzureLib entity & armor renderers, RenderPearl 26.3 `post_effect` shaders & SkyBox renderer (`SkyBoxManager`), HUDs (`JCraftAbilityHud`, `JoJoAnimeHudRenderer`, `JCraftHudOverlay`), screens (`JCraftConfigScreen`, `StandGuideScreen`, `JoJoRadialWheelScreen`), and GPU visual diagnostic (`ClientVisualDiagnostic`). |
| `net.arna.jcraft.mixin` / `mixin.client` | Split (`jcraft.mixins.json`) | 38 Java 25 Mixins split cleanly between `mixins` (common/server) and `client`. |

### Dedicated-Server Isolation Gate (ASM Bytecode Enforced)
`JCraft263ValidationTest.verifyDedicatedServerIsolation()` scans **every compiled non-client `.class` file** under `build/classes/java/main/net/arna/jcraft` with ASM and fails the build if any common/API class references:
- `net/minecraft/client/`, `com/mojang/blaze3d/`, `com/mojang/renderpearl/`, `org/lwjgl/`
- `net/arna/jcraft/client/`, `net/arna/jcraft/fabric/client/`, `net/arna/jcraft/mixin/client/`, `net/arna/jcraft/api/pose/`
- `net/fabricmc/fabric/api/client/`, `dev/architectury/event/events/client/`, `mod/azure/azurelib/render/`

All client-only visual callbacks cross the boundary via `JCraft.getClientEntityHandler()` (no-op on Dedicated Server) or common `ParticleOptions` (`BreathParticleOptions`, `LockOnParticleOptions`, `MoshParticleOptions`).

---

## 2. Data-Driven Content Pipelines

| Resource Path | Count / Purpose |
| :--- | :--- |
| `data/jcraft/stands/*.json` | 37 Stand definitions (29 playable/obtainable including Crazy Diamond & Chariot Requiem). |
| `data/jcraft/specs/*.json` | 5 Spec definitions (`brawler`, `anubis`, `vampire`, `hamon`, `ranger`). |
| `data/jcraft/evolutions/*.json` | 8 Stand evolution recipes loaded by `EvolutionItemHandler` (with client-side bootstrap for the Stand Guide). |
| `data/jcraft/movesets/**` | 298+ JSON move entries validated against registered `JMoveTypeRegistry` and `JMoveActionTypeRegistry` codecs. |
| `assets/jcraft/post_effect/*.json` + `shaders/post/*.fsh` | 4 native MC 26.3 RenderPearl post-processing shaders (`#version 330`, `layout(std140)` UBOs). |
| `assets/jcraft/lang/{en_us,pt_br}.json` | Complete bilingual localization enforced at exact 1:1 key parity by `:verifyMod`. |

---

## 3. Configuration Architecture

- **Server Settings (`JServerConfig`):** Single source of truth for Balance, Interaction/World, Gameplay, and Gravity rules, synced to clients and persisted to `jcraft.json` / `./config/jconfig.json`.
- **Gravity Unification (`GravityChangerConfig`):** All 6 directional-gravity fields (`keepWorldLook`, `rotationTime`, `worldVelocity`, `resetGravityOnDimensionChange`, `resetGravityOnRespawn`, `voidDamageAboveWorld`) are automatically synchronized via `JServerConfig.syncGravityConfig()`.
- **Client Settings (`JClientConfig`):** Managed via Cloth Config / AutoConfig and exposed in `JCraftConfigScreen` (including `hudScale`, shader toggles, HUD positioning, and keybinds).

---

## 4. Build, Reproducibility & 4-Layer Verification

- **Reproducible JARs:** `build.gradle` configures `preserveFileTimestamps = false` and `reproducibleFileOrder = true` for deterministic SHA-256 artifacts, with `-Xlint:removal` enabled on `JavaCompile`.
- **Gradle Tasks:**
  - `./gradlew build` / `./gradlew check` / `./gradlew test` — compiles with Java 25, validates `jcraft.classtweaker`, builds the self-contained Jar-in-Jar artifact, and runs `:verifyMod`.
  - `./gradlew deployMod -Pjcraft.deploy.targets="path1;path2"` — builds, deploys to the repository root and custom targets, and verifies identical SHA-256 checksums.
- **Verification Layers:**
  1. **Offline Gate (`:verifyMod` / CI):** 3,700+ assertions covering JSON assets, Mixins, ASM Dedicated-Server isolation, physics/combat math, content/balance, UI/UX, and build hygiene.
  2. **Dedicated Server Live Suites (`-Djcraft.selftest.{allstands,sptw,nonstand}=true`):** Boots a real Minecraft 26.3 Fabric Dedicated Server to summon all 29 playable Stands, run live physics/gravity/clock/combat/config assertions, and halt cleanly.
  3. **Real GPU Client Visual Suite (`-Djcraft.selftest.client=true`):** Boots the real graphical MC 26.3 client, renders all 4 RenderPearl shaders, Time Erase/MIH SkyBoxes, C-Moon directional camera/gravity, armor/mask layers, and verifies 9 captured screenshots.
