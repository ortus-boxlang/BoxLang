#!/usr/bin/env bash
######################################
# BoxLang Startup Benchmark
#
# Measures cold JVM startup time for BoxLang with and without AppCDS.
# Run from the project root:
#   ./workbench/scripts/benchmark-startup.sh [--jar path/to/boxlang.jar] [--runs N]
#
# Requirements: bash 3.2+, java on PATH, bc
######################################

set -euo pipefail

# ── Defaults ──────────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
JAR="${PROJECT_ROOT}/build/libs/boxlang-$(grep '^version=' "${PROJECT_ROOT}/gradle.properties" | cut -d= -f2)-snapshot.jar"
JSA_FILE="${PROJECT_ROOT}/build/appcds/boxlang.jsa"
RUNS=10
WARMUP=3
OUTPUT_FILE="${PROJECT_ROOT}/build/appcds/benchmark-results.md"

# ── Argument parsing ──────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
    case "$1" in
        --jar) JAR="$2"; shift 2 ;;
        --jsa) JSA_FILE="$2"; shift 2 ;;
        --runs) RUNS="$2"; shift 2 ;;
        --warmup) WARMUP="$2"; shift 2 ;;
        --output) OUTPUT_FILE="$2"; shift 2 ;;
        *) echo "Unknown argument: $1"; exit 1 ;;
    esac
done

mkdir -p "$(dirname "${OUTPUT_FILE}")"

# ── Helpers ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; CYAN='\033[0;36m'; YELLOW='\033[1;33m'; NC='\033[0m'
log()  { echo -e "${CYAN}[benchmark]${NC} $*"; }
ok()   { echo -e "${GREEN}[ok]${NC} $*"; }
warn() { echo -e "${YELLOW}[warn]${NC} $*"; }
fail() { echo -e "${RED}[fail]${NC} $*"; exit 1; }

# Time a single JVM startup (ms) — uses Unix epoch in nanoseconds via date on macOS/Linux
time_startup_ms() {
    local java_args=("$@")
    local start end elapsed
    start=$(python3 -c 'import time; print(int(time.time() * 1000))')
    java "${java_args[@]}" -cp "${JAR}" ortus.boxlang.runtime.BoxRunner --version > /dev/null 2>&1 || true
    end=$(python3 -c 'import time; print(int(time.time() * 1000))')
    echo $(( end - start ))
}

# Run N warmup iterations (discarded), then N measurement iterations; returns avg ms
benchmark_run() {
    local label="$1"; shift
    local java_args=("$@")
    local times=()

    log "Warming up (${WARMUP} runs): ${label}"
    for (( i=0; i<WARMUP; i++ )); do
        time_startup_ms "${java_args[@]}" > /dev/null
        printf "."
    done
    echo ""

    log "Measuring (${RUNS} runs): ${label}"
    for (( i=0; i<RUNS; i++ )); do
        local ms
        ms=$(time_startup_ms "${java_args[@]}")
        times+=("${ms}")
        printf "  run %2d: %dms\n" "$((i+1))" "${ms}"
    done

    # Compute min, max, avg
    local sum=0 min="${times[0]}" max="${times[0]}"
    for t in "${times[@]}"; do
        sum=$(( sum + t ))
        (( t < min )) && min=$t
        (( t > max )) && max=$t
    done
    local avg=$(( sum / RUNS ))

    echo "  min=${min}ms  avg=${avg}ms  max=${max}ms"
    # Return avg via stdout line — caller reads last line
    echo "__RESULT__:${label}:${min}:${avg}:${max}"
}

# ── Pre-flight checks ─────────────────────────────────────────────────────────
[[ ! -f "${JAR}" ]] && fail "JAR not found: ${JAR}\nRun './gradlew shadowJar' first."
java -version > /dev/null 2>&1     || fail "java not found on PATH"

JAVA_VER=$(java -version 2>&1 | head -2)
log "Java: ${JAVA_VER}"
log "JAR:  ${JAR}"
log "Runs: warmup=${WARMUP}, measured=${RUNS}"
echo ""

# ── Baseline (no AppCDS) ──────────────────────────────────────────────────────
log "=== BASELINE (no AppCDS) ==="
BASELINE_OUTPUT=$(benchmark_run "No AppCDS" -Xshare:off)
BASELINE_LINE=$(echo "${BASELINE_OUTPUT}" | grep "^__RESULT__")
BASELINE_MIN=$(echo "${BASELINE_LINE}" | cut -d: -f3)
BASELINE_AVG=$(echo "${BASELINE_LINE}" | cut -d: -f4)
BASELINE_MAX=$(echo "${BASELINE_LINE}" | cut -d: -f5)
ok "Baseline avg: ${BASELINE_AVG}ms (min: ${BASELINE_MIN}ms, max: ${BASELINE_MAX}ms)"
echo ""

# ── AppCDS (if .jsa exists) ───────────────────────────────────────────────────
CDS_AVG="N/A"; CDS_MIN="N/A"; CDS_MAX="N/A"; IMPROVEMENT="N/A"

if [[ -f "${JSA_FILE}" ]]; then
    log "=== WITH AppCDS ==="
    CDS_OUTPUT=$(benchmark_run "With AppCDS" -Xshare:on -XX:SharedArchiveFile="${JSA_FILE}")
    CDS_LINE=$(echo "${CDS_OUTPUT}" | grep "^__RESULT__")
    CDS_MIN=$(echo "${CDS_LINE}" | cut -d: -f3)
    CDS_AVG=$(echo "${CDS_LINE}" | cut -d: -f4)
    CDS_MAX=$(echo "${CDS_LINE}" | cut -d: -f5)

    if command -v bc &>/dev/null; then
        IMPROVEMENT=$(echo "scale=1; (${BASELINE_AVG} - ${CDS_AVG}) * 100 / ${BASELINE_AVG}" | bc)
        ok "AppCDS avg: ${CDS_AVG}ms (min: ${CDS_MIN}ms, max: ${CDS_MAX}ms)"
        ok "Improvement: ${IMPROVEMENT}% faster startup"
    else
        IMPROVEMENT=$(( (BASELINE_AVG - CDS_AVG) * 100 / BASELINE_AVG ))
        ok "AppCDS avg: ${CDS_AVG}ms (min: ${CDS_MIN}ms, max: ${CDS_MAX}ms)"
        ok "Improvement: ~${IMPROVEMENT}% faster startup"
    fi
else
    warn "No .jsa archive found at ${JSA_FILE}"
    warn "Run './gradlew generateCdsArchive' to create it, then re-run this benchmark."
fi

# ── Write results markdown ────────────────────────────────────────────────────
TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")
JAR_BASENAME=$(basename "${JAR}")

cat > "${OUTPUT_FILE}" << EOF
# BoxLang Startup Benchmark Results

**Date:** ${TIMESTAMP}
**JAR:** \`${JAR_BASENAME}\`
**Java:** \`${JAVA_VER}\`
**Measured runs:** ${RUNS} (after ${WARMUP} warmup runs)
**Metric:** Time from \`java\` process launch to exit (--version flag, cold start)

## Results

| Mode | Min | Avg | Max |
|------|-----|-----|-----|
| No AppCDS (baseline) | ${BASELINE_MIN}ms | ${BASELINE_AVG}ms | ${BASELINE_MAX}ms |
| With AppCDS | ${CDS_MIN}ms | ${CDS_AVG}ms | ${CDS_MAX}ms |

**Startup improvement with AppCDS: ${IMPROVEMENT}%**

## Notes

- Times measured by wrapping \`java ... --version\` in millisecond wall-clock timestamps.
- Each run is a fresh \`java\` process (no JVM reuse) to simulate real CLI cold-start.
- \`-Xshare:off\` forces the JVM to skip any default class-data sharing.
- \`-Xshare:on\` with the generated \`.jsa\` archive is the AppCDS configuration.
EOF

echo ""
ok "Results written to: ${OUTPUT_FILE}"
