# 미수정 / 미비 사항 정리

기준일: 2026-06-19

이 문서는 현재 레퍼런스 프로젝트에서 의도적으로 미룬 일, 아직 미완성인 일, 폐쇄망 반입 전 확인해야 할 일을 남긴다.

## 의도적으로 이번에 미룬 일

### 1. 메뉴 권한 seed 최소 권한화

현재 스캐폴드의 `메뉴등록.sql`은 `CAN_READ`, `CAN_CREATE`, `CAN_UPDATE`, `CAN_DELETE`, `CAN_APPROVE`, `CAN_CANCEL`, `CAN_DOWNLOAD`, `CAN_MASK_VIEW`를 모두 `Y`로 생성한다.

사용자 결정: 메뉴 권한 정책은 우선 전체 권한으로 생성한 뒤 나중에 수정한다.

남은 작업:

- 화면 모드별 기본 권한을 나눈다.
- `LIST`: `CAN_READ=Y`, 나머지 기본 `N`
- `EXCEL`: `CAN_READ=Y`, `CAN_DOWNLOAD=Y`
- `DETAIL`: `CAN_READ=Y`
- `CRUD`: `CAN_READ/CREATE/UPDATE/DELETE=Y`
- 개인정보 원문 조회가 필요한 화면만 `CAN_MASK_VIEW=Y`
- 스캐폴드 화면에서 권한 체크박스를 직접 수정할 수 있게 할지 결정한다.

### 2. 개인정보 마스킹 실제 적용

현재 `includePrivacy=true`는 `@PrivacyLog` 부착과 일부 JS formatter/마스킹 적용 지점 생성까지 담당한다. 하지만 정책상 최종 기준은 서버에서 마스킹된 값을 내려주는 것이다.

사용자 결정: 마스킹은 메뉴 권한 정리 후 진행한다.

남은 작업:

- 생성 Service의 목록 응답에서 `MaskingUtil`을 실제 적용한다.
- 엑셀 다운로드 데이터에도 동일 마스킹을 적용한다.
- `CAN_MASK_VIEW` 권한이 있는 경우에만 원문 조회 또는 비마스킹 조회를 허용한다.
- 원문 조회 API가 생기면 `@PrivacyLog`와 감사 로그를 반드시 남긴다.
- 프론트 JS 마스킹 formatter는 보조 표시용으로만 둘지, 서버 마스킹으로 완전히 통일할지 결정한다.

## 아직 미비한 부분

### 3. 기존 `SmsHistory` 생성물 재생성 필요

이번 수정은 스캐폴드 템플릿과 공통 기능을 고친 것이다. 이미 생성되어 있는 `SmsHistory` 화면 파일을 다시 덮어쓰지는 않았다.

남은 작업:

- `/system/scaffold`에서 발송이력조회를 다시 생성한다.
- `targetTable=SMS.SMS_HISTORY`, `pkColumn=SMS_HISTORY_ID`, 적절한 `lockColumn`을 확인한다.
- 생성 결과 미리보기 후 적용한다.
- 적용 후 `SmsHistoryMapper.xml`의 기존 CRUD TODO/bad SQL이 사라졌는지 확인한다.

주의:

- 현재 기존 `src/main/resources/mapper/sms/SmsHistoryMapper.xml`의 CRUD SQL은 이전 생성물 기준일 수 있다.
- local에서 등록/수정/삭제 버튼을 보이게 하면 기존 파일의 SQL 오류가 바로 드러날 수 있다.

### 4. 메뉴 트리 관리 화면은 아직 없음

현재 local에는 `/system/menu-tree` 확인 화면만 있다. 이는 `MenuSource`가 반환하는 메뉴 구조를 보여주는 확인용 화면이다.

남은 작업:

- `TB_MENU`, `TB_MENU_AUTH`를 관리하는 실제 메뉴/권한 CRUD 화면을 만든다.
- 역할별 권한 편집 UI를 만든다.
- 메뉴 정렬, 사용 여부, 표시 여부, 시스템 메뉴 여부를 수정할 수 있게 한다.
- local static 메뉴와 DB 메뉴를 비교할 수 있는 검증 기능을 둘지 결정한다.

### 5. `GlobalModelAdvice`의 API 요청 비용

`GlobalModelAdvice`는 layout 공통 모델을 채우기 위해 `menus`, `pageAuth`를 매 요청에 계산한다. `/sms/history/data` 같은 API 요청에도 메뉴 트리 조회가 붙을 수 있다.

현재 완화된 부분:

- `sms.auth.mode=local`만으로 화면 권한 전체 허용이 되지 않도록 수정했다.
- local profile에서만 `PageAuth.all()`을 내려준다.

남은 작업:

- JSON/API 요청에서는 layout용 `menus` 생성을 생략할지 검토한다.
- `HandlerMethod` 또는 `Accept` 헤더 기준으로 화면 요청과 API 요청을 나눌지 결정한다.
- 메뉴 트리는 세션/캐시로 줄일 수 있는지 확인한다.
- DB 메뉴 테이블 장애가 API 조회 실패로 번지는지 운영 기준으로 점검한다.

### 6. CRUD 즉시 실행 가능 범위의 한계

스캐폴드는 이제 `targetTable` 기준으로 `INSERT/UPDATE/DELETE`를 생성한다. 다만 DB 제약과 업무 규칙을 완전히 알 수는 없다.

남은 작업:

- PK가 시퀀스/트리거/IDENTITY인지 입력받아 INSERT에 반영할지 결정한다.
- 필수 NOT NULL 컬럼의 기본값/입력값을 DB 메타데이터로 표시할지 검토한다.
- 코드성 컬럼은 create/update 모달에서 text가 아니라 select/radio로 렌더링하는 기능을 추가한다.
- 날짜/시간 컬럼은 등록/수정 모달에서도 Toast UI DatePicker로 렌더링한다.
- 서버 DTO에 `@NotNull`, `@Size`, `@Pattern` 같은 검증 어노테이션을 DB 메타 기반으로 생성할지 검토한다.

### 7. 스캐폴드 SQL 분석 한계

옵션 갱신은 서버 `QueryColumnExtractor` 기준으로 통일했다. 브라우저의 콤마 split 파서는 제거했다.

남은 작업:

- `UNION`, `WITH`, 복잡한 `CASE`, vendor-specific Oracle 함수에서 컬럼/테이블 추출이 항상 기대대로 되는지 샘플을 늘린다.
- 분석 실패 시 UI에 실패 사유와 수동 입력 가이드를 표시한다.
- `targetTable` 자동 추론이 서브쿼리의 내부 FROM을 잡지 않는지 추가 테스트한다.

### 8. 폐쇄망 반입 패키지 갱신

기존 `_handoff` 압축 파일은 이전 작업 시점 산출물이다. 이번 수정분은 아직 새 zip/base64로 다시 묶지 않았다.

남은 작업:

- 최신 수정 파일 목록 기준으로 폐쇄망 반입 zip을 다시 만든다.
- 첨부 규약 제한이 있으면 zip을 base64로 다시 변환한다.
- 반입 목록에서 삭제 파일 `db/oracle/sms_hitory_menu_seed.sql`도 삭제 대상으로 명시한다.

## 이번에 정리된 것

- `tui-page-builder.js`는 `PAGE_AUTH`가 없으면 권한 없음으로 판단한다.
- `GlobalModelAdvice`는 `sms.auth.mode=local`만 보고 전체 화면 권한을 주지 않는다.
- local profile이 아닌데 `sms.auth.mode=local`이면 `AuthSourceGuard`가 부팅을 막는다.
- 스캐폴드 옵션 갱신은 서버 `QueryColumnExtractor` 기준으로 통일했다.
- 오타 seed 파일 `db/oracle/sms_hitory_menu_seed.sql`은 삭제했다.

## 최근 검증

```text
mvn -Dtest=ScaffoldTemplateTest,QueryColumnExtractorTest,GlobalModelAdviceTest,AuthSourceGuardTest test  PASS
mvn test                                                                                                  PASS
```
