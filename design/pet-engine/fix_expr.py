"""Nastinator's cats (cat-n16/17/18/45/49) place their top layer with After Effects expressions, which Lottie on Android
ignores, so they drew a 1% dot. Replace the top layers' anchor/position/scale with the fixed values the expressions meant
(centre a 3600 px precomp in the 720 px canvas at 20%). Originals kept in originals/."""
import json, shutil, os, sys
src = sys.argv[1]; bak = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'originals')
os.makedirs(bak, exist_ok=True)
for f in ['cat-n16', 'cat-n17', 'cat-n18', 'cat-n45', 'cat-n49']:
    p = os.path.join(src, f + '.json')
    if not os.path.exists(os.path.join(bak, f + '.json')): shutil.copy(p, os.path.join(bak, f + '.json'))
    d = json.load(open(os.path.join(bak, f + '.json'), encoding='utf-8'))
    W, H = d['w'], d['h']
    for l in d['layers']:
        ks = l['ks']; lw = l.get('w', W); lh = l.get('h', H)
        ks['a'] = {'a': 0, 'k': [lw / 2, lh / 2]}; ks['p'] = {'a': 0, 'k': [W / 2, H / 2]}; ks['s'] = {'a': 0, 'k': [100 * W / lw, 100 * H / lh]}
    json.dump(d, open(p, 'w', encoding='utf-8'), separators=(',', ':'), ensure_ascii=False)
    print(f, 'fixed')
