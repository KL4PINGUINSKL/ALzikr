import json, os
os.chdir(r'D:\ALzikr\android\app\src\main\assets\pets')
# Background layers only (the animal stays): a white solid, a sky disc and its matte, a background card.
drop = {
    'penguin-7.json': ['White Solid 1'],
    'sheep-5.json': ['White Solid 1', 'Shape Layer 1', 'Shape Layer 2'],
    'sheep-8.json': ['BG'],
    'rabbit-9.json': ['bg'],
    'sheep-2.json': ['BG Céu', 'Matte Céu'],
}
for f, names in drop.items():
    d = json.load(open(f, encoding='utf-8'))
    before = len(d['layers'])
    d['layers'] = [l for l in d['layers'] if l.get('nm') not in names]
    json.dump(d, open(f, 'w', encoding='utf-8'), separators=(',', ':'), ensure_ascii=False)
    print(f, before, '->', len(d['layers']))
