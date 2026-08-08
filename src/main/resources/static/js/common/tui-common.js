/**
 * @fileoverview tui-common.js — TUI Grid 표시와 부가 기능 공통 모듈.
 *
 * 표시 포맷, 배지, 개인정보 마스킹, 기본 옵션, 페이징, Excel export를 제공한다.
 *
 * 의존:
 *   - dayjs: 날짜 파싱/표시
 *   - TUI Grid: exportExcel 호출 대상
 *
 * 공개 API: window.TuiCommon(전역 const)
 *   - fmt, badgeByValue(), formatDate(), maskValue(), gridDefaults
 *   - updateTotalCount(), renderPagination(), exportExcel()
 *
 * formatter 호환:
 *   TUI Grid는 formatter에 `{value, row, column}` 객체를 전달하지만 화면 코드가 원시 값을
 *   직접 넘길 때도 있다. 이 모듈의 값 포매터는 두 호출 형태를 모두 허용한다.
 */
const TuiCommon = (() => {

    /** TUI formatter 인자면 value만 꺼내고, 일반 값이면 그대로 반환한다. */
    const rawValue = v => (v && typeof v === 'object' && 'value' in v) ? v.value : v;

    /* 의미론적 색상이 필요 없는 배지에 사용하는 값 해시 기반 팔레트다. */
    const BADGE_PALETTE = [
        'bg-primary', 'bg-success', 'bg-danger', 'bg-warning text-dark',
        'bg-info text-dark', 'bg-secondary'
    ];
    /* 값별 결과를 캐시해 반복 해시 계산을 피하고 색상 일관성을 유지한다. */
    const badgeColorCache = {};

    /**
     * 문자열 해시를 팔레트 index로 변환해 같은 표시값에 항상 같은 Bootstrap/CoreUI tone을 배정한다.
     * 이 색은 상태 의미를 표현하지 않는다. 성공/오류처럼 의미가 있는 값은 badgeByValue의 tones를 지정한다.
     */
    const badgeColorFor = (value) => {
        const key = String(value);
        if (!(key in badgeColorCache)) {
            let hash = 0;
            for (let i = 0; i < key.length; i++) {
                hash = ((hash << 5) - hash) + key.charCodeAt(i);
                hash |= 0;
            }
            badgeColorCache[key] = BADGE_PALETTE[Math.abs(hash) % BADGE_PALETTE.length];
        }
        return badgeColorCache[key];
    };

    /**
     * 날짜 값을 dayjs로 파싱해 지정 형식으로 표시한다.
     * @param {*} value 원시 날짜 또는 TUI formatter `{value}` 객체
     * @param {string} [pattern='YYYY-MM-DD'] dayjs 출력 패턴
     * @returns {string} 빈 값은 '-', 유효하지 않은 값은 원문 문자열
     */
    const formatDate = (value, pattern = 'YYYY-MM-DD') => {
        const val = rawValue(value);
        if (!val) return '-';
        const parsed = dayjs(val);
        return parsed.isValid() ? parsed.format(pattern) : String(val);
    };

    /**
     * 그리드 표시 단계에서 개인정보 일부를 `*`로 가린다. 원본 row 데이터는 변경하지 않는다.
     * @param {*} value 원시 값 또는 TUI formatter `{value}` 객체
     * @param {'PHONE'|'NAME'|'EMAIL'|'RRN'} type 마스킹 유형
     * @returns {string} 빈 값은 '-', 미지원 유형은 원문
     */
    const maskValue = (value, type) => {
        const val = rawValue(value);
        if (!val) return '-';
        const text = String(val);
        if (type === 'PHONE') return text.replace(/(\d{3})(\d+)(\d{4})/, function (_, a, b, c) { return a + '*'.repeat(b.length) + c; });
        if (type === 'NAME') return text.length <= 1 ? '*' : text[0] + '*'.repeat(text.length - 1);
        if (type === 'EMAIL') return text.replace(/^(.)(.*)(@.*)$/, function (_, a, b, c) { return a + '*'.repeat(b.length) + c; });
        if (type === 'RRN') return text.replace(/^(\d{6})[-]?(\d).*/, '$1-$2******');
        return text;
    };

    const fmt = {
        /* LocalDate는 날짜만, LocalDateTime은 분 단위까지 표시한다. */
        date: v => {
            const val = rawValue(v);
            const pattern = String(val || '').length > 10 ? 'YYYY-MM-DD HH:mm' : 'YYYY-MM-DD';
            return formatDate(val, pattern);
        },
    };

    /**
     * 코드별 label/tone 매핑을 캡처한 TUI Grid formatter를 만든다.
     * tones에 코드가 없으면 label 해시 기반 색상을 사용한다.
     *
     * @param {Object} options
     * @param {Object<string,string>} [options.labels={}] 코드 → 표시명
     * @param {Object<string,string>} [options.tones={}] 코드 → CSS class
     * @returns {Function} TUI Grid formatter 함수
     */
    const badgeByValue = ({ labels = {}, tones = {} } = {}) => ({ value }) => {
        const code = rawValue(value);
        if (!code) return '-';
        const label = labels[code] || String(code);
        const cls = tones[code] || badgeColorFor(label);
        return `<span class="badge ${cls}">${label}</span>`;
    };

    /* 목록 화면의 기준 옵션이며 화면별 예외는 builder의 gridOptions로 덮어쓴다. */
    const gridDefaults = {
        rowHeight: 38,
        bodyHeight: 380,
        minBodyHeight: 200,
        scrollX: true,
        scrollY: false,
    };

    /**
     * 총 건수 표시 요소를 현재 로케일의 숫자 문자열로 갱신한다.
     * v3 기본 selector는 screen-convention.md 계약의 `#total-count`다.
     */
    const updateTotalCount = (count, selector = '#total-count') => {
        const el = document.querySelector(selector);
        if (el) el.textContent = Number(count).toLocaleString();
    };

    /**
     * 10페이지 block 단위의 수동 pagination 버튼을 렌더링한다.
     *
     * 생성 HTML의 inline onclick에서 callback을 찾을 수 있도록 paginationId별 고유 함수를
     * window에 등록한다. 여러 그리드가 같은 화면에 있을 때는 서로 다른 paginationId를 사용해야 한다.
     *
     * @param {number} page 현재 1-based 페이지
     * @param {number} totalPages 전체 페이지 수
     * @param {Function} onMove 이동할 페이지 번호를 받는 callback
     * @param {string} [paginationId='pagination'] 버튼을 넣을 DOM id
     * @returns {void}
     */
    const renderPagination = (page, totalPages, onMove, paginationId = 'pagination') => {
        const wrap = document.getElementById(paginationId);
        if (!wrap || totalPages <= 0) {
            if (wrap) wrap.innerHTML = '';
            return;
        }

        const BLOCK = 10;
        const startPage = Math.floor((page - 1) / BLOCK) * BLOCK + 1;
        const endPage = Math.min(startPage + BLOCK - 1, totalPages);

        /* 하이픈을 underscore로 바꿔 inline onclick에서 호출 가능한 전역 함수명을 만든다. */
        const fnName = `__movePage_${paginationId.replace(/-/g, '_')}`;

        wrap.classList.add('d-flex', 'justify-content-center', 'gap-1');

        const btn = (label, p, disabled) =>
            `<button type="button" class="btn btn-sm btn-outline-secondary"
                     ${disabled ? 'disabled aria-disabled="true"' : `onclick="${fnName}(${p})"`}>${label}</button>`;

        let html = btn('«', 1, startPage === 1);
        html += btn('‹', startPage - 1, startPage === 1);
        for (let p = startPage; p <= endPage; p++) {
            html += `<button type="button" class="btn btn-sm ${p === page ? 'btn-primary' : 'btn-outline-secondary'}"
                             onclick="${fnName}(${p})">${p}</button>`;
        }
        html += btn('›', endPage + 1, endPage === totalPages);
        html += btn('»', totalPages, endPage === totalPages);

        wrap.innerHTML = html;
        window[fnName] = onMove;
    };

    /**
     * TUI Grid 내장 export API로 현재 grid 데이터를 xlsx로 저장한다.
     * 화면 권한 판정은 호출 버튼/TuiPageBuilder가 담당하며 이 함수는 전달받은 grid만 export한다.
     *
     * @param {Object|null} gridObj TUI Grid 인스턴스
     * @param {string} [fileName='download'] 확장자를 제외한 다운로드 파일명
     * @returns {void}
     */
    const exportExcel = (gridObj, fileName = 'download') => {
        if (!gridObj) return;
        gridObj.export('xlsx', { fileName: fileName });
    };

    return {
        fmt,
        badgeByValue,
        formatDate,
        maskValue,
        gridDefaults,
        updateTotalCount,
        renderPagination,
        exportExcel,
    };
})();
