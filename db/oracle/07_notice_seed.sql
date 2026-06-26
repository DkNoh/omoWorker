-- ============================================================
-- 공지사항 샘플 데이터 (scaffold 테스트용)
-- DBeaver에서 그대로 실행 가능한 단순 INSERT 스크립트.
-- 로컬 seed 기준 사용자: admin
-- ============================================================

INSERT INTO SMS.NOTICE (
    TITLE, CONTENT, NOTICE_TYPE, USE_YN, START_DT, END_DT, VIEW_CNT, REG_ID
) VALUES (
    '시스템 정기 점검 안내',
    TO_CLOB('6월 25일 00:00 ~ 02:00 시스템 정기 점검이 진행됩니다. 점검 시간 동안 서비스 이용이 제한됩니다.'),
    'GENERAL',
    'Y',
    DATE '2026-06-20',
    DATE '2026-06-30',
    128,
    'admin'
);

INSERT INTO SMS.NOTICE (
    TITLE, CONTENT, NOTICE_TYPE, USE_YN, START_DT, END_DT, VIEW_CNT, REG_ID
) VALUES (
    '[긴급] 발신 번호 일시 차단 안내',
    TO_CLOB('통신사 회선 점검으로 일부 발신 번호가 일시적으로 차단되었습니다. 복구 즉시 별도 공지하겠습니다.'),
    'URGENT',
    'Y',
    DATE '2026-06-21',
    DATE '2026-06-22',
    342,
    'admin'
);

INSERT INTO SMS.NOTICE (
    TITLE, CONTENT, NOTICE_TYPE, USE_YN, START_DT, END_DT, VIEW_CNT, REG_ID
) VALUES (
    '여름맞이 사내 이벤트 안내',
    TO_CLOB('7월 한 달간 사내 이벤트가 진행됩니다. 자세한 내용은 첨부 파일을 참고해 주세요.'),
    'EVENT',
    'Y',
    DATE '2026-07-01',
    DATE '2026-07-31',
    57,
    'admin'
);

INSERT INTO SMS.NOTICE (
    TITLE, CONTENT, NOTICE_TYPE, USE_YN, START_DT, END_DT, VIEW_CNT, REG_ID
) VALUES (
    '구 버전 메뉴 종료 안내',
    TO_CLOB('v2 메뉴 화면은 2026년 7월 1일부로 종료됩니다. v3로 전환해 주세요.'),
    'GENERAL',
    'N',
    DATE '2026-05-01',
    DATE '2026-05-31',
    980,
    'admin'
);

COMMIT;
