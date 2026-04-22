#!/bin/sh
set -eu

# Init script for Elasticsearch: waits for cluster health and creates index template + index
ES_URL=${ES_URL:-${SPRING_ELASTICSEARCH_URI:-http://elasticsearch:9200}}
echo "[es-init] Using ES_URL=${ES_URL}"

MAX_RETRIES=30
SLEEP=2
count=0
echo "[es-init] Waiting for Elasticsearch to become available..."
until curl -sSf "${ES_URL}/_cluster/health?wait_for_status=yellow&timeout=1s" > /dev/null 2>&1; do
  count=$((count+1))
  if [ "$count" -ge "$MAX_RETRIES" ]; then
    echo "[es-init] Elasticsearch did not become healthy after ${MAX_RETRIES} attempts"
    exit 1
  fi
  echo "[es-init] retry ${count}/${MAX_RETRIES}..."
  sleep ${SLEEP}
done

echo "[es-init] Elasticsearch is up — creating index template and index if needed"

TEMPLATE_PAYLOAD='{
  "index_patterns": ["users*"],
  "template": {
    "mappings": {
      "properties": {
        "id": { "type": "keyword" },
        "name": { "type": "text", "fields": { "keyword": { "type": "keyword" } } },
        "email": { "type": "keyword" },
        "createdAt": { "type": "date" }
      }
    }
  },
  "priority": 10
}'

echo "[es-init] Installing index template 'webflux_template'"
curl -sSf -X PUT "${ES_URL}/_index_template/webflux_template" -H 'Content-Type: application/json' -d "${TEMPLATE_PAYLOAD}" || {
  echo "[es-init] Failed to create index template (it may already exist)"
}

echo "[es-init] Ensuring index 'users' exists"
if ! curl -sSf -I -X HEAD "${ES_URL}/users" > /dev/null 2>&1; then
  INDEX_PAYLOAD='{
    "mappings": {
      "properties": {
        "id": { "type": "keyword" },
        "name": { "type": "text", "fields": { "keyword": { "type": "keyword" } } },
        "email": { "type": "keyword" },
        "createdAt": { "type": "date" }
      }
    }
  }'
  curl -sSf -X PUT "${ES_URL}/users" -H 'Content-Type: application/json' -d "${INDEX_PAYLOAD}" && echo "[es-init] Created index 'users'" || {
    echo "[es-init] Failed to create index 'users'"
    exit 1
  }
else
  echo "[es-init] Index 'users' already exists"
fi

echo "[es-init] Initialization complete"

