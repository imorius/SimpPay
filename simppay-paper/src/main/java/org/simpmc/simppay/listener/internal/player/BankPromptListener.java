package org.simpmc.simppay.listener.internal.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.commands.sub.banking.CancelCommand;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.MessageConfig;
import org.simpmc.simppay.config.types.menu.BankQrMenuConfig;
import org.simpmc.simppay.data.PaymentType;
import org.simpmc.simppay.event.PaymentBankPromptEvent;
import org.simpmc.simppay.event.PaymentFailedEvent;
import org.simpmc.simppay.event.PaymentSuccessEvent;
import org.simpmc.simppay.handler.banking.data.BankingData;
import org.simpmc.simppay.service.PaymentService;
import org.simpmc.simppay.util.MessageUtil;
import org.simpmc.simppay.util.qrcode.BankQrRenderer;
import xyz.xenondevs.inventoryaccess.map.MapPatch;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemWrapper;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.CartographyWindow;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BankPromptListener implements Listener {

    private static final ConcurrentHashMap<UUID, CartographyWindow> activeWindows = new ConcurrentHashMap<>();

    public BankPromptListener(SPPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void paymentPrompt(PaymentBankPromptEvent event) {
        MessageConfig config = ConfigManager.getInstance().getConfig(MessageConfig.class);
        BankingData bankingData = event.getBankingData();
        if (bankingData.getUrl() != null) {
            MessageUtil.sendMessage(event.getPlayerUUID(), config.promptPaymentLink.replace("<link>", bankingData.getUrl()));
        }

        Player player = Bukkit.getPlayer(event.getPlayerUUID());
        if (player == null) {
            return;
        }

        SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> {
            try {
                byte[] mapBytes = BankQrRenderer.render(bankingData);
                SPPlugin.getService(PaymentService.class).getPlayerBankQRCode().put(event.getPlayerUUID(), mapBytes);
                openPreview(event.getPlayerUUID(), mapBytes);
            } catch (Exception e) {
                MessageUtil.warn("[BankPrompt] Error preparing QR preview: " + e.getMessage());
                MessageUtil.sendMessage(event.getPlayerUUID(), config.bankQrUnavailable);
            }
        });
    }

    public static void openPreview(UUID playerUUID, byte[] mapBytes) {
        if (mapBytes == null) {
            MessageUtil.debug("[BankPrompt] No cached QR preview available for player: " + playerUUID);
            return;
        }

        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) {
            return;
        }

        SPPlugin.getInstance().getFoliaLib().getScheduler().runAtEntity(player, task -> {
            closePreviewNow(playerUUID);

            BankQrMenuConfig menuConfig = ConfigManager.getInstance().getConfig(BankQrMenuConfig.class);
            Gui upperGui = Gui.empty(2, 1);
            Gui lowerGui = Gui.empty(9, 4);
            applyConfiguredItems(player, upperGui, lowerGui, menuConfig);

            CartographyWindow window = CartographyWindow.split()
                    .setViewer(player)
                    .setTitle(menuConfig.title)
                    .setUpperGui(upperGui)
                    .setLowerGui(lowerGui)
                    .build();
            window.addCloseHandler(() -> activeWindows.remove(playerUUID, window));
            activeWindows.put(playerUUID, window);
            window.open();
            window.updateMap(new MapPatch(0, 0, BankQrRenderer.MAP_SIZE, BankQrRenderer.MAP_SIZE, mapBytes));
            MessageConfig config = ConfigManager.getInstance().getConfig(MessageConfig.class);
            MessageUtil.sendMessage(player, config.pendingBank);
            MessageUtil.debug("[BankPrompt] Opened bank QR preview for player: " + player.getName());
        });
    }

    private static void applyConfiguredItems(Player player, Gui upperGui, Gui lowerGui, BankQrMenuConfig config) {
        if (config.items == null) {
            return;
        }

        for (BankQrMenuConfig.MenuEntry entry : config.items) {
            if (!isValidEntry(entry)) {
                continue;
            }

            Item item = createMenuItem(player, entry);
            if (entry.inventory == BankQrMenuConfig.InventorySection.UPPER) {
                upperGui.setItem(entry.slot, item);
            } else {
                lowerGui.setItem(entry.slot, item);
            }
        }
    }

    private static boolean isValidEntry(BankQrMenuConfig.MenuEntry entry) {
        if (entry == null || entry.inventory == null || entry.item == null || entry.item.getMaterial() == null) {
            MessageUtil.warn("[BankPrompt] Skipping invalid bank QR menu entry: missing inventory or item material");
            return false;
        }

        int maxSlot = entry.inventory == BankQrMenuConfig.InventorySection.UPPER ? 1 : 35;
        if (entry.slot < 0 || entry.slot > maxSlot) {
            MessageUtil.warn("[BankPrompt] Skipping invalid bank QR menu entry: " + entry.inventory
                    + " slot " + entry.slot + " is outside 0-" + maxSlot);
            return false;
        }

        return true;
    }

    private static Item createMenuItem(Player player, BankQrMenuConfig.MenuEntry entry) {
        ItemWrapper itemProvider = new ItemWrapper(entry.item.getItemStack(player));
        if (entry.action == BankQrMenuConfig.MenuAction.CANCEL) {
            return new SimpleItem(itemProvider, click -> CancelCommand.cancel(click.getPlayer()));
        }
        return new SimpleItem(itemProvider);
    }

    public static void closePreview(UUID playerUUID) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            SPPlugin.getInstance().getFoliaLib().getScheduler().runAtEntity(player, task -> closePreviewNow(playerUUID));
            return;
        }
        closePreviewNow(playerUUID);
    }

    private static void closePreviewNow(UUID playerUUID) {
        CartographyWindow window = activeWindows.remove(playerUUID);
        if (window != null && window.isOpen()) {
            window.close();
        }
    }

    @EventHandler
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        if (event.getPaymentType() == PaymentType.BANKING) {
            closePreview(event.getPlayerUUID());
        }
    }

    @EventHandler
    public void onPaymentFailed(PaymentFailedEvent event) {
        if (event.getPaymentType() == PaymentType.BANKING) {
            closePreview(event.getPlayerUUID());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        closePreviewNow(event.getPlayer().getUniqueId());
    }
}
