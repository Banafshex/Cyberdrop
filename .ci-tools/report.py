import json, os, sys, urllib.request

path = sys.argv[1]
repo = os.environ["REPO"]
issue = os.environ.get("CI_ISSUE", "1")
token = os.environ["GITHUB_TOKEN"]
base = f"https://api.github.com/repos/{repo}/issues/{issue}/comments"
hdrs = {"Authorization": "token " + token, "Accept": "application/vnd.github+json",
        "Content-Type": "application/json", "User-Agent": "ci"}

# wipe previous relay comments to keep things tidy
try:
    with urllib.request.urlopen(urllib.request.Request(base + "?per_page=100", headers=hdrs)) as r:
        for c in json.load(r):
            if "CI log" in c.get("body", "")[:20]:
                urllib.request.urlopen(urllib.request.Request(
                    f"https://api.github.com/repos/{repo}/issues/comments/{c['id']}",
                    method="DELETE", headers=hdrs)).read()
except Exception as e:
    print("cleanup skipped:", e)

body = open(path, encoding="utf-8", errors="replace").read()[-55000:].replace("```", "'''")
payload = json.dumps({"body": f"CI log run `{os.environ.get('GITHUB_RUN_ID','')}` job `{os.environ.get('GITHUB_JOB','')}`\n\n```\n{body}\n```"}).encode()
req = urllib.request.Request(base, data=payload, method="POST", headers=hdrs)
with urllib.request.urlopen(req) as r:
    print("posted", r.status)
