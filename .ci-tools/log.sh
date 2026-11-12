# commit a tail of the given log file to .ci/log.txt so it can be read locally
set -u
f="$1"; name="${2:-log.txt}"
tail -c 120000 "$f" > .ci/$name
git config user.name "cyberdrop-ci"; git config user.email "ci@users.noreply.github.com"
git add -f .ci/$name
git commit -q -m "ci: $name [skip ci]" || echo "nothing to commit"
git pull -q --rebase origin "${GITHUB_REF_NAME}" 2>&1 | tail -2
git push origin "HEAD:${GITHUB_REF_NAME}" 2>&1 | tail -3
