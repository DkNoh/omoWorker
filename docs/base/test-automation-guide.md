# 테스트 자동화 가이드

폐쇄망에서 JUnit 테스트를 기본으로 운영하기 위한 자동화 구조다. 사내 AI 보유 + Jenkins 보유 환경(2026-06-12 확인)을 기준으로 한다.

## 자동화 구조 (3층)

```text
1층. 생성   : scaffold가 화면 코드와 함께 테스트 2종을 생성 (ServiceTest, ControllerTest)
2층. 규약   : ConventionTest가 소스 스캔으로 전 화면의 규약을 자동 검증
3층. 실행   : 로컬 mvn 게이트 + Jenkins 잡이 push마다 동일 게이트 실행
```

## 1층 — scaffold 테스트 생성

scaffold 산출물에 테스트 2종이 포함된다 (총 11종).

| 산출물 | 검증 내용 |
|---|---|
| `{Domain}ServiceTest.java` | Mapper를 Mockito mock 처리. `search()`가 count/selectList를 호출하고 `PageResponseDTO`로 감싸는지. CUD 옵션 시 위임 검증 추가 |
| `{Domain}ControllerTest.java` | standalone MockMvc로 `/data` 호출 → 200 + `ApiResponse` 포맷(`$.code`, `$.data.totalCount`) 검증. CRUD 옵션 시 `/create`, `/update`, `/delete`까지 함께 생성 — 200 + 성공 메시지(`$.message`, 예: "등록되었습니다.")와 Service 위임(`then(service).should()...`) 검증 |

생성된 테스트는 골격이다. **`// TODO: 업무 규칙 테스트` 부분을 채우는 것이 사람/AI의 몫**이며,
업무 규칙(상태 전이, 검증 조건, 마스킹)이 있는 화면은 TODO를 채우기 전까지 부분 완료다.

## 2층 — 컨벤션 테스트 (`src/test/java/com/scbk/sms/ConventionTest.java`)

소스 파일을 직접 스캔하므로 화면이 늘어나도 검증 범위가 자동으로 늘어난다.

| 규칙 | 근거 문서 |
|---|---|
| `*SearchRequestDTO`는 `PageRequestDTO` 상속 | project.md |
| Controller에 `/save` endpoint 금지 | menu-authority.md |
| Mapper XML에 `SELECT *` 금지 (파생 테이블 `A.*`는 허용) | mybatis-oracle.md |
| `OFFSET` 페이지 조회에 `ORDER BY` 필수 | mybatis-oracle.md |
| 신규 도메인 DTO/VO는 Lombok `@Data` (BASE 공통 코드는 제외) | project.md |

새 규약을 추가/변경할 때는 ConventionTest와 해당 규칙 문서를 함께 갱신한다.

## 3층 — 실행 자동화

로컬: 화면 생성 절차서의 게이트 그대로.

```text
mvn test
mvn -DskipTests package
```

Jenkins: push(또는 PR)마다 같은 게이트를 실행하는 잡을 구성한다. 예시 declarative pipeline:

```groovy
pipeline {
    agent any
    tools { jdk 'jdk-21' }   // 사내 Jenkins의 JDK21 tool 이름으로 교체
    stages {
        stage('Test')    { steps { sh 'mvn -B test' } }
        stage('Package') { steps { sh 'mvn -B -DskipTests package' } }
    }
    post {
        always { junit 'target/surefire-reports/*.xml' }
    }
}
```

Windows 에이전트면 `sh` 대신 `bat`. 사내 Jenkins 표준(공유 라이브러리, 알림)에 맞춰 조정한다.

## 사내 AI 운영 방식

- 화면 생성 loop에 테스트가 내장된다: 화면 생성 → 테스트 TODO 채움 → `mvn test` → 실패 시 원인 수정 → 같은 오류 3회 반복 시 보고.
- AI에게는 규칙 문장보다 견본이 정확하다. 견본 위치:
  - Service 테스트 견본: `MenuAuthServiceTest`, `CommonCodeServiceTest`
  - 유틸 테스트 견본: `MaskingUtilTest`
  - 예외 분기 견본: `GlobalExceptionHandlerTest`
- 업무 규칙 테스트는 사람이 케이스를 지시하고 AI가 작성하는 분업을 권장한다.

## Mapper(SQL) 검증에 대해

scaffold의 타입 추론이 생성 시점에 SQL을 실제 DB에서 1회 실행하므로 문법/컬럼 오류는 생성 단계에서 걸러진다.
운영 중 SQL 변경에 대한 `@MybatisTest` 스모크 테스트는 test profile(DB 접속) 정리가 필요해 도입하지 않았다.
필요해지는 시점에 local Oracle 기준 test profile과 함께 추가한다.

## 검증 결과

2026-06-12 기준:

```text
mvn test  PASS (58 tests — ConventionTest 5, GlobalExceptionHandlerTest 3, scaffold 테스트 템플릿 검증 2 포함)
```
