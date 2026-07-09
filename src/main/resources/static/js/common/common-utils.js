/**
 * common-utils.js (축소본 — Phase 1)
 * axios 인터셉터 / spinner / toast / alert / confirm / 세션만료 처리는 분리됨:
 *   - http-client.js   : axios interceptor + spinner + 세션만료 + get/post/put/delete/remove
 *   - notify.js        : toast / alert / confirm / frameworkModal helpers
 *   - modal-manager.js : 수동 비즈니스 모달 lifecycle
 *
 * 이 모듈에는 순수 유틸(콤보/날짜/검색/포맷/autocomplete)만 남는다.
 * 하위 호환 — toast/alert/confirm/refreshIcons는 Notify로 연결되어 기존 호출부가 그대로 동작한다.
 */
(function () {
    'use strict';

    // ════════════════════════════════════════════════════
    // 공통 코드 콤보박스 자동 생성 (.common-combo[data-code-type])
    // ════════════════════════════════════════════════════
    const initCombos = async () => {
        const comboList = document.querySelectorAll('.common-combo');
        if (comboList.length === 0) return;

        // 동일 코드를 여러 콤보에서 요청할 수 있으므로 캐시 사용
        const cache = {};

        for (const selectEl of comboList) {
            const type = selectEl.getAttribute('data-code-type');
            if (!type) continue;

            if (!cache[type]) {
                try {
                    const res = await axios.get(`/api/common-code/${type}`);
                    cache[type] = res.data;
                } catch (e) {
                    console.error(`공통 코드 조회 실패 [${type}]`, e);
                    cache[type] = [];
                }
            }

            // 기존 옵션('전체' 등) 보존하면서 추가
            const existing = selectEl.innerHTML;
            let appended = '';
            cache[type].forEach(item => {
                appended += `<option value="${item.code}">${item.name}</option>`;
            });
            selectEl.innerHTML = existing + appended;
        }
    };

    // ════════════════════════════════════════════════════
    // 날짜/시간 검색 폼 기본값 초기화
    // ════════════════════════════════════════════════════
    const setDefaultDateTime = (forceReset = false) => {
        const now = dayjs();
        const todayStr = now.format('YYYY-MM-DD');
        const fromTime = now.subtract(1, 'hour').format('HH:mm');
        const toTime = now.add(1, 'hour').format('HH:mm');

        // ① 분리형: #startDate / #startTime / #endDate / #endTime
        const startDate = document.querySelector('#startDate');
        const endDate = document.querySelector('#endDate');
        const startTime = document.querySelector('#startTime');
        const endTime = document.querySelector('#endTime');

        if (startDate && (forceReset || !startDate.value)) startDate.value = todayStr;
        if (endDate && (forceReset || !endDate.value)) endDate.value = todayStr;
        if (startTime && (forceReset || !startTime.value)) startTime.value = fromTime;
        if (endTime && (forceReset || !endTime.value)) endTime.value = toTime;

        // ② 통합형: #startDateTime / #endDateTime
        const startDT = document.querySelector('#startDateTime');
        const endDT = document.querySelector('#endDateTime');

        if (startDT && (forceReset || !startDT.value)) startDT.value = `${todayStr}T00:00`;
        if (endDT && (forceReset || !endDT.value)) endDT.value = `${todayStr}T23:59`;

        // ③ 단순 date input (날짜만)
        document.querySelectorAll('input[type="date"]').forEach(el => {
            if (forceReset || !el.value) el.value = todayStr;
        });
    };

    const resetFields = () => {
        const receiverNo = document.querySelector('#receiverNo');
        const sendType = document.querySelector('#sendType');

        if (receiverNo) receiverNo.value = '';
        if (sendType) sendType.value = '';

        setDefaultDateTime(true);
    };

    // ════════════════════════════════════════════════════
    // 데이터 포매터 (그리드/카드 등에서 자주 사용)
    // ════════════════════════════════════════════════════
    const fmt = {
        money: (val) => {
            if (val == null || val === '') return '0';
            const num = Number(val);
            if (isNaN(num)) return val;
            return num.toLocaleString();
        },
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

    // ════════════════════════════════════════════════════
    // Autocomplete (말풍선 자동완성) 공통 초기화
    // 사용 예)
    // CommonUtils.initAutocomplete({
    //     inputEl:   '#bankCdText',
    //     balloonEl: '#bankBalloon',
    //     apiUrl:    '/api/common-code/bank',
    //     syncCombo: '#bankCdCombo',   // 선택사항
    //     minLength:  1,
    //     debounceMs: 200,
    //     onSelect: (item) => console.log(item) // 선택사항
    // });
    // ════════════════════════════════════════════════════
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

        const close = () => { balloon.style.display = 'none'; };

        const select = (code, name) => {
            input.value = code;
            // 값 직접 설정 시 input 이벤트 미발생 → 수동 dispatch
            input.dispatchEvent(new Event('input', { bubbles: true }));

            if (combo) {
                combo.value = code;
                combo.dispatchEvent(new Event('change', { bubbles: true }));
            }
            close();
            if (onSelect) onSelect({ code, name });
        };

        // 콤보 → 텍스트 동기화
        if (combo) {
            combo.addEventListener('change', () => {
                input.value = combo.value;
                close();
            });
        }

        // 텍스트 입력 → 검색
        input.addEventListener('input', () => {
            clearTimeout(debounceTimer);
            const kw = input.value.trim();

            if (kw.length < minLength) {
                close();
                if (combo) combo.value = '';
                return;
            }

            debounceTimer = setTimeout(() => {
                axios.get(apiUrl, { params: { [paramName]: kw } })
                    .then(res => {
                        // 전역 인터셉터가 ApiResponse를 언래핑하므로 res.data가 곧 목록
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

        // 외부 클릭 시 닫기
        document.addEventListener('click', e => {
            if (!input.contains(e.target) && !balloon.contains(e.target)) close();
        });

        return { close, select };
    };

    // 자동 실행
    document.addEventListener('DOMContentLoaded', () => {
        initCombos();
    });

    // ════════════════════════════════════════════════════
    // 공개 — Notify로 re-export (기존 CommonUtils.* 호환)
    // ════════════════════════════════════════════════════
    const CommonUtils = {
        initCombos,
        initAutocomplete,
        setDefaultDateTime,
        resetFields,
        fmt,
        // Notify re-export (http-client.js, notify.js가 먼저 로드되어 있어야 함)
        get toast()     { return window.Notify ? window.Notify.toast   : () => {}; },
        get alert()     { return window.Notify ? window.Notify.alert   : () => {}; },
        get confirm()   { return window.Notify ? window.Notify.confirm : () => {}; },
        get refreshIcons() { return window.Notify ? window.Notify.refreshIcons : () => {}; }
    };

    window.CommonUtils = CommonUtils;
})();
