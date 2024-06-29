package de.ambertation.wunderreich.blockentities.renderer;

import de.ambertation.wunderreich.blockentities.WunderKisteBlockEntity;
import de.ambertation.wunderreich.blocks.WunderKisteBlock;
import de.ambertation.wunderreich.client.WunderreichClient;
import de.ambertation.wunderreich.registries.WunderreichBlocks;
import de.ambertation.wunderreich.registries.WunderreichRules;
import de.ambertation.wunderreich.utils.WunderKisteDomain;
import de.ambertation.wunderreich.utils.WunderKisteServerExtension;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BrightnessCombiner;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import org.jetbrains.annotations.NotNull;

@Environment(value = EnvType.CLIENT)
public class WunderkisteRenderer extends ChestRenderer<WunderKisteBlockEntity> {

    private static final String BOTTOM = "bottom";
    private static final String LID = "lid";
    private static final String LOCK = "lock";
    private static final Vertex[] TOP_PLANE = {
            new Vertex(2.0f / 16.0f, 10.001f / 16.0f, 2.0f / 16.0f, 13.0f / 16.0f, 13.0f / 16.0f),
            new Vertex(2.0f / 16.0f, 10.001f / 16.0f, 14.0f / 16.0f, 1.0f / 16.0f, 13.0f / 16.0f),
            new Vertex(14.0f / 16.0f, 10.001f / 16.0f, 14.0f / 16.0f, 1.0f / 16.0f, 1.0f / 16.0f),
            new Vertex(14.0f / 16.0f, 10.001f / 16.0f, 2.0f / 16.0f, 13.0f / 16.0f, 1.0f / 16.0f)
    };
    private final ModelPart lid;
    private final ModelPart bottom;
    private final ModelPart lock;

    public WunderkisteRenderer(BlockEntityRendererProvider.Context context) {
        super(context);

        ModelPart modelPart = context.bakeLayer(ModelLayers.CHEST);
        this.bottom = modelPart.getChild(BOTTOM);
        this.lid = modelPart.getChild(LID);
        this.lock = modelPart.getChild(LOCK);
    }


    private static Material getTopMaterial(WunderKisteDomain d) {
        return d.useMonochromeFallback
                ? WunderreichClient.WUNDER_KISTE_MONOCHROME_TOP_LOCATION
                : WunderreichClient.WUNDER_KISTE_TOP_LOCATION;
    }

    @Override
    public void render(
            WunderKisteBlockEntity blockEntity,
            float f,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource multiBufferSource,
            int i,
            int overlayCoords
    ) {
        final Level level = blockEntity.getLevel();
        final boolean renderInWorld = level != null;

        BlockState blockState = blockEntity.getBlockState();
        if (blockState == null) blockState = WunderreichBlocks.WUNDER_KISTE.defaultBlockState();
        if (!renderInWorld) blockState = blockState.setValue(ChestBlock.FACING, Direction.SOUTH);

        if ((blockState.getBlock() instanceof AbstractChestBlock abstractChestBlock)) {
            final WunderKisteDomain d = WunderreichRules.Wunderkiste.showColors()
                    ? WunderKisteServerExtension.getDomain(blockState)
                    : WunderKisteBlock.DEFAULT_DOMAIN;

            poseStack.pushPose();
            float g = blockState.getValue(ChestBlock.FACING).toYRot();
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(-g));
            poseStack.translate(-0.5, -0.5, -0.5);

            DoubleBlockCombiner.NeighborCombineResult<ChestBlockEntity> neighborCombineResult = renderInWorld
                    ? abstractChestBlock.combine(blockState, level, blockEntity.getBlockPos(), true)
                    : DoubleBlockCombiner.Combiner::acceptNone;
            float openness = neighborCombineResult.apply(ChestBlock.opennessCombiner(blockEntity)).get(f);
            openness = 1.0f - openness;
            openness = 1.0f - openness * openness * openness;

            final int uv2 = ((Int2IntFunction) neighborCombineResult.apply(new BrightnessCombiner())).applyAsInt(i);
            Material material = d.getMaterial();
            VertexConsumer vertexConsumer = material.buffer(multiBufferSource, RenderType::entityCutout);
            this.render(
                    poseStack,
                    vertexConsumer,
                    this.lid,
                    this.lock,
                    this.bottom,
                    openness,
                    uv2,
                    overlayCoords,
                    d.overlayColor
            );

            if (openness > 0) {
                material = getTopMaterial(d);
                vertexConsumer = material.buffer(multiBufferSource, RenderType::entitySolid);
                this.renderAnimTop(
                        poseStack,
                        vertexConsumer,
                        this.bottom,
                        uv2,
                        overlayCoords,
                        d.color
                );
            }
            poseStack.popPose();
        }
    }

    private void render(
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            ModelPart lidPart,
            ModelPart lockPart,
            ModelPart bottomPart,
            float f,
            int uv2,
            int overlayCoord,
            int color
    ) {
        lockPart.xRot = lidPart.xRot = -(f * 1.5707964f);

        lidPart.render(poseStack, vertexConsumer, uv2, overlayCoord, color);
        lockPart.render(poseStack, vertexConsumer, uv2, overlayCoord, color);
        bottomPart.render(poseStack, vertexConsumer, uv2, overlayCoord, color);
    }

    private void renderAnimTop(
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            ModelPart bottomPart,
            int uv2,
            int overlayCords,
            int color
    ) {

        poseStack.pushPose();
        bottomPart.translateAndRotate(poseStack);
        final PoseStack.Pose last = poseStack.last();
        final Matrix4f pose = last.pose();
        final Matrix3f npose = last.normal();

        Vector3f normal = new Vector3f(0.0f, 1.0f, 0.0f); //==Axis.YP
        normal.mul(npose);

        for (Vertex v : TOP_PLANE) {
            Vector4f vector4f = new Vector4f(v.pos.x(), v.pos.y(), v.pos.z(), 1.0f);
            vector4f.mul(pose);
            vertexConsumer.addVertex(
                    vector4f.x(),
                    vector4f.y(),
                    vector4f.z(),
                    color,
                    v.u,
                    v.v,
                    overlayCords,
                    uv2,
                    normal.x(),
                    normal.y(),
                    normal.z()
            );
        }

        poseStack.popPose();
    }

    @Environment(value = EnvType.CLIENT)
    static class Vertex {
        public final Vector3f pos;
        public final float u;
        public final float v;

        public Vertex(float f, float g, float h, float i, float j) {
            this(new Vector3f(f, g, h), i, j);
        }

        public Vertex(Vector3f vector3f, float f, float g) {
            this.pos = vector3f;
            this.u = f;
            this.v = g;
        }
    }
}
