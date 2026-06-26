# 분석 문서 세트 INDEX

프로젝트: `/Users/dk/Work/smsTest-v3`  
작업 성격: 읽기 전용 심층 분석, 소스 파일 변경 없음  
분석 일시: 2026-05-18

## 문서 목록

- [x] `00-INDEX.md` — 전체 문서 목록, 진행 체크리스트, 분석 일시
|- [x] `01-architecture.md` — 전체 아키텍처
- [x] `02-auth-and-security.md` — 인증/인가
- [x] `03-sms-history-feature.md` — 대표 기능 end-to-end
- [x] `04-scaffold-engine.md` — Query Scaffold 생성기
- [x] `05-data-model.md` — DB/도메인 모델
- [x] `06-common-infrastructure.md` — 공통 인프라
- [x] `07-mybatis-sql-inventory.md` — 전체 SQL 카탈로그
- [x] `08-conventions-and-debt.md` — 컨벤션 & 기술부채

## 진행 체크리스트

- [x] 구조 스캔 완료
- [x] `01-architecture.md` 작성 완료
- [x] `02-auth-and-security.md` 작성 완료
- [x] `03-sms-history-feature.md` 작성 완료
- [x] `04-scaffold-engine.md` 작성 완료
- [x] `05-data-model.md` 작성 완료
- [x] `06-common-infrastructure.md` 작성 완료
- [x] `07-mybatis-sql-inventory.md` 작성 완료
- [x] `08-conventions-and-debt.md` 작성 완료
- [x] 생성물 최종 검증 완료

## 메모

- `target/`, `build/`, `node_modules/`, `static/lib/`, `static/vendor/` 등은 진입하지 않는다.
- 각 문서는 실제 코드에서 확인한 내용만 기록한다.
- 클래스/메서드/SQL 언급 시 파일 경로와 줄 위치를 함께 기록한다.
