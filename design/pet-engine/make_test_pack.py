"""A device test pack for the sprite / frames / animated-WebP / still-PNG renderers (not shipped).
Pictures come from the GIF preview of cat-m4 «Cute Cat» by Omar (LottieFiles, Lottie Simple License)."""
import json, os, io, zipfile, hashlib
from PIL import Image, ImageSequence
HERE = os.path.dirname(os.path.abspath(__file__))
gif = Image.open(os.path.join(HERE, '..', 'pet-candidates5', 'small', 'cat-m4.gif'))
frames = [f.convert('RGBA') for f in ImageSequence.Iterator(gif)][:16]
# The preview has a white ground: make it transparent so the test also checks alpha.
def clear(im):
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if r > 245 and g > 245 and b > 245: px[x, y] = (255, 255, 255, 0)
    return im
frames = [clear(f) for f in frames]
w, h = frames[0].size
cols = 4; rows = (len(frames) + cols - 1) // cols
sheet = Image.new('RGBA', (w * cols, h * rows), (0, 0, 0, 0))
for i, f in enumerate(frames): sheet.paste(f, ((i % cols) * w, (i // cols) * h))
files = {}
def put(name, im, **kw):
    b = io.BytesIO(); im.save(b, **kw); files[name] = b.getvalue()
put('img/sheet.png', sheet, format='PNG')
for i, f in enumerate(frames[:8]): put(f'img/f{i}.png', f, format='PNG')
b = io.BytesIO(); frames[0].save(b, format='WEBP', save_all=True, append_images=frames[1:], duration=80, loop=0, lossless=False, quality=85); files['img/anim.webp'] = b.getvalue()
put('img/still.png', frames[0], format='PNG')
author = 'Omar'; page = 'https://lottiefiles.com/animations/cute-cat'
skins = [
    {'id': 'test-sprite', 'name': 'اختبار: صفحة إطارات', 'by': author, 'license': 'lsl', 'page': page,
     'states': {'idle': {'kind': 'sprite', 'path': 'img/sheet.png', 'cols': cols, 'rows': rows, 'frames': len(frames), 'fps': 12},
                'happy': {'kind': 'frames', 'paths': [f'img/f{i}.png' for i in range(8)], 'fps': 16, 'motion': 'hop'}},
     'fallback': {'kind': 'png', 'path': 'img/still.png'}},
    {'id': 'test-webp', 'name': 'اختبار: WebP متحرك', 'by': author, 'license': 'lsl', 'page': page,
     'states': {'idle': {'kind': 'webp', 'path': 'img/anim.webp'}}, 'fallback': {'kind': 'png', 'path': 'img/still.png'}},
    {'id': 'test-png', 'name': 'اختبار: صورة ثابتة', 'by': author, 'license': 'lsl', 'page': page, 'fallback': {'kind': 'png', 'path': 'img/still.png'}},
]
manifest = {'schema': 1, 'id': 'engine-test', 'version': 1, 'title': 'حزمة اختبار المحرّك',
            'licenses': [{'id': 'lsl', 'name': 'Lottie Simple License', 'url': 'https://lottiefiles.com/page/license'}],
            'species': [{'id': 'CAT', 'ar': 'قطة', 'skins': skins}],
            'files': {k: hashlib.sha256(v).hexdigest() for k, v in files.items()}}
out = os.path.join(HERE, 'engine-test.zip')
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as z:
    z.writestr('pack.json', json.dumps(manifest, ensure_ascii=False, indent=1))
    for k, v in files.items(): z.writestr(k, v)
print(out, os.path.getsize(out), {k: len(v) for k, v in files.items() if not k.startswith('img/f')})
