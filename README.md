# JCraft: Eyes of Ender — Superior 26.3 Edition (Fabric)

Recriação completa e aprimorada do **JCraft (JoJo's Bizarre Adventure)** para o **Minecraft Java Edition 26.3 (Fabric / Java 25)**, preservando **100% do conteúdo original** e elevando combate, gravidade direcional, shaders RenderPearl 26.3, arte final, segurança Dedicated-Server e interface para o padrão nativo da 26.3.

---

## Destaques da Edição 26.3 ("Superior Edition")

### 1. Combate & Game Feel
- **Física Cinética no Tempo Parado**: Projéteis disparados durante o *Time Stop* percorrem ~1.8 blocos, desaceleram até ficarem suspensos no ar com faíscas cinéticas (`ゴ`), acumulam **+25% de momento** e disparam juntos ao retomar o tempo.
- **Micro-Hitstop Configurável (`ENABLE_HITSTOP`)**: Congelamento de impacto seguro (1–4 ticks em golpes pesados/ultimates `>= 6.0` de dano, sem atrapalhar barrages e sem nunca sobrescrever *Time Stop* ativo).
- **Smart Combat Input Buffer (6 Ticks)** + **Combos Determinísticos de Hamon** (`HamonMoveStep`) + **Atributo Transiente `ATTACK_SPEED`** (sem spam de Mining Fatigue no usuário).
- **IA Tática de Stands e NPCs**: Estado neutro consciente de contexto (*whiff-punish*, *pushblock*, flanqueamento) e raycast de colisor na largura do corpo para quebrar cercas/vidros automaticamente com Barrage.
- **Resync Suave no Made in Heaven**: Aceleração real do relógio do Overworld com interpolação cúbica *ease-out* ao encerrar a habilidade.

### 2. Gravidade Direcional C-Moon & Apresentação Visual RenderPearl 26.3
- **Gravidade Direcional Real (6 direções)**: Hitbox, posição dos olhos, câmera rotacionada, dano de queda direcional, step-up em paredes/tetos e sincronização total de `GravityChangerConfig` via `JServerConfig`.
- **4 Shaders Nativos RenderPearl 26.3 (`#version 330` / `std140`)**: `za_warudo`, `time_erase`, `bites_the_dust` e `color_shift`.
- **SkyBoxes Nativos (`SkyBoxManager` + `SkyRendererMixin`)**: Céu dimensional estrelado no *Time Erase* (King Crimson) e rastros celestes/ecos de sol e lua no *Made in Heaven*.
- **Arte Final Dedicada**: 88 ícones 18×18 exclusivos, 28 poses distintas de usuário de Stand, corona emissiva de dupla passagem em *The Sun*, armaduras em `SpecUserMob` e texturas humanoides 64×32 para máscaras/chapéus.

### 3. Conteúdo Completo & Balanceamento
- **29 Stands Jogáveis e Obtíveis** (37 definições no registro), incluindo os novos **Crazy Diamond** (obtível via Flecha Stand, restauração de blocos, cura de aliados em Co-op, *Wall Breaker* e *Bearing Snap*) e **Chariot Requiem** (evolução exclusiva de *Silver Chariot + Flecha Requiem* com *Somnolence*, *Soul Swap*, contra-ataque *Stand Rebellion*, *Requiem Dominion* e *Shadow Step*).
- **5 Specs** (*Brawler*, *Anubis*, *Vampire*, *Hamon*, *Ranger*), **8 Evoluções**, **298+ Movesets** data-driven, **223 Itens**, dimensão *AU* e trocas data-driven da 26.3.
- **Todas as Opções de Servidor Expostas**: `CMOON_UTIL_DURATION`, `CMOON_ULT_RANGE`, `MIH_ACCELERATE_TICKS`, `SHA_SEARCH_RADIUS`, `USE_FOOLISH_SAND`, `UNIVERSAL_ABILITIES`, `SPTW_IGNITE_CAMPFIRES`, `ENABLE_HITSTOP`, `RESET_GRAVITY_ON_RESPAWN` e `VOID_DAMAGE_ABOVE_WORLD`.
- **Voo Condicional Balanceado em Survival**: Voo permitido apenas durante *Time Stop* ativo ou voo do *Gold Experience Requiem*, com revogação segura ao término.

### 4. UI, HUD & UX Polidos
- **Barra de Habilidades Escalável (`hudScale` 0.75x–1.50x)**: Destaque em tempo real do golpe ativo, micro-indicadores `↓` (agachado) e `↑` (aéreo) para variantes desbloqueadas, pílulas de contagem para *Za Warudo*, *Time Erase* e *Made in Heaven* e respeito automático a `UNIVERSAL_ABILITIES`.
- **Roda Radial Dinâmica (`Y`)**: Cabeçalho com o Stand equipado, nomes reais das skins, marcação da skin ativa (`✓`), desabilitação visual de slots inexistentes, animação suave e atalhos `1–8` + `G`.
- **Enciclopédia & Guia de Stands (`StandGuideScreen`)**: Consulta de todos os 29 Stands obtíveis (`◀ ▶`), prós/contras, variantes, `WINDUP`, `Armor`, `Unblockable`, evoluções e progressão.
- **Tradução Completa EN-US & PT-BR** com paridade 1:1 de chaves.

---

## Atalhos Principais Padrão

| Tecla | Ação |
| :---: | :--- |
| `V` | Invocar / Recolher Stand |
| `R` | Heavy Attack |
| `B` | Barrage (segurar; soltar cancela instantaneamente) |
| `H` | Ultimate / Time Stop (pressione novamente para retomar o tempo cedo) |
| `G` / `J` / `K` | Specials 1, 2 e 3 |
| `U` | Utility / Mobilidade |
| `O` | Modo Piloto Universal (câmera e voo 3D no Stand até 36m) |
| `Z` | Visão de Precisão 5x (Segurar) |
| `P` | JoJo Pose Taunt |
| `Y` | Roda Radial Rápida JoJo (Skins, Pose, Piloto, HUD, Guia e Configurações) |

---

## Instalação (Arquivo `.jar` Único Pronto para Jogar)

1. Instale o **Minecraft Java Edition 26.3** com **Fabric Loader `>= 0.18.0`** (Java 25).
2. Baixe **`jcraft-fabric-26.3.jar`** — todas as dependências (Fabric API, Architectury, AzureLib, Player Animation Library, Cardinal Components, Cloth Config e TerraBlender) já vão embutidas via Jar-in-Jar.
3. Coloque `jcraft-fabric-26.3.jar` na pasta `.minecraft/mods/` e inicie o jogo.

---

## Compilando, Validando e Arquitetura

Requer **JDK 25+** (ex.: Eclipse Temurin 25 / OpenJDK 26):

```bash
# Compila, valida AccessWidener/Mixins, roda :verifyMod (incluindo isolamento Dedicated-Server) e gera JAR reproduzível
./gradlew clean build

# Compila, copia para a raiz (e destinos opcionais separados por ';') e valida SHA-256
./gradlew deployMod
```

Consulte [ARCHITECTURE.md](ARCHITECTURE.md) para detalhes de pacotes, pipelines data-driven, isolamento Dedicated-Server e camadas de teste.
