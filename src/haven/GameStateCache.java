package haven;

import haven.res.ui.tt.armor.Armor;
import haven.res.ui.tt.q.qbuff.QBuff;
import haven.res.ui.tt.wear.Wear;
import haven.resutil.Curiosity;
import haven.resutil.FoodInfo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * Thread-safe cache of game state, updated on the UI thread via tick().
 * Any code (bots, HTTP handlers, etc.) can safely read snapshots from any thread.
 */
public class GameStateCache extends Widget {
    public static volatile GameStateCache instance;

    private volatile JSONArray inventorySnapshot = new JSONArray();
    private volatile JSONArray equipmentSnapshot = new JSONArray();
    private volatile JSONArray buffSnapshot = new JSONArray();
    private volatile JSONObject craftingSnapshot = new JSONObject();
    private volatile long lastUpdate = 0;
    private static final long UPDATE_INTERVAL_MS = 2000;
    private final GameUI gui;

    public GameStateCache(GameUI gui) {
        super(Coord.z);
        this.gui = gui;
        this.visible = false;
        instance = this;
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
        long now = System.currentTimeMillis();
        if (now - lastUpdate < UPDATE_INTERVAL_MS) return;
        lastUpdate = now;

        try { inventorySnapshot = snapshotInventory(); } catch (Exception ignored) {}
        try { equipmentSnapshot = snapshotEquipment(); } catch (Exception ignored) {}
        try { buffSnapshot = snapshotBuffs(); } catch (Exception ignored) {}
        try { craftingSnapshot = snapshotCrafting(); } catch (Exception ignored) {}

        // Notify any registered listeners
        for (Runnable listener : listeners) {
            try { listener.run(); } catch (Exception ignored) {}
        }
    }

    @Override
    public void destroy() {
        if (instance == this) instance = null;
        super.destroy();
    }

    // --- Public accessors (thread-safe reads) ---

    public JSONArray getInventory() { return inventorySnapshot; }
    public JSONArray getEquipment() { return equipmentSnapshot; }
    public JSONArray getBuffs() { return buffSnapshot; }
    public JSONObject getCrafting() { return craftingSnapshot; }
    public GameUI getGui() { return gui; }

    // --- Listener system for extensions ---
    private static final List<Runnable> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static void addListener(Runnable r) { listeners.add(r); }
    public static void removeListener(Runnable r) { listeners.remove(r); }

    // --- Snapshot builders (run on UI thread) ---

    private JSONArray snapshotInventory() {
        JSONArray items = new JSONArray();
        Inventory inv = gui.maininv;
        if (inv == null) return items;

        int idx = 0;
        for (Map.Entry<GItem, WItem> entry : inv.wmap.entrySet()) {
            try {
                JSONObject item = serializeItem(entry.getKey(), entry.getValue());
                if (item != null) {
                    item.put("slot", idx);
                    items.put(item);
                }
            } catch (Exception ignored) {}
            idx++;
        }
        return items;
    }

    private static final String[] SLOT_NAMES = {"Head", "Neck", "Shirt", "Chest Armor", "Gloves", "Belt",
        "Left Hand", "Right Hand", "Left Ring", "Right Ring", "Cloak", "Backpack",
        "Pants", "Leg Armor", "Cape", "Shoes", "?", "Eyewear", "Mouthwear",
        "Left Pouch", "Right Pouch", "Mask", "Shoulders"};

    private JSONArray snapshotEquipment() {
        JSONArray slots = new JSONArray();
        Equipory eq = gui.getequipory();
        if (eq == null) return slots;

        for (int i = 0; i < eq.slots.length; i++) {
            WItem wi = eq.slots[i];
            if (wi != null) {
                try {
                    JSONObject slot = serializeItem(wi.item, wi);
                    if (slot != null) {
                        slot.put("slot_index", i);
                        slot.put("slot_name", i < SLOT_NAMES.length ? SLOT_NAMES[i] : "Unknown");
                        slots.put(slot);
                    }
                } catch (Exception ignored) {}
            }
        }
        return slots;
    }

    private JSONArray snapshotBuffs() {
        JSONArray buffs = new JSONArray();
        if (gui.buffs == null) return buffs;

        for (Widget w = gui.buffs.child; w != null; w = w.next) {
            if (w instanceof Buff) {
                Buff b = (Buff) w;
                try {
                    JSONObject buff = new JSONObject();
                    Resource res = b.res.get();
                    if (res != null) {
                        buff.put("res", res.name);
                        Resource.Tooltip tt = res.layer(Resource.tooltip);
                        if (tt != null) buff.put("name", tt.t);
                    }
                    Double am = b.ameteri.get();
                    if (am != null) buff.put("meter", am);
                    buffs.put(buff);
                } catch (Loading ignored) {}
            }
        }
        return buffs;
    }

    private JSONObject snapshotCrafting() {
        JSONObject j = new JSONObject();
        CraftWindow cw = gui.makewnd;
        if (cw == null) { j.put("open", false); return j; }
        Makewindow mw = cw.makeWidget;
        if (mw == null) { j.put("open", false); return j; }

        j.put("open", true);
        j.put("recipe_name", mw.rcpnm != null ? mw.rcpnm : "");
        j.put("inputs", serializeSpecs(mw.inputs));
        j.put("outputs", serializeSpecs(mw.outputs));

        JSONArray qmods = new JSONArray();
        if (mw.qmod != null) {
            for (Indir<Resource> qm : mw.qmod) {
                try {
                    Resource r = qm.get();
                    Resource.Tooltip tt = r.layer(Resource.tooltip);
                    qmods.put(tt != null ? tt.t : r.name);
                } catch (Loading ignored) {}
            }
        }
        j.put("quality_mods", qmods);
        return j;
    }

    private JSONArray serializeSpecs(List<? extends Makewindow.SpecWidget> specs) {
        JSONArray arr = new JSONArray();
        if (specs == null) return arr;
        for (Makewindow.SpecWidget sw : specs) {
            try {
                JSONObject item = new JSONObject();
                Resource r = sw.spec.res.get();
                if (r != null) {
                    item.put("res", r.name);
                    Resource.Tooltip tt = r.layer(Resource.tooltip);
                    if (tt != null) item.put("name", tt.t);
                }
                item.put("optional", sw.opt);
                arr.put(item);
            } catch (Loading ignored) {}
        }
        return arr;
    }

    // --- Shared item serializer ---

    public static JSONObject serializeItem(GItem item, WItem wi) {
        if (item == null) return null;
        try {
            Resource res;
            try { res = item.getres(); } catch (Loading e) { return null; }
            if (res == null) return null;

            JSONObject j = new JSONObject();
            j.put("res", res.name);

            try {
                String name = item.getname();
                if (name != null) j.put("name", name);
            } catch (Exception ignored) {}

            double quality = 0;
            QBuff qb = item.getQBuff();
            if (qb != null) {
                quality = qb.q;
                j.put("quality", quality);
                j.put("q_multiplier", Math.sqrt(Math.max(quality, 1) / 10.0));
            }

            if (item.num >= 0) j.put("quantity", item.num);

            try {
                List<ItemInfo> info = item.info();
                if (info != null && !info.isEmpty()) {
                    double qMult = quality > 0 ? Math.sqrt(quality / 10.0) : 1.0;

                    FoodInfo food = ItemInfo.find(FoodInfo.class, info);
                    if (food != null) {
                        j.put("category", "food");
                        j.put("energy", food.end);
                        j.put("hunger", food.glut);
                        j.put("total_fep", food.sev);
                        j.put("base_fep_q10", food.sev / qMult);
                        JSONArray feps = new JSONArray();
                        if (food.evs != null) {
                            for (FoodInfo.Event ev : food.evs) {
                                try {
                                    JSONObject fep = new JSONObject();
                                    fep.put("name", ev.ev.nm);
                                    fep.put("value", ev.a);
                                    fep.put("base_q10", ev.a / qMult);
                                    feps.put(fep);
                                } catch (Exception ignored) {}
                            }
                        }
                        j.put("feps", feps);
                    }

                    Armor armor = ItemInfo.find(Armor.class, info);
                    if (armor != null) {
                        if (!j.has("category")) j.put("category", "armor");
                        j.put("armor_hard", armor.hard);
                        j.put("armor_soft", armor.soft);
                        j.put("armor_total", armor.hard + armor.soft);
                    }

                    Curiosity cur = ItemInfo.find(Curiosity.class, info);
                    if (cur != null) {
                        if (!j.has("category")) j.put("category", "curiosity");
                        j.put("lp", cur.exp);
                        j.put("mental_weight", cur.mw);
                        j.put("study_time_s", cur.time);
                        j.put("lph", cur.lph);
                    }

                    Wear wear = ItemInfo.find(Wear.class, info);
                    if (wear != null) {
                        j.put("durability_pct", wear.percentage);
                    }

                    // Weapon info — extract rendered tip text from abstract WeaponInfo subclasses
                    JSONArray weaponTips = new JSONArray();
                    for (ItemInfo ii : info) {
                        if (ii.getClass().getName().contains("WeaponInfo") ||
                            ii.getClass().getSuperclass() != null &&
                            ii.getClass().getSuperclass().getName().contains("WeaponInfo")) {
                            try {
                                java.lang.reflect.Method m = ii.getClass().getMethod("wpntips");
                                String tip = (String) m.invoke(ii);
                                if (tip != null && !tip.isEmpty())
                                    weaponTips.put(tip);
                            } catch (Exception ignored) {}
                        }
                    }
                    if (weaponTips.length() > 0) {
                        if (!j.has("category")) j.put("category", "weapon");
                        j.put("weapon_tips", weaponTips);
                    }

                    // Gilding/slots
                    for (ItemInfo ii : info) {
                        if (ii.getClass().getName().contains("ISlots")) {
                            if (!j.has("category")) j.put("category", "gilded");
                            try {
                                java.lang.reflect.Field leftF = ii.getClass().getField("left");
                                j.put("gilding_slots", leftF.getInt(ii));
                            } catch (Exception ignored) {}
                            break;
                        }
                    }

                    if (!j.has("category")) j.put("category", "misc");
                }
            } catch (Loading ignored) {}

            return j;
        } catch (Exception e) {
            return null;
        }
    }
}
