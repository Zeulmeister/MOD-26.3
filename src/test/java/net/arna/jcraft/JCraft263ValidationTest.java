package net.arna.jcraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.arna.jcraft.api.attack.enums.MoveInputType;
import net.arna.jcraft.client.combat.CombatInputBuffer;
import net.arna.jcraft.client.zoom.StarPlatinumPrecisionHandler;
import net.arna.jcraft.common.combat.TimeStopKineticPhysicsHandler;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class JCraft263ValidationTest {

    private static final Path RESOURCES_ROOT = Path.of("src/main/resources");
    private static int passedAssertions = 0;

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion failed: " + message);
        }
        passedAssertions++;
    }

    private static void checkEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " | expected=<" + expected + "> actual=<" + actual + ">");
        }
        passedAssertions++;
    }

    private static void checkClose(double expected, double actual, double eps, String message) {
        if (Math.abs(expected - actual) > eps) {
            throw new AssertionError(message + " | expected=" + expected + " actual=" + actual);
        }
        passedAssertions++;
    }

    private static List<Path> collectJsonFiles(Path dir) throws IOException {
        check(Files.isDirectory(dir), "Expected directory to exist: " + dir);
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
    }

    private static JsonObject parseJsonObject(Path path) throws IOException {
        String raw = Files.readString(path, StandardCharsets.UTF_8);
        JsonElement el = JsonParser.parseString(raw);
        check(el.isJsonObject(), "Expected JSON object in " + path);
        return el.getAsJsonObject();
    }

    static void verifyAllStandsSpecsEvolutionsAndMovesetsPresentAndValid() throws IOException {
        List<Path> stands = collectJsonFiles(RESOURCES_ROOT.resolve("data/jcraft/stands"));
        checkEquals(37, stands.size(), "All 37 Stands must be present in data/jcraft/stands");
        for (Path stand : stands) {
            parseJsonObject(stand);
        }

        List<Path> specs = collectJsonFiles(RESOURCES_ROOT.resolve("data/jcraft/specs"));
        checkEquals(5, specs.size(), "All 5 Specs must be present in data/jcraft/specs");
        for (Path spec : specs) {
            parseJsonObject(spec);
        }

        List<Path> evolutions = collectJsonFiles(RESOURCES_ROOT.resolve("data/jcraft/evolutions"));
        checkEquals(7, evolutions.size(), "All 7 Evolutions must be present in data/jcraft/evolutions");
        for (Path evo : evolutions) {
            parseJsonObject(evo);
        }

        List<Path> movesets = collectJsonFiles(RESOURCES_ROOT.resolve("data/jcraft/movesets"));
        check(movesets.size() >= 281, "All 281+ Movesets must be present in data/jcraft/movesets");
        int holdableCount = 0;
        for (Path moveset : movesets) {
            String content = Files.readString(moveset, StandardCharsets.UTF_8);
            parseJsonObject(moveset);
            if (content.contains("\"is_holdable\": true") || content.contains("\"is_holdable\":true")) {
                holdableCount++;
            }
        }
        check(holdableCount >= 12, "Custom is_holdable moveset customizations from Zeulmeister/MOD must be preserved");
    }

    static void verifyMc263AssetsWorldgenTradesAndTranslations() throws IOException {
        List<Path> items = collectJsonFiles(RESOURCES_ROOT.resolve("assets/jcraft/items"));
        checkEquals(223, items.size(), "All 223 MC 26.3 item definitions must be present");
        for (Path item : items) {
            parseJsonObject(item);
        }

        List<Path> recipes = collectJsonFiles(RESOURCES_ROOT.resolve("data/jcraft/recipe"));
        checkEquals(58, recipes.size(), "All 58 MC 26.3 recipes must be present");

        List<Path> lootTables = collectJsonFiles(RESOURCES_ROOT.resolve("data/jcraft/loot_table"));
        checkEquals(28, lootTables.size(), "All 28 MC 26.3 loot tables must be present");

        List<Path> advancements = collectJsonFiles(RESOURCES_ROOT.resolve("data/jcraft/advancement"));
        checkEquals(80, advancements.size(), "All 80 MC 26.3 advancements must be present");

        List<Path> trades = collectJsonFiles(RESOURCES_ROOT.resolve("data/jcraft/villager_trade/cartographer"));
        checkEquals(4, trades.size(), "All 4 data-driven MC 26.3 cartographer villager trades must be present");

        List<Path> postEffects = collectJsonFiles(RESOURCES_ROOT.resolve("assets/jcraft/post_effect"));
        checkEquals(4, postEffects.size(), "All 4 native MC 26.3 RenderPearl post_effects must be present");
        for (Path pe : postEffects) {
            JsonObject obj = parseJsonObject(pe);
            check(obj.has("passes"), "post_effect must declare passes: " + pe);
        }

        for (String shaderName : List.of("za_warudo.fsh", "time_erase.fsh", "bites_the_dust.fsh", "color_shift.fsh")) {
            Path shaderPath = RESOURCES_ROOT.resolve("assets/jcraft/shaders/post/" + shaderName);
            check(Files.isRegularFile(shaderPath), "Missing shader: " + shaderPath);
            String glsl = Files.readString(shaderPath, StandardCharsets.UTF_8);
            check(glsl.contains("#version 330"), "Shader must target GLSL #version 330 for RenderPearl 26.3: " + shaderName);
            check(glsl.contains("layout(std140)"), "Shader must use std140 UBO layout for RenderPearl 26.3: " + shaderName);
        }

        for (String lang : List.of("en_us.json", "pt_br.json")) {
            JsonObject langJson = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/lang/" + lang));
            check(langJson.has("key.jcraft.radial_menu"), "Missing radial menu key in " + lang);
            check(langJson.has("jcraft.config.title"), "Missing config screen title in " + lang);
            check(langJson.has("jcraft.radial.title"), "Missing radial wheel title in " + lang);
            check(langJson.has("jcraft.hud.pilot_active"), "Missing pilot HUD key in " + lang);
        }
    }

    static void verifyFabricModJsonEntrypointsAndMixins() throws Exception {
        Path processedModJson = Path.of("build/resources/main/fabric.mod.json");
        JsonObject modJson = parseJsonObject(Files.exists(processedModJson)
                ? processedModJson
                : RESOURCES_ROOT.resolve("fabric.mod.json"));

        checkEquals("jcraft", modJson.get("id").getAsString(), "Mod ID");
        checkEquals("jcraft.classtweaker", modJson.get("accessWidener").getAsString(), "AccessWidener/ClassTweaker");

        JsonObject depends = modJson.getAsJsonObject("depends");
        checkEquals("~26.3", depends.get("minecraft").getAsString(), "MC version dependency");
        checkEquals(">=25", depends.get("java").getAsString(), "Java version dependency");

        JsonArray cca = modJson.getAsJsonObject("custom").getAsJsonArray("cardinal-components");
        checkEquals(16, cca.size(), "All 16 Cardinal Components IDs must be registered in fabric.mod.json");

        ClassLoader cl = JCraft263ValidationTest.class.getClassLoader();
        JsonObject entrypoints = modJson.getAsJsonObject("entrypoints");
        for (String epKey : List.of("main", "client", "cardinal-components", "terrablender", "modmenu")) {
            JsonArray arr = entrypoints.getAsJsonArray(epKey);
            check(arr != null && arr.size() > 0, "Missing entrypoint group: " + epKey);
            for (JsonElement el : arr) {
                String className = el.getAsString();
                Class<?> clazz = Class.forName(className, false, cl);
                check(clazz != null, "Entrypoint class must load: " + className);
            }
        }

        JsonObject mixinsJson = parseJsonObject(RESOURCES_ROOT.resolve("jcraft.mixins.json"));
        checkEquals("JAVA_25", mixinsJson.get("compatibilityLevel").getAsString(), "Mixin CompatibilityLevel");
        String pkg = mixinsJson.get("package").getAsString();

        int totalMixins = 0;
        for (String group : List.of("mixins", "client")) {
            JsonArray list = mixinsJson.getAsJsonArray(group);
            check(list != null && list.size() > 0, "Mixin group must not be empty: " + group);
            for (JsonElement el : list) {
                String fqcn = pkg + "." + el.getAsString();
                String resourcePath = fqcn.replace('.', '/') + ".class";
                ClassNode classNode = new ClassNode();
                try (InputStream in = cl.getResourceAsStream(resourcePath)) {
                    check(in != null, "Compiled mixin bytecode must exist on classpath: " + fqcn);
                    new ClassReader(in).accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
                }

                AnnotationNode mixinNode = null;
                if (classNode.invisibleAnnotations != null) {
                    for (AnnotationNode ann : classNode.invisibleAnnotations) {
                        if ("Lorg/spongepowered/asm/mixin/Mixin;".equals(ann.desc)) {
                            mixinNode = ann;
                            break;
                        }
                    }
                }
                check(mixinNode != null, "Class must carry @Mixin bytecode annotation: " + fqcn);

                List<String> targetInternalNames = new ArrayList<>();
                if (mixinNode.values != null) {
                    for (int i = 0; i < mixinNode.values.size(); i += 2) {
                        String key = (String) mixinNode.values.get(i);
                        Object val = mixinNode.values.get(i + 1);
                        if ("value".equals(key) && val instanceof List<?> types) {
                            for (Object t : types) {
                                targetInternalNames.add(((Type) t).getInternalName());
                            }
                        } else if ("targets".equals(key) && val instanceof List<?> names) {
                            for (Object n : names) {
                                targetInternalNames.add(((String) n).replace('.', '/'));
                            }
                        }
                    }
                }
                check(!targetInternalNames.isEmpty(), "@Mixin must specify at least one target class: " + fqcn);
                for (String targetInternal : targetInternalNames) {
                    check(
                            cl.getResource(targetInternal + ".class") != null,
                            "@Mixin target class must exist in MC 26.3: " + targetInternal + " (from " + fqcn + ")"
                    );
                }
                totalMixins++;
            }
        }
        checkEquals(37, totalMixins, "All 37 common + client Mixins must be registered and bytecode-verified");
    }

    static void verifyKineticTimeStopPhysicsMath() {
        Vec3 initial = new Vec3(1.2, 0.0, 0.0);
        // Tick 0 at distance 0.6m (< 1.8m): maintains full launch velocity
        Vec3 inWindow = TimeStopKineticPhysicsHandler.computeDeceleratedVelocity(initial, 0.6, 0);
        checkEquals(initial, inWindow, "Projectile inside 1.8m launch window retains initial velocity");

        // Once distance >= 1.8m, decelerates smoothly to Vec3.ZERO
        Vec3 current = initial;
        double distance = 1.85;
        int ticks = 2;
        while (current.lengthSqr() > 0.0 && ticks < 30) {
            Vec3 next = TimeStopKineticPhysicsHandler.computeDeceleratedVelocity(current, distance, ticks);
            check(next.length() <= current.length(), "Kinetic deceleration must be monotonically non-increasing");
            current = next;
            distance += current.length();
            ticks++;
        }
        checkEquals(Vec3.ZERO, current, "Projectile must smoothly reach full mid-air suspension (Vec3.ZERO)");
        check(ticks >= 6 && ticks <= 25, "Deceleration to suspension should take a smooth visual window of ticks");

        Vec3 released = TimeStopKineticPhysicsHandler.computeReleaseVelocity(new Vec3(2.0, 0.0, 0.0));
        checkClose(2.5, released.x, 1.0E-6, "Released Time Stop projectile must carry +25% (1.25x) kinetic momentum");
        checkClose(0.0, released.y, 1.0E-6, "Released Y velocity");
        checkClose(0.0, released.z, 1.0E-6, "Released Z velocity");
    }

    static void verifySmartCombatInputBufferWindowAndExpiry() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        CombatInputBuffer.clear();
        check(!CombatInputBuffer.hasBufferedMove(), "Buffer initially empty");

        CombatInputBuffer.recordInput(MoveInputType.HEAVY);
        check(CombatInputBuffer.hasBufferedMove(), "Buffer recorded HEAVY");
        checkEquals(CombatInputBuffer.BUFFER_WINDOW_TICKS, CombatInputBuffer.getRemainingTicks(), "6-tick window");

        check(CombatInputBuffer.stepBuffer(false) == null, "Tick 1 stunned");
        check(CombatInputBuffer.stepBuffer(false) == null, "Tick 2 stunned");
        check(CombatInputBuffer.stepBuffer(false) == null, "Tick 3 stunned");
        check(CombatInputBuffer.hasBufferedMove(), "Still buffered after 3 ticks");

        checkEquals(MoveInputType.HEAVY, CombatInputBuffer.stepBuffer(true), "Dispatches HEAVY on frame 0 of recovery");
        check(!CombatInputBuffer.hasBufferedMove(), "Cleared after dispatch");

        CombatInputBuffer.recordInput(MoveInputType.ULTIMATE);
        for (int i = 0; i < CombatInputBuffer.BUFFER_WINDOW_TICKS; i++) {
            check(CombatInputBuffer.stepBuffer(false) == null, "Expiry countdown tick " + i);
        }
        check(!CombatInputBuffer.hasBufferedMove(), "Buffered move must expire after 6 ticks without ghost firing");
        check(CombatInputBuffer.stepBuffer(true) == null, "Nothing fires after expiry");
    }

    static void verifyStarPlatinumPrecisionZoomCurve() {
        checkClose(1.0, StarPlatinumPrecisionHandler.computeFovMultiplier(0.0f), 1.0E-6, "FOV at progress 0");
        checkClose(0.20, StarPlatinumPrecisionHandler.computeFovMultiplier(1.0f), 1.0E-6, "FOV at progress 1 (5x zoom)");

        double prev = 1.0;
        for (int step = 1; step <= 10; step++) {
            float t = step / 10.0f;
            double cur = StarPlatinumPrecisionHandler.computeFovMultiplier(t);
            check(cur < prev, "Precision zoom FOV curve must decrease smoothly and monotonically");
            prev = cur;
        }
    }

    static void verifySptwHairTexturesTimeSkipSafetyAndVelocityClamp() throws Exception {
        // 1. SPTW dedicated hair textures (all 4 skins + out-of-bounds clamping)
        for (int skin = 0; skin < 4; skin++) {
            String suffix = skin == 0 ? "default_hair.png" : "skin" + skin + "_hair.png";
            var sptwHair = net.arna.jcraft.client.renderer.entity.layer.SPHairLayer.getHairTexture(true, skin);
            checkEquals("jcraft:textures/entity/stands/star_platinum_the_world/" + suffix, sptwHair.toString(),
                    "SPTW hair texture path for skin " + skin);
            check(Files.exists(RESOURCES_ROOT.resolve("assets/jcraft/" + sptwHair.getPath())),
                    "SPTW hair texture file must exist: " + sptwHair.getPath());

            var spHair = net.arna.jcraft.client.renderer.entity.layer.SPHairLayer.getHairTexture(false, skin);
            checkEquals("jcraft:textures/entity/stands/star_platinum/" + suffix, spHair.toString(),
                    "SP hair texture path for skin " + skin);
        }
        checkEquals(
                "jcraft:textures/entity/stands/star_platinum_the_world/default_hair.png",
                net.arna.jcraft.client.renderer.entity.layer.SPHairLayer.getHairTexture(true, -5).toString(),
                "SPTW hair skin lower clamp"
        );
        checkEquals(
                "jcraft:textures/entity/stands/star_platinum_the_world/skin3_hair.png",
                net.arna.jcraft.client.renderer.entity.layer.SPHairLayer.getHairTexture(true, 99).toString(),
                "SPTW hair skin upper clamp"
        );

        // 2. TimeStop accumulated velocity clamping
        Vec3 huge = new Vec3(20.0, 0.0, 0.0);
        Vec3 clamped = net.arna.jcraft.common.component.impl.entity.CommonTimeStopComponentImpl.clampAccumulatedVelocity(huge);
        checkClose(net.arna.jcraft.common.component.impl.entity.CommonTimeStopComponentImpl.MAX_ACCUMULATED_LAUNCH_SPEED,
                clamped.length(), 1.0E-6, "Time Stop accumulated velocity clamped at max speed");
        Vec3 normal = new Vec3(1.0, 0.5, 0.0);
        checkEquals(normal, net.arna.jcraft.common.component.impl.entity.CommonTimeStopComponentImpl.clampAccumulatedVelocity(normal),
                "Normal Time Stop velocity preserved");

        // 3. TimeSkip safe block-face backoff (wall / ceiling / floor / miss)
        var wallHit = new net.minecraft.world.phys.BlockHitResult(
                new Vec3(10.0, 65.0, 0.0), net.minecraft.core.Direction.WEST, new net.minecraft.core.BlockPos(10, 65, 0), false);
        Vec3 safeWall = net.arna.jcraft.common.attack.moves.shared.TimeSkipMove.computeSafeTeleportPos(wallHit, 0.6, 1.8);
        check(safeWall.x < 9.7, "TimeSkip backs off from WEST wall face (x=" + safeWall.x + ")");

        var ceilHit = new net.minecraft.world.phys.BlockHitResult(
                new Vec3(0.0, 70.0, 0.0), net.minecraft.core.Direction.DOWN, new net.minecraft.core.BlockPos(0, 70, 0), false);
        Vec3 safeCeil = net.arna.jcraft.common.attack.moves.shared.TimeSkipMove.computeSafeTeleportPos(ceilHit, 0.6, 1.8);
        checkClose(70.0 - 1.85, safeCeil.y, 1.0E-6, "TimeSkip backs off below ceiling by entity height + 0.05");

        var floorHit = new net.minecraft.world.phys.BlockHitResult(
                new Vec3(0.0, 64.0, 0.0), net.minecraft.core.Direction.UP, new net.minecraft.core.BlockPos(0, 63, 0), false);
        Vec3 safeFloor = net.arna.jcraft.common.attack.moves.shared.TimeSkipMove.computeSafeTeleportPos(floorHit, 0.6, 1.8);
        checkClose(64.05, safeFloor.y, 1.0E-6, "TimeSkip offsets slightly above floor");

        var missHit = net.minecraft.world.phys.BlockHitResult.miss(
                new Vec3(14.0, 65.0, 0.0), net.minecraft.core.Direction.EAST, new net.minecraft.core.BlockPos(14, 65, 0));
        checkEquals(new Vec3(14.0, 65.0, 0.0),
                net.arna.jcraft.common.attack.moves.shared.TimeSkipMove.computeSafeTeleportPos(missHit, 0.6, 1.8),
                "TimeSkip miss teleports to exact end location");
    }

    static void verifyNonStandMechanicsAndMixins() throws Exception {
        ClassLoader cl = JCraft263ValidationTest.class.getClassLoader();

        // 1. MainMenuScreen overrides MC 26.3 extractLabels (not dead 1.20 renderLabels)
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/client/gui/screen/MainMenuScreen.class")) {
            check(in != null, "MainMenuScreen.class must exist");
            ClassReader cr = new ClassReader(in);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);
            boolean hasExtractLabels = cn.methods.stream().anyMatch(m -> "extractLabels".equals(m.name));
            boolean hasDeadRenderLabels = cn.methods.stream().anyMatch(m -> "renderLabels".equals(m.name));
            check(hasExtractLabels, "MainMenuScreen must override MC 26.3 extractLabels");
            check(!hasDeadRenderLabels, "MainMenuScreen must not keep dead 1.20 renderLabels method");
        }

        // 2. Cinderella's Kiss enchantment JSON supports jcraft:cinderella_mask
        JsonObject ckJson = JsonParser.parseString(Files.readString(
                RESOURCES_ROOT.resolve("data/jcraft/enchantment/cinderellas_kiss.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        checkEquals("jcraft:cinderella_mask", ckJson.get("supported_items").getAsString(),
                "cinderellas_kiss enchantment must support jcraft:cinderella_mask");
        checkEquals(3, ckJson.get("max_level").getAsInt(), "cinderellas_kiss max_level must be 3");

        // 3. Restored non-Stand mixins are registered in jcraft.mixins.json
        JsonObject mixinJson = JsonParser.parseString(Files.readString(
                RESOURCES_ROOT.resolve("jcraft.mixins.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        List<String> commonMixins = new ArrayList<>();
        for (JsonElement e : mixinJson.getAsJsonArray("mixins")) {
            commonMixins.add(e.getAsString());
        }
        for (String required : List.of("AnvilMenuMixin", "CombatTrackerAccessor", "FoodDataMixin", "GrindstoneMenuMixin",
                "GrindstoneSlotMixin", "ItemUseMixin", "VillagerEntityMixin")) {
            check(commonMixins.contains(required), "jcraft.mixins.json must include restored mixin: " + required);
        }

        // 4. Peacemaker decouples client key lookups via setClientKeySuppliers (zero client classes in common)
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/common/item/Peacemaker.class")) {
            check(in != null, "Peacemaker.class must exist");
            ClassReader cr = new ClassReader(in);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);
            check(cn.methods.stream().anyMatch(m -> "getShots".equals(m.name)),
                    "Peacemaker must define getShots(ItemStack)");
            check(cn.methods.stream().anyMatch(m -> "setClientKeySuppliers".equals(m.name)),
                    "Peacemaker must define setClientKeySuppliers(...)");
            check(cn.methods.stream().anyMatch(m -> "getFireKey".equals(m.name))
                            && cn.methods.stream().anyMatch(m -> "getAimKey".equals(m.name))
                            && cn.methods.stream().anyMatch(m -> "getReloadKey".equals(m.name)),
                    "Peacemaker must expose getFireKey/getAimKey/getReloadKey");
        }
        check(cl.getResource("net/arna/jcraft/common/item/Peacemaker$ClientKeys.class") == null,
                "Legacy Peacemaker$ClientKeys inner class must be removed from common");

        // 5. CinderellasKissEnchantment handles null/empty cleanly
        checkEquals(0, net.arna.jcraft.common.enchantments.CinderellasKissEnchantment.getCKLevel(null),
                "getCKLevel(null) returns 0");
        check(net.arna.jcraft.common.enchantments.CinderellasKissEnchantment.getCKHolder(null) == null,
                "getCKHolder(null) returns null");
    }

    static void verifyStandUserPosesAndArmSafety() throws Exception {
        ClassLoader cl = JCraft263ValidationTest.class.getClassLoader();
        net.arna.jcraft.api.pose.PoseModifiers.register();

        // 1. All Stand pose JSONs must parse cleanly through PoseModifiers.CODEC
        Path posesDir = RESOURCES_ROOT.resolve("assets/jcraft/poses");
        List<Path> poseFiles;
        try (var stream = Files.walk(posesDir)) {
            poseFiles = stream.filter(p -> p.getFileName().toString().equals("humanoid.json")).sorted().toList();
        }
        check(poseFiles.size() >= 7, "Expected at least 7 Stand humanoid pose JSONs, found " + poseFiles.size());
        for (Path poseFile : poseFiles) {
            JsonObject obj = JsonParser.parseString(Files.readString(poseFile, StandardCharsets.UTF_8)).getAsJsonObject();
            var parsed = net.arna.jcraft.api.pose.PoseModifiers.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, obj);
            check(parsed.result().isPresent(), "Pose JSON must parse cleanly: " + posesDir.relativize(poseFile)
                    + (parsed.error().isPresent() ? " (" + parsed.error().get().message() + ")" : ""));
        }

        // 2. star_platinum/humanoid.json must NOT force awkward rightArm splay or half-swing
        String spPoseRaw = Files.readString(posesDir.resolve("star_platinum/humanoid.json"), StandardCharsets.UTF_8);
        check(!spPoseRaw.contains("\"rightArm\""), "star_platinum/humanoid.json must leave rightArm natural");
        check(spPoseRaw.contains("\"leftArm\""), "star_platinum/humanoid.json must keep Jotaro leftArm pocket pose");

        // 3. the_world/humanoid.json must guard rightArm/leftArm offsets with RIGHT_ARM_EMPTY/LEFT_ARM_EMPTY
        JsonObject twPose = JsonParser.parseString(
                Files.readString(posesDir.resolve("the_world/humanoid.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        var twGroup = (net.arna.jcraft.api.pose.modifier.PoseModifierGroup) net.arna.jcraft.api.pose.PoseModifiers.CODEC
                .parse(com.mojang.serialization.JsonOps.INSTANCE, twPose).result().orElseThrow();
        check(twGroup.modifiers().size() == 3, "the_world pose must have 3 top-level modifier groups");
        var notSprintingGroup = (net.arna.jcraft.api.pose.modifier.PoseModifierGroup) twGroup.modifiers().get(2);
        boolean hasDirectArmMod = notSprintingGroup.modifiers().stream()
                .filter(m -> m instanceof net.arna.jcraft.api.pose.modifier.CustomPoseModifier)
                .map(m -> ((net.arna.jcraft.api.pose.modifier.CustomPoseModifier) m).part().partName())
                .anyMatch(name -> name.equals("rightArm") || name.equals("leftArm"));
        check(!hasDirectArmMod, "the_world USER_NOT_SPRINTING must not modify arms without arm-empty conditions");

        // 4. HumanoidModelMixin guards swings/riding/elytra/swimming and does not reset body angles at TAIL
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/mixin/client/HumanoidModelMixin.class")) {
            check(in != null, "HumanoidModelMixin.class must exist");
            ClassReader cr = new ClassReader(in);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);
            var setupMethod = cn.methods.stream()
                    .filter(m -> "jcraft$applyStandAndTimestopLevitationPoses".equals(m.name))
                    .findFirst().orElseThrow();
            boolean callsResetPartAngles = false;
            boolean checksSwingAnimation = false;
            boolean checksIsPassenger = false;
            boolean checksIsTwoHanded = false;
            for (var insn = setupMethod.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof org.objectweb.asm.tree.MethodInsnNode mi) {
                    if ("resetPartAngles".equals(mi.name)) callsResetPartAngles = true;
                    if ("isTwoHanded".equals(mi.name)) checksIsTwoHanded = true;
                } else if (insn instanceof org.objectweb.asm.tree.FieldInsnNode fi) {
                    if ("swingAnimation".equals(fi.name)) checksSwingAnimation = true;
                    if ("isPassenger".equals(fi.name)) checksIsPassenger = true;
                }
            }
            check(!callsResetPartAngles, "HumanoidModelMixin must not call resetPartAngles(body) at TAIL");
            check(checksSwingAnimation, "HumanoidModelMixin must guard pose application against swingAnimation");
            check(checksIsPassenger, "HumanoidModelMixin must guard pose application against isPassenger");
            check(checksIsTwoHanded, "HumanoidModelMixin must guard Time Stop arm pose against two-handed ArmPoses");
        }

        // 5. ModifierCondition checks swinging and two-handed item poses; LevitationPoseModifier checks arm conditions
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/api/pose/ModifierCondition.class")) {
            check(in != null, "ModifierCondition.class must exist");
            ClassReader cr = new ClassReader(in);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);
            boolean checksSwinging = cn.methods.stream().anyMatch(m -> {
                for (var insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof org.objectweb.asm.tree.MethodInsnNode mi && "isSwinging".equals(mi.name)) {
                        return true;
                    }
                }
                return false;
            });
            boolean checksCrossbow = cn.methods.stream().anyMatch(m -> {
                for (var insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof org.objectweb.asm.tree.MethodInsnNode mi && "isCharged".equals(mi.name)) {
                        return true;
                    }
                }
                return false;
            });
            check(checksSwinging, "ModifierCondition must guard arm pose conditions against isSwinging()");
            check(checksCrossbow, "ModifierCondition must guard arm pose conditions against charged crossbows");
        }
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/api/pose/modifier/LevitationPoseModifier.class")) {
            check(in != null, "LevitationPoseModifier.class must exist");
            ClassReader cr = new ClassReader(in);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);
            var applyMethod = cn.methods.stream().filter(m -> "apply".equals(m.name)).findFirst().orElseThrow();
            boolean checksRightArmEmpty = false;
            boolean checksLeftArmEmpty = false;
            for (var insn = applyMethod.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof org.objectweb.asm.tree.FieldInsnNode fi) {
                    if ("RIGHT_ARM_EMPTY".equals(fi.name)) checksRightArmEmpty = true;
                    if ("LEFT_ARM_EMPTY".equals(fi.name)) checksLeftArmEmpty = true;
                }
            }
            check(checksRightArmEmpty, "LevitationPoseModifier must guard rightArm with RIGHT_ARM_EMPTY");
            check(checksLeftArmEmpty, "LevitationPoseModifier must guard leftArm with LEFT_ARM_EMPTY");
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Running JCraft 26.3 Superior Edition Verification Suite ===");
        verifyAllStandsSpecsEvolutionsAndMovesetsPresentAndValid();
        System.out.println("[PASS] 37 Stands, 5 Specs, 7 Evolutions & 281 Movesets verified.");
        verifyMc263AssetsWorldgenTradesAndTranslations();
        System.out.println("[PASS] 223 Items, 4 RenderPearl 26.3 Post-Effects + GLSL 330 std140 Shaders, Trades & Translations verified.");
        verifyFabricModJsonEntrypointsAndMixins();
        System.out.println("[PASS] fabric.mod.json, 16 CCA Components, 5 Entrypoints & all 37 Java 25 Mixins verified.");
        verifyKineticTimeStopPhysicsMath();
        System.out.println("[PASS] Kinetic Time Stop Deceleration & +25% Momentum Release Physics verified.");
        verifySmartCombatInputBufferWindowAndExpiry();
        System.out.println("[PASS] 6-Tick Smart Combat Input Buffer verified.");
        verifyStarPlatinumPrecisionZoomCurve();
        System.out.println("[PASS] Star Platinum 5x Precision Zoom Curve verified.");
        verifySptwHairTexturesTimeSkipSafetyAndVelocityClamp();
        System.out.println("[PASS] SPTW Hair Textures, TimeSkip Collision Backoff & TimeStop Velocity Cap verified.");
        verifyNonStandMechanicsAndMixins();
        System.out.println("[PASS] Non-Stand Mixins, MainMenuScreen extractLabels, Cinderella's Kiss & Headless Safety verified.");
        verifyStandUserPosesAndArmSafety();
        System.out.println("[PASS] Stand User Poses, Star Platinum Right Arm & HumanoidModelMixin Arm Safety verified.");
        verifyTimeStopOnlyFlightKeybindsAndCleanHud();
        System.out.println("[PASS] Time-Stop-Only Flight, Debug-Chord-Safe Keybinds & Clean Stand Ability Dock verified.");
        verifyGameplayAndGameDesignUpgrades();
        System.out.println("[PASS] Gameplay & Game Design (AU Terrain, ItemToss, StandGuideScreen, Presets & PT-BR Parity) verified.");
        verifyStandAndSpecMechanicsUpgrades();
        System.out.println("[PASS] Stand/Spec Mechanics (37/37 Icons & Poses, Gauges, Stealth/Pilot Mixins, NPC Anims & Fixes) verified.");
        verifyReverseEngineeringFixesAllFourBatches();
        System.out.println("[PASS] Brutal Reverse-Engineering Fixes (Lotes 1-4: MIH Clock, Hamon Server Safety, Equipment, Features, TS, Gravity C-Moon) verified.");
        verifyDedicatedServerIsolation();
        System.out.println("[PASS] Dedicated-Server Bytecode Isolation Gate (0 common/api -> client leaks across all non-client classes) verified.");
        System.out.println("=== ALL " + passedAssertions + " ASSERTIONS PASSED SUCCESSFULLY ===");
    }

    static void verifyGameplayAndGameDesignUpgrades() throws Exception {
        ClassLoader cl = JCraft263ValidationTest.class.getClassLoader();

        // 1. Complete EN/PT-BR key parity & presence of Gameplay/Guide/Preset keys
        JsonObject enJson = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/lang/en_us.json"));
        JsonObject ptJson = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/lang/pt_br.json"));
        checkEquals(enJson.size(), ptJson.size(), "en_us.json and pt_br.json must have identical key counts");
        for (String key : enJson.keySet()) {
            check(ptJson.has(key), "pt_br.json missing translation key: " + key);
        }
        for (String requiredKey : List.of(
                "jcraft.guide.title", "jcraft.guide.tab.moveset", "jcraft.guide.tab.evolutions",
                "jcraft.guide.tab.combat", "jcraft.guide.tab.progression", "jcraft.radial.guide_center",
                "jcraft.radial.open_guide", "jcraft.welcome.guide_hint", "jcraft.about.moves",
                "jcraft.config.presets", "jcraft.config.preset.coop", "jcraft.config.preset.pvp",
                "jcraft.config.preset.chaos", "jcraft.config.open_guide")) {
            check(enJson.has(requiredKey) && ptJson.has(requiredKey), "Missing required lang key: " + requiredKey);
        }

        // 2. Improved Survival/Co-op balance defaults in compiled JServerConfig.<clinit> bytecode
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/common/config/JServerConfig.class")) {
            check(in != null, "JServerConfig.class must exist");
            ClassReader cr = new ClassReader(in);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);
            var clinit = cn.methods.stream().filter(m -> "<clinit>".equals(m.name)).findFirst().orElseThrow();
            Integer prevConst = null;
            Boolean ipsDefault = null, cdcDefault = null, ffDefault = null;
            for (var insn = clinit.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn.getOpcode() == org.objectweb.asm.Opcodes.ICONST_0) prevConst = 0;
                else if (insn.getOpcode() == org.objectweb.asm.Opcodes.ICONST_1) prevConst = 1;
                if (insn instanceof org.objectweb.asm.tree.FieldInsnNode fi && fi.getOpcode() == org.objectweb.asm.Opcodes.PUTSTATIC) {
                    if ("ENABLE_IPS".equals(fi.name)) ipsDefault = Integer.valueOf(1).equals(prevConst);
                    else if ("SURVIVAL_CDC".equals(fi.name)) cdcDefault = Integer.valueOf(1).equals(prevConst);
                    else if ("ENABLE_FRIENDLY_FIRE".equals(fi.name)) ffDefault = Integer.valueOf(1).equals(prevConst);
                }
            }
            checkEquals(Boolean.TRUE, ipsDefault, "JServerConfig.ENABLE_IPS bytecode default must be true");
            checkEquals(Boolean.TRUE, cdcDefault, "JServerConfig.SURVIVAL_CDC bytecode default must be true");
            checkEquals(Boolean.FALSE, ffDefault, "JServerConfig.ENABLE_FRIENDLY_FIRE bytecode default must be false");
        }

        // 3. DimensionalHopMove implements copyTerrainToAU and no longer has dead fixLightInAU stub
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/common/attack/moves/dirtydeedsdonedirtcheap/DimensionalHopMove.class")) {
            check(in != null, "DimensionalHopMove.class must exist");
            ClassReader cr = new ClassReader(in);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);
            check(cn.methods.stream().anyMatch(m -> "copyTerrainToAU".equals(m.name)),
                    "DimensionalHopMove must define copyTerrainToAU");
            check(cn.methods.stream().noneMatch(m -> "fixLightInAU".equals(m.name)),
                    "DimensionalHopMove must remove dead fixLightInAU stub");
        }

        // 4. ItemTossProjectile includes log-stripping & durability/drop helpers
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/common/entity/projectile/ItemTossProjectile.class")) {
            check(in != null, "ItemTossProjectile.class must exist");
            ClassReader cr = new ClassReader(in);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);
            check(cn.methods.stream().anyMatch(m -> "getStrippedState".equals(m.name)),
                    "ItemTossProjectile must implement getStrippedState");
            check(cn.methods.stream().anyMatch(m -> "damageCopyOne".equals(m.name)),
                    "ItemTossProjectile must implement damageCopyOne");
        }

        // 5. StandGuideScreen exists and is wired from JoJoRadialWheelScreen and JCraftConfigScreen
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/client/gui/screen/StandGuideScreen.class")) {
            check(in != null, "StandGuideScreen.class must exist");
        }
        // 6. TWOH & GER (and all other Stands) must no longer hold client-pose POSE fields or ClientPose holders
        for (String removedHolder : List.of(
                "net/arna/jcraft/common/entity/stand/TheWorldOverHeavenEntity$ClientPose.class",
                "net/arna/jcraft/common/entity/stand/GEREntity$ClientPose.class")) {
            check(cl.getResource(removedHolder) == null, "Legacy POSE holder must be removed from common Stand: " + removedHolder);
        }
        for (String caller : List.of(
                "net/arna/jcraft/client/gui/screen/JoJoRadialWheelScreen.class",
                "net/arna/jcraft/client/gui/screen/JCraftConfigScreen.class")) {
            try (InputStream in = cl.getResourceAsStream(caller)) {
                check(in != null, caller + " must exist");
                ClassReader cr = new ClassReader(in);
                ClassNode cn = new ClassNode();
                cr.accept(cn, 0);
                boolean opensGuide = cn.methods.stream().anyMatch(m -> {
                    for (var insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                        if (insn instanceof org.objectweb.asm.tree.MethodInsnNode mi
                                && "net/arna/jcraft/client/gui/screen/StandGuideScreen".equals(mi.owner)) {
                            return true;
                        }
                    }
                    return false;
                });
                check(opensGuide, caller + " must open StandGuideScreen");
            }
        }
    }

    static void verifyTimeStopOnlyFlightKeybindsAndCleanHud() throws Exception {
        ClassLoader cl = JCraft263ValidationTest.class.getClassLoader();

        // 1. ConditionalFlightHandler only registers GER Flight + Active Time Stop (NO unconditional Stand summon flight)
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/api/misc/ConditionalFlightHandler.class")) {
            check(in != null, "ConditionalFlightHandler.class must exist");
            ClassReader cr = new ClassReader(in);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);
            var regMethod = cn.methods.stream()
                    .filter(m -> "registerJCraftConditions".equals(m.name))
                    .findFirst().orElseThrow();
            int addCondCount = 0;
            for (var insn = regMethod.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof org.objectweb.asm.tree.MethodInsnNode mi && "addCondition".equals(mi.name)) {
                    addCondCount++;
                }
            }
            checkEquals(2, addCondCount, "ConditionalFlightHandler must register exactly 2 flight conditions (GER flight + active Time Stop)");
            boolean hasRevoke = cn.methods.stream().anyMatch(m -> "revokeStaleFlightIfNotAllowed".equals(m.name));
            check(hasRevoke, "ConditionalFlightHandler must expose revokeStaleFlightIfNotAllowed");
        }

        // 2. Timestops, JCraft.stopTimestop, JServerEvents.playerJoin and ClientPacketHandler all revoke stale flight
        for (String cls : List.of(
                "net/arna/jcraft/common/tickable/Timestops.class",
                "net/arna/jcraft/JCraft.class",
                "net/arna/jcraft/common/events/JServerEvents.class",
                "net/arna/jcraft/client/net/ClientPacketHandler.class")) {
            try (InputStream in = cl.getResourceAsStream(cls)) {
                check(in != null, cls + " must exist");
                ClassReader cr = new ClassReader(in);
                ClassNode cn = new ClassNode();
                cr.accept(cn, 0);
                boolean callsRevoke = cn.methods.stream().anyMatch(m -> {
                    for (var insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                        if (insn instanceof org.objectweb.asm.tree.MethodInsnNode mi
                                && "revokeStaleFlightIfNotAllowed".equals(mi.name)) {
                            return true;
                        }
                    }
                    return false;
                });
                check(callsRevoke, cls + " must call ConditionalFlightHandler.revokeStaleFlightIfNotAllowed");
            }
        }

        // 3. JCraftConfigScreen ignores MC 26.3 KeyMapping.Category.DEBUG chord keys in conflict checks
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/client/gui/screen/JCraftConfigScreen.class")) {
            check(in != null, "JCraftConfigScreen.class must exist");
            ClassReader cr = new ClassReader(in);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);
            var ignoreMethod = cn.methods.stream()
                    .filter(m -> "isIgnoredVanillaKey".equals(m.name))
                    .findFirst().orElseThrow();
            boolean referencesDebugCategory = false;
            for (var insn = ignoreMethod.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof org.objectweb.asm.tree.FieldInsnNode fi && "DEBUG".equals(fi.name)) {
                    referencesDebugCategory = true;
                }
            }
            check(referencesDebugCategory, "JCraftConfigScreen.isIgnoredVanillaKey must exclude KeyMapping.Category.DEBUG");
        }

        // 4. Clean Stand Ability Dock icons exist for SPTW, Star Platinum, The World, fallback & universal
        Path iconsRoot = RESOURCES_ROOT.resolve("assets/jcraft/textures/gui/ability_icons");
        for (String standDir : List.of("star_platinum_the_world", "star_platinum", "the_world", "fallback")) {
            for (String icon : List.of("light.png", "heavy.png", "barrage.png", "special1.png", "special2.png", "special3.png", "ult.png", "utility.png")) {
                check(Files.exists(iconsRoot.resolve(standDir).resolve(icon)),
                        "Missing ability icon texture: " + standDir + "/" + icon);
            }
        }
        for (String uniIcon : List.of("dash.png", "cooldowncancel.png", "combobreaker.png")) {
            check(Files.exists(iconsRoot.resolve("universal").resolve(uniIcon)),
                    "Missing universal ability icon texture: " + uniIcon);
        }

        // 5. JCraftAbilityHud and JoJoAnimeHudRenderer compile with integrated clean header helper
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/client/gui/hud/JoJoAnimeHudRenderer.class")) {
            check(in != null, "JoJoAnimeHudRenderer.class must exist");
            ClassReader cr = new ClassReader(in);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);
            check(cn.methods.stream().anyMatch(m -> "drawMinimalStandHeader".equals(m.name)),
                    "JoJoAnimeHudRenderer must expose drawMinimalStandHeader");
        }
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/client/gui/hud/JCraftAbilityHud.class")) {
            check(in != null, "JCraftAbilityHud.class must exist");
            ClassReader cr = new ClassReader(in);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);
            check(cn.methods.stream().anyMatch(m -> "renderAbilityTile".equals(m.name)),
                    "JCraftAbilityHud must define renderAbilityTile");
        }
    }

    static void verifyStandAndSpecMechanicsUpgrades() throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        // 1. All 36 non-NONE Stand icon folders + silver_chariot_possessed + fallback must have all 8 ability icons
        List<String> expectedIconDirs = List.of(
                "star_platinum", "the_world", "king_crimson", "d4c", "cream", "killer_queen",
                "white_snake", "silver_chariot", "magicians_red", "the_fool", "gold_experience",
                "hierophant_green", "the_sun", "purple_haze", "c_moon", "made_in_heaven",
                "the_world_over_heaven", "killer_queen_bites_the_dust", "gold_experience_requiem",
                "star_platinum_the_world", "purple_haze_distortion", "horus", "cinderella", "osiris",
                "atum", "chariot_requiem", "diver_down", "dragons_dream", "foo_fighters",
                "goo_goo_dolls", "shadow_the_world", "metallica", "the_hand", "mandom",
                "crazy_diamond", "aerosmith", "silver_chariot_possessed", "fallback"
        );
        Path iconsRoot = RESOURCES_ROOT.resolve("assets/jcraft/textures/gui/ability_icons");
        List<String> fullSlots = List.of("light.png", "heavy.png", "barrage.png", "special1.png", "special2.png", "special3.png", "ult.png", "utility.png");
        List<String> mandomSlots = List.of("ult.png", "utility.png");
        for (String standDir : expectedIconDirs) {
            List<String> expectedForStand = "mandom".equals(standDir) ? mandomSlots : fullSlots;
            for (String slot : expectedForStand) {
                Path png = iconsRoot.resolve(standDir).resolve(slot);
                check(Files.exists(png) && Files.size(png) > 0, "Missing or empty ability icon: " + standDir + "/" + slot);
            }
        }

        // 2. All 35 pose folders (covering 36 non-NONE Stands via SPTW inheriting star_platinum) must parse cleanly
        //    and all newly generated pose JSONs must gate arm edits behind RIGHT_ARM_EMPTY / LEFT_ARM_EMPTY
        List<String> expectedPoseDirs = List.of(
                "star_platinum", "the_world", "king_crimson", "d4c", "cream", "killer_queen",
                "white_snake", "silver_chariot", "magicians_red", "the_fool", "gold_experience",
                "hierophant_green", "the_sun", "purple_haze", "c_moon", "made_in_heaven",
                "the_world_over_heaven", "killer_queen_bites_the_dust", "gold_experience_requiem",
                "purple_haze_distortion", "horus", "cinderella", "osiris", "atum",
                "chariot_requiem", "diver_down", "dragons_dream", "foo_fighters", "goo_goo_dolls",
                "shadow_the_world", "metallica", "the_hand", "mandom", "crazy_diamond", "aerosmith"
        );
        Path posesRoot = RESOURCES_ROOT.resolve("assets/jcraft/poses");
        for (String poseDir : expectedPoseDirs) {
            Path poseFile = posesRoot.resolve(poseDir).resolve("humanoid.json");
            check(Files.exists(poseFile), "Missing Stand user pose humanoid.json: " + poseDir);
            JsonObject obj = parseJsonObject(poseFile);
            check(net.arna.jcraft.api.pose.PoseModifiers.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, obj)
                    .result().isPresent(), "Invalid PoseModifiers JSON for " + poseDir);
            String raw = Files.readString(poseFile, StandardCharsets.UTF_8);
            if (!List.of("the_world", "the_world_over_heaven", "king_crimson", "killer_queen",
                    "killer_queen_bites_the_dust", "gold_experience_requiem", "star_platinum").contains(poseDir)) {
                check(raw.contains("\"RIGHT_ARM_EMPTY\"") && raw.contains("\"LEFT_ARM_EMPTY\""),
                        "New pose for " + poseDir + " must keep both arms gated by EMPTY conditions");
            }
        }

        // 3. spec_user.animation.json contains both Metallica (mtl.*) and merged Ranger (rgr.*) animations
        JsonObject specAnimRoot = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/animations/spec/spec_user.animation.json"));
        JsonObject specAnims = specAnimRoot.getAsJsonObject("animations");
        for (String animKey : List.of("mtl.brg", "mtl.bsc", "an.pom", "an.slsh", "stw.cntr", "rgr.roll", "rgr.slide", "rgr.whip", "rgr.bash", "rgr.buts")) {
            check(specAnims.has(animKey), "spec_user.animation.json missing animation key: " + animKey);
        }

        // 4. Bytecode checks: JCraftHudOverlay registered, stealth mixins wired, PredictionMove lifecycle cleanup
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/client/gui/hud/JCraftHudOverlay.class")) {
            check(in != null, "JCraftHudOverlay.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            check(cn.methods.stream().anyMatch(m -> "init".equals(m.name)), "JCraftHudOverlay must expose init()");
            check(cn.methods.stream().anyMatch(m -> "render".equals(m.name)), "JCraftHudOverlay must expose render()");
        }
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/client/JCraftClient.class")) {
            check(in != null, "JCraftClient.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            boolean registersOverlay = cn.methods.stream().anyMatch(m -> {
                for (var insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof org.objectweb.asm.tree.MethodInsnNode mi
                            && mi.owner.endsWith("JCraftHudOverlay") && "init".equals(mi.name)) {
                        return true;
                    }
                }
                return false;
            });
            check(registersOverlay, "JCraftClient must call JCraftHudOverlay.init()");
        }
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/mixin/client/LevelExtractorMixin.class")) {
            check(in != null, "LevelExtractorMixin.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            check(cn.methods.stream().anyMatch(m -> "jcraft$hideStealthEntities".equals(m.name)),
                    "LevelExtractorMixin must inject jcraft$hideStealthEntities");
        }
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/mixin/client/GameRendererMixin.class")) {
            check(in != null, "GameRendererMixin.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            check(cn.methods.stream().anyMatch(m -> "jcraft$hideFirstPersonHandWhenStealthedOrPiloting".equals(m.name)),
                    "GameRendererMixin must inject jcraft$hideFirstPersonHandWhenStealthedOrPiloting");
        }
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/common/attack/moves/kingcrimson/PredictionMove.class")) {
            check(in != null, "PredictionMove.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            check(cn.methods.stream().anyMatch(m -> "onCancel".equals(m.name)), "PredictionMove must override onCancel");
            check(cn.methods.stream().anyMatch(m -> "onDeactivate".equals(m.name)), "PredictionMove must override onDeactivate");
        }

        // 5. Gauge i18n keys parity between en_us.json and pt_br.json
        JsonObject enUs = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/lang/en_us.json"));
        JsonObject ptBr = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/lang/pt_br.json"));
        checkEquals(enUs.keySet(), ptBr.keySet(), "en_us.json and pt_br.json keys must stay 100% in parity");
        for (String key : List.of("jcraft.hud.gauge.iron", "jcraft.hud.gauge.overheat", "jcraft.hud.gauge.obedience",
                "jcraft.hud.gauge.speedometer", "jcraft.hud.gauge.marker", "jcraft.hud.gauge.bloodlust",
                "jcraft.hud.gauge.focus", "jcraft.hud.phd.mode.harming", "jcraft.hud.phd.mode.nullifying",
                "jcraft.hud.phd.mode.debilitating")) {
            check(enUs.has(key) && ptBr.has(key), "Missing HUD gauge localization key: " + key);
        }
    }

    static void verifyReverseEngineeringFixesAllFourBatches() throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        // Lote 1: Equipment textures exist, duplicate player_animation/ removed, HamonWaveEntity has no ClientLevel ref
        for (String eq : List.of("cinderella_mask.png", "stone_mask.png", "red_hat.png")) {
            Path p = RESOURCES_ROOT.resolve("assets/jcraft/textures/entity/equipment/humanoid/" + eq);
            check(Files.exists(p) && Files.size(p) > 0, "Missing MC 26.3 equipment texture: " + eq);
        }
        check(!Files.exists(RESOURCES_ROOT.resolve("assets/jcraft/player_animation")),
                "Duplicate assets/jcraft/player_animation/ folder must remain deleted");

        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/common/entity/projectile/HamonWaveEntity.class")) {
            check(in != null, "HamonWaveEntity.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            boolean leaksClientLevel = cn.methods.stream().anyMatch(m -> {
                for (var insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof org.objectweb.asm.tree.TypeInsnNode ti && ti.desc.contains("ClientLevel")) return true;
                    if (insn instanceof org.objectweb.asm.tree.MethodInsnNode mi && mi.owner.contains("ClientLevel")) return true;
                }
                return false;
            });
            check(!leaksClientLevel, "HamonWaveEntity must not reference ClientLevel on dedicated servers");
        }

        // Lote 1: MIH TimeAccelStatePacket calls JUtils.setDayTime (no empty lambda)
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/common/network/s2c/TimeAccelStatePacket.class")) {
            check(in != null, "TimeAccelStatePacket.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            boolean callsSetDayTime = cn.methods.stream().anyMatch(m -> {
                for (var insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof org.objectweb.asm.tree.MethodInsnNode mi && "setDayTime".equals(mi.name)) return true;
                }
                return false;
            });
            check(callsSetDayTime, "TimeAccelStatePacket must invoke JUtils.setDayTime");
        }

        // Lote 2: FeatureRenderers extend RenderLayer, world effects + Time Stop mixins present
        for (String feat : List.of(
                "net/arna/jcraft/client/renderer/features/ArmoredMoveFeatureRenderer.class",
                "net/arna/jcraft/client/renderer/features/StuckKnivesFeatureRenderer.class",
                "net/arna/jcraft/client/renderer/features/HamonParticlesFeatureRenderer.class")) {
            try (InputStream in = cl.getResourceAsStream(feat)) {
                check(in != null, feat + " must exist");
                ClassNode cn = new ClassNode();
                new ClassReader(in).accept(cn, 0);
                check(cn.superName.endsWith("RenderLayer"), feat + " must extend MC 26.3 RenderLayer");
            }
        }
        for (String mixinCls : List.of(
                "net/arna/jcraft/mixin/client/AvatarRendererMixin.class",
                "net/arna/jcraft/mixin/client/AzurelibTimestopMixin.class",
                "net/arna/jcraft/mixin/client/ParticleGroupMixin.class",
                "net/arna/jcraft/mixin/client/LivingEntityRendererMixin.class")) {
            check(cl.getResource(mixinCls) != null, "New 26.3 mixin class must exist: " + mixinCls);
        }

        // Lote 3 & 4: AllStandsRuntimeDiagnostic + Gravity physics hooks in EntityMixin & CameraMixin
        check(cl.getResource("net/arna/jcraft/common/debug/AllStandsRuntimeDiagnostic.class") != null,
                "AllStandsRuntimeDiagnostic must compile cleanly");
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/mixin/EntityMixin.class")) {
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            check(cn.methods.stream().anyMatch(m -> "jcraft$rotateGravityBoundingBox".equals(m.name)),
                    "EntityMixin must apply directional gravity bounding box");
            check(cn.methods.stream().anyMatch(m -> "jcraft$transformMoveToWorldGravity".equals(m.name)),
                    "EntityMixin must transform movement to world gravity");
        }
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/mixin/client/CameraMixin.class")) {
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            check(cn.methods.stream().anyMatch(m -> "jcraft$applyGravityCameraRotation".equals(m.name)),
                    "CameraMixin must rotate camera quaternion for C-Moon gravity");
        }

        // Lote 5 (Client Visual + Directional Gravity Completion)
        for (String extraCls : List.of(
                "net/arna/jcraft/mixin/ServerGamePacketListenerImplMixin.class",
                "net/arna/jcraft/client/debug/ClientVisualDiagnostic.class")) {
            check(cl.getResource(extraCls) != null, "Class must exist: " + extraCls);
        }
        check(!net.arna.jcraft.client.JClientConfig.getInstance().isDisableMihAfterimages(),
                "MIH afterimages should be enabled by default");
    }

    private static boolean hasClientEnvironmentAnnotation(List<AnnotationNode> annotations) {
        if (annotations == null) return false;
        for (AnnotationNode an : annotations) {
            if ("Lnet/fabricmc/api/Environment;".equals(an.desc) && an.values != null) {
                for (int i = 0; i + 1 < an.values.size(); i += 2) {
                    if ("value".equals(an.values.get(i)) && an.values.get(i + 1) instanceof String[] arr
                            && arr.length == 2 && "CLIENT".equals(arr[1])) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String findForbiddenClientPrefix(String ref, List<String> forbiddenPrefixes) {
        if (ref == null) return null;
        for (String prefix : forbiddenPrefixes) {
            if (ref.contains(prefix)) {
                return prefix;
            }
        }
        return null;
    }

    static void verifyDedicatedServerIsolation() throws Exception {
        Path classesRoot = Path.of("build/classes/java/main");
        Path modRoot = classesRoot.resolve("net/arna/jcraft");
        check(Files.isDirectory(modRoot), "Compiled classes root must exist: " + modRoot);

        List<String> forbiddenPrefixes = List.of(
                "net/minecraft/client/",
                "net/arna/jcraft/client/",
                "net/arna/jcraft/fabric/client/",
                "net/arna/jcraft/mixin/client/",
                "net/arna/jcraft/api/pose/",
                "com/mojang/blaze3d/",
                "com/mojang/renderpearl/",
                "org/lwjgl/",
                "dev/architectury/event/events/client/",
                "dev/architectury/registry/client/",
                "net/fabricmc/fabric/api/client/",
                "mod/azure/azurelib/util/client/",
                "mod/azure/azurelib/render/"
        );

        // Verify common particle options exist in common.particle (zero client dependency)
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (String particleCls : List.of(
                "net/arna/jcraft/common/particle/BreathParticleOptions.class",
                "net/arna/jcraft/common/particle/LockOnParticleOptions.class",
                "net/arna/jcraft/common/particle/MoshParticleOptions.class")) {
            check(cl.getResource(particleCls) != null, "Common particle options class must exist: " + particleCls);
        }

        List<String> violations = new ArrayList<>();
        int scannedNonClientClasses = 0;
        int verifiedClientOnlyPoseClasses = 0;

        try (Stream<Path> walk = Files.walk(modRoot)) {
            List<Path> classFiles = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".class"))
                    .sorted()
                    .toList();

            for (Path cf : classFiles) {
                String rel = classesRoot.relativize(cf).toString().replace('\\', '/');
                if (rel.startsWith("net/arna/jcraft/client/")
                        || rel.startsWith("net/arna/jcraft/fabric/client/")
                        || rel.startsWith("net/arna/jcraft/mixin/client/")) {
                    continue;
                }

                ClassNode cn = new ClassNode();
                try (InputStream in = Files.newInputStream(cf)) {
                    new ClassReader(in).accept(cn, 0);
                }

                boolean classClientOnly = hasClientEnvironmentAnnotation(cn.visibleAnnotations)
                        || hasClientEnvironmentAnnotation(cn.invisibleAnnotations);

                if (rel.startsWith("net/arna/jcraft/api/pose/")) {
                    // Anonymous/inner classes of client-only pose API inherit outer boundary
                    boolean isInner = rel.contains("$");
                    if (!isInner) {
                        check(classClientOnly, "Client-only pose API class must be annotated @Environment(EnvType.CLIENT): " + rel);
                        verifiedClientOnlyPoseClasses++;
                    }
                    continue;
                }

                if (classClientOnly) {
                    continue;
                }

                scannedNonClientClasses++;

                String bad;
                if ((bad = findForbiddenClientPrefix(cn.superName, forbiddenPrefixes)) != null) {
                    violations.add(rel + " superName -> " + cn.superName + " (" + bad + ")");
                }
                for (String iface : cn.interfaces) {
                    if ((bad = findForbiddenClientPrefix(iface, forbiddenPrefixes)) != null) {
                        violations.add(rel + " interface -> " + iface + " (" + bad + ")");
                    }
                }
                for (var field : cn.fields) {
                    if (hasClientEnvironmentAnnotation(field.visibleAnnotations)
                            || hasClientEnvironmentAnnotation(field.invisibleAnnotations)) {
                        continue;
                    }
                    if ((bad = findForbiddenClientPrefix(field.desc, forbiddenPrefixes)) != null) {
                        violations.add(rel + " field " + field.name + " -> " + field.desc + " (" + bad + ")");
                    }
                }
                for (var method : cn.methods) {
                    if (hasClientEnvironmentAnnotation(method.visibleAnnotations)
                            || hasClientEnvironmentAnnotation(method.invisibleAnnotations)) {
                        continue;
                    }
                    if ((bad = findForbiddenClientPrefix(method.desc, forbiddenPrefixes)) != null) {
                        violations.add(rel + " method " + method.name + " desc -> " + method.desc + " (" + bad + ")");
                    }
                    for (var insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                        if (insn instanceof org.objectweb.asm.tree.MethodInsnNode mi) {
                            if ((bad = findForbiddenClientPrefix(mi.owner, forbiddenPrefixes)) != null
                                    || (bad = findForbiddenClientPrefix(mi.desc, forbiddenPrefixes)) != null) {
                                violations.add(rel + "#" + method.name + " calls " + mi.owner + "." + mi.name + mi.desc + " (" + bad + ")");
                            }
                        } else if (insn instanceof org.objectweb.asm.tree.FieldInsnNode fi) {
                            if ((bad = findForbiddenClientPrefix(fi.owner, forbiddenPrefixes)) != null
                                    || (bad = findForbiddenClientPrefix(fi.desc, forbiddenPrefixes)) != null) {
                                violations.add(rel + "#" + method.name + " accesses " + fi.owner + "." + fi.name + " (" + bad + ")");
                            }
                        } else if (insn instanceof org.objectweb.asm.tree.TypeInsnNode ti) {
                            if ((bad = findForbiddenClientPrefix(ti.desc, forbiddenPrefixes)) != null) {
                                violations.add(rel + "#" + method.name + " type " + ti.desc + " (" + bad + ")");
                            }
                        } else if (insn instanceof org.objectweb.asm.tree.InvokeDynamicInsnNode indy) {
                            if ((bad = findForbiddenClientPrefix(indy.desc, forbiddenPrefixes)) != null) {
                                violations.add(rel + "#" + method.name + " indy " + indy.desc + " (" + bad + ")");
                            }
                            if (indy.bsmArgs != null) {
                                for (Object arg : indy.bsmArgs) {
                                    String s = arg != null ? arg.toString() : null;
                                    if ((bad = findForbiddenClientPrefix(s, forbiddenPrefixes)) != null) {
                                        violations.add(rel + "#" + method.name + " indy arg " + s + " (" + bad + ")");
                                    }
                                }
                            }
                        } else if (insn instanceof org.objectweb.asm.tree.LdcInsnNode ldc && ldc.cst instanceof Type t) {
                            if ((bad = findForbiddenClientPrefix(t.getDescriptor(), forbiddenPrefixes)) != null) {
                                violations.add(rel + "#" + method.name + " ldc " + t.getDescriptor() + " (" + bad + ")");
                            }
                        }
                    }
                }
            }
        }

        check(verifiedClientOnlyPoseClasses >= 10,
                "Expected at least 10 @Environment(EnvType.CLIENT) api.pose classes, found " + verifiedClientOnlyPoseClasses);
        check(scannedNonClientClasses >= 700,
                "Expected at least 700 non-client classes scanned, found " + scannedNonClientClasses);
        check(violations.isEmpty(),
                "Dedicated-server isolation violations (" + violations.size() + "): "
                        + String.join("; ", violations.stream().limit(15).toList()));
    }
}
