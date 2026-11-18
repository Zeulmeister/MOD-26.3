# JCraft: Eyes of Ender — Superior 26.3 Edition (Fabric)

Recriação completa e aprimorada do **JCraft (JoJo's Bizarre Adventure)** para o **Minecraft Java Edition 26.3 (Fabric / Java 25)**, preservando **100% do conteúdo original e todas as customizações da versão anterior**, além de elevar o combate, a física, os shaders e a interface para um nível superior nativo da 26.3.

---

## Destaques e Melhorias Exclusivas da Edição 26.3 ("Nível Superior")

### 1. Combate Superior & Física Cinética no Time Stop
- **Física Cinética de Projéteis no Tempo Parado**: Facas, escalpelos, balas e projéteis lançados durante o *Time Stop* viajam ~1.8 blocos à frente, desaceleram suavemente até ficarem suspensos no ar com faíscas cinéticas (`ゴ`), acumulam **+25% de momento cinético** e disparam simultaneamente quando o tempo volta a correr.
- **Smart Combat Input Buffer (6 Ticks)**: Captura comandos de ataque pressionados nos últimos 6 ticks de recuperação/stun e executa automaticamente no *frame 0* da recuperação, garantindo combos fluidos estilo anime.
- **Raycast 3D Calibrado & Mineração 3D no Barrage**: Quebra de blocos precisa na mira 3D até 4.5m–5.0m (incluindo olhando para cima/baixo) e cancelamento instantâneo do Barrage ao soltar a tecla.
- **IA Aprimorada de Stands e NPCs**: *Whiff-punish* reativo e uso inteligente de *Pushblock* em NPCs usuários de Stand.

### 2. HUD Estilo Anime Moderno + Roda Radial Rápida (`Y`) + Tela de Configuração In-Game
- **HUD Estilo Anime JoJo**: Cartão estilizado do Stand ativo, barra de Guarda com alerta de quebra, cronômetro dramático de *ZA WARUDO* com contador de projéteis cinéticos suspensos, telemetria de eficiência do Modo Piloto (36m) e alerta vermelho caso seu corpo físico sofra dano enquanto pilota o Stand.
- **Roda Radial Interativa de 8 Fatias (`Y`)**: Troca instantânea de Skins do Stand (`Default`, `Skin 1`, `Skin 2`, `Skin 3`), *JoJo Pose Taunt*, alternância do *Modo Piloto*, alternância do *Anime HUD* e acesso direto às configurações.
- **Tela de Configuração In-Game (`JCraftConfigScreen` + ModMenu)**: Ajuste rápido de todas as opções visuais, shaders, HUD e buffer de combate sem precisar editar arquivos manualmente.

### 3. Shaders Nativos RenderPearl 26.3 (`post_effect`) & Onomatopeias Dinâmicas de Mangá
- **Pipeline Nativo RenderPearl GLSL `#version 330` (`std140` UBO)**:
  - `za_warudo`: Inversão cromática + dessaturação radial durante o Tempo Parado.
  - `time_erase`: Distorção carmesim espacial durante o *Time Erase* do King Crimson / Epitaph.
  - `bites_the_dust`: Aberração cromática temporal no *Rewind* do Mandom / Killer Queen: Bites the Dust.
  - `color_shift`: Troca dramática de paleta de cores estilo anime da David Production em momentos de clímax.
- **Onomatopeias Dinâmicas (`ゴゴゴ` / `ドドド`)**: Katakanas animados flutuantes em poses, invocações, barrages e *Time Stop*.

---

## Funcionalidades Customizadas Preservadas Integralmente
- **Time Stop Aprimorado**: Esfera dinâmica de 160 blocos acompanhando o usuário em movimento, pose constante de levitação estilo JoJo durante voo no *Time Stop*, silenciamento de sons ambientais externos durante o tempo parado, imunidade completa do usuário aos bloqueios de ação no próprio *Time Stop*, e retomada antecipada do tempo pressionando a tecla de Ultimate novamente (`resumeTimeEarly`).
- **Voo Livre Condicional em Survival**: Voo livre automático sempre que qualquer Stand estiver invocado (ou durante *Time Stop* ativo), com revogação limpa e segura ao recolher o Stand.
- **Modo Piloto Universal (`O`)**: Qualquer Stand pode ser pilotado remotamente em voo 3D até **36 blocos** com câmera na perspectiva do Stand, controle completo pelo mouse (`EntityClientTurnMixin`), eficiência total até 12m (redução gradual até 50% aos 36m) e proteção de *Resistance* no corpo do usuário.
- **Visão de Precisão Star Platinum (`Z`)**: Zoom óptico suave de **5x** (`0.20x` FOV com curva cúbica *ease-out*).
- **JoJo Pose Taunt (`P`)**: Pose dramática com trilha sonora e aura de partículas `ゴ`.
- **100% do Conteúdo JCraft**:
  - **37 Stands** completos (incluindo *Star Platinum*, *SP: The World*, *The World*, *TWOH*, *Silver Chariot*, *Magician's Red*, *The Fool*, *Hierophant Green*, *Cream*, *The Sun*, *Osiris*, *Atum*, *Horus*, *Shadow The World*, *Killer Queen*, *KQ: Bites the Dust*, *The Hand*, *Red Hot Chili Pepper*, *Bad Company*, *Gold Experience*, *GER*, *King Crimson*, *Metallica*, *Purple Haze*, *Purple Haze Distortion*, *Chariot Requiem*, *Aerosmith*, *WhiteSnake*, *C-Moon*, *Made in Heaven*, *Goo Goo Dolls*, *Foo Fighters*, *Diver Down*, *Weather Report*, *D4C*, *Tusk Act 1–4*, *Mandom*, *Cinderella*, *Mandandom*).
  - **5 Specs** (*Brawler*, *Anubis*, *Vampire*, *Hamon*, *Ranger*).
  - **7 Evoluções**, **281 Movesets** (com todos os `"is_holdable": true` e `canCancel: true` preservados), **223 Itens**, armaduras/roupas 3D, projéteis, estruturas de geração de mundo, dimensão *AU* e trocas de aldeões data-driven da 26.3.

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
| `Y` | Roda Radial Rápida JoJo (Skins, Pose, Piloto, HUD e Configurações) |

---

## Instalação (Arquivo `.jar` Único Pronto para Jogar)

1. Instale o **Minecraft Java Edition 26.3** com **Fabric Loader `>= 0.18.0`** (Java 25).
2. Baixe o arquivo **`jcraft-fabric-26.3.jar`** (na aba **Releases** ou na raiz do repositório) — **todas as bibliotecas necessárias (Fabric API, Architectury, AzureLib, Player Animation Library, Cardinal Components, Cloth Config e TerraBlender) já estão embutidas via Jar-in-Jar!**
3. Coloque `jcraft-fabric-26.3.jar` na pasta `.minecraft/mods/` e inicie o jogo.

---

## Compilando a partir do Código-Fonte

Requer **JDK 25** (ex.: Eclipse Temurin 25):

```bash
./gradlew build
```

O `.jar` completo pronto para uso será gerado em `build/libs/`.
