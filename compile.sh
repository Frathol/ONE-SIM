#!/bin/bash

# Configuration variables
TARGETDIR="target"
SRCDIR="src"
LIBDIR="lib"
RESOURCESDIR="src/gui/buttonGraphics"

echo "[INFO] Starting build process..."

# 1. Create target directory if it doesn't exist
if [ ! -d "$TARGETDIR" ]; then
    echo "[INFO] Creating target directory..."
    mkdir -p "$TARGETDIR" || {
        echo "[ERROR] Failed to create target directory."
        exit 1
    }
fi

# 2. Build classpath from all JAR files
echo "[INFO] Building classpath..."
# In Linux, the separator is ":"
CLASSPATH=".:$TARGETDIR"
if [ ! -d "$LIBDIR" ]; then
    echo "[WARNING] Library directory '$LIBDIR' not found."
else
    for f in "$LIBDIR"/*.jar; do
        if [ -e "$f" ]; then
            CLASSPATH="$CLASSPATH:$f"
        fi
    done
fi

# 3. Find Lombok JAR for Annotation Processor
LOMBOK_JAR=$(find "$LIBDIR" -name "lombok.jar" -o -name "Lombok.jar" | head -n 1)

# 4. Verify source directory exists
if [ ! -d "$SRCDIR" ]; then
    echo "[ERROR] Source directory '$SRCDIR' not found."
    exit 1
fi

# 5. Collect all Java source files
echo "[INFO] Collecting Java source files..."
find "$SRCDIR" -name "*.java" > sources.txt
if [ ! -s sources.txt ]; then
    echo "[ERROR] No Java source files found in '$SRCDIR'."
    rm -f sources.txt
    exit 1
fi

# 6. Compile the sources
echo "[INFO] Compiling Java sources..."
# Added -processorpath for Lombok and explicit -cp for JUnit/ECLA
javac -sourcepath "$SRCDIR" \
      -d "$TARGETDIR" \
      -cp "$CLASSPATH" \
      -processorpath "$LOMBOK_JAR" \
      @sources.txt

if [ $? -ne 0 ]; then
    echo "[ERROR] Compilation failed."
    rm -f sources.txt
    exit 1
fi

# Clean up the sources list
rm -f sources.txt

# 7. Copy resources if they exist
if [ -d "$RESOURCESDIR" ]; then
    echo "[INFO] Copying resources..."
    mkdir -p "$TARGETDIR/gui/buttonGraphics"
    cp -r "$RESOURCESDIR"/* "$TARGETDIR/gui/buttonGraphics/"
    if [ $? -eq 0 ]; then
        echo "[INFO] Resources copied successfully."
    else
        echo "[WARNING] Some resources may not have been copied properly."
    fi
fi

echo "[INFO] Build completed successfully!"