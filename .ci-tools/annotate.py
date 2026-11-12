import sys

path = sys.argv[1] if len(sys.argv) > 1 else "build.log"
try:
    data = open(path, encoding="utf-8", errors="replace").read()[-50000:]
except Exception as exc:  # noqa: BLE001
    data = "no log (%r)" % (exc,)
data = data.replace("%", "%25").replace("\r", "").replace("\n", "%0A")
for i in range(0, len(data), 6000):
    print("::error title=ci-log-%d::%s" % (i // 6000, data[i:i + 6000]))
