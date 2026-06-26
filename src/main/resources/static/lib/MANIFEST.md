# Frontend Vendored Libraries

CDN을 쓰지 않고 로컬에 박제(vendoring)한 프론트엔드 라이브러리 목록이다.
(`thymeleaf.md`: CDN 참조 금지, `static/lib`·`static/vendor` 로컬 파일만 사용)

## 규칙

- `static/lib`·`static/vendor`에 라이브러리 파일을 추가/교체하면 이 표의 버전·출처·라이선스를 함께 갱신한다.
- 교체 시 **동일 메이저/마이너 버전**을 우선한다. 버전을 올리면 API 호환성을 확인하고 비고에 기록한다.
- 가능하면 공식 `.min` 배포본을 사용한다. 프로젝트 자체 작성 소스(`static/css/*`, `static/js/**`)는 vendoring 대상이 아니며 minify하지 않는다(빌드 파이프라인 부재).

## static/lib

| 파일 | 라이브러리 | 버전 | 출처 | 라이선스 | 갱신일 | 비고 |
|---|---|---|---|---|---|---|
| imask.min.js | IMask.js | 7.6.1 | https://cdn.jsdelivr.net/npm/imask@7.6.1/dist/imask.min.js | MIT | 2026-06-19 | 포맷 마스킹. unmaskedValue로 전송값 분리 |
| just-validate.min.js | JustValidate | 4.3.0 | https://unpkg.com/just-validate@4.3.0/dist/just-validate.production.min.js | MIT | 2026-06-19 | 클라 폼 검증(서버 @Valid가 최종 권위) |
| tui-grid.min.js | TOAST UI Grid | 4.21.22 | https://cdn.jsdelivr.net/npm/tui-grid@4.21.22/dist/tui-grid.min.js | MIT | 2026-06-19 | tui-date-picker/tui-pagination/xlsx 전역 의존 |
| tui-grid.css | TOAST UI Grid | 4.21.22 | https://cdn.jsdelivr.net/npm/tui-grid@4.21.22/dist/tui-grid.css | MIT | 2026-06-19 | TUI는 CSS 별도 min 미배포(이 파일이 최종 dist) |
| tui-date-picker.min.js | TOAST UI Date Picker | 4.3.3 | https://cdn.jsdelivr.net/npm/tui-date-picker@4.3.3/dist/tui-date-picker.min.js | MIT | 2026-06-19 | |
| tui-date-picker.css | TOAST UI Date Picker | 4.3.3 | https://cdn.jsdelivr.net/npm/tui-date-picker@4.3.3/dist/tui-date-picker.css | MIT | 2026-06-19 | CSS min 미배포 |
| tui-pagination.min.js | TOAST UI Pagination | 3.4.1 | https://cdn.jsdelivr.net/npm/tui-pagination@3.4.1/dist/tui-pagination.min.js | MIT | 2026-06-19 | |
| tui-pagination.css | TOAST UI Pagination | 3.4.1 | https://cdn.jsdelivr.net/npm/tui-pagination@3.4.1/dist/tui-pagination.css | MIT | 2026-06-19 | CSS min 미배포 |
| axios.min.js | axios | 1.16.1 | https://www.npmjs.com/package/axios | MIT | (기존) | HTTP 클라이언트 |
| dayjs.min.js | Day.js | 확인 필요 | https://www.npmjs.com/package/dayjs | MIT | (기존) | 헤더에서 버전 미검출 — 교체 시 확정 |
| ko.js | Day.js locale (ko) | dayjs와 동일 | https://www.npmjs.com/package/dayjs | MIT | (기존) | dayjs 한국어 로케일 |
| xlsx.full.min.js | SheetJS (xlsx) | 확인 필요 | https://sheetjs.com/ | Apache-2.0 | (기존) | ⚠️ 배포 채널/버전 표기 확인 필요 |
| lucide.js | (커스텀 서브셋) | - | 프로젝트 자체 작성 | ISC(원본 lucide) | (기존) | 공식 lucide 아님. 사용 아이콘만 손으로 추린 5.8KB 구현 + 자체 createIcons(). **공식 .min으로 교체 금지** |

## static/vendor

| 파일 | 라이브러리 | 버전 | 출처 | 라이선스 | 갱신일 | 비고 |
|---|---|---|---|---|---|---|
| coreui/css/coreui.min.css | CoreUI | 확인 필요 | https://coreui.io/ | MIT | (기존) | 헤더에서 버전 확정 후 기입 |
| coreui/js/coreui.bundle.min.js | CoreUI | 확인 필요 | https://coreui.io/ | MIT | (기존) | |
