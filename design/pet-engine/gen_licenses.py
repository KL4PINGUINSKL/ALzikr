"""Writes docs/pet-engine/LICENSES.md (the asset license registry) from the core pack manifest."""
import json
p = json.load(open(r'D:\ALzikr\android\app\src\main\assets\pets\pack.json', encoding='utf-8'))
lic = {l['id']: l for l in p['licenses']}
rows = []
for s in p['species']:
    for k in s['skins']:
        rows.append(f"| `{k['id']}` | {s['ar']} | {k['name']} | {k['by'] or '—'} | {lic[k['license']]['name']} | {k['rarity']} | {k['unlock']['level']} | [page]({k['page']}) |")
out = ['# Pet asset license registry', '',
       f"Generated from `app/src/main/assets/pets/pack.json` (core pack v{p['version']}): {len(rows)} skins, {len(p['species'])} species.", '',
       '## Licenses', ''] + [f"- **{l['name']}** (`{l['id']}`): {l['url']} · attribution required: {'yes' if l['attribution'] else 'no'} · commercial use: {'yes' if l['commercial'] else 'no'}" for l in p['licenses']] + [
       '', 'Every skin\'s license was read from its own LottieFiles page when it was downloaded (reports in `design/pet-candidates*/report.json`).',
       'Two picks whose page did not state the license (swallow-m16, bear-m5) were deleted and are not in the app.', '',
       '## Skins', '', '| id | animal | name | author | license | rarity | unlock level | source |', '|---|---|---|---|---|---|---|---|'] + rows
open(r'D:\ALzikr\docs\pet-engine\LICENSES.md', 'w', encoding='utf-8', newline='\n').write('\n'.join(out) + '\n')
print(len(rows))
