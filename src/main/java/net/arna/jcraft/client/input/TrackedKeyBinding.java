package net.arna.jcraft.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import net.arna.jcraft.JCraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class TrackedKeyBinding implements IJKeyBinding {
    private static final Map<KeyMapping, TrackedKeyBinding> bindings = new HashMap<>();
    private static final Map<String, KeyMapping.Category> CATEGORIES = new ConcurrentHashMap<>();
    public static final KeyMapping.Category DEFAULT_CATEGORY = KeyMapping.Category.register(JCraft.id("general"));

    private static boolean resetForScreen = false;
    private final KeyMapping parent;
    private boolean changedThisTick, pressedThisTick, releasedThisTick;

    static {
        CATEGORIES.put("key.category.jcraft", DEFAULT_CATEGORY);
    }

    public static KeyMapping.Category resolveCategory(final String category) {
        if (category == null || category.isEmpty() || "key.category.jcraft".equals(category)) {
            return DEFAULT_CATEGORY;
        }
        return CATEGORIES.computeIfAbsent(category, key -> {
            String clean = key.replace("key.category.", "").replace('.', '_');
            return KeyMapping.Category.register(Identifier.fromNamespaceAndPath(JCraft.MOD_ID, clean));
        });
    }

    public static void resetValues(final boolean clientScreenAssigned) {
        bindings.values().forEach(TrackedKeyBinding::reset);
        if (clientScreenAssigned) {
            bindings.values().stream()
                    .filter(TrackedKeyBinding::isDown)
                    .forEach(TrackedKeyBinding::markReleased);
            resetForScreen = true;
        } else {
            resetForScreen = false;
        }
    }

    private TrackedKeyBinding(final KeyMapping parent) {
        this.parent = parent;
    }

    public static TrackedKeyBinding create(final String translationKey, final InputConstants.Type type, final int code,
                                           final String category) {
        return wrap(new KeyMapping(translationKey, type, code, resolveCategory(category)));
    }

    public static TrackedKeyBinding create(final String translationKey, final InputConstants.Type type, final int code,
                                           final KeyMapping.Category category) {
        return wrap(new KeyMapping(translationKey, type, code, category));
    }

    public static TrackedKeyBinding wrap(final KeyMapping binding) {
        return bindings.computeIfAbsent(binding, TrackedKeyBinding::new);
    }

    public static void onKeyPressSet(final KeyMapping binding, final boolean pressed) {
        final TrackedKeyBinding trackedBinding = bindings.get(binding);
        if (trackedBinding == null) {
            return;
        }

        if (pressed) {
            trackedBinding.markPressed();
        } else {
            trackedBinding.markReleased();
        }
    }

    public boolean isDown() {
        return parent.isDown();
    }

    private void markPressed() {
        changedThisTick = true;
        if (!releasedThisTick) {
            pressedThisTick = true;
        }
    }

    private void markReleased() {
        changedThisTick = true;
        pressedThisTick = false;
        releasedThisTick = true;
    }

    private void reset() {
        changedThisTick = pressedThisTick = releasedThisTick = false;
    }
}
