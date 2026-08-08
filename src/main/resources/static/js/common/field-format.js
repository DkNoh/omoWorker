/**
 * @fileoverview field-format.js — IMask 입력 포맷과 JustValidate 폼 검증 공통 모듈.
 *
 * 설계: 화면/스캐폴드는 입력 요소에 data 속성만 선언하고(lucide의 data-lucide와 동일 패턴),
 *       이 모듈이 그 속성을 읽어 라이브러리를 부착한다. 라이브러리를 교체해도 화면 코드는 불변.
 *
 *   <input name="receiverNo" data-mask="phone" data-validate="required|phone">
 *
 * 적용 시점:
 *   - 정적 폼(검색조건 등): DOMContentLoaded에서 applyMasks(document) 자동 호출.
 *   - 동적 폼(TuiPageBuilder 모달): 모달 생성 직후 applyFieldFormats(formEl) 호출.
 *
 * 주의:
 *   - 서버 @Valid가 최종 권위다. 이 모듈의 검증은 UX 보조다.
 *   - 마스크가 붙은 필드는 전송 시 unmaskedValue를 보낸다(FormBinder.toObject가 처리).
 *   - 주민등록번호(ssn) 등 민감 PII는 표시/저장 시 마스킹 정책(PrivacyLog, maskView)을 별도로 따른다.
 *
 * 종류 추가 방법:
 *   FieldFormat.registerMask('zipcode', () => ({ mask: '00000' }));
 *   FieldFormat.registerValidator('zipcode', () => ({ rule: 'customRegexp', value: /^\d{5}$/, errorMessage: '우편번호 형식 오류' }));
 *
 * 공개 API: window.FieldFormat(전역 const)
 *   - applyMasks(root), applyFieldFormats(root), validateForm(root), unmask(el)
 *   - registerMask(name, factory), registerValidator(name, factory)
 *   - maskRegistry, validatorRegistry는 매뉴얼/확장 코드에서 현재 등록값을 확인하기 위해 공개한다.
 */
const FieldFormat = (() => {

    /* --------------------------------------------------------------------------
     * IMask 옵션 레지스트리
     * -------------------------------------------------------------------------- */
    const maskRegistry = {
        /* 전화번호: 02/0XX 지역번호와 휴대폰 형식 중 IMask가 best-fit을 선택한다. */
        phone:  () => ({ mask: [
            { mask: '000-0000-0000' },
            { mask: '000-000-0000' },
            { mask: '00-0000-0000' },
            { mask: '00-000-0000' }
        ] }),
        /* 주민등록번호 6-7 형식. 표시/권한 마스킹은 별도 개인정보 정책을 따른다. */
        ssn:    () => ({ mask: '000000-0000000' }),
        /* 사업자등록번호 3-2-5 형식. */
        bizno:  () => ({ mask: '000-00-00000' }),
        /* 0 이상의 정수이며 화면에는 천 단위 구분자를 표시한다. */
        number: () => ({ mask: Number, thousandsSeparator: ',', scale: 0, min: 0, signed: false }),
        /* 자유 입력용 YYYY-MM-DD 형식. 검색조건 날짜는 TUI DatePicker를 사용한다. */
        date:   () => ({ mask: '0000-00-00' })
    };

    /* --------------------------------------------------------------------------
     * 클라이언트 검증 메시지
     *
     * JustValidate 4.x에는 v3의 setLocale/로케일 파일이 없다. 규칙별 errorMessage에
     * 한글을 주입하며 이 객체를 메시지 단일 관리점으로 사용한다. 길이와 범위처럼
     * 동적 값이 포함되는 메시지는 함수로 정의한다.
     * -------------------------------------------------------------------------- */
    const MESSAGES = {
        required:     '필수 입력 항목입니다.',
        email:        '이메일 형식이 올바르지 않습니다.',
        number:       '숫자만 입력할 수 있습니다.',
        integer:      '정수만 입력할 수 있습니다.',
        minlength:    (n) => `최소 ${n}자 이상 입력하세요.`,
        maxlength:    (n) => `최대 ${n}자까지 입력할 수 있습니다.`,
        min:          (n) => `${n} 이상의 값을 입력하세요.`,
        max:          (n) => `${n} 이하의 값을 입력하세요.`,
        phone:        '전화번호 형식이 올바르지 않습니다.',
        ssn:          '주민등록번호 형식이 올바르지 않습니다.',
        bizno:        '사업자등록번호 형식이 올바르지 않습니다.',
        date:         '날짜 형식(YYYY-MM-DD)이 올바르지 않습니다.',
        url:          'URL 형식이 올바르지 않습니다.',
        alpha:        '영문만 입력할 수 있습니다.',
        alphanumeric: '영문과 숫자만 입력할 수 있습니다.',
        hangul:       '한글만 입력할 수 있습니다.',
        code:         '영문으로 시작하는 영문/숫자 형식이어야 합니다.'
    };

    /* --------------------------------------------------------------------------
     * JustValidate 규칙 레지스트리
     *
     * factory 형식은 `(argument, element) => JustValidate rule`이다. 화면은
     * `data-validate="required|minlength:4|max:9999"`처럼 규칙과 인자만 선언한다.
     * -------------------------------------------------------------------------- */
    const validatorRegistry = {
        /* JustValidate 내장 규칙 */
        required:     () => ({ rule: 'required', errorMessage: MESSAGES.required }),
        email:        () => ({ rule: 'email', errorMessage: MESSAGES.email }),
        number:       () => ({ rule: 'number', errorMessage: MESSAGES.number }),
        integer:      () => ({ rule: 'integer', errorMessage: MESSAGES.integer }),
        minlength:    (arg) => ({ rule: 'minLength', value: Number(arg), errorMessage: MESSAGES.minlength(arg) }),
        maxlength:    (arg) => ({ rule: 'maxLength', value: Number(arg), errorMessage: MESSAGES.maxlength(arg) }),
        min:          (arg) => ({ rule: 'minNumber', value: Number(arg), errorMessage: MESSAGES.min(arg) }),
        max:          (arg) => ({ rule: 'maxNumber', value: Number(arg), errorMessage: MESSAGES.max(arg) }),
        /* 프로젝트 정규식 기반 규칙 */
        phone:        () => ({ rule: 'customRegexp', value: /^0\d{1,2}-?\d{3,4}-?\d{4}$/, errorMessage: MESSAGES.phone }),
        ssn:          () => ({ rule: 'customRegexp', value: /^\d{6}-?\d{7}$/, errorMessage: MESSAGES.ssn }),
        bizno:        () => ({ rule: 'customRegexp', value: /^\d{3}-?\d{2}-?\d{5}$/, errorMessage: MESSAGES.bizno }),
        date:         () => ({ rule: 'customRegexp', value: /^\d{4}-\d{2}-\d{2}$/, errorMessage: MESSAGES.date }),
        url:          () => ({ rule: 'customRegexp', value: /^https?:\/\/[^\s]+$/, errorMessage: MESSAGES.url }),
        alpha:        () => ({ rule: 'customRegexp', value: /^[a-zA-Z]+$/, errorMessage: MESSAGES.alpha }),
        alphanumeric: () => ({ rule: 'customRegexp', value: /^[a-zA-Z0-9]+$/, errorMessage: MESSAGES.alphanumeric }),
        hangul:       () => ({ rule: 'customRegexp', value: /^[가-힣]+$/, errorMessage: MESSAGES.hangul }),
        code:         () => ({ rule: 'customRegexp', value: /^[a-zA-Z][a-zA-Z0-9_-]*$/, errorMessage: MESSAGES.code })
    };

    /* form 요소가 제거되면 validator 참조도 GC 대상이 되도록 WeakMap으로 관리한다. */
    const validators = new WeakMap();

    /**
     * 런타임에 새 `data-mask` 이름을 등록하거나 기존 정의를 교체한다.
     * @param {string} name data-mask 속성에 사용할 이름
     * @param {Function} factory `(element) => IMask options` 팩토리
     * @returns {void}
     */
    const registerMask = (name, factory) => { maskRegistry[name] = factory; };

    /**
     * 런타임에 새 `data-validate` 이름을 등록하거나 기존 정의를 교체한다.
     * @param {string} name data-validate 토큰 이름
     * @param {Function} factory `(argument, element) => JustValidate rule` 팩토리
     * @returns {void}
     */
    const registerValidator = (name, factory) => { validatorRegistry[name] = factory; };

    /**
     * root 내부의 `[data-mask]` 요소에 IMask를 부착한다.
     *
     * `_imask`가 이미 있는 필드는 건너뛰어 중복 이벤트/마스크 인스턴스 생성을 막는다.
     * IMask 라이브러리가 없는 페이지에서는 선택 기능으로 간주하고 조용히 종료한다.
     *
     * @param {Document|Element|null} root 탐색 기준 DOM. 생략하면 document
     * @returns {void}
     */
    const applyMasks = (root) => {
        const scope = root || document;
        if (typeof IMask === 'undefined') {
            return;
        }
        scope.querySelectorAll('[data-mask]').forEach(el => {
            if (el._imask) {
                /* 동일 요소에 IMask와 이벤트가 중복 부착되지 않게 한다. */
                return;
            }
            const factory = maskRegistry[el.dataset.mask];
            if (!factory) {
                console.warn('[FieldFormat] 미등록 mask:', el.dataset.mask, el);
                return;
            }
            el._imask = IMask(el, factory(el));
        });
    };

    /**
     * root 내부의 `[data-validate]` 선언을 읽어 JustValidate 인스턴스를 구성한다.
     *
     * 토큰은 `|`로 구분하고 값은 `:` 뒤에 둔다(예: `required|minlength:4`).
     * 등록되지 않은 토큰은 무시하며, id 없는 필드에는 selector로 사용할 임시 id를 부여한다.
     * 같은 form을 다시 구성하면 이전 인스턴스를 destroy한 뒤 교체한다.
     *
     * @param {HTMLFormElement|Element} root 검증 범위이자 JustValidate 생성 대상
     * @returns {Object|null} JustValidate 인스턴스. 라이브러리/대상 필드가 없으면 null
     */
    const buildValidator = (root) => {
        if (typeof JustValidate === 'undefined' || !root) {
            return null;
        }
        const fields = root.querySelectorAll('[data-validate]');
        if (fields.length === 0) {
            return null;
        }
        /* 모달처럼 같은 form을 재사용할 때 이전 인스턴스와 listener를 먼저 정리한다. */
        const prev = validators.get(root);
        if (prev && typeof prev.destroy === 'function') {
            prev.destroy();
        }
        const validator = new JustValidate(root);
        fields.forEach(el => {
            if (!el.id) {
                /* addField가 CSS selector를 요구하므로 충돌 가능성이 낮은 id를 생성한다. */
                el.id = 'ff-' + Math.random().toString(36).slice(2);
            }
            const rules = el.dataset.validate.split('|')
                .map(token => {
                    const [name, arg] = token.split(':').map(s => s.trim());
                    const factory = validatorRegistry[name];
                    /* 미등록 토큰은 null로 정규화한 뒤 filter 단계에서 제거한다. */
                    return factory ? factory(arg, el) : null;
                })
                .filter(Boolean);
            if (rules.length > 0) {
                validator.addField('#' + el.id, rules);
            }
        });
        validators.set(root, validator);
        return validator;
    };

    /**
     * 동적으로 생성된 form에 마스크와 검증을 한 번에 부착한다.
     * @param {HTMLFormElement|Element} root 적용 범위
     * @returns {void}
     */
    const applyFieldFormats = (root) => {
        applyMasks(root);
        buildValidator(root);
    };

    /**
     * buildValidator/applyFieldFormats로 등록한 root form의 검증을 실행한다.
     * 검증기가 없거나 검증 필드가 없으면 화면 흐름을 막지 않고 true로 처리한다.
     * 각 필드를 개별 재검증해 오류 라벨을 렌더링하고 첫 오류 필드로 포커스를 이동한다.
     *
     * @param {HTMLFormElement|Element} root 검증할 form
     * @returns {Promise<boolean>} 모든 필드가 유효하면 true
     */
    const validateForm = (root) => {
        const validator = validators.get(root);
        if (!validator) {
            return Promise.resolve(true);
        }
        const fields = Array.from(root.querySelectorAll('[data-validate]'));
        if (fields.length === 0) {
            return Promise.resolve(true);
        }
        /*
         * JustValidate 4.3.0 revalidate()는 포커스만 하고 오류 라벨을 렌더링하지 않는
         * upstream 결함(horprogs/Just-validate#155)이 있다. revalidateField로 각 필드를
         * 개별 검증하고 첫 invalid 필드에 명시적으로 포커스를 이동한다.
         */
        return Promise.all(fields.map(el => validator.revalidateField('#' + el.id)))
            .then(results => {
                const firstInvalid = results.indexOf(false);
                if (firstInvalid !== -1) {
                    fields[firstInvalid].focus();
                }
                return results.every(Boolean);
            });
    };

    /**
     * 화면 표시용 마스크 문자를 제거한 실제 전송값을 읽는다.
     * @param {HTMLInputElement|null} el 값을 읽을 입력 요소
     * @returns {string} IMask 필드는 unmaskedValue, 일반 필드는 value, 요소가 없으면 빈 문자열
     */
    const unmask = (el) => {
        if (!el) {
            return '';
        }
        return el._imask ? el._imask.unmaskedValue : el.value;
    };

    return {
        applyMasks,
        applyFieldFormats,
        validateForm,
        unmask,
        registerMask,
        registerValidator,
        maskRegistry,
        validatorRegistry
    };
})();

/*
 * 정적 폼은 DOM 완성 후 document 전체에 마스크만 자동 부착한다. 검증기는 저장 동작과
 * 생명주기를 맞춰야 하므로 화면 또는 ModalManager가 명시적으로 구성한다.
 */
document.addEventListener('DOMContentLoaded', () => FieldFormat.applyMasks(document));
