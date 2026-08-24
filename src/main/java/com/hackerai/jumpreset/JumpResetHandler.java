package com.hackerai.jumpreset;

import net.minecraft.client.Minecraft;
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
     * ============================================================
     * CONFIGURAÇÃO DE VELOCITY
     * ============================================================
     *
     * Horizontal:
     * 0.10 = recebe 10% do KB = 90% de redução
     *
     * Vertical:
     * 1.00 = recebe 100% do KB = sem redução
     */

    private static final double VELOCITY_HORIZONTAL = 0.10D;
    private static final double VELOCITY_VERTICAL = 1.00D;

    /*
     * ============================================================
     * CONEXÃO
     * ============================================================
     */

    @SubscribeEvent
    public void onConnect(
            FMLNetworkEvent.ClientConnectedToServerEvent event) {

        if (!injected) {
            injectPipeline();
            injected = true;
        }
    }

    @SubscribeEvent
    public void onDisconnect(
            FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {

        injected = false;
    }

    /*
     * ============================================================
     * INJEÇÃO NO PIPELINE
     * ============================================================
     */

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

        /*
         * Evita duplicar o handler.
         */
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

                        /*
                         * Se o mod estiver desligado,
                         * não altera os pacotes.
                         */
                        if (!JumpResetMod.enabled) {
                            super.channelRead(ctx, msg);
                            return;
                        }

                        /*
                         * =================================================
                         * S12PacketEntityVelocity
                         * =================================================
                         */
                        if (msg instanceof S12PacketEntityVelocity) {

                            S12PacketEntityVelocity packet =
                                    (S12PacketEntityVelocity) msg;

                            /*
                             * Verifica se o velocity é do jogador.
                             */
                            if (mc.thePlayer != null &&
                                    packet.getEntityID()
                                            == mc.thePlayer.getEntityId()) {

                                /*
                                 * Primeiro deixa o Minecraft aplicar
                                 * o velocity normalmente.
                                 */
                                super.channelRead(ctx, msg);

                                /*
                                 * Depois reduz SOMENTE o horizontal.
                                 */
                                reduceHorizontalVelocity();

                                return;
                            }
                        }

                        /*
                         * =================================================
                         * S27PacketExplosion
                         * =================================================
                         */
                        if (msg instanceof S27PacketExplosion) {

                            /*
                             * Primeiro deixa o Minecraft processar
                             * a explosão normalmente.
                             */
                            super.channelRead(ctx, msg);

                            /*
                             * Depois reduz somente X/Z.
                             * Y permanece normal.
                             */
                            if (mc.thePlayer != null) {
                                reduceHorizontalVelocity();
                            }

                            return;
                        }

                        /*
                         * Outros pacotes continuam normalmente.
                         */
                        super.channelRead(ctx, msg);
                    }

                    @Override
                    public void write(
                            ChannelHandlerContext ctx,
                            Object msg,
                            ChannelPromise promise)
                            throws Exception {

                        /*
                         * Não modificamos os pacotes enviados.
                         */
                        super.write(ctx, msg, promise);
                    }
                }
        );
    }

    /*
     * ============================================================
     * REDUÇÃO HORIZONTAL
     * ============================================================
     */

    private void reduceHorizontalVelocity() {

        if (mc.thePlayer == null) {
            return;
        }

        /*
         * X = horizontal
         * Z = horizontal
         *
         * 0.10 = recebe 10% do KB original.
         * Portanto, 90% de redução.
         */
        mc.thePlayer.motionX *= VELOCITY_HORIZONTAL;
        mc.thePlayer.motionZ *= VELOCITY_HORIZONTAL;

        /*
         * Y NÃO É ALTERADO.
         *
         * O velocity vertical permanece exatamente
         * como o Minecraft aplicou.
         */
    }
}
