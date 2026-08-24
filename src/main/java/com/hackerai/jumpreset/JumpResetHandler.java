package com.hackerai.jumpreset;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;

public class JumpResetHandler {

    private Minecraft mc = Minecraft.getMinecraft();
    private boolean injected = false;

    // 0.1 = 90% de redução de knockback
    private final double REDUCTION_FACTOR = 0.1;

    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        if (!injected) {
            injectPipeline();
            injected = true;
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        injected = false;
    }

    private void injectPipeline() {
        if (mc.getNetHandler() == null || mc.getNetHandler().getNetworkManager() == null) return;
        ChannelPipeline pipeline = mc.getNetHandler().getNetworkManager().channel().pipeline();
        if (pipeline.get("jump_reset_handler") != null) return;

        pipeline.addBefore("packet_handler", "jump_reset_handler", new ChannelDuplexHandler() {

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                if (!JumpResetMod.enabled) {
                    super.channelRead(ctx, msg);
                    return;
                }

                if (msg instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity packet = (S12PacketEntityVelocity) msg;

                    if (packet.getEntityID() == mc.thePlayer.getEntityId()) {
                        sendJumpReset();

                        packet.setMotionX((int)(packet.getMotionX() * REDUCTION_FACTOR));
                        packet.setMotionY((int)(packet.getMotionY() * REDUCTION_FACTOR));
                        packet.setMotionZ((int)(packet.getMotionZ() * REDUCTION_FACTOR));

                        super.channelRead(ctx, packet);
                        return;
                    }
                }

                if (msg instanceof S27PacketExplosion) {
                    sendJumpReset();
                    S27PacketExplosion explosion = (S27PacketExplosion) msg;
                    explosion.setMotionX(explosion.getMotionX() * (float)REDUCTION_FACTOR);
                    explosion.setMotionY(explosion.getMotionY() * (float)REDUCTION_FACTOR);
                    explosion.setMotionZ(explosion.getMotionZ() * (float)REDUCTION_FACTOR);
                    super.channelRead(ctx, explosion);
                    return;
                }

                super.channelRead(ctx, msg);
            }

            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                super.write(ctx, msg, promise);
            }
        });
    }

    private void sendJumpReset() {
        if (mc.thePlayer == null) return;
        if (mc.getNetHandler() == null || mc.getNetHandler().getNetworkManager() == null) return;

        EntityPlayer player = mc.thePlayer;

        mc.getNetHandler().getNetworkManager().sendPacket(
            new C0BPacketEntityAction(player, C0BPacketEntityAction.Action.STOP_SPRINTING)
        );
        mc.getNetHandler().getNetworkManager().sendPacket(
            new C0BPacketEntityAction(player, C0BPacketEntityAction.Action.START_SNEAKING)
        );
        mc.getNetHandler().getNetworkManager().sendPacket(
            new C0BPacketEntityAction(player, C0BPacketEntityAction.Action.JUMP)
        );
        mc.getNetHandler().getNetworkManager().sendPacket(
            new C0BPacketEntityAction(player, C0BPacketEntityAction.Action.STOP_SNEAKING)
        );
    }
}
