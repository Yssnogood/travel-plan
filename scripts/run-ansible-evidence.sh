#!/bin/bash

set -euo pipefail

INVENTORY=${INVENTORY:-ansible/inventory/hosts.yml}
PLAYBOOK=${PLAYBOOK:-ansible/playbooks/deploy-all.yml}
ENVIRONMENT=${ENVIRONMENT:-staging}
EXTRA_VARS=${EXTRA_VARS:-}
OUTPUT_DIR=${OUTPUT_DIR:-artifacts/ansible}
SKIP_IDEMPOTENCE_CHECK=${SKIP_IDEMPOTENCE_CHECK:-false}

PROJECT_ROOT=$(cd "$(dirname "$0")/.." && pwd)
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
RUN_DIR="$PROJECT_ROOT/$OUTPUT_DIR/$TIMESTAMP"
RUN1_LOG="$RUN_DIR/run-1.log"
RUN2_LOG="$RUN_DIR/run-2.log"
SUMMARY_JSON="$RUN_DIR/summary.json"
REPORT_MD="$RUN_DIR/report.md"

mkdir -p "$RUN_DIR"

if ! command -v ansible-playbook >/dev/null 2>&1; then
  echo "ansible-playbook command not found. Install Ansible first." >&2
  exit 1
fi

function run_ansible_pass() {
  local pass_name=$1
  local log_file=$2

  local args=("-i" "$PROJECT_ROOT/$INVENTORY" "$PROJECT_ROOT/$PLAYBOOK" "-e" "environment=$ENVIRONMENT")
  if [[ -n "$EXTRA_VARS" ]]; then
    args+=("-e" "$EXTRA_VARS")
  fi

  echo "[$pass_name] Running ansible-playbook..."
  set +e
  ansible-playbook "${args[@]}" 2>&1 | tee "$log_file"
  local exit_code=${PIPESTATUS[0]}
  set -e

  local recap
  recap=$(grep -E '^[^[:space:]]+[[:space:]]*:[[:space:]]+ok=[0-9]+[[:space:]]+changed=[0-9]+[[:space:]]+unreachable=[0-9]+[[:space:]]+failed=[0-9]+' "$log_file" || true)

  local changed failed unreachable
  changed=$(echo "$recap" | awk -F'changed=| unreachable=' '{sum+=$2} END {print sum+0}')
  failed=$(echo "$recap" | awk -F'failed=' '{sum+=$2} END {print sum+0}')
  unreachable=$(echo "$recap" | awk -F'unreachable=| failed=' '{sum+=$2} END {print sum+0}')

  local success=false
  if [[ $exit_code -eq 0 && $failed -eq 0 && $unreachable -eq 0 ]]; then
    success=true
  fi

  echo "$pass_name|$exit_code|$changed|$failed|$unreachable|$success"
}

RUN1_RESULT=$(run_ansible_pass "run-1" "$RUN1_LOG")
IFS='|' read -r RUN1_NAME RUN1_EXIT RUN1_CHANGED RUN1_FAILED RUN1_UNREACHABLE RUN1_SUCCESS <<< "$RUN1_RESULT"

RUN2_EXIT=""
RUN2_CHANGED=""
RUN2_FAILED=""
RUN2_UNREACHABLE=""
RUN2_SUCCESS=""
IDEMPOTENCE_PASSED=""

if [[ "$SKIP_IDEMPOTENCE_CHECK" != "true" ]]; then
  RUN2_RESULT=$(run_ansible_pass "run-2" "$RUN2_LOG")
  IFS='|' read -r RUN2_NAME RUN2_EXIT RUN2_CHANGED RUN2_FAILED RUN2_UNREACHABLE RUN2_SUCCESS <<< "$RUN2_RESULT"

  if [[ "$RUN2_SUCCESS" == "true" && "$RUN2_CHANGED" -eq 0 ]]; then
    IDEMPOTENCE_PASSED="true"
  else
    IDEMPOTENCE_PASSED="false"
  fi
else
  IDEMPOTENCE_PASSED="skipped"
fi

OVERALL_SUCCESS="false"
if [[ "$RUN1_SUCCESS" == "true" ]]; then
  if [[ "$SKIP_IDEMPOTENCE_CHECK" == "true" || "$IDEMPOTENCE_PASSED" == "true" ]]; then
    OVERALL_SUCCESS="true"
  fi
fi

cat > "$SUMMARY_JSON" <<EOF
{
  "generatedAt": "$(date -Iseconds)",
  "environment": "$ENVIRONMENT",
  "inventory": "$PROJECT_ROOT/$INVENTORY",
  "playbook": "$PROJECT_ROOT/$PLAYBOOK",
  "skipIdempotenceCheck": $([[ "$SKIP_IDEMPOTENCE_CHECK" == "true" ]] && echo true || echo false),
  "runDirectory": "$RUN_DIR",
  "run1": {
    "exitCode": $RUN1_EXIT,
    "changed": $RUN1_CHANGED,
    "failed": $RUN1_FAILED,
    "unreachable": $RUN1_UNREACHABLE,
    "success": $RUN1_SUCCESS,
    "logFile": "$RUN1_LOG"
  },
  "run2": {
    "exitCode": ${RUN2_EXIT:-null},
    "changed": ${RUN2_CHANGED:-null},
    "failed": ${RUN2_FAILED:-null},
    "unreachable": ${RUN2_UNREACHABLE:-null},
    "success": ${RUN2_SUCCESS:-null},
    "logFile": "$RUN2_LOG"
  },
  "idempotencePassed": "$IDEMPOTENCE_PASSED",
  "overallSuccess": $OVERALL_SUCCESS
}
EOF

cat > "$REPORT_MD" <<EOF
# Ansible Execution Evidence

- Generated at: $(date -Iseconds)
- Environment: $ENVIRONMENT
- Inventory: $PROJECT_ROOT/$INVENTORY
- Playbook: $PROJECT_ROOT/$PLAYBOOK
- Output directory: $RUN_DIR

## Run Results

| Check | Result |
|-------|--------|
| Run 1 successful (no failed/unreachable) | $RUN1_SUCCESS |
| Run 1 changed tasks | $RUN1_CHANGED |
| Run 2 idempotence check | $IDEMPOTENCE_PASSED |
| Run 2 changed tasks | ${RUN2_CHANGED:-n/a} |
| Overall success | $OVERALL_SUCCESS |

## Logs

- run-1: $RUN1_LOG
- run-2: $RUN2_LOG
- summary: $SUMMARY_JSON
EOF

echo "Ansible evidence generated:"
echo "- $SUMMARY_JSON"
echo "- $REPORT_MD"

if [[ "$OVERALL_SUCCESS" != "true" ]]; then
  exit 1
fi

exit 0