# 미수정 / 미비 사항 정리

기준일: 2026-08-01

이 문서는 현재 레퍼런스 프로젝트에서 의도적으로 미룬 일, 아직 미완성인 일, 폐쇄망 반입 전 확인해야 할 일을 남긴다.

## 의도적으로 이번에 미룬 일

### 1. 메뉴 권한 seed 최소 권한화 — 해결 (2026-06-27)

MenuSqlTemplate가 screenMode 기준으로 CAN_* 기본값을 최소 권한으로 생성한다.

- ✅ `LIST`: `CAN_READ=Y`, 나머지 `N`
- ✅ `EXCEL`: `CAN_READ=Y`, `CAN_DOWNLOAD=Y`
- 과거 기록: 제거된 `DETAIL` screenMode는 `CAN_READ=Y`를 생성했다. 현재 지원 대상은 `LIST`/`EXCEL`/`CRUD` 3종이다.
- ✅ `CRUD`: `CAN_READ/CREATE/UPDATE/DELETE=Y`
- ✅ 개인정보 화면(`includePrivacy`): `CAN_MASK_VIEW=Y`
- 스캐폴드 화면에서 권한 체크박스를 직접 수정할 수 있게 할지 결정한다. (미해결)

### 2. 개인정보 마스킹 실제 적용

`includePrivacy=true` + 컬럼 `maskType` 옵션이 이제 ServiceTemplate에서 **실제 `MaskingUtil` 호출을 생성**한다(목록·엑셀). `@PrivacyLog` 자동 부착도 현행. (2026-06-27 해결)

남은 작업:

- ~~생성 Service의 목록 응답에서 `MaskingUtil`을 실제 적용한다.~~ ✅
- ~~엑셀 다운로드 데이터에도 동일 마스킹을 적용한다.~~ ✅
- ~~`CAN_MASK_VIEW` 권한이 있는 경우에만 원문 조회 또는 비마스킹 조회를 허용한다.~~ ✅ (MenuAuthInterceptor /unmask → MASK_VIEW)
- ~~원문 조회 API가 생기면 `@PrivacyLog`와 감사 로그를 반드시 남긴다.~~ ✅ (ControllerTemplate /unmask에 @PrivacyLog 부착)
- 프론트 JS 마스킹 formatter는 보조 표시용으로만 둘지, 서버 마스킹으로 완전히 통일할지 결정한다.

## 아직 미비한 부분

### 3. 기존 `SmsHistory` 생성물 재생성 — 운영 대상 제외

현재 메뉴에 배치된 `SmsHistory` 등 업무 화면은 Scaffold와 공통 자산을 검증하기 위한 테스트·참고 화면이다. 운영 화면 마이그레이션이나 하위 호환 대상이 아니다.

- 템플릿 변경은 `ScaffoldTemplateTest`의 의미 기반 assertion과 대표 Golden 결과로 검증한다. Golden은 LIST·EXCEL·CRUD·전체 옵션의 전체 산출물과 DB별 Mapper XML만 유지한다.
- 테스트 화면에 새 템플릿을 반영해야 하면 기존 수정본을 보존·병합하지 않고 폐기 후 다시 생성한다.
- 실제 업무 화면은 최초 생성 후 개발자 소유로 전환하며 템플릿 변경 때문에 재생성하지 않는다.

### 4. 메뉴 관리 화면 — 해결 (2026-07-27)

메뉴 관리 CRUD 화면이 구현되어 있다: `MenuManageController`(`/system/menu-manage`, tree/detail/data/create/update/delete) + Service/Mapper + 화면 + 테스트. `/system/menu-tree`는 `MenuSource` 구조 확인용 화면으로 유지한다.

남은 작업:

- 역할별 권한 편집 UI를 별도 화면으로 만들지 결정한다.
- local static 메뉴와 DB 메뉴를 비교할 수 있는 검증 기능을 둘지 결정한다.

### 5. `GlobalModelAdvice`의 API 요청 비용

`GlobalModelAdvice`는 layout 공통 모델을 채우기 위해 `menus`, `pageAuth`를 매 요청에 계산한다. `/sms/history/data` 같은 API 요청에도 메뉴 트리 조회가 붙을 수 있다.

현재 완화된 부분:

- `sms.auth.mode=local`만으로 화면 권한 전체 허용이 되지 않도록 수정했다.
- local profile에서만 `PageAuth.all()`을 내려준다.
- `menuTree`와 요청 URL별 `pageAuth`를 세션에서 재사용한다.
- 메뉴 관리 변경이 커밋되면 메뉴 revision을 증가시키고, 다음 화면 요청에서 기존 세션의 두 캐시를 함께 갱신한다.
- 실패·롤백된 변경은 revision을 증가시키지 않는다.

남은 작업:

- JSON/API 요청에서는 layout용 `menus` 생성을 생략할지 검토한다.
- `HandlerMethod` 또는 `Accept` 헤더 기준으로 화면 요청과 API 요청을 나눌지 결정한다.
- 다중 서버 운영 시 애플리케이션 로컬 메뉴 revision을 DB/Redis/이벤트 기반 공유 revision으로 바꿀지 결정한다.
- `TB_EMP_ROLE` 변경은 현재 principal 역할 목록을 바꾸지 않으므로 재로그인 정책을 운영 절차에 반영한다.
- DB 메뉴 테이블 장애가 API 조회 실패로 번지는지 운영 기준으로 점검한다.

### 6. CRUD 즉시 실행 가능 범위의 한계

스캐폴드는 이제 `targetTable` 기준으로 `INSERT/UPDATE/DELETE`를 생성한다. 다만 DB 제약과 업무 규칙을 완전히 알 수는 없다.

남은 작업:

- PK가 시퀀스/트리거/IDENTITY인지 입력받아 INSERT에 반영할지 결정한다.
- 필수 NOT NULL 컬럼의 기본값/입력값을 DB 메타데이터로 표시할지 검토한다.
- 코드성 컬럼은 create/update 모달에서 text가 아니라 select/radio로 렌더링하는 기능을 추가한다.
- 날짜/시간 컬럼은 등록/수정 모달에서도 Toast UI DatePicker로 렌더링한다.
- ~~서버 DTO에 `@NotNull`, `@Size`, `@Pattern` 같은 검증 어노테이션을 DB 메타 기반으로 생성할지 검토한다.~~ ✅ (`validate=required` → `@NotBlank`/`@NotNull` 구현. `@Size`/`@Pattern`은 후속)

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

### 9. 승인 시스템 구현 미착수

설계는 `docs/승인.md`에 있다 (메뉴 권한과 분리된 승인 요청/단계/이력/규칙 테이블과 Oracle DDL 초안). Service/화면 구현은 미착수다.

### 10. 스캐폴드 컬럼 헤더 설정 — 해결

옵션 갱신 시 대상 테이블의 JDBC 컬럼 comment를 `headerName` 기본값으로 채운다. 직접 컬럼에 alias가 있어도 원본 컬럼 comment를 연결하며 계산 컬럼은 alias/컬럼명으로 fallback한다. 사용자는 생성 전에 화면명을 수정할 수 있고, 같은 값이 그리드와 CRUD 등록·수정 모달에 반영된다.

실제 PK는 그리드와 등록·수정 모달에서 기본 숨김이지만 hidden 식별값으로 유지된다. 다른 컬럼도 `visible`, `modalVisible`, `editable`을 선택해 표시와 등록·수정 입력 허용 범위를 줄일 수 있다.

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
