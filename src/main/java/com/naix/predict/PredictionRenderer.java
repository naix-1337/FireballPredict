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
        if (!NaixTest.enabled || NaixTest.currentHitPos == null) return;

        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;
        if (world == null) return;

        BlockPos hit = NaixTest.currentHitPos;
        int color = NaixTest.currentColor;
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
            // 缓存 7×7×7 范围的方块状态，避免重复查询 World
            // 中心索引 3 = hit 位置，覆盖 hit±3 确保 5×5×5 的邻居查询都在缓存内
            final int SIZE = 7;
            boolean[] fullCache = new boolean[SIZE * SIZE * SIZE];
            boolean[] opaqueCache = new boolean[SIZE * SIZE * SIZE];

            for (int ix = 0; ix < SIZE; ix++) {
                for (int iy = 0; iy < SIZE; iy++) {
                    for (int iz = 0; iz < SIZE; iz++) {
                        BlockPos bp = hit.add(ix - 3, iy - 3, iz - 3);
                        int idx = (ix * SIZE + iy) * SIZE + iz;
                        if (!world.isAirBlock(bp)) {
                            net.minecraft.block.Block block = world.getBlockState(bp).getBlock();
                            fullCache[idx] = block.isFullCube();
                            opaqueCache[idx] = block.isOpaqueCube();
                        }
                    }
                }
            }

            Tessellator tess = Tessellator.getInstance();
            WorldRenderer wr = tess.getWorldRenderer();
            wr.begin(7, DefaultVertexFormats.POSITION_COLOR);

            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        int cx = dx + 3, cy = dy + 3, cz = dz + 3;
                        int ci = (cx * SIZE + cy) * SIZE + cz;
                        if (!fullCache[ci]) continue;

                        double bx = hit.getX() + dx;
                        double by = hit.getY() + dy;
                        double bz = hit.getZ() + dz;
                        double ex = bx + 1.0;
                        double ey = by + 1.0;
                        double ez = bz + 1.0;

                        int ni;

                        // 底 Y-
                        ni = (cx * SIZE + (cy - 1)) * SIZE + cz;
                        if (!opaqueCache[ni]) {
                            wr.pos(bx, by, bz).color(r, g, b, ALPHA).endVertex();
                            wr.pos(bx, by, ez).color(r, g, b, ALPHA).endVertex();
                            wr.pos(ex, by, ez).color(r, g, b, ALPHA).endVertex();
                            wr.pos(ex, by, bz).color(r, g, b, ALPHA).endVertex();
                        }
                        // 顶 Y+
                        ni = (cx * SIZE + (cy + 1)) * SIZE + cz;
                        if (!opaqueCache[ni]) {
                            wr.pos(bx, ey, bz).color(r, g, b, ALPHA).endVertex();
                            wr.pos(ex, ey, bz).color(r, g, b, ALPHA).endVertex();
                            wr.pos(ex, ey, ez).color(r, g, b, ALPHA).endVertex();
                            wr.pos(bx, ey, ez).color(r, g, b, ALPHA).endVertex();
                        }
                        // 西 X-
                        ni = ((cx - 1) * SIZE + cy) * SIZE + cz;
                        if (!opaqueCache[ni]) {
                            wr.pos(bx, by, bz).color(r, g, b, ALPHA).endVertex();
                            wr.pos(bx, ey, bz).color(r, g, b, ALPHA).endVertex();
                            wr.pos(bx, ey, ez).color(r, g, b, ALPHA).endVertex();
                            wr.pos(bx, by, ez).color(r, g, b, ALPHA).endVertex();
                        }
                        // 东 X+
                        ni = ((cx + 1) * SIZE + cy) * SIZE + cz;
                        if (!opaqueCache[ni]) {
                            wr.pos(ex, by, bz).color(r, g, b, ALPHA).endVertex();
                            wr.pos(ex, by, ez).color(r, g, b, ALPHA).endVertex();
                            wr.pos(ex, ey, ez).color(r, g, b, ALPHA).endVertex();
                            wr.pos(ex, ey, bz).color(r, g, b, ALPHA).endVertex();
                        }
                        // 北 Z-
                        ni = (cx * SIZE + cy) * SIZE + (cz - 1);
                        if (!opaqueCache[ni]) {
                            wr.pos(bx, by, bz).color(r, g, b, ALPHA).endVertex();
                            wr.pos(ex, by, bz).color(r, g, b, ALPHA).endVertex();
                            wr.pos(ex, ey, bz).color(r, g, b, ALPHA).endVertex();
                            wr.pos(bx, ey, bz).color(r, g, b, ALPHA).endVertex();
                        }
                        // 南 Z+
                        ni = (cx * SIZE + cy) * SIZE + (cz + 1);
                        if (!opaqueCache[ni]) {
                            wr.pos(bx, by, ez).color(r, g, b, ALPHA).endVertex();
                            wr.pos(bx, ey, ez).color(r, g, b, ALPHA).endVertex();
                            wr.pos(ex, ey, ez).color(r, g, b, ALPHA).endVertex();
                            wr.pos(ex, by, ez).color(r, g, b, ALPHA).endVertex();
                        }
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
