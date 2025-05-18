#!/bin/bash

APP_NAME="VotingSystem"
VERSION="1.0.0"
JAR_FILE="target/hellofx-0.0.1-SNAPSHOT.jar"
TMP_DIR="build-tmp"
OUTPUT_FILE="$APP_NAME"
JAVA_DIR="$TMP_DIR/java"

# Clean old builds
rm -rf "$TMP_DIR" "$OUTPUT_FILE" package.tar.gz

# Check for JAR
if [ ! -f "$JAR_FILE" ]; then
    echo "JAR file not found. Run: mvn clean package"
    exit 1
fi

# Create temp structure
mkdir -p "$JAVA_DIR/lib"
cp "$JAR_FILE" "$JAVA_DIR/"

# Try to locate JavaFX
find_javafx_sdk() {
    LOCATIONS=(
        "$HOME/.m2/repository/org/openjfx"
        "/usr/share/openjfx"
        "/usr/lib/jvm/javafx-sdk-21"
        "$HOME/javafx-sdk-21"
        "/opt/javafx-sdk-21"
    )

    jar tf "$JAR_FILE" | grep -q "javafx/" && {
        echo "JavaFX appears embedded"
        return 0
    }

    for loc in "${LOCATIONS[@]}"; do
        if [ -d "$loc" ]; then
            echo "Found JavaFX SDK at $loc"
            JAVAFX_SDK="$loc"
            return 0
        fi
    done

    return 1
}

if ! find_javafx_sdk; then
    read -p "Enter path to JavaFX SDK (or leave empty if embedded): " JAVAFX_SDK
fi

if [ -n "$JAVAFX_SDK" ] && [ -d "$JAVAFX_SDK/lib" ]; then
    echo "Copying JavaFX libraries..."
    cp "$JAVAFX_SDK/lib/"*.jar "$JAVA_DIR/lib/" 2>/dev/null || true
fi

# Create the launcher script
cat > "$TMP_DIR/launch.sh" << 'EOF'
#!/bin/bash

# Extract path
APPDIR="$(dirname "$(realpath "$0")")"
JAVADIR="$APPDIR/java"

if ! command -v java &> /dev/null; then
    echo "Java is not installed. Please install Java."
    exit 1
fi

# Try to run
java -jar "$JAVADIR/hellofx-0.0.1-SNAPSHOT.jar"
EXIT_CODE=$?

if [ $EXIT_CODE -ne 0 ]; then
    echo "Retrying with JavaFX modules..."
    java --module-path "$JAVADIR/lib" --add-modules javafx.controls,javafx.fxml \
         -jar "$JAVADIR/hellofx-0.0.1-SNAPSHOT.jar"
    exit $?
fi

exit $EXIT_CODE
EOF

chmod +x "$TMP_DIR/launch.sh"

# Tar it
echo "Creating self-contained payload..."
tar -czf package.tar.gz -C "$TMP_DIR" .

# Create final executable
echo "Creating $OUTPUT_FILE..."
cat > "$OUTPUT_FILE" << 'EOF'
#!/bin/bash
# Self-extracting executable launcher for VotingSystem

APP_NAME="VotingSystem"
WORKDIR=$(mktemp -d /tmp/$APP_NAME.XXXXXX)

# Cleanup on exit
cleanup() {
    rm -rf "$WORKDIR"
}
trap cleanup EXIT

# Extract payload
ARCHIVE_LINE=$(awk '/^__ARCHIVE_BELOW__/ {print NR + 1; exit 0; }' "$0")
tail -n +$ARCHIVE_LINE "$0" | tar -xz -C "$WORKDIR"

# Launch the app
exec "$WORKDIR/launch.sh"
exit $?
__ARCHIVE_BELOW__
EOF

# Append archive to stub
cat package.tar.gz >> "$OUTPUT_FILE"
chmod +x "$OUTPUT_FILE"

# Cleanup
rm -rf "$TMP_DIR" package.tar.gz

echo ""
echo "✅ Single-file launcher created: ./$OUTPUT_FILE"
echo "Run it with:"
echo "  chmod +x $OUTPUT_FILE"
echo "  ./$(basename "$OUTPUT_FILE")"