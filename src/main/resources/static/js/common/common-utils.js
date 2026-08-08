/**
 * @fileoverview common-utils.js — 화면 공통 유틸리티와 기존 CommonUtils 호환 API.
 *
 * Phase 1 축소본.
 * axios 인터셉터 / spinner / toast / alert / confirm / 세션만료 처리는 분리됨:
 *   - http-client.js   : axios interceptor + spinner + 세션만료 + get/post/put/delete/remove
 *   - notify.js        : toast / alert / confirm / frameworkModal helpers
 *   - modal-manager.js : 수동 비즈니스 모달 lifecycle
 *
 * 이 모듈에는 화면 상태를 직접 소유하지 않는 공통 유틸
 * (공통 코드 콤보, 날짜 기본값, 검색조건 초기화, 표시 포맷, autocomplete)만 남는다.
 *
 * 로드/의존 계약:
 *   - axios: 공통 코드 및 autocomplete API 호출
 *   - dayjs: 날짜/시간 기본값 계산
 *   - notify.js: toast/alert/confirm/refreshIcons 하위 호환 getter의 실제 구현
 *   - defaultLayout.html에서 notify.js 다음, 화면별 JS보다 먼저 로드한다.
 *
 * 공개 API: window.CommonUtils
 *   - initCombos(), initAutocomplete(), setDefaultDateTime(), resetFields(), fmt
 *   - toast/alert/confirm/refreshIcons는 Notify로 연결되어 기존 호출부가 그대로 동작한다.
 *
 * 실패 정책:
 *   - 화면에 대상 DOM이 없으면 조용히 종료한다.
 *   - 공통 코드 한 종류의 조회가 실패해도 다른 콤보 초기화는 계속한다.
 */
(function () {
    'use strict';

    /* --------------------------------------------------------------------------
     * 공통 코드 콤보박스
     * -------------------------------------------------------------------------- */
    /**
     * `.common-combo[data-code-type]` select를 서버 공통 코드로 채운다.
     *
     * 같은 code type을 사용하는 select가 여러 개여도 페이지 초기화 1회 동안 API는 한 번만 호출한다.
     * HTML에 미리 선언된 `전체`, `선택` 등의 option은 보존하고 조회 결과를 뒤에 추가한다.
     *
     * @returns {Promise<void>} 모든 콤보의 조회/구성이 끝나면 완료된다.
     */
    const initCombos = async () => {
        const comboList = document.querySelectorAll('.common-combo');
        if (comboList.length === 0) return;

        /* 함수 호출 1회 범위의 캐시다. 화면을 새로 열면 서버에서 다시 조회한다. */
        const cache = {};

        for (const selectEl of comboList) {
            const type = selectEl.getAttribute('data-code-type');
            if (!type) continue;

            if (!cache[type]) {
                try {
                    const res = await axios.get(`/api/common-code/${type}`);
                    /* http-client 응답 인터셉터가 ApiResponse.data를 이미 언래핑한다. */
                    cache[type] = res.data;
                } catch (e) {
                    console.error(`공통 코드 조회 실패 [${type}]`, e);
                    cache[type] = [];
                }
            }

            /* HTML에 선언된 '전체', '선택' 등의 기본 option을 보존한다. */
            const existing = selectEl.innerHTML;
            let appended = '';
            cache[type].forEach(item => {
                appended += `<option value="${item.code}">${item.name}</option>`;
            });
            selectEl.innerHTML = existing + appended;
        }
    };

    /* --------------------------------------------------------------------------
     * 날짜/시간 검색 기본값
     * -------------------------------------------------------------------------- */
    /**
     * 화면에 존재하는 공통 날짜/시간 검색 필드에 기본값을 채운다.
     *
     * `forceReset=false`면 사용자가 입력한 기존 값을 보존하고 빈 필드만 채운다.
     * `forceReset=true`면 초기화 버튼 처리처럼 현재 값을 기본값으로 덮어쓴다.
     * 지원 ID는 분리형(startDate/startTime/endDate/endTime), 통합형(startDateTime/endDateTime),
     * 그리고 일반 `input[type=date]`다.
     *
     * @param {boolean} [forceReset=false] 기존 값까지 기본값으로 교체할지 여부
     * @returns {void}
     */
    const setDefaultDateTime = (forceReset = false) => {
        const now = dayjs();
        const todayStr = now.format('YYYY-MM-DD');
        const fromTime = now.subtract(1, 'hour').format('HH:mm');
        const toTime = now.add(1, 'hour').format('HH:mm');

        /* 분리형: #startDate / #startTime / #endDate / #endTime */
        const startDate = document.querySelector('#startDate');
        const endDate = document.querySelector('#endDate');
        const startTime = document.querySelector('#startTime');
        const endTime = document.querySelector('#endTime');

        if (startDate && (forceReset || !startDate.value)) startDate.value = todayStr;
        if (endDate && (forceReset || !endDate.value)) endDate.value = todayStr;
        if (startTime && (forceReset || !startTime.value)) startTime.value = fromTime;
        if (endTime && (forceReset || !endTime.value)) endTime.value = toTime;

        /* 통합형: #startDateTime / #endDateTime */
        const startDT = document.querySelector('#startDateTime');
        const endDT = document.querySelector('#endDateTime');

        if (startDT && (forceReset || !startDT.value)) startDT.value = `${todayStr}T00:00`;
        if (endDT && (forceReset || !endDT.value)) endDT.value = `${todayStr}T23:59`;

        /* 일반 date input: 날짜만 설정한다. */
        document.querySelectorAll('input[type="date"]').forEach(el => {
            if (forceReset || !el.value) el.value = todayStr;
        });
    };

    /**
     * 레거시 공통 검색조건(receiverNo/sendType)을 비우고 날짜 범위를 다시 설정한다.
     * 화면에 해당 필드가 없어도 오류를 발생시키지 않는다.
     *
     * @returns {void}
     */
    const resetFields = () => {
        const receiverNo = document.querySelector('#receiverNo');
        const sendType = document.querySelector('#sendType');

        if (receiverNo) receiverNo.value = '';
        if (sendType) sendType.value = '';

        setDefaultDateTime(true);
    };

    /* --------------------------------------------------------------------------
     * 표시 포매터
     * -------------------------------------------------------------------------- */
    /** 그리드/카드 표시용 포매터 모음. 원본 데이터는 변경하지 않고 표시 문자열만 반환한다. */
    const fmt = {
        /** 숫자를 현재 브라우저 로케일의 천 단위 구분 문자열로 변환한다. */
        money: (val) => {
            if (val == null || val === '') return '0';
            const num = Number(val);
            if (isNaN(num)) return val;
            return num.toLocaleString();
        },
        /** 숫자 이외의 문자를 제거한 뒤 국내 전화번호 길이에 맞춰 하이픈을 넣는다. */
        phone: (val) => {
            if (!val) return '';
            const clean = String(val).replace(/[^0-9]/g, '');
            if (clean.length === 9) return clean.replace(/(\d{2})(\d{3})(\d{4})/, '$1-$2-$3');
            if (clean.length === 10) {
                if (clean.startsWith('02')) return clean.replace(/(\d{2})(\d{4})(\d{4})/, '$1-$2-$3');
                return clean.replace(/(\d{3})(\d{3})(\d{4})/, '$1-$2-$3');
            }
            if (clean.length === 11) return clean.replace(/(\d{3})(\d{4})(\d{4})/, '$1-$2-$3');
            return clean;
        }
    };

    /* --------------------------------------------------------------------------
     * Autocomplete
     * -------------------------------------------------------------------------- */
    /**
     * 입력창과 결과 말풍선을 연결하는 debounce 기반 autocomplete를 구성한다.
     *
     * API 응답은 `{code, name}` 객체 배열을 전제로 한다. 기본 renderer는 이 값을 HTML에 삽입하므로
     * 공통 코드처럼 서버에서 신뢰할 수 있게 관리되는 값에 사용한다. 임의 사용자 입력을 결과 객체에
     * 그대로 담는 API라면 화면에서 escape 처리된 `renderItem`을 별도로 전달해야 한다.
     *
     * @param {object} options
     * @param {string|Element} options.inputEl 검색어를 입력할 input 또는 selector
     * @param {string|Element} options.balloonEl 검색 결과를 표시할 컨테이너 또는 selector
     * @param {string} options.apiUrl 검색 API URL
     * @param {string|Element|null} [options.syncCombo=null] 선택 코드를 동기화할 select 또는 selector
     * @param {string} [options.paramName='keyword'] API query parameter 이름
     * @param {number} [options.minLength=1] 검색을 시작할 최소 글자 수
     * @param {number} [options.debounceMs=200] 마지막 입력 후 API를 호출하기까지의 지연 시간(ms)
     * @param {Function|null} [options.renderItem=null] 결과 객체 하나를 HTML 문자열로 바꾸는 함수
     * @param {Function|null} [options.onSelect=null] 항목 선택 후 `{code, name}`으로 호출할 함수
     * @returns {{close: Function, select: Function}|undefined} 외부 제어 함수. 필수 DOM이 없으면 undefined
     */
    const initAutocomplete = ({
        inputEl, balloonEl, apiUrl,
        syncCombo = null,
        paramName = 'keyword',
        minLength = 1,
        debounceMs = 200,
        renderItem = null,
        onSelect = null
    } = {}) => {
        const input = typeof inputEl === 'string' ? document.querySelector(inputEl) : inputEl;
        const balloon = typeof balloonEl === 'string' ? document.querySelector(balloonEl) : balloonEl;
        const combo = syncCombo ? (typeof syncCombo === 'string' ? document.querySelector(syncCombo) : syncCombo) : null;

        if (!input || !balloon) {
            console.warn('[Autocomplete] inputEl 또는 balloonEl을 찾을 수 없습니다.', { inputEl, balloonEl });
            return;
        }

        let debounceTimer;

        const renderItemFn = renderItem || ((item) =>
            `<div class="autocomplete-item" data-code="${item.code}" data-name="${item.name}">
                <span class="ac-code">${item.code}</span>
                <span class="ac-name">${item.name}</span>
             </div>`
        );

        /* DOM을 제거하지 않고 숨겨 다음 검색에서 같은 컨테이너를 재사용한다. */
        const close = () => { balloon.style.display = 'none'; };

        const select = (code, name) => {
            /* 표시 input과 선택 select에는 name이 아니라 업무 코드값을 동기화한다. */
            input.value = code;
            /* value 직접 설정은 input 이벤트를 발생시키지 않으므로 수동으로 알린다. */
            input.dispatchEvent(new Event('input', { bubbles: true }));

            if (combo) {
                combo.value = code;
                combo.dispatchEvent(new Event('change', { bubbles: true }));
            }
            close();
            if (onSelect) onSelect({ code, name });
        };

        /* select 변경을 검색 input에 동기화한다. */
        if (combo) {
            combo.addEventListener('change', () => {
                input.value = combo.value;
                close();
            });
        }

        /* 검색 input 변경을 debounce API 조회로 연결한다. */
        input.addEventListener('input', () => {
            clearTimeout(debounceTimer);
            const kw = input.value.trim();

            if (kw.length < minLength) {
                close();
                if (combo) combo.value = '';
                return;
            }

            /* 빠른 연속 입력은 취소하고 마지막 입력만 API 호출로 이어지게 한다. */
            debounceTimer = setTimeout(() => {
                axios.get(apiUrl, { params: { [paramName]: kw } })
                    .then(res => {
                        /* 전역 인터셉터가 ApiResponse를 언래핑하므로 res.data가 실제 목록이다. */
                        const list = res.data || [];
                        if (!list.length) {
                            balloon.innerHTML = '<div class="ac-empty">검색 결과 없음</div>';
                            balloon.style.display = 'block';
                            return;
                        }
                        balloon.innerHTML = list.map(renderItemFn).join('');
                        balloon.style.display = 'block';

                        balloon.querySelectorAll('.autocomplete-item').forEach(item => {
                            item.addEventListener('click', () =>
                                select(item.dataset.code, item.dataset.name));
                        });
                    })
                    .catch(() => close());
            }, debounceMs);
        });

        /* 입력창과 말풍선 바깥을 클릭하면 결과만 닫고 선택 값은 유지한다. */
        document.addEventListener('click', e => {
            if (!input.contains(e.target) && !balloon.contains(e.target)) close();
        });

        return { close, select };
    };

    /* 정적 화면은 별도 호출 없이 공통 코드 콤보를 초기화한다. */
    document.addEventListener('DOMContentLoaded', () => {
        initCombos();
    });

    /* --------------------------------------------------------------------------
     * 공개 API
     * -------------------------------------------------------------------------- */
    const CommonUtils = {
        initCombos,
        initAutocomplete,
        setDefaultDateTime,
        resetFields,
        fmt,
        /* notify.js 공개 API를 getter로 연결해 기존 CommonUtils 호출부를 유지한다. */
        get toast()     { return window.Notify ? window.Notify.toast   : () => {}; },
        get alert()     { return window.Notify ? window.Notify.alert   : () => {}; },
        get confirm()   { return window.Notify ? window.Notify.confirm : () => {}; },
        get refreshIcons() { return window.Notify ? window.Notify.refreshIcons : () => {}; }
    };

    window.CommonUtils = CommonUtils;
})();
