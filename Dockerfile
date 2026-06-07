# Use a pre-made Android build environment for maximum reliability
# This image includes the Android SDK, Build Tools, and common dependencies pre-installed.
FROM mingc/android-build-box:latest

# Set working directory
WORKDIR /app

# Copy the project files
# The .dockerignore file ensures that local build artifacts aren't copied
COPY . .

# Grant execution permissions to gradlew
RUN chmod +x ./gradlew

# Default command: build the debug APK
# --no-daemon is used to prevent the Gradle process from staying alive in the container
CMD ["./gradlew", "assembleDebug", "--no-daemon"]
