"""Spreads the looks over the levels so that every level from 2 to MAX opens something (a friend or a look).

Writes android/app/src/main/assets/pets/levels.json ({"skins": {id: level}}), which the app applies over the packs'
own levels (PetPacks.registry). The packs and their generators stay untouched, so re-exporting them keeps this.

Kept where they are: the friends' own levels, each friend's looks up to its own level (its first look), every look of
level 1 (open to everyone today), the legendary looks (the Studio sets them) and hidden looks. The other looks keep
their order and move to fill the empty levels, never below their friend's level and never out of order.

  python design/pet-engine/spread_levels.py
"""
import json
from pathlib import Path

MAX = 125
PETS = Path(__file__).resolve().parents[2] / 'android' / 'app' / 'src' / 'main' / 'assets' / 'pets'
packs = [json.loads((PETS / p).read_text(encoding='utf-8')) for p in ('pack.json', 'originals/pack.json', 'legendary/pack.json')]

# The registry's merge: core species first, then built-in ones; each species' looks in pack order.
species, order = {}, []
for p in packs:
    for s in p['species']:
        if s['id'] not in species:
            species[s['id']] = {'level': (s.get('unlock') or {}).get('level', 1), 'skins': []}; order.append(s['id'])
        species[s['id']]['skins'] += [dict(k, pack=p['id']) for k in s.get('skins', [])]

def lv(k): return (k.get('unlock') or {}).get('level', 1)

fixed, movable = {}, []           # level -> count ; [(cur level, rank, species, skin)]
for l in (species[s]['level'] for s in order): fixed[l] = fixed.get(l, 0) + 1
rank = 0
for s in order:
    sl = species[s]['level']
    for k in species[s]['skins']:
        rank += 1
        if k['pack'] == 'legendary' or k.get('hidden') or lv(k) <= 1 or lv(k) <= sl:
            if not k.get('hidden'): fixed[lv(k)] = fixed.get(lv(k), 0) + 1
            continue
        movable.append((lv(k), rank, s, k['id']))
movable.sort()
gaps = [l for l in range(2, MAX + 1) if not fixed.get(l)]
assert len(movable) >= len(gaps), (len(movable), len(gaps))

# Slots: one per empty level, the rest stay at the levels of the earliest looks (the start keeps a few per level).
extra = [m[0] for m in movable[:len(movable) - len(gaps)]]
slots = sorted(extra + gaps)
new, last = {}, {}
for (cur, _, s, sid), slot in zip(movable, slots):
    l = max(slot, species[s]['level'], last.get(s, 0))   # never below the friend, never before an earlier look
    new[sid] = l; last[s] = l

# Check: every level opens something, and each friend's looks stay in order.
opened = dict(fixed)
for l in new.values(): opened[l] = opened.get(l, 0) + 1
missing = [l for l in range(2, MAX + 1) if not opened.get(l)]
for s in order:
    ls = [new.get(k['id'], lv(k)) for k in species[s]['skins'] if not k.get('hidden')]
    assert ls == sorted(ls), (s, ls)
changed = {k: v for k, v in new.items() if v != next(lv(x) for s in order for x in species[s]['skins'] if x['id'] == k)}
(PETS / 'levels.json').write_text(json.dumps({'note': 'Made by design/pet-engine/spread_levels.py', 'skins': new}, ensure_ascii=False, indent=1), encoding='utf-8')
print(f'{len(movable)} looks spread over {len(gaps)} empty levels; {len(changed)} moved; still empty: {missing}')
per = {l: opened.get(l, 0) for l in range(1, MAX + 1)}
print('per level:', per)
