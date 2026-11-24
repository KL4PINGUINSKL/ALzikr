"""Adds a pack zip to the signed catalog (petpacks/catalog.json + catalog.json.sig) for PetPacks.CATALOG_URL.

  python publish_pack.py <pack.zip> <download-url> [catalog-dir]

The zip's own manifest (pack.json) must list every file's SHA-256; the catalog carries the zip's SHA-256. The catalog is
signed with the private key in D:\ALzikr-keys (never commit it); the app holds only the public key.
"""
import json, sys, os, hashlib, zipfile, subprocess, base64
zp, url = sys.argv[1], sys.argv[2]
out = sys.argv[3] if len(sys.argv) > 3 else os.path.join(os.path.dirname(os.path.abspath(__file__)), 'catalog')
KEY = r'D:\ALzikr-keys\petpacks-private.pem'
data = open(zp, 'rb').read()
m = json.loads(zipfile.ZipFile(zp).read('pack.json'))
assert url.startswith('https://'), 'https only'
os.makedirs(out, exist_ok=True)
cp = os.path.join(out, 'catalog.json')
cat = json.load(open(cp, encoding='utf-8')) if os.path.exists(cp) else {'packs': []}
cat['packs'] = [p for p in cat['packs'] if p['id'] != m['id']] + [{
    'id': m['id'], 'version': m['version'], 'title': m.get('title', m['id']), 'schema': m['schema'],
    'url': url, 'sha256': hashlib.sha256(data).hexdigest(), 'size': len(data)}]
body = json.dumps(cat, ensure_ascii=False, indent=1).encode('utf-8')
open(cp, 'wb').write(body)
sig = subprocess.run(['openssl', 'dgst', '-sha256', '-sign', KEY, cp], capture_output=True, check=True).stdout
open(cp + '.sig', 'w').write(base64.b64encode(sig).decode())
print('catalog:', cp, [(p['id'], p['version']) for p in cat['packs']])
