package net.arna.jcraft.client.model.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class StandMeteorModel extends EntityModel<LivingEntityRenderState> {
    public StandMeteorModel(final ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();
        partDefinition.addOrReplaceChild("body", CubeListBuilder
                .create()
                .texOffs(0, 0)
                .addBox(0, 0, 0, 128.0F, 128.0F, 128.0F),
                PartPose.offset(-64.0F, -64.0F, -64.0F)
        );
        return LayerDefinition.create(meshDefinition, 32, 32);
    }
}
