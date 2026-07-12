package com.naix.predict;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PredictionRenderer
{
    private static final float ALPHA = 0.3f;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event)
    {
        if (!FireballPredict.enabled || FireballPredict.currentHitPos == null) return;

        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;
        if (world == null) return;

        BlockPos hit = FireballPredict.currentHitPos;
        int color = FireballPredict.currentColor;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        double playerX = mc.thePlayer.lastTickPosX
            + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * event.partialTicks;
        double playerY = mc.thePlayer.lastTickPosY
            + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * event.partialTicks;
        double playerZ = mc.thePlayer.lastTickPosZ
            + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * event.partialTicks;

        GlStateManager.pushMatrix();
        GlStateManager.translate(-playerX, -playerY, -playerZ);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();

        try
        {
            Tessellator tess = Tessellator.getInstance();
            WorldRenderer wr = tess.getWorldRenderer();
            wr.begin(7, DefaultVertexFormats.POSITION_COLOR);

            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        BlockPos bp = hit.add(dx, dy, dz);
                        if (world.isAirBlock(bp)) continue;
                        if (!world.getBlockState(bp).getBlock().isFullCube()) continue;
                        if (world.getBlockState(bp.up()).getBlock().isOpaqueCube()
                         && world.getBlockState(bp.down()).getBlock().isOpaqueCube()
                         && world.getBlockState(bp.north()).getBlock().isOpaqueCube()
                         && world.getBlockState(bp.south()).getBlock().isOpaqueCube()
                         && world.getBlockState(bp.east()).getBlock().isOpaqueCube()
                         && world.getBlockState(bp.west()).getBlock().isOpaqueCube())
                            continue;

                        double bx = bp.getX();
                        double by = bp.getY();
                        double bz = bp.getZ();
                        double ex = bx + 1.0;
                        double ey = by + 1.0;
                        double ez = bz + 1.0;

                        // 底 Y-
                        wr.pos(bx, by, bz).color(r, g, b, ALPHA).endVertex();
                        wr.pos(bx, by, ez).color(r, g, b, ALPHA).endVertex();
                        wr.pos(ex, by, ez).color(r, g, b, ALPHA).endVertex();
                        wr.pos(ex, by, bz).color(r, g, b, ALPHA).endVertex();
                        // 顶 Y+
                        wr.pos(bx, ey, bz).color(r, g, b, ALPHA).endVertex();
                        wr.pos(ex, ey, bz).color(r, g, b, ALPHA).endVertex();
                        wr.pos(ex, ey, ez).color(r, g, b, ALPHA).endVertex();
                        wr.pos(bx, ey, ez).color(r, g, b, ALPHA).endVertex();
                        // 西 X-
                        wr.pos(bx, by, bz).color(r, g, b, ALPHA).endVertex();
                        wr.pos(bx, ey, bz).color(r, g, b, ALPHA).endVertex();
                        wr.pos(bx, ey, ez).color(r, g, b, ALPHA).endVertex();
                        wr.pos(bx, by, ez).color(r, g, b, ALPHA).endVertex();
                        // 东 X+
                        wr.pos(ex, by, bz).color(r, g, b, ALPHA).endVertex();
                        wr.pos(ex, by, ez).color(r, g, b, ALPHA).endVertex();
                        wr.pos(ex, ey, ez).color(r, g, b, ALPHA).endVertex();
                        wr.pos(ex, ey, bz).color(r, g, b, ALPHA).endVertex();
                        // 北 Z-
                        wr.pos(bx, by, bz).color(r, g, b, ALPHA).endVertex();
                        wr.pos(ex, by, bz).color(r, g, b, ALPHA).endVertex();
                        wr.pos(ex, ey, bz).color(r, g, b, ALPHA).endVertex();
                        wr.pos(bx, ey, bz).color(r, g, b, ALPHA).endVertex();
                        // 南 Z+
                        wr.pos(bx, by, ez).color(r, g, b, ALPHA).endVertex();
                        wr.pos(bx, ey, ez).color(r, g, b, ALPHA).endVertex();
                        wr.pos(ex, ey, ez).color(r, g, b, ALPHA).endVertex();
                        wr.pos(ex, by, ez).color(r, g, b, ALPHA).endVertex();
                    }
                }
            }

            tess.draw();
        }
        finally
        {
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableCull();
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
        }
    }
}
