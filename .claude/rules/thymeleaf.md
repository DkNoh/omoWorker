---
paths:
  - "**/templates/**/*.html"
  - "**/static/css/**/*.css"
---

# Thymeleaf / UI Rules

- 업무 화면 구조는 `docs/base/screen-convention.md`의 골격을 그대로 따른다.
- 업무 화면은 `layout:decorate="~{defaultLayout}"`를 사용한다. login.html은 예외다.
- 목록 그리드는 `TuiPageBuilder`로만 초기화한다. 그리드를 직접 구현하지 않는다.
- 버튼/그리드 ID는 `btn-search`, `btn-reset`, `btn-excel`, `grid`, `pagination`, `total-count`를 사용한다.
- CDN 참조를 금지한다. `static/lib`, `static/vendor` 로컬 파일만 사용한다.
- 화면은 업무 시스템 기준의 조용하고 명확한 UI로 만든다.
- 화면 설명 문구를 과하게 넣지 않는다.
- 로그인 후 기본 화면은 메뉴와 사용자 상태를 확인할 수 있어야 한다.
- 메뉴 렌더링은 Controller가 넘긴 메뉴 tree만 사용한다.
- 화면에서 권한을 임의 계산하지 않는다.
- URL은 하드코딩하더라도 `TB_MENU` 또는 static baseline과 일치해야 한다.
- 개인정보는 화면 표시 전 마스킹 정책을 확인한다.
- 텍스트가 버튼/패널을 넘치지 않도록 한다.
- 카드 안에 카드를 중첩하지 않는다.