######################################
# Launcher config: files, then environment variables
######################################
# Nothing here knows about a specific module. Everything is derived from the launcher that was
# invoked, least specific first:
#   files: boxlang.{options,properties} in the BoxLang home, then <launcher>.{options,properties}
#   env:   BOXLANG_OPTS / BOXLANG_PROPS, then <LAUNCHER>_OPTS / <LAUNCHER>_PROPS
# Later sources win, because the JVM uses the last occurrence of a repeated option.
# A module launcher records itself in BOXLANG_PROCESS_PATH; a direct run falls back to this
# launcher. The BoxLang home is resolved the same way the AppCDS cache directory is.
_BL_CFG_JVM_ARGS=""

if [ -n "${BOXLANG_HOME:-}" ]; then
    _BL_CFG_HOME="$BOXLANG_HOME"
else
    _BL_CFG_HOME=""
    _bl_cfg_prev=""
    for _bl_cfg_arg in "$@"; do
        if [ "$_bl_cfg_prev" = "--bx-home" ]; then
            _BL_CFG_HOME="$_bl_cfg_arg"
            _bl_cfg_prev=""
            continue
        fi
        case "$_bl_cfg_arg" in
            --bx-home=*) _BL_CFG_HOME="${_bl_cfg_arg#--bx-home=}" ;;
            --bx-home) _bl_cfg_prev="--bx-home" ;;
        esac
    done
    [ -z "$_BL_CFG_HOME" ] && _BL_CFG_HOME="$HOME/.boxlang"
fi

if [ -n "${BOXLANG_PROCESS_PATH:-}" ]; then
    _BL_CFG_LAUNCHER="$BOXLANG_PROCESS_PATH"
else
    _BL_CFG_LAUNCHER="$0"
    # Resolve a bare command name so a PATH lookup finds the launcher
    case "$_BL_CFG_LAUNCHER" in
        */*) ;;
        *) _BL_CFG_LAUNCHER="$(command -v -- "$_BL_CFG_LAUNCHER" 2>/dev/null || printf %s "$_BL_CFG_LAUNCHER")" ;;
    esac
fi
# The name comes from the launcher as invoked, so a symlink keeps its own name for the
# environment variable prefix, while its target is used to find the adjacent files.
_BL_CFG_NAME="${_BL_CFG_LAUNCHER##*/}"
case "$_BL_CFG_LAUNCHER" in
    */*) _BL_CFG_DIR="$(CDPATH= cd -P "${_BL_CFG_LAUNCHER%/*}" 2>/dev/null && pwd -P)" ;;
    *) _BL_CFG_DIR="." ;;
esac
_BL_CFG_STEM="${_BL_CFG_NAME%.*}"
_BL_CFG_BASE="$_BL_CFG_DIR/$_BL_CFG_STEM"

# Prefix for the process specific env vars: box -> BOX, boxlang-miniserver -> BOXLANG_MINISERVER
_BL_CFG_ENV_NAME="$(printf '%s' "$_BL_CFG_STEM" | tr '[:lower:]' '[:upper:]' | tr -c 'A-Z0-9_\n' '_')"

# Raw JVM arguments, one per line, # for comments, collected from a base path
_bl_cfg_add_options() {
    [ -f "$1.options" ] || return 0
    while IFS= read -r _bl_cfg_line || [ -n "$_bl_cfg_line" ]; do
        case "$_bl_cfg_line" in
            ''|'#'*) continue ;;
        esac
        _BL_CFG_JVM_ARGS="$_BL_CFG_JVM_ARGS $_bl_cfg_line"
    done < "$1.options"
}

# System properties, one key=value per line, # for comments, collected from a base path
_bl_cfg_add_properties() {
    [ -f "$1.properties" ] || return 0
    while IFS='=' read -r _bl_cfg_key _bl_cfg_value || [ -n "$_bl_cfg_key" ]; do
        case "$_bl_cfg_key" in
            ''|'#'*) continue ;;
        esac
        [ -z "$_bl_cfg_value" ] && continue
        _BL_CFG_JVM_ARGS="$_BL_CFG_JVM_ARGS -D$_bl_cfg_key=$_bl_cfg_value"
    done < "$1.properties"
}

# Semicolon delimited key=value pairs, the same format the CommandBox property variables use
_bl_cfg_add_props_var() {
    [ -n "$1" ] || return 0
    _bl_cfg_props="$1;"
    while [ -n "$_bl_cfg_props" ]; do
        _bl_cfg_piece="${_bl_cfg_props%%;*}"
        _bl_cfg_props="${_bl_cfg_props#*;}"
        [ -n "$_bl_cfg_piece" ] || continue
        _bl_cfg_key="${_bl_cfg_piece%%=*}"
        [ "$_bl_cfg_key" = "$_bl_cfg_piece" ] && continue
        _bl_cfg_value="${_bl_cfg_piece#*=}"
        [ -n "$_bl_cfg_key" ] && _BL_CFG_JVM_ARGS="$_BL_CFG_JVM_ARGS -D$_bl_cfg_key=$_bl_cfg_value"
    done
}

# Least specific first
_bl_cfg_add_options "$_BL_CFG_HOME/boxlang"
_bl_cfg_add_properties "$_BL_CFG_HOME/boxlang"
_bl_cfg_add_options "$_BL_CFG_BASE"
_bl_cfg_add_properties "$_BL_CFG_BASE"

# General BoxLang layer
[ -n "${BOXLANG_OPTS:-}" ] && _BL_CFG_JVM_ARGS="$_BL_CFG_JVM_ARGS $BOXLANG_OPTS"
_bl_cfg_add_props_var "${BOXLANG_PROPS:-}"

# Process layer, named after the launcher that was invoked. Skipped when that launcher is boxlang
# itself, since BOXLANG_OPTS / BOXLANG_PROPS were just applied.
if [ "$_BL_CFG_ENV_NAME" != "BOXLANG" ]; then
    eval "_bl_cfg_specific_args=\${${_BL_CFG_ENV_NAME}_OPTS:-}"
    eval "_bl_cfg_specific_props=\${${_BL_CFG_ENV_NAME}_PROPS:-}"
    [ -n "$_bl_cfg_specific_args" ] && _BL_CFG_JVM_ARGS="$_BL_CFG_JVM_ARGS $_bl_cfg_specific_args"
    _bl_cfg_add_props_var "$_bl_cfg_specific_props"
fi
######################################
# End launcher config
######################################
