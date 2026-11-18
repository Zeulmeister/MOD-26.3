package net.arna.jcraft.api;

import net.minecraft.resources.Identifier;

public interface IAttackerType {
    /**
     * Gets the unique identifier of this attacker type.
     * @return the unique identifier of this attacker type
     */
    Identifier getId();

    /**
     * The kind of this attacker type.
     * Usually either 'stand' or 'spec'.
     * Used to organize movesets.
     * @return the name of this attacker type
     */
    String kind();
}
