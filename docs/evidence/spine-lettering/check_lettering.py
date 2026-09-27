#!/usr/bin/env python3
"""Recheck the recorded Nothing UI; private preferences are optional and never copied."""
import argparse
from collections import Counter
import json
from pathlib import Path
import re
import statistics
import xml.etree.ElementTree as ET

import numpy as np
from PIL import Image


def nodes(root, name):
    return list(ET.parse(root / f"{name}.xml").iter("node"))


def bounds(node):
    return tuple(map(int, re.findall(r"\d+", node.get("bounds"))))


def track_pitch(root, name):
    tracklist = next(n for n in nodes(root, name) if n.get("content-desc") == "Track list")
    rows = [bounds(n) for n in tracklist if n.get("checkable") == "true"]
    return statistics.median(b[1] - a[1] for a, b in zip(rows, rows[1:]))


def title_ink(root, name):
    image = np.array(Image.open(root / f"{name}.png").convert("RGB")).astype(int)
    result = []
    for n in nodes(root, name):
        if n.get("content-desc") != "Mixtape Night Drive":
            continue
        x1, y1, x2, y2 = bounds(n)
        width = x2 - x1
        # Identical navy name ink; exclude the independently coloured symbol lane.
        crop = image[y1 + 4:y2 - 4, x1 + int(width * .07):x1 + int(width * .82)]
        r, g, b = crop[:, :, 0], crop[:, :, 1], crop[:, :, 2]
        mask = (b > g + 18) & (b > r + 25) & (r < 115) & (g < 145)
        ys = np.where(mask.sum(axis=1) >= 3)[0]
        assert len(ys), f"No navy title ink detected in {name}"
        result.append({"ink_height_px": int(ys[-1] - ys[0] + 1), "spine_height_px": y2 - y1})
    assert len(result) == 3
    return result


def library(root, name, expected_columns):
    all_nodes = nodes(root, name)
    descriptions = [n.get("content-desc", "") for n in all_nodes]
    assert "Tape list" in descriptions and "Cassette player" not in descriptions
    # Lazy layouts may expose prefetched offscreen nodes with zero-sized bounds.
    tapes = [n for n in all_nodes if n.get("content-desc", "").startswith(("Mixtape ", "Current mixtape ")) and bounds(n)[3] > bounds(n)[1]]
    columns = len({bounds(n)[0] for n in tapes})
    assert columns == expected_columns
    assert all(abs((bounds(n)[2] - bounds(n)[0]) / (bounds(n)[3] - bounds(n)[1]) - 6.5) < .05 for n in tapes)
    return {"columns": columns, "visible_tapes": len(tapes), "ratio": 6.5, "player_absent": True}


def preferences(path):
    return {n.get("name"): n.get("value") if n.get("value") is not None else n.text for n in ET.parse(path).getroot()}


def preference_checks(root):
    before = preferences(root / "before-visual-properties.xml")
    after = preferences(root / "after-visual-properties.xml")
    final = preferences(root / "final-visual-properties.xml")
    assert final == after, "Layout/ink changed unexpectedly after restart and UI verification"
    allowed_changes = ("name_color:", "symbol_color:")
    assert all(after.get(k) == v for k, v in before.items() if not k.startswith(allowed_changes))
    ids = [k.split(":", 1)[1] for k in after if k.startswith("spine_style_version:")]
    assert len(ids) == 13
    assert all(after["spine_style_version:" + i] == "1" for i in ids)
    assert all(after["name_color:" + i] != after["symbol_color:" + i] for i in ids)
    return {
        "tapes": len(ids), "existing_non_ink_properties_preserved": True,
        "every_initial_ink_pair_distinct": True, "persisted_styles_unchanged_after_qa": True,
        "alignment_counts": dict(Counter(after["spine_text_alignment:" + i] for i in ids)),
        "symbol_position_counts": dict(Counter(after["spine_symbol_placement:" + i] for i in ids)),
        "name_ink_count": len({after["name_color:" + i] for i in ids}),
        "symbol_ink_count": len({after["symbol_color:" + i] for i in ids}),
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--private-prefs", type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parent
    before = track_pitch(root, "before-tracklist-landscape")
    after = track_pitch(root, "final-03-large-landscape-tracks")
    portrait = track_pitch(root, "final-04-large-portrait-tracks")
    assert (before, after, portrait) == (95, 86, 86)
    sizes = {size: title_ink(root, name) for size, name in {
        "Large": "final-05-font-gallery-large", "Medium": "final-06-font-gallery-medium", "Small": "final-07-font-gallery-small"
    }.items()}
    for i in range(3):
        assert sizes["Large"][i]["ink_height_px"] > sizes["Medium"][i]["ink_height_px"] > sizes["Small"][i]["ink_height_px"]
        assert sizes["Large"][i]["ink_height_px"] / sizes["Large"][i]["spine_height_px"] > .70
    assert (root / "final-05-font-gallery-large.png").read_bytes() == (root / "final-08-font-gallery-restored.png").read_bytes()
    font_names = {"Kalam", "Patrick Hand", "Caveat", "Nanum Pen Script", "Indie Flower", "Gloria Hallelujah", "Architects Daughter", "Shadows Into Light"}
    seen = {n.get("text") for name in ["final-05-font-gallery-large", "final-09-font-gallery-middle", "final-10-font-gallery-bottom"] for n in nodes(root, name)}
    assert font_names <= seen
    names = {n.get("content-desc", "").removeprefix("Current mixtape ").removeprefix("Mixtape ")
             for name in ["final-11-landscape-library", "final-12-landscape-library-bottom"]
             for n in nodes(root, name) if n.get("content-desc", "").startswith(("Mixtape ", "Current mixtape "))}
    assert len(names) == 13 and "Then the Mountains Appeared" in names and "The Hills Beyond Reception" in names
    report = {
        "track_row_pitch_px": {"before": before, "after_landscape": after, "after_portrait": portrait},
        "row_pitch_reduction_percent": round((before - after) / before * 100, 2),
        "spine_name_ink": sizes, "large_setting_restored": True,
        "font_gallery_families_checked": sorted(font_names),
        "portrait_library": library(root, "final-01-large-portrait-library", 1),
        "landscape_library": library(root, "final-02-large-landscape-library", 2),
        "landscape_library_bottom": library(root, "final-12-landscape-library-bottom", 2),
        "all_13_full_names_in_accessibility_tree": True,
        "single_line_overrun": "Visually checked in final screenshots; renderer contract tested by JVM tests.",
    }
    if args.private_prefs:
        report["persistence"] = preference_checks(args.private_prefs)
    output = json.dumps(report, indent=2) + "\n"
    (root / "layout-checks.json").write_text(output)
    print(output)


if __name__ == "__main__":
    main()
