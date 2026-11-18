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
        checkEquals(23, totalMixins, "All 23 common + client Mixins must be registered and bytecode-verified");
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

    public static void main(String[] args) throws Exception {
        System.out.println("=== Running JCraft 26.3 Superior Edition Verification Suite ===");
        verifyAllStandsSpecsEvolutionsAndMovesetsPresentAndValid();
        System.out.println("[PASS] 37 Stands, 5 Specs, 7 Evolutions & 281 Movesets verified.");
        verifyMc263AssetsWorldgenTradesAndTranslations();
        System.out.println("[PASS] 223 Items, 4 RenderPearl 26.3 Post-Effects + GLSL 330 std140 Shaders, Trades & Translations verified.");
        verifyFabricModJsonEntrypointsAndMixins();
        System.out.println("[PASS] fabric.mod.json, 16 CCA Components, 5 Entrypoints & all 23 Java 25 Mixins verified.");
        verifyKineticTimeStopPhysicsMath();
        System.out.println("[PASS] Kinetic Time Stop Deceleration & +25% Momentum Release Physics verified.");
        verifySmartCombatInputBufferWindowAndExpiry();
        System.out.println("[PASS] 6-Tick Smart Combat Input Buffer verified.");
        verifyStarPlatinumPrecisionZoomCurve();
        System.out.println("[PASS] Star Platinum 5x Precision Zoom Curve verified.");
        System.out.println("=== ALL " + passedAssertions + " ASSERTIONS PASSED SUCCESSFULLY ===");
    }
}
