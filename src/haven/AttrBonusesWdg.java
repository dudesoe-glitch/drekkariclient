package haven;


import haven.res.ui.tt.attrmod.Attribute;
import haven.res.ui.tt.attrmod.resattr;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AttrBonusesWdg extends Widget implements ItemInfo.Owner {
    private static final Coord bonusc = new Coord(UI.scale(6), UI.scale(32));
    private static final ClassResolver<AttrBonusesWdg> ctxr = new ClassResolver<AttrBonusesWdg>()
            .add(Glob.class, wdg -> wdg.ui.sess.glob)
            .add(Session.class, wdg -> wdg.ui.sess);
    private final Scrollbar bar;

    private boolean needUpdate = false;
    private boolean needBuild = false;
    private boolean needRedraw = false;

    private WItem[] items;
    private Map<haven.res.ui.tt.attrmod.Entry, String> bonuses = new HashMap<>();
    private List<ItemInfo> info = null;
    private Tex tip = null;

    private CharWnd charWnd = null;

    public AttrBonusesWdg(int y) {
        super(new Coord(UI.scale(146), y - UI.scale(38)));
        add(new Label("Equipment bonuses:"), UI.scale(26), UI.scale(5));
        bar = adda(new Scrollbar(y - bonusc.y - UI.scale(38), 0, 0), sz.x, bonusc.y , 1, 0);
    }

    @Override
    public boolean mousewheel(MouseWheelEvent ev) {
        bar.ch(15 * ev.a);
        return true;
    }

    public void update(WItem[] items) {
        this.items = items;
        needUpdate = true;
    }

    @Override
    public void draw(GOut g) {
        if (needRedraw) {
            render();
        }

        if (tip != null) {
            Coord c = Coord.z;
            if (bar.visible) {
                c = c.sub(0, bar.val);
            }
            g.reclip(bonusc, sz).image(tip, c);
        }
        super.draw(g);
    }

    private void render() {
        try {
            if (info != null && !info.isEmpty()) {
                tip = new TexI(ItemInfo.longtip(info));
            } else {
                tip = null;
            }

            if (tip != null)
                bar.move(Coord.of(tip.sz().x + bar.sz.x + UI.scale(5), bar.c.y));
            int delta = tip != null ? tip.sz().y : 0;
            bar.visible = delta > bar.sz.y;
            bar.max = delta - bar.sz.y;
            bar.ch(0);

            needRedraw = false;
        } catch (Exception e) {
        }
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
        if (needUpdate) {
            doUpdate();
        }
        if (charWnd == null) {
            GameUI gui = ui.root.getchild(GameUI.class);
            if (gui != null) {
                charWnd = gui.chrwdg;
                if (charWnd != null) {
                    needBuild = true;
                }
            }
        }
        if (needBuild) {
            build();
        }
    }

    private void doUpdate() {
        try {
            bonuses = Arrays.stream(items)
                    .filter(Objects::nonNull)
                    .map(wItem -> wItem.item)
                    .distinct()
                    .map(GItem::info)
                    .map(ItemInfo::getBonuses)
                    .map(Map::entrySet)
                    .flatMap(Collection::stream)
                    .collect(
                            Collectors.toMap(
                                    Entry::getKey,
                                    Entry::getValue
                            )
                    );
            needUpdate = false;
            needBuild = true;
        } catch (Loading e) {
        }
    }

    static final Pattern pattern = Pattern.compile("\\{([+-]?\\d+)\\}");
    private void build() {
        try {
            Map<haven.res.ui.tt.attrmod.Entry, String> totalAttr = new HashMap<>();
            for (Map.Entry<haven.res.ui.tt.attrmod.Entry, String> entry : bonuses.entrySet()) {
                haven.res.ui.tt.attrmod.Entry key = entry.getKey();
                String value = entry.getValue();
                boolean exist = false;
                for (Map.Entry<haven.res.ui.tt.attrmod.Entry, String> entry2 : totalAttr.entrySet()) {
                    if (entry2.getKey().attr.name().equals(key.attr.name())) {
                        Matcher matcher = pattern.matcher(value);
                        Matcher matcher2 = pattern.matcher(entry2.getValue());
                        if (matcher.find() && matcher2.find()) {
                            int sum = Integer.parseInt(matcher.group(1)) + Integer.parseInt(matcher2.group(1));
                            entry2.setValue(String.format("$col[%s]{%s%d}", sum < 0 ? "235,96,96" : "96,235,96", sum < 0 ? "-" : "+", Math.abs(sum)));
                            exist = true;
                            break;
                        }
                    }
                }
                if (!exist) totalAttr.put(key, value);
            }
            ItemInfo compiled = make(totalAttr.entrySet()
                    .stream()
                    .sorted(this::BY_PRIORITY)
                    .collect(Collectors.toList())
            );
            info = compiled != null ? Collections.singletonList(compiled) : null;
            needBuild = false;
            needRedraw = true;
        } catch (Loading e) {
        }
    }

    private ItemInfo make(Collection<Entry<haven.res.ui.tt.attrmod.Entry, String>> mods) {
        if (mods.isEmpty()) {
            return null;
        }
        Resource res = Resource.remote().load("ui/tt/attrmod").get();
        ItemInfo.InfoFactory f = res.layer(Resource.CodeEntry.class).get(ItemInfo.InfoFactory.class);
        Object[] args = new Object[mods.size() * 2 + 1];
        int i = 1;
        for (Entry<haven.res.ui.tt.attrmod.Entry, String> entry : mods) {
            Matcher matcher = pattern.matcher(entry.getValue());
            if (entry.getKey().attr instanceof resattr && matcher.find()) {
                args[i] = ui.sess.getresid(((resattr) entry.getKey().attr).res);
                args[i + 1] = Integer.parseInt(matcher.group(1));
                i += 2;
            }
        }
        return f.build(this, new ItemInfo.Raw(args), args);
    }

    private int BY_PRIORITY(Map.Entry<haven.res.ui.tt.attrmod.Entry, String> o1, Map.Entry<haven.res.ui.tt.attrmod.Entry, String> o2) {
        String a1 =  o1.getKey().attr.name();
        String a2 =  o2.getKey().attr.name();
        return Integer.compare(Config.statsAndAttributesOrder.indexOf(a2), Config.statsAndAttributesOrder.indexOf(a1));
    }

    @Override
    public List<ItemInfo> info() {
        return info;
    }

    @Override
    public <T> T context(Class<T> cl) {
        return (ctxr.context(cl, this));
    }
}
