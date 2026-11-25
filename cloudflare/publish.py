"""Publish ALzikr to alzikr.alpharmasy.com (Cloudflare R2 bucket alzikr-downloads, served by the alzikr-site Worker).

    python publish.py            render + upload the pages, latest.json and (if new) the current APK
    python publish.py --render   only write the files into site/ (no upload)

Sources: versionName/versionCode from android/app/build.gradle.kts, release notes from AboutAl.CHANGES,
dates from the GitHub releases (when reachable), uploaded APKs from apks.json.
The site keeps the newest KEEP APKs; older versions stay on the page with their notes only.
"""
import hashlib, html, json, os, re, subprocess, sys, datetime

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SITE = os.path.join(HERE, "site")
HOST = "https://alzikr.alpharmasy.com"
KEEP = 3
AR_MONTHS = ["يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"]


def read(p): return open(p, encoding="utf-8").read()


def gradle_version():
    g = read(os.path.join(ROOT, "android", "app", "build.gradle.kts"))
    return re.search(r'versionName = "([^"]+)"', g).group(1), int(re.search(r"versionCode = (\d+)", g).group(1))


def changes():
    src = read(os.path.join(ROOT, "android", "app", "src", "main", "java", "com", "alzikr", "app", "AboutAl.kt"))
    body = src[src.index("val CHANGES"):]
    out = []
    for m in re.finditer(r'"(\d+(?:\.\d+)+)" to listOf\((.*?)\)(?:,\s*\n|\s*\n\s*\))', body, re.S):
        notes = [s.encode("utf-8").decode("unicode_escape").encode("latin-1").decode("utf-8") if "\\u" in s else s
                 for s in re.findall(r'"((?:[^"\\]|\\.)*)"', m.group(2))]
        out.append((m.group(1), [n.replace('\\"', '"') for n in notes]))
    return out


def dates():
    path = os.path.join(HERE, "dates.json")
    known = json.load(open(path, encoding="utf-8")) if os.path.exists(path) else {}
    try:
        r = subprocess.run(["gh", "release", "list", "--repo", "KL4PINGUINSKL/ALzikr", "--limit", "200", "--json", "tagName,publishedAt"],
                           capture_output=True, text=True, timeout=60)
        for x in json.loads(r.stdout or "[]"):
            known.setdefault(x["tagName"].lstrip("v"), x["publishedAt"][:10])
    except Exception:
        pass
    json.dump(known, open(path, "w", encoding="utf-8"), indent=1, sort_keys=True)
    return known


def sha256(p):
    h = hashlib.sha256()
    with open(p, "rb") as f:
        for b in iter(lambda: f.read(1 << 20), b""): h.update(b)
    return h.hexdigest()


def ar_date(d):
    if not d: return ""
    y, m, dd = map(int, d.split("-"))
    return f"{dd} {AR_MONTHS[m - 1]} {y}"


def mb(n): return f"{n / 1048576:.1f} ميغابايت"


def esc(s): return html.escape(s, quote=True)


ICON = """<svg viewBox="0 0 64 64" role="img" aria-label="ALzikr"><defs><linearGradient id="sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#1B2B6B"/><stop offset="1" stop-color="#0A1238"/></linearGradient></defs>
<rect x="1" y="1" width="62" height="62" rx="15" fill="url(#sky)" stroke="#2C3E8F" stroke-width="2"/>
<path d="M38 14a19 19 0 1 0 0 36a15 15 0 1 1 0-36z" fill="#E7C46A"/>
<path d="M45 21l1.6 3.4 3.4 1.6-3.4 1.6L45 31l-1.6-3.4L40 26l3.4-1.6z" fill="#FFF3C4"/><path d="M50 34l1 2 2 1-2 1-1 2-1-2-2-1 2-1z" fill="#FFF3C4"/></svg>"""

FAVICON = "data:image/svg+xml," + ("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 64 64'><rect width='64' height='64' rx='14' fill='%230F1A4A'/>"
                                   "<path d='M38 14a19 19 0 1 0 0 36a15 15 0 1 1 0-36z' fill='%23E7C46A'/></svg>")

CSS = """
:root{--bg:#070B1F;--sunken:#050817;--panel:#0F1636;--raised:#18214A;--border:#25306A;--border-strong:#3B4A94;
--text:#EEF1FB;--text-2:#C3CAE6;--muted:#97A0C6;--gold:#E7C46A;--gold-text:#F0D58C;--blue:#5B7CFF;--focus:#8FA6FF;--success:#4FD189;
--sans:"Segoe UI",Tahoma,system-ui,-apple-system,"Noto Naskh Arabic",Arial,sans-serif;--mono:ui-monospace,"Cascadia Mono",Consolas,monospace}
*{box-sizing:border-box}
body{margin:0;min-height:100vh;padding:clamp(20px,5vw,56px) clamp(16px,4vw,32px);display:flex;flex-direction:column;align-items:center;gap:24px;
font-family:var(--sans);color:var(--text);background:var(--bg);line-height:1.75;
background-image:radial-gradient(760px 420px at 50% -12%,rgba(231,196,106,.13),transparent 70%),radial-gradient(560px 340px at 10% 108%,rgba(91,124,255,.12),transparent 70%);
background-attachment:fixed;-webkit-text-size-adjust:100%;overflow-wrap:break-word}
nav{width:100%;max-width:720px;display:flex;gap:8px;justify-content:center;flex-wrap:wrap}
nav a{color:var(--text-2);text-decoration:none;font-size:14px;font-weight:600;padding:6px 14px;border-radius:999px;border:1px solid var(--border)}
nav a[aria-current]{color:#1a1405;background:var(--gold);border-color:var(--gold)}
nav a:hover{border-color:var(--focus)}
.card{width:100%;max-width:720px;min-width:0;background:var(--panel);border:1px solid var(--border);border-radius:18px;padding:clamp(18px,5vw,40px);box-shadow:0 24px 60px rgba(0,0,0,.5)}
.brand{display:flex;align-items:center;gap:14px}.brand svg{flex:none;width:54px;height:54px}
.name{margin:0;font-size:clamp(28px,6vw,38px);font-weight:700;line-height:1.15}
.eyebrow{margin:2px 0 0;font-size:14px;font-weight:600;color:var(--gold-text)}
.lede{margin:20px 0 0;font-size:clamp(15px,2.4vw,16.5px);color:var(--text-2)}
h2.section{margin:34px 0 14px;font-size:15px;font-weight:700;color:var(--gold-text)}
.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:12px}
.tile{background:var(--sunken);border:1px solid var(--border);border-radius:12px;padding:14px 16px}
.tile b{display:block;color:var(--text);font-size:15px;margin-bottom:4px}.tile span{color:var(--muted);font-size:13.5px}
.btn{display:inline-flex;align-items:center;justify-content:center;gap:8px;font-size:16px;font-weight:700;padding:13px 24px;color:#fff;background:#16A34A;border-radius:10px;text-decoration:none;white-space:nowrap}
.btn:hover{background:#22C55E;box-shadow:0 6px 20px rgba(34,197,94,.3)}.btn svg{width:18px;height:18px}
.btn.ghost{background:var(--raised);color:var(--text-2);border:1px solid var(--border-strong);font-size:14px;padding:9px 18px}
.btn.ghost:hover{color:var(--text);border-color:var(--focus);box-shadow:none}
.cta{display:flex;gap:12px;align-items:center;flex-wrap:wrap;margin-top:22px}.cta small{color:var(--muted)}
.family{display:grid;gap:12px}
.app{display:grid;grid-template-columns:auto minmax(0,1fr);gap:4px 14px;align-items:center;background:var(--sunken);border:1px solid var(--border);border-radius:12px;padding:16px;text-decoration:none;color:inherit}
.app:hover{border-color:var(--focus)}.app .logo{grid-row:span 2;width:44px;height:44px;border-radius:12px;display:grid;place-items:center;font-weight:800;font-size:15px;color:#fff}
.app b{font-size:16px}.app span{color:var(--muted);font-size:13.5px}.app .here{color:var(--gold-text);font-size:12px;font-weight:700;margin-inline-start:6px}
.lede+.release,.lede+.release--current{margin-top:22px}
.release{background:var(--sunken);border:1px solid var(--border);border-radius:12px;padding:16px 18px;min-width:0}
.release+.release{margin-top:12px}.release--current{border-color:rgba(231,196,106,.55);padding:clamp(16px,4vw,22px)}
.release-head{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:6px 16px;align-items:center}
.release-title{margin:0;min-width:0;font-size:inherit}
.release-version{font-size:17px;font-weight:700}.release--current .release-version{font-size:20px}
.pill{display:inline-block;margin-inline-start:8px;vertical-align:2px;font-size:11.5px;font-weight:700;padding:1px 9px;border-radius:999px;color:#1a1405;background:var(--gold)}
.release-meta{display:block;margin-top:2px;font-size:13px;color:var(--muted)}
.release-file{grid-column:1/-1;margin:2px 0 0;font-family:var(--mono);font-size:12.5px;color:var(--text-2);overflow-wrap:anywhere;direction:ltr;text-align:right}
.hash{grid-column:1/-1;margin-top:6px;padding-top:10px;border-top:1px solid var(--border);min-width:0}
.hash-label{display:flex;align-items:center;justify-content:space-between;gap:12px;font-size:12.5px;color:var(--muted)}
.hash code{display:block;margin-top:6px;font-family:var(--mono);font-size:12px;line-height:1.7;color:var(--text-2);word-break:break-all;direction:ltr;text-align:left;user-select:all}
.copy{flex:none;font-family:inherit;font-size:12px;font-weight:600;color:var(--text-2);padding:3px 10px;border-radius:6px;border:1px solid var(--border-strong);background:var(--raised);cursor:pointer}
.copy:hover{color:var(--text);border-color:var(--focus)}.copy[data-done]{color:var(--success);border-color:var(--success)}
.notes{margin-top:12px}.notes>summary{cursor:pointer;width:fit-content;font-size:13.5px;font-weight:600;color:var(--gold-text)}
.notes ul{margin:10px 0 0;padding-inline-start:20px;font-size:14px;color:var(--text-2)}.notes li{margin:0 0 6px}.notes li::marker{color:var(--gold)}
.steps{margin:0;padding-inline-start:22px;color:var(--text-2);font-size:14.5px}
footer{font-size:13px;color:var(--muted);text-align:center}footer a{color:var(--gold-text);text-decoration:none}
:focus-visible{outline:2px solid var(--focus);outline-offset:3px}
@media (max-width:480px){.release-head{grid-template-columns:minmax(0,1fr)}.release-head .btn{justify-self:stretch}}
"""

DL_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 3v12"/><path d="m7 11 5 5 5-5"/><path d="M4 20h16"/></svg>'

FEATURES = [
    ("الصلاة والأذان", "مواقيت تُحسب على الهاتف بعشر طرق، أذان بأصوات حقيقية، تنبيه قبل الصلاة، القبلة بالبوصلة، وسجل الصلوات والقضاء."),
    ("الأذكار والأوراد", "أذكار حصن المسلم للصباح والمساء والنوم وبعد الصلاة والمناسبات، وأورادك الخاصة، والسبحة بعدّاد واهتزاز."),
    ("القرآن الكريم", "المصحف بصفحاته، والتلاوة بأصوات القرّاء آيةً آية، والتفاسير، والعلامات والملاحظات، وخطة ختمة بوردٍ يومي."),
    ("الحديث والكتب", "الأحاديث مع شروحها ودرجاتها، وكتب أهل السنة تُنزَّل وتُقرأ داخل التطبيق."),
    ("يومي", "مهام مرتبطة بالصلوات، وعادات، وملاحظات، وجلسات تركيز تتوقف عند الأذان."),
    ("صديقك", "صديق يرافقك ويكبر بعبادتك: مستويات وأصدقاء جدد وثيمات وأوسمة، وتحديات يومية لطيفة."),
    ("مواسم الطاعة", "أدلة رمضان والعشر الأواخر وذي الحجة وعرفة والعيدين والأيام البيض، والتقويم الهجري."),
    ("خفيف ويعمل دون إنترنت", "المواقيت والأذكار والمصحف تعمل دون اتصال، ولا إعلانات ولا حسابات."),
]

FAMILY = [
    ("ALzikr", "ذِكر: رفيق المسلم اليومي للصلاة والأذكار والقرآن — أندرويد", "/", "#1B2B6B", True),
    ("ALpharmaSy", "المساعد الصيدلي الذكي السوري: التفاعلات الدوائية والجرعات والبدائل والأدوية السورية للصيادلة والأطباء — ويب", "https://alpharmasy.com", "#0E8A6A", False),
    ("ALgeneral", "منصة مودات لعبة C&C Generals Zero Hour: إدارة المودات ومزامنتها للعب الجماعي ومحرر الخرائط — ويندوز", "https://algeneral.alpharmasy.com/download.html", "#6A4CF0", False),
]


def page(title, desc, current, body):
    tabs = [("/", "عن التطبيق"), ("/download.html", "التحميل والتحديثات")]
    nav = "".join(f'<a href="{h}"{" aria-current=page" if h == current else ""}>{t}</a>' for h, t in tabs)
    return f"""<!doctype html>
<html lang="ar" dir="rtl">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="color-scheme" content="dark">
<meta name="description" content="{esc(desc)}">
<title>{esc(title)}</title>
<link rel="icon" href="{FAVICON}">
<!-- GENERATED by cloudflare/publish.py. Edit the script, not this file. -->
<style>{CSS}</style>
</head>
<body>
<nav>{nav}</nav>
{body}
<footer>ALzikr من عائلة <b>ALprojects</b> · <a href="https://alpharmasy.com">ALpharmaSy</a> · <a href="https://algeneral.alpharmasy.com/download.html">ALgeneral</a></footer>
</body>
</html>
"""


def brand(sub):
    return f'<div class="brand">{ICON}<div><h1 class="name">ذِكر <span style="color:var(--muted);font-size:.55em">ALzikr</span></h1><p class="eyebrow">{sub}</p></div></div>'


def render_index(cur):
    tiles = "".join(f'<div class="tile"><b>{esc(t)}</b><span>{esc(d)}</span></div>' for t, d in FEATURES)
    fam = "".join(
        f'<a class="app" href="{h}"><div class="logo" style="background:{c}">{n[2:4].upper() if n != "ALzikr" else "ذ"}</div>'
        f'<b>{n}{"<span class=here>هذا التطبيق</span>" if here else ""}</b><span>{esc(d)}</span></a>' for n, d, h, c, here in FAMILY)
    body = f"""<main class="card">
{brand("رفيق المسلم اليومي · أندرويد")}
<p class="lede">ذِكر تطبيق خفيف يجمع ما يحتاجه المسلم في يومه: مواقيت الصلاة والأذان، والأذكار والأوراد، والقرآن الكريم بالتلاوة والتفسير، والحديث وشروحه، ومهامك وعاداتك — بلا إعلانات ولا حسابات، ويعمل أغلبه دون إنترنت.</p>
<div class="cta"><a class="btn" href="/ALzikr.apk">{DL_ICON}تحميل لأندرويد</a><a class="btn ghost" href="/download.html">كل الإصدارات وما الجديد</a>
<small>الإصدار {cur["version"]} · {mb(cur["size"])} · أندرويد 8 فما فوق</small></div>
<h2 class="section">ماذا في التطبيق</h2>
<div class="grid">{tiles}</div>
<h2 class="section">طريقة التثبيت</h2>
<ol class="steps"><li>اضغط «تحميل لأندرويد» وانتظر اكتمال الملف.</li><li>افتح الملف، وإن طُلب منك فاسمح للمتصفح بتثبيت التطبيقات.</li><li>اضغط «تثبيت». بعدها يبحث التطبيق عن التحديثات بنفسه.</li></ol>
<h2 class="section">🤲 لا تنسَنا من دعائك</h2>
<p class="lede" style="margin-top:0">إن نفعك هذا التطبيق فادعُ لصاحبه ولوالديه ولأهله وللمسلمين أجمعين بالمغفرة والرحمة والقبول، وأن يجعله الله صدقةً جاريةً لهم. وشاركه مع أهلك وأحبابك، فالدالّ على الخير كفاعله.</p>
<h2 class="section">عائلة ALprojects</h2>
<div class="family">{fam}</div>
</main>"""
    return page("ذِكر ALzikr — رفيق المسلم اليومي", "تطبيق ذِكر لأندرويد: مواقيت الصلاة والأذان والأذكار والقرآن والتفسير والحديث، من عائلة ALprojects.", "/", body)


def render_download(rels, apks):
    arts = []
    for i, (v, notes, d) in enumerate(rels):
        a = apks.get(v)
        cur = i == 0
        meta = " · ".join(x for x in ["أندرويد", ar_date(d), mb(a["size"]) if a else ""] if x)
        dl = (f'<a class="btn{"" if cur else " ghost"}" href="/{a["file"]}" aria-label="تحميل ذِكر {v}">{DL_ICON}{"تحميل لأندرويد" if cur else "تحميل"}</a>' if a else "")
        extra = ""
        if a:
            sid = "sha-" + v.replace(".", "-")
            extra = (f'<p class="release-file">{a["file"]} · {a["size"]:,} bytes</p>'
                     f'<div class="hash"><div class="hash-label"><span>بصمة SHA-256 للملف</span><button class="copy" type="button" data-for="{sid}">نسخ</button></div><code id="{sid}">{a["sha256"]}</code></div>')
        lis = "".join(f"<li>{esc(n)}</li>" for n in notes)
        arts.append(f"""<article class="release{" release--current" if cur else ""}" id="v{v.replace(".", "-")}">
<div class="release-head"><h2 class="release-title"><span class="release-version">ذِكر {v}</span>{"<span class=pill>الأحدث</span>" if cur else ""}<span class="release-meta">{meta}</span></h2>{dl}{extra}</div>
<details class="notes"{" open" if cur else ""}><summary>ما الجديد في {v}</summary><ul>{lis}</ul></details></article>""")
        if cur and len(rels) > 1:
            arts.append('<h2 class="section">الإصدارات السابقة</h2>')
    body = f"""<main class="card">
{brand("التحميل والتحديثات")}
<p class="lede">أحدث إصدار في الأعلى. التطبيق المثبّت يبحث عن التحديثات بنفسه من هذا الموقع ومن GitHub، ويعرض عليك الجديد قبل التحديث. هنا آخر {KEEP} إصدارات، كلٌّ منها قابل للتحميل مع ملاحظاته.</p>
{"".join(arts)}
</main>
<script>
document.querySelectorAll('.copy').forEach(b=>b.addEventListener('click',()=>{{const t=document.getElementById(b.dataset.for).textContent;
(navigator.clipboard?navigator.clipboard.writeText(t):Promise.reject()).then(()=>{{b.textContent='نُسخ';b.dataset.done=1;setTimeout(()=>{{b.textContent='نسخ';delete b.dataset.done}},1600)}}).catch(()=>{{}})}}));
</script>"""
    return page(f"تحميل ذِكر {rels[0][0]}", f"تحميل تطبيق ذِكر {rels[0][0]} لأندرويد وملاحظات كل إصدار.", "/download.html", body)


def upload(path, key, ctype):
    subprocess.run([sys.executable, os.path.join(HERE, "upload.py"), path, key, ctype], check=True)


def vkey(v): return [int(x) for x in v.split(".")]


def main():
    render_only = "--render" in sys.argv
    version, code = gradle_version()
    if "--version" in sys.argv:   # publish an older build than the one in build.gradle (e.g. while the next batch is in progress)
        version = sys.argv[sys.argv.index("--version") + 1]; code = None
    apks_path = os.path.join(HERE, "apks.json")
    apks = json.load(open(apks_path, encoding="utf-8")) if os.path.exists(apks_path) else {}
    ch = {v: n for v, n in changes() if vkey(v) <= vkey(version)}
    if version not in ch: raise SystemExit(f"{version} is not in AboutAl.CHANGES")
    order = sorted(ch, key=vkey, reverse=True)
    # The newest KEEP versions that have an APK in dist/ are the ones offered for download.
    keep = [v for v in order if os.path.exists(os.path.join(ROOT, "dist", f"ALzikr-{v}-Android.apk"))][:KEEP]
    if not keep or keep[0] != version: raise SystemExit(f"missing dist/ALzikr-{version}-Android.apk")
    for v in keep:
        if v in apks: continue
        local = os.path.join(ROOT, "dist", f"ALzikr-{v}-Android.apk")
        name = os.path.basename(local)
        if not render_only: upload(local, name, "application/vnd.android.package-archive")
        apks[v] = {"file": name, "size": os.path.getsize(local), "sha256": sha256(local), "versionCode": code if v == version else None}
    dd = dates()
    dd.setdefault(version, datetime.date.today().isoformat())
    rels = [(v, ch[v], dd.get(v, "")) for v in order]
    cur = apks[version]
    latest = {
        "app": "ALzikr", "version": version, "versionCode": cur.get("versionCode"),
        "downloadPage": f"{HOST}/download.html", "downloadUrl": f"{HOST}/{cur['file']}",
        "sha256": cur["sha256"], "size": cur["size"], "releaseDate": dd.get(version, ""),
        "releases": [{"version": v, "date": d, "notes": n} for v, n, d in rels[:10]],
    }
    os.makedirs(SITE, exist_ok=True)
    files = {"index.html": render_index({"version": version, "size": cur["size"]}),
             "download.html": render_download([r for r in rels if r[0] in keep], apks),
             "latest.json": json.dumps(latest, ensure_ascii=False, indent=1)}
    for name, text in files.items():
        open(os.path.join(SITE, name), "w", encoding="utf-8", newline="\n").write(text)
    if render_only: print("rendered into", SITE); return
    for name, ctype in [("index.html", "text/html; charset=utf-8"), ("download.html", "text/html; charset=utf-8"), ("latest.json", "application/json; charset=utf-8")]:
        upload(os.path.join(SITE, name), name, ctype)
    # APKs no longer offered leave the bucket.
    for v in [v for v in apks if v not in keep]:
        subprocess.run(["npx", "-y", "wrangler@latest", "r2", "object", "delete", f"alzikr-downloads/{apks[v]['file']}", "--remote"], cwd=HERE, shell=os.name == "nt")
        del apks[v]
    json.dump(apks, open(apks_path, "w", encoding="utf-8"), indent=1)
    print("published", version, "->", HOST, "downloads:", ", ".join(keep))


if __name__ == "__main__":
    main()
