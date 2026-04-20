
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

JAR_PATH="$ROOT_DIR/target/SuperApp.jar"

if [[ ! -f "$JAR_PATH" ]]; then
  echo "Сборка не найдена. Сначала выполните: mvn clean package"
  exit 1
fi

exec java -jar "$JAR_PATH"
