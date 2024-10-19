/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.util.*;
import java.util.function.*;
import java.io.*;
import java.nio.file.*;
import java.net.URI;
import java.net.URLConnection;
import java.io.PrintStream;

public class Config {
    public static final Properties jarprops = getjarprops();
    public static final String confid = "Hurricane";
    public static final Variable<Boolean> par = Variable.def(() -> true);
    public final Properties localprops = getlocalprops();

    private static Config global = null;
    public static Config get() {
	if(global != null)
	    return(global);
	synchronized(Config.class) {
	    if(global == null)
		global = new Config();
	    return(global);
	}
    }

    private static Properties getjarprops() {
	Properties ret = new Properties();
	try(InputStream fp = Config.class.getResourceAsStream("boot-props")) {
	    if(fp != null)
		ret.load(fp);
	} catch(Exception exc) {
	    /* XXX? Catch all exceptions? It just seems dumb to
	     * potentially crash here for unforeseen reasons. */
	    new Warning(exc, "unexpected error occurred when loading local properties").issue();
	}
	return(ret);
    }

    private static Properties getlocalprops() {
	Properties ret = new Properties();
	try {
	    Path jar = Utils.srcpath(Config.class);
	    if(jar != null) {
		try(InputStream fp = Files.newInputStream(jar.resolveSibling("haven-config.properties"))) {
		    ret.load(fp);
		} catch(NoSuchFileException exc) {
		    /* That's quite alright. */
		}
	    }
	} catch(Exception exc) {
	    new Warning(exc, "unexpected error occurred when loading neighboring properties").issue();
	}
	return(ret);
    }

    public String getprop(String name, String def) {
	String ret;
	if((ret = jarprops.getProperty(name)) != null)
	    return(ret);
	if((ret = localprops.getProperty(name)) != null)
	    return(ret);
	return(Utils.getprop(name, def));
    }

    public static final Path parsepath(String p) {
	if((p == null) || p.equals(""))
	    return(null);
	return(Utils.path(p));
    }

    public static final URI parseuri(String url) {
	if((url == null) || url.equals(""))
	    return(null);
	return(Utils.uri(url));
    }

    public static void parsesvcaddr(String spec, Consumer<String> host, Consumer<Integer> port) {
	if((spec.length() > 0) && (spec.charAt(0) == '[')) {
	    int p = spec.indexOf(']');
	    if(p > 0) {
		String hspec = spec.substring(1, p);
		if(spec.length() == p + 1) {
		    host.accept(hspec);
		    return;
		} else if((spec.length() > p + 1) && (spec.charAt(p + 1) == ':')) {
		    host.accept(hspec);
		    port.accept(Integer.parseInt(spec.substring(p + 2)));
		    return;
		}
	    }
	}
	int p = spec.indexOf(':');
	if(p >= 0) {
	    host.accept(spec.substring(0, p));
	    port.accept(Integer.parseInt(spec.substring(p + 1)));
	    return;
	} else {
	    host.accept(spec);
	    return;
	}
    }

    public static class Variable<T> {
	public final Function<Config, T> init;
	private boolean inited = false;
	private T val;

	private Variable(Function<Config, T> init) {
	    this.init = init;
	}

	public T get() {
	    if(!inited) {
		synchronized(this) {
		    if(!inited) {
			val = init.apply(Config.get());
			inited = true;
		    }
		}
	    }
	    return(val);
	}

	public void set(T val) {
	    synchronized(this) {
		inited = true;
		this.val = val;
	    }
	}

	public static <V> Variable<V> def(Supplier<V> defval) {
	    return(new Variable<>(cfg -> defval.get()));
	}

	public static <V> Variable<V> prop(String name, Function<String, V> parse, Supplier<V> defval) {
	    return(new Variable<>(cfg -> {
			String pv = cfg.getprop(name, null);
			return((pv == null) ? defval.get() : parse.apply(pv));
	    }));
	}

	public static Variable<String> prop(String name, String defval) {
	    return(prop(name, Function.identity(), () -> defval));
	}
	public static Variable<Integer> propi(String name, int defval) {
	    return(prop(name, Integer::parseInt, () -> defval));
	}
	public static Variable<Boolean> propb(String name, boolean defval) {
	    return(prop(name, Utils::parsebool, () -> defval));
	}
	public static Variable<Double> propf(String name, Double defval) {
	    return(prop(name, Double::parseDouble, () -> defval));
	}
	public static Variable<byte[]> propb(String name, byte[] defval) {
	    return(prop(name, Utils::hex2byte, () -> defval));
	}
	public static Variable<URI> propu(String name, URI defval) {
	    return(prop(name, Config::parseuri, () -> defval));
	}
	public static Variable<URI> propu(String name, String defval) {
	    return(propu(name, parseuri(defval)));
	}
	public static Variable<Path> propp(String name, Path defval) {
	    return(prop(name, Config::parsepath, () -> defval));
	}
	public static Variable<Path> propp(String name, String defval) {
	    return(propp(name, parsepath(defval)));
	}
    }

    public static class Services {
	public static final Variable<URI> directory = Config.Variable.propu("haven.svcdir", "");
	public final URI rel;
	public final Properties props;

	public Services(URI rel, Properties props) {
	    this.rel = rel;
	    this.props = props;
	}

	private static Services fetch(URI uri) {
	    Properties props = new Properties();
	    if(uri != null) {
		Object[] data;
		try {
		    try(InputStream fp = Http.fetch(uri.toURL())) {
			data = new StreamMessage(fp).list();
		    }
		} catch(IOException exc) {
		    throw(new RuntimeException(exc));
		}
		for(Object d : data) {
		    Object[] p = (Object[])d;
		    props.put(p[0], p[1]);
		}
	    }
	    return(new Services(uri, props));
	}

	private static Services global = null;
	public static Services get() {
	    if(global != null)
		return(global);
	    synchronized(Services.class) {
		if(global == null)
		    global = fetch(directory.get());
		return(global);
	    }
	}

	public URI geturi(String name) {
	    String val = props.getProperty(name);
	    if(val == null)
		return(null);
	    return(rel.resolve(parseuri(val)));
	}

	public static Variable<URI> var(String name, String defval) {
	    URI def = parseuri(defval);
	    return new Variable<URI>(cfg -> {
		    String pv = cfg.getprop("haven." + name, null);
		    if(pv != null)
			return(parseuri(pv));
		    return(Services.get().geturi(name));
	    });
	}
    }

    private static void usage(PrintStream out) {
	out.println("usage: haven.jar [OPTIONS] [SERVER[:PORT]]");
	out.println("Options include:");
	out.println("  -h                 Display this help");
	out.println("  -d                 Display debug text");
	out.println("  -P                 Enable profiling");
	out.println("  -G                 Enable GPU profiling");
	out.println("  -f                 Fullscreen mode");
	out.println("  -U URL             Use specified external resource URL");
	out.println("  -r DIR             Use specified resource directory (or HAVEN_RESDIR)");
	out.println("  -A AUTHSERV[:PORT] Use specified authentication server");
	out.println("  -u USER            Authenticate as USER (together with -C)");
	out.println("  -C HEXCOOKIE       Authenticate with specified hex-encoded cookie");
	out.println("  -p PREFSPEC        Use alternate preference prefix");
    }

    public static void cmdline(String[] args) {
	PosixArgs opt = PosixArgs.getopt(args, "hdPGfU:r:A:u:C:p:");
	if(opt == null) {
	    usage(System.err);
	    System.exit(1);
	}
	for(char c : opt.parsed()) {
	    switch(c) {
	    case 'h':
		usage(System.out);
		System.exit(0);
		break;
	    case 'd':
		UIPanel.dbtext.set(true);
		break;
	    case 'P':
		UIPanel.profile.set(true);
		break;
	    case 'G':
		UIPanel.profilegpu.set(true);
		break;
	    case 'f':
		MainFrame.initfullscreen.set(true);
		break;
	    case 'r':
		Resource.resdir.set(Utils.path(opt.arg));
		break;
	    case 'A':
		parsesvcaddr(opt.arg, Bootstrap.authserv::set, Bootstrap.authport::set);
		break;
	    case 'U':
		try {
		    Resource.resurl.set(Utils.uri(opt.arg));
		} catch(IllegalArgumentException e) {
		    System.err.println(e);
		    System.exit(1);
		}
		break;
	    case 'u':
		Bootstrap.authuser.set(opt.arg);
		break;
	    case 'C':
		Bootstrap.authck.set(Utils.hex2byte(opt.arg));
		break;
	    case 'p':
		Utils.prefspec.set(opt.arg);
		break;
	    }
	}
	if(opt.rest.length > 0)
	    parsesvcaddr(opt.rest[0], Bootstrap.defserv::set, Bootstrap.mainport::set);
	if(opt.rest.length > 1)
	    Bootstrap.servargs.set(Utils.splice(opt.rest, 1));
    }

    static {
	Console.setscmd("par", new Console.Command() {
		public void run(Console cons, String[] args) {
		    par.set(Utils.parsebool(args[1]));
		}
	    });
    }

	public static final LinkedHashMap<String, String> properKeyNames = new LinkedHashMap<String, String>(){{
		put("None", " ");
		put("Back Quote", "`");
		put("Equals", "=");
		put("Minus", "-");
		put("Open Bracket", "[");
		put("Close Bracket", "]");
		put("Back Slash", "\\");
		put("Semicolon", ";");
		put("Quote", "'");
		put("Period", ".");
		put("Comma", ",");
		put("Slash", "/");
		put("Up", "↑");
		put("Down", "↓");
		put("Left", "←");
		put("Right", "→");
	}};

	public static final HashMap<String, String> mandatoryAlwaysEnabledMapIcons = new LinkedHashMap<String, String>(){{
		// ND: Map Icons that should ALWAYS be enabled. Players should be forced to see these at all times.
		// The key is the icon name, the value is the error message sent when they try to disable the icon.
		put("Player", "You should ALWAYS see players on the map. I don't care what you have to say.");
		put("Cave Passage", "Let's keep Caves visible, yeah?");
		put("Swirling Vortex", "Vortexes can be dangerous. You don't want to miss them, right?");
		put("Boost Speed", "You need to see Speed Boosts at all times. Keep them enabled.");
		put("Burrow", "Burrows can hide things, like someone trying to ambush you. Keep them enabled.");
	}};

	public static final List<String> statsAndAttributesOrder = new ArrayList<String>(){{
		// ND: I ordered them backwards, in case there's some stupid stat I overlooked, like "Swimming".
		// When they're ordered like this, the overlooked stat should show up last, rather than first.
		add("Swimming");
		add("Lore");
		add("Survival");
		add("Farming");
		add("Cooking");
		add("Carpentry");
		add("Masonry");
		add("Smithing");
		add("Sewing");
		add("Stealth");
		add("Exploration");
		add("Marksmanship");
		add("Melee Combat");
		add("Unarmed Combat");
		add("Psyche");
		add("Will");
		add("Dexterity");
		add("Charisma");
		add("Perception");
		add("Constitution");
		add("Intelligence");
		add("Agility");
		add("Strength");
	}};

	public static final String[] critterResPaths = {
			"gfx/kritter/bayshrimp/bayshrimp",
			"gfx/kritter/bogturtle/bogturtle",
			"gfx/kritter/brimstonebutterfly/brimstonebutterfly",
			"gfx/kritter/cavecentipede/cavecentipede",
			"gfx/kritter/cavemoth/cavemoth",
			"gfx/kritter/chicken/chick",
			"gfx/kritter/chicken/chicken", // ND: This seems to be the model for wild chickens, both hens and roosters.
			"gfx/kritter/chicken/hen", // ND: This might be pointless?
			"gfx/kritter/chicken/rooster", // ND: This might be pointless?
			"gfx/kritter/crab/crab",
			"gfx/kritter/dragonfly/dragonfly",
			"gfx/kritter/earthworm/earthworm",
			"gfx/kritter/firefly/firefly",
			"gfx/kritter/forestlizard/forestlizard",
			"gfx/kritter/forestsnail/forestsnail",
			"gfx/kritter/frog/frog",
			"gfx/kritter/grasshopper/grasshopper",
			"gfx/kritter/hedgehog/hedgehog",
			"gfx/kritter/irrbloss/irrbloss",
			"gfx/kritter/jellyfish/jellyfish",
			"gfx/kritter/ladybug/ladybug",
			"gfx/kritter/lobster/lobster",
			"gfx/kritter/magpie/magpie",
			"gfx/kritter/mallard/mallard", // ND: I haven't checked yet, but I assume it could be the same case as with the chickens
			"gfx/kritter/mallard/mallard-f", // ND: This might be pointless?
			"gfx/kritter/mallard/mallard-m", // ND: This might be pointless?
			"gfx/kritter/mole/mole",
			"gfx/kritter/monarchbutterfly/monarchbutterfly",
			"gfx/kritter/moonmoth/moonmoth",
			"gfx/kritter/opiumdragon/opiumdragon",
			"gfx/kritter/ptarmigan/ptarmigan",
			"gfx/kritter/quail/quail",
			"gfx/kritter/rat/rat",
			"gfx/kritter/rockdove/rockdove",
			"gfx/kritter/sandflea/sandflea",
			"gfx/kritter/seagull/seagull",
			"gfx/kritter/silkmoth/silkmoth",
			"gfx/kritter/springbumblebee/springbumblebee",
			"gfx/kritter/squirrel/squirrel",
			"gfx/kritter/stagbeetle/stagbeetle",
			"gfx/kritter/stalagoomba/stalagoomba",
			"gfx/kritter/toad/toad",
			"gfx/kritter/waterstrider/waterstrider",
			"gfx/kritter/woodgrouse/woodgrouse-f", // ND: Only female can be chased, males will fight you
			"gfx/kritter/woodworm/woodworm",
			"gfx/kritter/whirlingsnowflake/whirlingsnowflake",
			"gfx/kritter/bullfinch/bullfinch",

			"gfx/terobjs/items/grub", // ND: lmao
			"gfx/terobjs/items/hoppedcow",
			"gfx/terobjs/items/mandrakespirited",
	};

	public static final String[] beastResPaths = {
			"gfx/kritter/bear/bear",
			"gfx/kritter/lynx/lynx",
			"gfx/kritter/walrus/walrus",
			"gfx/kritter/mammoth/mammoth",
			"gfx/kritter/troll/troll",
			"gfx/kritter/spermwhale/spermwhale",
			"gfx/kritter/orca/orca",
			"gfx/kritter/moose/moose",
			"gfx/kritter/wolf/wolf",
			"gfx/kritter/bat/bat",
			"gfx/kritter/goldeneagle/goldeneagle",
			"gfx/kritter/eagleowl/eagleowl",
			"gfx/kritter/caveangler/caveangler",
			"gfx/kritter/boar/boar",
			"gfx/kritter/badger/badger",
			"gfx/kritter/wolverine/wolverine",
			"gfx/kritter/boreworm/boreworm",
			"gfx/kritter/ooze/greenooze",
			"gfx/kritter/adder/adder",
			"gfx/kritter/rat/caverat",
			"gfx/kritter/goat/wildgoat",
			"gfx/kritter/cavelouse/cavelouse"
	};

	public static final String[] housesResPaths = {
			"gfx/terobjs/arch/logcabin",
			"gfx/terobjs/arch/timberhouse",
			"gfx/terobjs/arch/stonestead",
			"gfx/terobjs/arch/stonemansion",
			"gfx/terobjs/arch/greathall",
			"gfx/terobjs/arch/stonetower",
			"gfx/terobjs/arch/windmill",
			"gfx/terobjs/arch/primitivetent",
			"gfx/terobjs/arch/greenhouse",
	};

	public static final String[] containersResPaths = {
			// ND: Each container might have different peekrbufs for each state. This needs to be checked for each new container, in each state (Empty & Closed || Empty & Open, Full & Closed || Full & Open).
			"gfx/terobjs/cupboard",
			"gfx/terobjs/chest",
			"gfx/terobjs/crate",
			"gfx/terobjs/largechest",
			"gfx/terobjs/coffer",
			"gfx/terobjs/exquisitechest",
			//"gfx/terobjs/wbasket", // ND: This one only has open/closed peekrbufs, no other fullness indicators lmao
			"gfx/terobjs/birchbasket",
			"gfx/terobjs/metalcabinet",
			"gfx/terobjs/stonecasket",
			"gfx/terobjs/bonechest",
			"gfx/terobjs/leatherbasket",
			"gfx/terobjs/woodbox",
			"gfx/terobjs/linencrate",
	};

	public static final String[] workstationsResPaths = {
			"gfx/terobjs/ttub",
			"gfx/terobjs/dframe",
			"gfx/terobjs/cheeserack",
			"gfx/terobjs/gardenpot",
	};


	public final static Set<String> stoneItemBaseNames = new HashSet<String>(Arrays.asList(
			"gneiss",
			"basalt",
			"cinnabar",
			"sunstone",
			"dolomite",
			"feldspar",
			"flint",
			"granite",
			"hornblende",
			"limestone",
			"marble",
			"porphyry",
			"quartz",
			"sandstone",
			"schist",
			"blackcoal",
			"coal",
			"zincspar",
			"apatite",
			"sodalite",
			"fluorospar",
			"soapstone",
			"olivine",
			"gabbro",
			"alabaster",
			"microlite",
			"mica",
			"kyanite",
			"corund",
			"orthoclase",
			"breccia",
			"diabase",
			"arkose",
			"diorite",
			"slate",
			"jasper",
			"rhyolite",
			"pegmatite",
			"greenschist",
			"eclogite",
			"pumice",
			"serpentine",
			"chert",
			"graywacke"
	));

	public final static Set<String> oreItemBaseNames = new HashSet<String>(Arrays.asList(
			"cassiterite",
			"chalcopyrite",
			"malachite",
			"ilmenite",
			"limonite",
			"hematite",
			"magnetite",
			"peacockore",
			"leadglance",
			"cuprite"
	));

	public final static Set<String> preciousOreItemBaseNames = new HashSet<String>(Arrays.asList(
			"galena",
			"argentite",
			"hornsilver",
			"petzite",
			"sylvanite",
			"nagyagite"
	));

	public final static Set<String> minedCuriosItemBaseNames = new HashSet<String>(Arrays.asList(
			"catgold",
			"petrifiedshell",
			"strangecrystal"
	));



}
