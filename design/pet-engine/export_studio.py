"""Builds the built-in "studio" pack (assets/pets/studio/): the 49 friends of ALzikr Pet Studio's seven new collections
(Royal, Elemental, Prehistoric, Ocean, Cyber, Candy, Cosmic), read-only from the Studio folder.

Each friend: its painting awake and asleep (moved by the app's rig, like the legendary friends), the landmarks the Studio
placed (eyes, head, neck, tail, legs, the sleeping pose's points), its gait, and its painted home.

Levels: every level from 15 on opens one new friend. The 49 and the five legendary friends are put in the order of the
levels the Studio gave them (cheapest first) and take one level each, so the last one opens at 15 + 54 - 1 = 68.
The levels go to assets/pets/levels.json ("species"), which the app applies over the packs; the legendary looks move
with their friend (same spacing), and the new friends' looks get placed later with spread_levels.py.

  python design/pet-engine/export_studio.py
"""
import json, shutil, subprocess
from pathlib import Path
from PIL import Image

HERE = Path(__file__).resolve().parent
STUDIO = HERE.parent.parent / 'ALzikr Pet Studio'
PETS = HERE.parent.parent / 'android' / 'app' / 'src' / 'main' / 'assets' / 'pets'
OUT = PETS / 'studio'
FIRST = 15                       # levels 1..14 already open a friend each
COLLECTIONS = ['royal', 'elemental', 'prehistoric', 'ocean', 'cyber', 'candy', 'cosmic']

DUMP = r"""
const path=require('path'),root=process.argv[1],g=globalThis;
g.OriginalCatalog={defaults:{species:{},companions:[],upgrades:[]}};g.OriginalPlates=[];g.OriginalEnvironment={rooms:{}};g.OriginalMotion={gaits:{}};
const out=[];
for(const c of %s){require(path.join(root,c,'assets.js'));require(path.join(root,c,'register.js'));
 const k=Object.keys(g).find(k=>k.endsWith('Assets')&&g[k].models&&g[k].models[0]&&g[k].models[0].collection===c);
 for(const {src,sleepSrc,...m} of g[k].models){const s=g.OriginalCatalog.defaults.species[m.species]||{};out.push({...m,gait:s.gait,home:s.home,room:s.defaultUpgrade});}}
console.log(JSON.stringify({models:out,gaits:g.OriginalMotion.gaits}));
""" % json.dumps(COLLECTIONS)
data = json.loads(subprocess.run(['node', '-e', DUMP, str(STUDIO)], capture_output=True, text=True, check=True).stdout)

# The Studio's motion table (originals/motion.js) plus the collections' own gaits: cycle s, stride, lift, sway, bob.
GAITS = {'stalk': [1.34, 10, 6, 2, 2], 'waddle': [1.30, 8, 6, 8, 3], 'glide': [1.65, 10, 3, 2, 4], 'trot': [1.08, 11, 8, 3, 4],
         'stride': [1.65, 15, 9, 2, 3], 'hop': [1.22, 7, 8, 3, 10], 'lumber': [1.75, 9, 5, 5, 3], 'crawl': [2.4, 6, 3, 1, 1],
         'perch': [1.40, 6, 6, 4, 7], 'bound': [1.26, 12, 10, 3, 13], 'dart': [.85, 14, 9, 3, 10], 'heavy': [1.94, 7, 4, 2, 1.5],
         'peck': [1.04, 7, 8, 4, 5], 'roll': [1.62, 7, 5, 5, 3], 'amble': [1.65, 9, 5, 2, 2], 'orbit': [2.8, 6, 3, 3, 6]}
GAITS.update(data['gaits'])

# id: (Arabic name, about, default name, home's Arabic name)
AR = {
    'ROYAL_PEACOCK': ('الطاووس الملكي', 'يفرد ذيله المرصّع بالألوان', 'تاج', 'جناح الحديقة المرصّعة'),
    'ROYAL_LION': ('الأسد الذهبي', 'مهيب الطلعة، رحيم بأصحابه', 'ضرغام', 'قاعة الرخام المشمسة'),
    'ROYAL_TIGER': ('النمر الأبيض', 'يمشي بهدوء في قصر الثلج', 'ثلج', 'قصر الثلج الياقوتي'),
    'ROYAL_STAG': ('الأيّل الملكي', 'قرونه كأغصان الغابة', 'أغصان', 'بلاط الغابة الزمرّدي'),
    'ROYAL_PANTHER': ('الفهد الأسود', 'رشيق هادئ كليلة مقمرة', 'ليل', 'قصر القمر البنفسجي'),
    'ROYAL_EAGLE': ('العُقاب الملكي', 'يحلّق عاليًا فوق الجبال', 'باز', 'وكر الجبل'),
    'ELEMENTAL_SALAMANDER': ('سمندل النار', 'دافئ كجمرة لطيفة لا تحرق', 'جمرة', 'الجزيرة البركانية'),
    'ELEMENTAL_WOLF': ('ذئب الجليد', 'فروه يلمع كالصقيع', 'صقيع', 'عرين الجبل المتجمّد'),
    'ELEMENTAL_TURTLE': ('السلحفاة البلّورية', 'على ظهرها بلّورات مضيئة', 'بلّورة', 'حديقة كهف البلّور'),
    'ELEMENTAL_BUTTERFLY': ('فراشة الماس', 'جناحاها يعكسان الضوء كالماس', 'ماسة', 'الغابة المضيئة'),
    'ELEMENTAL_ELEPHANT': ('الفيل الحجري', 'ثابت كالصخر، حنون رفيق', 'صخر', 'الملاذ الحجري'),
    'ELEMENTAL_HORSE': ('حصان البرق', 'سريع كالبرق في المرج', 'برق', 'مرج العاصفة'),
    'PREHISTORIC_DODO': ('طائر الدودو', 'طائر قديم لا يطير، يمشي متمايلًا', 'دودو', 'بستان السرخس'),
    'PREHISTORIC_BRACHIOSAURUS': ('البراكيوصور الصغير', 'عنقه طويل يطال أعالي الشجر', 'سامق', 'وادي الديناصورات'),
    'PREHISTORIC_TYRANNOSAURUS': ('التيرانوصور الصغير', 'يقفز بحماس بذراعيه القصيرتين', 'رعد', 'كهف النهر'),
    'PREHISTORIC_MAMMOTH': ('الماموث الصوفي', 'صوفه الكثيف يدفئه في البرد', 'صوف', 'ملجأ العصر الجليدي'),
    'PREHISTORIC_TRICERATOPS': ('الترايسيراتوبس', 'ثلاثة قرون ودرع على رأسه', 'قرون', 'الفسحة المزهرة'),
    'PREHISTORIC_SABER_TIGER': ('النمر سيفيّ الأنياب', 'نابان طويلان وقلب طيب', 'ناب', 'عرين الوادي'),
    'PREHISTORIC_PTERANODON': ('التيرانودون', 'زاحف طائر بجناحين عريضين', 'شراع', 'وكر الشلال'),
    'PREHISTORIC_ANKYLOSAURUS': ('الأنكيلوصور', 'مدرّع بذيل كالمطرقة', 'درع', 'غابة الطحلب القديمة'),
    'OCEAN_OCTOPUS': ('الأخطبوط الصغير', 'ثمانية أذرع تحب الاستكشاف', 'مرجان', 'مغارة المرجان'),
    'OCEAN_DOLPHIN': ('الدلفين', 'ذكي مرح يقفز فوق الموج', 'موجة', 'البحيرة المشمسة'),
    'OCEAN_SEAHORSE': ('فرس البحر', 'يسبح منتصبًا بين الأعشاب', 'فُرَيس', 'حضانة عشب البحر'),
    'OCEAN_SHARK': ('القرش الصغير', 'سبّاح سريع بزعنفة شامخة', 'زعنف', 'ملجأ الشعاب'),
    'OCEAN_WHALE': ('الحوت الأزرق', 'أكبر المخلوقات، هادئ عظيم', 'محيط', 'قاعة المحيط'),
    'OCEAN_JELLYFISH': ('قنديل البحر المضيء', 'يتوهّج في أعماق البحر', 'قنديل', 'الكهف المتوهّج'),
    'OCEAN_ORCA': ('الأوركا', 'أسود وأبيض، يحب عائلته', 'قطب', 'القوس القطبي'),
    'OCEAN_NARWHAL': ('كركدن البحر', 'في رأسه ناب طويل كالرمح', 'رمح', 'قصر اللؤلؤ'),
    'OCEAN_CRYSTAL_OCTOPUS': ('الأخطبوط البلّوري', 'أذرعه شفافة تلمع كالجمشت', 'جمشت', 'السفينة الغارقة البنفسجية'),
    'CYBER_CAT': ('القطة الآلية', 'تموء بصوت معدني لطيف', 'ترس', 'شقة القطة الذكية'),
    'CYBER_PENGUIN': ('البطريق الآلي', 'يتمايل في محطة الأبحاث القطبية', 'بُرغي', 'محطة الأبحاث القطبية'),
    'CYBER_WOLF': ('الذئب الآلي', 'عيناه تضيئان كالمصابيح', 'ومضة', 'مرصد الجبل التقني'),
    'CYBER_PANDA': ('الباندا الآلية', 'تتدحرج في مختبر الخيزران', 'بكسل', 'مختبر الخيزران'),
    'CYBER_EAGLE': ('النسر المعدني', 'جناحاه من صفائح لامعة', 'فولاذ', 'محطة السماء'),
    'CYBER_DRAGON': ('التنين الميكانيكي', 'تروس نحاسية وقلب دافئ', 'نحاس', 'المرصد النحاسي'),
    'CANDY_RABBIT': ('أرنب المارشملو', 'طريّ كالحلوى، يقفز بخفّة', 'ملبّس', 'كوخ غيمة المارشملو'),
    'CANDY_BEAR': ('دب الشوكولاتة', 'بنّي حلو يحب الدفء', 'كاكاو', 'كوخ الكاكاو'),
    'CANDY_CAT': ('قطة الفراولة', 'وردية بلون الفراولة', 'فراولة', 'حديقة الفراولة'),
    'CANDY_PANDA': ('باندا البسكويت', 'مقرمشة لطيفة', 'بسكوتة', 'بيت البسكويت'),
    'CANDY_PENGUIN': ('بطريق المثلّجات', 'بارد حلو كالبوظة', 'بوظة', 'قلعة المثلّجات'),
    'CANDY_DRAGON': ('تنين الحلوى', 'بألوان قوس قزح والحلوى', 'قزح', 'وادي حلوى قوس قزح'),
    'COSMIC_MOON_OWL': ('بومة القمر', 'تسهر مع القمر في حديقته', 'هلال', 'الحديقة القمرية'),
    'COSMIC_STAR_STAG': ('أيّل النجوم', 'قرونه تحمل النجوم', 'ثريّا', 'مرصد النجوم'),
    'COSMIC_NEBULA_BUTTERFLY': ('فراشة السديم', 'جناحاها من غبار النجوم', 'سديم', 'مرج السديم'),
    'COSMIC_GALAXY_WHALE': ('حوت المجرّة', 'يسبح في محيط السماء', 'مجرّة', 'محيط السماء الطافي'),
    'COSMIC_AURORA_WOLF': ('ذئب الشفق', 'فروه يتلوّن كالشفق القطبي', 'شفق', 'عرين جبل الشفق'),
    'COSMIC_GALACTIC_BIRD': ('طائر الكواكب', 'يطير بين الكواكب', 'كوكب', 'قفص الكواكب'),
    'COSMIC_UNIVERSE_TURTLE': ('سلحفاة الفضاء', 'على ظهرها نجوم وكواكب صغيرة', 'مدار', 'البركة السماوية'),
    'COSMIC_INFINITY_DRAGON': ('تنين المدارات', 'أبهى الأصدقاء وآخرهم', 'بهاء', 'قصر المدارات'),
}
PERS = {'royal': (.5, .4, .4, .4, .7), 'elemental': (.7, .3, .6, .4, .8), 'prehistoric': (.7, .4, .7, .4, .8), 'ocean': (.6, .4, .7, .5, .9),
        'cyber': (.7, .3, .6, .6, .9), 'candy': (.8, .4, .9, .6, .7), 'cosmic': (.5, .5, .5, .4, .9)}

def sid(s): return s.replace('-', '_')
def drawn(img):
    a = img.convert('RGBA').resize((400, 400)).getchannel('A').point(lambda v: 255 if v > 24 else 0)
    return list(a.getbbox() or (0, 0, 400, 400))

shutil.rmtree(OUT, ignore_errors=True)
for d in ('art', 'rigs', 'homes'): (OUT / d).mkdir(parents=True)
species, homes = [], {}
for m in data['models']:
    sp, col = sid(m['species']), m['collection']
    slug = m['id'].split('-', 1)[1]                    # royal-peacock -> peacock
    ar, about, name, room = AR[sp]
    src = STUDIO / col / 'art'
    art, sleep = src / f'{slug}.webp', src / f'{slug}-sleep.webp'
    shutil.copy(art, OUT / 'art' / f'{m["id"]}.webp'); shutil.copy(sleep, OUT / 'art' / f'{m["id"]}-sleep.webp')
    v, s2 = drawn(Image.open(art)), drawn(Image.open(sleep))
    rig = {'gait': m['gait'], 'g': GAITS[m['gait']], 'art': f'studio/art/{m["id"]}.webp', 'sleep': f'studio/art/{m["id"]}-sleep.webp',
           'view': [min(v[0], s2[0]), min(v[1], s2[1]), max(v[2], s2[2]), max(v[3], s2[3])], 'species': sp,
           'bounds': m['bounds'], 'eyes': m['eyes'], 'eyeRadius': m['eyeRadius'], **{n: m[n] for n in ('head', 'neck', 'tail', 'near', 'far', 'tip') if n in m},
           'rest': m['sleep']}
    (OUT / 'rigs' / f'{m["id"]}.json').write_text(json.dumps(rig), encoding='utf-8')
    states = {st: {'kind': 'rig', 'path': f'studio/rigs/{m["id"]}.json', 'anim': st, 'loop': True} for st in ('idle', 'walk', 'happy', 'sleep')}
    species.append({'id': sp, 'ar': ar, 'about': about, 'defaultName': name, 'unlock': {'level': m['unlockLevel']}, 'collection': col,
                    'personality': dict(zip(['energy', 'sleepy', 'playful', 'chatty', 'curious'], PERS[col])),
                    'skins': [{'id': m['id'], 'name': ar, 'by': 'ALzikr Pet Studio', 'license': 'alzikr-original', 'rarity': 'epic',
                               'unlock': {'level': m['unlockLevel']}, 'fallback': {'kind': 'png', 'path': f'studio/art/{m["id"]}.webp'}, 'states': states}]})
    shutil.copy(src / f'{m["room"]}.webp', OUT / 'homes' / f'{m["room"]}.webp')
    homes[sp] = {'name': room, 'path': f'studio/homes/{m["room"]}.webp'}

# One level each, from FIRST on, in the order of the Studio's levels (the legendary friends among them).
legendary = json.loads((PETS / 'legendary' / 'pack.json').read_text(encoding='utf-8'))
queue = [(s['unlock']['level'], i, s['id']) for i, s in enumerate(species)] + \
        [(s['unlock']['level'], 100 + i, s['id']) for i, s in enumerate(legendary['species'])]
queue.sort()
lv = {spid: FIRST + n for n, (_, _, spid) in enumerate(queue)}
for s in species:
    s['unlock'] = {'level': lv[s['id']]}
    for k in s['skins']: k['unlock'] = {'level': lv[s['id']]}
pack = {'schema': 1, 'id': 'studio', 'version': 1, 'title': 'أصدقاء الاستوديو', 'builtin': True,
        'licenses': [{'id': 'alzikr-original', 'name': 'ALzikr original artwork (ALzikr Pet Studio)', 'commercial': True}], 'species': species}
(OUT / 'pack.json').write_text(json.dumps(pack, ensure_ascii=False, indent=1), encoding='utf-8')
(OUT / 'room.json').write_text(json.dumps({'homes': homes, 'furniture': [], 'upgrades': []}, ensure_ascii=False, indent=1), encoding='utf-8')

# levels.json: the legendary friends' new levels, their looks moved with them (same spacing).
levels = json.loads((PETS / 'levels.json').read_text(encoding='utf-8'))
levels['species'] = {s['id']: lv[s['id']] for s in legendary['species']}
for s in legendary['species']:
    shift = lv[s['id']] - s['unlock']['level']
    for k in s['skins']: levels['skins'][k['id']] = k['unlock']['level'] + shift
(PETS / 'levels.json').write_text(json.dumps(levels, ensure_ascii=False, indent=1), encoding='utf-8')

# The Kotlin enum lines (Species) for PetArt.kt.
(HERE / 'studio_species.kt.txt').write_text('\n'.join(f'    {s["id"]}("{s["ar"]}", "{s["about"]}"),' for s in species) + '\n', encoding='utf-8')
size = sum(p.stat().st_size for p in OUT.rglob('*') if p.is_file())
print(f'{len(species)} friends; {size/1e6:.1f} MB; levels {FIRST}..{max(lv.values())}')
for _, _, spid in queue: print(lv[spid], spid)
