package org.simpmc.simppay.config.types.menu;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import org.bukkit.Material;
import org.simpmc.simppay.config.annotations.Folder;
import org.simpmc.simppay.config.types.data.menu.DisplayItem;
import org.simpmc.simppay.config.types.data.menu.RoleType;

import java.util.List;

@Configuration
@Folder("menus")
public class BankQrMenuConfig {
    @Comment("Title cua cua so QR banking. Ho tro PlaceholderAPI dang %player_name%.")
    public String title = "Thanh toan ngan hang";

    @Comment({
            "Item model cho item map QR that o slot cartography index 0.",
            "De trong de giu hanh vi mac dinh. Vi du: simppay:bank/qr"
    })
    public String mapItemModel = "";

    @Comment({
            "Cac item tinh trong cua so QR.",
            "DisplayItem ho tro item-model, vi du simppay:bank/cancel.",
            "UPPER slot 0-1 la 2 slot cartography ma InvUI cho phep cau hinh.",
            "LOWER slot 0-35 la inventory nguoi choi trong split window.",
            "Slot map QR that cua cartography window la index 0 va dung map-item-model ben tren."
    })
    public List<MenuEntry> items = List.of(
            new MenuEntry(
                    InventorySection.UPPER,
                    0,
                    MenuAction.CANCEL,
                    DisplayItem.builder()
                            .material(Material.BARRIER)
                            .amount(1)
                            .role(RoleType.NONE)
                            .name("<red>Huy thanh toan")
                            .lores(List.of("<gray>Click de huy lenh thanh toan ngan hang."))
                            .itemModel("simppay:bank/cancel")
                            .build()
            )
    );

    @Configuration
    public static class MenuEntry {
        public InventorySection inventory = InventorySection.UPPER;
        public int slot = 0;
        public MenuAction action = MenuAction.NONE;
        public DisplayItem item = DisplayItem.builder()
                .material(Material.GRAY_STAINED_GLASS_PANE)
                .amount(1)
                .role(RoleType.NONE)
                .name(" ")
                .build();

        public MenuEntry() {
        }

        public MenuEntry(InventorySection inventory, int slot, MenuAction action, DisplayItem item) {
            this.inventory = inventory;
            this.slot = slot;
            this.action = action;
            this.item = item;
        }
    }

    public enum InventorySection {
        UPPER,
        LOWER
    }

    public enum MenuAction {
        NONE,
        CANCEL
    }
}
