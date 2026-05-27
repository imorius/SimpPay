package org.simpmc.simppay.listener.internal.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.MessageConfig;
import org.simpmc.simppay.data.PaymentType;
import org.simpmc.simppay.event.PaymentBankPromptEvent;
import org.simpmc.simppay.event.PaymentFailedEvent;
import org.simpmc.simppay.event.PaymentSuccessEvent;
import org.simpmc.simppay.forms.BankingForm;
import org.simpmc.simppay.handler.banking.data.BankingData;
import org.simpmc.simppay.menu.BankQrMenuView;
import org.simpmc.simppay.service.PaymentService;
import org.simpmc.simppay.util.FloodgateUtil;
import org.simpmc.simppay.util.MessageUtil;
import org.simpmc.simppay.util.qrcode.BankQrRenderer;

public class BankPromptListener implements Listener {

    public BankPromptListener(SPPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void paymentPrompt(PaymentBankPromptEvent event) {
        MessageConfig config = ConfigManager.getInstance().getConfig(MessageConfig.class);
        BankingData bankingData = event.getBankingData();
        PaymentService paymentService = SPPlugin.getService(PaymentService.class);
        paymentService.getPlayerBankingData().put(event.getPlayerUUID(), bankingData);

        if (bankingData.getUrl() != null) {
            MessageUtil.sendMessage(event.getPlayerUUID(), config.promptPaymentLink.replace("<link>", bankingData.getUrl()));
        }

        Player player = Bukkit.getPlayer(event.getPlayerUUID());
        if (player == null) {
            return;
        }

        if (FloodgateUtil.isBedrockPlayer(player)) {
            BankingForm.send(player, bankingData);
            MessageUtil.sendMessage(player, config.pendingBank);
            return;
        }

        SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> {
            try {
                byte[] mapBytes = BankQrRenderer.render(bankingData);
                paymentService.getPlayerBankQRCode().put(event.getPlayerUUID(), mapBytes);
                BankQrMenuView.openPreview(event.getPlayerUUID(), mapBytes);
            } catch (Exception e) {
                MessageUtil.warn("[BankPrompt] Error preparing QR preview: " + e.getMessage());
                MessageUtil.sendMessage(event.getPlayerUUID(), config.bankQrUnavailable);
            }
        });
    }

    @EventHandler
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        if (event.getPaymentType() == PaymentType.BANKING) {
            BankQrMenuView.closePreview(event.getPlayerUUID());
        }
    }

    @EventHandler
    public void onPaymentFailed(PaymentFailedEvent event) {
        if (event.getPaymentType() == PaymentType.BANKING) {
            BankQrMenuView.closePreview(event.getPlayerUUID());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        BankQrMenuView.closePreviewNow(event.getPlayer().getUniqueId());
    }
}
