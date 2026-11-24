# Pet Engine validation report (2026-11-24, ALzikr 1.3.6 test build, local only)

Device: Samsung SM-S928B (Android 15/One UI, 120 Hz), debug build installed over the previous 1.3.6 test build (user data kept).

## Verified

### Automated (JVM unit tests, `gradlew testDebugUnitTest`): 18 / 18 pass
- **Migration / regression**: the core pack equals the pre-engine baseline (`test/resources/pet_baseline.json`, dumped from
  the old `PetLooks` and `Progress.speciesLevels`): 13 species in order, Arabic names, default names, species unlock levels,
  and the 110 old skins in their order with the same id, name, author, zoom, lift, darkLines and unlock level. The live
  facade (`PetLooks`, `Progress.lookLevel(species, i)`, `Progress.speciesLevel`) agrees with the pack (LegacyParityTest).
- **Asset validation**: all 173 core skins are self-contained valid Lottie files within limits; every skin has a verified
  license and source page.
- **States**: every one of the 20 states resolves for every skin; chains end at idle without loops; fallback adds motion;
  still fallback works; state machine priorities and return-to-base.
- **Behaviour / evolution / memory / seasons**: sleep at night for a sleepy friend; energetic friend walks/plays at noon;
  a baby stage never shows states it may not; stage boundaries; memory speaks only from recorded events; Hijri windows
  that wrap the year.
- **Manifest safety**: newer schema, unsafe ids, `..` paths, executable extensions and duplicate skins are refused.
- **Pack store**: install → update → rollback → remove; refused: wrong zip hash, zip slip, `.dex`, unlisted file, file hash
  mismatch, a pack claiming to be built in, re-installing the same version; nothing half-installed after a refusal.
- **Catalog**: a catalog signed by `publish_pack.py` (openssl, ALzikr's key) verifies with the key in the app; a tampered
  catalog does not; non-https and unknown hosts are dropped.

### On the phone
- No crash: pet page, rewards, kids section, all-looks page, preview, packs, credits, fiqh, tajweed, stats, the five tabs,
  and going to the home screen (crash log empty).
- **All-looks grid** inspected in screenshots: old and new skins draw; the 63 new skins appear after the old ones.
- **Downloadable pack path**: a test pack (sprite sheet + frame sequence + animated WebP + still PNG) installed through
  `PackStore` on the device (version 1 active, files in place), listed on «حزم الأصدقاء» with its size; then removed.
- **Renderers**: sprite sheet and animated WebP animate (two screenshots differ), still PNG stays still.
- **Merge paths**: enabling them made «الدب الملوّح» (bear-m17) draw; it was blank before.
- **Performance, pet page, 12 s, `top` + `gfxinfo`** (same phone, same data):

  | | Before engine | After |
  |---|---|---|
  | CPU (one core) | 93–107 % (mean ≈ 96 %) | 67–77 % (mean ≈ 71 %) |
  | Frames rendered | 1,266 | 623 |
  | gfxinfo "janky" | 0 % | 41 % (90th pct 19 ms) |

  The frame count and CPU dropped because animations now run at their budget instead of every 120 Hz display frame.
  gfxinfo judges frames against the 120 Hz deadline, so frames that now carry several animation updates are counted as
  "janky" even though the intended rate is 30 fps. This is a real trade-off to watch: if motion looks less smooth to the
  user, raise `Place.APP` to 60.

## Not verified / known issues

- **Overlay performance**: not measured. The floating friend is switched off on the phone and I did not change the
  user's settings.
- **Battery drain over time**: not measured (needs a long run).
- **Reduced-motion and battery-saving switches**: implemented and wired; not exercised on the device.
- **Behaviour, evolution scale, memory sentences** in the real app: compiled and unit-tested, not observed on the device
  over time.
- **dotLottie**: renderer and validator written; no `.lottie` file was tested on the device.
- **Rive**: not implemented (see ARCHITECTURE.md). The controlled benchmark was not run.
- **Android versions other than 15**: not tested (no emulator run this time). The WebP renderer has an Android 8
  fallback (first frame) that was not exercised.
- **Pre-existing broken looks** (they were already broken before the engine; the engine did not cause them):
  - cat-n16, cat-n17, cat-n18, cat-n45, cat-n49 (Nastinator) draw only a dot: they are built with After Effects
    expressions, which Lottie on Android does not run. A repair of the top-level transforms did not help (inner layers use
    expressions too), so the files were restored unchanged. Recommendation: hide or replace these five.
  - 17 old and 5 new skins contain some expressions; most draw correctly (checked in the grid), but they may differ from
    their previews in details.
- **rabbit-m2** («الأرنب الصغير») shows a brand name («劲园bao») under the rabbit. It is a logo-like mark; the user may
  want to drop this skin.
- **Some new skins include a background scene** (e.g. cat-m10, cat-m16, sheep-m3, panda-m11, elephant-m1, penguin-m2);
  they were chosen as shown in the gallery and kept as is.
