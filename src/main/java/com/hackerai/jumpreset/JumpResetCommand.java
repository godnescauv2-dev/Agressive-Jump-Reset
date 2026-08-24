package com.hackerai.jumpreset;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class JumpResetCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "jumpreset";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/jumpreset";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        JumpResetMod.enabled = !JumpResetMod.enabled;
        String status = JumpResetMod.enabled
            ? EnumChatFormatting.GREEN + "ativado"
            : EnumChatFormatting.RED + "desativado";
        sender.addChatMessage(new ChatComponentText(
            EnumChatFormatting.AQUA + "[JumpReset] " + status
        ));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
