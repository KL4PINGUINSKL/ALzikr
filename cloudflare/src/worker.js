// alzikr.alpharmasy.com: serves the R2 bucket "alzikr-downloads" (page, latest.json, APKs),
// and accepts chunked (multipart) uploads from the release script, guarded by the UPLOAD_KEY secret.
const TYPES = { html: "text/html; charset=utf-8", json: "application/json; charset=utf-8", apk: "application/vnd.android.package-archive", png: "image/png", svg: "image/svg+xml", css: "text/css", js: "text/javascript", mp3: "audio/mpeg", sig: "text/plain; charset=utf-8", zip: "application/zip" };

export default {
  async fetch(req, env) {
    const url = new URL(req.url);
    let key = decodeURIComponent(url.pathname.slice(1));
    if (url.pathname.startsWith("/_upload/")) return upload(req, env, url);
    if (req.method !== "GET" && req.method !== "HEAD") return new Response("Method not allowed", { status: 405 });
    if (key === "" ) key = "index.html";
    if (key === "download" || key === "about") key += ".html";
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
    return new Response(obj.body, { headers: h });
  },
};

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
