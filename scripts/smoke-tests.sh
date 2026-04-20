#!/bin/bash
# Smoke Tests Script for Travel Plan
# Usage: ./smoke-tests.sh [environment]

set -e

ENVIRONMENT=${1:-staging}
echo "Running smoke tests for environment: $ENVIRONMENT"

# Set base URL based on environment
case $ENVIRONMENT in
  production)
    BASE_URL="https://api.travelplan.com"
    ;;
  staging)
    BASE_URL="https://staging-api.travelplan.com"
    ;;
  *)
    BASE_URL="http://localhost:8080"
    ;;
esac

echo "Base URL: $BASE_URL"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

TESTS_PASSED=0
TESTS_FAILED=0

# Helper function to run a test
run_test() {
    local name=$1
    local url=$2
    local expected_status=${3:-200}
    
    echo -n "Testing: $name... "
    
    status=$(curl -s -o /dev/null -w "%{http_code}" "$url" --max-time 10 || echo "000")
    
    if [ "$status" -eq "$expected_status" ]; then
        echo -e "${GREEN}PASSED${NC} (HTTP $status)"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}FAILED${NC} (Expected $expected_status, got $status)"
        ((TESTS_FAILED++))
    fi
}

# Health checks
echo ""
echo "=== Health Check Tests ==="
run_test "Auth Service Health" "$BASE_URL/auth/actuator/health"
run_test "User Service Health" "$BASE_URL/users/actuator/health"
run_test "Travel Service Health" "$BASE_URL/travels/actuator/health"
run_test "Payment Service Health" "$BASE_URL/payments/actuator/health"
run_test "Notification Service Health" "$BASE_URL/notifications/actuator/health"

# API Endpoint tests
echo ""
echo "=== API Endpoint Tests ==="
run_test "Auth Login Endpoint (OPTIONS)" "$BASE_URL/auth/login" 200
run_test "Users Endpoint (Unauthorized)" "$BASE_URL/users" 401
run_test "Travels Endpoint (Unauthorized)" "$BASE_URL/travels" 401
run_test "Payments Endpoint (Unauthorized)" "$BASE_URL/payments" 401

# Test with a valid token (if available in environment)
if [ -n "$TEST_JWT_TOKEN" ]; then
    echo ""
    echo "=== Authenticated Tests ==="
    
    auth_status=$(curl -s -o /dev/null -w "%{http_code}" \
        -H "Authorization: Bearer $TEST_JWT_TOKEN" \
        "$BASE_URL/users/me" --max-time 10)
    
    if [ "$auth_status" -eq "200" ]; then
        echo -e "Authenticated User Endpoint: ${GREEN}PASSED${NC}"
        ((TESTS_PASSED++))
    else
        echo -e "Authenticated User Endpoint: ${RED}FAILED${NC} (HTTP $auth_status)"
        ((TESTS_FAILED++))
    fi
fi

# Summary
echo ""
echo "=== Test Summary ==="
echo -e "Passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Failed: ${RED}$TESTS_FAILED${NC}"
echo "Total: $((TESTS_PASSED + TESTS_FAILED))"

# Exit with error if any tests failed
if [ $TESTS_FAILED -gt 0 ]; then
    echo ""
    echo -e "${RED}Some tests failed!${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}All smoke tests passed!${NC}"
exit 0
