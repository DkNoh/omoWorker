# Scaffold Contract v1

이 문서는 Query Scaffold가 **생성하는 파일의 소유권·재생성·확장 계약**을 정의한다. 목적: 스캐폴드가 마이그레이션 엔진(N 시스템 복제 기반)이므로, **재생성이 항상 안전**해야 하고 **확장은 정해진 슬롯**으로만 이뤄져야 한다. 그래야 레퍼런스가 개선될 때 마이그레이션된 시스템도 그 개선을 받을 수 있다.

> 관련: `query-scaffold-implementation.md`(구현), `v2-scaffold-reference.md`(v2 분석), `.claude/rules/scaffold-query.md`(실행 규칙)

## 0. 구현 상태 (반드시 먼저 읽을 것)

이 문서는 **목표 계약(target v1)** + **현행 구현**을 섞어 둔다. 각 조항의 상태를 반드시 확인하라. (현행 = 코드에 구현됨 / 목표 = 아직 미구현, 계약만 정의)

| 조항 | 상태 | 비고 |
|---|---|---|
| §1 생성 산출물 11~12종 | **현행** | `ScaffoldService.generateFiles` 검증됨 |
| §1 `{Domain}Rules`(사용자 소유) 파일 | **현행** | RulesTemplate가 CRUD 모드에서 `{Domain}Rules.java` 생성. 마커 없음 → 재생성 시 USER_OWNED 보존 |
| §2 소유/버전 마커 | **현행(완료)** | 전 12개 템플릿이 `Scaffold 생성(v1) — scaffold 소유` 마커 부착(Service/Controller/MapperXml/Dto/Vo/MapperInterface/UpdateRequestDto/Html/Js/MenuSql/ServiceTest/ControllerTest) |
| §3 재생성 보증(사용자 영역 보존) | **현행(일부)** | `ScaffoldFileApplier`가 `SCAFFOLD_OWNED_MARKER`가 없는 기존 파일을 사용자 소유로 간주해 재생성 시 보존(skip, USER_OWNED). 단 `{Domain}Rules` 파일의 최초 1회 생성 후 스킵 등 더 세분된 보존은 A-2와 함께 |
| §4-A 선언 옵션(screenMode/includePrivacy/컬럼 옵션) | **현행** | `includePrivacy`+컬럼 `maskType`이 실제 `MaskingUtil.maskPhone/maskName/maskRrn/maskCard` 호출 생성(목록·엑셀). `@PrivacyLog` 자동 부착도 현행. 원문 비마스킹 조회(`CAN_MASK_VIEW` + `/unmask`)는 A-2 후속 |
| §4-A `@PrivacyLog` 자동 부착 | **현행** | ControllerTemplate가 /data·/excel에 부착(검증됨) |
| §4-B Service→`rules.validateOnCreate()` 호출 | **현행** | RulesTemplate가 `{Domain}Rules` @Component 생성(validateOnCreate/Update stub). ServiceTemplate가 create/update에서 hook 호출. Rules는 마커 없음 → 재생성 보존 |
| §4-C 탈출(unplugged) 마커 | **목표(미구현)** | 마커 메커니즘 자체 없음 |
| §6 drift gate | **목표(미구현)** | A-3 작업 대기 |

> **현재 보존 범위**: §3가 부분 구현됨 — 마커(`Scaffold 생성`)가 없는 기존 파일은 재생성 시 보존된다. 단 마커가 있는(scaffold 소유) 파일은 여전히 덮어쓰므로, 마커를 지우지 않은 채 파일을 손편집하면 날아갈 수 있다. 사용자 영역으로 전환하려면 마커 라인을 제거해 `USER_OWNED` 상태로 만든다.

---

## 1. 생성 범위와 소유권

스캐폴드는 한 번의 선언(`ScaffoldRequestDTO`: rawQuery + 옵션)으로 아래를 생성한다.

| 산출물 | 소유 | 재생성 시 동작 |
|---|---|---|
| `{Domain}VO`, `{Domain}SearchRequestDTO`, `{Domain}UpdateRequestDTO` | **scaffold** | 덮어쓰기 |
| `{Domain}Mapper`(interface) + `{Domain}Mapper.xml` | **scaffold** | 덮어쓰기 |
| `{Domain}Service`, `{Domain}Controller` | **scaffold 골격** | 덮어쓰기(단, 업무 로직은 §4 확장 슬롯에만) |
| `{domain}.html`, `{domain}.js` | **scaffold** | 덮어쓰기 |
| `db/oracle/{domain}-menu.sql` | **scaffold** | 덮어쓰기 |
| `{Domain}ServiceTest`, `{Domain}ControllerTest` | **scaffold 골격 + 사용자 TODO** | 골격은 덮어쓰기, TODO 케이스는 §4-B |
| **`{Domain}Rules`**(확장 클래스) | **사용자** | **최초 1회만 stub 생성, 이후 재생성 스킵** |

**원칙**: 사용자가 직접 소유하는 코드는 **`{Domain}Rules`(또는 별도 협력자) 하나**로 한정한다. 그 외 생성 파일의 골격은 scaffold 소유다.

## 2. 버전/소유 마커 (machine-visible)

모든 scaffold 생성 파일은 헤더에 소유·버전 마커를 단다. 사람 눈과 `grep` 모두로 식별 가능해야 한다(Oracle 권고: "규칙은 문서가 아니라 마커/테스트로").

- **Java**(class javadoc):
  ```java
  /**
   * Scaffold 생성(v1) — scaffold 소유.
   * 골격은 재생성 시 덮어쓴다. 업무 로직은 §4 확장 슬롯에만 넣는다.
   */
  ```
- **XML/HTML/JS**(첫 줄 주석):
  ```
  <!-- Scaffold 생성(v1) — scaffold 소유. 골격만 재생성 대상. -->
  // Scaffold 생성(v1) — scaffold 소유. 골격만 재생성 대상.
  ```

`v1`은 본 계약 버전이다. 계약이 바뀌면 버전을 올리고 마이그레이션 노트를 남긴다(§6).

## 3. 재생성 보증

재생성(`scavenge` / 재생성 메뉴)은:
1. **scaffold 소유 영역**을 새 선언 기반으로 **완전히 덮어쓴다** (병합 아님).
2. **사용자 소유 영역**(`{Domain}Rules`, 그리고 테스트의 TODO 케이스 블록)은 **건드리지 않는다**.
3. `{Domain}Rules`가 없으면 **최초 1회 stub만 생성**하고, 이후 재생성은 그 파일을 스킵한다(사용자 편집 보존).

→ 그러므로 사용자는 "재생성하면 내 코드가 날아갈까" 걱정 없이 언제든 재생성할 수 있다. 이것이 본 계약의 핵심 보증이다.

## 4. 확장(기능 추가) — 3단 슬롯

"생성 파일 편집 금지 ≠ 기능 추가 금지"다. 기능은 아래 3경로 중 하나로만 넣는다.

### 4-A. 선언 풍부화 (1순위, 대부분의 "기능")
마스킹/엑셀/원문상세/필수검증/권한 같은 횡단 기능은 **메타데이터 옵션**으로 제공한다. 기능 추가 = 선언 1줄 + 재생성. 생성 파일을 손대지 않는다.
> 현재 선언 가능: `screenMode`(LIST/EXCEL/DETAIL/CRUD), `includePrivacy`, 컬럼 옵션(`editable`/`inputMask`/`validate`/`maskType`).
> A-2 로 확장 예정: 엑셀 헤더/마스킹 자동화, 원문상세(`maskView`) 선언화, 서버 `@NotBlank/@Size/@Pattern` 동시 생성.

### 4-B. Hook / 확장 클래스 (2순위, 업무 로직)
업무 검증·파생 계산·연동 사이드이펙트 등은 **`{Domain}Rules`**(사용자 소유)에 넣는다. scaffold 생성 `{Domain}Service`는 항상 `rules.validateOnCreate(request)` / `rules.validateOnUpdate(request)` 등을 호출하는 **고정된 호출점**을 둔다(구현은 A-2).

- `{Domain}Service`(scaffold 소유): `rules.validateOnCreate(req); mapper.insert(req);` 처럼 **호출만**.
- `{Domain}Rules`(사용자 소유): `validateOnCreate` 본문을 자유롭게 구현.

사용자 코드는 `{Domain}Rules` 한 파일에만 존재 → 재생성이 절대 덮어쓰지 않는다.

### 4-C. 명시적 탈출 (예외, 5% 이하)
4-A·4-B로 못 표현하는 특수 케이스(비표준 조인, 레거시 호환 등)는 **해당 파일의 마커를 `scaffold 소유 → 탈출(unplugged)`로 바꾸고** 수동 소유로 전환한다. 그 파일은 이후 재생성이 스킵한다.
- **의도적·표시된** 예외여야 한다(커밋 메시지에 사유 명시).
- 같은 패턴이 2회 이상 반복되면 **4-A/4-B 선언/hook으로 흡수**해 다시 scaffold 소유로 돌린다(탈출이 아키텍처가 되는 것 방지).

## 5. 이스케이프해치 정책 (요약)

| 상황 | 허용 경로 | 금지 |
|---|---|---|
| 횡단 기능(마스킹/엑셀/검증/권한) | 4-A 선언 | 생성 파일 직손편집 |
| 업무 로직/검증/연동 | 4-B `{Domain}Rules` | 생성 Service 직손편집 |
| 진짜 특수 케이스 | 4-C 마킹 후 수동 소유 | 은닉된 무표시 편집 |

**위반 징후**(감지 대상, §6 게이트): scaffold 소유 파일에 마커 없는 편집, `{Domain}Rules` 무시한 Service 직편집, 탈출 마커 없는 수동 파일.

## 6. 계약 위반 감지 (drift gate)

A-3에서 구현: fixture 선언 → 재생성 → 산출물 diff. 예상치 못한 diff(사용자가 scaffold 소유 영역을 편집했거나, 템플릿이 무표시로 바뀐 경우)면 CI 실패. 이 게이트가 "계약이 말로만 있고 깨지는" 것을 막는다.

## 7. 계약 위반 시

1. 생성 파일 직편집을 발견하면 → 해당 변경을 4-A/4-B/4-C 중 하나로 이전.
2. 탈출(unplugged) 파일이 늘어나면 → 레퍼런스가 그 기능을 선언/hook으로 흡수할 수 있는지 검토(A-4 확장의 입력).
3. 재생성 diff가 의도적(템플릿 개선)이면 → fixture를 의도 변경으로 갱신 + 커밋 메시지에 명시.

## 8. 버전

- **v1**(본 문서): 소유권 마커, 3단 확장 슬롯, `{Domain}Rules` 사용자 소유, 재생성 보증 정의. 엔진 마커/후크 일부 적용 중(Service 골격).
- 다음 버전 후보: hook 호출점 전 도메인 적용(A-2 완료 후), drift 게이트 통과 기준 확정(A-3).
