from pathlib import Path
import json, re, xml.etree.ElementTree as E

ROOT = Path(__file__).parent
checks = []
def nodes(name):
    return [n.attrib for n in E.parse(ROOT / (name + '.xml')).iter('node') if n.get('bounds') != '[0,0][0,0]']
def desc(n): return n.get('content-desc', '')
def bounds(n): return list(map(int, re.findall(r'\d+', n['bounds'])))
def find(ns, label): return next(n for n in ns if desc(n) == label)
def tapes(ns): return [n for n in ns if desc(n).startswith(('Mixtape ', 'Current mixtape '))]
def shape(n):
    x1,y1,x2,y2 = bounds(n)
    return x2-x1, y2-y1

def check(name, passed, **details):
    checks.append(dict(check=name, passed=bool(passed), **details))
    assert passed, checks[-1]

for name, edge in [('after-02-first-landscape-playing', 'first'), ('after-03-first-portrait', 'first'), ('after-07-last-portrait', 'last'), ('after-08-last-landscape', 'last'), ('after-09-middle-landscape', 'middle'), ('after-10-middle-portrait', 'middle')]:
    ns=nodes(name); rows=tapes(ns); current=next(n for n in rows if desc(n).startswith('Current mixtape '))
    area=bounds(find(ns, 'Tape list')); cb=bounds(current); w,h=shape(current)
    check(name+'-current-fully-visible', cb[1]>=area[1] and cb[3]<=area[3] and abs(h-w/6.5)<=1)
    check(name+'-single-column-player-shelf', len({bounds(n)[0] for n in rows})==1)
    if edge=='first':
        gap=cb[1]-area[1]
        check(name+'-bounded-top', gap<=24, gap_px=gap)
    elif edge=='last':
        gap=area[3]-cb[3]
        check(name+'-bounded-bottom', gap<=24, gap_px=gap)
    else:
        error=abs((cb[1]+cb[3]-area[1]-area[3])/2)
        check(name+'-middle-still-centered', error<=1, error_px=error)

check('first-shelf-verified-during-playback', any(desc(n)=='Pause playback' for n in nodes('after-02-first-landscape-playing')))

def shelf_snapshot(name): return [(desc(n),bounds(n)) for n in tapes(nodes(name))]
check('manual-player-shelf-scroll-retained', shelf_snapshot('after-04-manual-shelf-scroll')==shelf_snapshot('after-05-manual-scroll-retained'))
track=nodes('after-06-last-tracklist')
check('selecting-tape-opens-tracklist', any(desc(n)=='Track list' for n in track) and not any(desc(n)=='Tape list' for n in track))

all_names=set()
for name in ['after-01-landscape-library','after-12-landscape-library','after-13-landscape-library-bottom','after-15-landscape-library-retained']:
    ns=nodes(name); rows=tapes(ns); area=bounds(find(ns, 'Tape list'))
    check(name+'-two-columns', len({bounds(n)[0] for n in rows})==2)
    check(name+'-no-player', not any(desc(n)=='Cassette player' for n in ns))
    check(name+'-spines-fit-half-page', all(shape(n)[0]<(area[2]-area[0])*.51 for n in rows))
    all_names.update(re.sub(r'^(?:Current mixtape |Mixtape )','',desc(n)) for n in rows)
check('all-13-tapes-reachable-by-grid-scrolling',len(all_names)==13,names=sorted(all_names))
for name in ['after-11-portrait-library','after-14-portrait-library-retained']:
    ns=nodes(name)
    check(name+'-one-column', len({bounds(n)[0] for n in tapes(ns)})==1)
    check(name+'-no-player', not any(desc(n)=='Cassette player' for n in ns))
check('portrait-library-scroll-retained-on-rotation',shelf_snapshot('after-11-portrait-library')==shelf_snapshot('after-14-portrait-library-retained'))
check('landscape-grid-scroll-retained-on-rotation',shelf_snapshot('after-13-landscape-library-bottom')==shelf_snapshot('after-15-landscape-library-retained'))

before=nodes('before-first-tape-shelf'); after=nodes('after-02-first-landscape-playing')
def first_gap(ns):
    current=next(n for n in tapes(ns) if desc(n).startswith('Current mixtape '))
    return bounds(current)[1]-bounds(find(ns,'Tape list'))[1]
check('removed-old-half-screen-padding',first_gap(after)<first_gap(before),before_px=first_gap(before),after_px=first_gap(after))
result=dict(passed=len(checks),failures=0,checks=checks)
(ROOT/'layout-checks.json').write_text(json.dumps(result,indent=2)+'\n')
print(f'{len(checks)} device layout checks passed.')
