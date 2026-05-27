package org.simpmc.simppay.util;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PlayerDeliveryUtilTest {
    @Test
    void sendMessageWithNullPlayerDoesNotThrow() {
        assertDoesNotThrow(() -> MessageUtil.sendMessage((Player) null, "message"));
    }

    @Test
    void sendSoundWithNullPlayerDoesNotThrow() {
        Sound sound = Sound.sound(Key.key("minecraft:block.note_block.pling"), Sound.Source.PLAYER, 1.0f, 1.0f);

        assertDoesNotThrow(() -> SoundUtil.sendSound((Player) null, sound));
    }
}
