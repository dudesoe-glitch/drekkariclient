package haven;

import org.json.JSONObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemAutoDrop {
    private static final String PREF_KEY = "itemAutoDropConfig";
    private static final Map<String, Integer> itemThresholds = new ConcurrentHashMap<>();
    private static boolean enabled = false;

    static {
        load();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean val) {
        enabled = val;
        Utils.setprefb("itemAutoDropEnabled", val);
    }

    public static Map<String, Integer> getItems() {
        return itemThresholds;
    }

    public static boolean shouldDrop(String baseName, double quality) {
        if (!enabled || baseName == null)
            return false;
        Integer threshold = itemThresholds.get(baseName);
        if (threshold == null)
            return false;
        return quality > 0.1 && threshold > quality;
    }

    public static void addItem(String baseName, int threshold) {
        itemThresholds.put(baseName, threshold);
        save();
    }

    public static void removeItem(String baseName) {
        itemThresholds.remove(baseName);
        save();
    }

    public static void updateThreshold(String baseName, int threshold) {
        if (itemThresholds.containsKey(baseName)) {
            itemThresholds.put(baseName, threshold);
            save();
        }
    }

    public static void save() {
        try {
            JSONObject json = new JSONObject();
            for (Map.Entry<String, Integer> entry : itemThresholds.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
            Utils.setpref(PREF_KEY, json.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        try {
            enabled = Utils.getprefb("itemAutoDropEnabled", false);
            String jsonStr = Utils.getpref(PREF_KEY, null);
            if (jsonStr != null && !jsonStr.isEmpty()) {
                JSONObject json = new JSONObject(jsonStr);
                for (String key : json.keySet()) {
                    itemThresholds.put(key, json.getInt(key));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
