#!/bin/bash

# Dynamic FEL Analysis Script
# Compatible with GROMACS 2021.x and 2023.x
# Usage: ./fel_analysis_dynamic.sh <xtc_file> <tpr_file> <index_file> <output_dir> [options]

set -e

# ============================================================================
# CONFIGURATION & DEFAULTS
# ============================================================================

SCRIPT_NAME=$(basename "$0")
STEP=10
MAX_TIME=100

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ============================================================================
# USAGE FUNCTION
# ============================================================================

usage() {
    cat << EOF
Usage: $SCRIPT_NAME [OPTIONS] --xtc <file> --tpr <file> --ndx <file> --output <dir>

Required Options:
  --xtc FILE           Path to MD trajectory file (.xtc)
  --tpr FILE           Path to run input file (.tpr)
  --ndx FILE           Path to index file (.ndx)
  --output DIR         Output directory for results

Optional Options:
  --step SIZE          Time window size in ns (default: 10)
  --max-time TIME      Total simulation time in ns (default: 100)
  --help               Show this help message

Examples:
  $SCRIPT_NAME --xtc MD.xtc --tpr MD.tpr --ndx index.ndx --output FEL_OUTPUT
  $SCRIPT_NAME --xtc MD.xtc --tpr MD.tpr --ndx index.ndx --output FEL_OUTPUT --step 20 --max-time 200
EOF
    exit 1
}

# ============================================================================
# LOGGING FUNCTIONS
# ============================================================================

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

# ============================================================================
# PARSE ARGUMENTS
# ============================================================================

XTC_FILE=""
TPR_FILE=""
NDX_FILE=""
OUTPUT_DIR=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --xtc)
            XTC_FILE="$2"
            shift 2
            ;;
        --tpr)
            TPR_FILE="$2"
            shift 2
            ;;
        --ndx)
            NDX_FILE="$2"
            shift 2
            ;;
        --output)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        --step)
            STEP="$2"
            shift 2
            ;;
        --max-time)
            MAX_TIME="$2"
            shift 2
            ;;
        --help)
            usage
            ;;
        *)
            log_error "Unknown option: $1"
            usage
            ;;
    esac
done

# ============================================================================
# VALIDATE INPUTS
# ============================================================================

if [ -z "$XTC_FILE" ] || [ -z "$TPR_FILE" ] || [ -z "$NDX_FILE" ] || [ -z "$OUTPUT_DIR" ]; then
    log_error "Missing required options"
    usage
fi

if [ ! -f "$XTC_FILE" ]; then
    log_error "XTC file not found: $XTC_FILE"
    exit 1
fi

if [ ! -f "$TPR_FILE" ]; then
    log_error "TPR file not found: $TPR_FILE"
    exit 1
fi

if [ ! -f "$NDX_FILE" ]; then
    log_error "NDX file not found: $NDX_FILE"
    exit 1
fi

# ============================================================================
# CONVERT TO ABSOLUTE PATHS
# ============================================================================

# Convert relative paths to absolute paths BEFORE changing directory
XTC_FILE="$(cd "$(dirname "$XTC_FILE")" && pwd)/$(basename "$XTC_FILE")"
TPR_FILE="$(cd "$(dirname "$TPR_FILE")" && pwd)/$(basename "$TPR_FILE")"
NDX_FILE="$(cd "$(dirname "$NDX_FILE")" && pwd)/$(basename "$NDX_FILE")"

# ============================================================================
# SETUP
# ============================================================================

mkdir -p "$OUTPUT_DIR"
cd "$OUTPUT_DIR"

log_info "Starting FEL Analysis"
log_info "XTC: $XTC_FILE"
log_info "TPR: $TPR_FILE"
log_info "NDX: $NDX_FILE"
log_info "Output: $OUTPUT_DIR"
log_info "Step: ${STEP}ns, Max Time: ${MAX_TIME}ns"

echo "========================================="
echo "FEL Analysis - PCA"
echo "========================================="
echo ""

# ============================================================================
# STEP 1: PCA Covariance (C-alpha only)
# ============================================================================

log_info "Computing covariance matrix (C-alpha)..."
echo "3 3" | gmx covar \
    -f "$XTC_FILE" \
    -s "$TPR_FILE" \
    -n "$NDX_FILE" \
    -o eigenval.xvg \
    -v eigenvec.trr

if [ -f eigenval.xvg ]; then
    log_success "Eigenvalues saved to: eigenval.xvg"
else
    log_error "Failed to generate eigenval.xvg"
    exit 1
fi

echo ""

# ============================================================================
# STEP 2: PCA Projection (PC1 vs PC2)
# ============================================================================

log_info "Projecting trajectory onto first 2 PCs..."
echo "3 3" | gmx anaeig \
    -v eigenvec.trr \
    -f "$XTC_FILE" \
    -eig eigenval.xvg \
    -s "$TPR_FILE" \
    -first 1 \
    -last 2 \
    -2d pca_2dproj.xvg \
    -n "$NDX_FILE"

if [ -f pca_2dproj.xvg ]; then
    log_success "PC1 vs PC2 projection saved to: pca_2dproj.xvg"
else
    log_error "Failed to generate pca_2dproj.xvg"
    exit 1
fi

echo ""

# ============================================================================
# STEP 3: Free Energy Landscape (SHAM)
# ============================================================================

log_info "Computing free energy landscape..."
gmx sham \
    -f pca_2dproj.xvg \
    -ls FEL.xpm \
    -notime

if [ -f FEL.xpm ]; then
    log_success "Free energy landscape computed: FEL.xpm"
else
    log_error "Failed to generate FEL.xpm"
    exit 1
fi

echo ""

# ============================================================================
# STEP 4: Convert to viewable formats
# ============================================================================

log_info "Converting to PDF/EPS..."

gmx xpm2ps -f FEL.xpm -o FEL.eps -rainbow red 2>/dev/null || log_warning "xpm2ps unavailable"

if command -v convert &> /dev/null; then
    if convert FEL.eps FEL.pdf 2>/dev/null; then
        log_success "PDF generated: FEL.pdf"
    else
        log_warning "Failed to convert EPS to PDF"
    fi
else
    log_warning "ImageMagick (convert) not installed, skipping EPS->PDF conversion"
fi

echo ""

# ============================================================================
# SUMMARY
# ============================================================================

echo "========================================="
log_success "ANALYSIS COMPLETE"
echo "========================================="
echo ""
echo "Output files in: $(pwd)"
echo ""

# List output files
echo "Generated files:"
for file in eigenval.xvg eigenvec.trr pca_2dproj.xvg FEL.xpm FEL.eps FEL.pdf; do
    if [ -f "$file" ]; then
        size=$(ls -lh "$file" | awk '{print $5}')
        echo "  ✓ $file ($size)"
    fi
done

echo ""
echo "Output Summary:"
echo ""
echo "1. eigenval.xvg"
echo "   - Eigenvalues (variance explained by each PC)"
echo "   - PC1 typically explains 30-60% of motion"
echo "   - PC2 typically explains 10-20% of motion"
echo ""
echo "2. pca_2dproj.xvg"
echo "   - PC1 vs PC2 coordinates for each frame"
echo "   - 3 columns: Frame#, PC1value, PC2value"
echo ""
echo "3. FEL.xpm / FEL.pdf / FEL.eps"
echo "   - 2D/3D Free Energy Landscape"
echo "   - X-axis = PC1 (first principal component)"
echo "   - Y-axis = PC2 (second principal component)"
echo "   - Colors = Free energy (blue=stable, red=unstable)"
echo ""

log_success "Analysis finished at $(date)"