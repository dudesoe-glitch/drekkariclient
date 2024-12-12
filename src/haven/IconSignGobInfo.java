package haven;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class IconSignGobInfo extends GobInfo {
    private static final Color COL = new Color(252, 235, 255, 255);
    private static final Color BG = new Color(0, 0, 0, 84);
    private static final Map<Pair<Color, String>, Text.Line> TEXT_CACHE = new HashMap<>();
    private String contents = null;
	Tex signInfoTex;

    protected IconSignGobInfo(Gob owner) {
	super(owner);
	center = new Pair<>(0.5, 1.0);
    }
    
    @Override
    protected boolean enabled() {
		return OptWnd.showIconSignTextCheckBox.a;
    }

    @Override
    protected Tex render() {
	if(gob == null || gob.getres() == null) {return null;}
	up(6);
	BufferedImage content = content();
	if(content == null) {
		signInfoTex = null;
		return null;
	}
	if (signInfoTex != null)
		return signInfoTex;
	return signInfoTex = new TexI(ItemInfo.catimgsh(3, 0, BG, content));
    }

    private BufferedImage content() {
	this.contents = null;
	String resName = gob.getres().name;
	if(resName == null) {return null;}
	Optional<String> contents = Optional.empty();
	
	if(resName.startsWith("gfx/terobjs/iconsign")) {
		Message sdt = gob.sdtm();
		if(!sdt.eom()) {
			int resid = sdt.uint16();
			if((resid & 0x8000) != 0) {
				resid &= ~0x8000;
			}

			Session session = gob.context(Session.class);
			Indir<Resource> cres = session.getres2(resid);
			if(cres != null) {
				try {
					contents = Optional.of(cres.get().basename());
				} catch (Exception ignored){}
			}
		}
	}
	
	if(contents.isPresent()) {
	    this.contents = contents.get();
	    String text = this.contents;
		if (!text.isEmpty()) {
			text = text.substring(0, 1).toUpperCase() + text.substring(1);
		}
		text = removePrefix(text);

        return PUtils.strokeImg(text(text, COL).img);
	}
	return null;
    }
    
    private static Text.Line text(String text, Color col) {
	Pair<Color, String> key = new Pair<>(col, text);
	if(TEXT_CACHE.containsKey(key)) {
	    return TEXT_CACHE.get(key);
	} else {
	    Text.Line line = Text.std.renderstroked(text, col, Color.black);
	    TEXT_CACHE.put(key, line);
	    return line;
	}
    }

    @Override
    public String toString() {
	Resource res = gob.getres();
	return String.format("GobInfo<%s>", res != null ? res.name : "<loading>");
    }


	public static String removePrefix(String input) {
		if (input != null) {
			// Convert the input string to lowercase
			input = input.toLowerCase();

			// Define a list of prefixes to check
			String[] prefixes = {"wurst-", "wblock-", "board-", "seed-"};

			// Iterate over the prefixes and remove any that match the start of the string
			for (String prefix : prefixes) {
				if (input.startsWith(prefix)) {
					input = input.substring(prefix.length());
					break;  // Once a prefix is removed, no need to check further
				}
			}

			// Capitalize the first letter of the remaining string
			if (input.length() > 0) {
				input = input.substring(0, 1).toUpperCase() + input.substring(1);
			}
		}

		// Return the modified string, or the original if no modifications were made
		return input;
	}



}