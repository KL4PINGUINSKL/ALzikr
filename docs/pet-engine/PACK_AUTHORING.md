# Writing a pet pack

A pack is a zip with `pack.json` at its root and picture files beside it. No code.

## Manifest (schema 1)

```json
{
  "schema": 1,
  "id": "spring-friends",            // a-z 0-9 . _ - ; stable forever
  "version": 1,                       // integer; every update must be higher
  "title": "أصدقاء الربيع",
  "licenses": [
    { "id": "cc-by-4", "name": "CC BY 4.0", "url": "https://creativecommons.org/licenses/by/4.0/", "attribution": true, "commercial": true }
  ],
  "season": { "fromMonth": 9, "fromDay": 1, "toMonth": 10, "toDay": 3 },   // optional, Hijri, for seasonal packs
  "species": [
    {
      "id": "CAT",                      // must be one of the app's animals in schema 1
      "skins": [
        {
          "id": "spring-cat",           // unique across all packs
          "name": "قطة الربيع",
          "by": "Author Name",          // required when the license asks for attribution
          "license": "cc-by-4",
          "page": "https://source.example/item",
          "rarity": "rare",             // common | rare | epic
          "unlock": { "level": 6 },
          "fit": { "zoom": 1.1, "lift": 0.05, "darkLines": false },
          "states": {
            "idle":  { "kind": "lottie", "path": "cat/idle.json" },
            "walk":  { "kind": "sprite", "path": "cat/walk.png", "cols": 6, "rows": 2, "frames": 12, "fps": 12 },
            "sleep": { "kind": "webp",   "path": "cat/sleep.webp" },
            "happy": { "kind": "frames", "paths": ["cat/h0.png", "cat/h1.png", "cat/h2.png"], "fps": 10, "motion": "hop" },
            "wave":  { "kind": "dotlottie", "path": "cat/wave.lottie", "from": 0.0, "to": 0.5 }
          },
          "fallback": { "kind": "png", "path": "cat/still.png" }
        }
      ]
    }
  ],
  "files": { "cat/idle.json": "<sha256>", "cat/walk.png": "<sha256>", "…": "…" }
}
```

Rules
- Paths are relative, without `..`, `:` or backslashes; the extension must fit the kind.
- Every file in the zip (except `pack.json`) must appear in `files` with its lowercase SHA-256, and nothing else may be in the zip.
- Allowed types: `json, lottie, png, webp, riv, txt, md`. Max 3 MB per file, 40 MB per pack, 2,000 files.
- Each skin needs at least `idle` or a `fallback`. Give a `fallback` PNG for every skin: it is the widget/certificate
  picture of last resort and the picture shown under reduced motion when nothing else loads.
- States you do not provide fall back automatically (see ARCHITECTURE.md); provide the ones that matter most: idle, walk,
  sleep, happy.
- Lottie: no expressions (After Effects `$bm_rt`), no linked images (embed them), whole-number width/height.
- Licenses must be verified on the asset's own page. CC BY needs `by`; the app lists it on the «الشكر والرخص» page.
- Religious content never goes into packs, and no pack or level may lock worship or knowledge features.

## Publishing

1. Build the zip (see `design/pet-engine/make_test_pack.py` for a complete example that writes the hashes).
2. Upload it to a GitHub release of the ALzikr repo.
3. `python design/pet-engine/publish_pack.py <zip> <https download url> <repo>/petpacks` signs and updates `catalog.json`
   with the private key in `D:\ALzikr-keys` (never commit it).
4. Commit `petpacks/catalog.json` and `catalog.json.sig`. Phones see the pack under «حزم الأصدقاء ← البحث عن حزم».

To test on a device before publishing: copy the zip to the app's private `files/incoming/<name>.zip` with
`adb shell run-as com.alzikr.app …` and open the app with `--es petinstall <name>`; it goes through the same checks.
