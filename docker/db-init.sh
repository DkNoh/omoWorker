#!/usr/bin/env bash
# 로컬 docker Oracle 스키마 초기화. 컨테이너가 healthy 된 뒤 1회 실행한다.
#
#   export ORACLE_ADMIN_PASSWORD=oracle
#   export SMS_DB_PASSWORD=1234
#   docker compose up -d --wait
#   ./docker/db-init.sh
#
# 하는 일:
#   1) SMS 스키마 유저 생성
#   2) 로컬 전용 EMP/DEP 테스트 픽스처 (로그인용, 운영 테이블 대용)
#   3) db/oracle 의 DDL/seed 적재 (SMS.TB_*)
set -euo pipefail

ADMIN_PW="${ORACLE_ADMIN_PASSWORD:-oracle}"
SMS_PW="${SMS_DB_PASSWORD:-1234}"
SVC="localhost:1521/SMS"
CT="sms-oracle"
# 한글 seed(menu) 깨짐 방지
EXEC=(docker exec -e NLS_LANG=.AL32UTF8 -i "$CT")

echo "[1/4] SMS 스키마 유저 생성"
"${EXEC[@]}" sqlplus -S "system/${ADMIN_PW}@${SVC}" >/dev/null <<SQL
WHENEVER SQLERROR CONTINUE
CREATE USER SMS IDENTIFIED BY "${SMS_PW}" DEFAULT TABLESPACE USERS QUOTA UNLIMITED ON USERS;
ALTER USER SMS IDENTIFIED BY "${SMS_PW}" ACCOUNT UNLOCK;
GRANT CONNECT, RESOURCE TO SMS;
EXIT
SQL

echo "[2/4] 로컬 EMP/DEP 테스트 픽스처"
"${EXEC[@]}" sqlplus -S "SMS/${SMS_PW}@${SVC}" < docker/local-emp-dep-seed.sql | grep -iE "ORA-|SP2-" | head || true

echo "[3/4] db/oracle DDL/seed 적재 (SMS 유저)"
for f in db/oracle/01_*.sql db/oracle/02_*.sql db/oracle/03_*.sql; do
  echo "   >> $(basename "$f")"
  "${EXEC[@]}" sqlplus -S "SMS/${SMS_PW}@${SVC}" < "$f" | grep -iE "ORA-|SP2-" | head -5 || true
done

echo "[4/4] 검증"
"${EXEC[@]}" sqlplus -S "SMS/${SMS_PW}@${SVC}" <<'SQL'
SET PAGESIZE 100
SELECT table_name FROM user_tables ORDER BY table_name;
PROMPT --- 로그인 테스트 사번 (admin) ---
SELECT E.EMP_ID, E.DEP_ID, E.EMP_NM, D.DEP_NM
FROM SMS.EMP E JOIN SMS.DEP D ON D.DEP_ID = E.DEP_ID
WHERE E.ACT_YN = 'Y' AND D.ACT_YN = 'Y';
EXIT
SQL
