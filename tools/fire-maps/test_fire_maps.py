import re
import unittest
from pathlib import Path

import fire_maps

HERE = Path(__file__).resolve().parent
GUIDE = HERE.parents[1] / "docs" / "user-guide.md"


class ModeDataTest(unittest.TestCase):
    def setUp(self):
        self.modes = fire_maps.load_modes()

    def test_first_pass_modes_present(self):
        keys = [m["key"] for m in self.modes]
        self.assertIn("note", keys)
        self.assertIn("drum-xox", keys)

    def test_controls_are_known(self):
        for mode in self.modes:
            for name in mode.get("controls", {}):
                self.assertIn(name, fire_maps.CONTROLS, f"{mode['key']}: unknown control {name}")
            self.assertIn(mode["entry"], fire_maps.CONTROLS, mode["key"])

    def test_encoders_have_one_line_per_page(self):
        for mode in self.modes:
            self.assertEqual(len(mode["encoders"]), 4, mode["key"])
            pages = fire_maps.encoder_pages(mode)
            for column in mode["encoders"]:
                self.assertEqual(len(column), len(pages), mode["key"])

    def test_pad_regions_use_valid_rows(self):
        for mode in self.modes:
            for region in mode["pads"]:
                rows = region["rows"]
                self.assertTrue(rows and all(1 <= r <= 4 for r in rows), mode["key"])
                self.assertEqual(rows, list(range(rows[0], rows[-1] + 1)), mode["key"])

    def test_guide_heading_exists(self):
        headings = {m.group(1).strip() for m in re.finditer(r"^#{2,4} (.+)$", GUIDE.read_text(), re.M)}
        for mode in self.modes:
            self.assertIn(mode["guide"], headings, mode["key"])


class LayoutTest(unittest.TestCase):
    def test_every_mode_lays_out_cleanly(self):
        for mode in fire_maps.load_modes():
            with self.subTest(mode=mode["key"]):
                drawing = fire_maps.draw_mode(mode)
                self.assertEqual(fire_maps.layout_problems(drawing), [])
                self.assertTrue(drawing.svg().startswith("<svg"))

    def test_lint_catches_overlapping_text(self):
        d = fire_maps.Drawing()
        d.text_block(100, 100, ["Title", "a long line of text"], "start", "m")
        d.text_block(110, 110, ["Other"], "start", "m")
        self.assertTrue(fire_maps.layout_problems(d))

    def test_lint_catches_crossing_leaders(self):
        d = fire_maps.Drawing()
        d.leader([(0, 0), (100, 100)], "m")
        d.leader([(0, 100), (100, 0)], "m")
        self.assertTrue(fire_maps.layout_problems(d))

    def test_lint_catches_leader_through_text(self):
        d = fire_maps.Drawing()
        d.text_block(50, 50, ["Title"], "start", "m")
        d.leader([(0, 45), (200, 45)], "m")
        self.assertTrue(fire_maps.layout_problems(d))


class WebExportTest(unittest.TestCase):
    def test_web_data_has_one_entry_per_mode(self):
        import make_fire_maps
        modes = fire_maps.load_modes()
        data = make_fire_maps.web_modes(modes)
        self.assertEqual([m["key"] for m in data], [m["key"] for m in modes])
        for entry in data:
            self.assertEqual(set(entry), {"key", "title", "entry", "manual", "how", "intro", "notes", "svg"})
            self.assertIn(entry["entry"], fire_maps.MODE_BUTTONS)
            self.assertTrue(entry["svg"].startswith("<svg"))
            self.assertNotIn("`", entry["how"] + entry["intro"] + "".join(entry["notes"]))

    def test_web_module_is_marked_generated(self):
        import make_fire_maps
        ts = make_fire_maps.web_module([{"key": "k", "title": "T", "how": "", "intro": "", "notes": [], "svg": "<svg/>"}])
        self.assertTrue(ts.startswith("// Written by tools/fire-maps/make_fire_maps.py in bitwig-oikontrol"))
        self.assertIn("export const MODES = [", ts)

    def test_manual_ids_are_guide_heading_slugs(self):
        import make_fire_maps
        self.assertEqual(make_fire_maps.manual_id("DRUM Mode"), "drum-mode")
        self.assertEqual(make_fire_maps.manual_id("Multiclip Seq mode"), "multiclip-seq-mode")
        self.assertEqual(make_fire_maps.manual_id("Launcher mode (`PERFORM`)"), "launcher-mode")


class InlineTest(unittest.TestCase):
    def test_control_names_render_as_kbd(self):
        self.assertEqual(fire_maps.inline("Press `NOTE` & go"), "Press <kbd>NOTE</kbd> &amp; go")


class TagTest(unittest.TestCase):
    def test_split_tag(self):
        self.assertEqual(fire_maps.split_tag("SHIFT: fine nudge"), ("SHIFT", "fine nudge"))
        self.assertEqual(fire_maps.split_tag("hold pad + turn: pick device"), ("hold pad + turn", "pick device"))
        self.assertEqual(fire_maps.split_tag("Plain text"), (None, "Plain text"))


if __name__ == "__main__":
    unittest.main()
