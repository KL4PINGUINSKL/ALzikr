// Renders the scenes of «اقتراح ثيمات» (Codex's theme proposals) for the app: every scene in the seven lights of the day
// (fajr, sunrise, dhuhr, asr, maghrib, isha, midnight; the colors of INTEGRATION.md), and the seasons scene in its four
// foliages too. Out: design/themes/out/<scene>-<light>[-<season>].png, then make_codex_scenes.py turns them into WebP.
//   node design/themes/render_codex_scenes.cjs
const { chromium } = require(process.env.PW || 'playwright');
const fs = require('fs'), path = require('path');
const SRC = path.join(__dirname, '..', '..', 'اقتراح ثيمات', 'assets');
const OUT = path.join(__dirname, 'out');
const LIGHTS = {
  fajr: ['#585F8F', '#ACBED0', .5], sunrise: ['#97B4B8', '#F1C881', 0], dhuhr: ['#82BBBE', '#E8E6BD', 0], asr: ['#86A6A0', '#E5C18C', 0],
  maghrib: ['#AC738F', '#EAAF78', .15], isha: ['#14213D', '#435674', .85], midnight: ['#0B1630', '#263952', .85],
};
const SEASONS = { spring: '#D9A8AF', summer: '#729875', autumn: '#C79759', winter: '#E4EAE5' };
(async () => {
  fs.mkdirSync(OUT, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 960, height: 1200 }, deviceScaleFactor: 1 });
  for (const f of fs.readdirSync(SRC).filter(f => f.endsWith('.svg'))) {
    const scene = f.replace('.svg', ''); const svg = fs.readFileSync(path.join(SRC, f), 'utf8');
    for (const [light, [top, bottom, stars]] of Object.entries(LIGHTS)) {
      for (const [season, foliage] of scene === 'seasons' ? Object.entries(SEASONS) : [[null, null]]) {
        await page.setContent(`<html><body style="margin:0;background:#000"><div id="s" style="width:960px;height:1200px;--sky-top:${top};--sky-bottom:${bottom};--stars:${stars};${foliage ? '--foliage:' + foliage : ''}">${svg.replace('<svg ', '<svg width="960" height="1200" ')}</div></body></html>`);
        await page.locator('#s').screenshot({ path: path.join(OUT, `${scene}-${light}${season ? '-' + season : ''}.png`) });
      }
    }
    console.log('rendered', scene);
  }
  await browser.close();
})().catch(e => { console.error(e); process.exit(1); });
