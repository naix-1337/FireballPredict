package com.naix.predict;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityWitherSkull;
import net.minecraft.init.Items;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
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
    public static final String VERSION = "2.2";

    public static boolean enabled = true;
    public static BlockPos currentHitPos = null;
    public static EntityFireball currentFireball = null;
    public static Vec3 currentFireballOrigin = null;
    public static int currentColor = 0x00FF00;
    public static double currentETA = -1;

    private static final double NEAR_DISTANCE = 8.0D;
    private static final double MEDIUM_DISTANCE = 24.0D;
    private static final double FAR_DISTANCE = 48.0D;

    public static KeyBinding keyToggle;
    private int tickCounter = 0;
    private int warningCounter = 0;
    private static final double WARN_RANGE = 2.5D;

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        // 1. 创建按键绑定对象
        keyToggle = new KeyBinding(
            "key.fireball_predict.toggle",
            Keyboard.KEY_R,
            "key.categories.fireball_predict"
        );
        
        // 2. 注册到 ClientRegistry，使其在按键控制菜单中出现
        ClientRegistry.registerKeyBinding(keyToggle);

        // 3. 【关键修复】 Forge 1.8.9 中必须将包含 @SubscribeEvent 的类全部注册到 MinecraftForge.EVENT_BUS
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new PredictionRenderer());
    }

    // === 按键响应监听 ===
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event)
    {
        // 自动响应玩家在设置菜单中自定义修改后的按键
        if (keyToggle != null && keyToggle.isPressed()) {
            enabled = !enabled;
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(
                    new ChatComponentText(enabled ? "§a[火焰弹预测] 已开启" : "§c[火焰弹预测] 已关闭")
                );
            }
        }
    }

    // === 每帧预测计算 ===
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;
        if (++tickCounter % 2 != 0) return;
        if (!enabled) {
            currentHitPos = null;
            currentFireball = null;
            currentFireballOrigin = null;
            currentColor = 0;
            currentETA = -1;
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;
        if (world == null || mc.thePlayer == null) {
            currentHitPos = null;
            currentFireball = null;
            currentFireballOrigin = null;
            currentColor = 0;
            currentETA = -1;
            return;
        }

        BlockPos newHit = null;
        int color = 0xFF0000;

        double minDist = Double.MAX_VALUE;
        EntityFireball bestFireball = null;
        Vec3 playerPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
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
            Vec3 end = start.addVector(dir.xCoord * 500, dir.yCoord * 500, dir.zCoord * 500);

            MovingObjectPosition mop = world.rayTraceBlocks(start, end);
            if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                Vec3 hitVec = mop.hitVec;
                double dx = hitVec.xCoord - fb.posX;
                double dy = hitVec.yCoord - fb.posY;
                double dz = hitVec.zCoord - fb.posZ;
                double impactDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);

                BlockPos hitPos = mop.getBlockPos();
                Vec3 hitCenter = new Vec3(
                    hitPos.getX() + 0.5, hitPos.getY() + 0.5, hitPos.getZ() + 0.5);
                double playerDistance = playerPos.squareDistanceTo(hitCenter);
                if (playerDistance < minDist) {
                    minDist = playerDistance;
                    newHit = hitPos;
                    color = getDistanceColor(impactDistance);
                    bestFireball = fb;
                }
            }
        }

        if (bestFireball != null && bestFireball != currentFireball) {
            currentFireballOrigin = new Vec3(bestFireball.posX, bestFireball.posY, bestFireball.posZ);
        } else if (bestFireball == null) {
            currentFireballOrigin = null;
        }

        currentFireball = bestFireball;

        if (bestFireball != null && newHit != null) {
            Vec3 fbPos = new Vec3(bestFireball.posX, bestFireball.posY, bestFireball.posZ);
            Vec3 hitCenter = new Vec3(newHit.getX() + 0.5, newHit.getY() + 0.5, newHit.getZ() + 0.5);
            double fbSpeed = Math.sqrt(
                bestFireball.motionX * bestFireball.motionX +
                bestFireball.motionY * bestFireball.motionY +
                bestFireball.motionZ * bestFireball.motionZ
            );
            currentETA = fbPos.distanceTo(hitCenter) / (fbSpeed * 20.0);
        } else {
            currentETA = -1;
        }

        if (newHit == null) {
            for (EntityPlayer p : world.playerEntities) {
                if (p.getHeldItem() == null || p.getHeldItem().getItem() != Items.fire_charge)
                    continue;

                Vec3 eye = p.getPositionEyes(1.0f);
                Vec3 look = p.getLook(1.0f);
                Vec3 end = eye.addVector(look.xCoord * 500, look.yCoord * 500, look.zCoord * 500);

                MovingObjectPosition mop = world.rayTraceBlocks(eye, end);
                if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    newHit = mop.getBlockPos();
                    color = 0xFFFF00;
                    break;
                }
            }
        }

        currentHitPos = newHit;
        currentColor = newHit == null ? 0 : color;

        if (newHit != null && color != 0xFFFF00) {
            double dx = Math.abs(mc.thePlayer.posX - (newHit.getX() + 0.5));
            double dy = Math.abs(mc.thePlayer.posY - (newHit.getY() + 0.5));
            double dz = Math.abs(mc.thePlayer.posZ - (newHit.getZ() + 0.5));
            if (dx <= WARN_RANGE && dy <= WARN_RANGE && dz <= WARN_RANGE) {
                if (++warningCounter % 5 == 0) {
                    mc.thePlayer.addChatMessage(
                        new ChatComponentText("§c当前位于烈焰弹爆炸范围内"));
                }
            }
        }
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