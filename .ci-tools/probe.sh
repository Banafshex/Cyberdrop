set -u
probe() {
  echo "### $1"
  curl -s --max-time 25 "$1" -o /tmp/raw.xml || echo "(curl failed)"
  grep -o '<version>[^<]*</version>' /tmp/raw.xml 2>/dev/null | tail -14 | tr '\n' ' '
  echo
  grep -o '<\(latest\|release\)>[^<]*</\1>' /tmp/raw.xml 2>/dev/null
  echo
}
M=https://repo1.maven.org/maven2
G=https://dl.google.com/dl/android/maven2
probe $M/io/github/webrtc-sdk/android/maven-metadata.xml
probe $M/org/webrtc/google-webrtc/maven-metadata.xml
probe $M/com/dafruits/webrtc/maven-metadata.xml
probe $M/org/eclipse/paho/org.eclipse.paho.client.mqttv3/maven-metadata.xml
probe $G/androidx/compose/compose-bom/maven-metadata.xml
probe $G/com/android/tools/build/gradle/maven-metadata.xml
probe $G/androidx/activity/activity-compose/maven-metadata.xml
probe $G/androidx/lifecycle/lifecycle-runtime-compose/maven-metadata.xml
echo "=== env"
ls /usr/local/lib/android/sdk/platforms /usr/local/lib/android/sdk/build-tools 2>&1
java -version 2>&1
echo "=== git push test"
mkdir -p .ci && date > .ci/probe.txt
git config user.name "cyberdrop-ci"; git config user.email "ci@users.noreply.github.com"
git add -f .ci/probe.txt && git commit -q -m "ci: probe [skip ci]" || echo "COMMIT FAILED rc=$?"
git push origin "HEAD:${GITHUB_REF_NAME}" 2>&1 || echo "PUSH FAILED rc=$?"
