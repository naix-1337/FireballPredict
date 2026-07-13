package com.naix.predict;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityWitherSkull;
import net.minecraft.init.Items;
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
    public static final String VERSION = "2.1";

    // 开关状态 (PredictionRenderer 读取)
    public static boolean enabled = true;
    public static BlockPos currentHitPos = null;
    public static EntityFireball currentFireball = null;
    public static int currentColor = 0x00FF00;

    // 火球距离预测撞击点越近，警告颜色越偏红；越远则越偏绿。
    private static final double NEAR_DISTANCE = 8.0D;
    private static final double MEDIUM_DISTANCE = 24.0D;
    private static final double FAR_DISTANCE = 48.0D;

    private KeyBinding keyToggle;
    private int tickCounter = 0;
    private int warningCounter = 0;
    private static final double WARN_RANGE = 2.5D;  // 5×5×5 范围

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
        if (++tickCounter % 2 != 0) return;
        if (!enabled) {
            currentHitPos = null;
            currentFireball = null;
            currentColor = 0;
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;
        if (world == null || mc.thePlayer == null) {
            currentHitPos = null;
            currentFireball = null;
            currentColor = 0;
            return;
        }

        BlockPos newHit = null;
        int color = 0xFF0000;

        // 模式 1：火球检测。仍取落点距离玩家最近的火球。
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
            Vec3 end = start.addVector(dir.xCoord * 100, dir.yCoord * 100, dir.zCoord * 100);

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

        currentFireball = bestFireball;

        // 模式 2：玩家手持烈焰弹。保留原有预测，颜色固定为黄色。
        if (newHit == null) {
            for (EntityPlayer p : world.playerEntities) {
                if (p.getHeldItem() == null || p.getHeldItem().getItem() != Items.fire_charge)
                    continue;

                Vec3 eye = p.getPositionEyes(1.0f);
                Vec3 look = p.getLook(1.0f);
                Vec3 end = eye.addVector(look.xCoord * 100, look.yCoord * 100, look.zCoord * 100);

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

        // 玩家在落点 5×5×5 范围内时，聊天栏红字警告（每秒 2 次）
        // 仅在火球模式（模式 1）下触发，手持烈焰弹不提示
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
        return (red << 16) | (green << 8) | blue; //
    }
}
