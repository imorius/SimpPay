package org.simpmc.simppay.util;

import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.simpmc.simppay.SPPlugin;

import java.util.UUID;

public class SoundUtil {
    public static void sendSound(Player player, Sound sound) {
        if (player == null || !player.isOnline()) {
            return;
        }
        SPPlugin.getInstance().getFoliaLib().getScheduler().runAtEntity(player, task -> {
            player.playSound(sound, Sound.Emitter.self());
        });
    }

    public static void sendSound(UUID playerUUID, Sound sound) {
        Player player = Bukkit.getPlayer(playerUUID);
        sendSound(player, sound);
    }
}
