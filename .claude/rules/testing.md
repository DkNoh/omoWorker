---
paths:
  - "**/*.java"
  - "**/*.xml"
  - "**/*.html"
  - "**/*.css"
  - "**/pom.xml"
---

# Testing Rules

- 코드 변경 후 완료 조건은 `mvn test`와 `mvn -DskipTests package` 성공이다.
- 테스트는 given / when / then 구조를 사용한다.
- Service 단위 테스트는 Mapper를 Mockito로 mock 처리한다.
- Mapper 테스트는 `@MybatisTest`를 사용하며 운영 DB를 사용하지 않는다.
- Controller 테스트는 `@Valid` 실패와 공통 `ApiResponse` 에러 포맷을 확인한다.
- LDAP은 local에서 사용할 수 없으므로 테스트에서 mock 처리하거나 profile로 분리한다.
- DB는 local Oracle에 직접 연결해 테스트할 수 있다. 운영 DB만 금지다.
- scaffold가 생성한 ServiceTest/ControllerTest를 배치하고 `// TODO` 업무 규칙 테스트를 채운다.
- `ConventionTest`가 규약(DTO 상속, /save 금지, SELECT * 금지, 정렬 필수, Lombok)을 자동 검증한다. 규약 변경 시 함께 갱신한다.
- 전체 전략은 `docs/base/test-automation-guide.md`를 따른다.
- Spring Boot 서버 기동은 AI가 직접 하지 않는다. 서버 실행은 사용자가 담당한다.
- 테스트/빌드 미실행 상태는 완료가 아니라 부분 완료다.
- 같은 오류가 3회 반복되면 원문 오류, 시도한 3가지, 추정 원인을 보고한다.
- Maven 실행 전 `JAVA_HOME`이 필요하면 설치된 JDK 21을 사용한다.
- 문서만 변경한 경우 Maven 실행은 필수 대상이 아니다.