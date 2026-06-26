-- ============================================================
-- SMS 발송 이력 샘플 데이터
-- DBeaver에서 그대로 실행 가능한 단순 INSERT 스크립트.
-- 로컬 seed 기준 사용자: admin / D001
-- ============================================================

INSERT INTO SMS.SMS_HISTORY (
    REQUEST_ID, SENT_AT, RECEIVER_NO, SENDER_NO, SEND_TYPE, SEND_STATUS,
    RESULT_CD, RESULT_MSG, MESSAGE, EMP_ID, DEP_ID, REG_ID
) VALUES (
    'REQ202606180001',
    TIMESTAMP '2026-06-18 09:00:00',
    '01012345678',
    '0215880000',
    'SMS',
    'SUCCESS',
    '0000',
    '정상 발송',
    TO_CLOB('[SMS] 인증번호는 482913입니다.'),
    'admin',
    'D001',
    'SYSTEM'
);

INSERT INTO SMS.SMS_HISTORY (
    REQUEST_ID, SENT_AT, RECEIVER_NO, SENDER_NO, SEND_TYPE, SEND_STATUS,
    RESULT_CD, RESULT_MSG, MESSAGE, EMP_ID, DEP_ID, REG_ID
) VALUES (
    'REQ202606180002',
    TIMESTAMP '2026-06-18 09:05:00',
    '01023456789',
    '0215880000',
    'LMS',
    'SUCCESS',
    '0000',
    '정상 발송',
    TO_CLOB('[LMS] 고객님의 상담 예약이 6월 19일 14시에 확정되었습니다.'),
    'admin',
    'D001',
    'SYSTEM'
);

INSERT INTO SMS.SMS_HISTORY (
    REQUEST_ID, SENT_AT, RECEIVER_NO, SENDER_NO, SEND_TYPE, SEND_STATUS,
    RESULT_CD, RESULT_MSG, MESSAGE, EMP_ID, DEP_ID, REG_ID
) VALUES (
    'REQ202606180003',
    TIMESTAMP '2026-06-18 09:10:00',
    '01034567890',
    '0215880000',
    'SMS',
    'FAIL',
    'E101',
    '수신 번호 형식 오류',
    TO_CLOB('[SMS] 결제 승인 안내 메시지입니다.'),
    'admin',
    'D001',
    'SYSTEM'
);

INSERT INTO SMS.SMS_HISTORY (
    REQUEST_ID, SENT_AT, RECEIVER_NO, SENDER_NO, SEND_TYPE, SEND_STATUS,
    RESULT_CD, RESULT_MSG, MESSAGE, EMP_ID, DEP_ID, REG_ID
) VALUES (
    'REQ202606180004',
    TIMESTAMP '2026-06-18 10:20:00',
    '01045678901',
    '0215880000',
    'ALIMTALK',
    'SUCCESS',
    '0000',
    '정상 발송',
    TO_CLOB('[알림톡] 주문하신 상품이 출고되었습니다.'),
    'admin',
    'D001',
    'SYSTEM'
);

INSERT INTO SMS.SMS_HISTORY (
    REQUEST_ID, SENT_AT, RECEIVER_NO, SENDER_NO, SEND_TYPE, SEND_STATUS,
    RESULT_CD, RESULT_MSG, MESSAGE, EMP_ID, DEP_ID, REG_ID
) VALUES (
    'REQ202606180005',
    TIMESTAMP '2026-06-18 11:35:00',
    '01056789012',
    '0215880000',
    'MMS',
    'SENT',
    '0001',
    '통신사 결과 대기',
    TO_CLOB('[MMS] 이벤트 안내 이미지가 포함된 메시지입니다.'),
    'admin',
    'D001',
    'SYSTEM'
);

INSERT INTO SMS.SMS_HISTORY (
    REQUEST_ID, SENT_AT, RECEIVER_NO, SENDER_NO, SEND_TYPE, SEND_STATUS,
    RESULT_CD, RESULT_MSG, MESSAGE, EMP_ID, DEP_ID, REG_ID
) VALUES (
    'REQ202606180006',
    TIMESTAMP '2026-06-18 13:00:00',
    '01067890123',
    '0215880000',
    'SMS',
    'CANCEL',
    'C001',
    '사용자 발송 취소',
    TO_CLOB('[SMS] 예약 발송 취소 테스트 메시지입니다.'),
    'admin',
    'D001',
    'SYSTEM'
);

COMMIT;
