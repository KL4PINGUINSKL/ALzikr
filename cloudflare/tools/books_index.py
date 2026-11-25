"""Builds data/books.json: every Arabic book on IslamHouse (id, title, authors, pdf url, size, short description),
so the app's global search finds any book title without crawling the API on the phone. Run, then upload with:
python upload.py site/data/books.json data/books.json "application/json; charset=utf-8" """
import json, os, sys, time, urllib.request
API = "https://api3.islamhouse.com/v3/paV29H2gm56kvLPy"
out = []
page, last = 1, 1
while page <= last:
    for t in range(5):
        try:
            req = urllib.request.Request(f"{API}/main/books/ar/ar/{page}/50/json", headers={"User-Agent": "Mozilla/5.0 ALzikr-index"})
            with urllib.request.urlopen(req, timeout=60) as r: d = json.loads(r.read())
            break
        except Exception as e:
            print("retry", page, e, flush=True); time.sleep(3 * (t + 1))
    else: raise SystemExit(f"page {page} failed")
    last = d["links"]["pages_number"]
    for o in d["data"]:
        authors = "، ".join(a.get("title", "") for a in (o.get("prepared_by") or []) if a.get("title"))
        pdf = next((a for a in (o.get("attachments") or []) if str(a.get("extension_type", "")).upper() == "PDF"), None)
        desc = " ".join((o.get("description") or "").split())[:160]
        out.append([str(o["id"]), o.get("title", ""), authors, pdf.get("url") if pdf else "", pdf.get("size", "") if pdf else "", desc])
    print(page, "/", last, len(out), flush=True); page += 1; time.sleep(0.3)
os.makedirs(os.path.join(os.path.dirname(__file__), "..", "site", "data"), exist_ok=True)
p = os.path.join(os.path.dirname(__file__), "..", "site", "data", "books.json")
json.dump({"made": time.strftime("%Y-%m-%d"), "books": out}, open(p, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
print("wrote", p, os.path.getsize(p))
