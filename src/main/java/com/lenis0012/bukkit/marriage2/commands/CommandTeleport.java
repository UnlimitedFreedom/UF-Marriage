package com.lenis0012.bukkit.marriage2.commands;

import com.lenis0012.bukkit.marriage2.MData;
import com.lenis0012.bukkit.marriage2.MPlayer;
import com.lenis0012.bukkit.marriage2.Marriage;
import com.lenis0012.bukkit.marriage2.config.Message;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Created by Lennart on 7/9/2015.
 */
public class CommandTeleport extends Command {

    public CommandTeleport(Marriage marriage) {
        super(marriage, "tp");
        setDescription("Teleport to your partner.");
    }

    @Override
    public void execute() {
        MPlayer mPlayer = marriage.getMPlayer(player.getUniqueId());
        MData marriage = mPlayer.getMarriage();
        if (marriage != null) {
            Player partner = Bukkit.getPlayer(marriage.getOtherPlayer(player.getUniqueId()));
            if (partner != null) {
                player.teleport(partner);
                reply(Message.TELEPORTED);
                partner.sendMessage(ChatColor.translateAlternateColorCodes('&', Message.TELEPORTED_2.toString()));
            } else {
                reply(Message.PARTNER_NOT_ONLINE);
            }
        } else {
            reply(Message.NOT_MARRIED);
        }
    }
}
