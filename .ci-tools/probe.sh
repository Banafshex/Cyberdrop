set -u
M=https://repo1.maven.org/maven2
G=https://dl.google.com/dl/android/maven2
meta() {
  echo "### $1"
  curl -s --max-time 25 "$1" -o /tmp/raw.xml || echo "(curl failed)"
  grep -o '<version>[^<]*</version>' /tmp/raw.xml 2>/dev/null | tail -10 | tr '\n' ' '
  echo
  grep -o '<\(latest\|release\)>[^<]*</\1>' /tmp/raw.xml 2>/dev/null
  echo
}
meta $M/io/github/webrtc-sdk/android/maven-metadata.xml
meta $M/org/webrtc/google-webrtc/maven-metadata.xml
meta $M/org/eclipse/paho/org.eclipse.paho.client.mqttv3/maven-metadata.xml
meta $G/androidx/compose/compose-bom/maven-metadata.xml
meta $G/com/android/tools/build/gradle/maven-metadata.xml

dump() { # $1 = aar url
  echo "=== AAR $1"
  curl -s --max-time 60 "$1" -o /tmp/w.aar || { echo "download failed"; return; }
  ls -l /tmp/w.aar
  rm -rf /tmp/wx && mkdir -p /tmp/wx && cd /tmp/wx && unzip -o -q /tmp/w.aar || { echo "unzip failed"; return; }
  ls
  JAVAP=$(ls /usr/lib/jvm/*/bin/javap 2>/dev/null | head -1); [ -z "$JAVAP" ] && JAVAP=javap
  for c in 'org.webrtc.PeerConnection$Observer' 'org.webrtc.PeerConnection$IceServer' 'org.webrtc.DataChannel' 'org.webrtc.DataChannel$Buffer' 'org.webrtc.PeerConnectionFactory' 'org.webrtc.SessionDescription' 'org.webrtc.SdpObserver' 'org.webrtc.MediaConstraints' 'org.webrtc.PeerConnection$RTCConfiguration' 'org.webrtc.PeerConnection$IceConnectionState' 'org.webrtc.PeerConnection$PeerConnectionState'; do
    echo "--- $c"
    $JAVAP -cp classes.jar "$c" 2>&1 | head -40
  done
}
VER=$(curl -s --max-time 20 $M/io/github/webrtc-sdk/android/maven-metadata.xml | grep -o '<release>[^<]*</release>' | sed 's/<[^>]*>//g')
echo "webrtc-sdk release=$VER"
dump "$M/io/github/webrtc-sdk/android/$VER/android-$VER.aar"
dump "$M/org/webrtc/google-webrtc/1.0.32006/google-webrtc-1.0.32006.aar"
echo "=== env"
ls /usr/local/lib/android/sdk/platforms /usr/local/lib/android/sdk/build-tools 2>&1
java -version 2>&1
