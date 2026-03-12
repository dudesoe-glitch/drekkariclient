package haven.automated;

import haven.*;
import haven.Button;
import haven.Label;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import static haven.OCache.posres;

public class CombatDistanceTool extends BotBase {
    public static final Map<String, Double> animalDistances = Map.ofEntries(
            Map.entry("gfx/kritter/adder/adder", 17.1),
            Map.entry("gfx/kritter/ant/ant", 15.2),
            Map.entry("gfx/kritter/cattle/cattle", 27.0),
            Map.entry("gfx/kritter/badger/badger", 19.9),
            Map.entry("gfx/kritter/bear/bear", 24.7),
            Map.entry("gfx/kritter/bear/polarbear", 24.7),
            Map.entry("gfx/kritter/boar/boar", 25.1),
            Map.entry("gfx/kritter/caveangler/caveangler", 27.2),
            Map.entry("gfx/kritter/cavelouse/cavelouse", 22.0),
            Map.entry("gfx/kritter/fox/fox", 18.1),
            Map.entry("gfx/kritter/horse/horse", 23.0),
            Map.entry("gfx/kritter/lynx/lynx", 20.0),
            Map.entry("gfx/kritter/mammoth/mammoth", 30.3),
            Map.entry("gfx/kritter/moose/moose", 25.0),
            Map.entry("gfx/kritter/narwhal/narwhal", 32.0),
            Map.entry("gfx/kritter/orca/orca", 49.25),
            Map.entry("gfx/kritter/reddeer/reddeer", 25.0),
            Map.entry("gfx/kritter/roedeer/roedeer", 22.0),
            Map.entry("gfx/kritter/spermwhale/spermwhale", 112.2),
            Map.entry("gfx/kritter/goat/wildgoat", 18.9),
            Map.entry("gfx/kritter/wolf/wolf", 25.0),
            Map.entry("gfx/kritter/wolverine/wolverine", 21.0),
            Map.entry("gfx/borka/body", 55.0)
    );
    public static final Map<String, Double> vehicleDistance = Map.ofEntries(
            Map.entry("gfx/terobjs/vehicle/rowboat", 13.3),
            Map.entry("gfx/terobjs/vehicle/dugout", 7.4),
            Map.entry("gfx/terobjs/vehicle/snekkja", 29.35),
            Map.entry("gfx/terobjs/vehicle/knarr", 54.5),
            Map.entry("gfx/kritter/horse/stallion", 5.4),
            Map.entry("gfx/kritter/horse/mare", 5.4)
    );

    // Weapon type → optimal combat distance (world units)
    // Range multipliers from Ring of Brodgar wiki. World distance ≈ BASE_MELEE_DIST * multiplier.
    // Partial name match on resource path (e.g., "hirdsword" matches "gfx/invobjs/small/hirdsword").
    public static final double BASE_MELEE_DIST = 13.5; // Base melee distance for range 1.0 weapons
    public static final Map<String, Double> weaponDistances = new HashMap<>();
    static {
        // Range 1.0 weapons (~13.5 world units)
        weaponDistances.put("stoneaxe", BASE_MELEE_DIST * 1.0);      // Stone Axe
        weaponDistances.put("metalaxe", BASE_MELEE_DIST * 1.0);      // Metal Axe
        weaponDistances.put("cleaver", BASE_MELEE_DIST * 1.0);       // Butcher's Cleaver
        weaponDistances.put("flintknife", BASE_MELEE_DIST * 1.0);    // Flint Knife
        weaponDistances.put("ceramicknife", BASE_MELEE_DIST * 1.0);  // Ceramic Knife
        weaponDistances.put("obsidiandagger", BASE_MELEE_DIST * 1.0);// Obsidian Dagger
        weaponDistances.put("throwingaxe", BASE_MELEE_DIST * 1.0);   // Tinker's Throwing Axe
        // Range 1.2 weapons (~16.2 world units)
        weaponDistances.put("hirdsword", BASE_MELEE_DIST * 1.2);     // Hirdsman's Sword
        weaponDistances.put("bronzesword", BASE_MELEE_DIST * 1.2);   // Bronze Sword
        weaponDistances.put("fyrdsword", BASE_MELEE_DIST * 1.2);     // Fyrdsman's Sword
        weaponDistances.put("woodsmansaxe", BASE_MELEE_DIST * 1.2);  // Woodsman's Axe
        weaponDistances.put("pickaxe", BASE_MELEE_DIST * 1.2);       // Pickaxe
        // Range 1.4 weapons (~18.9 world units)
        weaponDistances.put("b12axe", BASE_MELEE_DIST * 1.4);        // Battleaxe of the 12th Bay
        // Range 1.5 weapons (~20.3 world units)
        weaponDistances.put("cutblade", BASE_MELEE_DIST * 1.5);      // Cutblade
        // Range 1.6 weapons (~21.6 world units)
        weaponDistances.put("boarspear", BASE_MELEE_DIST * 1.6);     // Boar Spear
        weaponDistances.put("giantneedle", BASE_MELEE_DIST * 1.6);   // Giant Needle
        // Blunt (estimated — not on wiki range table)
        weaponDistances.put("sledgehammer", BASE_MELEE_DIST * 1.3);  // Sledgehammer (estimated)
        weaponDistances.put("scythe", BASE_MELEE_DIST * 1.4);        // Scythe (estimated)
        // Ranged weapons (estimated — wiki doesn't list bow/sling ranges)
        weaponDistances.put("huntersbow", 50.0);
        weaponDistances.put("rangersbow", 55.0);
        weaponDistances.put("sling", 40.0);
    }

    private final Label currentDistanceLabel;
    private TextEntry distanceEntry;

    private String value;
    private boolean autoRespace;
    private boolean autoWeaponDetect;
    private boolean autoReattack;
    private long lastRespaceTime;

    public void setValue(String value) {
        this.value = value;
    }

    public CombatDistanceTool(GameUI gui) {
        super(gui, new Coord(240, 120), "Combat Distancing Tool", true);
        this.value = "";
        this.autoRespace = Utils.getprefb("combatDist_autoRespace", false);
        this.autoWeaponDetect = Utils.getprefb("combatDist_autoWeapon", false);
        this.autoReattack = Utils.getprefb("combatDist_autoReattack", false);
        checkHP = false;
        checkEnergy = false;
        checkStamina = false;
        checkInventory = false;

        Widget prev;

        prev = add(new Label("Set Distance:"), 0, 6);

        distanceEntry = new TextEntry(UI.scale(80), value) {
            @Override
            protected void changed() {
                setValue(this.buf.line());
            }
        };
        prev = add(distanceEntry, prev.pos("ur").adds(2, 0));

        prev = add(new Button(UI.scale(40), "Go") {
            @Override
            public void click() {
                moveToDistance();
            }
        }, prev.pos("ur").adds(4, -2));

        prev = add(new Button(UI.scale(50), "Auto") {
            @Override
            public void click() {
                tryToAutoDistance();
            }
        }, prev.pos("bl").adds(0, 6));

        int y = UI.scale(40);
        add(new CheckBox("Auto-respace") {{ a = autoRespace; }
            public void set(boolean val) { autoRespace = val; a = val; Utils.setprefb("combatDist_autoRespace", val); }
        }, UI.scale(0, y));
        y += UI.scale(18);
        add(new CheckBox("Auto-detect weapon") {{ a = autoWeaponDetect; }
            public void set(boolean val) { autoWeaponDetect = val; a = val; Utils.setprefb("combatDist_autoWeapon", val); }
        }, UI.scale(0, y));
        y += UI.scale(18);
        add(new CheckBox("Auto-re-attack peaced") {{ a = autoReattack; }
            public void set(boolean val) { autoReattack = val; a = val; Utils.setprefb("combatDist_autoReattack", val); }
        }, UI.scale(0, y));
        y += UI.scale(20);

        currentDistanceLabel = new Label("Current dist: No target");
        add(currentDistanceLabel, UI.scale(new Coord(0, y)));
        pack();
    }

    @Override
    protected String windowPrefKey() {
        return "wndc-combatDistanceToolWindow";
    }

    @Override
    protected void onCleanup() {
        gui.combatDistanceTool = null;
    }

    /** Skip idlePlayer — we're in combat. */
    @Override
    public void stop() {
        stop = true;
        try {
            if (gui.map != null && gui.map.pfthread != null) {
                gui.map.pfthread.interrupt();
            }
        } catch (Exception ignored) {}
        if (botThread != null) {
            botThread.interrupt();
            botThread = null;
        }
    }

    @Override
    public void run() {
        try {
            while (!stop) {
                try {
                    Fightview fv = gui.fv;
                    if (fv != null && fv.current != null) {
                        double dist = getDistance(fv.current.gobid);
                        if (dist < 0) {
                            currentDistanceLabel.settext("No target");
                        } else {
                            DecimalFormat df = new DecimalFormat("#.##");
                            String result = df.format(dist);
                            currentDistanceLabel.settext("Current dist: " + result + " units.");
                        }

                        // Auto-detect weapon distance
                        if (autoWeaponDetect) {
                            double weaponDist = detectWeaponDistance();
                            if (weaponDist > 0) {
                                value = String.valueOf(weaponDist);
                                distanceEntry.settext(value);
                            }
                        }

                        // Auto-respace: continuously maintain set distance
                        if (autoRespace && System.currentTimeMillis() - lastRespaceTime > 600) {
                            try {
                                double targetDist = Double.parseDouble(value);
                                if (dist > 0 && Math.abs(dist - targetDist) > 3.0) {
                                    moveToDistance(targetDist);
                                    lastRespaceTime = System.currentTimeMillis();
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    } else {
                        currentDistanceLabel.settext("Current dist: No target");

                        // Auto-re-attack: if we had targets that were peaced, re-engage
                        if (autoReattack && fv != null) {
                            for (Fightview.Relation rel : fv.lsrel) {
                                if (rel.gobid != 0 && fv.current == null) {
                                    Gob gob = gui.map.glob.oc.getgob(rel.gobid);
                                    if (gob != null) {
                                        gui.fv.wdgmsg("bump", (int) rel.gobid);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}

                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private double detectWeaponDistance() {
        try {
            Equipory equip = gui.getequipory();
            if (equip == null) return -1;
            WItem leftHand = equip.slots[6];
            if (leftHand == null) return -1;
            String resName = leftHand.item.res.get().name;
            // Match by partial name (resource path ends with weapon name)
            for (Map.Entry<String, Double> entry : weaponDistances.entrySet()) {
                if (resName.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private void tryToAutoDistance() {
        if (gui != null && gui.map != null && gui.map.player() != null && gui.fv != null && gui.fv.current != null) {
            Double value = -1.0;

            double addedValue = 0.0;

            Gob player = ui.gui.map.player();
            if (player != null && player.occupiedGobID != null) {
                Gob vehicle = ui.sess.glob.oc.getgob(player.occupiedGobID);
                if (vehicle != null && vehicle.getres() != null) {
                    addedValue = vehicleDistance.getOrDefault(vehicle.getres().name, 0.0);
                }
            }
            Gob enemy = getEnemy();
            if(enemy != null && enemy.getres() != null){
                value = animalDistances.get(enemy.getres().name);
            }
            if(value != null && value > 0){
                moveToDistance(value+addedValue);
            }

        }
    }

    private void moveToDistance() {
        try {
            double distance = Double.parseDouble(value);
            Gob enemy = getEnemy();
            if (enemy != null && gui.map.player() != null) {
                double angle = enemy.rc.angle(gui.map.player().rc);
                gui.map.wdgmsg("click", Coord.z, getNewCoord(enemy, distance, angle).floor(posres), 1, 0);
            } else {
                gui.msg("No visible target.", Color.WHITE);
            }
            setfocus(ui.gui.portrait); // ND: do this to defocus the text entry box after you click on "Go"
        } catch (NumberFormatException e) {
            gui.errorsilent("Wrong distance format. Use ##.###");
        }
    }

    private void moveToDistance(double distance) {
        try {
            Gob enemy = getEnemy();
            if (enemy != null && gui.map.player() != null) {
                double angle = enemy.rc.angle(gui.map.player().rc);
                gui.map.wdgmsg("click", Coord.z, getNewCoord(enemy, distance, angle).floor(posres), 1, 0);
            } else {
                gui.msg("No visible target.", Color.WHITE);
            }
            setfocus(ui.gui.portrait); // ND: do this to defocus the text entry box after you click on "Go"
        } catch (NumberFormatException e) {
            gui.errorsilent("Wrong distance format. Use ##.###");
        }
    }

    private Coord2d getNewCoord(Gob enemy, double distance, double angle) {
        return new Coord2d(enemy.rc.x + distance * Math.cos(angle), enemy.rc.y + distance * Math.sin(angle));
    }

    private Gob getEnemy() {
        if (gui.fv.current != null) {
            long id = gui.fv.current.gobid;
            synchronized (gui.map.glob.oc) {
                for (Gob gob : gui.map.glob.oc) {
                    if (gob.id == id) {
                        return gob;
                    }
                }
            }
        }
        return null;
    }

    private double getDistance(long gobId) {
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                if (gob.id == gobId && gui.map.player() != null) {
                    return gob.rc.dist(gui.map.player().rc);
                }
            }
        }
        return -1;
    }
}
