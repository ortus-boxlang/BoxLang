#!/bin/sh
######################################
# BoxLang Environment File Loader
# Loads environment variables from a .env file
# Usage: Source this script before executing BoxLang
######################################

# Default environment file
ENV_FILE=".env"

# Read --envfile=<path> out of the arguments. The arguments are not rebuilt or forwarded from
# here: BoxLang discards the flag itself, so they reach it exactly as they were given.
for arg in "$@"; do
    case "$arg" in
        --envfile=*)
            ENV_FILE="${arg#--envfile=}"
            ;;
    esac
done

# Load environment variables from the given file path
load_env_file() {
    local file="$1"
    [ -f "$file" ] || return 0
    # Uncomment for debugging: echo "Loading environment variables from $file"
    while IFS='=' read -r key value || [ -n "$key" ]; do
        # Skip comments and empty lines
        case "$key" in
            ''|'#'*)
                continue
                ;;
        esac
        # Remove leading/trailing whitespace from key
        key=$(echo "$key" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
        # Skip if key is empty after trimming
        [ -z "$key" ] && continue
        # Remove leading/trailing whitespace and quotes from value
        value=$(echo "$value" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//' | sed 's/^"\(.*\)"$/\1/' | sed "s/^'\(.*\)'$/\1/")
        export "$key=$value"
    done < "$file"
}

# Load the user's home secrets file first (~/.box.env)
load_env_file "$HOME/.box.env"

# Load the project-level environment file
load_env_file "$ENV_FILE"
