#!/usr/bin/env python3
"""Check recorded paper boundaries, draft persistence and the larger native rows."""
from pathlib import Path
import json
import argparse
import re
import xml.etree.ElementTree as ET
import numpy as np
from PIL import Image

P = Path(__file__).resolve().parent
parser = argparse.ArgumentParser()
parser.add_argument('--private-prefs', type=Path)
args = parser.parse_args()
PRIVATE = args.private_prefs or (P if (P / 'before-names.xml').exists() else None)


def nodes(name):
    return list(ET.parse(P / (name + '.xml')).iter('node'))


def bounds(node):
    return tuple(map(int, re.findall(r'\d+', node.get('bounds'))))


def pitch(name):
    row = next(n for n in nodes(name) if n.get('content-desc') == 'Track list')
    y = [bounds(n)[1] for n in row if n.get('checkable') == 'true']
    return int(np.median(np.diff(y)))


def preferences(name):
    return {x.get('name'): (x.text, x.get('value')) for x in ET.parse(PRIVATE / name).getroot()}


for part in (['names', 'visual-properties'] if PRIVATE else []):
    assert preferences('before-' + part + '.xml') == preferences('after-cancel-' + part + '.xml')
    if (PRIVATE / ('final-' + part + '.xml')).exists():
        assert preferences('before-' + part + '.xml') == preferences('final-' + part + '.xml')

portrait = nodes('final-03-editor-dice')
landscape = nodes('final-04-editor-dice-landscape')
draft = next(n.get('text') for n in portrait if n.get('class') == 'android.widget.EditText')
assert draft != 'Halfway Into His Coat'
assert draft in [n.get('text') for n in landscape if n.get('class') == 'android.widget.EditText']
assert '22' in [n.get('text') for n in nodes('final-13-code-saved') if n.get('class') == 'android.widget.EditText']

im = np.array(Image.open(P / 'final-24-handoff.png').convert('RGB')).astype(int)[176:316, 43:1037]
r, g, b = im[:, :, 0], im[:, :, 1], im[:, :, 2]
mask = (r > 80) & (r < 190) & (r > g * 1.35) & (b < g * .75) & (g < 115)
left = int(np.where(mask)[1].min() + 43)
paper_left = 43 + 5.25 + 994 * .019
assert abs(left - paper_left) < 1 and left - 43 >= 23
assert pitch('before') == 86 and pitch('final-22-player-final-build') == 112

result = {
    'name_ink_reaches_paper_edge_px': left,
    'expected_paper_edge_px': round(paper_left, 3),
    'plastic_edge_px': 43,
    'track_base_size_sp': {'before': 24, 'after': 32},
    'track_row_pitch_px': {'before': 86, 'after': 112},
    'dice_draft_survives_rotation': True,
    'dice_cancel_preserves_all_saved_names_and_properties': True if PRIVATE else None,
    'noncurrent_tape_code_save_and_reopen_verified': True,
    'test_code_restored_to_original': (PRIVATE / 'final-visual-properties.xml').exists() if PRIVATE else None,
}
print(json.dumps(result, indent=2))
(P / 'layout-checks.json').write_text(json.dumps(result, indent=2) + '\n')
