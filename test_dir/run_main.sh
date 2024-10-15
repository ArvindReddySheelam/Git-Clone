#!/bin/bash
cd src
javac Main.java
exec java Main "$@"

if [ "$1" == "init" ]; then
    # Create a .git directory and any necessary files
    mkdir -p .git/objects
    echo "Initialized empty Git repository"
else
    echo "Unknown command: $1"
fi
