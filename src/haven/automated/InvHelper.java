package haven.automated;

import haven.*;
import haven.res.ui.tt.q.qbuff.QBuff;
import haven.resutil.FoodInfo;

import java.util.*;

/**
 * Shared inventory utility methods for bot automation.
 * All methods are static, null-safe, and handle Loading exceptions gracefully.
 */
public class InvHelper {

    /** Known drink container resource base names. */
    private static final Set<String> DRINK_CONTAINERS = new HashSet<>(Arrays.asList(
        "bucket", "flask", "waterskin", "waterflask", "kuksa", "tankard", "mug", "cup"
    ));

    /** Find the first item in inventory matching the exact resource name. */
    public static WItem findFirstByName(Inventory inv, String resName) {
        if (inv == null || resName == null) return null;
        for (Widget wdg = inv.child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                WItem wi = (WItem) wdg;
                try {
                    if (wi.item.getres().name.equals(resName)) {
                        return wi;
                    }
                } catch (Loading ignored) {}
            }
        }
        return null;
    }

    /** Find the first item in inventory whose resource name contains the substring. */
    public static WItem findFirstContaining(Inventory inv, String partial) {
        if (inv == null || partial == null) return null;
        for (Widget wdg = inv.child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                WItem wi = (WItem) wdg;
                try {
                    if (wi.item.getres().name.contains(partial)) {
                        return wi;
                    }
                } catch (Loading ignored) {}
            }
        }
        return null;
    }

    /** Find all items matching exact resource name. */
    public static List<WItem> findAllByName(Inventory inv, String resName) {
        List<WItem> result = new ArrayList<>();
        if (inv == null || resName == null) return result;
        for (Widget wdg = inv.child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                WItem wi = (WItem) wdg;
                try {
                    if (wi.item.getres().name.equals(resName)) {
                        result.add(wi);
                    }
                } catch (Loading ignored) {}
            }
        }
        return result;
    }

    /** Find all items whose resource name contains the substring. */
    public static List<WItem> findAllContaining(Inventory inv, String partial) {
        List<WItem> result = new ArrayList<>();
        if (inv == null || partial == null) return result;
        for (Widget wdg = inv.child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                WItem wi = (WItem) wdg;
                try {
                    if (wi.item.getres().name.contains(partial)) {
                        result.add(wi);
                    }
                } catch (Loading ignored) {}
            }
        }
        return result;
    }

    /** Check if inventory contains any item matching exact resource name. */
    public static boolean contains(Inventory inv, String resName) {
        return findFirstByName(inv, resName) != null;
    }

    /** Get the highest quality item matching the resource name, or null if none found. */
    public static WItem findBestQuality(Inventory inv, String resName) {
        List<WItem> items = findAllByName(inv, resName);
        if (items.isEmpty()) return null;

        WItem best = null;
        double bestQ = -1;
        for (WItem wi : items) {
            double q = getQuality(wi.item);
            if (q > bestQ) {
                bestQ = q;
                best = wi;
            }
        }
        return best;
    }

    /** Get free space count in inventory (in 1x1 slots). Returns 0 if inv is null. */
    public static int getFreeSpace(Inventory inv) {
        if (inv == null) return 0;
        return inv.getFreeSpace();
    }

    /** Check if item is a drink container (waterskin, flask, bucket, kuksa, etc.). */
    public static boolean isDrinkContainer(WItem item) {
        if (item == null) return false;
        try {
            String resName = item.item.getres().name;
            String baseName = resName.substring(resName.lastIndexOf('/') + 1);
            return DRINK_CONTAINERS.contains(baseName);
        } catch (Loading ignored) {}
        return false;
    }

    /** Check if item is food (has FoodInfo). */
    public static boolean isFood(WItem item) {
        if (item == null) return false;
        try {
            for (ItemInfo info : item.item.info()) {
                if (info instanceof FoodInfo) {
                    return true;
                }
            }
        } catch (Loading ignored) {}
        return false;
    }

    /** Get item quality, or -1 if not loaded or unavailable. */
    public static double getQuality(GItem item) {
        if (item == null) return -1;
        try {
            QBuff qb = item.getQBuff();
            if (qb != null) {
                return qb.q;
            }
        } catch (Loading ignored) {}
        return -1;
    }

    /** Get item resource name, or null if not loaded. */
    public static String getItemName(GItem item) {
        if (item == null) return null;
        try {
            Resource res = item.getres();
            if (res != null) {
                return res.name;
            }
        } catch (Loading ignored) {}
        return null;
    }

    /**
     * Get all items from all open inventories and item stacks, excluding Belt and Keyring.
     * Skips "stack of" container items (their contents are collected separately from
     * ContentsWindows). This is a cleaner equivalent of
     * {@code AUtils.getAllItemsFromAllInventoriesAndStacksExcludeBeltAndKeyring()}.
     *
     * @param gui the GameUI instance
     * @return list of all WItems, possibly empty
     */
    public static List<WItem> getAllItemsExcludeBeltKeyring(GameUI gui) {
        List<WItem> items = new ArrayList<>();
        if (gui == null) return items;

        for (Inventory inventory : gui.getAllInventories()) {
            if (inventory.parent instanceof Window) {
                String cap = ((Window) inventory.parent).cap;
                if (cap != null && (cap.contains("Belt") || cap.contains("Keyring"))) {
                    continue;
                }
            }
            for (WItem item : inventory.getAllItems()) {
                try {
                    if (!item.item.getname().contains("stack of")) {
                        items.add(item);
                    }
                } catch (Loading ignored) {}
            }
        }

        items.addAll(gui.getAllContentsWindows());
        return items;
    }

    /** Transfer item to another inventory (Shift+Click behavior). */
    public static void transferToPlayer(GItem item) {
        if (item == null) return;
        item.wdgmsg("transfer", Coord.z);
    }

    /** Drop item from inventory (Ctrl+Click behavior). */
    public static void dropItem(GItem item) {
        if (item == null) return;
        item.wdgmsg("drop", Coord.z);
    }
}
