"""Builds the built-in "legendary" pack (assets/pets/legendary/) from ALzikr Pet Studio/legendary (read-only).

The Studio has not marked the landmarks (legendary/landmarks.json) yet, so the rigs carry the pictures and the drawn
area only: the app moves the whole body (breathing, hop, sway, sleep picture). When landmarks.json appears, they are
copied into the rigs and the limbs move too; accessories are added then (they need the anchors).

  python design/pet-engine/export_legendary.py
"""
import json, os, shutil
from pathlib import Path
from PIL import Image

HERE = Path(__file__).resolve().parent
SRC = HERE.parent.parent / 'ALzikr Pet Studio' / 'legendary'
OUT = HERE.parent.parent / 'android' / 'app' / 'src' / 'main' / 'assets' / 'pets' / 'legendary'
col = json.loads((SRC / 'collection.json').read_text(encoding='utf-8'))
marks = json.loads((SRC / 'landmarks.json').read_text()) if (SRC / 'landmarks.json').exists() else {}

AR = {
    'species': {
        'CELESTIAL_DRAGON': ('التنين السماوي', 'ينساب في الهواء ويوجّه نفسه بذيله', 'سماء'),
        'WINGED_UNICORN': ('الحصان المجنّح', 'رشيق بجناحين من ريش وقرن لامع', 'جناح'),
        'GRIFFIN': ('الغريفين', 'رأس عُقاب وجسم أسد، حارس وفيّ', 'عُقاب'),
        'PHOENIX': ('العنقاء', 'طائر من ريش كاللهب الدافئ', 'شُعلة'),
        'COSMIC_DRAGON': ('التنين الكوني', 'يحمل النجوم في حراشفه', 'نجم'),
    },
    'skins': {
        'celestial-blue': 'لازوردي', 'emerald': 'زمرّد', 'golden': 'ذهبي', 'frost': 'جليدي', 'flame': 'لهب', 'lunar': 'قمري', 'crystal': 'بلّوري',
        'cloudsilk': 'حرير الغيم', 'briar-rose': 'ورد البرّ', 'glacier-plume': 'ريشة الجليد',
        'honeycrag': 'صخرة العسل', 'storm-ruff': 'العاصفة', 'copper-kestrel': 'صقر النحاس',
        'emberbloom': 'زهرة الجمر', 'blue-hour': 'الساعة الزرقاء', 'gilded-fan': 'المروحة المذهّبة',
        'deep-orbit': 'المدار العميق', 'nebula-veil': 'حجاب السديم', 'pulsar': 'النابض',
    },
    'rooms': {
        'Crystal Caves': 'كهوف البلّور', 'Mountain Castle': 'قلعة الجبل', 'Floating Islands': 'الجزر الطائرة',
        'Enchanted Meadow': 'المرج المسحور', 'Flower Garden': 'حديقة الزهر', 'Cloud Palace': 'قصر الغيم',
        'Mountain Nest': 'عشّ الجبل', 'Ancient Tower': 'البرج العتيق', 'Aerial Sanctuary': 'الملاذ الجوي',
        'Volcanic Shelf': 'الرفّ البركاني', 'Golden Garden': 'الحديقة الذهبية', 'Illuminated Palace': 'القصر المضيء',
        'Celestial Islands': 'الجزر السماوية', 'Starlit Lake': 'بحيرة النجوم', 'Cosmic Palace': 'القصر الكوني',
    },
    'furniture': {
        'Crystal cushion': 'وسادة بلّورية', 'Cloud pavilion bed': 'سرير جناح الغيم', 'Eagle nest bed': 'عشّ العُقاب',
        'Fireglass brazier': 'موقد الزجاج الناري', 'Moonstone chaise': 'أريكة حجر القمر', 'Quartz lantern': 'فانوس الكوارتز',
        'Rose trellis': 'عريشة الورد', 'Lookout telescope': 'منظار المراقبة', 'Golden citrus tree': 'شجرة الحمضيات الذهبية',
        'Brass orrery': 'مجسّم الأفلاك النحاسي', 'Alpine bonsai': 'بونساي جبلي', 'Blue peony planter': 'أصيص الفاوانيا الزرقاء',
        'Silver fir': 'التنّوب الفضي', 'Emberflower planter': 'أصيص زهر الجمر', 'Nebula fern': 'سرخس السديم',
    },
}
# Body rhythm (cycle s, stride, lift, sway, bob) for whole-body motion; gliders sway, the unicorn strides.
GAIT = {'CELESTIAL_DRAGON': ('glide', [1.65, 10, 3, 2, 4]), 'WINGED_UNICORN': ('stride', [1.65, 15, 9, 2, 3]), 'GRIFFIN': ('glide', [1.65, 10, 3, 2, 4]),
        'PHOENIX': ('glide', [1.65, 10, 3, 2, 4]), 'COSMIC_DRAGON': ('glide', [1.65, 10, 3, 2, 4])}
PERS = {'CELESTIAL_DRAGON': (.6, .4, .6, .4, .9), 'WINGED_UNICORN': (.8, .3, .7, .4, .7), 'GRIFFIN': (.7, .4, .5, .3, .8),
        'PHOENIX': (.7, .3, .6, .6, .7), 'COSMIC_DRAGON': (.5, .5, .5, .4, .9)}

def drawn(img):
    a = img.convert('RGBA').resize((400, 400)).getchannel('A').point(lambda v: 255 if v > 24 else 0)
    return list(a.getbbox() or (0, 0, 400, 400))

shutil.rmtree(OUT, ignore_errors=True)
(OUT / 'art').mkdir(parents=True); (OUT / 'rigs').mkdir(); (OUT / 'homes').mkdir(); (OUT / 'items').mkdir()
species = []
for sp in col['species']:
    ar, about, name = AR['species'][sp['id']]
    gait, g = GAIT[sp['id']]
    skins = []
    for k in sorted([k for k in col['skins'] if k['species'] == sp['id']], key=lambda k: k['level']):
        art, sleep = SRC / 'art' / (k['id'] + '.webp'), SRC / 'art' / (k['id'] + '-sleep.webp')
        shutil.copy(art, OUT / 'art' / art.name)
        rig = {'gait': gait, 'g': g, 'art': f'legendary/art/{art.name}'}
        if sleep.exists(): shutil.copy(sleep, OUT / 'art' / sleep.name); rig['sleep'] = f'legendary/art/{sleep.name}'
        v = drawn(Image.open(art))
        if sleep.exists():
            s2 = drawn(Image.open(sleep)); v = [min(v[0], s2[0]), min(v[1], s2[1]), max(v[2], s2[2]), max(v[3], s2[3])]
        rig['view'] = v
        m = marks.get(k['source'])
        if m:   # the Studio's proof is 200 px; model units are 400
            pt = lambda xy: [2 * xy[0], 2 * xy[1]]
            rig.update({'eyes': [pt(m[0][0:2]), pt(m[0][2:4])], 'eyeRadius': m[0][4] * 2, 'head': [2 * m[0][0], 2 * m[0][1] + 12],
                        **dict(zip(['neck', 'tail', 'near', 'far', 'tip'], [pt(x) for x in m[1:6]]))})
        (OUT / 'rigs' / (k['id'] + '.json')).write_text(json.dumps(rig), encoding='utf-8')
        states = {st: {'kind': 'rig', 'path': f'legendary/rigs/{k["id"]}.json', 'anim': st, 'loop': True} for st in ('idle', 'walk', 'happy', 'sleep')}
        skins.append({'id': k['id'], 'name': AR['skins'][k['source']], 'by': 'ALzikr Pet Studio', 'license': 'alzikr-original', 'rarity': 'epic',
                      'unlock': {'level': k['level']}, 'fallback': {'kind': 'png', 'path': f'legendary/art/{art.name}'}, 'states': states})
    species.append({'id': sp['id'], 'ar': ar, 'about': about, 'defaultName': name, 'unlock': {'level': sp['level']},
                    'personality': dict(zip(['energy', 'sleepy', 'playful', 'chatty', 'curious'], PERS[sp['id']])), 'skins': skins})
pack = {'schema': 1, 'id': 'legendary', 'version': 1, 'title': 'الأصدقاء الأسطوريون', 'builtin': True,
        'licenses': [{'id': 'alzikr-original', 'name': 'ALzikr original artwork (ALzikr Pet Studio)', 'commercial': True}], 'species': species}
(OUT / 'pack.json').write_text(json.dumps(pack, ensure_ascii=False, indent=1), encoding='utf-8')

# Rooms change with the level (three per animal); furniture by level for its own animal.
homes = {}
for i, sp in enumerate(col['species']):
    st = []
    for j, s in enumerate(sp['stages']):
        f = SRC / 'art' / f'legend-room-{i}-{j}.webp'
        shutil.copy(f, OUT / 'homes' / f.name)
        st.append({'name': AR['rooms'][s['name']], 'level': s['level'], 'path': f'legendary/homes/{f.name}'})
    homes[sp['id']] = {'name': st[0]['name'], 'path': st[0]['path'], 'stages': st}
furniture = []
for f in col['furniture']:
    shutil.copy(SRC / 'art' / (f['id'] + '.webp'), OUT / 'items' / (f['id'] + '.webp'))
    furniture.append({'id': f['id'], 'name': AR['furniture'][f['name']], 'level': f['level'], 'only': f['only'], 'x': f['x'], 'y': f['y'],
                      'scale': f['scale'], 'size': 128, 'path': f'legendary/items/{f["id"]}.webp'})
(OUT / 'room.json').write_text(json.dumps({'homes': homes, 'furniture': furniture, 'upgrades': []}, ensure_ascii=False, indent=1), encoding='utf-8')
size = sum(p.stat().st_size for p in OUT.rglob('*') if p.is_file())
print(f'{len(species)} species, {sum(len(s["skins"]) for s in species)} skins, landmarks for {len(marks)}; {size/1e6:.1f} MB')
