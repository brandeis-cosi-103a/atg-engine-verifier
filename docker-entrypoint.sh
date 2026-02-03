#!/bin/sh
set -e

usage() {
    echo "ATG Engine Verifier"
    echo ""
    echo "Usage: docker run --rm -v \$(pwd)/target:/jars ghcr.io/brandeis-cosi-103a/atg-engine-verifier /jars/engine.jar com.example.MyEngine"
    echo ""
    echo "Arguments:"
    echo "  JAR_PATH      Path to the student's engine JAR (mounted via -v)"
    echo "  CLASS_NAME    Fully qualified class name of the Engine implementation"
    echo ""
    echo "Options:"
    echo "  --verbose     Show detailed output including game traces"
    echo "  --help        Show this help message"
    echo ""
    echo "Exit codes:"
    echo "  0 - Engine is compliant (all invariant checks passed)"
    echo "  1 - Violations detected (engine does not comply with game rules)"
    echo "  2 - Error (invalid arguments, JAR not found, class not found, etc.)"
    exit 2
}

# Check for help flag
if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
    usage
fi

# Parse arguments
VERBOSE=""
JAR_PATH=""
CLASS_NAME=""

while [ $# -gt 0 ]; do
    case "$1" in
        --verbose)
            VERBOSE="--verbose"
            shift
            ;;
        --help|-h)
            usage
            ;;
        *)
            if [ -z "$JAR_PATH" ]; then
                JAR_PATH="$1"
            elif [ -z "$CLASS_NAME" ]; then
                CLASS_NAME="$1"
            else
                echo "Error: Too many arguments"
                usage
            fi
            shift
            ;;
    esac
done

# Validate arguments
if [ -z "$JAR_PATH" ] || [ -z "$CLASS_NAME" ]; then
    echo "Error: Missing required arguments"
    echo ""
    usage
fi

# Check if JAR exists
if [ ! -f "$JAR_PATH" ]; then
    echo "Error: JAR file not found: $JAR_PATH"
    echo ""
    echo "Make sure to mount your JAR directory using -v:"
    echo "  docker run --rm -v \$(pwd)/target:/jars ghcr.io/brandeis-cosi-103a/atg-engine-verifier /jars/engine.jar com.example.MyEngine"
    exit 2
fi

# Run the verifier
exec java -cp "/app/verifier.jar:$JAR_PATH" edu.brandeis.cosi103a.verifier.VerifierHarness "$JAR_PATH" "$CLASS_NAME" $VERBOSE
