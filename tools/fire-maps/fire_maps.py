"""Akai Fire mode maps: the Fire panel as SVG with callouts saying what each control does in a mode.

The panel is drawn from measured coordinates. Every control has a fixed callout slot, so it is labelled in
the same place on every map. The labels come from modes.toml. The SVG carries class names only; maps.css
sets every colour.
"""
import html
import re
import tomllib
from pathlib import Path

HERE = Path(__file__).resolve().parent
MODES_FILE = HERE / "modes.toml"

# ---- panel geometry, in panel units (body 1000 x 500)
PW, PH = 1000, 500
ENC_X = [121, 227, 336, 442]
ENC_Y, ENC_R = 84, 25
SELECT_XY, SELECT_R = (775, 84), 27
BROWSER_XY = (700, 84)
GRID_L, GRID_R = (842, 84), (912, 84)
PAT_UP, PAT_DN = (512, 58), (512, 112)
OLED = (562, 44, 108, 80)
KNOB_MODE_XY = (50, 128)
PAD_X0, PAD_PITCH_X, PAD_W = 140, 52.5, 42
PAD_Y0, PAD_PITCH_Y, PAD_H = 172, 66, 52
MUTE_X, MUTE_R = 54, 15
BOTTOM_Y, BTN_W, BTN_H = 455, 46, 36
BOTTOM = {"STEP": 52, "NOTE": 122, "DRUM": 192, "PERFORM": 262, "SHIFT": 332, "ALT": 402,
          "PATTERN": 694, "PLAY": 764, "STOP": 834, "REC": 904}
MODE_BUTTONS = ["STEP", "NOTE", "DRUM", "PERFORM", "SHIFT", "ALT"]
TRANSPORT = ["PATTERN", "PLAY", "STOP", "REC"]

ENCODER_PAGES = ["CH", "MX", "U1", "U2"]
CONTROLS = {"KNOB MODE", "PATTERN UP/DOWN", "BROWSER", "SELECT", "GRID LEFT/RIGHT",
            "MUTE 1", "MUTE 2", "MUTE 3", "MUTE 4", "TRANSPORT", *MODE_BUTTONS}

# ---- canvas: the panel sits at (OX, OY)
OX, OY = 250, 250
W, H = 1540, 960
LH = 21                      # line height
LEFT_X = OX - 36             # left callouts end here
RIGHT_X = OX + PW + 36       # right callouts start here
LANE_X = OX - 14             # left leaders drop through this lane
TOP_Y = OY - 200             # baseline of the top callout titles

# estimated glyph widths for the layout lint, in px per character
TITLE_PX, LINE_PX, TAG_PX = 10.0, 8.4, 7.6
TITLE_SIZE, LINE_SIZE = 17, 15


def P(x, y):
    """panel units to canvas units"""
    return OX + x, OY + y


def pad_center(row, col):
    return PAD_X0 + col * PAD_PITCH_X + PAD_W / 2, PAD_Y0 + row * PAD_PITCH_Y + PAD_H / 2


def split_tag(line):
    """'SHIFT: fine nudge' -> ('SHIFT', 'fine nudge'). The tag renders small and muted."""
    m = re.match(r"^([^:]{1,24}): (.+)$", line)
    return (m.group(1), m.group(2)) if m else (None, line)


def inline(text):
    """`CONTROL` -> <kbd>CONTROL</kbd>: backticks mark control names, as in the user guide"""
    return re.sub(r"`([^`]+)`", r"<kbd>\1</kbd>", html.escape(text))


class Drawing:
    """SVG parts plus what the layout lint needs: text boxes and leader segments, grouped per callout"""

    def __init__(self):
        self.parts = []
        self.boxes = []      # (group, x0, y0, x1, y1)
        self.segments = []   # (group, (x0, y0), (x1, y1))
        self.group = 0
        self.panel_box = None

    def new_group(self):
        self.group += 1
        return self.group

    def add(self, s):
        self.parts.append(s)

    # -- text and leaders
    def text_block(self, x, y, lines, anchor, kind, group=None):
        """lines[0] is the title; later lines may carry a 'TAG: text' prefix. y is the title baseline."""
        group = group or self.new_group()
        widest = 0
        for n, line in enumerate(lines):
            ty = y + n * LH
            if n == 0:
                self.add(f'<text x="{x:.1f}" y="{ty:.1f}" text-anchor="{anchor}" class="c-title {kind}">{html.escape(line)}</text>')
                width = len(line) * TITLE_PX
            else:
                tag, body = split_tag(line)
                tspan = f'<tspan class="c-tag">{html.escape(tag)}</tspan> ' if tag else ""
                self.add(f'<text x="{x:.1f}" y="{ty:.1f}" text-anchor="{anchor}" class="c-line">{tspan}{html.escape(body)}</text>')
                width = len(body) * LINE_PX + (len(tag) * TAG_PX + 5 if tag else 0)
            widest = max(widest, width)
        x0 = {"start": x, "middle": x - widest / 2, "end": x - widest}[anchor]
        y0 = y - TITLE_SIZE + 3
        y1 = y + (len(lines) - 1) * LH + 5
        self.boxes.append((group, x0, y0, x0 + widest, y1))
        return group

    def leader(self, pts, kind, dot=True, group=None):
        group = group or self.new_group()
        d = "M" + " L".join(f"{x:.1f},{y:.1f}" for x, y in pts)
        self.add(f'<path d="{d}" class="ld {kind}"/>')
        if dot:
            x, y = pts[-1]
            self.add(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="4" class="ldot {kind}"/>')
        for a, b in zip(pts, pts[1:]):
            self.segments.append((group, a, b))
        return group

    def bounds(self, margin=16):
        xs, ys = [], []
        for _, x0, y0, x1, y1 in self.boxes:
            xs += [x0, x1]
            ys += [y0, y1]
        for _, a, b in self.segments:
            xs += [a[0], b[0]]
            ys += [a[1], b[1]]
        if self.panel_box:
            xs += [self.panel_box[0], self.panel_box[2]]
            ys += [self.panel_box[1], self.panel_box[3]]
        return min(xs) - margin, min(ys) - margin, max(xs) + margin, max(ys) + margin

    def svg(self):
        label = getattr(self, "label", "")
        body = "\n".join(self.parts)
        x0, y0, x1, y1 = self.bounds()
        return (f'<svg class="fire-map" viewBox="{x0:.0f} {y0:.0f} {x1 - x0:.0f} {y1 - y0:.0f}" xmlns="http://www.w3.org/2000/svg" '
                f'role="img" aria-label="{html.escape(label)}">\n{body}\n</svg>')


# ---- the panel
def draw_panel(d, lit, entry, pad_rows):
    def cls(name):
        return " entry" if name == entry else " on" if name in lit else ""

    a = d.add
    a(f'<g transform="translate({OX},{OY})">')
    a(f'<rect x="0" y="0" width="{PW}" height="{PH}" rx="22" class="d-body"/>')
    for i, name in enumerate(["CHANNEL", "MIXER", "USER 1", "USER 2"]):
        a(f'<circle cx="34" cy="{24 + i * 15}" r="4" class="d-led"/><text x="44" y="{28 + i * 15}" class="d-tiny">{name}</text>')
    x, y = KNOB_MODE_XY
    a(f'<rect x="{x - 21}" y="{y - 10}" width="42" height="20" rx="4" class="d-btn{cls("KNOB MODE")}"/>')
    for ex in ENC_X:
        knob(d, ex, ENC_Y, ENC_R, " on" if "ENCODERS" in lit else "")
    for bx, by in (PAT_UP, PAT_DN):
        a(f'<rect x="{bx - 20}" y="{by - 10}" width="40" height="20" rx="4" class="d-btn{cls("PATTERN UP/DOWN")}"/>')
    ox, oy, ow, oh = OLED
    a(f'<rect x="{ox}" y="{oy}" width="{ow}" height="{oh}" rx="6" class="d-screen"/>')
    bx, by = BROWSER_XY
    a(f'<rect x="{bx - 20}" y="{by - 20}" width="40" height="40" rx="6" class="d-btn{cls("BROWSER")}"/>')
    knob(d, *SELECT_XY, SELECT_R, cls("SELECT"))
    for gx, gy in (GRID_L, GRID_R):
        a(f'<rect x="{gx - 22}" y="{gy - 11}" width="44" height="22" rx="4" class="d-btn{cls("GRID LEFT/RIGHT")}"/>')
    a(f'<text x="{GRID_L[0] - 30}" y="{GRID_L[1] + 4}" class="d-tiny" text-anchor="end">◀</text>')
    a(f'<text x="{GRID_R[0] + 30}" y="{GRID_R[1] + 4}" class="d-tiny">▶</text>')
    a('<text x="910" y="40" class="d-logo" text-anchor="end">FIRE</text>')
    for i in range(4):
        my = pad_center(i, 0)[1]
        a(f'<circle cx="{MUTE_X}" cy="{my}" r="{MUTE_R}" class="d-btn{cls(f"MUTE {i + 1}")}"/>')
        a(f'<rect x="{MUTE_X + 24}" y="{my - 11}" width="7" height="22" rx="2" class="d-led"/>')
    for g in range(1, 4):
        sx = PAD_X0 + g * 4 * PAD_PITCH_X - (PAD_PITCH_X - PAD_W) / 2
        a(f'<line x1="{sx}" y1="{PAD_Y0 - 6}" x2="{sx}" y2="{PAD_Y0 + 3 * PAD_PITCH_Y + PAD_H + 6}" class="d-sep"/>')
    for r in range(4):
        for c in range(16):
            a(f'<rect x="{PAD_X0 + c * PAD_PITCH_X}" y="{PAD_Y0 + r * PAD_PITCH_Y}" width="{PAD_W}" height="{PAD_H}" '
              f'rx="5" class="d-pad {pad_rows.get(r, "")}"/>')
    for name, bx in BOTTOM.items():
        c = "" if name in TRANSPORT else cls(name)
        a(f'<rect x="{bx - BTN_W / 2}" y="{BOTTOM_Y - BTN_H / 2}" width="{BTN_W}" height="{BTN_H}" rx="5" class="d-btn{c}"/>')
        a(f'<text x="{bx}" y="{BOTTOM_Y + 3}" class="d-tiny in{c}" text-anchor="middle">{name}</text>')
    a('<text x="548" y="463" class="d-logo" text-anchor="middle">AKAI</text>')
    a('</g>')
    d.panel_box = (OX, OY, OX + PW, OY + PH)


def knob(d, x, y, r, c):
    d.add(f'<circle cx="{x}" cy="{y}" r="{r + 5}" class="d-tray"/><circle cx="{x}" cy="{y}" r="{r}" class="d-knob{c}"/>')
    d.add(f'<line x1="{x}" y1="{y - r + 4}" x2="{x}" y2="{y - r + 12}" class="d-ind"/>')


# ---- callout slots: one per control, the same on every map
def encoder_pages(mode):
    """the tags for each encoder line: the four encoder pages, unless the mode pages its encoders differently"""
    return mode.get("pages", ENCODER_PAGES)


def slot_encoders(d, columns, pages):
    for i, labels in enumerate(columns):
        tx = OX - 60 + i * 170
        lines = [f"Encoder {i + 1}"] + [f"{page}: {label}" for page, label in zip(pages, labels)]
        g = d.text_block(tx, TOP_Y, lines, "start", "m")
        ex, ey = P(ENC_X[i], ENC_Y)
        bend = OY - 40 - i * 12
        d.leader([(tx + 6, TOP_Y + (len(lines) - 1) * LH + 10), (tx + 6, bend), (ex, bend), (ex, ey - ENC_R)], "m", group=g)


def slot_pattern(d, lines, kind):
    tx = OX + 640
    g = d.text_block(tx, TOP_Y, lines, "start", kind)
    px, py = P(PAT_UP[0], PAT_UP[1] - 10)
    bend = OY - 40
    d.leader([(tx + 6, TOP_Y + (len(lines) - 1) * LH + 10), (tx + 6, bend), (px, bend), (px, py)], kind, group=g)


def slot_right(d, y, lines, kind, to, drop_x):
    """label right of the panel; the leader runs left along the title line, then down to the control"""
    g = d.text_block(RIGHT_X, y, lines, "start", kind)
    d.leader([(RIGHT_X - 10, y - 6), (drop_x, y - 6), to], kind, group=g)


def slot_left(d, y, lines, kind, to):
    """label left of the panel; the leader drops through the left lane to the control's height"""
    g = d.text_block(LEFT_X, y, lines, "end", kind)
    d.leader([(LEFT_X + 10, y - 6), (LANE_X, y - 6), (LANE_X, to[1]), to], kind, group=g)


def slot_bottom(d, items):
    """mode buttons as a staircase: text hangs right of each leader, the leftmost button's text lowest,
    so no leader crosses another button's text"""
    by = OY + BOTTOM_Y + BTN_H / 2
    ty = by + 62
    for name, lines, kind in reversed(items):
        bx = P(BOTTOM[name], 0)[0]
        g = d.text_block(bx - 8, ty, lines, "start", kind)
        d.leader([(bx, ty - TITLE_SIZE - 2), (bx, by)], kind, group=g)
        ty += (len(lines) - 1) * LH + 46


def slot_transport(d, lines):
    x0, x1 = P(BOTTOM["PATTERN"] - 26, 0)[0], P(BOTTOM["REC"] + 26, 0)[0]
    by = OY + BOTTOM_Y + BTN_H / 2 + 20
    xm = (x0 + x1) / 2
    g = d.text_block(xm, by + 50, lines, "middle", "s")
    d.leader([(x0, by - 8), (x0, by), (x1, by), (x1, by - 8)], "s", dot=False, group=g)
    d.leader([(xm, by), (xm, by + 30)], "s", dot=False, group=g)


def slot_pads(d, region, kind_index):
    rows = [r - 1 for r in region["rows"]]
    x = OX + PAD_X0 + 16 * PAD_PITCH_X + 4
    y0 = OY + PAD_Y0 + rows[0] * PAD_PITCH_Y + 4
    y1 = OY + PAD_Y0 + rows[-1] * PAD_PITCH_Y + PAD_H - 4
    ym = (y0 + y1) / 2
    lines = region["lines"]
    ty = ym + 6 - LH / 2 if len(lines) > 2 else ym + 6
    g = d.text_block(RIGHT_X, ty, lines, "start", "p")
    d.leader([(x, y0), (x + 10, y0), (x + 10, y1), (x, y1)], "p", dot=False, group=g)
    d.leader([(x + 10, ty - 6), (RIGHT_X - 10, ty - 6)], "p", dot=False, group=g)


def draw_mode(mode):
    controls = dict(mode.get("controls", {}))
    entry = mode["entry"]
    pad_rows = {}
    for n, region in enumerate(mode["pads"]):
        for r in region["rows"]:
            pad_rows[r - 1] = f"r{n % 3}"
    lit = set(controls) | {"ENCODERS"}
    d = Drawing()
    draw_panel(d, lit, entry, pad_rows)

    slot_encoders(d, mode["encoders"], encoder_pages(mode))
    if "PATTERN UP/DOWN" in controls:
        slot_pattern(d, controls["PATTERN UP/DOWN"], "m")
    right = [("BROWSER", OY - 135, P(BROWSER_XY[0], BROWSER_XY[1] - 20), P(BROWSER_XY[0], 0)[0]),
             ("SELECT", OY - 70, P(SELECT_XY[0], SELECT_XY[1] - SELECT_R), P(SELECT_XY[0], 0)[0]),
             ("GRID LEFT/RIGHT", OY + 20, P(GRID_R[0], GRID_R[1] - 11), P(GRID_R[0], 0)[0])]
    for name, y, to, drop_x in right:
        if name in controls:
            slot_right(d, y, controls[name], "m", to, drop_x)
    if "KNOB MODE" in controls:
        slot_left(d, OY + 100, controls["KNOB MODE"], "m", P(KNOB_MODE_XY[0] - 21, KNOB_MODE_XY[1]))
    for i in range(4):
        name = f"MUTE {i + 1}"
        if name in controls:
            lines = controls[name]
            my = pad_center(i, 0)[1]
            ty = OY + my + (6 if len(lines) == 1 else -2)
            slot_left(d, ty, lines, "m", P(MUTE_X - MUTE_R, my))
    for n, region in enumerate(mode["pads"]):
        slot_pads(d, region, n)
    bottom = []
    for name in MODE_BUTTONS:
        if name == entry:
            bottom.append((name, controls.get(name, [name]), "e"))
        elif name in controls:
            bottom.append((name, controls[name], "m"))
    slot_bottom(d, bottom)
    slot_transport(d, controls.get("TRANSPORT", ["Transport", "same in every mode"]))
    d.label = mode["title"]
    return d


# ---- layout lint
def _overlap(a, b, pad=2):
    return a[0] < b[2] + pad and b[0] < a[2] + pad and a[1] < b[3] + pad and b[1] < a[3] + pad


def _segments_cross(p, q, r, s):
    def orient(a, b, c):
        v = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0])
        return (v > 1e-9) - (v < -1e-9)
    o1, o2, o3, o4 = orient(p, q, r), orient(p, q, s), orient(r, s, p), orient(r, s, q)
    return o1 * o2 < 0 and o3 * o4 < 0


def _segment_hits_box(a, b, box):
    x0, y0, x1, y1 = box
    steps = max(2, int(max(abs(b[0] - a[0]), abs(b[1] - a[1])) / 2))
    for i in range(steps + 1):
        t = i / steps
        x, y = a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t
        if x0 < x < x1 and y0 < y < y1:
            return True
    return False


def layout_problems(d):
    problems = []
    boxes = d.boxes
    for i, (g1, *b1) in enumerate(boxes):
        if not (0 <= b1[0] and b1[2] <= W and 0 <= b1[1] and b1[3] <= H):
            problems.append(f"text box {b1} leaves the canvas")
        if d.panel_box and _overlap(b1, d.panel_box, pad=0):
            problems.append(f"text box {b1} overlaps the panel")
        for g2, *b2 in boxes[i + 1:]:
            if _overlap(b1, b2, pad=6):
                problems.append(f"text boxes overlap: {b1} / {b2}")
    for g, a, b in d.segments:
        for g2, *box in boxes:
            if g2 != g and _segment_hits_box(a, b, box):
                problems.append(f"leader {a}->{b} crosses text {box}")
    segs = d.segments
    for i, (g1, a, b) in enumerate(segs):
        for g2, c, e in segs[i + 1:]:
            if g1 != g2 and _segments_cross(a, b, c, e):
                problems.append(f"leaders cross: {a}->{b} / {c}->{e}")
    return problems


def load_modes(path=MODES_FILE):
    with open(path, "rb") as f:
        return tomllib.load(f)["mode"]
