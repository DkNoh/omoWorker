# TOAST UI Editor 매뉴얼 (프로젝트 한정)

> 대상 라이브러리: **TOAST UI Editor 3.2.2** (vendored, CDN 미사용, SHA-256 검증 완료)
>
> 대상 파일
> - `src/main/resources/static/lib/toastui-editor/3.2.2/toastui-editor-all.min.js` — 에디터 본체 (`all` 번들)
> - `src/main/resources/static/lib/toastui-editor/3.2.2/toastui-editor.min.css` — 에디터 스타일
> - `src/main/resources/static/lib/toastui-editor/3.2.2/LICENSE` — MIT 라이선스 원문
> - `src/main/resources/static/js/basic/notice-popup.js` — 에디터 초기화·사용 코드
> - `src/main/resources/templates/basic/notice-popup.html` — 에디터 로드 템플릿
>
> 이 문서는 TOAST UI Editor 전체 API를 다루지 않는다. **이 프로젝트에서 실제로 쓰는 부분만** 다룬다.

---

## 목차

1. [개요](#1-개요)
2. [프로젝트 로드 방식](#2-프로젝트-로드-방식)
3. [프로젝트 사용 방식 — 공지사항 팝업 에디터](#3-프로젝트-사용-방식--공지사항-팝업-에디터)
4. [핵심 API 요약](#4-핵심-api-요약)
5. [주의사항 및 프로젝트 특이사항](#5-주의사항-및-프로젝트-특이사항)
6. [참조](#6-참조)

---

## 1. 개요

| 항목 | 값 |
|---|---|
| 라이브러리 | TOAST UI Editor |
| 버전 | **3.2.2** (SHA-256 검증 완료) |
| 라이선스 | MIT |
| 출처 | https://uicdn.toast.com/editor/3.2.2/toastui-editor-all.min.js |
| 용도 | 공지사항 팝업(`notice-popup.html`)의 본문 WYSIWYG 에디터 |

`MANIFEST.md` 등재 행:

```text
| toastui-editor/3.2.2/toastui-editor-all.min.js | TOAST UI Editor | 3.2.2 | ... | MIT | 2026-07-13 | 공지 팝업 본문 에디터. `all` 번들(의존 포함), UMD 전역 `toastui.Editor`. SHA-256 f50e1b7c... |
| toastui-editor/3.2.2/toastui-editor.min.css    | TOAST UI Editor | 3.2.2 | ... | MIT | 2026-07-13 | 에디터 전용 스타일. SHA-256 c70e24c6... |
| toastui-editor/3.2.2/LICENSE                    | TOAST UI Editor | 3.2.2 | ... | MIT | 2026-07-13 | 표준 MIT 원문. SHA-256 1c7c070d... |
```

### `all` 번들이란

`toastui-editor-all.min.js`는 에디터 본체 + 의존성(ProseMirror, Markdown-it 등)을 하나로 합친 UMD 번들이다. 별도 의존 스크립트 추가 없이 이 파일 하나로 동작한다.

---

## 2. 프로젝트 로드 방식

### 2.1 defaultLayout이 아닌 notice-popup.html에서만 로드

TOAST UI Editor는 **`defaultLayout.html`에 로드되지 않는다.** 공지사항 팝업 화면(`notice-popup.html`)에서만 로드한다. 모든 페이지에서 에디터가 필요하지 않으므로, 해당 화면에만 한정해 로드한다.

### 2.2 CSS 로드 — `<head>` 내부

`notice-popup.html`의 `<head>`에서 직접 로드한다 (`layout:fragment="css"` 아님):

```html
<!-- notice-popup.html 11행 -->
<link rel="stylesheet" th:href="@{/lib/toastui-editor/3.2.2/toastui-editor.min.css}">
```

### 2.3 JS 로드 — `layout:fragment="script"` (body 하단)

```html
<!-- notice-popup.html 116~126행 -->
<th:block layout:fragment="script">
    <script th:src="@{/lib/toastui-editor/3.2.2/toastui-editor-all.min.js}"></script>
    <script th:inline="javascript">
        window.noticePageAuth = {
            create: /*[[${pageAuth.create}]]*/ false,
            update: /*[[${pageAuth.update}]]*/ false,
            delete: /*[[${pageAuth.delete}]]*/ false
        };
    </script>
    <script th:src="@{/js/basic/notice-popup.js}"></script>
</th:block>
```

### 2.4 로드 순서

```text
defaultLayout <head>의 공통 라이브러리 (tui-grid, axios, dayjs, ...)
  → defaultLayout <body> 하단: CoreUI bundle JS
    → layout:fragment="script":
        1. toastui-editor-all.min.js   ← 에디터 본체
        2. noticePageAuth 인라인 스크립트
        3. notice-popup.js             ← 에디터 초기화 코드
```

에디터 JS는 **반드시 `notice-popup.js`보다 먼저** 로드되어야 한다. `notice-popup.js`가 `toastui.Editor` 전역을 참조하므로 순서가 뒤바뀌면 에디터가 초기화되지 않는다.

### 2.5 전역 변수

| 전역 | 설명 |
|---|---|
| `window.toastui.Editor` | UMD 전역. `new toastui.Editor({...})`로 인스턴스 생성 |

---

## 3. 프로젝트 사용 방식 — 공지사항 팝업 에디터

### 3.1 HTML 구조

```html
<!-- notice-popup.html 56~61행 -->
<div class="mb-3">
    <label for="content" id="content-label" class="form-label fw-bold">본문</label>
    <!-- Toast UI Editor가 이 요소를 대체한다 -->
    <div id="content-editor"></div>
    <textarea id="content" name="content" class="d-none" aria-hidden="true"></textarea>
</div>
```

- `#content-editor`: 에디터가 마운트되는 컨테이너. 에디터가 이 요소 내부에 자체 DOM을 생성한다.
- `#content` (hidden textarea): 에디터 미로드 시 폴백용. 에디터가 정상 로드되면 사용하지 않는다.

### 3.2 에디터 초기화

```javascript
// notice-popup.js initEditor() (50~64행)
function initEditor() {
    if (typeof toastui === 'undefined' || !toastui.Editor) {
        console.warn('Toast UI Editor가 로드되지 않았습니다. ...');
        return;
    }
    editor = new toastui.Editor({
        el: document.querySelector('#content-editor'),
        height: '400px',
        initialEditType: 'wysiwyg',
        previewStyle: 'vertical',
        placeholder: '본문을 입력하세요'
    });
    applyEditorA11y();
}
```

| 옵션 | 값 | 설명 |
|---|---|---|
| `el` | `#content-editor` | 에디터 마운트 대상 DOM 요소 |
| `height` | `'400px'` | 에디터 높이 |
| `initialEditType` | `'wysiwyg'` | 초기 편집 모드. WYSIWYG(리치 텍스트)으로 시작 |
| `previewStyle` | `'vertical'` | 마크다운 미리보기 스타일 (WYSIWYG 모드에서는 미사용) |
| `placeholder` | `'본문을 입력하세요'` | 빈 상태 안내 문구 |

### 3.3 데이터 읽기/쓰기

```javascript
// 상세 조회 시 에디터에 내용 채우기 (fillForm, 164~168행)
if (editor && data.content) {
    editor.setMarkdown(data.content);
} else if (!editor) {
    document.getElementById('content').value = data.content || '';
}

// 저장 시 에디터에서 내용 추출 (collectFormData, 227~229행)
if (editor) {
    data.content = editor.getMarkdown();
}
```

서버와 주고받는 본문 형식은 **Markdown**이다. WYSIWYG 모드에서 편집하더라도 `getMarkdown()`으로 Markdown 문자열을 추출해 저장한다.

### 3.4 접근성 (A11Y) 처리

에디터 생성 직후 `applyEditorA11y()`가 에디터 내부 DOM에 접근성 속성을 부여한다:

```javascript
// notice-popup.js applyEditorA11y() (69~86행)
const surfaces = root.querySelectorAll('.toastui-editor-contents[contenteditable="true"]');
surfaces.forEach(function (el) {
    el.setAttribute('aria-labelledby', CONTENT_LABEL_ID);  // 'content-label'
    el.setAttribute('aria-label', '본문');
    el.setAttribute('role', 'textbox');
    el.setAttribute('aria-multiline', 'true');
});

const groups = root.querySelectorAll('.toastui-editor-defaultUI, .toastui-editor');
groups.forEach(function (el) {
    el.setAttribute('role', 'group');
    el.setAttribute('aria-labelledby', CONTENT_LABEL_ID);
});
```

에디터가 동적으로 생성하는 DOM이므로, **에디터 생성 직후에만** 이 속성을 설정할 수 있다.

---

## 4. 핵심 API 요약

이 프로젝트에서 사용하는 API는 4개뿐이다.

| API | 용도 | 호출 위치 |
|---|---|---|
| `new toastui.Editor(options)` | 에디터 인스턴스 생성 | `notice-popup.js` `initEditor()` |
| `editor.setMarkdown(md)` | Markdown 문자열로 에디터 내용 설정 | `notice-popup.js` `fillForm()` |
| `editor.getMarkdown()` | 에디터 내용을 Markdown 문자열로 추출 | `notice-popup.js` `collectFormData()` |
| `typeof toastui !== 'undefined'` | 에디터 로드 여부 가드 | `notice-popup.js` `initEditor()` |

### 생성자 옵션 (프로젝트 사용분)

```javascript
new toastui.Editor({
    el: HTMLElement,          // 필수. 마운트 대상
    height: '400px',          // 에디터 높이
    initialEditType: 'wysiwyg', // 'markdown' | 'wysiwyg'
    previewStyle: 'vertical',   // 'tab' | 'vertical'
    placeholder: 'string'       // 빈 상태 안내
})
```

---

## 5. 주의사항 및 프로젝트 특이사항

### 5.1 defaultLayout에 추가 금지

에디터는 공지사항 팝업 전용이다. `defaultLayout.html`에 로드하면 모든 페이지에서 불필요한 JS/CSS가 로드된다. 새 화면에서 에디터가 필요하면 `notice-popup.html`의 로드 패턴을 참고해 해당 화면 템플릿에만 추가한다.

### 5.2 `all` 번들 교체 주의

`toastui-editor-all.min.js`는 의존 포함 번들이다. `toastui-editor.min.js`(core만)로 교체하면 ProseMirror 등 의존이 빠져 동작하지 않는다. 교체 시 반드시 `all` 번들을 사용한다.

### 5.3 SHA-256 검증 완료

2026-07-13 기준 세 파일 모두 SHA-256 해시가 MANIFEST.md에 기록되어 있다. 파일 무결성 확인 시 아래 값과 대조한다:

| 파일 | SHA-256 |
|---|---|
| `toastui-editor-all.min.js` | `f50e1b7c0fc4e5d9a1ccd0d8be78cb3a950ccb3bf676fbf1627810c76aeaedd8` |
| `toastui-editor.min.css` | `c70e24c68fefc205e8e504edc07fd6a5efd3044a623b4be7e3ac16cc8a736ed9` |
| `LICENSE` | `1c7c070da956f0c3122883c1b4eb706b1b819971082ccde001fe2d241d90e3c0` |

### 5.4 에디터 미로드 폴백

`initEditor()`에서 `toastui` 전역이 없으면 경고 로그만 남기고 반환한다. 이 경우 `editor`는 `null`로 유지되며, `fillForm()`과 `collectFormData()`가 hidden textarea(`#content`)로 폴백한다. 에디터 없이도 폼 제출은 가능하지만 WYSIWYG 편집은 불가하다.

### 5.5 Markdown 저장 형식

서버 DB에 저장되는 본문은 Markdown 문자열이다. WYSIWYG 모드에서 작성한 내용도 `getMarkdown()`을 거치면 Markdown으로 직렬화된다. 조회 시 `setMarkdown()`이 Markdown을 파싱해 WYSIWYG으로 렌더링한다.

### 5.6 버전 업그레이드

동일 메이저/마이너(3.2.x) 내 업그레이드를 우선한다. 3.x → 4.x 등 메이저 변경 시 API 호환성을 확인하고, SHA-256 해시를 갱신해 MANIFEST.md에 기록한다.

---

## 6. 참조

- `src/main/resources/static/lib/MANIFEST.md` — 버전·라이선스·SHA-256 (29~31행)
- `src/main/resources/templates/basic/notice-popup.html` — 로드 템플릿 (11행 CSS, 117행 JS)
- `src/main/resources/static/js/basic/notice-popup.js` — 에디터 초기화·사용 (50~86행, 164~168행, 227~229행)
- `src/main/resources/static/lib/toastui-editor/3.2.2/LICENSE` — MIT 라이선스 원문
