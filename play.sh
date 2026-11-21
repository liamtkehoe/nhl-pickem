#!/usr/bin/env bash
#Starts application     ./play.sh            run the existing build
#                       ./play.sh --rebuild  rebuild first (after pulling or changing code)
set -euo pipefail
cd "$(dirname "$0")"

#1. Back up the database before anything touches it
mkdir -p data/backups
if [[ -f data/pickem.db ]]; then
  cp data/pickem.db "data/backups/pickem-$(date +%Y%m%d-%H%M%S).db"
  ls -1t data/backups/pickem-*.db | tail -n +11 | while read -r old; do rm -- "$old"; done
fi

#2. Build if asked, or if there's no jar yet
JAR=$(ls target/nhl-pickem-*.jar 2>/dev/null | head -n 1 || true)
if [[ "${1:-}" == "--rebuild" || -z "$JAR" ]]; then
  echo "Starting NHL Pick'em..."
  ./mvnw -q -Pfrontend -DskipTests clean package
  JAR=$(ls target/nhl-pickem-*.jar | head -n 1)
fi

#3. Open the browser once the server is up
( until curl -sf http://localhost:8080/api/health > /dev/null; do sleep 1; done
  open http://localhost:8080 ) &

echo "Starting NHL Pick'em at http://localhost:8080 (Ctrl+C to stop)"
exec java --enable-native-access=ALL-UNNAMED -jar "$JAR"