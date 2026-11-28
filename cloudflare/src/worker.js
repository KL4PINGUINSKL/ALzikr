// alzikr.alpharmasy.com: serves the R2 bucket "alzikr-downloads" (page, latest.json, APKs),
// and accepts chunked (multipart) uploads from the release script, guarded by the UPLOAD_KEY secret.
const TYPES = { html: "text/html; charset=utf-8", json: "application/json; charset=utf-8", apk: "application/vnd.android.package-archive", png: "image/png", svg: "image/svg+xml", css: "text/css", js: "text/javascript", mp3: "audio/mpeg", sig: "text/plain; charset=utf-8", zip: "application/zip" };

export default {
  async fetch(req, env, ctx) {
    const url = new URL(req.url);
    if (url.pathname === "/stats") return stats(req, env);
    if (url.pathname === "/feedback") return feedback(req, env);
    if (url.pathname === "/reports") return reports(req, env);
    let key = decodeURIComponent(url.pathname.slice(1));
    if (url.pathname.startsWith("/_upload/")) return upload(req, env, url);
    if (req.method !== "GET" && req.method !== "HEAD") return new Response("Method not allowed", { status: 405 });
    if (key === "" ) key = "index.html";
    if (key === "download") key = "download.html";
    if (key === "about" || key === "about.html" || key === "index") key = "index.html";
    if (key === "ALzikr.apk") {   // fixed link: always the newest APK
      const l = await env.BUCKET.get("latest.json");
      if (!l) return new Response("Not found", { status: 404 });
      key = new URL((await l.json()).downloadUrl).pathname.slice(1);
    }
    const ext = key.split(".").pop().toLowerCase();
    const range = req.headers.get("range");
    const obj = req.method === "HEAD" ? await env.BUCKET.head(key) : await env.BUCKET.get(key, range ? { range: req.headers } : {});
    if (!obj) return new Response("Not found", { status: 404, headers: { "content-type": "text/plain; charset=utf-8" } });
    const h = new Headers();
    h.set("content-type", TYPES[ext] || "application/octet-stream");
    h.set("etag", obj.httpEtag);
    h.set("accept-ranges", "bytes");
    h.set("cache-control", ext === "apk" ? "public, max-age=86400" : "no-cache");
    if (ext === "apk") h.set("content-disposition", `attachment; filename="${key.split("/").pop()}"`);
    if (req.method === "HEAD") { h.set("content-length", String(obj.size)); return new Response(null, { headers: h }); }
    if (range && obj.range) {
      const off = obj.range.offset ?? 0, len = obj.range.length ?? obj.size - off;
      h.set("content-range", `bytes ${off}-${off + len - 1}/${obj.size}`);
      h.set("content-length", String(len));
      return new Response(obj.body, { status: 206, headers: h });
    }
    h.set("content-length", String(obj.size));
    if (ext === "apk") ctx.waitUntil(count(req, env, key));
    return new Response(obj.body, { headers: h });
  },
};

// ---- download counter (D1 "alzikr-stats"): full GETs of an APK only (no HEAD, no resumed range), never blocks the download.
async function count(req, env, key) {
  try {
    if (!env.STATS) return;
    const m = key.match(/ALzikr-(\d+(?:\.\d+)+)-Android\.apk$/);
    const version = m ? m[1] : key.split("/").pop();
    const ua = req.headers.get("user-agent") || "";
    const source = ua === "ALzikr" || ua.startsWith("ALzikr") ? "app" : "web";
    const country = (req.cf && req.cf.country) || "??";
    const day = new Date().toISOString().slice(0, 10);
    await env.STATS.prepare("INSERT INTO downloads (day, version, source, country, n) VALUES (?, ?, ?, ?, 1) ON CONFLICT (day, version, source, country) DO UPDATE SET n = n + 1")
      .bind(day, version, source, country).run();
  } catch (e) { /* counting must never break a download */ }
}

// ---- /stats: a private page (HTTP Basic password = STATS_KEY secret)
async function stats(req, env) {
  const auth = req.headers.get("authorization") || "";
  let pass = "";
  try { pass = atob(auth.replace(/^Basic\s+/i, "")).split(":").slice(1).join(":"); } catch (e) {}
  if (!env.STATS_KEY || pass !== env.STATS_KEY)
    return new Response("كلمة السر مطلوبة", { status: 401, headers: { "www-authenticate": 'Basic realm="ALzikr stats", charset="UTF-8"', "content-type": "text/plain; charset=utf-8" } });
  const q = (sql) => env.STATS.prepare(sql).all().then((r) => r.results);
  const [total, byVer, bySrc, byCountry, byDay] = await Promise.all([
    q("SELECT COALESCE(SUM(n),0) AS n, MIN(day) AS since FROM downloads"),
    q("SELECT version, SUM(n) AS n, SUM(CASE WHEN source='web' THEN n ELSE 0 END) AS web, SUM(CASE WHEN source='app' THEN n ELSE 0 END) AS app FROM downloads GROUP BY version ORDER BY n DESC"),
    q("SELECT source, SUM(n) AS n FROM downloads GROUP BY source ORDER BY n DESC"),
    q("SELECT country, SUM(n) AS n FROM downloads GROUP BY country ORDER BY n DESC LIMIT 30"),
    q("SELECT day, SUM(n) AS n, SUM(CASE WHEN source='web' THEN n ELSE 0 END) AS web, SUM(CASE WHEN source='app' THEN n ELSE 0 END) AS app FROM downloads WHERE day >= date('now','-60 day') GROUP BY day ORDER BY day DESC"),
  ]);
  let names; try { names = new Intl.DisplayNames(["ar"], { type: "region" }); } catch (e) {}
  const land = (c) => { try { return c && c !== "??" ? (names ? names.of(c) : c) : "غير معروف"; } catch (e) { return c; } };
  const flag = (c) => /^[A-Z]{2}$/.test(c || "") ? String.fromCodePoint(...[...c].map((x) => 0x1f1a5 + x.charCodeAt(0))) : "🌐";
  const src = (s) => s === "app" ? "تحديث من داخل التطبيق" : "صفحة التحميل (المتصفح)";
  const esc = (t) => String(t).replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]);
  const max = Math.max(1, ...byDay.map((d) => d.n));
  const rows = (list, f) => list.length ? list.map(f).join("") : `<tr><td colspan="4" class="muted">لا تحميلات بعد</td></tr>`;
  const t = total[0] || { n: 0 };
  const html = `<!doctype html><html lang="ar" dir="rtl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>تحميلات ALzikr</title><style>
:root{--bg:#0f1512;--card:#17201b;--ink:#e9efe9;--muted:#93a39a;--acc:#3fbf8f;--gold:#d9b25f;--line:#26332c}
@media (prefers-color-scheme: light){:root{--bg:#f6f4ee;--card:#fff;--ink:#1d2a23;--muted:#6b7a71;--acc:#1f7a5a;--gold:#a8812c;--line:#e4e0d4}}
body{margin:0;background:var(--bg);color:var(--ink);font:15px/1.6 system-ui,"Segoe UI",Tahoma,sans-serif}
main{max-width:760px;margin:auto;padding:20px 16px 40px}h1{font-size:22px;margin:0 0 4px}h2{font-size:16px;margin:0 0 10px;color:var(--gold)}
.card{background:var(--card);border:1px solid var(--line);border-radius:16px;padding:16px;margin-top:14px}
.big{font-size:44px;font-weight:700;color:var(--acc);line-height:1.1}.muted{color:var(--muted);font-size:13px}
table{width:100%;border-collapse:collapse}td,th{padding:7px 4px;border-bottom:1px solid var(--line);text-align:right;font-variant-numeric:tabular-nums}th{color:var(--muted);font-weight:500;font-size:13px}
td.n{text-align:left;font-weight:600}.bar{height:8px;border-radius:4px;background:var(--acc);min-width:2px}
</style></head><body><main>
<h1>تحميلات ALzikr من موقع Cloudflare</h1>
<div class="muted">يُحسب كل تحميل كامل لملف التطبيق مرة واحدة (استئناف التحميل لا يُحسب). لا تشمل GitHub وALhub.</div>
<div class="card"><div class="big">${t.n}</div><div class="muted">تحميلًا${t.since ? " منذ " + esc(t.since) : ""}</div></div>
<div class="card"><h2>المصدر</h2><table>${rows(bySrc, (r) => `<tr><td>${src(r.source)}</td><td class="n">${r.n}</td></tr>`)}</table></div>
<div class="card"><h2>حسب النسخة</h2><table><tr><th>النسخة</th><th>من الصفحة</th><th>من التطبيق</th><th style="text-align:left">المجموع</th></tr>${rows(byVer, (r) => `<tr><td>${esc(r.version)}</td><td>${r.web}</td><td>${r.app}</td><td class="n">${r.n}</td></tr>`)}</table></div>
<div class="card"><h2>حسب البلد</h2><table>${rows(byCountry, (r) => `<tr><td>${flag(r.country)} ${esc(land(r.country))}</td><td class="n">${r.n}</td></tr>`)}</table></div>
<div class="card"><h2>آخر ٦٠ يومًا</h2><table><tr><th>اليوم</th><th style="width:45%"></th><th>صفحة / تطبيق</th><th style="text-align:left">المجموع</th></tr>${rows(byDay, (r) => `<tr><td>${esc(r.day)}</td><td><div class="bar" style="width:${Math.round(100 * r.n / max)}%"></div></td><td class="muted">${r.web} / ${r.app}</td><td class="n">${r.n}</td></tr>`)}</table></div>
</main></body></html>`;
  return new Response(html, { headers: { "content-type": "text/html; charset=utf-8", "cache-control": "no-store", "x-robots-tag": "noindex" } });
}

// POST /_upload/start?key=K&type=T -> {uploadId}; PUT /_upload/part?key&id&n (body) -> {etag};
// POST /_upload/done?key&id (body [{partNumber,etag}]) ; PUT /_upload/put?key&type (small file)
async function upload(req, env, url) {
  if (!env.UPLOAD_KEY || req.headers.get("x-upload-key") !== env.UPLOAD_KEY) return new Response("Forbidden", { status: 403 });
  const q = url.searchParams, key = q.get("key"), op = url.pathname.slice(9);
  const meta = { httpMetadata: { contentType: q.get("type") || "application/octet-stream" } };
  if (op === "put") { const o = await env.BUCKET.put(key, req.body, meta); return Response.json({ etag: o.etag }); }
  if (op === "start") { const m = await env.BUCKET.createMultipartUpload(key, meta); return Response.json({ uploadId: m.uploadId }); }
  const m = env.BUCKET.resumeMultipartUpload(key, q.get("id"));
  if (op === "part") { const p = await m.uploadPart(Number(q.get("n")), req.body); return Response.json(p); }
  if (op === "done") { const o = await m.complete(await req.json()); return Response.json({ etag: o.etag, size: o.size }); }
  if (op === "abort") { await m.abort(); return Response.json({ ok: true }); }
  return new Response("Bad op", { status: 400 });
}

// ---- /feedback (POST from the app: a problem or a feature request) and /reports (private list, STATS_KEY password)
async function feedback(req, env) {
  if (req.method !== "POST") return new Response("Method not allowed", { status: 405 });
  try {
    const b = await req.json();
    const text = String(b.text || "").trim().slice(0, 4000);
    if (!text) return Response.json({ ok: false }, { status: 400 });
    const kind = ["bug", "idea", "other"].includes(b.kind) ? b.kind : "other";
    const country = (req.cf && req.cf.country) || "??";
    const ip = req.headers.get("cf-connecting-ip") || "";
    const day = new Date().toISOString().slice(0, 10);
    const who = ip ? Array.from(new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(ip + day)))).slice(0, 8).map((x) => x.toString(16).padStart(2, "0")).join("") : "";
    // At most 20 a day from one place (no flooding); the address itself is never kept.
    const n = await env.STATS.prepare("SELECT COUNT(*) AS n FROM feedback WHERE day = ? AND who = ?").bind(day, who).first("n");
    if (n >= 20) return Response.json({ ok: false, reason: "limit" }, { status: 429 });
    await env.STATS.prepare("INSERT INTO feedback (at, day, kind, text, contact, version, device, android, country, who) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
      .bind(new Date().toISOString(), day, kind, text, String(b.contact || "").slice(0, 200), String(b.version || "").slice(0, 20),
        String(b.device || "").slice(0, 80), String(b.android || "").slice(0, 10), country, who).run();
    return Response.json({ ok: true });
  } catch (e) { return Response.json({ ok: false }, { status: 500 }); }
}

async function reports(req, env) {
  const auth = req.headers.get("authorization") || "";
  let pass = "";
  try { pass = atob(auth.replace(/^Basic\s+/i, "")).split(":").slice(1).join(":"); } catch (e) {}
  if (!env.STATS_KEY || pass !== env.STATS_KEY)
    return new Response("كلمة السر مطلوبة", { status: 401, headers: { "www-authenticate": 'Basic realm="ALzikr reports", charset="UTF-8"', "content-type": "text/plain; charset=utf-8" } });
  const list = (await env.STATS.prepare("SELECT * FROM feedback ORDER BY at DESC LIMIT 300").all()).results;
  const esc = (t) => String(t ?? "").replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]);
  const label = { bug: "🐞 مشكلة", idea: "💡 اقتراح", other: "💬 أخرى" };
  const cards = list.length ? list.map((r) => `<div class="card"><div class="top"><b>${label[r.kind] || esc(r.kind)}</b><span class="muted">${esc(r.at.slice(0, 16).replace("T", " "))} · ${esc(r.country)} · ${esc(r.version)}${r.device ? " · " + esc(r.device) + " (Android " + esc(r.android) + ")" : ""}</span></div><p>${esc(r.text).replace(/\n/g, "<br>")}</p>${r.contact ? `<div class="muted">للرد: ${esc(r.contact)}</div>` : ""}</div>`).join("") : `<div class="card muted">لا رسائل بعد</div>`;
  const html = `<!doctype html><html lang="ar" dir="rtl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>رسائل ALzikr</title><style>
:root{--bg:#0f1512;--card:#17201b;--ink:#e9efe9;--muted:#93a39a;--acc:#3fbf8f;--line:#26332c}
@media (prefers-color-scheme: light){:root{--bg:#f6f4ee;--card:#fff;--ink:#1d2a23;--muted:#6b7a71;--acc:#1f7a5a;--line:#e4e0d4}}
body{margin:0;background:var(--bg);color:var(--ink);font:15px/1.7 system-ui,"Segoe UI",Tahoma,sans-serif}main{max-width:760px;margin:auto;padding:20px 16px 40px}
.card{background:var(--card);border:1px solid var(--line);border-radius:14px;padding:14px;margin-top:12px}.top{display:flex;flex-wrap:wrap;gap:8px;justify-content:space-between}
.muted{color:var(--muted);font-size:13px}p{margin:8px 0 4px;white-space:normal;overflow-wrap:anywhere}h1{font-size:22px;margin:0}</style></head><body><main>
<h1>رسائل المستخدمين (${list.length})</h1>${cards}</main></body></html>`;
  return new Response(html, { headers: { "content-type": "text/html; charset=utf-8", "cache-control": "no-store", "x-robots-tag": "noindex" } });
}
