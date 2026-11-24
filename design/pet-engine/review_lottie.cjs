// Contact sheets of every core (LottieFiles) look at a mid frame, with the app's fit, plus simple clarity numbers.
// node design/pet-engine/review_lottie.cjs <outDir>
const fs = require('node:fs'), path = require('node:path');
const STUDIO = path.resolve(__dirname, '../../ALzikr Pet Studio');
const req = require('node:module').createRequire(path.join(STUDIO, 'package.json'));
const { createCanvas, LottieAnimation } = req('@napi-rs/canvas');
const base = path.resolve(__dirname, '../../android/app/src/main/assets/pets');
const out = process.argv[2]; const pack = JSON.parse(fs.readFileSync(path.join(base, 'pack.json')));
const S = 150, rows = [];
for (const g of pack.species) {
  const cols = 6, sheet = createCanvas(cols * S, Math.ceil(g.skins.length / cols) * (S + 18)), cx = sheet.getContext('2d');
  cx.fillStyle = '#ffffff'; cx.fillRect(0, 0, sheet.width, sheet.height);
  g.skins.forEach((k, i) => {
    const raw = fs.readFileSync(path.join(base, k.states.idle.path), 'utf8');
    const a = LottieAnimation.loadFromData(raw);
    const f = createCanvas(S, S), c = f.getContext('2d'); const z = (k.fit && k.fit.zoom) || 1, lift = (k.fit && k.fit.lift) || 0;
    a.seek(.45); c.save(); c.translate(S * (1 - z) / 2, S * (1 - z) / 2 - lift * S); c.scale(z, z); a.render(c, { x: 0, y: 0, width: S, height: S }); c.restore();
    const d = c.getImageData(0, 0, S, S).data; let n = 0, x0 = S, y0 = S, x1 = 0, y1 = 0;
    for (let y = 0; y < S; y++) for (let x = 0; x < S; x++) if (d[(y * S + x) * 4 + 3] > 40) { n++; x0 = Math.min(x0, x); x1 = Math.max(x1, x); y0 = Math.min(y0, y); y1 = Math.max(y1, y); }
    const x = i % cols * S, y = Math.floor(i / cols) * (S + 18);
    cx.fillStyle = '#eef0ea'; cx.fillRect(x + 2, y + 2, S - 4, S - 4); cx.drawImage(f, x, y);
    cx.fillStyle = k.hidden ? '#c00' : '#222'; cx.font = '13px sans-serif'; cx.fillText(k.id + ' L' + k.unlock.level, x + 4, y + S + 13);
    rows.push({ sp: g.id, id: k.id, level: k.unlock.level, hidden: !!k.hidden, cover: +(n / (S * S)).toFixed(3), box: n ? Math.max(x1 - x0, y1 - y0) / S : 0 });
  });
  fs.writeFileSync(path.join(out, g.id + '.png'), sheet.toBuffer('image/png'));
}
fs.writeFileSync(path.join(out, 'metrics.json'), JSON.stringify(rows, null, 1));
console.log(rows.length, 'looks; small (box<.45 or cover<.04):', rows.filter(r => r.box < .45 || r.cover < .04).map(r => r.id).join(' '));
