/**
 * field-format.js
 * 입력 포맷 마스킹(IMask) + 클라이언트 폼 검증(JustValidate) 공통 모듈.
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
 */
const FieldFormat = (() => {

    // ── 마스크 레지스트리: name -> IMask 옵션 팩토리 (el을 받아 동적 구성 가능) ──
    const maskRegistry = {
        // 전화번호: 02/0XX 지역번호와 휴대폰을 모두 수용 (IMask가 best-fit 선택)
        phone:  () => ({ mask: [
            { mask: '000-0000-0000' },
            { mask: '000-000-0000' },
            { mask: '00-0000-0000' },
            { mask: '00-000-0000' }
        ] }),
        // 주민등록번호 6-7 (민감 PII)
        ssn:    () => ({ mask: '000000-0000000' }),
        // 사업자등록번호 3-2-5
        bizno:  () => ({ mask: '000-00-00000' }),
        // 숫자 (천단위 콤마)
        number: () => ({ mask: Number, thousandsSeparator: ',', scale: 0, min: 0, signed: false }),
        // 날짜 YYYY-MM-DD (자유 입력용. 검색조건 날짜는 기존 TUI DatePicker 사용)
        date:   () => ({ mask: '0000-00-00' })
    };

    // ── 검증 레지스트리: name -> (arg, el) => JustValidate rule ──
    const validatorRegistry = {
        required:  () => ({ rule: 'required' }),
        email:     () => ({ rule: 'email' }),
        number:    () => ({ rule: 'number' }),
        minlength: (arg) => ({ rule: 'minLength', value: Number(arg) }),
        maxlength: (arg) => ({ rule: 'maxLength', value: Number(arg) }),
        phone:     () => ({ rule: 'customRegexp', value: /^0\d{1,2}-?\d{3,4}-?\d{4}$/, errorMessage: '전화번호 형식이 올바르지 않습니다.' }),
        ssn:       () => ({ rule: 'customRegexp', value: /^\d{6}-?\d{7}$/, errorMessage: '주민등록번호 형식이 올바르지 않습니다.' }),
        bizno:     () => ({ rule: 'customRegexp', value: /^\d{3}-?\d{2}-?\d{5}$/, errorMessage: '사업자등록번호 형식이 올바르지 않습니다.' })
    };

    // form(root) 요소 -> JustValidate 인스턴스
    const validators = new WeakMap();

    const registerMask = (name, factory) => { maskRegistry[name] = factory; };
    const registerValidator = (name, factory) => { validatorRegistry[name] = factory; };

    /** root 내부의 [data-mask] 요소에 IMask를 부착한다(중복 부착 방지). */
    const applyMasks = (root) => {
        const scope = root || document;
        if (typeof IMask === 'undefined') {
            return;
        }
        scope.querySelectorAll('[data-mask]').forEach(el => {
            if (el._imask) {
                return; // 이미 부착됨
            }
            const factory = maskRegistry[el.dataset.mask];
            if (!factory) {
                console.warn('[FieldFormat] 미등록 mask:', el.dataset.mask, el);
                return;
            }
            el._imask = IMask(el, factory(el));
        });
    };

    /** root 내부의 [data-validate]로 JustValidate 검증기를 구성한다. */
    const buildValidator = (root) => {
        if (typeof JustValidate === 'undefined' || !root) {
            return null;
        }
        const fields = root.querySelectorAll('[data-validate]');
        if (fields.length === 0) {
            return null;
        }
        // 같은 폼을 재사용하는 경우(모달) 이전 인스턴스를 정리한다
        const prev = validators.get(root);
        if (prev && typeof prev.destroy === 'function') {
            prev.destroy();
        }
        const validator = new JustValidate(root);
        fields.forEach(el => {
            if (!el.id) {
                el.id = 'ff-' + Math.random().toString(36).slice(2);
            }
            const rules = el.dataset.validate.split('|')
                .map(token => {
                    const [name, arg] = token.split(':').map(s => s.trim());
                    const factory = validatorRegistry[name];
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

    /** 마스크 + 검증을 한 번에 부착한다(동적 폼용). */
    const applyFieldFormats = (root) => {
        applyMasks(root);
        buildValidator(root);
    };

    /** root 폼의 검증을 실행한다. 검증기가 없으면 통과(true). 반환: Promise<boolean> */
    const validateForm = (root) => {
        const validator = validators.get(root);
        if (!validator) {
            return Promise.resolve(true);
        }
        return validator.revalidate();
    };

    /** 마스크 필드는 unmaskedValue를, 아니면 value를 반환한다. */
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

// 정적 폼(검색조건 등)의 마스크는 페이지 로드 시 자동 부착한다.
document.addEventListener('DOMContentLoaded', () => FieldFormat.applyMasks(document));
