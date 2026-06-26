/**
 * tui-common.js
 * TUI Grid 공통 유틸리티 모듈
 */
const TuiCommon = (() => {

    const rawValue = v => (v && typeof v === 'object' && 'value' in v) ? v.value : v;

    // 값 해시 기반 자동 색상 배지용 팔레트 (의미론적 색상이 필요 없을 때 사용)
    // 같은 값은 항상 같은 색을 보장한다.
    const BADGE_PALETTE = [
        'bg-primary', 'bg-success', 'bg-danger', 'bg-warning text-dark',
        'bg-info text-dark', 'bg-secondary'
    ];
    const badgeColorCache = {};
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

    const formatDate = (value, pattern = 'YYYY-MM-DD') => {
        const val = rawValue(value);
        if (!val) return '-';
        const parsed = dayjs(val);
        return parsed.isValid() ? parsed.format(pattern) : String(val);
    };

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
        // TUI Grid formatter({value})와 직접 호출(문자열) 양쪽을 지원한다
        // 날짜만(LocalDate)은 YYYY-MM-DD, 일시(LocalDateTime)는 YYYY-MM-DD HH:mm
        date: v => {
            const val = rawValue(v);
            const pattern = String(val || '').length > 10 ? 'YYYY-MM-DD HH:mm' : 'YYYY-MM-DD';
            return formatDate(val, pattern);
        },

        sendStatus: ({ value }) => {
            const map = {
                SUCCESS: ['bg-success', '성공'],
                FAIL: ['bg-danger', '실패'],
                WAIT: ['bg-warning text-dark', '대기'],
            };
            const clsAndLabel = map[value] || ['', value || '-'];
            const cls = clsAndLabel[0];
            const label = clsAndLabel[1];
            return `<span class="badge ${cls}">${label}</span>`;
        },

        sendType: ({ value }) => {
            const cls = { SMS: 'bg-primary', LMS: 'bg-info text-dark', ALIMTALK: 'bg-warning text-dark' };
            const label = { SMS: 'SMS', LMS: 'LMS', ALIMTALK: '알림톡' };
            return value
                ? `<span class="badge ${cls[value] || 'bg-secondary'}">${label[value] || value}</span>`
                : '-';
        },

        resendYn: ({ value }) =>
            value === 'Y'
                ? '<span class="badge bg-primary">Y</span>'
                : '<span class="badge bg-secondary">N</span>',

        // 값이 뭐가 오든 자동으로 색상을 다르게 뿌려주는 범용 배지 포매터.
        // 값(또는 라벨)을 해시해 팔레트에서 색을 고른다. 같은 값은 항상 같은 색.
        // 코드값→한글 라벨 매핑이 필요하면 TuiCommon.autoBadge({ APPR:'승인', ... }) 팩토리를 사용한다.
        autoBadge: ({ value }) => {
            const val = rawValue(value);
            if (!val) return '-';
            const label = String(val);
            return `<span class="badge ${badgeColorFor(label)}">${label}</span>`;
        },
    };

    // autoBadge의 라벨 매핑 버전 (팩토리).
    // 사용: formatter: TuiCommon.autoBadge({ APPR: '승인', REJ: '반려', REAPPR: '재승인' })
    // 색은 라벨(한글) 기준으로 자동 할당되므로, 승인/반려/재승인은 각각 다른 색이 보장된다(팔레트 범위 내).
    const autoBadge = (labels = {}) => ({ value }) => {
        const code = rawValue(value);
        if (!code) return '-';
        const label = labels[code] || String(code);
        return `<span class="badge ${badgeColorFor(label)}">${label}</span>`;
    };

    const gridDefaults = {
        rowHeight: 42,
        bodyHeight: 420,
        minBodyHeight: 300,
        scrollX: true,
        scrollY: true,
    };

    // v3 화면 골격 기준은 id="total-count"다 (screen-convention.md)
    const updateTotalCount = (count, selector = '#total-count') => {
        const el = document.querySelector(selector);
        if (el) el.textContent = Number(count).toLocaleString();
    };

    const renderPagination = (page, totalPages, onMove, paginationId = 'pagination') => {
        const wrap = document.getElementById(paginationId);
        if (!wrap || totalPages <= 0) {
            if (wrap) wrap.innerHTML = '';
            return;
        }

        const BLOCK = 10;
        const startPage = Math.floor((page - 1) / BLOCK) * BLOCK + 1;
        const endPage = Math.min(startPage + BLOCK - 1, totalPages);

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

    const exportExcel = (gridObj, fileName = 'download') => {
        if (!gridObj) return;
        gridObj.export('xlsx', { fileName: fileName });
    };

    return {
        fmt,
        autoBadge,
        formatDate,
        maskValue,
        gridDefaults,
        updateTotalCount,
        renderPagination,
        exportExcel,
    };
})();
