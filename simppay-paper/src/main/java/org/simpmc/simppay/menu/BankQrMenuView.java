package org.simpmc.simppay.menu;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.commands.sub.banking.CancelCommand;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.MessageConfig;
import org.simpmc.simppay.config.types.menu.BankQrMenuConfig;
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

public final class BankQrMenuView {

    private static final ConcurrentHashMap<UUID, CartographyWindow> activeWindows = new ConcurrentHashMap<>();

    private BankQrMenuView() {
    }

    public static void openPreview(UUID playerUUID, byte[] mapBytes) {
        if (mapBytes == null) {
            MessageUtil.debug("[BankQrMenu] No cached QR preview available for player: " + playerUUID);
            return;
        }

        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) {
            return;
        }

        SPPlugin.getInstance().getFoliaLib().getScheduler().runAtEntity(player, task -> {
            closePreviewNow(playerUUID);

            BankQrMenuConfig menuConfig = ConfigManager.getInstance().getConfig(BankQrMenuConfig.class);
            CartographyWindow window = createWindow(player, menuConfig);
            window.addCloseHandler(() -> activeWindows.remove(playerUUID, window));
            activeWindows.put(playerUUID, window);
            window.open();
            applyMapItemModel(player, menuConfig);
            window.updateMap(new MapPatch(0, 0, BankQrRenderer.MAP_SIZE, BankQrRenderer.MAP_SIZE, mapBytes));

            MessageConfig config = ConfigManager.getInstance().getConfig(MessageConfig.class);
            MessageUtil.sendMessage(player, config.pendingBank);
            MessageUtil.debug("[BankQrMenu] Opened bank QR preview for player: " + player.getName());
        });
    }

    public static void closePreview(UUID playerUUID) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            SPPlugin.getInstance().getFoliaLib().getScheduler().runAtEntity(player, task -> closePreviewNow(playerUUID));
            return;
        }
        closePreviewNow(playerUUID);
    }

    public static void closePreviewNow(UUID playerUUID) {
        CartographyWindow window = activeWindows.remove(playerUUID);
        if (window != null && window.isOpen()) {
            window.close();
        }
    }

    private static CartographyWindow createWindow(Player player, BankQrMenuConfig menuConfig) {
        Gui upperGui = Gui.empty(2, 1);
        Gui lowerGui = Gui.empty(9, 4);
        applyConfiguredItems(player, upperGui, lowerGui, menuConfig);

        String title = PlaceholderAPI.setPlaceholders(player, menuConfig.title);
        return CartographyWindow.split()
                .setViewer(player)
                .setTitle(title)
                .setUpperGui(upperGui)
                .setLowerGui(lowerGui)
                .build();
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
            MessageUtil.warn("[BankQrMenu] Skipping invalid bank QR menu entry: missing inventory or item material");
            return false;
        }

        int maxSlot = entry.inventory == BankQrMenuConfig.InventorySection.UPPER ? 1 : 35;
        if (entry.slot < 0 || entry.slot > maxSlot) {
            MessageUtil.warn("[BankQrMenu] Skipping invalid bank QR menu entry: " + entry.inventory
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

    private static void applyMapItemModel(Player player, BankQrMenuConfig config) {
        if (config.mapItemModel == null || config.mapItemModel.isBlank()) {
            return;
        }

        if (!Key.parseable(config.mapItemModel)) {
            MessageUtil.warn("[BankQrMenu] Invalid map item-model '" + config.mapItemModel
                    + "', skipping custom QR map item model");
            return;
        }

        Inventory topInventory = player.getOpenInventory().getTopInventory();
        ItemStack mapItem = topInventory.getItem(0);
        if (mapItem == null || mapItem.isEmpty()) {
            MessageUtil.warn("[BankQrMenu] Could not apply QR map item model because cartography slot 0 is empty");
            return;
        }

        ItemStack customMapItem = mapItem.clone();
        customMapItem.setData(DataComponentTypes.ITEM_MODEL, Key.key(config.mapItemModel));
        customMapItem.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true));
        topInventory.setItem(0, customMapItem);
    }
}
