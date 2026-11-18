package net.arna.jcraft.fabric.common.component.impl.world;

import net.arna.jcraft.common.util.JValueIO;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.arna.jcraft.common.component.impl.world.CommonTexasHoldEmComponentImpl;
import net.arna.jcraft.fabric.common.component.world.TexasHoldEmComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public class TexasHoldEmComponentImpl extends CommonTexasHoldEmComponentImpl implements TexasHoldEmComponent {

    public TexasHoldEmComponentImpl(Level world) {
        super(world);
    }

    @Override
    public void readData(ValueInput in) {
        super.readFromNbt(JValueIO.read(in, "JCraftData"));
    }

    @Override
    public void writeData(ValueOutput out) {
        CompoundTag tag = new CompoundTag();
        super.writeToNbt(tag);
        JValueIO.write(out, "JCraftData", tag);
    }
}
