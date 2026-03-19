#!/usr/bin/env bash
######################################
# BoxLang AppCDS Launcher Benchmark
#
# Compares startup and execution performance between:
#   boxlang  — launcher WITH AppCDS cache (~/.boxlang/cache/<ver>.jsa)
#   boxlang2 — launcher WITHOUT AppCDS (pure baseline)
#
# Two tests are run:
#   1. `--version`  — measures raw JVM startup overhead
#   2. `<script>`   — measures startup + BoxLang script execution
#
# Usage (from project root):
#   ./workbench/scripts/benchmark-appcds.sh [options]
#
# Options:
#   --bin-dir DIR   Directory with boxlang and boxlang2 binaries
#                   (default: build/distributions/bin)
#   --script FILE   BoxLang script for test 2
#                   (default: workbench/scripts/test.bxs)
#   --runs N        Number of measured runs per variant (default: 10)
#   --warmup N      Warmup runs discarded before measurement (default: 3)
#   --output FILE   Markdown report path
#                   (default: build/appcds/benchmark-appcds-results.md)
#
# Requirements: bash 3.2+, python3 (for ms timing), java on PATH
######################################

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# ── Defaults ──────────────────────────────────────────────────────────────────
BIN_DIR="${PROJECT_ROOT}/build/distributions/bin"
BXS_SCRIPT="${SCRIPT_DIR}/test.bxs"
RUNS=10
WARMUP=3
OUTPUT_FILE="${PROJECT_ROOT}/build/appcds/benchmark-appcds-results.md"

# ── Argument parsing ──────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
    case "$1" in
        --bin-dir) BIN_DIR="$2";    shift 2 ;;
        --script)  BXS_SCRIPT="$2"; shift 2 ;;
        --runs)    RUNS="$2";       shift 2 ;;
        --warmup)  WARMUP="$2";     shift 2 ;;
        --output)  OUTPUT_FILE="$2";shift 2 ;;
        -h|--help)
            sed -n '/#/,/^[^#]/p' "$0" | grep '^#' | sed 's/^# \?//'
            exit 0 ;;
        *) echo "Unknown argument: $1" >&2; exit 1 ;;
    esac
done

BOXLANG="${BIN_DIR}/boxlang"
BOXLANG2="${BIN_DIR}/boxlang2"

mkdir -p "$(dirname "${OUTPUT_FILE}")"

# ── Colors ────────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
DIM='\033[2m'
NC='\033[0m'

log()  { echo -e "${CYAN}[benchmark]${NC} $*"; }
ok()   { echo -e "${GREEN}[ok]${NC}        $*"; }
warn() { echo -e "${YELLOW}[warn]${NC}      $*"; }
fail() { echo -e "${RED}[fail]${NC}      $*" >&2; exit 1; }
hdr()  { echo -e "\n${BOLD}${BLUE}══ $* ══${NC}"; }

# ── Timing helper ─────────────────────────────────────────────────────────────
# Returns current epoch in milliseconds.  Prefers bash 5+ built-in, falls back
# to python3 (always present on macOS), then coarse date +%s.
now_ms() {
    # shellcheck disable=SC2154
    if [[ -n "${EPOCHREALTIME+x}" ]]; then
        # bash 5+: EPOCHREALTIME is seconds.microseconds
        printf '%.0f\n' "$(awk "BEGIN{printf \"%.0f\", ${EPOCHREALTIME}*1000}")"
    elif command -v python3 &>/dev/null; then
        python3 -c 'import time; print(int(time.time() * 1000))'
    else
        echo $(( $(date +%s) * 1000 ))
    fi
}

# Time a single command invocation, print elapsed ms to stdout.
time_cmd_ms() {
    local start end
    start=$(now_ms)
    "$@" > /dev/null 2>&1 || true
    end=$(now_ms)
    echo $(( end - start ))
}

# ── Benchmark runner ──────────────────────────────────────────────────────────
# run_benchmark LABEL CMD [ARGS...]
#   Runs WARMUP silent iterations then RUNS measured iterations.
#   Prints individual times and a summary line.
#   Emits a machine-readable  __RESULT__:LABEL:MIN:AVG:MAX  line.
run_benchmark() {
    local label="$1"; shift
    local cmd=("$@")
    local -a times
    times=()

    printf "${DIM}  Warming up  (%d runs)${NC}" "${WARMUP}"
    for (( i=0; i<WARMUP; i++ )); do
        time_cmd_ms "${cmd[@]}" > /dev/null
        printf "."
    done
    echo ""

    echo "  Measuring   (${RUNS} runs):"
    for (( i=0; i<RUNS; i++ )); do
        local ms
        ms=$(time_cmd_ms "${cmd[@]}")
        times+=("${ms}")
        printf "    run %2d: %dms\n" "$((i+1))" "${ms}"
    done

    # Compute min / avg / max
    local sum=0 min="${times[0]}" max="${times[0]}"
    for t in "${times[@]}"; do
        sum=$(( sum + t ))
        (( t < min )) && min=$t || true
        (( t > max )) && max=$t || true
    done
    local avg=$(( sum / RUNS ))

    echo -e "  ${BOLD}→ min=${min}ms  avg=${avg}ms  max=${max}ms${NC}"
    # Machine-readable result (colon-separated, label must not contain colons)
    echo "__RESULT__:${label}:${min}:${avg}:${max}"
}

# Extract a field from the __RESULT__ line (fields: 1=tag 2=label 3=min 4=avg 5=max)
parse_result() {
    local output="$1" field="$2"
    echo "${output}" | grep "^__RESULT__" | cut -d: -f"${field}"
}

# ── Improvement percentage ────────────────────────────────────────────────────
improvement_pct() {
    local baseline="$1" improved="$2"
    if (( baseline == 0 )); then echo "0"; return; fi
    if command -v bc &>/dev/null; then
        echo "scale=1; (${baseline} - ${improved}) * 100 / ${baseline}" | bc
    else
        echo $(( (baseline - improved) * 100 / baseline ))
    fi
}

# ── Print comparison table ────────────────────────────────────────────────────
print_comparison() {
    local label="$1"
    local b_min="$2" b_avg="$3" b_max="$4"   # boxlang2 (baseline)
    local c_min="$5" c_avg="$6" c_max="$7"   # boxlang  (AppCDS)

    local pct
    pct=$(improvement_pct "${b_avg}" "${c_avg}")

    echo ""
    echo -e "${BOLD}  ${label} — Summary${NC}"
    printf "  %-28s  %8s  %8s  %8s\n" "Variant" "Min" "Avg" "Max"
    printf "  %-28s  %8s  %8s  %8s\n" \
        "----------------------------" "--------" "--------" "--------"
    printf "  %-28s  %7dms  %7dms  %7dms\n" \
        "boxlang2  (no AppCDS)" "${b_min}" "${b_avg}" "${b_max}"
    printf "  %-28s  %7dms  %7dms  %7dms\n" \
        "boxlang   (AppCDS)"   "${c_min}" "${c_avg}" "${c_max}"
    echo ""

    if (( c_avg < b_avg )); then
        ok "${BOLD}AppCDS is ${pct}% faster${NC}  (avg: ${c_avg}ms vs ${b_avg}ms)"
    elif (( c_avg > b_avg )); then
        warn "AppCDS is SLOWER by ${pct}%  (avg: ${c_avg}ms vs ${b_avg}ms)"
        warn "The JSA cache may be absent, stale, or incompatible — run 'boxlang --version' once to regenerate."
    else
        ok "No measurable difference  (avg: ${c_avg}ms)"
    fi
}

# ══════════════════════════════════════════════════════════════════════════════
# Pre-flight checks
# ══════════════════════════════════════════════════════════════════════════════
[[ -x "${BOXLANG}" ]]  || fail "boxlang not found or not executable:\n  ${BOXLANG}"
[[ -x "${BOXLANG2}" ]] || fail "boxlang2 not found or not executable:\n  ${BOXLANG2}"
[[ -f "${BXS_SCRIPT}" ]] || fail "BoxLang script not found:\n  ${BXS_SCRIPT}"
command -v java &>/dev/null || fail "java not found on PATH"

JAVA_VER=$(java -version 2>&1 | head -1)
TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")

echo ""
echo -e "${BOLD}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}║         BoxLang AppCDS Launcher Benchmark                  ║${NC}"
echo -e "${BOLD}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
log "Java:      ${JAVA_VER}"
log "boxlang:   ${BOXLANG}   ${GREEN}← AppCDS enabled${NC}"
log "boxlang2:  ${BOXLANG2}  ${YELLOW}← no AppCDS (baseline)${NC}"
log "Script:    ${BXS_SCRIPT}"
log "Config:    warmup=${WARMUP} runs, measured=${RUNS} runs"

# Detect AppCDS cache location and warn if missing
BL_JAR=$(ls "${BIN_DIR}/../lib"/boxlang-*.jar 2>/dev/null | head -1 || true)
if [[ -n "${BL_JAR}" ]]; then
    BL_VER=$(basename "${BL_JAR}" .jar | sed 's/^boxlang-//')
    JSA_FILE="${HOME}/.boxlang/cache/boxlang-${BL_VER}.jsa"
    if [[ -f "${JSA_FILE}" ]]; then
        ok "AppCDS cache: ${JSA_FILE}"
    else
        warn "AppCDS cache not found: ${JSA_FILE}"
        warn "Run 'boxlang --version' once first — it will auto-generate the cache (~3 s)."
        echo ""
        read -rp "  Continue anyway? [y/N] " REPLY
        [[ "${REPLY}" =~ ^[Yy]$ ]] || exit 0
    fi
fi
echo ""

# ══════════════════════════════════════════════════════════════════════════════
# TEST 1: --version
# ══════════════════════════════════════════════════════════════════════════════
hdr "TEST 1 — \`--version\`  (JVM startup overhead)"
echo ""

log "Running: boxlang2 --version  (no AppCDS)"
OUT_NO_CDS_V=$(run_benchmark "boxlang2-version" "${BOXLANG2}" --version)
NO_CDS_V_MIN=$(parse_result "${OUT_NO_CDS_V}" 3)
NO_CDS_V_AVG=$(parse_result "${OUT_NO_CDS_V}" 4)
NO_CDS_V_MAX=$(parse_result "${OUT_NO_CDS_V}" 5)
echo ""

log "Running: boxlang --version  (AppCDS)"
OUT_CDS_V=$(run_benchmark "boxlang-version" "${BOXLANG}" --version)
CDS_V_MIN=$(parse_result "${OUT_CDS_V}" 3)
CDS_V_AVG=$(parse_result "${OUT_CDS_V}" 4)
CDS_V_MAX=$(parse_result "${OUT_CDS_V}" 5)

print_comparison "--version" \
    "${NO_CDS_V_MIN}" "${NO_CDS_V_AVG}" "${NO_CDS_V_MAX}" \
    "${CDS_V_MIN}"    "${CDS_V_AVG}"    "${CDS_V_MAX}"

# ══════════════════════════════════════════════════════════════════════════════
# TEST 2: Script execution
# ══════════════════════════════════════════════════════════════════════════════
hdr "TEST 2 — \`$(basename "${BXS_SCRIPT}")\`  (startup + script execution)"
echo ""

log "Running: boxlang2 ${BXS_SCRIPT}  (no AppCDS)"
OUT_NO_CDS_S=$(run_benchmark "boxlang2-script" "${BOXLANG2}" "${BXS_SCRIPT}")
NO_CDS_S_MIN=$(parse_result "${OUT_NO_CDS_S}" 3)
NO_CDS_S_AVG=$(parse_result "${OUT_NO_CDS_S}" 4)
NO_CDS_S_MAX=$(parse_result "${OUT_NO_CDS_S}" 5)
echo ""

log "Running: boxlang ${BXS_SCRIPT}  (AppCDS)"
OUT_CDS_S=$(run_benchmark "boxlang-script" "${BOXLANG}" "${BXS_SCRIPT}")
CDS_S_MIN=$(parse_result "${OUT_CDS_S}" 3)
CDS_S_AVG=$(parse_result "${OUT_CDS_S}" 4)
CDS_S_MAX=$(parse_result "${OUT_CDS_S}" 5)

print_comparison "Script execution" \
    "${NO_CDS_S_MIN}" "${NO_CDS_S_AVG}" "${NO_CDS_S_MAX}" \
    "${CDS_S_MIN}"    "${CDS_S_AVG}"    "${CDS_S_MAX}"

# ══════════════════════════════════════════════════════════════════════════════
# Markdown report
# ══════════════════════════════════════════════════════════════════════════════
V_PCT=$(improvement_pct "${NO_CDS_V_AVG}" "${CDS_V_AVG}")
S_PCT=$(improvement_pct "${NO_CDS_S_AVG}" "${CDS_S_AVG}")

cat > "${OUTPUT_FILE}" << MDEOF
# BoxLang AppCDS Benchmark Results

**Date:** ${TIMESTAMP}
**Java:** \`${JAVA_VER}\`
**Runs:** ${RUNS} measured (${WARMUP} warmup discarded)

---

## Test 1: \`--version\` (JVM startup overhead)

| Variant | Min | Avg | Max |
|---------|----:|----:|----:|
| boxlang2 (no AppCDS) | ${NO_CDS_V_MIN}ms | ${NO_CDS_V_AVG}ms | ${NO_CDS_V_MAX}ms |
| boxlang  (AppCDS)    | ${CDS_V_MIN}ms    | ${CDS_V_AVG}ms    | ${CDS_V_MAX}ms    |

**AppCDS startup improvement: ${V_PCT}%**

---

## Test 2: Script execution (\`$(basename "${BXS_SCRIPT}")\`)

| Variant | Min | Avg | Max |
|---------|----:|----:|----:|
| boxlang2 (no AppCDS) | ${NO_CDS_S_MIN}ms | ${NO_CDS_S_AVG}ms | ${NO_CDS_S_MAX}ms |
| boxlang  (AppCDS)    | ${CDS_S_MIN}ms    | ${CDS_S_AVG}ms    | ${CDS_S_MAX}ms    |

**AppCDS script execution improvement: ${S_PCT}%**

---

## How to read these numbers

- Each run spawns a **fresh JVM process** — no process reuse — to simulate real CLI cold-start.
- \`boxlang\` picks up the AppCDS \`.jsa\` archive from \`~/.boxlang/cache/\` (auto-created on first run).
- \`boxlang2\` is the same launcher script without the AppCDS block — pure JVM baseline.
- Warmup rounds let the OS page-cache warm up before measurements begin.
- A negative improvement % means AppCDS is slower (stale/missing cache).
MDEOF

echo ""
ok "Markdown report → ${OUTPUT_FILE}"
echo ""
