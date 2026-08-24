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

    private final Minecraft mc = Minecraft.getMinecraft();

    private boolean injected = false;

    /*
     * 1.0 = 100% do knockback original
     * 0.5 = 50% do knockback
     * 0.1 = 10% do knockback
     *
     * ATENÇÃO:
     * Isso é redução de knockback, não um jump reset puro.
     */
    private static final double REDUCTION_FACTOR = 0.1D;

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

        if (mc.getNetHandler() == null) {
            return;
        }

        if (mc.getNetHandler().getNetworkManager() == null) {
            return;
        }

        ChannelPipeline pipeline =
                mc.getNetHandler()
                  .getNetworkManager()
                  .channel()
                  .pipeline();

        if (pipeline.get("jump_reset_handler") != null) {
            return;
        }

        pipeline.addBefore(
                "packet_handler",
                "jump_reset_handler",
                new ChannelDuplexHandler() {

                    @Override
                    public void channelRead(
                            ChannelHandlerContext ctx,
                            Object msg) throws Exception {

                        if (!JumpResetMod.enabled) {
                            super.channelRead(ctx, msg);
                            return;
                        }

                        /*
                         * VELOCITY PACKET
                         */
                        if (msg instanceof S12PacketEntityVelocity) {

                            S12PacketEntityVelocity packet =
                                    (S12PacketEntityVelocity) msg;

                            if (mc.thePlayer != null &&
                                packet.getEntityID() == mc.thePlayer.getEntityId()) {

                                /*
                                 * Primeiro deixa o Minecraft aplicar
                                 * o velocity normalmente.
                                 */
                                super.channelRead(ctx, msg);

                                /*
                                 * Depois reduz o knockback aplicado.
                                 */
                                reducePlayerVelocity();

                                /*
                                 * Tenta executar o jump reset.
                                 */
                                sendJumpReset();

                                return;
                            }
                        }

                        /*
                         * EXPLOSION PACKET
                         */
                        if (msg instanceof S27PacketExplosion) {

                            super.channelRead(ctx, msg);

                            if (mc.thePlayer != null) {

                                /*
                                 * O S27PacketExplosion não precisa
                                 * ser alterado diretamente.
                                 *
                                 * Depois que o Minecraft aplica
                                 * a explosão, reduzimos o movimento
                                 * resultante do jogador.
                                 */
                                reducePlayerVelocity();

                                sendJumpReset();
                            }

                            return;
                        }

                        super.channelRead(ctx, msg);
                    }

                    @Override
                    public void write(
                            ChannelHandlerContext ctx,
                            Object msg,
                            ChannelPromise promise) throws Exception {

                        super.write(ctx, msg, promise);
                    }
                }
        );
    }

    /**
     * Reduz o velocity atualmente aplicado ao jogador.
     */
    private void reducePlayerVelocity() {

        if (mc.thePlayer == null) {
            return;
        }

        mc.thePlayer.motionX *= REDUCTION_FACTOR;
        mc.thePlayer.motionY *= REDUCTION_FACTOR;
        mc.thePlayer.motionZ *= REDUCTION_FACTOR;
    }

    /**
     * Executa o jump reset.
     *
     * C0BPacketEntityAction.Action.JUMP NÃO EXISTE
     * no Minecraft 1.8.9.
     *
     * O Action.JUMP do código antigo estava errado.
     */
    private void sendJumpReset() {

        if (mc.thePlayer == null) {
            return;
        }

        EntityPlayer player = mc.thePlayer;

        /*
         * Um pulo normal deve ser executado pelo próprio
         * player.jump(), e não através de C0B.Action.JUMP.
         */
        if (player.onGround) {
            player.jump();
        }
    }
}
