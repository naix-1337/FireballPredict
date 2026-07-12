package com.naix.predict;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityWitherSkull;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.input.Keyboard;

import java.util.List;

@Mod(modid = FireballPredict.MODID, version = FireballPredict.VERSION)
public class FireballPredict
{
    public static final String MODID = "fireball_predict";
    public static final String VERSION = "1.0";

    // 开关状态 (PredictionRenderer 读取)
    public static boolean enabled = true;
    public static BlockPos currentHitPos = null;
    public static int currentColor = 0x00FF00;

    // 火球距离预测撞击点越近，警告颜色越偏红；越远则越偏绿。
    private static final double NEAR_DISTANCE = 8.0D;
    private static final double MEDIUM_DISTANCE = 24.0D;
    private static final double FAR_DISTANCE = 48.0D;

    private KeyBinding keyToggle;

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        // R 键：开关火焰弹预测
        keyToggle = new KeyBinding(
            "key.naix_test.fireball",     // 描述
            Keyboard.KEY_R,                // R 键
            "key.categories.naix_test"     // 类别
        );
        ClientRegistry.registerKeyBinding(keyToggle);

        // 注册事件
        FMLCommonHandler.instance().bus().register(this);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new PredictionRenderer());
    }

    // === R 键开关 ===
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event)
    {
        if (keyToggle.isPressed()) {

            enabled = !enabled;
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(
                    new ChatComponentText(enabled ? "§a[火焰弹预测] 已开启" : "§c[火焰弹预测] 已关闭")
                );
            }
        }
    }

    // === 每帧预测 ===
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;
        if (!enabled) {
            currentHitPos = null;
            currentColor = 0;
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;
        if (world == null || mc.thePlayer == null) {
            currentHitPos = null;
            currentColor = 0;
            return;
        }

        BlockPos newHit = null;
        int color = 0x00FF00;
        double nearestImpactDistance = Double.MAX_VALUE;

        // 只检测已经存在于世界中的火球实体，不检测玩家手持的烈焰弹。
        List<Entity> entities = world.loadedEntityList;
        for (int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            if (!(e instanceof EntityFireball)) continue;
            if (e instanceof EntityWitherSkull) continue;

            EntityFireball fb = (EntityFireball) e;
            double speed = fb.motionX * fb.motionX + fb.motionY * fb.motionY + fb.motionZ * fb.motionZ;
            if (speed < 0.0001) continue;

            Vec3 start = new Vec3(fb.posX, fb.posY, fb.posZ);
            Vec3 dir = new Vec3(fb.motionX, fb.motionY, fb.motionZ).normalize();
            Vec3 end = start.addVector(dir.xCoord * 100, dir.yCoord * 100, dir.zCoord * 100);

            MovingObjectPosition mop = world.rayTraceBlocks(start, end);
            if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                Vec3 hitVec = mop.hitVec;
                double dx = hitVec.xCoord - fb.posX;
                double dy = hitVec.yCoord - fb.posY;
                double dz = hitVec.zCoord - fb.posZ;
                double impactDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);

                // 多个火球同时存在时，优先显示马上就要撞击的火球。
                if (impactDistance < nearestImpactDistance) {
                    nearestImpactDistance = impactDistance;
                    newHit = mop.getBlockPos();
                    color = getDistanceColor(impactDistance);
                }
            }
        }

        currentHitPos = newHit;
        currentColor = newHit == null ? 0 : color;
    }

    private static int getDistanceColor(double distance)
    {
        if (distance <= NEAR_DISTANCE) return 0xFF0000;
        if (distance >= FAR_DISTANCE) return 0x00FF00;

        if (distance <= MEDIUM_DISTANCE) {
            float progress = (float) ((distance - NEAR_DISTANCE) / (MEDIUM_DISTANCE - NEAR_DISTANCE));
            return rgb(255, Math.round(255 * progress), 0);
        }

        float progress = (float) ((distance - MEDIUM_DISTANCE) / (FAR_DISTANCE - MEDIUM_DISTANCE));
        return rgb(Math.round(255 * (1.0F - progress)), 255, 0);
    }

    private static int rgb(int red, int green, int blue)
    {
        return (red << 16) | (green << 8) | blue;
    }
}
