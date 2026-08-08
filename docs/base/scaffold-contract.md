# Scaffold Contract v2 — 최초 생성 후 개발자 소유

이 문서는 Query Scaffold가 생성하는 코드의 품질 기준과 생성물 소유권을 정의한다.

> **핵심 원칙**: `.tpl`은 새 화면을 처음 만들기 위한 원본이다. 한 번 적용된 생성물은 개발자가 직접 수정하고 소유한다. 이후 템플릿 변경을 기존 화면에 자동 반영하지 않는다.

## 0. 원본과 생성물의 경계

| 대상 | 역할과 권위 |
|---|---|
| `scaffold-templates/**/*.tpl` | 앞으로 새로 생성할 표준 화면의 원본 |
| `MapperXmlViewFactory.java` 등 생성 로직 | 템플릿에 전달할 SQL·메타데이터 계산 원본 |
| `scaffold-cases/*.json` | 당시 생성 입력을 보관하는 재현용 기록. 기존 화면을 템플릿과 계속 동기화하는 선언이 아님 |
| 적용된 Java/XML/HTML/JS/SQL | 적용 직후부터 개발자 소유 소스. 업무 요구에 맞게 직접 수정 |

템플릿은 생성된 업무 화면의 상위 원본이 아니다. 개발자가 수정한 화면과 템플릿이 달라도 정상이며, 템플릿 변경 때문에 기존 화면을 고칠 필요는 없다.

## 1. screenMode

Scaffold는 세 가지 `screenMode`별 `.tpl`로 기본 화면을 생성한다.

| screenMode | 기본 형태 | 템플릿 디렉터리 |
|---|---|---|
| `LIST` | 검색 + 그리드 | `scaffold-templates/list/` |
| `EXCEL` | LIST + 엑셀 다운로드 | `scaffold-templates/excel/` |
| `CRUD` | LIST + 등록 버튼 + 행 클릭 편집 모달 | `scaffold-templates/crud/` |

CRUD 모달은 `fragments/modal-base.html`, `modal-manager.js`, `admin-form-detail.css`를 사용한다. 알 수 없는 `screenMode`는 거부한다.

## 2. 생성 범위

| 산출물 | 비고 |
|---|---|
| `{Domain}SearchRequestDTO.java` | `PageRequestDTO` 상속 |
| `{Domain}UpdateRequestDTO.java` | CRUD 전용 화이트리스트 DTO |
| `{Domain}VO.java` | 조회 결과 모델 |
| `{Domain}Mapper.java`, `{Domain}Mapper.xml` | 목록·COUNT 및 선택 기능별 CUD SQL |
| `{Domain}Service.java`, `{Domain}Controller.java` | 기본 서비스와 endpoint |
| `{Domain}ServiceTest.java`, `{Domain}ControllerTest.java` | 기본 계약 테스트 |
| `{domainId}.html`, `{domainId}.js` | `screenMode`별 기본 화면과 동작 |
| `메뉴등록.sql` | 메뉴 및 최소 기능 권한 seed |

Scaffold가 제공하지 않는 업무 규칙, 복잡한 집계, 외부 연계, 특수 모달·팝업, 화면 고유 상호작용은 기본 골격 생성 후 개발자가 구현한다.

## 3. 표준 개발 흐름

```text
1. 도메인·테이블·조회 SQL·screenMode를 확정한다.
2. /system/scaffold에서 분석 → 옵션 검토 → 생성 → 적용 미리보기를 수행한다.
3. 신규 파일인지 확인한 뒤 한 번 적용한다.
4. 생성된 파일의 소유권을 개발자에게 넘긴다.
5. 업무 로직, 화면 고유 UI, 추가 API와 테스트를 직접 구현한다.
6. mvn test와 mvn -DskipTests package를 통과시킨다.
```

생성기는 완성된 업무 화면을 보장하지 않는다. 반복 코드와 기본 레이아웃을 안전하게 시작하는 도구다.

## 4. 생성 품질 기준

| 항목 | 기본 보장 |
|---|---|
| 응답 | `ApiResponse`, `PageResponseDTO` 공통 계약 |
| 검증 | 옵션에 따른 서버 검증 어노테이션과 클라이언트 기본 검증 |
| 개인정보 | 선택한 컬럼의 마스킹 및 원문 조회 기본 연결 |
| 권한 | `screenMode`별 최소 `CAN_*` 권한 seed |
| 화면 | `defaultLayout`, `TuiPageBuilder`, `ApiClient`, `FormBinder`, `ModalManager` 등 공통 자산 사용 |
| SQL | 쿼리 시그니처, 공통 검색조건, DB 방언별 페이징 기본 구조 |
| 적용 | 신규/변경없음/덮어쓰기 미리보기와 명시적 적용 |

업무 의미의 정확성은 개발자가 검토한다. 특히 복잡한 SQL, 수정 허용 컬럼, 낙관적 잠금, 개인정보와 권한은 생성 직후 반드시 확인한다.

### SQL 검색조건과 COUNT 판정

- 직접 컬럼만 조회하는 단순 SELECT는 원본 테이블 별칭을 유지하고 `searchConditions`를 `baseQuery` 내부에 적용한다.
- 일반 테이블 JOIN도 집계·계산·서브쿼리가 없으면 검색조건을 내부에 적용할 수 있지만, JOIN 중복 가능성 때문에 COUNT는 목록 쿼리 래퍼를 유지한다.
- 단일 테이블 단순 SELECT만 `SELECT COUNT(1) + FROM + searchConditions` 직접 COUNT를 생성한다.
- `DISTINCT`, 계산 컬럼, 집계, `GROUP BY/HAVING`, `UNION`, CTE, 서브쿼리, 윈도우 함수, raw SQL 내부 정렬·페이징은 검색조건 외부 배치와 래퍼 COUNT를 유지한다.
- 구조 분석이 실패하거나 확신할 수 없으면 최적화하지 않는다. 생성 직후 목록 범위와 COUNT 범위가 같은지 개발자가 확인한다.

## 5. 생성 파일 소유권

모든 생성 파일의 `Scaffold 생성` 헤더는 생성 출처를 표시할 뿐, 템플릿과 계속 동기화된다는 뜻이 아니다.

- 개발자는 생성된 Service, Controller, Mapper, DTO/VO, HTML, JS와 테스트를 직접 수정한다.
- `ConventionTest`는 공통 금지 규칙과 구조를 검사한다. 생성 파일이 현재 `.tpl`과 동일한지는 검사하지 않는다.
- 개발자 수정 내용을 템플릿이나 `scaffold-cases`에 역으로 반영할 의무가 없다.
- 템플릿이 개선돼도 이미 개발 중이거나 완료된 화면은 영향받지 않는다.

## 6. 재생성은 갱신이 아니라 폐기 후 신규 생성

재생성에는 병합, 보호 영역, 사용자 수정 보존 기능이 없다. 같은 경로에 적용하면 기존 파일 전체를 덮어쓴다.

따라서 다음 규칙을 지킨다.

1. 개발자가 수정한 화면에는 일반적인 업데이트 수단으로 재생성을 사용하지 않는다.
2. 다시 생성하려면 기존 수정본을 폐기한다는 결정을 먼저 내린다.
3. 버전 관리로 기존 소스를 보존하고, 미리보기의 `OVERWRITE` 파일을 전부 확인한다.
4. 가능하면 새 `domainId` 또는 별도 작업 위치에 생성해 비교한 뒤 필요한 내용만 개발자가 옮긴다.
5. `ScaffoldRegenerateMain` 쓰기 모드는 모든 활성 case의 현재 수정본을 폐기하고 새 생성물로 교체하기로 결정했을 때 사용한다. 테스트·데모 화면뿐 아니라 개발자 수정 화면도 의도적으로 버리기로 했다면 대상이 될 수 있다.

`--dry-run`도 병합 가능성을 판단하지 않는다. 현재 case로 다시 만들 때 어떤 파일이 달라지는지만 보여준다.

```bash
# 읽기 전용 변경 감지. 파일은 쓰지 않는다.
mvn compile exec:java \
  -Dexec.mainClass=com.scbk.sms.service.system.scaffold.ScaffoldRegenerateMain \
  -Dexec.args=--dry-run
```

인자 없는 `ScaffoldRegenerateMain`은 모든 활성 case를 덮어쓴다. 기존 화면을 부분 갱신하거나 수정 내용을 보존하는 용도로는 사용하지 않고, 전체 대상의 폐기·신규 생성을 명시적으로 결정한 경우에 실행한다.

## 7. 관련 문서

- 실제 생성 순서: `docs/base/screen-generation-guide.md`
- 생성기 사용법과 옵션: `docs/menual/스케폴드_manual.md`
- 화면 레이아웃·공통 자산 규약: `docs/base/screen-convention.md`
- 스캐폴드가 맞지 않는 도메인 판단: `docs/base/domain-boundary-guide.md`
