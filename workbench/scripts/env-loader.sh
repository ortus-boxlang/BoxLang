#!/bin/sh
######################################
# BoxLang Environment File Loader
# Loads environment variables from a .env file
# Usage: Source this script before executing BoxLang
######################################

# Default environment file
ENV_FILE=".env"

# Parse command line arguments to find --envfile parameter
# Store filtered arguments for later use
FILTERED_ARGS=""
for arg in "$@"; do
    case "$arg" in
        --envfile=*)
            ENV_FILE="${arg#--envfile=}"
            ;;
        *)
            if [ -z "$FILTERED_ARGS" ]; then
                FILTERED_ARGS="$arg"
            else
                FILTERED_ARGS="$FILTERED_ARGS $arg"
            fi
            ;;
    esac
done

# Load the environment file if it exists
if [ -f "$ENV_FILE" ]; then
    # Uncomment for debugging: echo "Loading environment variables from $ENV_FILE"
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

        # Export the variable
        export "$key=$value"
    done < "$ENV_FILE"
fi
