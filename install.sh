#!/bin/sh

set -e

# Constants
BINARY_NAME="databricks-connect-installer"
GITHUB_REPO="wanishing/databricks-connect-installer"
INSTALL_DIR="${HOME}/.local/bin"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "${BLUE}🔌 Installing Databricks Connect Installer...${NC}"

# Detect OS and architecture
OS="$(uname -s)"
ARCH="$(uname -m)"

case "$OS" in
    "Darwin") OS="macos" ;;
    "Linux") OS="linux" ;;
    *) echo "${RED}Error: Unsupported operating system: $OS${NC}" && exit 1 ;;
esac

case "$ARCH" in
    "x86_64") ARCH="amd64" ;;
    "arm64"|"aarch64") ARCH="arm64" ;;
    *) echo "${RED}Error: Unsupported architecture: $ARCH${NC}" && exit 1 ;;
esac

# Get the latest release version
echo "${BLUE}📦 Fetching latest release...${NC}"
echo "${BLUE}Checking GitHub API response:${NC}"
curl -v "https://api.github.com/repos/${GITHUB_REPO}/releases/latest"
LATEST_RELEASE=$(curl -s "https://api.github.com/repos/${GITHUB_REPO}/releases/latest" | grep '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/')

if [ -z "$LATEST_RELEASE" ]; then
    echo "${RED}Error: Could not determine latest release version${NC}"
    exit 1
fi

VERSION=${LATEST_RELEASE#v}  # Remove 'v' prefix if present
echo "${BLUE}📥 Downloading version ${VERSION}...${NC}"

# Create download URL
DOWNLOAD_URL="https://github.com/${GITHUB_REPO}/releases/download/${LATEST_RELEASE}/${BINARY_NAME}-${VERSION}-${OS}-latest.zip"
echo "${BLUE}Downloading from: ${DOWNLOAD_URL}${NC}"

# Create temporary directory
TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

# Download and extract
echo "${BLUE}Downloading installer...${NC}"
echo "${BLUE}Temporary directory: ${TMP_DIR}${NC}"
curl -v -L "$DOWNLOAD_URL" -o "$TMP_DIR/installer.zip"
echo "${BLUE}Download complete. File size:${NC}"
ls -lh "$TMP_DIR/installer.zip"

if ! curl -L --fail --progress-bar "$DOWNLOAD_URL" -o "$TMP_DIR/installer.zip"; then
    echo "${RED}Error: Failed to download installer${NC}"
    echo "${RED}URL: ${DOWNLOAD_URL}${NC}"
    exit 1
fi

echo "${BLUE}Extracting installer...${NC}"
if ! unzip -q "$TMP_DIR/installer.zip" -d "$TMP_DIR"; then
    echo "${RED}Error: Failed to extract installer${NC}"
    exit 1
fi

# Create install directory if it doesn't exist
mkdir -p "$INSTALL_DIR"

# Move binary to install directory
echo "${BLUE}Installing to ${INSTALL_DIR}...${NC}"
if ! mv "$TMP_DIR/${BINARY_NAME}" "$INSTALL_DIR/"; then
    echo "${RED}Error: Failed to install binary${NC}"
    exit 1
fi
chmod +x "$INSTALL_DIR/${BINARY_NAME}"

echo "${GREEN}✅ Successfully installed Databricks Connect Installer!${NC}"
echo
echo "To use the installer, run:"
echo "  ${BLUE}${BINARY_NAME}${NC}"
echo
echo "Make sure ${BLUE}$INSTALL_DIR${NC} is in your PATH."

# Check if install directory is in PATH
if ! echo "$PATH" | grep -q "$INSTALL_DIR"; then
    echo
    echo "${RED}Warning: $INSTALL_DIR is not in your PATH${NC}"
    echo "Add the following to your shell configuration file (.bashrc, .zshrc, etc.):"
    echo "  ${BLUE}export PATH=\"\$PATH:$INSTALL_DIR\"${NC}"
fi 