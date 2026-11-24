# ALzikr Pet Engine: architecture

The friends ("صديقي") are data, not code. A versioned **pack manifest** describes species, skins, animation states,
evolution stages, personality, accessories, licenses and unlock requirements; the engine reads it and draws any skin in any
state, everywhere a friend appears. Nothing in a pack is executable.

## Components (package `com.alzikr.app.pets`)

| Component | File | Role | Android-free (unit-tested on the JVM) |
|---|---|---|---|
| Data model + parser | `PetModel.kt` | `Pack`, `SpeciesDef`, `Skin`, `Source`, `PetState`, `Stage`, `Personality`, `Accessory`, `Season`, `License`; `PackParser` (schema check, safe paths, duplicate ids) | yes |
| Animation State Machine | `PetLogic.kt` → `StateResolver`, `PetStateMachine` | 20 standard states, fallback chains to `idle`, code motion added on fallback, timed states with priorities | yes |
| Behaviour Engine | `PetLogic.kt` → `Behavior` | next state from personality, hour, mood, sleep window and stage; rare moves | yes |
| Evolution Engine | `PetLogic.kt` → `Evolution` | stages by level (صغير، فضولي، أليف، مميّز، رفيق قديم), scale, states allowed per stage | yes |
| Pet Memory | `PetLogic.kt` → `PetMemory`; storage in `Pet.remember/readingHour/memoryLine` | real events only (level, khatma, streak, reading hour) → plain sentences; never quotes Quran or hadith | yes (logic) |
| Asset Validator | `PetLogic.kt` → `AssetValidator` | existence, format magic, Lottie sanity (version, rate, size, timeline, layers, no linked images), size limits, SHA-256, license present, Rive refused | yes |
| Pack store (install/rollback/remove) | `PackStore.kt` → `PackStore` | safe zip extraction, hash checks, atomic switch, previous version kept for rollback | yes |
| Trusted catalog | `PackStore.kt` → `Catalog` | ECDSA P-256 signature over the catalog, https + host allow-list, schema filter | yes |
| Pet Pack Manager | `PetPacks.kt` | registry = core pack (assets) + installed packs; file access; catalog fetch; install/update/rollback/remove; local test install | needs Context |
| Performance Manager | `PetPerf.kt` | per-place frame budgets, battery saving, reduced motion (system + app), screen-off and not-visible suspension | needs Context |
| Renderers | `PetRender.kt` → `PetView`, `PetFrames` | Lottie JSON, dotLottie, sprite sheet, frame sequence, animated WebP, still PNG; one clock per view at the allowed fps; bitmap LRU (≤ 1/16 heap, ≤ 24 MB) | Compose |
| Pet Preview, Packs, Credits | `PetEngineUi.kt` | preview any skin × 20 states with the resolution shown; pack management and motion/battery switches; license registry | Compose |
| Unlocks and Rewards | `Progress.kt` (existing) reading pack data | species and skin levels, rarity, seasonal windows, kept choices | — |

App-side facades kept for compatibility: `PetLooks` (now built from the registry) and `PetLottie(...)` (now calls
`PetView`). Every existing call site (in-app stage, floating overlay, widget, kids section, rewards, certificate) goes
through them, so all places share the engine.

## Data

- Core pack: `app/src/main/assets/pets/pack.json` (schema 1, version 2): 13 species, 173 skins (110 existing + 63 new).
  Asset paths are relative to `assets/pets/`; the files did not move.
- Downloaded packs: `filesDir/petpacks/<id>/<version>/` with `active` pointing at the version in use.
- Stable identifiers: species ids are the existing enum names (`CAT`…); skin ids are the existing look ids (`cat-n3`…), so
  saved choices (`pet_look_<SPECIES>`), kept unlocks (`prog_keep_*`), XP, badges and friendship are untouched.

## States and fallbacks

**Four states since 1.4.0 (as in ALzikr Pet Studio): `idle, walk, happy, sleep`.** Other names in a pack are ignored. Formerly: `idle, walk, run, sleep, wake_up, happy, laugh, jump, wave, sit, play, talk, celebrate, curious, yawn, stretch, thinking,
appear, disappear, reading`. Each falls back along a chain that always ends at `idle` (e.g. `celebrate → jump → happy → idle`,
`wake_up → stretch → idle`, `reading → sit → idle`). When a state falls back, the engine adds the requested state's motion
by code (hop, bob, sway, tilt, slow, fast, fade), so a single-loop Lottie still "jumps". Last resort: the skin's still
`fallback`. Every core skin resolves every state (unit test).

## Performance

| Place | Frame budget | Saving mode |
|---|---|---|
| App (header/stage, room) | 60 fps | 30 |
| Floating overlay | 30 fps | 15 |
| Kids | 60 fps | 30 |
| Preview | 30 fps | 15 |
| Grids (pickers) | 12 fps, and only the chosen item moves | 8 |

- All friends read one shared clock per frame rate (`PetClock`), so they advance in the same frame; a clock runs only while a friend uses it.
- The in-app movement simulation now runs at the place budget instead of every display frame (up to 120 Hz).
- Reduced motion (phone setting "remove animations" or the app switch): still frames, no code motion; widget uses one frame.
- Screen off → nothing animates; the overlay stops while ALzikr is open; the app's friends stop while it is in the background.
- The widget still uses pre-rendered frames in a `ViewFlipper` (widgets cannot run Lottie); taps update text only.
- The overlay window keeps its rules: not focusable, touches only on the sprite, hidden during calls/quiet hours rules for
  bubbles, removable from its notification, and dragged to ✕ to hide.

## Packs: safety

Only pictures and data: `json, lottie, png, webp, riv, txt, md`. Every file must be listed in the manifest with its SHA-256;
the zip's SHA-256 must match the signed catalog; zip-slip paths, absolute paths and unknown types are refused; a pack cannot
claim to be built in; versions only move forward (rollback keeps the previous one). Install is staged and switched
atomically. The catalog URL is `https://raw.githubusercontent.com/KL4PINGUINSKL/ALzikr/main/petpacks/catalog.json`
(+ `.sig`); nothing is published there yet, so the list is empty.

## Not done yet (honest status)

- **Rive renderer**: not added. The schema reserves `kind: "riv"`, the validator refuses it, and the renderer shows the
  skin's still fallback. Adding it needs the `app.rive:rive-android` library and a benchmark against Lottie on the phone.
- **Accessories and inventory**: model, parser and validator only; no accessory art exists yet, so no rendering or
  "bag" screen.
- **Seasonal packs**: the `season` window (Hijri) is parsed and enforced for skins; no seasonal content exists yet.
- **Per-animal state animations**: current Lottie skins are single loops; real `walk/sleep/...` animations arrive with packs
  that have them (sprite/frames/WebP renderers are ready and were tested with a test pack).

## Added in 1.4.0 (app side, `PetHome.kt`, `PetUi.kt`)

- «صديقي» section in the bottom bar (the page stays in «المزيد»); the page has tabs: friend, look, room, memories, bag, settings.
- Interactions menu → `PetActions` → the stage (talk, play with the yarn ball, sit, sleep, wave); friendship only.
- `PetRoom` (time-of-day window, ten items by level, shown/hidden from the bag), `PetBag`, `PetAlbum` (real moments, share as a picture).
- Overlay modes `Pet.floatMode`: normal, mini (34 dp still icon, a tap opens it for 15 s), quiet (no bubbles, strolls or nudges).
- Quiet while reading: `QuietWhileReading()` in the Quran reader, reading mode, hadith reader, sharh and text reading.
- Children's progress is separate: `Progress` keys get a `kid_` prefix while `Pet.kids`.
- `Skin.hidden`: kept for whoever chose it, not offered (the five expression-based cats).
- One `PackStore` per process (a second instance cleared the staging area under a running install).

## ALzikr Pet Studio originals (built in, 1.4.0)

- `assets/pets/originals/` is a second built-in pack (`PetPacks.BUILTIN`): 32 original designs, two per animal, Arabic names,
  plus three new animals (سلحفاة level 12, ثعلب 13, بومة 14). Built-in packs may add animals; downloaded ones still cannot.
- Generated by `design/pet-engine/export_originals.cjs` (names in `originals_ar.json`). It loads the Studio's modules read-only;
  nothing in `ALzikr Pet Studio/` is changed.
- New source kind `rig` (`PetRig.kt`): the painting (400 px) is bent live with `drawBitmapMesh` around its landmarks (head,
  neck, feet, tail, ear/wing tip) using the Studio's own motion curves (idle, walk, happy, sleep; sleep uses its own painting;
  blinks blend a closed-eye picture). ~100 KB per design instead of ~1 MB of 256 px sprite sheets (the whole pack is 4.1 MB,
  sprites would have been ~30 MB), sharp at any size, at the place's frame budget. The widget takes frames over 4.6 s.
- The room (`PetRoom`) now composes the Studio's painted home for the animal, the light of the time of day, upgrades (string
  lights at level 5, canopy and planters at 10) and furniture by level (`room.json`), each piece shown/hidden from the bag.
- Not yet: accessories (the Studio's wardrobe) and per-pose art beyond the four motions.
