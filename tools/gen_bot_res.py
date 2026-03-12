#!/usr/bin/env python3
"""
Generate Haven & Hearth .res files for Hurricane bot menu entries.

Usage:
    python gen_bot_res.py                  # Generate all bot .res files
    python gen_bot_res.py --list           # List all bot definitions
    python gen_bot_res.py --parse FILE     # Parse and dump a .res file
    python gen_bot_res.py --only NAME      # Generate only one bot (by internal name)

The .res binary format (all integers are little-endian):
    1. Header: b"Haven Resource 1" (16 bytes)
    2. uint16: resource version
    3. Repeating layers, each:
       - Null-terminated string: layer type name
       - int32: layer data length
       - bytes[length]: layer data

Layer types used:
    "action" - Menu entry metadata (parent path, display name, ad entries)
    "image"  - Icon image (header + PNG data)
    "tooltip" - Hover text (raw UTF-8 string)
"""

import argparse
import hashlib
import io
import struct
import sys
import zlib
from pathlib import Path

# ---------------------------------------------------------------------------
# Paths
# ---------------------------------------------------------------------------
SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_DIR = SCRIPT_DIR.parent
BOTS_DIR = PROJECT_DIR / "res" / "customclient" / "menugrid" / "Bots"

# ---------------------------------------------------------------------------
# .res format constants
# ---------------------------------------------------------------------------
RES_SIG = b"Haven Resource 1"
RES_VERSION = 4
ACTION_PARENT = "customclient/menugrid/Bots"
ACTION_PARENT_VER = 4

# ---------------------------------------------------------------------------
# Bot definitions
# ---------------------------------------------------------------------------
# Each tuple: (filename, display_name, internal_name, tooltip, icon_rgb, icon_symbol)
#   icon_rgb:    (R, G, B) tuple for generated icon background
#   icon_symbol: 1-3 char label drawn on the icon
BOTS = [
    (
        "ButcherBot.res",
        "Butcher Bot",
        "ButcherBot",
        "Butcher Bot",
        (200, 50, 50),
        "BU",
    ),
    (
        "CellarDiggingBot.res",
        "Cellar Digging Bot",
        "CellarDiggingBot",
        "Cellar Digging Bot",
        (120, 100, 80),
        "CD",
    ),
    (
        "ClayDiggingBot.res",
        "Clay Digging Bot",
        "ClayDiggingBot",
        "Clay Digging Bot",
        (180, 130, 70),
        "CL",
    ),
    (
        "CleanupBot.res",
        "Cleanup Bot",
        "CleanupBot",
        "Cleanup Bot",
        (90, 160, 90),
        "CU",
    ),
    (
        "FarmingBot.res",
        "Farming Bot",
        "FarmingBot",
        "Farming Bot",
        (50, 180, 50),
        "FA",
    ),
    (
        "FishingBot.res",
        "Fishing Bot",
        "FishingBot",
        "Fishing Bot",
        (60, 120, 200),
        "FI",
    ),
    (
        "ForagingBot.res",
        "Foraging Bot",
        "ForagingBot",
        "Foraging Bot",
        (100, 200, 80),
        "FO",
    ),
    (
        "GrubGrubBot.res",
        "Grub-Grub Bot",
        "GrubGrubBot",
        "Grub-Grub Bot",
        (180, 160, 60),
        "GG",
    ),
    (
        "MiningBot.res",
        "Mining Bot",
        "MiningBot",
        "Mining Bot",
        (140, 140, 160),
        "MI",
    ),
    (
        "OceanScoutBot.res",
        "Ocean Scout Bot",
        "OceanScoutBot",
        "Ocean Scout Bot",
        (40, 100, 180),
        "OS",
    ),
    (
        "OreSmeltingBot.res",
        "Ore Smelting Bot",
        "OreSmeltingBot",
        "Ore Smelting Bot",
        (220, 140, 40),
        "SM",
    ),
    (
        "RoastingSpitBot.res",
        "Roasting Spit Bot",
        "RoastingSpitBot",
        "Roasting Spit Bot",
        (200, 100, 50),
        "RS",
    ),
    (
        "TarKilnEmptierBot.res",
        "Tar Kiln Emptier Bot",
        "TarKilnEmptierBot",
        "Tar Kiln Emptier Bot",
        (80, 80, 80),
        "TK",
    ),
]

# ---------------------------------------------------------------------------
# Low-level binary helpers (all little-endian, matching H&H Message class)
# ---------------------------------------------------------------------------

def write_cstring(s: str) -> bytes:
    """Encode a null-terminated ASCII string."""
    return s.encode("ascii") + b"\x00"


def write_uint16(v: int) -> bytes:
    """Encode a uint16 in little-endian."""
    return struct.pack("<H", v)


def write_int16(v: int) -> bytes:
    """Encode a signed int16 in little-endian."""
    return struct.pack("<h", v)


def write_int32(v: int) -> bytes:
    """Encode a signed int32 in little-endian."""
    return struct.pack("<i", v)


def write_uint8(v: int) -> bytes:
    """Encode a uint8."""
    return struct.pack("B", v)


def write_float32(v: float) -> bytes:
    """Encode a float32 in little-endian."""
    return struct.pack("<f", v)


def read_cstring(data: bytes, offset: int):
    """Read a null-terminated string, return (string, new_offset)."""
    end = data.index(b"\x00", offset)
    return data[offset:end].decode("ascii"), end + 1


# ---------------------------------------------------------------------------
# .res layer builders
# ---------------------------------------------------------------------------

def build_action_layer(
    display_name: str,
    internal_name: str,
    parent_path: str = ACTION_PARENT,
    parent_ver: int = ACTION_PARENT_VER,
    hotkey: int = 0,
) -> bytes:
    """
    Build the data payload for an "action" layer.

    The ad array follows the established pattern:
        ["@", "<parent_leaf>", "<internal_name>"]
    where parent_leaf is the last component of parent_path.
    """
    parent_leaf = parent_path.rsplit("/", 1)[-1]
    ad_entries = ["@", parent_leaf, internal_name]

    buf = io.BytesIO()
    buf.write(write_cstring(parent_path))
    buf.write(write_uint16(parent_ver))
    buf.write(write_cstring(display_name))
    buf.write(write_cstring(""))  # prerequisite skill (empty)
    buf.write(write_uint16(hotkey))
    buf.write(write_uint16(len(ad_entries)))
    for entry in ad_entries:
        buf.write(write_cstring(entry))
    return buf.getvalue()


def build_image_layer(png_data: bytes, scale: float = 4.0) -> bytes:
    """
    Build the data payload for an "image" layer.

    Image layer header format (old format, ver < 128):
        uint8  ver   = 0
        int8   z_hi  = 0    (z = z_hi * 256 + ver = 0)
        int16  subz  = 0
        uint8  fl    = 4    (bit 2 set = has metadata)
        int16  id    = -1
        int16  ox    = 0    (offset x)
        int16  oy    = 0    (offset y)
        -- metadata (because fl & 4) --
        string key   = "scale"
        uint8  len   = 4
        float32 val  = scale
        string key   = ""   (empty = end of metadata)
        -- PNG data follows --
    """
    buf = io.BytesIO()
    buf.write(write_uint8(0))       # ver
    buf.write(write_uint8(0))       # z_hi (int8)
    buf.write(write_int16(0))       # subz
    buf.write(write_uint8(4))       # fl (has metadata)
    buf.write(write_int16(-1))      # id
    buf.write(write_int16(0))       # offset x
    buf.write(write_int16(0))       # offset y
    # Metadata key-value pairs
    buf.write(write_cstring("scale"))
    buf.write(write_uint8(4))       # value length = 4 bytes
    buf.write(write_float32(scale))
    buf.write(write_cstring(""))    # empty key = end of metadata
    # PNG image data
    buf.write(png_data)
    return buf.getvalue()


def build_tooltip_layer(text: str) -> bytes:
    """Build the data payload for a "tooltip" layer (raw UTF-8)."""
    return text.encode("utf-8")


def build_pagina_layer(text: str) -> bytes:
    """Build the data payload for a "pagina" layer (raw UTF-8)."""
    return text.encode("utf-8")


# ---------------------------------------------------------------------------
# .res file assembly
# ---------------------------------------------------------------------------

def build_res_file(version: int, layers: list) -> bytes:
    """
    Assemble a complete .res file.

    Args:
        version: Resource version (uint16).
        layers: List of (type_name, layer_data) tuples.

    Returns:
        Complete .res file bytes.
    """
    buf = io.BytesIO()
    buf.write(RES_SIG)
    buf.write(write_uint16(version))
    for type_name, layer_data in layers:
        buf.write(write_cstring(type_name))
        buf.write(write_int32(len(layer_data)))
        buf.write(layer_data)
    return buf.getvalue()


# ---------------------------------------------------------------------------
# .res file parsing (for --parse mode)
# ---------------------------------------------------------------------------

def parse_res_file(filepath: Path) -> dict:
    """
    Parse a .res file into its components.

    Returns:
        dict with keys: version, layers (list of dicts with type, data, parsed).
    """
    data = filepath.read_bytes()
    sig = data[:16]
    if sig != RES_SIG:
        raise ValueError(f"Invalid signature: {sig!r}")
    version = struct.unpack_from("<H", data, 16)[0]

    layers = []
    offset = 18
    while offset < len(data):
        type_name, offset = read_cstring(data, offset)
        length = struct.unpack_from("<i", data, offset)[0]
        offset += 4
        layer_data = data[offset : offset + length]
        offset += length

        parsed = {}
        if type_name == "action":
            parsed = _parse_action(layer_data)
        elif type_name == "image":
            parsed = _parse_image(layer_data)
        elif type_name == "tooltip":
            parsed["text"] = layer_data.decode("utf-8", errors="replace")
        elif type_name == "pagina":
            parsed["text"] = layer_data.decode("utf-8", errors="replace")

        layers.append({"type": type_name, "length": length, "data": layer_data, "parsed": parsed})

    return {"version": version, "layers": layers}


def _parse_action(data: bytes) -> dict:
    """Parse an action layer's data payload."""
    off = 0
    parent, off = read_cstring(data, off)
    parent_ver = struct.unpack_from("<H", data, off)[0]
    off += 2
    display_name, off = read_cstring(data, off)
    prereq, off = read_cstring(data, off)
    hotkey = struct.unpack_from("<H", data, off)[0]
    off += 2
    ad_count = struct.unpack_from("<H", data, off)[0]
    off += 2
    ads = []
    for _ in range(ad_count):
        s, off = read_cstring(data, off)
        ads.append(s)
    return {
        "parent": parent,
        "parent_ver": parent_ver,
        "display_name": display_name,
        "prereq": prereq,
        "hotkey": hotkey,
        "ad": ads,
    }


def _parse_image(data: bytes) -> dict:
    """Parse an image layer's header (does not decode the PNG)."""
    off = 0
    ver = data[off]; off += 1
    if ver < 128:
        z_hi = struct.unpack_from("b", data, off)[0]; off += 1
        z = z_hi * 256 + ver
        subz = struct.unpack_from("<h", data, off)[0]; off += 2
        fl = data[off]; off += 1
        img_id = struct.unpack_from("<h", data, off)[0]; off += 2
        ox = struct.unpack_from("<h", data, off)[0]; off += 2
        oy = struct.unpack_from("<h", data, off)[0]; off += 2
        meta = {}
        if fl & 4:
            while True:
                key, off = read_cstring(data, off)
                if key == "":
                    break
                vlen = data[off]; off += 1
                if vlen & 0x80:
                    vlen = struct.unpack_from("<i", data, off)[0]; off += 4
                vdata = data[off : off + vlen]; off += vlen
                if key == "scale":
                    meta[key] = struct.unpack_from("<f", vdata, 0)[0]
                else:
                    meta[key] = vdata.hex()
        png_start = data.find(b"\x89PNG\r\n\x1a\n", off - 20)
        png_size = len(data) - png_start if png_start >= 0 else 0
        return {
            "ver": ver, "z": z, "subz": subz, "fl": fl, "id": img_id,
            "offset": (ox, oy), "meta": meta, "png_offset": png_start,
            "png_size": png_size,
        }
    return {"ver": ver, "note": "new format (ver >= 128), not fully parsed"}


# ---------------------------------------------------------------------------
# PNG generation (pure Python, no PIL/Pillow)
# ---------------------------------------------------------------------------
# Generates a 32x32 RGBA PNG icon with a colored rounded-rectangle shape
# and a 1-3 character label.

# Simple 5x7 pixel font for uppercase letters and digits (subset).
# Each character is a list of 7 rows, each row an int bitmask (5 bits wide).
_FONT_5X7 = {
    "A": [0b01110, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001],
    "B": [0b11110, 0b10001, 0b10001, 0b11110, 0b10001, 0b10001, 0b11110],
    "C": [0b01110, 0b10001, 0b10000, 0b10000, 0b10000, 0b10001, 0b01110],
    "D": [0b11110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b11110],
    "E": [0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b11111],
    "F": [0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b10000],
    "G": [0b01110, 0b10001, 0b10000, 0b10111, 0b10001, 0b10001, 0b01110],
    "H": [0b10001, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001],
    "I": [0b01110, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110],
    "J": [0b00111, 0b00010, 0b00010, 0b00010, 0b00010, 0b10010, 0b01100],
    "K": [0b10001, 0b10010, 0b10100, 0b11000, 0b10100, 0b10010, 0b10001],
    "L": [0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b11111],
    "M": [0b10001, 0b11011, 0b10101, 0b10101, 0b10001, 0b10001, 0b10001],
    "N": [0b10001, 0b11001, 0b10101, 0b10011, 0b10001, 0b10001, 0b10001],
    "O": [0b01110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110],
    "P": [0b11110, 0b10001, 0b10001, 0b11110, 0b10000, 0b10000, 0b10000],
    "Q": [0b01110, 0b10001, 0b10001, 0b10001, 0b10101, 0b10010, 0b01101],
    "R": [0b11110, 0b10001, 0b10001, 0b11110, 0b10100, 0b10010, 0b10001],
    "S": [0b01110, 0b10001, 0b10000, 0b01110, 0b00001, 0b10001, 0b01110],
    "T": [0b11111, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100],
    "U": [0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110],
    "V": [0b10001, 0b10001, 0b10001, 0b10001, 0b01010, 0b01010, 0b00100],
    "W": [0b10001, 0b10001, 0b10001, 0b10101, 0b10101, 0b11011, 0b10001],
    "X": [0b10001, 0b10001, 0b01010, 0b00100, 0b01010, 0b10001, 0b10001],
    "Y": [0b10001, 0b10001, 0b01010, 0b00100, 0b00100, 0b00100, 0b00100],
    "Z": [0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b10000, 0b11111],
    "-": [0b00000, 0b00000, 0b00000, 0b11111, 0b00000, 0b00000, 0b00000],
    " ": [0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00000],
}


def _render_text(text: str, scale: int = 1):
    """
    Render text into a 2D pixel grid using the 5x7 bitmap font.

    Returns:
        (width, height, pixels) where pixels[y][x] is True/False.
    """
    chars = [_FONT_5X7.get(c, _FONT_5X7[" "]) for c in text.upper()]
    char_w = 5 * scale
    spacing = 1 * scale
    total_w = len(chars) * char_w + (len(chars) - 1) * spacing
    total_h = 7 * scale

    pixels = [[False] * total_w for _ in range(total_h)]
    x_off = 0
    for glyph in chars:
        for gy, row_bits in enumerate(glyph):
            for gx in range(5):
                if row_bits & (1 << (4 - gx)):
                    for sy in range(scale):
                        for sx in range(scale):
                            py = gy * scale + sy
                            px = x_off + gx * scale + sx
                            if 0 <= px < total_w and 0 <= py < total_h:
                                pixels[py][px] = True
        x_off += char_w + spacing

    return total_w, total_h, pixels


def generate_icon_png(
    rgb: tuple,
    symbol: str,
    size: int = 32,
) -> bytes:
    """
    Generate a simple icon PNG (RGBA, given size).

    The icon is a colored rounded rectangle with a white text label centered on it.
    """
    r, g, b = rgb
    # Darken color for border
    br, bg, bb = max(0, r - 60), max(0, g - 60), max(0, b - 60)
    # Lighten color for inner highlight
    hr, hg, hb = min(255, r + 40), min(255, g + 40), min(255, b + 40)

    # Create pixel buffer (RGBA)
    img = [[(0, 0, 0, 0)] * size for _ in range(size)]

    # Draw rounded rectangle
    corner_r = 4
    for y in range(size):
        for x in range(size):
            # Check if inside rounded rectangle
            inside = True
            # Check corners
            for cy, cx in [(corner_r, corner_r),
                           (corner_r, size - 1 - corner_r),
                           (size - 1 - corner_r, corner_r),
                           (size - 1 - corner_r, size - 1 - corner_r)]:
                if ((y < corner_r or y > size - 1 - corner_r) and
                    (x < corner_r or x > size - 1 - corner_r)):
                    dy = y - cy
                    dx = x - cx
                    if dy * dy + dx * dx > corner_r * corner_r:
                        inside = False
                        break
            if not inside:
                continue

            # Border (2px)
            is_border = (x < 2 or x >= size - 2 or y < 2 or y >= size - 2)
            # Also check rounded border in corners
            if not is_border:
                for cy, cx in [(corner_r, corner_r),
                               (corner_r, size - 1 - corner_r),
                               (size - 1 - corner_r, corner_r),
                               (size - 1 - corner_r, size - 1 - corner_r)]:
                    if ((y <= corner_r or y >= size - 1 - corner_r) and
                        (x <= corner_r or x >= size - 1 - corner_r)):
                        dy = y - cy
                        dx = x - cx
                        dist_sq = dy * dy + dx * dx
                        inner_r = corner_r - 2
                        if dist_sq > inner_r * inner_r:
                            is_border = True
                            break

            if is_border:
                img[y][x] = (br, bg, bb, 255)
            elif y < size // 3:
                # Top highlight gradient
                t = y / (size // 3)
                cr = int(hr + (r - hr) * t)
                cg = int(hg + (g - hg) * t)
                cb = int(hb + (b - hb) * t)
                img[y][x] = (cr, cg, cb, 255)
            else:
                img[y][x] = (r, g, b, 255)

    # Render text label
    text_scale = 2
    tw, th, text_pixels = _render_text(symbol, scale=text_scale)
    tx_start = (size - tw) // 2
    ty_start = (size - th) // 2 + 1  # slight offset down for visual centering

    for ty in range(th):
        for tx in range(tw):
            if text_pixels[ty][tx]:
                px = tx_start + tx
                py = ty_start + ty
                if 0 <= px < size and 0 <= py < size:
                    # White text with slight dark shadow
                    if (px + 1 < size and py + 1 < size and
                        img[py][px][3] > 0):
                        # Shadow pixel
                        if px + 1 < size and py + 1 < size:
                            sx, sy = px + 1, py + 1
                            if img[sy][sx][3] > 0:
                                img[sy][sx] = (0, 0, 0, 100)
                    if img[py][px][3] > 0:
                        img[py][px] = (255, 255, 255, 255)

    return _encode_png(img, size, size)


def _encode_png(pixels, width: int, height: int) -> bytes:
    """
    Encode RGBA pixel data as a PNG file (standard library only).

    Args:
        pixels: 2D list of (R, G, B, A) tuples, pixels[y][x].
        width: Image width.
        height: Image height.

    Returns:
        PNG file bytes.
    """

    def _chunk(chunk_type: bytes, data: bytes) -> bytes:
        """Build a PNG chunk: length + type + data + CRC."""
        out = struct.pack(">I", len(data))
        out += chunk_type
        out += data
        crc = zlib.crc32(chunk_type + data) & 0xFFFFFFFF
        out += struct.pack(">I", crc)
        return out

    # PNG signature
    sig = b"\x89PNG\r\n\x1a\n"

    # IHDR: width, height, bit_depth=8, color_type=6(RGBA), compression=0, filter=0, interlace=0
    ihdr_data = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    ihdr = _chunk(b"IHDR", ihdr_data)

    # IDAT: deflated scanlines with filter byte 0 (None) per row
    raw = io.BytesIO()
    for y in range(height):
        raw.write(b"\x00")  # filter type: None
        for x in range(width):
            r, g, b, a = pixels[y][x]
            raw.write(bytes([r, g, b, a]))
    compressed = zlib.compress(raw.getvalue(), 9)
    idat = _chunk(b"IDAT", compressed)

    # IEND
    iend = _chunk(b"IEND", b"")

    return sig + ihdr + idat + iend


# ---------------------------------------------------------------------------
# Main generation logic
# ---------------------------------------------------------------------------

def generate_bot_res(
    display_name: str,
    internal_name: str,
    tooltip: str,
    icon_rgb: tuple,
    icon_symbol: str,
    icon_size: int = 32,
    icon_scale: float = 4.0,
) -> bytes:
    """
    Generate a complete .res file for a bot menu entry.

    Args:
        display_name: Human-readable bot name for the menu.
        internal_name: Java class name (used in ad entries).
        tooltip: Hover tooltip text.
        icon_rgb: (R, G, B) color for the generated icon.
        icon_symbol: 1-3 character label for the icon.
        icon_size: Icon dimensions in pixels (square).
        icon_scale: Scale factor stored in image metadata.

    Returns:
        Complete .res file bytes.
    """
    action_data = build_action_layer(display_name, internal_name)
    png_data = generate_icon_png(icon_rgb, icon_symbol, size=icon_size)
    image_data = build_image_layer(png_data, scale=icon_scale)
    tooltip_data = build_tooltip_layer(tooltip)

    layers = [
        ("action", action_data),
        ("image", image_data),
        ("tooltip", tooltip_data),
    ]
    return build_res_file(RES_VERSION, layers)


def generate_all(output_dir: Path = BOTS_DIR, only: str = None, force: bool = False):
    """Generate .res files for all (or one) bot definitions."""
    output_dir.mkdir(parents=True, exist_ok=True)

    generated = []
    skipped = []

    for filename, display_name, internal_name, tooltip, icon_rgb, icon_symbol in BOTS:
        if only and internal_name != only:
            continue

        filepath = output_dir / filename
        res_data = generate_bot_res(
            display_name=display_name,
            internal_name=internal_name,
            tooltip=tooltip,
            icon_rgb=icon_rgb,
            icon_symbol=icon_symbol,
        )

        # Check if file already exists with different content
        if filepath.exists() and not force:
            existing = filepath.read_bytes()
            if existing == res_data:
                skipped.append(filename)
                continue
            # Parse existing to check if it has a custom icon or pagina
            try:
                existing_parsed = parse_res_file(filepath)
                has_custom_icon = False
                has_pagina = False
                existing_action = None
                for layer in existing_parsed["layers"]:
                    if layer["type"] == "pagina":
                        has_pagina = True
                    if layer["type"] == "action":
                        existing_action = layer["parsed"]
                    if layer["type"] == "image":
                        # Check if it has a unique (non-placeholder) icon
                        png_start = layer["data"].find(b"\x89PNG")
                        if png_start >= 0:
                            existing_png_hash = hashlib.md5(layer["data"][png_start:]).hexdigest()
                            # If the file already has correct action data, skip it
                            if (existing_action and
                                existing_action["display_name"] == display_name and
                                internal_name in existing_action["ad"]):
                                has_custom_icon = True

                if has_custom_icon and has_pagina:
                    # File has custom icon AND pagina - preserve it
                    skipped.append(f"{filename} (has custom icon + pagina, use --force to overwrite)")
                    continue
                elif has_custom_icon:
                    # Correct action + custom icon, but we'd replace the icon
                    skipped.append(f"{filename} (has correct action data, use --force to overwrite)")
                    continue
            except Exception:
                pass  # Can't parse, just overwrite

        filepath.write_bytes(res_data)
        generated.append(filename)
        print(f"  Generated: {filepath}")

    return generated, skipped


def dump_res_file(filepath: Path):
    """Parse and display contents of a .res file."""
    parsed = parse_res_file(filepath)
    print(f"File: {filepath}")
    print(f"Version: {parsed['version']}")
    print(f"Layers: {len(parsed['layers'])}")
    print()

    for i, layer in enumerate(parsed["layers"]):
        print(f"  Layer {i}: type=\"{layer['type']}\", length={layer['length']}")
        p = layer["parsed"]
        if layer["type"] == "action":
            print(f"    Parent: {p['parent']} (ver {p['parent_ver']})")
            print(f"    Display name: {p['display_name']}")
            print(f"    Prereq: \"{p['prereq']}\"")
            print(f"    Hotkey: {p['hotkey']}")
            print(f"    Ad entries: {p['ad']}")
        elif layer["type"] == "image":
            if "z" in p:
                print(f"    Format: old (ver={p['ver']}), z={p['z']}, subz={p['subz']}")
                print(f"    Flags: {p['fl']}, id={p['id']}, offset={p['offset']}")
                if p.get("meta"):
                    print(f"    Metadata: {p['meta']}")
                print(f"    PNG: offset={p['png_offset']}, size={p['png_size']} bytes")
            else:
                print(f"    {p}")
        elif layer["type"] in ("tooltip", "pagina"):
            text = p.get("text", "")
            if len(text) > 100:
                text = text[:100] + "..."
            print(f"    Text: \"{text}\"")
        print()


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(
        description="Generate Haven & Hearth .res files for Hurricane bot menu entries.",
    )
    parser.add_argument(
        "--list", action="store_true",
        help="List all bot definitions without generating files.",
    )
    parser.add_argument(
        "--parse", metavar="FILE",
        help="Parse and dump the contents of a .res file.",
    )
    parser.add_argument(
        "--only", metavar="NAME",
        help="Generate only the bot with this internal name (e.g., ButcherBot).",
    )
    parser.add_argument(
        "--force", action="store_true",
        help="Overwrite existing files even if they have custom icons.",
    )
    parser.add_argument(
        "--output-dir", metavar="DIR", type=Path, default=BOTS_DIR,
        help=f"Output directory (default: {BOTS_DIR}).",
    )

    args = parser.parse_args()

    if args.list:
        print("Bot definitions:")
        print(f"{'Filename':<28} {'Display Name':<24} {'Internal Name':<20} {'Color':<16} {'Symbol'}")
        print("-" * 100)
        for filename, display_name, internal_name, tooltip, rgb, symbol in BOTS:
            color_str = f"({rgb[0]:>3},{rgb[1]:>3},{rgb[2]:>3})"
            print(f"{filename:<28} {display_name:<24} {internal_name:<20} {color_str:<16} {symbol}")
        return

    if args.parse:
        filepath = Path(args.parse)
        if not filepath.exists():
            print(f"Error: File not found: {filepath}", file=sys.stderr)
            sys.exit(1)
        dump_res_file(filepath)
        return

    print(f"Generating bot .res files in: {args.output_dir}")
    print()
    generated, skipped = generate_all(
        output_dir=args.output_dir,
        only=args.only,
        force=args.force,
    )
    print()
    if generated:
        print(f"Generated {len(generated)} file(s).")
    if skipped:
        print(f"Skipped {len(skipped)} file(s):")
        for s in skipped:
            print(f"  - {s}")
    if not generated and not skipped:
        if args.only:
            print(f"Error: No bot found with internal name '{args.only}'.", file=sys.stderr)
            sys.exit(1)
        else:
            print("Nothing to do.")


if __name__ == "__main__":
    main()
