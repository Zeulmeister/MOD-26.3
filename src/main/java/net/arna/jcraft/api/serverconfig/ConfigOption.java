package net.arna.jcraft.api.serverconfig;

import com.google.gson.JsonElement;
import lombok.Getter;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ConfigOption {
    private static final Map<Identifier, ConfigOption> options = new LinkedHashMap<>();

    @Getter
    private final Identifier key;
    @Getter
    private final Identifier category, subcategory;

    protected ConfigOption(final Identifier key, final Identifier category, final Identifier subcategory) {
        this.key = key;
        this.category = category;
        this.subcategory = subcategory;

        if (options.containsKey(key)) {
            throw new IllegalArgumentException("Option with the given key already exists: " + key);
        }

        options.put(key, this);
    }

    public static @Nullable ConfigOption getOption(final Identifier key) {
        return options.get(key);
    }

    public static Map<Identifier, ConfigOption> getOptions() {
        return Collections.unmodifiableMap(options);
    }

    /**
     * Writes the value of this option to the given bytebuffer.
     * @param buf The buffer to write to
     */
    public abstract void write(final FriendlyByteBuf buf);

    /**
     * Reads from the given bytebuffer and updates this option's value.
     * @param buf The buffer to read from
     */
    public abstract void read(final FriendlyByteBuf buf);

    /**
     * Writes the value of this option to a JSON element for serialization.
     * @return The resulting JSON element
     */
    public abstract JsonElement write();

    /**
     * Reads the given JSON element and updates this option's value.
     * @param element The JSON element to read
     */
    public abstract void read(final JsonElement element);

    @Environment(EnvType.CLIENT)
    public abstract AbstractFieldBuilder<?, ?, ?> createField(ConfigBuilder builder, Component name, Runnable markDirty);
}
