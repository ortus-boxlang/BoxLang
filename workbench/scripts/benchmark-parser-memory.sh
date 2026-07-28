#!/usr/bin/env bash
######################################
# BoxLang parser retained-memory benchmark
#
# Downloads a pinned ColdBox corpus into build/ and runs the benchmark in a
# fresh JVM. Run from any directory inside the repository.
######################################

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
CORPUS_DIR="${PROJECT_ROOT}/build/parser-memory/coldbox-platform"
COLDBOX_REPOSITORY="https://github.com/ColdBox/coldbox-platform.git"
COLDBOX_REVISION="389fc37eb19d519324dd55a046c7da318b6c843f"
GRADLE_ARGS=()
USE_DEFAULT_CORPUS=true

while [[ $# -gt 0 ]]; do
	case "$1" in
		--materialize-source-text) GRADLE_ARGS+=("-PmaterializeSourceText"); shift ;;
		--jfr) GRADLE_ARGS+=("-Pjfr"); shift ;;
		--limit) GRADLE_ARGS+=("-Plimit=$2"); shift 2 ;;
		--corpus) CORPUS_DIR="$2"; USE_DEFAULT_CORPUS=false; shift 2 ;;
		*) echo "Unknown argument: $1"; exit 1 ;;
	esac
done

if [[ "${USE_DEFAULT_CORPUS}" == true ]]; then
	if [[ ! -d "${CORPUS_DIR}/.git" ]]; then
		mkdir -p "$(dirname "${CORPUS_DIR}")"
		git clone "${COLDBOX_REPOSITORY}" "${CORPUS_DIR}"
	fi

	git -C "${CORPUS_DIR}" fetch --depth 1 origin "${COLDBOX_REVISION}"
	git -C "${CORPUS_DIR}" checkout --detach "${COLDBOX_REVISION}"
elif [[ ! -d "${CORPUS_DIR}" ]]; then
	echo "Corpus directory does not exist: ${CORPUS_DIR}"
	exit 1
fi

"${PROJECT_ROOT}/gradlew" parserMemoryBenchmark "-Pcorpus=${CORPUS_DIR}" "${GRADLE_ARGS[@]}"
