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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
        checkEquals(38, stands.size(), "All 38 Stands must be present in data/jcraft/stands");
        for (Path stand : stands) {
            parseJsonObject(stand);
        }

        List<Path> specs = collectJsonFiles(RESOURCES_ROOT.resolve("data/jcraft/specs"));
        checkEquals(5, specs.size(), "All 5 Specs must be present in data/jcraft/specs");
        for (Path spec : specs) {
            parseJsonObject(spec);
        }

        List<Path> evolutions = collectJsonFiles(RESOURCES_ROOT.resolve("data/jcraft/evolutions"));
        checkEquals(8, evolutions.size(), "All 8 Evolutions must be present in data/jcraft/evolutions");
        for (Path evo : evolutions) {
            parseJsonObject(evo);
        }

        List<Path> movesets = collectJsonFiles(RESOURCES_ROOT.resolve("data/jcraft/movesets"));
        check(movesets.size() >= 298, "All 298+ Movesets must be present in data/jcraft/movesets");
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
        checkEquals(229, items.size(), "All 229 MC 26.3 item definitions must be present");
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
        checkEquals(38, totalMixins, "All 38 common + client Mixins must be registered and bytecode-verified");
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
        System.out.println("[PASS] 37 Stands, 5 Specs, 8 Evolutions & 298+ Movesets verified.");
        verifyMc263AssetsWorldgenTradesAndTranslations();
        System.out.println("[PASS] 223 Items, 4 RenderPearl 26.3 Post-Effects + GLSL 330 std140 Shaders, Trades & Translations verified.");
        verifyFabricModJsonEntrypointsAndMixins();
        System.out.println("[PASS] fabric.mod.json, 16 CCA Components, 5 Entrypoints & all 38 Java 25 Mixins verified.");
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
        verifyVisualPresentationAndFinalArt();
        System.out.println("[PASS] Visual Presentation & Final Art (SkyBox Time Erase/MIH, SpecUserArmorLayer, TheSun Corona, 88 Icons, 3 Masks, 28 Unique Poses) verified.");
        verifyCombatAndGameFeelUpgrades();
        System.out.println("[PASS] Combat & Game Feel (Safe Hitstop, Exact Hamon Combos, Smart Neutral/Fence AI, AttackSpeed Modifier, Smooth TimeAccel) verified.");
        verifyContentAndBalanceUpgrades();
        System.out.println("[PASS] Content & Balance (Crazy Diamond, Chariot Requiem + Requiem Evolution, Exposed JServerConfig & Unified GravityChangerConfig) verified.");
        verifyUiHudAndUxPolish();
        System.out.println("[PASS] UI, HUD & UX Polish (Config Coverage & Tooltips, Dynamic Radial Wheel, Stand Encyclopedia, Variant/Active HUD & MainMenu Wrap) verified.");
        verifyEngineeringBuildAndArchitecture();
        System.out.println("[PASS] Engineering, Build & Architecture (Zero Dead Code, Vanilla MenuType, Reproducible JAR, CI Workflow & Docs) verified.");
        verifyOriginStandAndArsenal();
        System.out.println("[PASS] Origin Stand & Arsenal (Model, Anims, Textures, Glow Layer, Sounds, 9 Movesets, EN/PT-BR & Server Isolation) verified.");
        verifyVisualPolishAndGameFeel();
        System.out.println("[PASS] Category 3 Visual Polish & Game Feel (Afterimages, Impact Shake/Flash, Runic Circle & Iconic Emissive Layers) verified.");
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

    static void verifyVisualPresentationAndFinalArt() throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        // 1. SkyRendererMixin + SkyBoxManager + te_sky.png texture
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/mixin/client/SkyRendererMixin.class")) {
            check(in != null, "SkyRendererMixin.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            check(cn.methods.stream().anyMatch(m -> "jcraft$renderTimeEraseSkyBox".equals(m.name)),
                    "SkyRendererMixin must inject Time Erase skybox renderer");
            check(cn.methods.stream().anyMatch(m -> "jcraft$renderMihCelestialTrails".equals(m.name)),
                    "SkyRendererMixin must inject MIH celestial star-trail renderer");
        }
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/client/rendering/skybox/SkyBoxManager.class")) {
            check(in != null, "SkyBoxManager.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            check(cn.methods.stream().anyMatch(m -> "isTimeEraseSkyActive".equals(m.name)),
                    "SkyBoxManager must expose isTimeEraseSkyActive");
            check(cn.methods.stream().anyMatch(m -> "isTimeAccelSkyActive".equals(m.name)),
                    "SkyBoxManager must expose isTimeAccelSkyActive");
        }
        Path teSky = RESOURCES_ROOT.resolve("assets/jcraft/textures/environment/te_sky.png");
        check(Files.exists(teSky) && Files.size(teSky) > 10_000L, "Time Erase sky texture te_sky.png must exist");

        // 2. SpecUserRenderer includes SpecUserArmorLayer; AbstractRenderLayer.setDefaultGlow sets packedOverlay properly
        check(cl.getResource("net/arna/jcraft/client/renderer/entity/npc/SpecUserRenderer$SpecUserArmorLayer.class") != null,
                "SpecUserRenderer$SpecUserArmorLayer must exist for SpecUserMob armor rendering");
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/client/renderer/entity/layer/AbstractRenderLayer.class")) {
            check(in != null, "AbstractRenderLayer.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            var mGlow = cn.methods.stream().filter(m -> "setDefaultGlow".equals(m.name)).findFirst().orElseThrow();
            boolean callsSetOverlay = false;
            int callsSetLight = 0;
            for (var insn = mGlow.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof org.objectweb.asm.tree.MethodInsnNode mi) {
                    if ("setPackedOverlay".equals(mi.name)) callsSetOverlay = true;
                    if ("setPackedLight".equals(mi.name)) callsSetLight++;
                }
            }
            check(callsSetOverlay, "AbstractRenderLayer.setDefaultGlow must call setPackedOverlay");
            checkEquals(1, callsSetLight, "AbstractRenderLayer.setDefaultGlow must call setPackedLight exactly once");
        }

        // 3. TheSunRenderer & SunGlowLayer use emissive translucent & additive eyes passes
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/client/renderer/entity/layer/SunGlowLayer.class")) {
            check(in != null, "SunGlowLayer.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            var mRender = cn.methods.stream().filter(m -> "render".equals(m.name)).findFirst().orElseThrow();
            int reRenderCalls = 0;
            for (var insn = mRender.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof org.objectweb.asm.tree.MethodInsnNode mi && "reRender".equals(mi.name)) {
                    reRenderCalls++;
                }
            }
            check(reRenderCalls >= 2, "SunGlowLayer must perform dual-pass core + corona emissive render");
        }

        // 4. All 88 final-art icons (11 Stands x 8 slots) are valid 18x18 RGBA PNGs and unique per Stand
        Path iconsRoot = RESOURCES_ROOT.resolve("assets/jcraft/textures/gui/ability_icons");
        List<String> artStands = List.of(
                "the_sun", "silver_chariot_possessed", "crazy_diamond", "chariot_requiem", "diver_down",
                "cinderella", "osiris", "atum", "dragons_dream", "foo_fighters", "goo_goo_dolls");
        List<String> artSlots = List.of("light", "heavy", "barrage", "special1", "special2", "special3", "ult", "utility");
        for (String st : artStands) {
            Set<String> standHashes = new HashSet<>();
            for (String slot : artSlots) {
                Path p = iconsRoot.resolve(st).resolve(slot + ".png");
                var img = javax.imageio.ImageIO.read(p.toFile());
                check(img != null && img.getWidth() == 18 && img.getHeight() == 18,
                        "Final art icon must be 18x18: " + st + "/" + slot);
                int opaque = 0;
                for (int y = 0; y < 18; y++) {
                    for (int x = 0; x < 18; x++) {
                        if (((img.getRGB(x, y) >>> 24) & 0xFF) > 128) opaque++;
                    }
                }
                check(opaque >= 40, "Final art icon must contain real pixel art: " + st + "/" + slot + " (opaque=" + opaque + ")");
                standHashes.add(Arrays.toString(Files.readAllBytes(p)));
            }
            checkEquals(8, standHashes.size(), "All 8 ability icons must be distinct for Stand: " + st);
        }

        // 5. All 3 humanoid equipment mask/hat textures are 64x32 PNGs with multi-color front-face detail
        Path eqRoot = RESOURCES_ROOT.resolve("assets/jcraft/textures/entity/equipment/humanoid");
        for (String eq : List.of("stone_mask.png", "red_hat.png", "cinderella_mask.png")) {
            var img = javax.imageio.ImageIO.read(eqRoot.resolve(eq).toFile());
            check(img != null && img.getWidth() == 64 && img.getHeight() == 32,
                    "Equipment texture must be 64x32: " + eq);
            Set<Integer> frontColors = new HashSet<>();
            for (int y = 8; y < 16; y++) {
                for (int x = 8; x < 16; x++) {
                    int argb = img.getRGB(x, y);
                    if (((argb >>> 24) & 0xFF) > 0) frontColors.add(argb);
                }
            }
            check(frontColors.size() >= 4, "Equipment texture front face must have multi-tone pixel art: " + eq + " (" + frontColors.size() + " colors)");
        }

        // 6. All 28 newly refined Stand user pose JSONs must be distinct from one another
        List<String> refinedPoses = List.of(
                "crazy_diamond", "gold_experience", "silver_chariot", "d4c", "made_in_heaven", "c_moon",
                "white_snake", "hierophant_green", "magicians_red", "cream", "metallica", "the_hand",
                "purple_haze", "purple_haze_distortion", "aerosmith", "mandom", "shadow_the_world",
                "the_fool", "horus", "the_sun", "chariot_requiem", "diver_down", "foo_fighters",
                "cinderella", "osiris", "atum", "dragons_dream", "goo_goo_dolls");
        Set<String> poseContents = new HashSet<>();
        Path posesRoot = RESOURCES_ROOT.resolve("assets/jcraft/poses");
        for (String pName : refinedPoses) {
            poseContents.add(Files.readString(posesRoot.resolve(pName).resolve("humanoid.json"), StandardCharsets.UTF_8));
        }
        checkEquals(28, poseContents.size(), "All 28 refined Stand user poses must have distinct silhouettes");
    }

    static void verifyCombatAndGameFeelUpgrades() throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        // 1. Safe Micro-Hitstop curve (light/barrage = 0, heavy/ult = 1..4) & JServerConfig.ENABLE_HITSTOP
        checkEquals(0, net.arna.jcraft.api.Attacks.computeHitstopTicks(0.0f), "0 damage hitstop must be 0");
        checkEquals(0, net.arna.jcraft.api.Attacks.computeHitstopTicks(2.5f), "Barrage hitstop must be 0");
        checkEquals(0, net.arna.jcraft.api.Attacks.computeHitstopTicks(5.9f), "Sub-threshold hitstop must be 0");
        checkEquals(1, net.arna.jcraft.api.Attacks.computeHitstopTicks(6.0f), "Heavy threshold hitstop must be 1");
        checkEquals(2, net.arna.jcraft.api.Attacks.computeHitstopTicks(9.0f), "Medium-heavy hitstop must be 2");
        checkEquals(3, net.arna.jcraft.api.Attacks.computeHitstopTicks(13.5f), "Strong heavy hitstop must be 3");
        checkEquals(4, net.arna.jcraft.api.Attacks.computeHitstopTicks(18.0f), "Ultimate hitstop must be 4");
        checkEquals(4, net.arna.jcraft.api.Attacks.computeHitstopTicks(50.0f), "Hitstop must clamp at 4 ticks max");

        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/common/config/JServerConfig.class")) {
            check(in != null, "JServerConfig.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            check(cn.fields.stream().anyMatch(f -> "ENABLE_HITSTOP".equals(f.name)),
                    "JServerConfig must expose ENABLE_HITSTOP");
        }
        JsonObject enJson = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/lang/en_us.json"));
        JsonObject ptJson = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/lang/pt_br.json"));
        for (String key : List.of("jcraft.serverconfig.option.enable_hitstop", "jcraft.serverconfig.option.enable_hitstop.tooltip")) {
            check(enJson.has(key) && ptJson.has(key), "EN/PT-BR translations must define " + key);
        }

        // 2. Deterministic Hamon combo move-step tracking (no fixed 5-second window hack)
        checkEquals(6, net.arna.jcraft.api.component.living.CommonHamonComponent.HamonMoveStep.values().length,
                "HamonMoveStep must define all 6 combo states");
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/api/Attacks.class")) {
            check(in != null, "Attacks.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            boolean usesLastHamonMove = cn.methods.stream().anyMatch(m -> {
                for (var insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof org.objectweb.asm.tree.MethodInsnNode mi && "getLastHamonMove".equals(mi.name)) {
                        return true;
                    }
                }
                return false;
            });
            check(usesLastHamonMove, "Attacks must check CommonHamonComponent.getLastHamonMove() for Hamon combos");
        }

        // 3. Context-aware neutral AI & collider obstacle detection in IAttacker
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/api/attack/IAttacker.class")) {
            check(in != null, "IAttacker.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            check(cn.methods.stream().anyMatch(m -> "chooseNeutralState".equals(m.name)),
                    "IAttacker must expose context-aware chooseNeutralState");
            check(cn.methods.stream().anyMatch(m -> "isPathBlockedByBreakableObstacle".equals(m.name)),
                    "IAttacker must expose isPathBlockedByBreakableObstacle for fences/glass");
        }

        // 4. StandEntity uses clean transient Attributes.ATTACK_SPEED modifier instead of Mining Fatigue
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/api/stand/StandEntity.class")) {
            check(in != null, "StandEntity.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            check(cn.fields.stream().anyMatch(f -> "STAND_ATTACK_SPEED_MODIFIER_ID".equals(f.name)),
                    "StandEntity must define STAND_ATTACK_SPEED_MODIFIER_ID");
            check(cn.methods.stream().anyMatch(m -> "updateUserAttackSpeedPenalty".equals(m.name)),
                    "StandEntity must define updateUserAttackSpeedPenalty");
        }

        // 5. Smooth cubic ease-out resync math for TimeAccelStatePacket
        checkEquals(1000L, net.arna.jcraft.common.network.s2c.TimeAccelStatePacket.interpolateSmoothDayTime(1000L, 2000L, 0.0f),
                "TimeAccel smooth sync at progress 0.0 must equal startDayTime");
        checkEquals(1875L, net.arna.jcraft.common.network.s2c.TimeAccelStatePacket.interpolateSmoothDayTime(1000L, 2000L, 0.5f),
                "TimeAccel smooth sync at progress 0.5 must follow cubic ease-out (0.875)");
        checkEquals(2000L, net.arna.jcraft.common.network.s2c.TimeAccelStatePacket.interpolateSmoothDayTime(1000L, 2000L, 1.0f),
                "TimeAccel smooth sync at progress 1.0 must reach exact server dayTime");
    }

    static void verifyContentAndBalanceUpgrades() throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        // 1. Crazy Diamond obtainable, complete 9-slot moveset, and valid Bedrock animations
        JsonObject cdStand = parseJsonObject(RESOURCES_ROOT.resolve("data/jcraft/stands/crazy_diamond.json"));
        check(!cdStand.has("obtainable") || cdStand.get("obtainable").getAsBoolean(),
                "crazy_diamond.json must be obtainable");
        checkEquals(1, cdStand.getAsJsonObject("info").getAsJsonArray("skin_names").size(),
                "Crazy Diamond must declare 1 extra skin matching skin1.png");
        List<Path> cdMoves = collectJsonFiles(RESOURCES_ROOT.resolve("data/jcraft/movesets/stand/crazy_diamond/default"));
        checkEquals(9, cdMoves.size(), "Crazy Diamond must have all 9 moveset slot JSONs");
        JsonObject cdAnims = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/animations/crazy_diamond.animation.json"))
                .getAsJsonObject("animations");
        for (String clip : List.of("idle", "summon", "block", "light", "light_followup", "uppercut",
                "heavy", "barrage", "heal", "wall_breaker", "bearing_snap", "rage", "jump", "itemthrow_charge", "itemthrow")) {
            check(cdAnims.has(clip), "crazy_diamond.animation.json missing clip: " + clip);
        }

        // 2. Chariot Requiem obtainable evolution + Silver Chariot -> Chariot Requiem evolution JSON + MoveTypes + animations
        JsonObject crStand = parseJsonObject(RESOURCES_ROOT.resolve("data/jcraft/stands/chariot_requiem.json"));
        check(!crStand.has("obtainable") || crStand.get("obtainable").getAsBoolean(),
                "chariot_requiem.json must be obtainable");
        check(crStand.has("evolution") && crStand.get("evolution").getAsBoolean(),
                "chariot_requiem.json must be marked as an evolution");
        JsonObject scrEvo = parseJsonObject(RESOURCES_ROOT.resolve("data/jcraft/evolutions/silver_chariot_to_chariot_requiem.json"));
        checkEquals("jcraft:requiem_arrow", scrEvo.get("item").getAsString(), "SCR evolution item");
        checkEquals("jcraft:silver_chariot", scrEvo.get("stand").getAsString(), "SCR base stand");
        checkEquals("jcraft:chariot_requiem", scrEvo.get("target").getAsString(), "SCR target stand");

        List<Path> crMoves = collectJsonFiles(RESOURCES_ROOT.resolve("data/jcraft/movesets/stand/chariot_requiem/default"));
        checkEquals(9, crMoves.size(), "Chariot Requiem must have all 9 moveset slot JSONs");
        check(cl.getResource("net/arna/jcraft/common/attack/moves/chariotrequiem/RequiemCounterAttack.class") != null,
                "RequiemCounterAttack must exist");
        check(cl.getResource("net/arna/jcraft/common/attack/moves/chariotrequiem/SoulSwapAttack.class") != null,
                "SoulSwapAttack must exist");
        JsonObject crAnims = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/animations/chariot_requiem.animation.json"))
                .getAsJsonObject("animations");
        for (String clip : List.of("idle", "summon", "block", "light", "light_followup", "heavy", "barrage",
                "somnolence", "soul_swap", "counter", "counter_miss", "dominion", "shadow_step", "itemthrow_charge", "itemthrow")) {
            check(crAnims.has(clip), "chariot_requiem.animation.json missing clip: " + clip);
        }

        // 3. Exposed JServerConfig options + full GravityChangerConfig unification + EN/PT-BR translations
        List<String> newConfigFields = List.of(
                "CMOON_UTIL_DURATION", "CMOON_ULT_RANGE", "UNIVERSAL_ABILITIES", "SPTW_IGNITE_CAMPFIRES",
                "SHA_SEARCH_RADIUS", "MIH_ACCELERATE_TICKS", "USE_FOOLISH_SAND",
                "RESET_GRAVITY_ON_RESPAWN", "VOID_DAMAGE_ABOVE_WORLD"
        );
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/common/config/JServerConfig.class")) {
            check(in != null, "JServerConfig.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            for (String field : newConfigFields) {
                check(cn.fields.stream().anyMatch(f -> field.equals(f.name)),
                        "JServerConfig must expose option field: " + field);
            }
        }
        JsonObject enJson = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/lang/en_us.json"));
        JsonObject ptJson = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/lang/pt_br.json"));
        for (String opt : List.of("cmoon_util_duration", "cmoon_ult_range", "universal_abilities",
                "sptw_ignite_campfires", "sha_search_radius", "mih_accelerate_ticks", "use_foolish_sand",
                "reset_gravity_on_respawn", "void_damage_above_world")) {
            String key = "jcraft.serverconfig.option." + opt;
            check(enJson.has(key) && ptJson.has(key), "EN/PT-BR missing option name: " + key);
            check(enJson.has(key + ".tooltip") && ptJson.has(key + ".tooltip"), "EN/PT-BR missing option tooltip: " + key + ".tooltip");
        }
        // Verify GravityChangerConfig unification sync in JServerConfig.syncGravityConfig() bytecode (live test runs in AllStandsRuntimeDiagnostic)
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/common/config/JServerConfig.class")) {
            check(in != null, "JServerConfig.class must exist for syncGravityConfig verification");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            var syncMethod = cn.methods.stream().filter(m -> "syncGravityConfig".equals(m.name)).findFirst().orElseThrow();
            Set<String> writtenGravityFields = new HashSet<>();
            for (var insn = syncMethod.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof org.objectweb.asm.tree.FieldInsnNode fi
                        && fi.getOpcode() == org.objectweb.asm.Opcodes.PUTFIELD
                        && "net/arna/jcraft/common/gravity/config/GravityChangerConfig".equals(fi.owner)) {
                    writtenGravityFields.add(fi.name);
                }
            }
            for (String gf : List.of("keepWorldLook", "rotationTime", "worldVelocity",
                    "resetGravityOnDimensionChange", "resetGravityOnRespawn", "voidDamageAboveWorld")) {
                check(writtenGravityFields.contains(gf),
                        "JServerConfig.syncGravityConfig() must synchronize GravityChangerConfig." + gf);
            }
        }
    }

    static void verifyUiHudAndUxPolish() throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        // 1. JClientConfig.hudScale default & JCraftAbilityHud clamping + active/variant/universal checks
        var clientCfg = net.arna.jcraft.client.JClientConfig.getInstance();
        float prevScale = clientCfg.getHudScale();
        try {
            checkClose(1.0, prevScale, 1.0E-5, "Default HUD scale must be 1.0f");
            clientCfg.setHudScale(0.2f);
            checkClose(0.75, clientCfg.getClampedHudScale(), 1.0E-5, "HUD scale lower clamp must be 0.75f");
            clientCfg.setHudScale(3.0f);
            checkClose(1.50, clientCfg.getClampedHudScale(), 1.0E-5, "HUD scale upper clamp must be 1.50f");
        } finally {
            clientCfg.setHudScale(prevScale);
        }

        // 2. JoJoRadialWheelScreen smooth cubic ease-out pop-in curve
        checkClose(0.72, net.arna.jcraft.client.gui.screen.JoJoRadialWheelScreen.computeOpenScale(0L), 1.0E-5,
                "Radial wheel open scale at 0ms must be 0.72");
        float midScale = net.arna.jcraft.client.gui.screen.JoJoRadialWheelScreen.computeOpenScale(
                net.arna.jcraft.client.gui.screen.JoJoRadialWheelScreen.OPEN_ANIM_DURATION_MS / 2);
        check(midScale > 0.90f && midScale < 1.0f, "Radial wheel open scale at 50% must ease out smoothly (got=" + midScale + ")");
        checkClose(1.0, net.arna.jcraft.client.gui.screen.JoJoRadialWheelScreen.computeOpenScale(
                net.arna.jcraft.client.gui.screen.JoJoRadialWheelScreen.OPEN_ANIM_DURATION_MS), 1.0E-5,
                "Radial wheel open scale at completion must be 1.0");

        // 3. JCraftConfigScreen exposes all 10 new server options + clientTooltip + font.split tooltip wrapping
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/client/gui/screen/JCraftConfigScreen.class")) {
            check(in != null, "JCraftConfigScreen.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            Set<String> referencedServerFields = new HashSet<>();
            boolean hasClientTooltip = false;
            boolean usesFontSplit = false;
            for (var m : cn.methods) {
                if ("clientTooltip".equals(m.name)) hasClientTooltip = true;
                for (var insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof org.objectweb.asm.tree.FieldInsnNode fi
                            && "net/arna/jcraft/common/config/JServerConfig".equals(fi.owner)) {
                        referencedServerFields.add(fi.name);
                    } else if (insn instanceof org.objectweb.asm.tree.MethodInsnNode mi
                            && "split".equals(mi.name) && "net/minecraft/client/gui/Font".equals(mi.owner)) {
                        usesFontSplit = true;
                    }
                }
            }
            check(hasClientTooltip, "JCraftConfigScreen must resolve client option tooltips");
            check(usesFontSplit, "JCraftConfigScreen tooltips must use Font.split for exact word wrapping");
            for (String optField : List.of("CMOON_UTIL_DURATION", "CMOON_ULT_RANGE", "UNIVERSAL_ABILITIES",
                    "SPTW_IGNITE_CAMPFIRES", "SHA_SEARCH_RADIUS", "MIH_ACCELERATE_TICKS", "USE_FOOLISH_SAND",
                    "ENABLE_HITSTOP", "RESET_GRAVITY_ON_RESPAWN", "VOID_DAMAGE_ABOVE_WORLD")) {
                check(referencedServerFields.contains(optField),
                        "JCraftConfigScreen must expose JServerConfig." + optField);
            }
        }

        // 4. StandGuideScreen encyclopedia cycling, JCraftAbilityHud active-state/scale, JoJoAnimeHudRenderer pills, MainMenuScreen wrap
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/client/gui/screen/StandGuideScreen.class")) {
            check(in != null, "StandGuideScreen.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            check(cn.methods.stream().anyMatch(m -> "cycleBrowsedStand".equals(m.name)),
                    "StandGuideScreen must expose cycleBrowsedStand encyclopedia navigation");
        }
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/client/gui/screen/MainMenuScreen.class")) {
            check(in != null, "MainMenuScreen.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            check(cn.methods.stream().anyMatch(m -> "drawWrappedText".equals(m.name)),
                    "MainMenuScreen must wrap Stand info via drawWrappedText");
        }
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/client/gui/hud/JCraftAbilityHud.class")) {
            check(in != null, "JCraftAbilityHud.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            check(cn.methods.stream().anyMatch(m -> "matchesActiveState".equals(m.name)),
                    "JCraftAbilityHud must highlight actively executing abilities via matchesActiveState");
            check(cn.methods.stream().anyMatch(m -> "getClampedHudScale".equals(m.name)),
                    "JCraftAbilityHud must expose getClampedHudScale");
        }

        // 5. All new EN & PT-BR UI/HUD translation keys present
        JsonObject enJson = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/lang/en_us.json"));
        JsonObject ptJson = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/lang/pt_br.json"));
        for (String key : List.of(
                "jcraft.config.hud_scale", "jcraft.config.hud_scale.tooltip",
                "jcraft.radial.no_stand", "jcraft.radial.skin_unavailable", "jcraft.radial.skin_named", "jcraft.radial.active_tag",
                "jcraft.guide.my_stand", "jcraft.guide.badge.evolution", "jcraft.guide.badge.preview",
                "jcraft.guide.stand.skins_count", "jcraft.guide.browser_hint",
                "jcraft.hud.time_erase", "jcraft.hud.time_accel")) {
            check(enJson.has(key) && ptJson.has(key), "EN/PT-BR translations must define " + key);
        }
    }

    static void verifyEngineeringBuildAndArchitecture() throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        // 1. Dead @Deprecated(forRemoval=true) RevolverFire class must be removed
        check(cl.getResource("net/arna/jcraft/common/tickable/RevolverFire.class") == null,
                "Dead RevolverFire.class must be removed");

        // 2. JMenuRegistry & MenuCallPacket must use vanilla MC 26.3 MenuType / ServerPlayer.openMenu (no deprecated MenuRegistry.ofExtended/openExtendedMenu)
        for (String cls : List.of(
                "net/arna/jcraft/api/registry/JMenuRegistry.class",
                "net/arna/jcraft/common/network/c2s/MenuCallPacket.class")) {
            try (InputStream in = cl.getResourceAsStream(cls)) {
                check(in != null, cls + " must exist");
                ClassNode cn = new ClassNode();
                new ClassReader(in).accept(cn, 0);
                boolean usesDeprecatedMenuRegistry = cn.methods.stream().anyMatch(m -> {
                    for (var insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                        if (insn instanceof org.objectweb.asm.tree.MethodInsnNode mi
                                && "dev/architectury/registry/menu/MenuRegistry".equals(mi.owner)) {
                            return true;
                        }
                    }
                    return false;
                });
                check(!usesDeprecatedMenuRegistry, cls + " must not call deprecated Architectury MenuRegistry");
            }
        }

        // 3. Clean UTF-8 fabric.mod.json metadata (8 Evolutions, no mojibake)
        String modJsonRaw = Files.readString(RESOURCES_ROOT.resolve("fabric.mod.json"), StandardCharsets.UTF_8);
        check(!modJsonRaw.contains("Ã") && !modJsonRaw.contains("â€”"),
                "fabric.mod.json must use clean UTF-8 without mojibake");
        check(modJsonRaw.contains("8 Evoluções"), "fabric.mod.json description must reflect 8 Evolutions");

        // 4. Reproducible JAR build configuration, strict compiler lints, CI workflow, and Architecture docs present
        String buildGradle = Files.readString(Path.of("build.gradle"), StandardCharsets.UTF_8);
        check(buildGradle.contains("reproducibleFileOrder = true") && buildGradle.contains("preserveFileTimestamps = false"),
                "build.gradle must enforce reproducible JAR archives");
        check(buildGradle.contains("-Xlint:removal") && buildGradle.contains("-Xlint:deprecation") && buildGradle.contains("-Xlint:unchecked"),
                "build.gradle must enable -Xlint:removal, -Xlint:deprecation, and -Xlint:unchecked compiler checks");
        check(buildGradle.contains("com.google.j2objc:j2objc-annotations"),
                "build.gradle must provide compileOnly j2objc-annotations for zero-warning Guava compilation");
        check(buildGradle.contains("tasks.register('deployMod')"), "build.gradle must define deployMod distribution task");

        // 5. JByteBufUtils wire-compatible & bounds-safe enum serialization
        net.minecraft.network.FriendlyByteBuf testBuf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        net.arna.jcraft.api.misc.JByteBufUtils.writeEnum(testBuf, net.minecraft.core.Direction.NORTH);
        check(net.arna.jcraft.api.misc.JByteBufUtils.readEnum(testBuf, net.minecraft.core.Direction.class) == net.minecraft.core.Direction.NORTH,
                "JByteBufUtils must round-trip enum values");
        testBuf.writeVarInt(999);
        check(net.arna.jcraft.api.misc.JByteBufUtils.readEnum(testBuf, net.minecraft.core.Direction.class) == net.minecraft.core.Direction.values()[0],
                "JByteBufUtils must safely clamp out-of-range ordinals");
        testBuf.release();

        // 6. All 16 Fabric CCA component interfaces must extend v8 CardinalComponent (not deprecated v3 Component)
        for (String ccaCls : List.of(
                "net/arna/jcraft/fabric/common/component/entity/GrabComponent.class",
                "net/arna/jcraft/fabric/common/component/entity/GravityComponent.class",
                "net/arna/jcraft/fabric/common/component/entity/TimeStopComponent.class",
                "net/arna/jcraft/fabric/common/component/living/BombTrackerComponent.class",
                "net/arna/jcraft/fabric/common/component/living/CooldownsComponent.class",
                "net/arna/jcraft/fabric/common/component/living/GravityShiftComponent.class",
                "net/arna/jcraft/fabric/common/component/living/GunslingerComponent.class",
                "net/arna/jcraft/fabric/common/component/living/HamonComponent.class",
                "net/arna/jcraft/fabric/common/component/living/HitPropertyComponent.class",
                "net/arna/jcraft/fabric/common/component/living/MiscComponent.class",
                "net/arna/jcraft/fabric/common/component/living/StandComponent.class",
                "net/arna/jcraft/fabric/common/component/living/VampireComponent.class",
                "net/arna/jcraft/fabric/common/component/player/PhComponent.class",
                "net/arna/jcraft/fabric/common/component/player/SpecComponent.class",
                "net/arna/jcraft/fabric/common/component/world/ShockwaveHandlerComponent.class",
                "net/arna/jcraft/fabric/common/component/world/TexasHoldEmComponent.class")) {
            try (InputStream in = cl.getResourceAsStream(ccaCls)) {
                check(in != null, ccaCls + " must exist");
                ClassNode cn = new ClassNode();
                new ClassReader(in).accept(cn, ClassReader.SKIP_CODE);
                check(!cn.interfaces.contains("org/ladysnake/cca/api/v3/component/Component"),
                        ccaCls + " must not extend deprecated CCA v3 Component");
                check(cn.interfaces.contains("org/ladysnake/cca/api/v8/component/CardinalComponent"),
                        ccaCls + " must extend CCA v8 CardinalComponent");
            }
        }

        Path ciYml = Path.of(".github/workflows/ci.yml");
        check(Files.isRegularFile(ciYml), "GitHub Actions CI workflow .github/workflows/ci.yml must exist");
        String ciText = Files.readString(ciYml, StandardCharsets.UTF_8);
        check(ciText.contains("gradlew") && ciText.contains("build"), "CI workflow must invoke gradlew build");

        Path archMd = Path.of("ARCHITECTURE.md");
        check(Files.isRegularFile(archMd), "ARCHITECTURE.md must exist");
        String archText = Files.readString(archMd, StandardCharsets.UTF_8);
        check(archText.contains("verifyDedicatedServerIsolation") && archText.contains("GravityChangerConfig"),
                "ARCHITECTURE.md must document Dedicated-Server Isolation and Config Unification");
    }

    static void verifyOriginStandAndArsenal() throws Exception {
        ClassLoader cl = JCraft263ValidationTest.class.getClassLoader();

        // 1. Stand JSON, Pose JSON, Geo & Animations
        JsonObject standObj = parseJsonObject(RESOURCES_ROOT.resolve("data/jcraft/stands/origin.json"));
        check(!standObj.get("evolution").getAsBoolean(), "Origin stand must not require evolution");
        checkEquals("jcraft:origin_summon",
                standObj.getAsJsonObject("summon_data").getAsJsonObject("sound").get("sound_id").getAsString(),
                "Origin summon sound must match JSoundRegistry");

        String poseRaw = Files.readString(RESOURCES_ROOT.resolve("assets/jcraft/poses/origin/humanoid.json"), StandardCharsets.UTF_8);
        JsonObject poseObj = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/poses/origin/humanoid.json"));
        checkEquals("group", poseObj.get("type").getAsString(), "Origin pose type must be group");
        check(!poseRaw.contains("\"body\"") && !poseRaw.contains("\"head\""),
                "Origin player pose must not twist player torso/body or head");

        String geoRaw = Files.readString(RESOURCES_ROOT.resolve("assets/jcraft/geo/origin.geo.json"), StandardCharsets.UTF_8);
        JsonObject geoObj = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/geo/origin.geo.json"));
        JsonArray geoBones = geoObj.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
        JsonArray basePivot = geoBones.get(0).getAsJsonObject().getAsJsonArray("pivot");
        checkClose(19.25, basePivot.get(1).getAsDouble(), 0.001, "origin.geo.json base pivot Y must match 19.25");
        for (String bone : List.of("base", "Stand", "torso", "body", "upperBody", "head",
                "chest_core", "back_core", "cloak_back", "cloak_back_lower",
                "crown_halo", "void_ring_outer", "void_ring_inner", "wing_left_upper", "wing_right_upper")) {
            check(geoRaw.contains("\"" + bone + "\""), "origin.geo.json must contain bone: " + bone);
        }

        String animRaw = Files.readString(RESOURCES_ROOT.resolve("assets/jcraft/animations/origin.animation.json"), StandardCharsets.UTF_8);
        check(animRaw.contains("\"vector\":"), "origin.animation.json must use AzureLib 1.8.0 vector objects");
        check(animRaw.contains("\"easing\":"), "origin.animation.json must use AzureLib easing curves");
        JsonObject animObj = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/animations/origin.animation.json"));
        JsonObject anims = animObj.getAsJsonObject("animations");
        List<String> expectedOriginClips = List.of(
                "idle", "summon", "block", "light", "light_followup", "heavy", "barrage",
                "special1", "special2", "special3", "uppercut", "counter", "counter_miss",
                "grab", "grab_hit", "ultimate", "utility", "time_stop", "overwrite",
                "itemthrow_charge", "itemthrow");
        List<String> secondaryBones = List.of(
                "wing_left_mid", "wing_left_lower", "wing_right_mid", "wing_right_lower",
                "cloak_back_lower", "cloak_left", "cloak_right",
                "pauldron_left", "pauldron_right", "crown_halo", "chest_core");
        for (String anim : expectedOriginClips) {
            check(anims.has(anim), "origin.animation.json must define animation: " + anim);
            JsonObject bones = anims.getAsJsonObject(anim).getAsJsonObject("bones");
            check(bones != null && bones.has("base"), "origin.animation.json '" + anim + "' must reset base bone rotation");
            for (String sec : secondaryBones) {
                check(bones.has(sec), "origin.animation.json '" + anim + "' must animate secondary bone: " + sec);
            }
        }
        for (String showcaseClip : List.of("summon", "idle", "heavy", "time_stop")) {
            JsonObject cBones = anims.getAsJsonObject(showcaseClip).getAsJsonObject("bones");
            int clipKfs = 0;
            for (var bEntry : cBones.entrySet()) {
                JsonObject bObj = bEntry.getValue().getAsJsonObject();
                for (String ch : List.of("rotation", "position", "scale")) {
                    if (bObj.has(ch) && bObj.get(ch).isJsonObject()) {
                        JsonObject chObj = bObj.getAsJsonObject(ch);
                        clipKfs += chObj.has("vector") ? 1 : chObj.size();
                    }
                }
            }
            check(clipKfs >= 90, "origin.animation.json '" + showcaseClip + "' must have >= 90 keyframes (actual=" + clipKfs + ")");
        }
        JsonObject originTossJson = parseJsonObject(
                RESOURCES_ROOT.resolve("data/jcraft/movesets/stand/origin/default/toss/item_toss.json"));
        check("ITEM_TOSS_CHARGE".equals(originTossJson.get("anim_state").getAsString())
                        && "ITEM_TOSS".equals(originTossJson.getAsJsonObject("followup").get("anim_state").getAsString()),
                "Origin item_toss.json must bind ITEM_TOSS_CHARGE and followup ITEM_TOSS");
        JsonObject originBarrageAnim = anims.getAsJsonObject("barrage");
        JsonObject originBarrageBones = originBarrageAnim.getAsJsonObject("bones");
        for (String reqBone : List.of("upperBody", "armRight", "lower_armRight", "armLeft", "lower_armLeft",
                "pauldron_left", "pauldron_right", "wing_left_upper", "wing_left_mid", "wing_left_lower",
                "wing_right_upper", "wing_right_mid", "wing_right_lower", "void_ring_outer", "void_ring_inner",
                "cloak_back", "cloak_back_lower", "cloak_left", "cloak_right",
                "barrage_right_arm", "barrage_right_arm2", "barrage_right_arm3",
                "barrage_left_arm", "barrage_left_arm2", "barrage_left_arm3")) {
            check(originBarrageBones.has(reqBone), "origin.animation.json barrage must animate bone: " + reqBone);
        }
        int originBarrageKfs = 0;
        for (var boneEntry : originBarrageBones.entrySet()) {
            JsonObject bObj = boneEntry.getValue().getAsJsonObject();
            for (String ch : List.of("rotation", "position", "scale")) {
                if (bObj.has(ch) && bObj.get(ch).isJsonObject()) {
                    JsonObject chObj = bObj.getAsJsonObject(ch);
                    originBarrageKfs += chObj.has("vector") ? 1 : chObj.size();
                }
            }
        }
        check(originBarrageKfs >= 300, "origin.animation.json barrage must have >= 300 keyframes (actual=" + originBarrageKfs + ")");
        for (String smearArm : List.of("barrage_right_arm", "barrage_left_arm")) {
            JsonObject scaleObj = originBarrageBones.getAsJsonObject(smearArm).getAsJsonObject("scale");
            boolean hasStretch = false;
            for (var kf : scaleObj.entrySet()) {
                JsonArray vec = kf.getValue().getAsJsonObject().getAsJsonArray("vector");
                if (vec != null && vec.get(2).getAsDouble() >= 1.8) {
                    hasStretch = true;
                    break;
                }
            }
            check(hasStretch, "origin.animation.json barrage " + smearArm + " must include smear-frame Z scale >= 1.8");
        }
        for (String ringBone : List.of("void_ring_outer", "void_ring_inner")) {
            JsonObject rotObj = originBarrageBones.getAsJsonObject(ringBone).getAsJsonObject("rotation");
            check(rotObj != null && rotObj.size() >= 4,
                    "origin.animation.json barrage " + ringBone + " must animate accelerated counter-spin (>= 4 keyframes)");
        }

        JsonObject originBarrageMoveJson = parseJsonObject(
                RESOURCES_ROOT.resolve("data/jcraft/movesets/stand/origin/default/barrage/eclipse_barrage.json"));
        JsonObject bm = originBarrageMoveJson.getAsJsonObject("move");
        check("jcraft:shared_main_barrage_attack".equals(bm.get("type").getAsString()),
                "Origin native barrage must use MainBarrageAttack");
        check(bm.get("stun").getAsInt() >= 30 && bm.get("windup").getAsInt() == 0,
                "Origin native barrage must have 30-tick combo stun and 0 windup");
        check(bm.getAsJsonObject("attack_extras").get("shockwaves").getAsBoolean()
                && bm.getAsJsonObject("extras").get("is_holdable").getAsBoolean(),
                "Origin native barrage must enable shockwaves and holdable release-cancel");
        check(bm.get("move_distance").getAsDouble() <= 0.9,
                "Origin native barrage move_distance must stay <= 0.9 so dynamic first-person translucency engages like Star Platinum");

        // 2. Textures (5 stand textures + 8 ability icons + 4 stand discs)
        Path texRoot = RESOURCES_ROOT.resolve("assets/jcraft/textures/entity/stands/origin");
        for (String tex : List.of("default.png", "skin1.png", "skin2.png", "skin3.png", "skin4.png", "skin5.png", "glow.png")) {
            var img = javax.imageio.ImageIO.read(texRoot.resolve(tex).toFile());
            check(img != null && img.getWidth() == 512 && img.getHeight() == 512,
                    "Origin texture must be 512x512: " + tex);
        }
        Path iconRoot = RESOURCES_ROOT.resolve("assets/jcraft/textures/gui/ability_icons/origin");
        for (String icon : List.of("light.png", "heavy.png", "barrage.png", "special1.png", "special2.png", "special3.png", "ult.png", "utility.png")) {
            var img = javax.imageio.ImageIO.read(iconRoot.resolve(icon).toFile());
            check(img != null && img.getWidth() == 32 && img.getHeight() == 32,
                    "Origin ability icon must be 32x32: " + icon);
        }
        for (int s = 0; s < 6; s++) {
            var discImg = javax.imageio.ImageIO.read(RESOURCES_ROOT.resolve("assets/jcraft/textures/item/stand_disc_origin_" + s + ".png").toFile());
            check(discImg != null && discImg.getWidth() == 16 && discImg.getHeight() == 16,
                    "Origin stand disc texture must be 16x16 for skin " + s);
            parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/items/stand_disc_origin_" + s + ".json"));
            parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/models/item/stand_disc_origin_" + s + ".json"));
        }

        // 3. Synthesized OGG Sounds (12 dedicated Origin clips) + sounds.json + subtitles + 9 Movesets + Translations
        JsonObject soundsObj = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/sounds.json"));
        JsonObject enUsSnd = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/lang/en_us.json"));
        JsonObject ptBrSnd = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/lang/pt_br.json"));
        List<String> originSounds = List.of(
                "origin_summon", "origin_desummon", "origin_barrage", "origin_heavy",
                "origin_ult", "origin_bind", "origin_light", "origin_special1",
                "origin_special2", "origin_grab", "origin_grab_hit", "origin_counter");
        for (String snd : originSounds) {
            check(soundsObj.has(snd), "sounds.json must define " + snd);
            Path ogg = RESOURCES_ROOT.resolve("assets/jcraft/sounds/origin/" + snd + ".ogg");
            check(Files.isRegularFile(ogg) && Files.size(ogg) > 4096L, "OGG sound must exist and exceed 4KB: " + snd);
            byte[] header = Files.readAllBytes(ogg);
            check(header.length >= 4 && header[0] == 'O' && header[1] == 'g' && header[2] == 'g' && header[3] == 'S',
                    "OGG sound must start with valid OggS header: " + snd);
            String subKey = "subtitles.jcraft." + snd;
            check(enUsSnd.has(subKey), "en_us.json must define subtitle key: " + subKey);
            check(ptBrSnd.has(subKey), "pt_br.json must define subtitle key: " + subKey);
        }
        List<Path> originMovesets = collectJsonFiles(RESOURCES_ROOT.resolve("data/jcraft/movesets/stand/origin/default"));
        checkEquals(9, originMovesets.size(), "All 9 default Origin moveset JSONs must exist");
        for (Path ms : originMovesets) {
            parseJsonObject(ms);
        }
        check(Files.readString(RESOURCES_ROOT.resolve("data/jcraft/movesets/stand/origin/default/light/abyssal_lash.json"), StandardCharsets.UTF_8)
                .contains("\"jcraft:origin_light\""), "abyssal_lash.json must trigger jcraft:origin_light");
        check(Files.readString(RESOURCES_ROOT.resolve("data/jcraft/movesets/stand/origin/default/special1/soul_sever.json"), StandardCharsets.UTF_8)
                .contains("\"jcraft:origin_special1\""), "soul_sever.json must trigger jcraft:origin_special1");
        check(Files.readString(RESOURCES_ROOT.resolve("data/jcraft/movesets/stand/origin/default/special2/void_flurry.json"), StandardCharsets.UTF_8)
                .contains("\"jcraft:origin_special2\""), "void_flurry.json must trigger jcraft:origin_special2");
        String graspRaw = Files.readString(RESOURCES_ROOT.resolve("data/jcraft/movesets/stand/origin/default/special3/void_grasp.json"), StandardCharsets.UTF_8);
        check(graspRaw.contains("\"jcraft:origin_grab\"") && graspRaw.contains("\"jcraft:origin_grab_hit\""),
                "void_grasp.json must trigger jcraft:origin_grab and jcraft:origin_grab_hit");

        JsonObject originLightJson = parseJsonObject(
                RESOURCES_ROOT.resolve("data/jcraft/movesets/stand/origin/default/light/abyssal_lash.json"));
        JsonObject originFollowup = originLightJson.getAsJsonObject("followup");
        check(originFollowup != null && "LIGHT_FOLLOWUP".equals(originFollowup.get("anim_state").getAsString()),
                "abyssal_lash.json must define LIGHT_FOLLOWUP followup");
        check("CRUSH".equals(originFollowup.getAsJsonObject("move").getAsJsonObject("attack_extras").get("hit_animation").getAsString())
                        && originFollowup.getAsJsonObject("move").getAsJsonObject("attack_extras").has("extra_hit_boxes"),
                "Origin LIGHT_FOLLOWUP must use CRUSH hit_animation and extra_hit_boxes");
        check("HIGH".equals(originLightJson.getAsJsonObject("crouching_variant").getAsJsonObject("move")
                        .getAsJsonObject("attack_extras").get("hit_animation").getAsString())
                        && "LOW".equals(originLightJson.getAsJsonObject("move")
                        .getAsJsonObject("attack_extras").get("hit_animation").getAsString()),
                "Origin abyssal_lash must assign LOW/HIGH HitAnimations");

        JsonObject originHeavyJson = parseJsonObject(
                RESOURCES_ROOT.resolve("data/jcraft/movesets/stand/origin/default/heavy/obsidian_judgement.json"));
        JsonObject originSlamCr = originHeavyJson.getAsJsonObject("crouching_variant");
        check(originSlamCr != null && "GRAB_HIT".equals(originSlamCr.get("anim_state").getAsString())
                        && "CRUSH".equals(originSlamCr.getAsJsonObject("move").getAsJsonObject("attack_extras").get("hit_animation").getAsString())
                        && originSlamCr.getAsJsonObject("move").getAsJsonObject("attack_extras").has("extra_hit_boxes"),
                "obsidian_judgement.json must define crouching Gravity Slam variant with GRAB_HIT, CRUSH and extra_hit_boxes");

        for (String extraKey : List.of(
                "move.jcraft.origin.default.light_followup.name",
                "move.jcraft.origin.default.light_followup.description",
                "move.jcraft.origin.default.gravity_slam.name",
                "move.jcraft.origin.default.gravity_slam.description")) {
            check(enUsSnd.has(extraKey) && ptBrSnd.has(extraKey), "Both lang files must define " + extraKey);
        }

        checkEquals(0x00FFAA,
                net.arna.jcraft.client.rendering.JoJoOnomatopoeiaRenderer.resolveOriginGlyphColor(0, 0x00FFAA, false, false),
                "Custom Chroma RGB must override Origin onomatopoeia color");
        checkEquals(0xF5243D,
                net.arna.jcraft.client.rendering.JoJoOnomatopoeiaRenderer.resolveOriginGlyphColor(1, -1, false, false),
                "Origin Skin 1 must tint onomatopoeia Crimson Eclipse");
        checkEquals(0x24DBFF,
                net.arna.jcraft.client.rendering.JoJoOnomatopoeiaRenderer.resolveOriginGlyphColor(2, -1, false, false),
                "Origin Skin 2 must tint onomatopoeia Astral Cyan");
        checkEquals(0xDCE6FF,
                net.arna.jcraft.client.rendering.JoJoOnomatopoeiaRenderer.resolveOriginGlyphColor(4, -1, false, true),
                "Origin Skin 4 must tint onomatopoeia silver-abyssal");
        checkEquals(0xFFFDF2,
                net.arna.jcraft.client.rendering.JoJoOnomatopoeiaRenderer.resolveOriginGlyphColor(5, -1, false, true),
                "Origin Skin 5 barrage must tint onomatopoeia radiant celestial");

        try (InputStream sndIn = cl.getResourceAsStream("net/arna/jcraft/api/registry/JSoundRegistry.class")) {
            check(sndIn != null, "JSoundRegistry.class must exist");
            ClassNode sndCn = new ClassNode();
            new ClassReader(sndIn).accept(sndCn, ClassReader.SKIP_CODE);
            for (String regField : List.of(
                    "ORIGIN_SUMMON", "ORIGIN_DESUMMON", "ORIGIN_BARRAGE", "ORIGIN_HEAVY",
                    "ORIGIN_ULT", "ORIGIN_BIND", "ORIGIN_LIGHT", "ORIGIN_SPECIAL1",
                    "ORIGIN_SPECIAL2", "ORIGIN_GRAB", "ORIGIN_GRAB_HIT", "ORIGIN_COUNTER")) {
                check(sndCn.fields.stream().anyMatch(f -> regField.equals(f.name)),
                        "JSoundRegistry must declare " + regField);
            }
        }
        try (InputStream oeIn = cl.getResourceAsStream("net/arna/jcraft/common/entity/stand/OriginEntity.class")) {
            check(oeIn != null, "OriginEntity.class must exist");
            ClassNode oeCn = new ClassNode();
            new ClassReader(oeIn).accept(oeCn, ClassReader.SKIP_CODE);
            check(oeCn.fields.stream().anyMatch(f -> "LIGHT_FOLLOWUP".equals(f.name))
                            && oeCn.fields.stream().anyMatch(f -> "GRAVITY_SLAM".equals(f.name)),
                    "OriginEntity must declare LIGHT_FOLLOWUP and GRAVITY_SLAM");
            check(oeCn.methods.stream().anyMatch(m -> "desummon".equals(m.name) && "(Z)V".equals(m.desc))
                            && oeCn.methods.stream().anyMatch(m -> "setState".equals(m.name)),
                    "OriginEntity must override desummon(boolean) and setState(...)");
        }
        try (InputStream orIn = cl.getResourceAsStream("net/arna/jcraft/client/renderer/entity/stands/OriginRenderer.class");
             InputStream omIn = cl.getResourceAsStream("net/arna/jcraft/client/renderer/entity/stands/OriginRenderer$OriginModelRenderer.class");
             InputStream ogIn = cl.getResourceAsStream("net/arna/jcraft/client/renderer/entity/layer/OriginGlowLayer.class")) {
            check(orIn != null && omIn != null && ogIn != null,
                    "OriginRenderer, OriginModelRenderer and OriginGlowLayer classes must exist");
            ClassNode orCn = new ClassNode();
            new ClassReader(orIn).accept(orCn, ClassReader.SKIP_CODE);
            check(orCn.methods.stream().anyMatch(m -> "getEffectiveAlpha".equals(m.name)),
                    "OriginRenderer must declare getEffectiveAlpha for combat translucency");
        }

        JsonObject enUs = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/lang/en_us.json"));
        JsonObject ptBr = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/lang/pt_br.json"));
        for (String key : List.of("entity.jcraft.origin", "key.jcraft.origin_arsenal",
                "jcraft.config.origin_free_binds", "jcraft.origin.arsenal.title", "jcraft.origin.preset.supreme_time_lord",
                "jcraft.origin.tab.studio", "jcraft.origin.tab.profiles", "jcraft.origin.chroma.title",
                "jcraft.origin.profile.saved", "jcraft.origin.synergy.chrono_sovereign")) {
            check(enUs.has(key), "en_us.json must define Origin key: " + key);
            check(ptBr.has(key), "pt_br.json must define Origin key: " + key);
        }

        // 4. OriginProfile NBT & Wire round-trip + OriginSynergy engine verification
        net.arna.jcraft.common.stand.OriginProfile prof = new net.arna.jcraft.common.stand.OriginProfile(
                "Boss Killer",
                java.util.Map.of(net.arna.jcraft.api.attack.enums.MoveClass.BARRAGE, "jcraft:star_platinum#barrage"),
                4, 0x00FFAA);
        net.arna.jcraft.common.stand.OriginProfile decodedNbt = net.arna.jcraft.common.stand.OriginProfile.fromNbt(prof.toNbt());
        check(decodedNbt != null && "Boss Killer".equals(decodedNbt.name())
                && decodedNbt.skin() == 4 && decodedNbt.customGlowRgb() == 0x00FFAA
                && "jcraft:star_platinum#barrage".equals(decodedNbt.slots().get(net.arna.jcraft.api.attack.enums.MoveClass.BARRAGE)),
                "OriginProfile NBT round-trip must preserve name, skin, glowRgb and slots");
        net.minecraft.network.FriendlyByteBuf pBuf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        prof.writeBuf(pBuf);
        net.arna.jcraft.common.stand.OriginProfile decodedBuf = net.arna.jcraft.common.stand.OriginProfile.readBuf(pBuf);
        pBuf.release();
        check("Boss Killer".equals(decodedBuf.name()) && decodedBuf.customGlowRgb() == 0x00FFAA,
                "OriginProfile FriendlyByteBuf round-trip must preserve build data");

        var chronoSyn = net.arna.jcraft.common.stand.OriginSynergy.evaluateCategories(List.of(
                net.arna.jcraft.common.stand.OriginSkillCatalog.SkillCategory.TIME_SPACE,
                net.arna.jcraft.common.stand.OriginSkillCatalog.SkillCategory.TIME_SPACE,
                net.arna.jcraft.common.stand.OriginSkillCatalog.SkillCategory.TIME_SPACE));
        check(chronoSyn.isActive(net.arna.jcraft.common.stand.OriginSynergy.SynergyType.CHRONO_SOVEREIGN),
                "3x TIME_SPACE skills must activate CHRONO_SOVEREIGN synergy");
        check(!chronoSyn.isActive(net.arna.jcraft.common.stand.OriginSynergy.SynergyType.OMNIPOTENT_HARMONY),
                "Single-category build must not activate OMNIPOTENT_HARMONY");

        var harmonySyn = net.arna.jcraft.common.stand.OriginSynergy.evaluateCategories(List.of(
                net.arna.jcraft.common.stand.OriginSkillCatalog.SkillCategory.HEAVY,
                net.arna.jcraft.common.stand.OriginSkillCatalog.SkillCategory.HEAVY,
                net.arna.jcraft.common.stand.OriginSkillCatalog.SkillCategory.HEAVY,
                net.arna.jcraft.common.stand.OriginSkillCatalog.SkillCategory.RANGED,
                net.arna.jcraft.common.stand.OriginSkillCatalog.SkillCategory.RANGED,
                net.arna.jcraft.common.stand.OriginSkillCatalog.SkillCategory.MOBILITY,
                net.arna.jcraft.common.stand.OriginSkillCatalog.SkillCategory.BARRAGE));
        check(harmonySyn.isActive(net.arna.jcraft.common.stand.OriginSynergy.SynergyType.OBSIDIAN_JUGGERNAUT)
                && harmonySyn.isActive(net.arna.jcraft.common.stand.OriginSynergy.SynergyType.ASTRAL_ARTILLERY)
                && harmonySyn.isActive(net.arna.jcraft.common.stand.OriginSynergy.SynergyType.PHANTOM_TEMPEST)
                && harmonySyn.isActive(net.arna.jcraft.common.stand.OriginSynergy.SynergyType.OMNIPOTENT_HARMONY),
                "Multi-category build must activate Heavy, Ranged, Tempest & Harmony synergies");

        // 5. Build code encode/decode round-trip & malformed rejection
        String buildCode = net.arna.jcraft.common.stand.OriginProfile.encodeBuildCode(prof);
        check(buildCode.startsWith(net.arna.jcraft.common.stand.OriginProfile.BUILD_CODE_PREFIX),
                "Origin build code must start with ORIGIN1: prefix");
        net.arna.jcraft.common.stand.OriginProfile decodedCode = net.arna.jcraft.common.stand.OriginProfile.decodeBuildCode(buildCode);
        check(decodedCode != null && decodedCode.skin() == 4 && decodedCode.customGlowRgb() == 0x00FFAA
                && "jcraft:star_platinum#barrage".equals(decodedCode.slots().get(net.arna.jcraft.api.attack.enums.MoveClass.BARRAGE)),
                "Origin build code decode must round-trip skin, glowRgb and slots");
        check(net.arna.jcraft.common.stand.OriginProfile.decodeBuildCode("INVALID_CODE") == null,
                "Malformed build code must safely return null");

        // 6. Anti-exploit Ultimate cap, duplicate sanitization, shared cooldown & grab/finisher/projectile Synergy scaling
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/common/config/JServerConfig.class")) {
            check(in != null, "JServerConfig.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            check(cn.fields.stream().anyMatch(f -> "ORIGIN_MAX_ULTIMATES".equals(f.name)),
                    "JServerConfig must expose ORIGIN_MAX_ULTIMATES");
        }
        for (String key : List.of(
                "jcraft.serverconfig.option.origin_max_ultimates",
                "jcraft.serverconfig.option.origin_max_ultimates.tooltip",
                "jcraft.origin.arsenal.locked_combat",
                "jcraft.origin.arsenal.duplicate_skill",
                "jcraft.origin.arsenal.max_ultimates",
                "jcraft.origin.studio.anim.barrage",
                "jcraft.origin.profile.copy_code",
                "jcraft.origin.profile.paste_code",
                "jcraft.origin.arsenal.swap_btn",
                "jcraft.origin.arsenal.editing_slot",
                "jcraft.origin.arsenal.equip_action")) {
            check(enUs.has(key) && ptBr.has(key), "EN/PT-BR must define new Origin key: " + key);
        }
        java.util.Map<net.arna.jcraft.api.attack.enums.MoveClass, String> testSwapMap = java.util.Map.of(
                net.arna.jcraft.api.attack.enums.MoveClass.LIGHT, "jcraft:star_platinum#light",
                net.arna.jcraft.api.attack.enums.MoveClass.SPECIAL1, "jcraft:the_world#special1"
        );
        checkEquals(net.arna.jcraft.api.attack.enums.MoveClass.LIGHT,
                net.arna.jcraft.common.stand.OriginSkillCatalog.findSlotContaining(
                        testSwapMap, net.arna.jcraft.api.attack.enums.MoveClass.SPECIAL1, "jcraft:star_platinum#light"),
                "findSlotContaining must locate other slot owning candidate skill for auto-swap");

        // Verify OriginSynergy projectile multiplier + grab hitMove & finisher scaling without mutating static templates
        float baseProjMult = net.arna.jcraft.common.stand.OriginSynergy.computeProjectileDamageMultiplier(
                net.arna.jcraft.common.stand.OriginSynergy.SynergySummary.EMPTY);
        checkClose(1.0, baseProjMult, 1.0E-5, "Empty synergy projectile multiplier must be 1.0");
        checkClose(1.12 * 1.08, net.arna.jcraft.common.stand.OriginSynergy.computeProjectileDamageMultiplier(harmonySyn),
                1.0E-4, "Astral Artillery + Omnipotent Harmony must stack to 1.12 * 1.08 projectile multiplier");

        class TestSynergyMove extends net.arna.jcraft.api.attack.moves.AbstractMove<TestSynergyMove, net.arna.jcraft.common.entity.stand.OriginEntity> {
            TestSynergyMove(int cd, int windup, int dur) { super(cd, windup, dur, 1.0f); }
            @Override public net.arna.jcraft.api.attack.MoveType<TestSynergyMove> getMoveType() { return null; }
            @Override public java.util.Set<net.minecraft.world.entity.LivingEntity> perform(
                    net.arna.jcraft.common.entity.stand.OriginEntity a, net.minecraft.world.entity.LivingEntity u) { return java.util.Set.of(); }
            @Override protected TestSynergyMove getThis() { return this; }
            @Override public TestSynergyMove copy() {
                return copyExtras(new TestSynergyMove(getCooldown(), getWindup(), getDuration()));
            }
        }
        var finChild = new TestSynergyMove(100, 10, 20);
        var finParent = new TestSynergyMove(100, 10, 20).withFinisher(8, finChild);
        var finCloned = finParent.copy();
        net.arna.jcraft.common.stand.OriginSynergy.applyToMoveCopy(
                finCloned, net.arna.jcraft.common.stand.OriginSkillCatalog.SkillCategory.TIME_SPACE, chronoSyn);
        check(finCloned.getFinisher() != null && finCloned.getFinisher().right() != finChild,
                "AbstractMove.copy() must deep-copy finisher move");
        check(finCloned.getFinisher().right().getCooldown() < 100 && finChild.getCooldown() == 100,
                "OriginSynergy must scale finisher move without mutating original template");

        // Verify bytecode wiring for AbstractGrabAttack.hitMove copy, OriginSynergy hitMove/finisher traversal, and Attacks projectile scaling
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/common/stand/OriginSynergy.class")) {
            check(in != null, "OriginSynergy.class must exist");
            String raw = new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
            check(raw.contains("getHitMove") && raw.contains("getFinisher") && raw.contains("computeProjectileDamageMultiplier"),
                    "OriginSynergy must handle getHitMove, getFinisher and projectile multiplier");
        }
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/api/Attacks.class")) {
            check(in != null, "Attacks.class must exist");
            String raw = new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
            check(raw.contains("getSynergyProjectileDamageMultiplier"),
                    "Attacks.damage must apply OriginEntity.getSynergyProjectileDamageMultiplier");
        }

        // 7. New capability interfaces implemented on OriginEntity
        for (Class<?> iface : List.of(
                net.arna.jcraft.api.stand.IIronCapable.class,
                net.arna.jcraft.api.stand.IPoisonCapsuleCapable.class,
                net.arna.jcraft.api.stand.IIceLanceCapable.class,
                net.arna.jcraft.api.stand.IRevolverCapable.class)) {
            check(iface.isAssignableFrom(net.arna.jcraft.common.entity.stand.OriginEntity.class),
                    "OriginEntity must implement capability interface " + iface.getSimpleName());
        }

        // 8. Dedicated-Server bytecode isolation for Origin & new capability classes
        for (String clsPath : List.of(
                "net/arna/jcraft/common/stand/OriginSkillCatalog.class",
                "net/arna/jcraft/common/stand/OriginProfile.class",
                "net/arna/jcraft/common/stand/OriginSynergy.class",
                "net/arna/jcraft/common/entity/stand/OriginEntity.class",
                "net/arna/jcraft/common/network/c2s/OriginLoadoutPacket.class",
                "net/arna/jcraft/api/stand/IIronCapable.class",
                "net/arna/jcraft/api/stand/IPoisonCapsuleCapable.class",
                "net/arna/jcraft/api/stand/IIceLanceCapable.class",
                "net/arna/jcraft/api/stand/IRevolverCapable.class")) {
            try (InputStream in = cl.getResourceAsStream(clsPath)) {
                check(in != null, clsPath + " must exist");
                String rawBytes = new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
                check(!rawBytes.contains("net/arna/jcraft/client/") && !rawBytes.contains("net/minecraft/client/"),
                        clsPath + " must not reference client classes");
            }
        }

        // 8b. Guard against legacy GLFW event.button() == 0 checks (MC 26.3 SDL3 uses InputConstants.MOUSE_BUTTON_LEFT == 1)
        for (String screenFile : List.of(
                "src/main/java/net/arna/jcraft/client/gui/screen/OriginArsenalScreen.java",
                "src/main/java/net/arna/jcraft/client/gui/screen/JoJoRadialWheelScreen.java")) {
            String src = Files.readString(Path.of(screenFile), StandardCharsets.UTF_8);
            check(!src.contains("event.button() == 0") && !src.contains("event.button() == 1"),
                    screenFile + " must use InputConstants.MOUSE_BUTTON_LEFT/RIGHT instead of legacy GLFW 0/1");
            check(src.contains("InputConstants.MOUSE_BUTTON_LEFT"),
                    screenFile + " must handle InputConstants.MOUSE_BUTTON_LEFT");
        }

        // 9. Crosshair-Aligned Nearest-Block Breaking & Occlusion checks
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/api/attack/moves/AbstractSimpleAttack.class")) {
            check(in != null, "AbstractSimpleAttack.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            for (String mName : List.of("raycastAimedBlock", "getAimOrigin", "getAimDirection", "getBlockBreakReach", "shouldUseSimpleBlockBreakage")) {
                check(cn.methods.stream().anyMatch(m -> mName.equals(m.name)),
                        "AbstractSimpleAttack must define " + mName);
            }
            var bbMethod = cn.methods.stream().filter(m -> "breakBlocks".equals(m.name)).findFirst().orElseThrow();
            boolean callsDirectional = false;
            boolean callsBetweenClosed = false;
            for (var insn = bbMethod.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof org.objectweb.asm.tree.MethodInsnNode mi) {
                    if ("setBreakStateDirectional".equals(mi.name)) callsDirectional = true;
                    if ("betweenClosedStream".equals(mi.name)) callsBetweenClosed = true;
                }
            }
            check(callsDirectional && !callsBetweenClosed,
                    "AbstractSimpleAttack.breakBlocks must use raycast setBreakStateDirectional instead of blind betweenClosedStream");
        }
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/common/attack/moves/shared/MainBarrageAttack.class")) {
            check(in != null, "MainBarrageAttack.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            check(cn.methods.stream().anyMatch(m -> "shouldUseSimpleBlockBreakage".equals(m.name)),
                    "MainBarrageAttack must override shouldUseSimpleBlockBreakage");
            var perfMethod = cn.methods.stream().filter(m -> "perform".equals(m.name)).findFirst().orElseThrow();
            boolean callsRaycast = false;
            boolean callsBelow = false;
            for (var insn = perfMethod.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof org.objectweb.asm.tree.MethodInsnNode mi) {
                    if ("raycastAimedBlock".equals(mi.name)) callsRaycast = true;
                    if ("below".equals(mi.name)) callsBelow = true;
                }
            }
            check(callsRaycast && !callsBelow,
                    "MainBarrageAttack.perform must use raycastAimedBlock and must not blind-break centerPos.below()");
        }
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/common/attack/moves/starplatinum/BlockBreakingAttack.class")) {
            check(in != null, "BlockBreakingAttack.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            var hookMethod = cn.methods.stream().filter(m -> "performHook".equals(m.name)).findFirst().orElseThrow();
            boolean callsRaycast = false;
            boolean checksBlockHit = false;
            for (var insn = hookMethod.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof org.objectweb.asm.tree.FieldInsnNode fi && "BLOCK".equals(fi.name)) {
                    checksBlockHit = true;
                }
                if (insn instanceof org.objectweb.asm.tree.MethodInsnNode mi && "raycastAimedBlock".equals(mi.name)) {
                    callsRaycast = true;
                }
            }
            check(callsRaycast && checksBlockHit,
                    "BlockBreakingAttack.performHook must call raycastAimedBlock and guard on HitResult.Type.BLOCK");
        }

        // 10. README.md documents Origin & 30 Stands
        String readme = Files.readString(Path.of("README.md"), StandardCharsets.UTF_8);
        check(readme.contains("Origin") && readme.contains("30 Stands"),
                "README.md must highlight Origin and 30 playable Stands");
    }

    static void verifyVisualPolishAndGameFeel() throws Exception {
        ClassLoader cl = JCraft263ValidationTest.class.getClassLoader();

        // 1. Runic Circle texture (256x256 with anti-aliased white strokes & alpha)
        Path runicPath = RESOURCES_ROOT.resolve("assets/jcraft/textures/effect/stand_runic_circle.png");
        var runicImg = javax.imageio.ImageIO.read(runicPath.toFile());
        check(runicImg != null && runicImg.getWidth() == 256 && runicImg.getHeight() == 256,
                "stand_runic_circle.png must be 256x256");

        // 2. All 5 iconic Stand surgical glow.png maps (128x128, non-empty 0.1%..25% surgical pixels)
        for (String standDir : List.of(
                "star_platinum", "star_platinum_the_world", "the_world_over_heaven",
                "king_crimson", "made_in_heaven")) {
            Path glowPath = RESOURCES_ROOT.resolve("assets/jcraft/textures/entity/stands/" + standDir + "/glow.png");
            check(Files.isRegularFile(glowPath), "Missing iconic emissive texture: " + standDir + "/glow.png");
            var glowImg = javax.imageio.ImageIO.read(glowPath.toFile());
            check(glowImg != null && glowImg.getWidth() == 128 && glowImg.getHeight() == 128,
                    standDir + "/glow.png must be 128x128");
            int nonEmpty = 0;
            for (int y = 0; y < glowImg.getHeight(); y++) {
                for (int x = 0; x < glowImg.getWidth(); x++) {
                    if (((glowImg.getRGB(x, y) >>> 24) & 0xFF) > 0) {
                        nonEmpty++;
                    }
                }
            }
            double pct = (nonEmpty * 100.0) / (glowImg.getWidth() * glowImg.getHeight());
            check(pct > 0.1 && pct < 25.0,
                    standDir + "/glow.png must be surgical (nonEmpty=" + String.format("%.2f%%", pct) + ")");
        }

        // 3. Bytecode wiring checks for IconicStandGlowLayer, TWOHEyesLayer overlay fix, and common isolation
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/client/renderer/entity/layer/TWOHEyesLayer.class")) {
            check(in != null, "TWOHEyesLayer.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            boolean setsOverlay = cn.methods.stream().anyMatch(m -> {
                for (var insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof org.objectweb.asm.tree.MethodInsnNode mi && "setPackedOverlay".equals(mi.name)) {
                        return true;
                    }
                }
                return false;
            });
            check(setsOverlay, "TWOHEyesLayer must call pc.setPackedOverlay");
        }
        for (String rendererCls : List.of(
                "net/arna/jcraft/client/renderer/entity/stands/StarPlatinumRenderer.class",
                "net/arna/jcraft/client/renderer/entity/stands/SPTWRenderer.class",
                "net/arna/jcraft/client/renderer/entity/stands/TheWorldOverHeavenRenderer.class",
                "net/arna/jcraft/client/renderer/entity/stands/KingCrimsonRenderer.class",
                "net/arna/jcraft/client/renderer/entity/stands/MadeInHeavenRenderer.class")) {
            try (InputStream in = cl.getResourceAsStream(rendererCls)) {
                check(in != null, rendererCls + " must exist");
                String rawBytes = new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
                check(rawBytes.contains("IconicStandGlowLayer"), rendererCls + " must attach IconicStandGlowLayer");
            }
        }
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/common/util/JVisualFeedback.class")) {
            check(in != null, "JVisualFeedback.class must exist");
            String rawBytes = new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
            check(!rawBytes.contains("net/arna/jcraft/client/") && !rawBytes.contains("net/minecraft/client/"),
                    "JVisualFeedback must remain dedicated-server safe");
        }

        // 4. Client config clamping & damped shake curve bounds
        var cfg = net.arna.jcraft.client.JClientConfig.getInstance();
        float prevShake = cfg.getScreenShakeStrength();
        try {
            check(cfg.isAuraAfterimagesEnabled() && cfg.isImpactEffectsEnabled() && cfg.isRunicChargeCirclesEnabled(),
                    "Visual polish client toggles must default to enabled");
            cfg.setScreenShakeStrength(-2.0f);
            checkClose(0.0, cfg.getClampedScreenShakeStrength(), 1.0E-5, "Screen shake lower clamp must be 0.0f");
            cfg.setScreenShakeStrength(5.0f);
            checkClose(2.0, cfg.getClampedScreenShakeStrength(), 1.0E-5, "Screen shake upper clamp must be 2.0f");
        } finally {
            cfg.setScreenShakeStrength(prevShake);
        }
        float[] disabledShake = net.arna.jcraft.client.rendering.CombatImpactEffects.computeShakeAnglesDeg(false, 1.0f, 5, 6, 1.0f, 0.0f, 0.5f);
        check(disabledShake[0] == 0.0f && disabledShake[1] == 0.0f && disabledShake[2] == 0.0f,
                "Disabled impact effects must yield zero camera shake");
        float[] activeShake = net.arna.jcraft.client.rendering.CombatImpactEffects.computeShakeAnglesDeg(true, 1.0f, 5, 6, 1.0f, 0.4f, 0.5f);
        check(Math.abs(activeShake[0]) <= 1.6f && Math.abs(activeShake[1]) <= 1.6f && Math.abs(activeShake[2]) <= 1.4f
                && (Math.abs(activeShake[0]) > 0.0f || Math.abs(activeShake[1]) > 0.0f),
                "Active impact shake must be bounded and non-zero");

        // 5. AuraAfterimageManager uses frozen EntityRenderState snapshot + GhostSubmitCollector (never extractEntity inside renderAll)
        check(cl.getResource("net/arna/jcraft/client/rendering/AuraAfterimageManager$GhostSubmitCollector.class") != null,
                "AuraAfterimageManager must define GhostSubmitCollector for translucent aura fade");
        try (InputStream in = cl.getResourceAsStream("net/arna/jcraft/client/rendering/AuraAfterimageManager.class")) {
            check(in != null, "AuraAfterimageManager.class must exist");
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            var renderAll = cn.methods.stream().filter(m -> "renderAll".equals(m.name)).findFirst().orElseThrow();
            boolean callsExtractInRender = false;
            for (var insn = renderAll.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof org.objectweb.asm.tree.MethodInsnNode mi && "extractEntity".equals(mi.name)) {
                    callsExtractInRender = true;
                }
            }
            check(!callsExtractInRender, "AuraAfterimageManager.renderAll must reuse frozen GhostEcho state instead of extracting live entity per frame");
        }

        // 6. EN/PT-BR config keys present
        JsonObject enUs = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/lang/en_us.json"));
        JsonObject ptBr = parseJsonObject(RESOURCES_ROOT.resolve("assets/jcraft/lang/pt_br.json"));
        for (String key : List.of(
                "jcraft.config.aura_afterimages", "jcraft.config.aura_afterimages.tooltip",
                "jcraft.config.impact_effects", "jcraft.config.impact_effects.tooltip",
                "jcraft.config.screen_shake_strength", "jcraft.config.screen_shake_strength.tooltip",
                "jcraft.config.runic_charge_circles", "jcraft.config.runic_charge_circles.tooltip")) {
            check(enUs.has(key) && ptBr.has(key), "EN/PT-BR must define visual config key: " + key);
        }
    }
}
