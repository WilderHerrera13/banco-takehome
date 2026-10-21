#!/usr/bin/env bash
# Empaqueta la solución en un ZIP autocontenido: al descomprimirlo y ejecutar
#   docker compose up --build
# el stack completo queda arriba (Postgres + Kafka + los 2 microservicios).
set -euo pipefail
cd "$(dirname "$0")"

ZIP="${1:-$HOME/Desktop/banco-takehome.zip}"
ROOT="$(pwd)"

echo "▸ limpio artefactos de build"
mvn -q clean >/dev/null 2>&1 || true
find . -name target -type d -prune -exec rm -rf {} + 2>/dev/null || true

echo "▸ empaqueto en $ZIP"
rm -f "$ZIP"
cd "$ROOT"
zip -r -q "$ZIP" . \
  -x "*.DS_Store" \
  -x "*/target/*" -x "target/*" \
  -x "*/.git/*" -x ".git/*" \
  -x "*/.idea/*" -x ".idea/*" \
  -x "*.iml" -x "*.log"

echo "▸ contenido (top nivel):"
unzip -l "$ZIP" | awk 'NR>3 && NF>=4 {print "   " $4}' | grep -v "/$" | head -12
echo "▸ total: $(unzip -l "$ZIP" | tail -1 | awk '{print $2}') archivos, $(du -h "$ZIP" | cut -f1)"
echo "▸ listo: $ZIP"
