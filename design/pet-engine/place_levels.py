"""Places every friend and every look on the levels (assets/pets/levels.json: "species" and "skins"), which the app applies
over the packs (PetPacks.registry). The packs themselves stay untouched.

  * Levels 1..40: a new friend at every level (1..14 as they always were, then the Studio's friends and the legendary ones
    in the order of the Studio's levels).
  * After 40: a new friend every two or three levels (2, 3, 2, 3 …); the levels between open a look.
  * When the friends are all open, a look a level; the legendary friends' extra looks come last, at the highest levels.
  * Looks that already open by level 40 stay where they are, and every look of level 1 stays open to everyone.
  * Each friend's first look opens with it; its other looks keep their order and never open before it.

  python design/pet-engine/place_levels.py
"""
import json
from pathlib import Path

PETS = Path(__file__).resolve().parents[2] / 'android' / 'app' / 'src' / 'main' / 'assets' / 'pets'
DENSE = 40                     # one friend a level up to here
GAPS = [2, 3]                  # then a friend every 2 or 3 levels
files = ('pack.json', 'originals/pack.json', 'legendary/pack.json', 'studio/pack.json')
packs = [json.loads((PETS / p).read_text(encoding='utf-8')) for p in files]
old = json.loads((PETS / 'levels.json').read_text(encoding='utf-8'))

species, order = {}, []
for p in packs:
    for s in p['species']:
        if s['id'] not in species:
            species[s['id']] = {'level': old.get('species', {}).get(s['id'], (s.get('unlock') or {}).get('level', 1)), 'skins': []}; order.append(s['id'])
        species[s['id']]['skins'] += [dict(k, pack=p['id']) for k in s.get('skins', [])]
def cur(k): return old['skins'].get(k['id'], (k.get('unlock') or {}).get('level', 1))

# Friends: those of level 1..14 stay; the rest keep their order (by today's level) and take the new places.
line = sorted([s for s in order if species[s]['level'] > 14], key=lambda s: species[s]['level'])
lv, level, g = {}, 14, 0
for s in line:
    if level < DENSE: level += 1
    else: level += GAPS[g % len(GAPS)]; g += 1
    lv[s] = level
for s in order: species[s]['level'] = lv.get(s, species[s]['level'])
last_friend = max(lv.values())
friend_levels = {species[s]['level'] for s in order}

# Looks.
new, normal, legend = {}, [], []
for rank, s in enumerate(order):
    sl = species[s]['level']
    ks = [k for k in species[s]['skins'] if not k.get('hidden')]          # the order the app shows them in
    floor, moving, peak, first_done = 1, False, 0, sl <= 1
    for i, k in enumerate(ks):
        c = cur(k)
        if c <= 1 and not moving and floor <= 1: continue                # open to everyone
        if not first_done: new[k['id']] = floor = sl; first_done = True; continue   # opens with its friend
        if not moving and k['pack'] != 'legendary' and floor <= c <= DENSE: new[k['id']] = floor = c; continue
        moving = True; peak = max(peak, c)
        (legend if k['pack'] == 'legendary' else normal).append((peak, rank, i, k['id'], s))
normal.sort(); legend.sort(key=lambda x: (species[x[4]]['level'], x[0], x[1], x[2]))
slots = [l for l in range(DENSE + 1, last_friend) if l not in friend_levels]
nxt = last_friend
def slot():
    global nxt
    if slots: return slots.pop(0)
    nxt += 1; return nxt
lastof = {}
for _, _, _, kid, s in normal + legend:
    l = slot()
    l = max(l, species[s]['level'], lastof.get(s, 0))
    new[kid] = l; lastof[s] = l
last_look = max(new.values())

# Checks: every level up to the last opens something; each friend's looks open in order and never before it.
opened = {}
for s in order: opened[species[s]['level']] = opened.get(species[s]['level'], 0) + 1
for k, l in new.items(): opened[l] = opened.get(l, 0) + 1
missing = [l for l in range(2, last_look + 1) if not opened.get(l)]
assert not missing, missing
for s in order:
    ls = [new.get(k['id'], cur(k)) for k in species[s]['skins'] if not k.get('hidden')]
    assert ls == sorted(ls) and all(l >= species[s]['level'] or l <= 1 for l in ls), (s, ls)
out = {'note': 'Made by design/pet-engine/place_levels.py', 'species': {s: species[s]['level'] for s in order if s in lv}, 'skins': new}
(PETS / 'levels.json').write_text(json.dumps(out, ensure_ascii=False, indent=1), encoding='utf-8')
print(f'friends: one a level to {DENSE}, then every 2-3 levels; the last friend at {last_friend}')
print(f'looks placed: {len(normal)} moved, {len(legend)} legendary at the end; the last look at {last_look}')
print('friends after 40:', [(species[s]['level'], s) for s in line if lv[s] > DENSE])
