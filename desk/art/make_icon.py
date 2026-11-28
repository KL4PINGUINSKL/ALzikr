"""ALzikr Desk's icon: the app's gold crescent and stars on royal blue (as android/.../ic_launcher_*.xml),
with a small gold bar chart in the corner (downloads and messages). Writes art/icon.png and ALzikrDesk.ico."""
import os
from PIL import Image, ImageDraw, ImageFilter, ImageChops

S = 1024; k = S / 108
HERE = os.path.dirname(os.path.abspath(__file__))

def P(x, y): return (x * k, y * k)

def radial(size, center, radius, stops):
    img = Image.new("RGBA", (size, size))
    px = img.load(); cx, cy = center
    for y in range(size):
        for x in range(size):
            t = min(1.0, ((x - cx) ** 2 + (y - cy) ** 2) ** .5 / radius)
            for (o0, c0), (o1, c1) in zip(stops, stops[1:]):
                if t <= o1:
                    f = (t - o0) / (o1 - o0) if o1 > o0 else 0
                    px[x, y] = tuple(int(c0[i] + (c1[i] - c0[i]) * f) for i in range(4)); break
    return img

base = radial(S // 4, (58 * k / 4, 48 * k / 4), 76 * k / 4,
              [(0, (0x1A, 0x32, 0x74, 255)), (.5, (0x0E, 0x1E, 0x4E, 255)), (1, (0x05, 0x0C, 0x26, 255))]).resize((S, S), Image.BICUBIC)
d = ImageDraw.Draw(base)
for x, y, r in [(28, 30, .9), (84, 26, .7), (86, 78, .8), (24, 74, .6), (40, 86, .7), (62, 24, .6)]:
    d.ellipse([P(x - r, y - r), P(x + r, y + r)], fill=(255, 255, 255, 90))

# soft glows
glow = Image.new("RGBA", (S, S), (0, 0, 0, 0)); g = ImageDraw.Draw(glow)
g.ellipse([P(22, 24), P(86, 88)], fill=(243, 210, 122, 40))
g.ellipse([P(63, 30), P(84, 51)], fill=(255, 244, 207, 90))
g.ellipse([P(73, 52), P(85, 64)], fill=(243, 210, 122, 80))
base = Image.alpha_composite(base, glow.filter(ImageFilter.GaussianBlur(S / 30)))

# the crescent: a circle less a smaller one, gold gradient
outer = Image.new("L", (S, S), 0); ImageDraw.Draw(outer).ellipse([P(50 - 23, 56 - 23), P(50 + 23, 56 + 23)], fill=255)
inner = Image.new("L", (S, S), 0); ImageDraw.Draw(inner).ellipse([P(59.83 - 19.5, 48.77 - 19.5), P(59.83 + 19.5, 48.77 + 19.5)], fill=255)
mask = ImageChops.subtract(outer, inner)
grad = Image.new("RGBA", (S, S)); gp = grad.load()
c0, c1, c2 = (0xFF, 0xEB, 0xB0), (0xE2, 0xB8, 0x50), (0xB8, 0x8A, 0x2A)
for y in range(0, S, 2):
    for x in range(0, S, 2):
        t = max(0, min(1, ((x / k - 34) * 32 + (y / k - 34) * 46) / (32 * 32 + 46 * 46)))
        a, b, f = (c0, c1, t / .55) if t < .55 else (c1, c2, (t - .55) / .45)
        c = tuple(int(a[i] + (b[i] - a[i]) * f) for i in range(3)) + (255,)
        gp[x, y] = c
        if x + 1 < S: gp[x + 1, y] = c
        if y + 1 < S: gp[x, y + 1] = c; gp[min(x + 1, S - 1), y + 1] = c
base.paste(grad, (0, 0), mask)

def star(cx, cy, r, col):
    pts = []
    import math
    for i in range(16):
        a = math.pi / 2 + i * math.pi / 8
        rr = r if i % 4 == 0 else (r * .22 if i % 2 == 1 else r * .32)
        pts.append(P(cx + rr * math.cos(a), cy - rr * math.sin(a)))
    ImageDraw.Draw(base).polygon(pts, fill=col)
star(73.5, 40.5, 8.5, (255, 244, 207, 255))
star(79, 58, 5, (243, 210, 122, 255))

# the badge: three gold bars on a dark round plate, bottom left
d = ImageDraw.Draw(base)
d.rounded_rectangle([P(16, 70), P(46, 96)], radius=6 * k, fill=(5, 12, 38, 235), outline=(243, 210, 122, 255), width=int(1.2 * k))
for i, h in enumerate([8, 14, 19]):
    x0 = 21 + i * 7.5
    d.rounded_rectangle([P(x0, 91 - h), P(x0 + 5, 91)], radius=1.2 * k, fill=(243, 210, 122, 255))

# rounded square like a Windows app tile
m = Image.new("L", (S, S), 0); ImageDraw.Draw(m).rounded_rectangle([0, 0, S - 1, S - 1], radius=S * .2, fill=255)
out = Image.new("RGBA", (S, S), (0, 0, 0, 0)); out.paste(base, (0, 0), m)
out.save(os.path.join(HERE, "icon.png"))
out.save(os.path.join(HERE, "..", "ALzikrDesk.ico"), sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
print("ok")
