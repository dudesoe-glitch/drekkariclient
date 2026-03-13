package haven.automated;

import haven.*;
import java.util.*;

/**
 * Transfers all items matching a given resource name from all open container
 * inventories to the player's main inventory.
 * Triggered by Alt+Shift+Click on an item.
 */
public class TransferAllFromContainersScript implements Runnable {
    private final GameUI gui;
    private final String targetResName;

    public TransferAllFromContainersScript(GameUI gui, String targetResName) {
        this.gui = gui;
        this.targetResName = targetResName;
    }

    @Override
    public void run() {
        try {
            Inventory playerInv = gui.maininv;
            if (playerInv == null) return;

            List<Inventory> allInvs = gui.getAllInventories();
            int transferred = 0;

            for (Inventory inv : allInvs) {
                if (inv == playerInv) continue;
                // Skip belt/keyring inventories
                Window w = inv.getparent(Window.class);
                if (w != null && (w.cap == null || "Belt".equals(w.cap) || "Key Ring".equals(w.cap)))
                    continue;

                List<WItem> matching = new ArrayList<>();
                for (WItem wi : inv.getAllItems()) {
                    if (Thread.currentThread().isInterrupted()) return;
                    try {
                        if (wi.item.resname().equals(targetResName)) {
                            matching.add(wi);
                        }
                    } catch (Loading ignored) {
                    }
                }

                for (WItem wi : matching) {
                    if (Thread.currentThread().isInterrupted()) return;
                    wi.item.wdgmsg("transfer", Coord.z, -1);
                    transferred++;
                    Thread.sleep(30);
                }
            }

            if (transferred > 0) {
                gui.errorsilent("Transferred " + transferred + " item(s) from containers.");
            }
        } catch (InterruptedException ignored) {
        } catch (Exception e) {
            gui.errorsilent("Transfer error: " + e.getMessage());
        }
    }
}
