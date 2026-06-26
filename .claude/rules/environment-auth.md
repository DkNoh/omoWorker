---
paths:
  - "**/application*.yml"
  - "**/config/**/*Auth*.java"
  - "**/auth/**/*.java"
  - "**/templates/login.html"
---

# Environment / Auth Rules

- 상세 기준은 `docs/base/environment-auth-policy.md`를 따른다.
- local/dev/prod의 차이는 인증 방식뿐이다.
- local은 ID-only 인증을 사용한다.
- dev/prod는 LDAP 인증을 사용한다.
- 사용자 확인은 모든 환경에서 `EMP`, `DEP` 기준이다.
- 사용자는 항상 `(EMP_ID, DEP_ID)`로 식별한다.
- local 전용 사용자/권한 테이블을 만들지 않는다.
- `EMP.PERM_*`는 인증/권한 판단에 사용하지 않는다.
- LDAP URL, manager DN은 placeholder 가능하지만 비밀번호는 환경변수로만 둔다.
- 인증 실패를 임시 계정이나 인메모리 사용자로 우회하지 않는다.