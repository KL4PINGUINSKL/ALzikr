import json, re, subprocess
out = {}
pat = re.compile(r'"contentUrl":"(https://assets-v2\.lottiefiles\.com/a/[^"]+\.gif)","name":"([^"]*)".*?"acquireLicensePage":"([^"]+)".*?"creator":\{"@type":"Person","name":"([^"]*)"')
for q in ['cow', 'hippo', 'elephant', 'giraffe']:
    items, seen = [], set()
    for page in (1, 2):
        url = f'https://lottiefiles.com/free-animations/{q}' + (f'?page={page}' if page > 1 else '')
        html = subprocess.run(['curl', '-sL', '-A', 'Mozilla/5.0', url], capture_output=True).stdout.decode('utf-8', 'ignore').replace('\\"', '"').replace('\u002F', '/')
        for g, n, p, by in pat.findall(html):
            if p not in seen: seen.add(p); items.append({'name': n, 'gif': g, 'page': p, 'by': by})
    out[q] = items; print(q, len(items))
json.dump(out, open('search.json', 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
