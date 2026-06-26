# 그리드 셀 포매터 사용 가이드 (Grid Formatter Guide)

> 관련 규약: `screen-convention.md` "TuiPageBuilder / TuiCommon 계약" 절 참고.

## 1. 기본 개념

`formatter`는 셀 값을 받아 **HTML 문자열을 반환**하는 함수다.

```javascript
{
    header: '발송상태',
    name: 'sendStatus',
    formatter: ({ value }) => `<span class="badge bg-success">${value}</span>`
}
```

- 인자: `{ row, value }` — `value`는 셀 값, `row`는 행 전체
- 반환 HTML은 이스케이프되지 않으므로 사용자 입력 직접 삽입 금지(XSS). 정형 코드값에만 사용

---

## 2. 자동 색상 배지 (`autoBadge`) — 가장 추천

**코드값이 뭐가 오든 자동으로 색을 다르게 뿌려주는 범용 포매터.** 화면마다 색상 매핑 테이블을 작성할 필요 없이, 값만 넘기면 된다.

- 같은 값은 항상 같은 색 (해시 기반 자동 할당)
- 다른 값은 가능한 한 다른 색 (6색 팔레트 순환)
- 승인/반려/재승인이면 자동으로 3가지 색으로 표시됨

### 방식 A: 값 그대로 표시 (코드값 자체가 표시용일 때)

```javascript
columns: [
    { header: '부서코드', name: 'deptCd', formatter: TuiCommon.fmt.autoBadge }
]
```

- `IT` → 파랑, `HR` → 초록, `FIN` → 빨강 … (자동 할당)

### 방식 B: 코드값 → 한글 라벨 매핑 (가장 많이 쓰는 패턴)

`TuiCommon.autoBadge(라벨맵)` 팩토리를 사용한다. 색은 라벨 기준으로 자동 할당된다.

```javascript
// static/js/system/appr.js (예시)
columns: [
    {
        header: '승인상태',
        name: 'apprStat',
        formatter: TuiCommon.autoBadge({
            APPR:   '승인',
            REJ:    '반려',
            REAPPR: '재승인'
        })
    }
]
```

- 승인(초록), 반려(빨강), 재승인(노랑) … 자동으로 3가지 색
- 코드값이 늘어나면 라벨맵에 한 줄만 추가하면 됨
- 색을 직접 지정할 필요 없음

> **언제 autoBadge를 쓰나?** "그냥 값마다 색만 다르게 보여주면 되는" 모든 코드값 컬럼. 의미론적 색상(성공=초록 고정)이 꼭 필요한 경우에만 아래 3절 방식을 쓴다.

---

## 3. 의미론적 색상이 필요한 경우

"성공은 무조건 초록, 실패는 무조건 빨강"처럼 색에 의미가 있을 때는 직접 지정한다.

### 3.1 기존 공통 포매터 (해당 도메인일 때만)

| 포매터 | 대상 | 특징 |
|---|---|---|
| `TuiCommon.fmt.date` | 날짜 컬럼 | 범용 |
| `TuiCommon.fmt.sendStatus` | `SUCCESS`/`FAIL`/`WAIT` | SMS 전용. 성공=초록 고정 |
| `TuiCommon.fmt.sendType` | `SMS`/`LMS`/`ALIMTALK` | SMS 전용 |
| `TuiCommon.fmt.resendYn` | `Y`/`N` | SMS 전용 |

> `sendStatus` 등은 SMS 도메인에 특화된 값이다. 다른 화면에서는 억지로 재사용하지 말고 아래 3.2처럼 직접 작성한다.

### 3.2 화면에서 직접 매핑 (색을 직접 지정해야 할 때)

```javascript
// static/js/system/notice.js
columns: [
    {
        header: '게시여부',
        name: 'postYn',
        formatter: ({ value }) => {
            const map = { Y: ['bg-success', '게시'], N: ['bg-secondary', '미게시'] };
            const [cls, label] = map[value] || ['', value || '-'];
            return `<span class="badge ${cls}">${label}</span>`;
        }
    }
]
```

### 3.3 조건부 색상 / 강조

```javascript
{
    header: '대기건수',
    name: 'waitCount',
    formatter: ({ value }) => {
        const n = Number(value) || 0;
        const cls = n >= 100 ? 'text-danger fw-bold' : (n > 0 ? 'text-warning' : 'text-muted');
        return `<span class="${cls}">${n.toLocaleString()}</span>`;
    }
}
```

### 3.4 두 필드 합쳐 표시

```javascript
{
    header: '담당자',
    name: 'empNm',
    formatter: ({ row }) => `${row.empNm} (${row.deptNm || '-'})`
}
```

---

## 4. 아이콘 / 기호 표시

그리드 셀에는 `data-lucide`를 **사용할 수 없다** (TUI Grid 가상 렌더링이 스크롤 시 셀을 동적 생성/제거하므로 `lucide.createIcons()`가 새 셀을 치환하지 못함). 대신 아래 방식 중 하나를 선택한다.

**방식 A: 유니코드 문자 + CSS 클래스 (가장 간단)**

```javascript
{
    header: '중요',
    name: 'priorityYn',
    formatter: ({ value }) => value === 'Y'
        ? '<span class="text-danger fw-bold">★</span>'
        : '<span class="text-muted">☆</span>'
}
```

**방식 B: 인라인 SVG 직접 반환 (정교한 아이콘 필요 시)**

```javascript
const ICON_CHECK = '<svg class="grid-ic" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>';

{
    header: '처리결과',
    name: 'resultYn',
    formatter: ({ value }) => value === 'Y' ? ICON_CHECK : '-'
}
```

SVG 경로는 [lucide.dev](https://lucide.dev)에서 해당 아이콘의 "SVG" 코드를 복사해 가져오면 된다. 폐쇄망에서는 오픈 시 미리 복사해 두거나, `static/lib/lucide.js` 번들 내에서 경로를 찾는다.

아이콘 전용 CSS는 화면 CSS 파일 또는 `admin-ui-bridge.css`에 추가한다:

```css
.grid-ic { width: 1rem; height: 1rem; vertical-align: middle; }
.grid-ic-success { color: var(--cui-success); }
.grid-ic-danger { color: var(--cui-danger); }
```

---

## 5. 공통 포매터에 추가하는 기준

**기본 원칙: `autoBadge`로 충분한 경우가 대부분이다.** 색에 의미가 있고 여러 화면에서 같은 규칙이 필요할 때만 공통(`tui-common.js`)에 추가한다.

| 조건 | 예시 |
|---|---|
| ✅ 동일한 코드값이 **3개 이상 화면**에서 반복 | `SUCCESS`/`FAIL`/`WAIT` (SMS 도메인 전반) |
| ✅ 표시 형태(색상/라벨)가 어디서나 **동일**해야 함 | 날짜 표시(`YYYY-MM-DD`) |
| ✅ 향후 값이 추가되면 **전체 화면이 일괄 변경**되어야 함 | 발송상태 신규 코드 추가 |

공통에 추가할 때의 작성 규칙:

```javascript
// tui-common.js 의 fmt 객체에 추가
apprStat: ({ value }) => {
    const map = { APPR:['bg-success','승인'], REJ:['bg-danger','반려'], REAPPR:['bg-warning text-dark','재승인'] };
    const [cls, label] = map[value] || ['', value || '-'];
    return `<span class="badge ${cls}">${label}</span>`;
}
```

1. `({ value, row })` 형태로 받기
2. 빈 값 시 `'-'` 반환
3. 코드값은 맵(map)으로 관리 (if/else 사다리 금지)
4. CoreUI/Bootstrap 변수 사용 (`bg-success`, `var(--cui-success)`)

---

## 6. 행 단위 스타일

셀이 아닌 **행 전체** 배경을 바꾸려면 formatter가 아닌 `onGridUpdated` 콜백에서 처리한다.

```javascript
new TuiPageBuilder({
    // ...
    onGridUpdated: (page) => {
        // 필요 시 행 단위 커스텀 로직
    }
});
```

---

## 7. 요약 — 무엇을 쓸까

| 상황 | 방식 |
|---|---|
| **값마다 색만 다르게** (가장 흔함) | **`TuiCommon.fmt.autoBadge`** 또는 **`TuiCommon.autoBadge({라벨맵})`** |
| 날짜 표시 | `TuiCommon.fmt.date` |
| SMS 발송상태/유형 (의미론적 색 고정) | `TuiCommon.fmt.sendStatus` 등 |
| 색을 직접 지정해야 함 | 화면 JS에서 코드값→배지 매핑 (3.2절) |
| 조건부 강조 | 화면 JS (3.3절) |
| 두 필드 합치기 | 화면 JS (3.4절) |
| 단순 기호/아이콘 | 유니코드 또는 인라인 SVG (4절) |
| data-lucide | 그리드 셀에 **사용 금지** |