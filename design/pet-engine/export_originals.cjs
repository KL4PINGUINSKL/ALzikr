// Builds ALzikr's built-in "Originals" pack from the ALzikr Pet Studio sources, without changing the studio.
// It loads the studio's own modules (read-only) and bakes sprite sheets at a size fit for the phone stage,
// plus the rooms and furniture, into android/app/src/main/assets/pets/originals/.
//
//   node design/pet-engine/export_originals.cjs [px=256] [quality=80]
const path = require('node:path'), fs = require('node:fs');
const STUDIO = path.resolve(__dirname, '../../ALzikr Pet Studio');
const OUT = path.resolve(__dirname, '../../android/app/src/main/assets/pets/originals');
const req = require('node:module').createRequire(path.join(STUDIO, 'package.json'));
const { createCanvas, Path2D, loadImage } = req('@napi-rs/canvas'); global.Path2D = Path2D;
for (const n of ['plates', 'environment', 'catalog', 'motion', 'render', 'world', 'project']) require(path.join(STUDIO, 'originals', n + '.js'));
const PX = +(process.argv[2] || 256), Q = +(process.argv[3] || 80);
const names = require('./originals_ar.json');
const config = OriginalProject.config, E = OriginalEnvironment;

const enc = c => c.toBuffer('image/webp', Q);
function decode(uri) { return Buffer.from(uri.split(',')[1], 'base64'); }
function put(rel, buf) { const f = path.join(OUT, rel); fs.mkdirSync(path.dirname(f), { recursive: true }); fs.writeFileSync(f, buf); return buf.length; }

(async () => {
  fs.rmSync(OUT, { recursive: true, force: true });
  let bytes = 0; const species = new Map();
  for (const m of OriginalPlates) {
    const ar = names.skins[m.id] || (/[؀-ۿ]/.test(m.name) ? m.name : null); if (!ar) throw Error('no Arabic name for ' + m.id);
    const p = new OriginalRender.Puppet(m, await loadImage(m.src), createCanvas, await loadImage(m.sleepSrc));
    const gait = config.species[m.species].gait, style = OriginalWorld.makeHome().style;
    // The app animates the painting itself (mesh warp, same motion as the studio), so only the pictures and the
    // landmarks ship: standing art, sleeping art, and the closed-eye version the studio computes for blinks.
    bytes += put(`art/${m.id}.webp`, decode(m.src)); bytes += put(`art/${m.id}-sleep.webp`, decode(m.sleepSrc));
    const cc = createCanvas(p.size, p.size), cx = cc.getContext('2d'), id = cx.createImageData(p.size, p.size); id.data.set(p.closed); cx.putImageData(id, 0, 0);
    bytes += put(`art/${m.id}-closed.webp`, cc.toBuffer('image/webp', 90));
    // The drawn area (standing and sleeping, alpha > 24), in model units, so small places show the animal, not the artboard.
    const view = [400, 400, 0, 0];
    for (const img of [await loadImage(m.src), await loadImage(m.sleepSrc)]) {
      const c = createCanvas(400, 400), x = c.getContext('2d'); x.drawImage(img, 0, 0, 400, 400); const d = x.getImageData(0, 0, 400, 400).data;
      for (let y = 0; y < 400; y++) for (let xx = 0; xx < 400; xx++) if (d[(y * 400 + xx) * 4 + 3] > 24) { view[0] = Math.min(view[0], xx); view[1] = Math.min(view[1], y); view[2] = Math.max(view[2], xx + 1); view[3] = Math.max(view[3], y + 1); }
    }
    const rig = { view, gait, g: OriginalMotion.gaits[gait], art: `originals/art/${m.id}.webp`, sleep: `originals/art/${m.id}-sleep.webp`, closed: `originals/art/${m.id}-closed.webp`,
      eyes: m.eyes, eyeRadius: m.eyeRadius, head: m.head, neck: m.neck, tail: m.tail, near: m.near, far: m.far, tip: m.tip, bounds: m.bounds, sleepBounds: m.sleep && m.sleep.bounds };
    bytes += put(`rigs/${m.id}.json`, Buffer.from(JSON.stringify(rig)));
    const atlas = Object.fromEntries(['idle', 'walk', 'happy', 'sleep'].map(st => [st, { kind: 'rig', path: `originals/rigs/${m.id}.json`, anim: st, loop: true }]));
    // A still for places that cannot animate (fallback).
    const still = createCanvas(PX, PX), sc = still.getContext('2d'); sc.scale(PX / 400, PX / 400); p.draw(sc, 'idle', 0, style, [], null);
    bytes += put(`stills/${m.id}.webp`, enc(still));
    const level = Number(config.skins.find(s => s.id === m.id)?.level ?? 1);
    const skin = { id: 'originals-' + m.id, name: ar, by: 'ALzikr Pet Studio', license: 'alzikr-original', page: '', unlock: { level },
      fallback: { kind: 'png', path: 'originals/stills/' + m.id + '.webp' }, states: atlas };
    if (!species.has(m.species)) species.set(m.species, { id: m.species, skins: [] });
    species.get(m.species).skins.push(skin);
  }
  // Our own designs open after every LottieFiles look of the same animal: one per level after the highest one,
  // in the Studio's order.
  const core = JSON.parse(fs.readFileSync(path.join(OUT, '..', 'pack.json'), 'utf8'));
  for (const sp of species.values()) {
    sp.skins.sort((a, b) => a.unlock.level - b.unlock.level);
    const top = Math.max(0, ...(core.species.find(c => c.id === sp.id)?.skins || []).filter(k => !k.hidden).map(k => k.unlock.level));
    // An animal with none starts at its own unlock level, one new look per level after it.
    const own = core.species.find(c => c.id === sp.id)?.unlock?.level ?? names.species[sp.id]?.level ?? 1;
    sp.skins.forEach((k, i) => { k.unlock.level = top > 0 ? top + 1 + i : own + i; });
  }
  // Rooms and furniture (the room screen composes them in the app).
  for (const [sp, uri] of Object.entries(E.rooms)) bytes += put(`homes/${sp.toLowerCase()}.webp`, decode(uri));
  for (const f of config.furniture) bytes += put(`items/${f.id}.webp`, decode(E.props[f.id]));
  bytes += put('items/garden.webp', decode(E.props.garden)); bytes += put('items/curtain.webp', decode(E.props.curtain));
  const sizes = { floor: 190, bed: 114, toy: 55, plant: 87, window: 100, lamp: 47, table: 99, print: 72, garden: 123, mobile: 62, cushion: 73, curtain: 155 };
  const room = {
    homes: Object.fromEntries(Object.entries(config.species).map(([sp, s]) => [sp, { name: names.homes[sp], path: `originals/homes/${sp.toLowerCase()}.webp` }])),
    furniture: config.furniture.map(f => ({ id: f.id, name: names.furniture[f.id], level: f.level, only: f.only || null,
      x: f.x, y: f.y, scale: f.scale, size: sizes[f.id] || 128, path: `originals/items/${f.id}.webp` })),
    upgrades: config.upgrades.map(u => ({ id: u.id, level: u.level, layers: u.layers })),
  };
  put('room.json', Buffer.from(JSON.stringify(room, null, 1)));
  const extra = Object.entries(names.species).map(([id, s]) => ({ id, ar: s.ar, about: s.about, defaultName: s.name, unlock: { level: s.level }, personality: s.personality }));
  const pack = { schema: 1, id: 'originals', version: 1, title: 'أصدقاء الذكر الأصليون', builtin: true,
    licenses: [{ id: 'alzikr-original', name: 'ALzikr original artwork (ALzikr Pet Studio)', commercial: true, url: '' }],
    species: [...species.values()].map(s => ({ ...(extra.find(e => e.id === s.id) || { id: s.id }), skins: s.skins })) };
  put('pack.json', Buffer.from(JSON.stringify(pack, null, 1)));
  console.log(JSON.stringify({ px: PX, q: Q, skins: OriginalPlates.length, mb: +(bytes / 1e6).toFixed(2) }));
})().catch(e => { console.error(e); process.exitCode = 1; });
