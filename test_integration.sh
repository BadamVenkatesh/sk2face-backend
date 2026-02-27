#!/usr/bin/env bash
# ================================================================
# SK2Face Backend – Comprehensive Integration Test Suite
# Tests EVERY route in EVERY service, both direct AND via API Gateway
# ================================================================
set -uo pipefail

# ── Colors ──
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

PASS=0
FAIL=0
TOTAL=0

# Ports
EUREKA=http://localhost:8761
AUTH=http://localhost:8081
USER_SVC=http://localhost:8082
MATCH=http://localhost:8083
ML=http://localhost:9090
GW=http://localhost:8080

# ── Helpers ──────────────────────────────────────────────────────
assert_status() {
    local label="$1" expected="$2" actual="$3" body="${4:-}"
    TOTAL=$((TOTAL + 1))
    if [ "$actual" = "$expected" ]; then
        echo -e "  ${GREEN}✅ PASS${NC} ${label}  (HTTP $actual)"
        PASS=$((PASS + 1))
    else
        echo -e "  ${RED}❌ FAIL${NC} ${label}  (expected $expected, got $actual)"
        [ -n "$body" ] && echo -e "        ${RED}Body: ${body:0:400}${NC}"
        FAIL=$((FAIL + 1))
    fi
}

section() {
    echo ""
    echo -e "${CYAN}${BOLD}═══════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}${BOLD}  $1${NC}"
    echo -e "${CYAN}${BOLD}═══════════════════════════════════════════════════════${NC}"
}

sub() {
    echo -e "  ${YELLOW}▶ $1${NC}"
}

wait_for() {
    local name="$1" url="$2" max="${3:-90}"
    echo -ne "  ${YELLOW}⏳ Waiting for ${name}...${NC}"
    for i in $(seq 1 "$max"); do
        if curl -sf -o /dev/null -w '' "$url" 2>/dev/null; then
            echo -e " ${GREEN}ready (${i}s)${NC}"
            return 0
        fi
        sleep 1
    done
    echo -e " ${RED}TIMEOUT after ${max}s${NC}"
    return 1
}

# ═══════════════════════════════════════════════════════════════
section "1. WAITING FOR SERVICES TO BE READY"
# ═══════════════════════════════════════════════════════════════
wait_for "Eureka"        "$EUREKA"                  90
wait_for "Auth Service"  "$AUTH/auth/validate"       120 || true
wait_for "User Service"  "$USER_SVC/api/users"       120 || true
wait_for "Match Service" "$MATCH/api/match/history"  120 || true
wait_for "ML Service"    "$ML/health"                120 || true
wait_for "API Gateway"   "$GW/actuator/health"       120 || true

# ═══════════════════════════════════════════════════════════════
section "2. EUREKA SERVER HEALTH"
# ═══════════════════════════════════════════════════════════════
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$EUREKA")
assert_status "Eureka dashboard (200)" "200" "$STATUS"

APPS_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$EUREKA/eureka/apps" -H "Accept: application/json")
assert_status "GET /eureka/apps (registry listing)" "200" "$APPS_STATUS"

# ═══════════════════════════════════════════════════════════════
section "3. ML SERVICE (port 9090) – Direct"
# ═══════════════════════════════════════════════════════════════

sub "3a. Health check"
RESP=$(curl -s -w "\n%{http_code}" "$ML/health")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
assert_status "GET /health" "200" "$CODE" "$BODY"
echo -e "     Body: $BODY"

sub "3b. POST /match with non-existent path (expect 400)"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$ML/match" \
    -H "Content-Type: application/json" \
    -d '{"image_path": "/app/data/photos/m1-004-01.jpg"}')
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
assert_status "POST /match (bad path → 400)" "400" "$CODE" "$BODY"

# ═══════════════════════════════════════════════════════════════
section "4. AUTH SERVICE (port 8081) – Direct"
# ═══════════════════════════════════════════════════════════════

TIMESTAMP=$(date +%s)
TEST_USER="testuser_${TIMESTAMP}"
TEST_PASS="TestPass123@"
TEST_EMAIL="${TEST_USER}@sk2face.test"

sub "4a. POST /auth/register"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$AUTH/auth/register" \
    -H "Content-Type: application/json" \
    -d "{
        \"username\": \"${TEST_USER}\",
        \"password\": \"${TEST_PASS}\",
        \"employeeId\": \"EMP${TIMESTAMP}\",
        \"fullName\": \"Test User ${TIMESTAMP}\",
        \"officialEmail\": \"${TEST_EMAIL}\",
        \"designation\": \"Engineer\",
        \"departmentName\": \"QA\",
        \"phoneNumber\": \"9876543210\"
    }")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
assert_status "POST /auth/register" "201" "$CODE" "$BODY"
echo -e "     Response: ${BODY:0:300}"

sub "4b. POST /auth/register (duplicate → 409)"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$AUTH/auth/register" \
    -H "Content-Type: application/json" \
    -d "{
        \"username\": \"${TEST_USER}\",
        \"password\": \"${TEST_PASS}\",
        \"employeeId\": \"EMP${TIMESTAMP}\",
        \"fullName\": \"Test User ${TIMESTAMP}\",
        \"officialEmail\": \"${TEST_EMAIL}\",
        \"designation\": \"Engineer\",
        \"departmentName\": \"QA\",
        \"phoneNumber\": \"9876543210\"
    }")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
# Expect 4xx (conflict or bad request)
if [[ "$CODE" == "409" || "$CODE" == "400" || "$CODE" == "500" ]]; then
    echo -e "  ${GREEN}✅ PASS${NC} POST /auth/register (duplicate → HTTP $CODE)"
    PASS=$((PASS + 1)); TOTAL=$((TOTAL + 1))
else
    echo -e "  ${RED}❌ FAIL${NC} POST /auth/register (expected 4xx for duplicate, got $CODE)"
    echo -e "     Body: ${BODY:0:300}"
    FAIL=$((FAIL + 1)); TOTAL=$((TOTAL + 1))
fi

sub "4c. POST /auth/login"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$AUTH/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\": \"${TEST_USER}\", \"password\": \"${TEST_PASS}\"}")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
assert_status "POST /auth/login" "200" "$CODE" "$BODY"
echo -e "     Response: ${BODY:0:300}"

ACCESS_TOKEN=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('accessToken',''))" 2>/dev/null || echo "")
REFRESH_TOKEN=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('refreshToken',''))" 2>/dev/null || echo "")

sub "4d. POST /auth/login (wrong password → 401)"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$AUTH/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\": \"${TEST_USER}\", \"password\": \"WrongPassword999\"}")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
if [[ "$CODE" == "401" || "$CODE" == "403" || "$CODE" == "400" ]]; then
    echo -e "  ${GREEN}✅ PASS${NC} POST /auth/login (bad creds → HTTP $CODE)"
    PASS=$((PASS + 1)); TOTAL=$((TOTAL + 1))
else
    echo -e "  ${RED}❌ FAIL${NC} POST /auth/login (expected 4xx for bad creds, got $CODE)"
    FAIL=$((FAIL + 1)); TOTAL=$((TOTAL + 1))
fi

if [ -z "$ACCESS_TOKEN" ]; then
    echo -e "  ${RED}⚠️  Could not extract accessToken — skipping token-dependent tests${NC}"
else
    echo -e "  ${GREEN}  ✓ accessToken extracted${NC}"
    echo -e "  ${GREEN}  ✓ refreshToken: ${REFRESH_TOKEN:0:20}...${NC}"

    sub "4e. GET /auth/validate (valid token → 200)"
    RESP=$(curl -s -w "\n%{http_code}" "$AUTH/auth/validate" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}")
    BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
    assert_status "GET /auth/validate (valid)" "200" "$CODE" "$BODY"
    echo -e "     Response: ${BODY:0:200}"

    sub "4f. GET /auth/validate (no token → 401)"
    RESP=$(curl -s -w "\n%{http_code}" "$AUTH/auth/validate")
    BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
    assert_status "GET /auth/validate (no token → 401)" "401" "$CODE" "$BODY"

    sub "4g. POST /auth/refresh"
    if [ -n "$REFRESH_TOKEN" ]; then
        RESP=$(curl -s -w "\n%{http_code}" -X POST "$AUTH/auth/refresh" \
            -H "Content-Type: application/json" \
            -d "{\"refreshToken\": \"${REFRESH_TOKEN}\"}")
        BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
        assert_status "POST /auth/refresh" "200" "$CODE" "$BODY"
        NEW_ACCESS=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('accessToken',''))" 2>/dev/null || echo "")
        [ -n "$NEW_ACCESS" ] && ACCESS_TOKEN="$NEW_ACCESS" && echo -e "     ${GREEN}✓ New token obtained${NC}"
    else
        echo -e "     ${YELLOW}⚠️ No refresh token available${NC}"
    fi

    sub "4h. POST /auth/logout"
    RESP=$(curl -s -w "\n%{http_code}" -X POST "$AUTH/auth/logout" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}")
    BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
    assert_status "POST /auth/logout" "200" "$CODE" "$BODY"

    sub "4i. GET /auth/validate (revoked token → 401)"
    RESP=$(curl -s -w "\n%{http_code}" "$AUTH/auth/validate" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}")
    BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
    assert_status "GET /auth/validate (after logout → 401)" "401" "$CODE" "$BODY"
fi

# Get a fresh valid token for remaining tests
sub "Fresh login for further testing"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$AUTH/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\": \"${TEST_USER}\", \"password\": \"${TEST_PASS}\"}")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
FRESH_TOKEN=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('accessToken',''))" 2>/dev/null || echo "")
FRESH_REFRESH=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('refreshToken',''))" 2>/dev/null || echo "")
if [ -n "$FRESH_TOKEN" ]; then
    echo -e "     ${GREEN}✓ Fresh token obtained for downstream tests${NC}"
else
    echo -e "     ${RED}⚠️ Could not get fresh token — gateway-authenticated tests may fail${NC}"
fi

# ═══════════════════════════════════════════════════════════════
section "5. USER SERVICE (port 8082) – Direct"
# ═══════════════════════════════════════════════════════════════

sub "5a. GET /api/users (list all)"
RESP=$(curl -s -w "\n%{http_code}" "$USER_SVC/api/users")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
assert_status "GET /api/users" "200" "$CODE" "$BODY"

sub "5b. POST /api/users (create)"
DIRECT_USER_ID=$((TIMESTAMP + 9000))
RESP=$(curl -s -w "\n%{http_code}" -X POST "$USER_SVC/api/users" \
    -H "Content-Type: application/json" \
    -d "{
        \"userId\": ${DIRECT_USER_ID},
        \"employeeId\": \"EMP_DT_${TIMESTAMP}\",
        \"fullName\": \"Direct Test User\",
        \"officialEmail\": \"direct_${TIMESTAMP}@sk2face.test\",
        \"designation\": \"Tester\",
        \"departmentName\": \"QA\",
        \"phoneNumber\": \"1234567890\"
    }")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
assert_status "POST /api/users (create)" "201" "$CODE" "$BODY"
echo -e "     Response: ${BODY:0:300}"

CREATED_ID=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('userId','') or d.get('data',{}).get('id',''))" 2>/dev/null || echo "")
echo -e "     Created user ID: ${CREATED_ID}"

if [ -n "$CREATED_ID" ]; then
    sub "5c. GET /api/users/${CREATED_ID}"
    RESP=$(curl -s -w "\n%{http_code}" "$USER_SVC/api/users/${CREATED_ID}")
    BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
    assert_status "GET /api/users/${CREATED_ID}" "200" "$CODE" "$BODY"

    sub "5d. PUT /api/users/${CREATED_ID} (update)"
    RESP=$(curl -s -w "\n%{http_code}" -X PUT "$USER_SVC/api/users/${CREATED_ID}" \
        -H "Content-Type: application/json" \
        -d "{\"fullName\": \"Updated Test User\", \"designation\": \"Senior Tester\"}")
    BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
    assert_status "PUT /api/users/${CREATED_ID}" "200" "$CODE" "$BODY"

    sub "5e. GET /api/users/${CREATED_ID} (non-existent large ID)"
    RESP=$(curl -s -w "\n%{http_code}" "$USER_SVC/api/users/999999")
    BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
    assert_status "GET /api/users/999999 (not found → 404)" "404" "$CODE" "$BODY"

    sub "5f. DELETE /api/users/${CREATED_ID}"
    RESP=$(curl -s -w "\n%{http_code}" -X DELETE "$USER_SVC/api/users/${CREATED_ID}")
    CODE=$(echo "$RESP" | tail -1)
    assert_status "DELETE /api/users/${CREATED_ID} (→ 204)" "204" "$CODE"
else
    echo -e "     ${YELLOW}⚠️ Could not extract user ID — skipping GET/PUT/DELETE tests${NC}"
    FAIL=$((FAIL + 3)); TOTAL=$((TOTAL + 3))
fi

# ═══════════════════════════════════════════════════════════════
section "6. MATCH SERVICE (port 8083) – Direct"
# ═══════════════════════════════════════════════════════════════

sub "6a. GET /api/match/history (with X-USER-ID header)"
RESP=$(curl -s -w "\n%{http_code}" "$MATCH/api/match/history" \
    -H "X-USER-ID: 1")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
assert_status "GET /api/match/history" "200" "$CODE" "$BODY"

sub "6b. GET /api/match/history (missing header → 400)"
RESP=$(curl -s -w "\n%{http_code}" "$MATCH/api/match/history")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
if [[ "$CODE" == "400" || "$CODE" == "500" ]]; then
    echo -e "  ${GREEN}✅ PASS${NC} GET /api/match/history (no header → HTTP $CODE)"
    PASS=$((PASS + 1)); TOTAL=$((TOTAL + 1))
else
    echo -e "  ${RED}❌ FAIL${NC} GET /api/match/history (expected 4xx without header, got $CODE)"
    FAIL=$((FAIL + 1)); TOTAL=$((TOTAL + 1))
fi

sub "6c. GET /api/match/history (paginated ?page=0&size=5)"
RESP=$(curl -s -w "\n%{http_code}" "$MATCH/api/match/history?page=0&size=5" \
    -H "X-USER-ID: 1")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
assert_status "GET /api/match/history?page=0&size=5" "200" "$CODE" "$BODY"

sub "6d. POST /api/match (ML integration — may fail if data not available)"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$MATCH/api/match" \
    -H "Content-Type: application/json" \
    -H "X-USER-ID: 1" \
    -d "{\"imageUrl\": \"/app/data/photos/m1-004-01.jpg\"}")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
if [ "$CODE" = "200" ]; then
    assert_status "POST /api/match (ML integration)" "200" "$CODE" "$BODY"
    echo -e "     Response: ${BODY:0:300}"
else
    echo -e "  ${YELLOW}⚠️ POST /api/match returned HTTP $CODE (ML data may not be present)${NC}"
    echo -e "     Body: ${BODY:0:300}"
    TOTAL=$((TOTAL + 1)); FAIL=$((FAIL + 1))
fi

# ═══════════════════════════════════════════════════════════════
section "7. API GATEWAY (port 8080) – All routes proxied through gateway"
# ═══════════════════════════════════════════════════════════════

sub "7a. GET /actuator/health (open path)"
RESP=$(curl -s -w "\n%{http_code}" "$GW/actuator/health")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
assert_status "GW GET /actuator/health (open)" "200" "$CODE" "$BODY"
echo -e "     Body: $BODY"

# ── Gateway open paths (no auth needed) ──────────────────────
sub "7b. POST /auth/register via Gateway (open path)"
GW_TIMESTAMP=$((TIMESTAMP + 1))
GW_USER="gwuser_${GW_TIMESTAMP}"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$GW/auth/register" \
    -H "Content-Type: application/json" \
    -d "{
        \"username\": \"${GW_USER}\",
        \"password\": \"${TEST_PASS}\",
        \"employeeId\": \"GWMP${GW_TIMESTAMP}\",
        \"fullName\": \"Gateway Test User\",
        \"officialEmail\": \"${GW_USER}@sk2face.test\",
        \"designation\": \"DevOps\",
        \"departmentName\": \"Engineering\",
        \"phoneNumber\": \"9001234567\"
    }")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
assert_status "GW POST /auth/register" "201" "$CODE" "$BODY"
echo -e "     Response: ${BODY:0:300}"

sub "7c. POST /auth/login via Gateway (open path)"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$GW/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\": \"${GW_USER}\", \"password\": \"${TEST_PASS}\"}")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
assert_status "GW POST /auth/login" "200" "$CODE" "$BODY"
echo -e "     Response: ${BODY:0:300}"

GW_TOKEN=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('accessToken',''))" 2>/dev/null || echo "")
GW_REFRESH=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('refreshToken',''))" 2>/dev/null || echo "")
[ -n "$GW_TOKEN" ] && echo -e "     ${GREEN}✓ GW token extracted${NC}" || echo -e "     ${RED}⚠️ GW token extraction failed${NC}"

sub "7d. GET /auth/validate via Gateway (open path)"
RESP=$(curl -s -w "\n%{http_code}" "$GW/auth/validate" \
    -H "Authorization: Bearer ${GW_TOKEN}")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
assert_status "GW GET /auth/validate (valid token)" "200" "$CODE" "$BODY"

sub "7e. POST /auth/refresh via Gateway (open path)"
if [ -n "$GW_REFRESH" ]; then
    RESP=$(curl -s -w "\n%{http_code}" -X POST "$GW/auth/refresh" \
        -H "Content-Type: application/json" \
        -d "{\"refreshToken\": \"${GW_REFRESH}\"}")
    BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
    assert_status "GW POST /auth/refresh" "200" "$CODE" "$BODY"
    NEW_GW=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('accessToken',''))" 2>/dev/null || echo "")
    [ -n "$NEW_GW" ] && GW_TOKEN="$NEW_GW" && echo -e "     ${GREEN}✓ Refreshed GW token${NC}"
fi

# ── Protected routes (require JWT via Gateway) ───────────────
sub "7f. Gateway auth filter — no token → 401 (protected /api/users)"
RESP=$(curl -s -w "\n%{http_code}" "$GW/api/users")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
assert_status "GW GET /api/users (no auth → 401)" "401" "$CODE" "$BODY"

sub "7g. Gateway auth filter — bad token → 401"
RESP=$(curl -s -w "\n%{http_code}" "$GW/api/users" \
    -H "Authorization: Bearer invalid.token.here")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
assert_status "GW GET /api/users (bad token → 401)" "401" "$CODE" "$BODY"

if [ -n "$GW_TOKEN" ]; then
    sub "7h. GW → User Service: GET /api/users (with valid JWT)"
    RESP=$(curl -s -w "\n%{http_code}" "$GW/api/users" \
        -H "Authorization: Bearer ${GW_TOKEN}")
    BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
    assert_status "GW GET /api/users (authenticated)" "200" "$CODE" "$BODY"

    sub "7i. GW → User Service: POST /api/users"
    GW_USER_TS=$((TIMESTAMP + 2))
    GW_DIRECT_UID=$((TIMESTAMP + 9001))
    RESP=$(curl -s -w "\n%{http_code}" -X POST "$GW/api/users" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer ${GW_TOKEN}" \
        -d "{
            \"userId\": ${GW_DIRECT_UID},
            \"employeeId\": \"GWDT${GW_USER_TS}\",
            \"fullName\": \"GW Created User\",
            \"officialEmail\": \"gwcreated_${GW_USER_TS}@sk2face.test\",
            \"designation\": \"Analyst\",
            \"departmentName\": \"IT\",
            \"phoneNumber\": \"9876000001\"
        }")
    BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
    assert_status "GW POST /api/users (create via GW)" "201" "$CODE" "$BODY"
    echo -e "     Response: ${BODY:0:300}"

    GW_CREATED_ID=$GW_DIRECT_UID
    echo -e "     GW-created user ID: ${GW_CREATED_ID}"

    if [ -n "$GW_CREATED_ID" ]; then
        sub "7j. GW → User Service: GET /api/users/${GW_CREATED_ID}"
        RESP=$(curl -s -w "\n%{http_code}" "$GW/api/users/${GW_CREATED_ID}" \
            -H "Authorization: Bearer ${GW_TOKEN}")
        BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
        assert_status "GW GET /api/users/${GW_CREATED_ID}" "200" "$CODE" "$BODY"

        sub "7k. GW → User Service: PUT /api/users/${GW_CREATED_ID}"
        RESP=$(curl -s -w "\n%{http_code}" -X PUT "$GW/api/users/${GW_CREATED_ID}" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer ${GW_TOKEN}" \
            -d "{\"fullName\": \"GW Updated User\", \"designation\": \"Lead Analyst\"}")
        BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
        assert_status "GW PUT /api/users/${GW_CREATED_ID}" "200" "$CODE" "$BODY"

        sub "7l. GW → User Service: DELETE /api/users/${GW_CREATED_ID}"
        RESP=$(curl -s -w "\n%{http_code}" -X DELETE "$GW/api/users/${GW_CREATED_ID}" \
            -H "Authorization: Bearer ${GW_TOKEN}")
        CODE=$(echo "$RESP" | tail -1)
        assert_status "GW DELETE /api/users/${GW_CREATED_ID}" "204" "$CODE"
    else
        echo -e "     ${YELLOW}⚠️ No GW user ID extracted — skipping GW user GET/PUT/DELETE${NC}"
        FAIL=$((FAIL + 3)); TOTAL=$((TOTAL + 3))
    fi

    sub "7m. GW → Match Service: GET /api/match/history (token → X-USER-ID injected by GW)"
    RESP=$(curl -s -w "\n%{http_code}" "$GW/api/match/history" \
        -H "Authorization: Bearer ${GW_TOKEN}")
    BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
    assert_status "GW GET /api/match/history (auth injected X-USER-ID)" "200" "$CODE" "$BODY"

    sub "7n. GW → Match Service: POST /api/match"
    RESP=$(curl -s -w "\n%{http_code}" -X POST "$GW/api/match" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer ${GW_TOKEN}" \
        -d "{\"imageUrl\": \"/app/data/photos/m1-004-01.jpg\"}")
    BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
    if [ "$CODE" = "200" ]; then
        assert_status "GW POST /api/match (ML integration via GW)" "200" "$CODE" "$BODY"
        echo -e "     Response: ${BODY:0:300}"
    else
        echo -e "  ${YELLOW}⚠️ GW POST /api/match → HTTP $CODE (ML data may not be available)${NC}"
        echo -e "     Body: ${BODY:0:300}"
        TOTAL=$((TOTAL + 1)); FAIL=$((FAIL + 1))
    fi

    sub "7o. POST /auth/logout via Gateway (invalidate GW token)"
    RESP=$(curl -s -w "\n%{http_code}" -X POST "$GW/auth/logout" \
        -H "Authorization: Bearer ${GW_TOKEN}")
    BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
    assert_status "GW POST /auth/logout" "200" "$CODE" "$BODY"

    sub "7p. Protected route after logout → 401"
    RESP=$(curl -s -w "\n%{http_code}" "$GW/api/users" \
        -H "Authorization: Bearer ${GW_TOKEN}")
    BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -1)
    assert_status "GW GET /api/users (revoked token → 401)" "401" "$CODE" "$BODY"
else
    echo -e "  ${YELLOW}⚠️ No GW token — skipping all authenticated gateway tests${NC}"
    FAIL=$((FAIL + 9)); TOTAL=$((TOTAL + 9))
fi

# ═══════════════════════════════════════════════════════════════
section "RESULTS SUMMARY"
# ═══════════════════════════════════════════════════════════════
echo ""
echo -e "  ${BOLD}Total: ${TOTAL}   ${GREEN}Passed: ${PASS}${NC}   ${RED}Failed: ${FAIL}${NC}"
echo ""
if [ "$FAIL" -eq 0 ]; then
    echo -e "  ${GREEN}${BOLD}🎉 ALL TESTS PASSED!${NC}"
else
    echo -e "  ${RED}${BOLD}⚠️  ${FAIL}/${TOTAL} TESTS FAILED — review details above${NC}"
fi
echo ""
