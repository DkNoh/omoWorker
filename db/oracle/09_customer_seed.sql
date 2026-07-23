-- ============================================================
-- SMS 고객 마스터 샘플 데이터 (UTF-8 실행)
-- SMS.SMS_HISTORY 의 샘플 RECEIVER_NO 와 모바일 번호를 맞춰,
-- 고객별 발송 집계가 0건 이상 나오도록 구성.
-- ============================================================

INSERT INTO SMS.CUSTOMER (
    CUSTOMER_NM, MOBILE_NO, EMAIL, BIRTH_DT, GENDER_CD,
    AGREE_YN, AGREE_DT, MEMO, USE_YN, REG_ID
) VALUES (
    '김민준', '01012345678', 'minjun.kim@example.com', '19850312', 'M',
    'Y', TIMESTAMP '2026-04-01 10:00:00', 'VIP 고객', 'Y', 'SYSTEM'
);

INSERT INTO SMS.CUSTOMER (
    CUSTOMER_NM, MOBILE_NO, EMAIL, BIRTH_DT, GENDER_CD,
    AGREE_YN, AGREE_DT, MEMO, USE_YN, REG_ID
) VALUES (
    '이서연', '01023456789', 'seoyeon.lee@example.com', '19900825', 'F',
    'Y', TIMESTAMP '2026-05-12 14:30:00', NULL, 'Y', 'SYSTEM'
);

INSERT INTO SMS.CUSTOMER (
    CUSTOMER_NM, MOBILE_NO, EMAIL, BIRTH_DT, GENDER_CD,
    AGREE_YN, AGREE_DT, MEMO, USE_YN, REG_ID
) VALUES (
    '박지훈', '01034567890', 'jihoon.park@example.com', '19920710', 'M',
    'N', NULL, '수신거부 고객', 'Y', 'SYSTEM'
);

INSERT INTO SMS.CUSTOMER (
    CUSTOMER_NM, MOBILE_NO, EMAIL, BIRTH_DT, GENDER_CD,
    AGREE_YN, AGREE_DT, MEMO, USE_YN, REG_ID
) VALUES (
    '최유나', '01045678901', 'yuna.choi@example.com', '19951130', 'F',
    'Y', TIMESTAMP '2026-06-01 09:15:00', '알림톡 선호', 'Y', 'SYSTEM'
);

INSERT INTO SMS.CUSTOMER (
    CUSTOMER_NM, MOBILE_NO, EMAIL, BIRTH_DT, GENDER_CD,
    AGREE_YN, AGREE_DT, MEMO, USE_YN, REG_ID
) VALUES (
    '정도현', '01056789012', 'dohyun.jung@example.com', '19881201', 'M',
    'Y', TIMESTAMP '2026-03-20 11:00:00', NULL, 'Y', 'SYSTEM'
);

INSERT INTO SMS.CUSTOMER (
    CUSTOMER_NM, MOBILE_NO, EMAIL, BIRTH_DT, GENDER_CD,
    AGREE_YN, AGREE_DT, MEMO, USE_YN, REG_ID
) VALUES (
    '강서아', '01099998888', 'seoah.kang@example.com', '20000101', 'F',
    'N', NULL, '미발송 고객(테스트용)', 'Y', 'SYSTEM'
);

INSERT INTO SMS.CUSTOMER (
    CUSTOMER_NM, MOBILE_NO, EMAIL, BIRTH_DT, GENDER_CD,
    AGREE_YN, AGREE_DT, MEMO, USE_YN, REG_ID
) VALUES (
    '윤재혁', '01011112222', 'jaehyuk.yoon@example.com', '19790615', 'M',
    'Y', TIMESTAMP '2026-02-10 16:45:00', '휴면 전환 예정', 'N', 'SYSTEM'
);

COMMIT;
