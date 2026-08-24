package com.hackerai.jumpreset;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.common.MinecraftForge;

@Mod(modid = JumpResetMod.MODID, version = JumpResetMod.VERSION, name = "JumpReset")
public class JumpResetMod {
    public static final String MODID = "jumpreset";
    public static final String VERSION = "1.0";
    public static boolean enabled = false;

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new JumpResetHandler());
    }

    @EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new JumpResetCommand());
    }
}
