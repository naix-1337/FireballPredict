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
    public static final String VERSION = "1.0";

    // 开关状态 (PredictionRenderer 读取)
    public static boolean enabled = true;
    public static BlockPos currentHitPos = null;
    public static int currentColor = 0xFF0000; // 0xFF0000=红 0xFFFF00=黄

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
        int color = 0xFF0000; // 默认红色

        // 模式 1：火球检测
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
                newHit = mop.getBlockPos();
                color = 0xFF0000;
                break;
            }
        }

        // 模式 2：玩家手持烈焰弹
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
        if (newHit != null) currentColor = color;
    }
}
