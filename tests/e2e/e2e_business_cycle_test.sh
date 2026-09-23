#!/usr/bin/env bash
set -e

echo "======================================================="
echo " CALCUAPP END-TO-END BUSINESS CYCLE INTEGRATION TEST   "
echo "======================================================="

# 1. Execute Backend PHPUnit E2E Business Cycle Suite
echo "[Step 1/2] Running Backend E2E Business Cycle PHPUnit Suite..."
cd backend
php artisan test --filter=EndToEndBusinessCycleTest
cd ..

# 2. Execute Android Unit & Integration Test Suite
echo "[Step 2/2] Running Android Unit & Data Layer Test Suite..."
cd android
chmod +x gradlew
./gradlew testDebugUnitTest
cd ..

echo "======================================================="
echo " SUCCESS: FULL E2E BUSINESS CYCLE TEST PASSED CLEANLY!  "
echo "======================================================="
