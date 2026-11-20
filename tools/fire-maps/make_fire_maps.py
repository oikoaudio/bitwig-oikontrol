#!/usr/bin/env python3
"""Writes the Akai Fire mode maps as one HTML page and, when Chromium is available, a PDF with one page per mode.

    make_fire_maps.py OUTDIR [--no-pdf] [--web WEB_DIR]

    OUTDIR/fire-mode-maps.html
    OUTDIR/fire-mode-maps.pdf
    WEB_DIR/app/manuals/oikontrol/maps/maps-data.ts     with --web: the maps for the oikoaudio website
"""
import html
import json
import re
import shutil
import subprocess
import sys
from pathlib import Path

import fire_maps

HERE = Path(__file__).resolve().parent
LEGEND = [("mode", "In this mode"), ("pads", "Pad areas"), ("entry", "Button that enters the mode"),
          ("shared", "Same in every mode")]


def checked_drawing(mode):
    drawing = fire_maps.draw_mode(mode)
    problems = fire_maps.layout_problems(drawing)
    if problems:
        raise SystemExit(f"make_fire_maps: {mode['key']} layout:\n  " + "\n  ".join(problems))
    return drawing


def mode_section(mode):
    drawing = checked_drawing(mode)
    notes = "".join(f"<li>{fire_maps.inline(n)}</li>" for n in mode.get("notes", []))
    return f"""<section class="mode" id="{mode['key']}">
<h2>{html.escape(mode['title'])}</h2>
<p class="how">{fire_maps.inline(mode['how'])} {fire_maps.inline(mode['intro'])}</p>
{drawing.svg()}
<ul>{notes}</ul>
</section>"""


def page(modes):
    css = (HERE / "maps.css").read_text()
    nav = "".join(f'<a href="#{m["key"]}">{html.escape(m["title"])}</a>' for m in modes)
    legend = "".join(f'<span><i style="background:var(--{k})"></i>{t}</span>' for k, t in LEGEND)
    sections = "\n".join(mode_section(m) for m in modes)
    return f"""<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Akai Fire mode maps · Oikontrol</title><style>{css}</style></head>
<body><main>
<div class="intro-page"><h1>Akai Fire mode maps</h1>
<p>One map per mode: what each control does there. Encoder callouts list one line per encoder page:
<b>CH</b> Channel, <b>MX</b> Mixer, <b>U1</b> User 1, <b>U2</b> User 2. Press <b>KNOB MODE</b> to change page.
The user guide has the details.</p>
<p class="legend">{legend}</p><nav>{nav}</nav></div>
{sections}
</main></body></html>
"""


def manual_id(heading):
    """the website manual's id for a user-guide heading: its slug, without any parenthetical"""
    text = re.sub(r"\(.*?\)", "", heading)
    return re.sub(r"[^a-z0-9]+", "-", text.lower()).strip("-")


def web_modes(modes):
    """the maps as data for the website's page: text fields are HTML, with control names in <kbd>"""
    return [{
        "key": m["key"],
        "title": m["title"],
        "entry": m["entry"],
        "manual": manual_id(m["guide"]),
        "how": fire_maps.inline(m["how"]),
        "intro": fire_maps.inline(m["intro"]),
        "notes": [fire_maps.inline(n) for n in m.get("notes", [])],
        "svg": checked_drawing(m).svg(),
    } for m in modes]


def web_module(data):
    head = "// Written by tools/fire-maps/make_fire_maps.py in bitwig-oikontrol: change it there, not here\n"
    return head + f"export const MODES = {json.dumps(data, ensure_ascii=False, indent=1)};\n"


def find_chromium():
    for name in ("chromium", "chromium-browser", "google-chrome", "google-chrome-stable"):
        path = shutil.which(name)
        if path:
            return path
    return None


def main():
    argv = sys.argv[1:]
    web = None
    if "--web" in argv:
        i = argv.index("--web")
        if i + 1 >= len(argv):
            raise SystemExit(__doc__)
        web = Path(argv[i + 1]).resolve()
        del argv[i:i + 2]
    args = [a for a in argv if not a.startswith("--")]
    if len(args) != 1:
        raise SystemExit(__doc__)
    modes = fire_maps.load_modes()
    out = Path(args[0]).resolve()
    out.mkdir(parents=True, exist_ok=True)
    target = out / "fire-mode-maps.html"
    target.write_text(page(modes))
    print(f"make_fire_maps: {target}")
    if web:
        d = web / "app" / "manuals" / "oikontrol" / "maps"
        d.mkdir(parents=True, exist_ok=True)
        (d / "maps-data.ts").write_text(web_module(web_modes(modes)))
        print(f"make_fire_maps: {d / 'maps-data.ts'}")
    if "--no-pdf" in argv:
        return
    chromium = find_chromium()
    if not chromium:
        print("make_fire_maps: no Chromium found, skipping the PDF")
        return
    pdf = out / "fire-mode-maps.pdf"
    subprocess.run([chromium, "--headless", "--disable-gpu", "--no-pdf-header-footer",
                    f"--print-to-pdf={pdf}", target.as_uri()], check=True, capture_output=True)
    print(f"make_fire_maps: {pdf}")


if __name__ == "__main__":
    main()
