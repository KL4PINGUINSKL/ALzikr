"""Upload a file to alzikr.alpharmasy.com (R2 bucket alzikr-downloads) in 8 MB parts, retrying each part.
Usage: python upload.py <local file> <key in bucket> [content-type]"""
import json, os, sys, time, urllib.request
BASE = "https://alzikr.alpharmasy.com/_upload/"
KEY = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".upload-key")).read().strip()
PART = 8 * 1024 * 1024

def call(method, op, params, data=None, tries=8):
    q = "&".join(f"{k}={urllib.parse.quote(str(v))}" for k, v in params.items())
    for i in range(tries):
        try:
            r = urllib.request.Request(BASE + op + "?" + q, data=data, method=method,
                                       headers={"x-upload-key": KEY, "user-agent": "alzikr-upload"})
            with urllib.request.urlopen(r, timeout=300) as resp: return json.loads(resp.read())
        except Exception as e:
            print(f"  {op} try {i+1} failed: {e}", flush=True); time.sleep(3 + 3 * i)
    raise SystemExit(f"{op} failed")

import urllib.parse
path, key = sys.argv[1], sys.argv[2]
ctype = sys.argv[3] if len(sys.argv) > 3 else "application/octet-stream"
size = os.path.getsize(path)
if size <= PART:
    print(call("PUT", "put", {"key": key, "type": ctype}, open(path, "rb").read()))
else:
    uid = call("POST", "start", {"key": key, "type": ctype})["uploadId"]
    parts = []
    with open(path, "rb") as f:
        n = 1
        while chunk := f.read(PART):
            p = call("PUT", "part", {"key": key, "id": uid, "n": n}, chunk)
            parts.append({"partNumber": n, "etag": p["etag"]})
            print(f"  part {n}: {min(n*PART, size)/size:.0%}", flush=True); n += 1
    print(call("POST", "done", {"key": key, "id": uid}, json.dumps(parts).encode()))
