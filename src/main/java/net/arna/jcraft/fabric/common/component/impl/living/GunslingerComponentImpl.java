package net.arna.jcraft.fabric.common.component.impl.living;

import net.arna.jcraft.common.util.JValueIO;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.nbt.CompoundTag;

import net.arna.jcraft.common.component.impl.living.CommonGunslingerComponentImpl;
import net.arna.jcraft.fabric.common.component.JComponents;
import net.arna.jcraft.fabric.common.component.living.GunslingerComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class GunslingerComponentImpl extends CommonGunslingerComponentImpl implements GunslingerComponent {

    public GunslingerComponentImpl(LivingEntity entity) {
        super(entity);
    }

    @Override
    public void sync(Entity entity) {
        JComponents.GUNSLINGER.sync(entity);
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
