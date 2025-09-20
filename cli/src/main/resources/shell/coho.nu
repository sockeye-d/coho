# The Kotlin programmer's static website generator
extern coho [
    path: path = "main.coho.kts" # Path to the coho script file
    --build-path (-B): path = "build" # Path to the build directory
    --verbose # Show extra information
    --force # Force overwrite existing files to create a new coho project
    --create # Create a new coho script file
    --no-progress # Don't show progress
    --debug-times # Show execution times
]

# Show a live-updating localhost webserver
extern "coho serve" [
    path: path = "main.coho.kts" # Path to the coho script file
    --build-path (-B): path = "build" # Path to the build directory
    --verbose # Show extra information
    --force # Force overwrite existing files to create a new coho project
    --create # Create a new coho script file
    --no-progress # Don't show progress
    --debug-times # Show execution times
    --server-port: int = 8080 # Integrated server port
    --port-retry-count: int = 65535 # How many times to try different ports if the current port is already in use (default: infinite)
    --no-reload-script # Don't inject the hot reload script into HTML files (only when using integrated server, it's never included in build outputs)
]