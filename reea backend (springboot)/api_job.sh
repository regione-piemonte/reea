#!/bin/sh

set -x

API_URL="http://reea-rp-01-reeabe:8080/reeabe/batch/elaborazioneTracciati"

TMP_RESPONSE="/tmp/api_response.txt"

echo "=================================================="
echo "$(date) - START API JOB"
echo "=================================================="

echo ""
echo "===== ENVIRONMENT INFO ====="
echo "Hostname: $(hostname)"
echo "Current user:"
id || true

echo ""
echo "Working directory:"
pwd || true

echo ""
echo "PATH:"
echo "$PATH"

echo ""
echo "Curl location:"
which curl || true

echo ""
echo "Curl version:"
curl --version || true

echo ""
echo "Mail location:"
which mail || echo "mail command NOT FOUND"

echo ""
echo "DNS test:"
getent hosts mailfarm-app.csi.it || true

echo ""
echo "API URL:"
echo "$API_URL"

echo "============================"
echo ""

echo "$(date) - Avvio chiamata API"

HTTP_CODE=$(curl -sS \
  -X GET \
  --connect-timeout 10 \
  --max-time 30 \
  -o "$TMP_RESPONSE" \
  -w "%{http_code}" \
  "$API_URL")

CURL_EXIT_CODE=$?

echo ""
echo "===== CURL RESULT ====="
echo "HTTP_CODE=$HTTP_CODE"
echo "CURL_EXIT_CODE=$CURL_EXIT_CODE"
echo "======================="
echo ""

echo "------------- RESPONSE BODY -------------"

if [ -f "$TMP_RESPONSE" ]; then
    cat "$TMP_RESPONSE"
else
    echo "Response file not found"
fi

echo ""
echo "-----------------------------------------"

if [ "$CURL_EXIT_CODE" -ne 0 ]; then
    echo ""
    echo "ERRORE CURL"
    echo "CURL_EXIT_CODE=$CURL_EXIT_CODE"
    echo "$(date) - EXIT 1"
    exit 1
fi

if [ "$HTTP_CODE" -lt 200 ] || [ "$HTTP_CODE" -ge 300 ]; then
    echo ""
    echo "ERRORE HTTP"
    echo "HTTP_CODE=$HTTP_CODE"
    echo "$(date) - EXIT 1"
    exit 1
fi

echo ""
echo "$(date) - API invocata correttamente"
echo ""

#
# BLOCCO MAIL DISABILITATO PER DEBUG
#
# BODY=$(cat <<EOF
# Test mail
# Timestamp: $(date)
# HTTP CODE: $HTTP_CODE
# EOF
# )
#
# echo "$BODY" | mail \
#   -s "TEST MAIL" \
#   -r "davide.elia@csi.it" \
#   -S smtp="mailfarm-app.csi.it:25" \
#   davide.elia@csi.it
#
# echo "MAIL EXIT CODE=$?"
#

echo "$(date) - JOB COMPLETATO CON SUCCESSO"
echo "EXIT CODE 0"

exit 0