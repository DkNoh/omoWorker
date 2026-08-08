/**
 * @fileoverview form-binder.js — 상세 form과 DTO 객체 사이의 공통 변환 모듈.
 *
 * 기준 문서: screen-convention.md의 "상세폼 화면 규약".
 *
 * 계약: form 필드의 name = 응답 JSON 필드명 = UpdateRequestDTO 프로퍼티명.
 * 자동 바인딩은 화면 편의일 뿐, 수정 요청은 반드시 UpdateRequestDTO(화이트리스트)로만 받는다.
 *
 * 의존/공개 계약:
 *   - 외부 라이브러리 의존 없음. `data-mask` 필드는 field-format.js가 생성한 `_imask`를 선택적으로 사용한다.
 *   - window.FormBinder = { bind, toObject } 형태로 화면별 JS에 노출한다.
 *   - DOM을 직접 읽고 쓰지만 submit/API 호출은 수행하지 않는다.
 */
const FormBinder = (() => {

    /**
     * 조회 응답을 form에 자동 바인딩한다.
     * - data[name]이 있는 필드만 채운다. 없는 필드는 건드리지 않는다.
     * - checkbox: 'Y' 또는 true면 checked
     * - radio: 같은 name 중 value가 일치하는 항목 checked
     * - select/text/textarea: value 지정 (null은 빈 값)
     *
     * @param {string} formSelector 대상 form을 찾는 CSS selector
     * @param {Object} data 조회 API에서 받은 key-value 객체
     * @returns {void} form 또는 data가 없으면 아무 작업 없이 종료한다.
     */
    const bind = (formSelector, data) => {
        const form = document.querySelector(formSelector);
        if (!form || !data) return;

        /* 응답에 존재하는 key만 순회하므로 화면 전용 필드의 현재 값은 보존된다. */
        Object.keys(data).forEach(name => {
            const fields = form.querySelectorAll(`[name="${name}"]`);
            if (fields.length === 0) return;

            const value = data[name];
            fields.forEach(field => {
                if (field.type === 'checkbox') {
                    field.checked = (value === 'Y' || value === true);
                } else if (field.type === 'radio') {
                    field.checked = String(field.value) === String(value);
                } else {
                    field.value = (value === null || value === undefined) ? '' : value;
                }
            });
        });
    };

    /**
     * form의 name 필드를 읽어 전송용 객체로 만든다.
     * - disabled 필드는 제외한다.
     * - checkbox: checked 여부를 'Y'/'N'으로 변환
     * - radio: checked 항목의 value (선택 없음이면 null)
     * - 빈 문자열은 null로 전송한다 (BASE 확정 정책, 2026-06-12)
     * - data-mask 필드(IMask 부착)는 표시값이 아니라 unmaskedValue를 전송한다 (field-format.js)
     * - 같은 name의 radio는 최초 순회 시 null을 만들고 checked 항목을 만나면 실제 값으로 덮어쓴다.
     *
     * @param {string} formSelector 대상 form을 찾는 CSS selector
     * @returns {Object} DTO 전송에 사용할 plain object. form이 없으면 빈 객체
     */
    const toObject = (formSelector) => {
        const form = document.querySelector(formSelector);
        const result = {};
        if (!form) return result;

        /* name 없는 표현용 필드와 disabled 필드는 서버 DTO 대상이 아니므로 제외한다. */
        form.querySelectorAll('input[name], select[name], textarea[name]').forEach(field => {
            if (field.disabled) return;
            const name = field.name;

            if (field.type === 'checkbox') {
                result[name] = field.checked ? 'Y' : 'N';
            } else if (field.type === 'radio') {
                if (field.checked) {
                    result[name] = field.value;
                } else if (!(name in result)) {
                    result[name] = null;
                }
            } else {
                const raw = field._imask ? field._imask.unmaskedValue : field.value;
                result[name] = raw === '' ? null : raw;
            }
        });
        return result;
    };

    return { bind, toObject };
})();
