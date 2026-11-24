"""Reads PetLooks (PetLottie.kt) and Progress.speciesLevels as they are in the code, and writes the baseline the migration must equal."""
import re, json, sys
src = open(r'D:\ALzikr\android\app\src\main\java\com\alzikr\app\PetLottie.kt', encoding='utf-8').read()
prog = open(r'D:\ALzikr\android\app\src\main\java\com\alzikr\app\Progress.kt', encoding='utf-8').read()
pet = open(r'D:\ALzikr\android\app\src\main\java\com\alzikr\app\Pet.kt', encoding='utf-8').read()
art = open(r'D:\ALzikr\android\app\src\main\java\com\alzikr\app\PetArt.kt', encoding='utf-8').read()
body = src[src.index('val looks: Map<Species, List<Look>> = mapOf('):src.index('fun of(s: Species)')]
out = {}
for m in re.finditer(r'Species\.(\w+) to listOf\((.*?)\)\s*,?\s*(?=Species\.|\)\s*$)', body, re.S):
    sp, inner = m.group(1), m.group(2)
    looks = []
    for l in re.finditer(r'Look\("([^"]+)", "([^"]+)", "([^"]*)"((?:, \w+ = [^,)]+)*)\)', inner):
        extra = dict(re.findall(r'(\w+) = ([^,)]+)', l.group(4)))
        looks.append({'id': l.group(1), 'name': l.group(2), 'by': l.group(3),
                      'zoom': float(extra.get('zoom', '1f').rstrip('f')), 'lift': float(extra.get('lift', '0f').rstrip('f')),
                      'darkLines': extra.get('darkLines', 'false') == 'true'})
    out[sp] = looks
lv = dict(re.findall(r'Species\.(\w+) to (\d+)', prog[prog.index('val speciesLevels'):prog.index('fun speciesLevel(')]))
names = dict(re.findall(r'Species\.(\w+) -> "([^"]+)"', pet[pet.index('fun defaultName'):pet.index('fun defaultName') + 900]))
enum = re.search(r'enum class Species\((.*?)\)\s*\{(.*?);', art, re.S)
print('enum head:', enum.group(1) if enum else None)
species = re.findall(r'(\w+)\("([^"]+)"', enum.group(2)) if enum else []
base = {'species': [{'id': s, 'ar': ar, 'defaultName': names.get(s), 'unlockLevel': int(lv.get(s, 1)), 'skins': out.get(s, [])} for s, ar in species]}
json.dump(base, open(sys.argv[1], 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
print(len(species), 'species', sum(len(v) for v in out.values()), 'skins', {k: len(v) for k, v in out.items()})
