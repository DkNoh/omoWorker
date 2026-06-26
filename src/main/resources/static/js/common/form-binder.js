/**
 * form-binder.js
 * 상세폼 화면의 자동 바인딩 공통 모듈. (screen-convention.md "상세폼 화면 규약")
 *
 * 계약: form 필드의 name = 응답 JSON 필드명 = UpdateRequestDTO 프로퍼티명.
 * 자동 바인딩은 화면 편의일 뿐, 수정 요청은 반드시 UpdateRequestDTO(화이트리스트)로만 받는다.
 */
const FormBinder = (() => {

    /**
     * 조회 응답을 form에 자동 바인딩한다.
     * - data[name]이 있는 필드만 채운다. 없는 필드는 건드리지 않는다.
     * - checkbox: 'Y' 또는 true면 checked
     * - radio: 같은 name 중 value가 일치하는 항목 checked
     * - select/text/textarea: value 지정 (null은 빈 값)
     */
    const bind = (formSelector, data) => {
        const form = document.querySelector(formSelector);
        if (!form || !data) return;

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
     */
    const toObject = (formSelector) => {
        const form = document.querySelector(formSelector);
        const result = {};
        if (!form) return result;

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
