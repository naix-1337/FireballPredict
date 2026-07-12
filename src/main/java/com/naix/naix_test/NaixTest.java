package com.naix.naix_test;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.input.Keyboard;

@Mod(modid = NaixTest.MODID, version = NaixTest.VERSION)
public class NaixTest
{
    public static final String MODID = "naix_test";
    public static final String VERSION = "1.0";

    private KeyBinding keyHelloWorld;

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        // 注册按键：R键，类别 "Naix Test"
        keyHelloWorld = new KeyBinding(
            "key.naix_test.hello.desc",       // 按键描述（显示在控制设置中）
            Keyboard.KEY_R,                    // 默认绑定 R 键
            "key.categories.naix_test"         // 按键类别
        );
        ClientRegistry.registerKeyBinding(keyHelloWorld);

        // 注册事件监听（KeyInputEvent 在 FML bus 上）
        FMLCommonHandler.instance().bus().register(this);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event)
    {
        if (keyHelloWorld.isPressed())
        {
            Minecraft.getMinecraft().thePlayer.addChatMessage(
                new ChatComponentText("Hello World!")
            );
        }
    }
}
