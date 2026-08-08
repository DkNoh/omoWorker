# SheetJS (xlsx) 매뉴얼 (프로젝트 한정)

> 대상 라이브러리: **SheetJS (xlsx)** — 버전 확인 필요 (vendored, CDN 미사용)
>
> 대상 파일
> - `src/main/resources/static/lib/xlsx.full.min.js` — 라이브러리 본체
> - `src/main/resources/static/js/common/tui-common.js` — `TuiCommon.exportExcel()` 래퍼
> - `src/main/java/com/scbk/sms/util/ExcelUtil.java` — 서버 사이드 엑셀 다운로드 (Apache POI, SheetJS 미사용)
>
> 이 문서는 SheetJS 전체 API를 다루지 않는다. **이 프로젝트에서 실제로 쓰는 부분만** 다룬다.
> 관련 문서: [`tui-grid_manual.md`](./tui-grid_manual.md) (TUI Grid export), `docs/base/common-response-contract.md`

---

## 목차

1. [개요](#1-개요)
2. [프로젝트 로드 방식](#2-프로젝트-로드-방식)
3. [프로젝트 사용 방식 — 엑셀 다운로드 두 가지 경로](#3-프로젝트-사용-방식--엑셀-다운로드-두-가지-경로)
4. [핵심 API 요약](#4-핵심-api-요약)
5. [주의사항 및 프로젝트 특이사항](#5-주의사항-및-프로젝트-특이사항)
6. [참조](#6-참조)

---

## 1. 개요

| 항목 | 값 |
|---|---|
| 라이브러리 | SheetJS (xlsx) |
| 버전 | **확인 필요** — 파일 헤더에서 버전 미검출. 교체 시 확정 필요 |
| 라이선스 | Apache-2.0 |
| 출처 | https://sheetjs.com/ |
| 용도 | TUI Grid의 클라이언트 사이드 엑셀 내보내기(`grid.export('xlsx')`)를 위한 전역 의존성 |

`MANIFEST.md` 등재 행:

```text
| xlsx.full.min.js | SheetJS (xlsx) | 확인 필요 | https://sheetjs.com/ | Apache-2.0 | (기존) | ⚠️ 배포 채널/버전 표기 확인 필요 |
```

### 이 프로젝트에서 SheetJS의 위치

SheetJS는 **TUI Grid가 내부적으로 사용하는 전역 의존성**으로 로드된다. 프로젝트 JS 코드가 `XLSX` 전역 변수를 직접 호출하지 않는다. TUI Grid의 `export('xlsx')` 메서드가 내부적으로 `window.XLSX`를 참조해 `.xlsx` 파일을 생성한다.

---

## 2. 프로젝트 로드 방식

### 2.1 스크립트 태그 위치

`src/main/resources/templates/defaultLayout.html`의 `<head>` 내부:

```html
<script src="/lib/tui-pagination.min.js"></script>   <!-- 17행 -->
<script src="/lib/xlsx.full.min.js"></script>         <!-- 18행: SheetJS -->
<script src="/lib/tui-date-picker.min.js"></script>   <!-- 19행 -->
<script src="/lib/tui-grid.min.js"></script>          <!-- 20행: xlsx 전역 의존 -->
```

### 2.2 로드 순서

xlsx는 **반드시 tui-grid.min.js보다 먼저** 로드되어야 한다. TUI Grid가 초기화 시 `window.XLSX` 전역 변수를 참조하므로, 순서가 뒤바뀌면 `grid.export('xlsx')` 호출 시 런타임 오류가 발생한다.

```text
tui-pagination → xlsx → tui-date-picker → tui-grid → axios → dayjs → ...
```

### 2.3 전역 변수

| 전역 | 설명 |
|---|---|
| `window.XLSX` | SheetJS UMD 전역. 프로젝트 코드에서 직접 사용하지 않으며, TUI Grid 내부에서만 참조 |

---

## 3. 프로젝트 사용 방식 — 엑셀 다운로드 두 가지 경로

이 프로젝트에는 엑셀 다운로드 경로가 **두 가지** 존재한다.

### 3.1 클라이언트 사이드 — TUI Grid export (SheetJS 사용)

`TuiCommon.exportExcel()`이 TUI Grid의 내장 export를 호출하고, TUI Grid가 내부적으로 SheetJS를 사용해 브라우저에서 `.xlsx` 파일을 생성·다운로드한다.

```javascript
// tui-common.js (111~114행)
const exportExcel = (gridObj, fileName = 'download') => {
    if (!gridObj) return;
    gridObj.export('xlsx', { fileName: fileName });
};
```

호출 예:

```javascript
// 화면 JS에서
TuiCommon.exportExcel(grid, 'SMS_발송내역');
```

이 경로는 **그리드에 현재 렌더링된 데이터**만 내보낸다. 서버에서 전체 데이터를 다시 조회하지 않는다.

### 3.2 서버 사이드 — Scaffold EXCEL + ExcelUtil (Apache POI 사용, SheetJS 미사용)

Scaffold **EXCEL** 모드는 검색 조건 전체를 다운로드하는 서버 endpoint를 생성한다. 이 경로는 **SheetJS와 무관**하며, 서버에서 Apache POI(`SXSSFWorkbook`)로 `.xlsx`를 스트리밍한다. 현재 메뉴의 테스트 화면이 아니라 `scaffold-templates/excel/` 생성 계약을 기준으로 한다.

```javascript
// scaffold-templates/excel/page.js.tpl 생성 결과
const btnExcel = document.querySelector('#btn-excel');
if (btnExcel) {
    btnExcel.addEventListener('click', () => {
        if (!window.PAGE_AUTH || window.PAGE_AUTH.download !== true) {
            CommonUtils.toast('엑셀 다운로드 권한이 없습니다.', 'warning');
            return;
        }
        const params = new URLSearchParams(pageBuilder.getSearchParams());
        window.location.href = API.excel + '?' + params.toString();
    });
}
```

서버 흐름:

```text
브라우저 GET /<module>/<domain>/excel?검색조건
  → {Domain}Controller.downloadExcel()
    → {Domain}Service.downloadExcel()
      → {Domain}Mapper.selectListForExcel()      // 전체 데이터 조회
      → ExcelUtil.downloadExcel()               // Apache POI SXSSF 스트리밍
        → HttpServletResponse OutputStream으로 .xlsx 전송
```

`ExcelUtil.java` 핵심:

```java
// SXSSFWorkbook: 100행 단위로 메모리 유지, 나머지 임시 파일 플러시 (OOM 방지)
try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
    Sheet sheet = workbook.createSheet("Data");
    // 헤더 스타일 + 데이터 행 작성 ...
    workbook.write(response.getOutputStream());
}
```

### 3.3 두 경로 비교

| 구분 | 클라이언트 (TUI Grid) | 서버 (ExcelUtil) |
|---|---|---|
| 사용 라이브러리 | SheetJS (xlsx) | Apache POI (SXSSF) |
| 데이터 범위 | 그리드 현재 페이지/렌더링 데이터 | 검색 조건 전체 데이터 |
| 권한 검사 | 별도 없음 | `PAGE_AUTH.download` + 서버 `MenuAuthInterceptor` |
| 감사 로그 | 없음 | `@PrivacyLog` 적용 대상 |
| 대용량 처리 | 브라우저 메모리 제한 | SXSSF 스트리밍 (OOM 방지) |
| 사용 화면 | LIST/CRUD의 현재 페이지 export | Scaffold EXCEL 생성 화면 |

---

## 4. 핵심 API 요약

프로젝트 코드에서 직접 호출하는 SheetJS API는 **없다**. TUI Grid를 경유하는 간접 사용만 존재한다.

### 4.1 TUI Grid 경유 (간접)

```javascript
// TUI Grid 인스턴스의 export 메서드
grid.export('xlsx', { fileName: '파일명' });
```

- `export()` 내부: `window.XLSX.utils.json_to_sheet()` → `XLSX.writeFile()` 순으로 호출
- 파일명: `fileName` 옵션 미지정 시 기본값 `'download'`

### 4.2 TuiCommon.exportExcel 래퍼

```javascript
TuiCommon.exportExcel(gridObj, fileName)
```

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `gridObj` | TUI Grid 인스턴스 | `null`/`undefined` 시 아무 동작 없음 |
| `fileName` | string (기본 `'download'`) | 다운로드 파일명 (확장자 제외) |

---

## 5. 주의사항 및 프로젝트 특이사항

### 5.1 버전 미확인

MANIFEST.md에 **"확인 필요"** 로 표기되어 있다. 파일 헤더에서 버전 문자열이 검출되지 않았다. 라이브러리 교체 시 동일 메이저/마이너 버전을 우선하고, 교체 후 버전을 확정해 MANIFEST.md에 기입한다.

### 5.2 `xlsx.full.min.js` 사용

`xlsx.min.js`(core)가 아닌 `xlsx.full.min.js`(full)이다. full 빌드에는 코드페이지(cpexcel) 등 추가 의존이 포함되어 있다. TUI Grid export가 정상 동작하려면 full 빌드가 필요하다.

### 5.3 직접 호출 금지

프로젝트 JS 코드에서 `XLSX.*` API를 직접 호출하지 않는다. 엑셀 내보내기는 반드시 `TuiCommon.exportExcel()` 또는 서버 사이드 `ExcelUtil` 경로를 사용한다.

### 5.4 서버 사이드 엑셀과의 혼동 주의

`ExcelUtil.java`는 Apache POI 기반이며 SheetJS와 무관하다. 서버 사이드 다운로드(`/excel` 엔드포인트)는 `window.location.href` 방식으로 브라우저 네비게이션을 사용하므로, axios 인터셉터(spinner, CSRF)를 거치지 않는다.

### 5.5 개인정보·감사 로그

서버 사이드 엑셀 다운로드는 감사 로그 대상이다. 개인정보 컬럼은 마스킹된 값으로 조회한 데이터만 전달한다. 클라이언트 사이드 export는 이 통제를 우회하므로, 개인정보 포함 화면에서는 서버 사이드 경로를 사용한다.

---

## 6. 참조

- `src/main/resources/static/lib/MANIFEST.md` — 버전·라이선스·출처 (27행)
- `src/main/resources/templates/defaultLayout.html` — 로드 순서 (18행)
- `src/main/resources/static/js/common/tui-common.js` — `exportExcel()` (111~114행)
- `src/main/resources/scaffold-templates/excel/page.js.tpl` — 서버 사이드 엑셀 다운로드 화면 생성 계약
- `src/test/resources/scaffold-golden/excel_JS.txt` — EXCEL 대표 생성 결과
- `src/main/java/com/scbk/sms/util/ExcelUtil.java` — 서버 사이드 Apache POI 유틸리티
- [`tui-grid_manual.md`](./tui-grid_manual.md) — TUI Grid export 상세
