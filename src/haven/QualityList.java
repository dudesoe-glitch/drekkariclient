package haven;

import haven.res.ui.tt.q.qbuff.QBuff;
import haven.res.ui.tt.q.quality.Quality;

import java.util.*;

/**
 * Multi-type quality aggregation for items.
 * Currently H&H sends a single "Quality" type per item, but this class
 * supports future expansion to multiple quality types (e.g., Armor, FEP, Curiosity).
 *
 * Provides aggregation methods (Mean, Max, Min, Average) over all quality types.
 * Ported from EnderWiggin's hafen-client design.
 */
public class QualityList {

    /** Aggregation mode for reducing multiple qualities to a single value. */
    public enum SingleType {
        /** Root-mean-square: sqrt(sum(q^2) / n) */
        Mean {
            @Override
            public double aggregate(List<QualityEntry> qualities) {
                if (qualities.isEmpty()) return 0;
                double sumSq = 0;
                for (QualityEntry q : qualities) sumSq += q.value * q.value;
                return Math.sqrt(sumSq / qualities.size());
            }
        },
        /** Arithmetic average: sum(q) / n */
        Average {
            @Override
            public double aggregate(List<QualityEntry> qualities) {
                if (qualities.isEmpty()) return 0;
                double sum = 0;
                for (QualityEntry q : qualities) sum += q.value;
                return sum / qualities.size();
            }
        },
        /** Minimum value across all types */
        Min {
            @Override
            public double aggregate(List<QualityEntry> qualities) {
                double min = Double.MAX_VALUE;
                for (QualityEntry q : qualities) {
                    if (q.value < min) min = q.value;
                }
                return min == Double.MAX_VALUE ? 0 : min;
            }
        },
        /** Maximum value across all types */
        Max {
            @Override
            public double aggregate(List<QualityEntry> qualities) {
                double max = 0;
                for (QualityEntry q : qualities) {
                    if (q.value > max) max = q.value;
                }
                return max;
            }
        };

        public abstract double aggregate(List<QualityEntry> qualities);
    }

    /** A single quality entry with type name, value, and pre-computed multiplier. */
    public static class QualityEntry {
        public final String type;
        public final double value;
        /** Pre-computed effectiveness multiplier: sqrt(q/10). */
        public final double multiplier;

        public QualityEntry(String type, double value) {
            this.type = type;
            this.value = value;
            this.multiplier = Math.sqrt(value / 10.0);
        }
    }

    private final List<QualityEntry> entries;
    private final Map<SingleType, Double> cache = new EnumMap<>(SingleType.class);

    private QualityList(List<QualityEntry> entries) {
        this.entries = Collections.unmodifiableList(entries);
    }

    /** Get all quality entries. */
    public List<QualityEntry> entries() {
        return entries;
    }

    /** Number of quality types on this item. */
    public int size() {
        return entries.size();
    }

    /** True if no quality data. */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** Get the single primary quality value (first entry, typically "Quality"). */
    public double single() {
        return entries.isEmpty() ? 0 : entries.get(0).value;
    }

    /** Get the primary quality multiplier. */
    public double multiplier() {
        return entries.isEmpty() ? 0 : entries.get(0).multiplier;
    }

    /** Get aggregated quality value using the specified method. */
    public double get(SingleType type) {
        return cache.computeIfAbsent(type, t -> t.aggregate(entries));
    }

    /** Find a quality entry by type name. */
    public QualityEntry byType(String typeName) {
        for (QualityEntry e : entries) {
            if (e.type.equals(typeName)) return e;
        }
        return null;
    }

    /**
     * Extract QualityList from an item's info list.
     * Handles the current H&H protocol where there's a single Quality instance.
     */
    public static QualityList make(List<ItemInfo> infos) {
        List<QualityEntry> entries = new ArrayList<>();
        for (ItemInfo info : infos) {
            if (info instanceof Quality) {
                Quality q = (Quality) info;
                entries.add(new QualityEntry(q.name, q.q));
            } else if (info instanceof QBuff) {
                QBuff q = (QBuff) info;
                entries.add(new QualityEntry(q.name, q.q));
            }
        }
        return new QualityList(entries);
    }

    /**
     * Extract QualityList from an item, checking contents first (for containers).
     * This mirrors GItem.getQBuff() logic but returns a richer QualityList.
     */
    public static QualityList fromItem(GItem item) {
        try {
            List<ItemInfo> infos = item.info();
            // Check contents first (e.g., water in a bucket shows water's quality)
            for (ItemInfo info : infos) {
                if (info instanceof ItemInfo.Contents) {
                    QualityList sub = make(((ItemInfo.Contents) info).sub);
                    if (!sub.isEmpty()) return sub;
                }
            }
            return make(infos);
        } catch (Loading l) {
            return EMPTY;
        }
    }

    /** Empty quality list singleton. */
    public static final QualityList EMPTY = new QualityList(Collections.emptyList());
}
