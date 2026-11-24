"""Builds the built-in core pack (app/src/main/assets/pets/pack.json) from the migration baseline and the license reports.

  python make_core.py                 existing 110 skins only (step 2: no visible change)
  python make_core.py --with-new      + the user's 63 picks from pet-candidates5 (step 4)
  python make_core.py --with-new --prune   without the unclear looks in removed_skins.json (1.4.0)

Unlock levels reproduce the app exactly: species levels from Progress.speciesLevels, and look i of a species opens at
level 1 for i < 3, otherwise at level i - 1 (one new look per level, no cap). New picks continue that numbering.
"""
import json, os, re, sys, glob
HERE = os.path.dirname(os.path.abspath(__file__))
D = os.path.join(HERE, '..')
APP = r'D:\ALzikr\android\app\src\main\assets\pets'
base = json.load(open(os.path.join(HERE, 'baseline.json'), encoding='utf-8'))
art = open(r'D:\ALzikr\android\app\src\main\java\com\alzikr\app\PetArt.kt', encoding='utf-8').read()
about = dict(re.findall(r'^\s+(\w+)\("[^"]+", "([^"]+)"\)', art, re.M))

# Where each existing look came from: page, author, license (from the earlier download reports).
src = {}
tsv = {l.split('\t')[1].split('/')[-1].replace('.gif', ''): l.split('\t')[2].strip() for l in open(os.path.join(D, 'pet-candidates/chosen.tsv'), encoding='utf-8') if '\t' in l}
for row in json.load(open(os.path.join(D, 'pet-candidates/report.json'), encoding='utf-8')):
    label, code, lic = row[0], row[1], row[2]
    src[code] = {'page': tsv.get(code, ''), 'license': lic}
for f in ['pet-candidates2', 'pet-candidates3', 'pet-candidates4']:
    for r in json.load(open(os.path.join(D, f, 'report.json'), encoding='utf-8')):
        src[r['code']] = {'page': r['page'], 'license': r['license'], 'by': r['by'].strip()}

# Personality (0..1): energy, sleepy, playful, chatty, curious. Drives the friend's own behaviour.
personality = {
    'CAT': (.6, .5, .9, .5, .9), 'PENGUIN': (.6, .4, .7, .6, .6), 'ALBATROSS': (.8, .3, .5, .3, .7), 'SWALLOW': (.9, .3, .6, .7, .6),
    'RABBIT': (.8, .4, .8, .4, .7), 'SHEEP': (.4, .6, .5, .5, .4), 'CHICKEN': (.7, .4, .6, .8, .8), 'COW': (.3, .6, .4, .4, .3),
    'ELEPHANT': (.4, .5, .6, .4, .5), 'GIRAFFE': (.8, .3, .7, .4, .9), 'BIRD': (.8, .3, .6, .9, .6), 'BEAR': (.4, .9, .5, .4, .4),
    'PANDA': (.3, .8, .6, .3, .4),
}
LSL = 'lottie-simple'
licenses = [{'id': LSL, 'name': 'Lottie Simple License', 'url': 'https://lottiefiles.com/page/license', 'attribution': False, 'commercial': True}]

def look_level(i): return 1 if i < 3 else i - 1

# Looks removed for being unclear (python make_core.py --with-new --prune). Levels of the others do not move.
removed = set(json.load(open(os.path.join(HERE, 'removed_skins.json'), encoding='utf-8'))['ids']) if '--prune' in sys.argv else set()

new_by_species = {}
if '--with-new' in sys.argv:
    meta = json.load(open(os.path.join(HERE, 'new_skins.json'), encoding='utf-8'))
    for m in meta: new_by_species.setdefault(m['species'], []).append(m)

species = []
for s in base['species']:
    p = personality[s['id']]
    skins = []
    allk = [dict(k, new=False) for k in s['skins']] + [dict(k, new=True) for k in new_by_species.get(s['id'], [])]
    for i, k in enumerate(allk):
        if k['id'] in removed: continue
        info = src.get(k['id'], {})
        lic = (k.get('license') or info.get('license') or '')
        if lic != 'Lottie Simple License': raise SystemExit(f"{k['id']}: license not verified ({lic!r})")
        skin = {'id': k['id'], 'name': k['name'], 'by': k.get('by') or info.get('by', ''), 'license': LSL, 'page': k.get('page') or info.get('page', ''),
                'rarity': 'rare' if k['new'] else 'common', 'unlock': {'level': look_level(i)},
                'states': {'idle': {'kind': 'lottie', 'path': k['id'] + '.json'}}}
        fit = {}
        if k.get('zoom', 1.0) != 1.0: fit['zoom'] = k['zoom']
        if k.get('lift', 0.0) != 0.0: fit['lift'] = k['lift']
        if k.get('darkLines'): fit['darkLines'] = True
        if fit: skin['fit'] = fit
        # Built with After Effects expressions, which Lottie on Android does not run: they draw a dot. Kept for whoever chose them.
        if k['id'] in ('cat-n16', 'cat-n17', 'cat-n18', 'cat-n45', 'cat-n49'): skin['hidden'] = True
        skins.append(skin)
    species.append({'id': s['id'], 'ar': s['ar'], 'about': about.get(s['id'], ''), 'defaultName': s['defaultName'], 'unlock': {'level': s['unlockLevel']},
                    'personality': dict(zip(['energy', 'sleepy', 'playful', 'chatty', 'curious'], p)), 'skins': skins})

pack = {'schema': 1, 'id': 'core', 'version': 3 if removed else 2 if new_by_species else 1, 'title': 'أصدقاء ALzikr', 'builtin': True, 'licenses': licenses, 'species': species}
missing = [k['id'] for s in species for k in s['skins'] if not os.path.exists(os.path.join(APP, k['id'] + '.json'))]
if missing: raise SystemExit('missing asset files: ' + ', '.join(missing))
json.dump(pack, open(os.path.join(APP, 'pack.json'), 'w', encoding='utf-8', newline='\n'), ensure_ascii=False, indent=1)
print('core pack v%d: %d species, %d skins' % (pack['version'], len(species), sum(len(s['skins']) for s in species)))
nopage = [k['id'] for s in species for k in s['skins'] if not k['page']]
print('skins without a recorded page:', len(nopage), nopage[:20])
