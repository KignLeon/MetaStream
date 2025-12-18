#!/bin/bash

# MetaStream Live - Automated Pre-Demo Validation Script
# Run this before your demo to ensure everything works

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
PASS=0
FAIL=0
WARN=0

# Helper functions
print_header() {
    echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"
}

test_pass() {
    echo -e "${GREEN}✅ PASS:${NC} $1"
    ((PASS++))
}

test_fail() {
    echo -e "${RED}❌ FAIL:${NC} $1"
    ((FAIL++))
}

test_warn() {
    echo -e "${YELLOW}⚠️  WARN:${NC} $1"
    ((WARN++))
}

test_info() {
    echo -e "   ℹ️  $1"
}

# ============================================================================
# PHASE 1: Environment Checks
# ============================================================================
print_header "Phase 1: Environment Validation"

# Test 1.1: Java
echo -n "Testing Java installation... "
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
    if [[ "$JAVA_VERSION" =~ ^(17|[2-9][0-9]) ]]; then
        test_pass "Java $JAVA_VERSION detected"
    else
        test_fail "Java version $JAVA_VERSION is < 17"
    fi
else
    test_fail "Java not found in PATH"
fi

# Test 1.2: Maven
echo -n "Testing Maven installation... "
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version 2>&1 | head -1 | cut -d' ' -f3)
    test_pass "Maven $MVN_VERSION detected"
else
    test_fail "Maven not found in PATH"
fi

# Test 1.3: Node.js
echo -n "Testing Node.js installation... "
if command -v node &> /dev/null; then
    NODE_VERSION=$(node -v)
    test_pass "Node.js $NODE_VERSION detected"
else
    test_fail "Node.js not found in PATH"
fi

# Test 1.4: FFmpeg
echo -n "Testing FFmpeg installation... "
if command -v ffmpeg &> /dev/null; then
    FFMPEG_VERSION=$(ffmpeg -version 2>&1 | head -1 | cut -d' ' -f3)
    test_pass "FFmpeg $FFMPEG_VERSION detected"
else
    test_fail "FFmpeg not found in PATH"
fi

# Test 1.5: Port availability
echo -n "Checking port 8080 availability... "
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null 2>&1; then
    test_warn "Port 8080 already in use (may be Java backend already running)"
else
    test_pass "Port 8080 available"
fi

echo -n "Checking port 8000 availability... "
if lsof -Pi :8000 -sTCP:LISTEN -t >/dev/null 2>&1; then
    test_warn "Port 8000 already in use (may be FFmpeg server already running)"
else
    test_pass "Port 8000 available"
fi

echo -n "Checking port 1935 availability... "
if lsof -Pi :1935 -sTCP:LISTEN -t >/dev/null 2>&1; then
    test_warn "Port 1935 already in use (may be RTMP server already running)"
else
    test_pass "Port 1935 available"
fi

# ============================================================================
# PHASE 2: Project Structure Validation
# ============================================================================
print_header "Phase 2: Project Structure Validation"

# Test 2.1: pom.xml exists
if [ -f "pom.xml" ]; then
    test_pass "pom.xml found"
    
    # Check for required dependencies
    if grep -q "spark-core" pom.xml; then
        test_pass "Spark dependency present"
    else
        test_fail "Spark dependency missing from pom.xml"
    fi
    
    if grep -q "gson" pom.xml; then
        test_pass "Gson dependency present"
    else
        test_fail "Gson dependency missing from pom.xml"
    fi
    
    if grep -q "slf4j-simple" pom.xml; then
        test_pass "SLF4J logging configured"
    else
        test_fail "SLF4J dependency missing from pom.xml"
    fi
else
    test_fail "pom.xml not found - are you in project root?"
    exit 1
fi

# Test 2.2: Source files exist
REQUIRED_FILES=(
    "src/main/java/com/mts/Main.java"
    "src/main/java/com/mts/User.java"
    "src/main/java/com/mts/StreamSession.java"
    "src/main/java/com/mts/ChatMessage.java"
    "src/main/java/com/mts/FileLogger.java"
    "src/main/java/com/mts/NotificationService.java"
    "src/main/java/com/mts/SMSNotifier.java"
    "src/main/java/com/mts/TTSNotifier.java"
    "src/main/java/com/mts/WebSocketHandler.java"
    "src/main/java/com/mts/MediaServerClient.java"
)

for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        test_pass "$(basename $file) exists"
    else
        test_fail "$(basename $file) missing"
    fi
done

# Test 2.3: HTML files exist
HTML_FILES=(
    "src/main/resources/public/index.html"
    "src/main/resources/public/livedash.html"
    "src/main/resources/public/viewer.html"
    "src/main/resources/public/summary.html"
)

for file in "${HTML_FILES[@]}"; do
    if [ -f "$file" ]; then
        test_pass "$(basename $file) exists"
    else
        test_fail "$(basename $file) missing"
    fi
done

# Test 2.4: Media server files
if [ -d "media-server" ]; then
    test_pass "media-server directory exists"
    
    if [ -f "media-server/package.json" ]; then
        test_pass "package.json exists"
    else
        test_fail "media-server/package.json missing"
    fi
    
    if [ -f "media-server/app.js" ]; then
        test_pass "app.js exists"
    else
        test_fail "media-server/app.js missing"
    fi
else
    test_fail "media-server directory not found"
fi

# ============================================================================
# PHASE 3: Learning Outcomes Code Validation
# ============================================================================
print_header "Phase 3: Learning Outcomes Validation"

# LO1: Encapsulation
echo -n "Checking LO1 (Encapsulation)... "
if grep -q "private.*username" src/main/java/com/mts/User.java; then
    if grep -q "public.*getUsername" src/main/java/com/mts/User.java; then
        test_pass "LO1 - Encapsulation implemented (private fields + public getters)"
    else
        test_fail "LO1 - Missing getters for private fields"
    fi
else
    test_fail "LO1 - No private fields found in User.java"
fi

# LO2: Arrays
echo -n "Checking LO2 (Arrays/Collections)... "
if grep -q "ArrayList<ChatMessage>" src/main/java/com/mts/StreamSession.java; then
    test_pass "LO2 - ArrayList<ChatMessage> implemented"
else
    test_fail "LO2 - ArrayList<ChatMessage> not found in StreamSession"
fi

# LO3: Aggregation
echo -n "Checking LO3 (Aggregation)... "
if grep -q "private.*User user" src/main/java/com/mts/StreamSession.java; then
    test_pass "LO3 - User object aggregated in StreamSession"
else
    test_fail "LO3 - User aggregation not found"
fi

# LO4: Inheritance/Polymorphism
echo -n "Checking LO4 (Polymorphism)... "
if [ -f "src/main/java/com/mts/NotificationService.java" ]; then
    if grep -q "interface NotificationService" src/main/java/com/mts/NotificationService.java; then
        if grep -q "implements NotificationService" src/main/java/com/mts/SMSNotifier.java; then
            test_pass "LO4 - Interface and implementations present"
        else
            test_fail "LO4 - SMSNotifier doesn't implement interface"
        fi
    else
        test_fail "LO4 - NotificationService is not an interface"
    fi
else
    test_fail "LO4 - NotificationService.java not found"
fi

# LO5: Generic Collections
echo -n "Checking LO5 (Generics)... "
if grep -q "List<ChatMessage>" src/main/java/com/mts/StreamSession.java; then
    test_pass "LO5 - Generic collection List<ChatMessage> used"
else
    test_fail "LO5 - No generic collection found"
fi

# LO6: GUI/Events
echo -n "Checking LO6 (GUI/Events)... "
if grep -q "post(\"/api/stream/start" src/main/java/com/mts/Main.java; then
    test_pass "LO6 - Event-driven HTTP endpoints present"
else
    test_fail "LO6 - No event endpoints found in Main.java"
fi

# LO7: Exception Handling
echo -n "Checking LO7 (Exception Handling)... "
if grep -q "try.*catch" src/main/java/com/mts/FileLogger.java; then
    test_pass "LO7 - Try-catch blocks implemented in FileLogger"
else
    test_fail "LO7 - No exception handling in FileLogger"
fi

# LO8: File I/O
echo -n "Checking LO8 (File I/O)... "
if grep -q "FileWriter" src/main/java/com/mts/FileLogger.java; then
    if grep -q "readString" src/main/java/com/mts/FileLogger.java; then
        test_pass "LO8 - File read/write operations implemented"
    else
        test_fail "LO8 - File read operation missing"
    fi
else
    test_fail "LO8 - FileWriter not found in FileLogger"
fi

# ============================================================================
# PHASE 4: Build Test
# ============================================================================
print_header "Phase 4: Build Validation"

echo "Running Maven clean package..."
if mvn clean package -q -DskipTests > /tmp/mvn-build.log 2>&1; then
    test_pass "Maven build successful"
    
    if [ -f "target/metastream-1.0-SNAPSHOT.jar" ]; then
        JAR_SIZE=$(du -h target/metastream-1.0-SNAPSHOT.jar | cut -f1)
        test_pass "JAR file created (Size: $JAR_SIZE)"
    else
        test_fail "JAR file not found in target/"
    fi
else
    test_fail "Maven build failed - check /tmp/mvn-build.log for details"
    cat /tmp/mvn-build.log
fi

# ============================================================================
# PHASE 5: Media Server Validation
# ============================================================================
print_header "Phase 5: Media Server Validation"

if [ -d "media-server" ]; then
    cd media-server
    
    echo "Checking npm dependencies..."
    if [ -d "node_modules" ]; then
        test_pass "node_modules directory exists"
    else
        test_warn "node_modules not found - run 'npm install' in media-server/"
    fi
    
    # Check for required npm packages
    if [ -f "package.json" ]; then
        if grep -q "express" package.json; then
            test_pass "Express dependency declared"
        else
            test_fail "Express dependency missing"
        fi
    fi
    
    cd ..
else
    test_fail "media-server directory not found"
fi

# ============================================================================
# FINAL REPORT
# ============================================================================
print_header "Validation Summary"

echo -e "${GREEN}✅ Passed: $PASS${NC}"
echo -e "${RED}❌ Failed: $FAIL${NC}"
echo -e "${YELLOW}⚠️  Warnings: $WARN${NC}"
echo ""

if [ $FAIL -eq 0 ]; then
    echo -e "${GREEN}🎉 ALL CRITICAL TESTS PASSED!${NC}"
    echo ""
    echo "Your project is ready for demo. Next steps:"
    echo "1. Start FFmpeg server: cd media-server && npm start"
    echo "2. Start Java backend: java -jar target/metastream-1.0-SNAPSHOT.jar"
    echo "3. Open http://localhost:8080 in browser"
    echo ""
    exit 0
else
    echo -e "${RED}⚠️  CRITICAL ISSUES FOUND${NC}"
    echo ""
    echo "Fix these issues before demo:"
    echo "1. Review failed tests above"
    echo "2. Check build logs in /tmp/mvn-build.log"
    echo "3. Ensure all source files are present"
    echo ""
    exit 1
fi