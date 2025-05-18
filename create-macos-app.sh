#!/bin/bash

# Set variables
APP_NAME="Voting System"
JAR_FILE="target/hellofx-0.0.1-SNAPSHOT.jar"
VERSION="1.0.0"
BUNDLE_ID="org.main.voting"
COPYRIGHT="© $(date +%Y) Voting System"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found at $JAR_FILE"
    echo "Please run 'mvn clean package' first"
    exit 1
fi

# Create app directory structure
APP_DIR="$APP_NAME.app"
CONTENTS_DIR="$APP_DIR/Contents"
MACOS_DIR="$CONTENTS_DIR/MacOS"
RESOURCES_DIR="$CONTENTS_DIR/Resources"
JAVA_DIR="$RESOURCES_DIR/Java"

echo "Creating app bundle structure..."
mkdir -p "$MACOS_DIR" "$JAVA_DIR"

# Copy JAR file
echo "Copying JAR file..."
cp "$JAR_FILE" "$JAVA_DIR/"

# Find JavaFX SDK
find_javafx_sdk() {
    # Check common locations for JavaFX SDK
    POSSIBLE_LOCATIONS=(
        # Maven repository
        "$HOME/.m2/repository/org/openjfx"
        # Project target
        "target/dependency"
        # Custom JavaFX SDK installation
        "/Library/Java/JavaVirtualMachines/javafx-sdk-21"
        "/opt/javafx-sdk-21"
        "$HOME/javafx-sdk-21"
    )

    echo "Looking for JavaFX SDK..."
    
    # Check if JavaFX is embedded in the JAR (shaded)
    jar tf "$JAR_FILE" | grep -q "javafx/" && {
        echo "JavaFX appears to be embedded in your JAR file."
        echo "Creating a simpler launcher that doesn't need external JavaFX..."
        return 0
    }
    
    # Check Maven repository for JavaFX modules
    MAVEN_FX_CONTROLS="$HOME/.m2/repository/org/openjfx/javafx-controls/${javafx.version}"
    if [ -d "$MAVEN_FX_CONTROLS" ]; then
        echo "Found JavaFX in Maven repository: $HOME/.m2/repository/org/openjfx"
        JAVAFX_SDK="$HOME/.m2/repository/org/openjfx"
        return 0
    fi
    
    # Look in common locations
    for location in "${POSSIBLE_LOCATIONS[@]}"; do
        if [ -d "$location" ]; then
            echo "Found possible JavaFX SDK at: $location"
            JAVAFX_SDK="$location"
            return 0
        fi
    done
    
    return 1
}

# Try to find JavaFX SDK
if ! find_javafx_sdk; then
    echo "JavaFX SDK not found automatically."
    echo "You can:"
    echo "1. Download the JavaFX SDK from https://gluonhq.com/products/javafx/"
    echo "2. Install it with homebrew: brew install --cask javafx-sdk"
    echo "3. Extract it from your Maven repository if you use Maven"
    echo ""
    read -p "Enter path to JavaFX SDK (leave empty to create a launcher that assumes JavaFX is embedded): " JAVAFX_SDK
fi

# Create launcher script based on whether JavaFX SDK is specified
echo "Creating launcher script..."
if [ -n "$JAVAFX_SDK" ] && [ -d "$JAVAFX_SDK" ]; then
    echo "Using JavaFX SDK from: $JAVAFX_SDK"
    # Copy JavaFX libraries
    echo "Copying JavaFX libraries..."
    mkdir -p "$JAVA_DIR/lib"
    
    # First, try looking for the direct lib directory
    if [ -d "$JAVAFX_SDK/lib" ]; then
        cp "$JAVAFX_SDK/lib/"*.dylib "$JAVA_DIR/lib/" 2>/dev/null || true
        cp "$JAVAFX_SDK/lib/"*.jar "$JAVA_DIR/lib/" 2>/dev/null || true
    else
        # For Maven repository - we need to find the macOS-specific libraries
        FX_MODULES=("javafx-base" "javafx-controls" "javafx-fxml" "javafx-graphics" "javafx-media" "javafx-swing" "javafx-web")
        for module in "${FX_MODULES[@]}"; do
            # Find the JAR files
            find "$JAVAFX_SDK" -name "$module-*.jar" -not -path "*-javadoc.jar" -not -path "*-sources.jar" | grep -v "monocle" | head -1 | xargs -I{} cp {} "$JAVA_DIR/lib/" 2>/dev/null || true
            
            # Find the native libraries for macOS
            find "$JAVAFX_SDK" -name "$module-*-mac.jar" | head -1 | xargs -I{} cp {} "$JAVA_DIR/lib/" 2>/dev/null || true
            find "$JAVAFX_SDK" -name "*.dylib" | xargs -I{} cp {} "$JAVA_DIR/lib/" 2>/dev/null || true
        done
    fi
    
    # Create launcher script that uses bundled JavaFX
    cat > "$MACOS_DIR/VotingSystem" << 'EOF'
#!/bin/bash

# Get the directory where the script is located
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
RESOURCES_DIR="$DIR/../Resources"
JAVA_DIR="$RESOURCES_DIR/Java"

# Check if Java is installed
if ! command -v java &> /dev/null; then
    osascript -e 'tell app "System Events" to display dialog "Java is not installed. Please install Java and try again." buttons {"OK"} default button 1 with title "Java Not Found" with icon stop'
    exit 1
fi

# Set debug output
exec > "$HOME/Library/Logs/VotingSystem.log" 2>&1
echo "Starting Voting System Application at $(date)"
echo "Java location: $(which java)"
echo "Path: $PATH"
echo "Java version: $(java -version 2>&1)"
echo "App directory: $DIR"
echo "Resources directory: $RESOURCES_DIR"
echo "Java directory: $JAVA_DIR"
echo "Available files in Java directory:"
ls -la "$JAVA_DIR"
echo "Available files in lib directory:"
ls -la "$JAVA_DIR/lib"

# Launch the application
java \
    --module-path "$JAVA_DIR/lib" \
    --add-modules javafx.controls,javafx.fxml,javafx.graphics \
    -jar "$JAVA_DIR/hellofx-0.0.1-SNAPSHOT.jar"

exit_code=$?
echo "Application exited with code $exit_code at $(date)"
exit $exit_code
EOF
else
    echo "No JavaFX SDK specified. Creating a simpler launcher..."
    # Create a simple launcher that assumes JavaFX is embedded or available
    cat > "$MACOS_DIR/VotingSystem" << 'EOF'
#!/bin/bash

# Get the directory where the script is located
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
RESOURCES_DIR="$DIR/../Resources"
JAVA_DIR="$RESOURCES_DIR/Java"

# Check if Java is installed
if ! command -v java &> /dev/null; then
    osascript -e 'tell app "System Events" to display dialog "Java is not installed. Please install Java and try again." buttons {"OK"} default button 1 with title "Java Not Found" with icon stop'
    exit 1
fi

# Set debug output
exec > "$HOME/Library/Logs/VotingSystem.log" 2>&1
echo "Starting Voting System Application at $(date)"
echo "Java location: $(which java)"
echo "Path: $PATH"
echo "Java version: $(java -version 2>&1)"
echo "App directory: $DIR"
echo "Resources directory: $RESOURCES_DIR"
echo "Java directory: $JAVA_DIR"

# Try various launch methods
echo "Attempting to launch the application..."

echo "Method 1: Direct JAR execution"
java -jar "$JAVA_DIR/hellofx-0.0.1-SNAPSHOT.jar"
if [ $? -eq 0 ]; then
    echo "Application exited successfully at $(date)"
    exit 0
fi

echo "Method 2: With JavaFX modules"
java --module-path "$JAVA_DIR" --add-modules javafx.controls,javafx.fxml,javafx.graphics -jar "$JAVA_DIR/hellofx-0.0.1-SNAPSHOT.jar"
if [ $? -eq 0 ]; then
    echo "Application exited successfully at $(date)"
    exit 0
fi

echo "Method 3: With system JavaFX if available"
JAVAFX_PATHS=(
    "/Library/Java/JavaVirtualMachines/javafx-sdk-21/lib"
    "/opt/javafx-sdk-21/lib"
    "$HOME/javafx-sdk-21/lib"
    "$HOME/Library/Java/JavaVirtualMachines/javafx-sdk-21/lib"
)

for path in "${JAVAFX_PATHS[@]}"; do
    if [ -d "$path" ]; then
        echo "Found JavaFX at $path, trying to use it"
        java --module-path "$path" --add-modules javafx.controls,javafx.fxml,javafx.graphics -jar "$JAVA_DIR/hellofx-0.0.1-SNAPSHOT.jar"
        if [ $? -eq 0 ]; then
            echo "Application exited successfully at $(date)"
            exit 0
        fi
    fi
done

echo "All launch methods failed. Please make sure JavaFX is installed properly."
osascript -e 'tell app "System Events" to display dialog "Failed to launch Voting System. Check the log at ~/Library/Logs/VotingSystem.log for details." buttons {"OK"} default button 1 with title "Launch Failed" with icon stop'
exit 1
EOF
fi

# Make the launcher script executable
chmod +x "$MACOS_DIR/VotingSystem"

# Create Info.plist
echo "Creating Info.plist..."
cat > "$CONTENTS_DIR/Info.plist" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>English</string>
    <key>CFBundleExecutable</key>
    <string>VotingSystem</string>
    <key>CFBundleIdentifier</key>
    <string>$BUNDLE_ID</string>
    <key>CFBundleDisplayName</key>
    <string>$APP_NAME</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>CFBundleName</key>
    <string>$APP_NAME</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>$VERSION</string>
    <key>CFBundleVersion</key>
    <string>$VERSION</string>
    <key>NSHumanReadableCopyright</key>
    <string>$COPYRIGHT</string>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>NSSupportsAutomaticGraphicsSwitching</key>
    <true/>
</dict>
</plist>
EOF

# Create PkgInfo file (required for some macOS versions)
echo "Creating PkgInfo..."
echo "APPL????" > "$CONTENTS_DIR/PkgInfo"

echo "Application bundle created: $APP_DIR"
echo ""
echo "To run the application:"
echo "  1. Double-click the app bundle in Finder"
echo "  2. Or run: open \"$APP_DIR\""
echo ""
echo "Debug logs will be written to: ~/Library/Logs/VotingSystem.log"
echo "Check this file if the application doesn't start properly."
echo ""
echo "Note: You may need to right-click and select 'Open' the first time to bypass Gatekeeper."
