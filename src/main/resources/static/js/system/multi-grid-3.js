// 다중 그리드(3 그리드) 정적 샘플 — 발송 현황 모니터링 (대기 / 성공 / 실패)
//
// 화면 동작 요약
// 1. DOMContentLoaded 후 TuiPageBuilder 인스턴스 3개가 각자 고유 ID 로 그리드를 초기화한다.
//    - 대기:  readyGrid   / readyPagination   / #readyTotalCount   / readyPageSize
//    - 성공:  successGrid / successPagination / #successTotalCount / successPageSize
//    - 실패:  failGrid    / failPagination    / #failTotalCount    / failPageSize
// 2. 세 그리드가 서로 다른 endpoint(상태별 필터)를 조회하고 독립적으로 페이징된다.
//
// 이 샘플의 endpoint 는 실제 존재하지 않는다. file://·서버(http) 어느 쪽으로 열어도
// 오류 모달이 뜨지 않도록 axios.get 을 URL 의 상태값으로 분기하는 mock 서버로 항상 대체한다.
// 조회/페이징 흐름은 TuiPageBuilder 의 실제 코드 경로를 그대로 탄다.
// 실제 화면 전환 시 MOCK_SEND 와 mockListResponse 를 실제 API 호출로 교체한다.
(function () {
    'use strict';

    const API = {
        ready: '/sms/monitor/ready/data',
        success: '/sms/monitor/success/data',
        fail: '/sms/monitor/fail/data'
    };

    // 상태별로 섞여 있는 발송 mock 데이터 — 각 그리드가 자기 상태만 필터링해 조회한다.
    const MOCK_SEND = [
        { sendDttm: '2026-07-28 10:05:12', receiverNo: '010-1234-5678', sendType: 'SMS',      sendStatus: 'READY' },
        { sendDttm: '2026-07-28 10:02:47', receiverNo: '010-2345-6789', sendType: 'ALIMTALK', sendStatus: 'READY' },
        { sendDttm: '2026-07-28 09:58:33', receiverNo: '010-3456-7890', sendType: 'LMS',      sendStatus: 'READY' },
        { sendDttm: '2026-07-28 09:55:08', receiverNo: '010-4567-8901', sendType: 'SMS',      sendStatus: 'SUCCESS' },
        { sendDttm: '2026-07-28 09:51:26', receiverNo: '010-5678-9012', sendType: 'SMS',      sendStatus: 'SUCCESS' },
        { sendDttm: '2026-07-28 09:47:54', receiverNo: '010-6789-0123', sendType: 'ALIMTALK', sendStatus: 'SUCCESS' },
        { sendDttm: '2026-07-28 09:44:19', receiverNo: '010-7890-1234', sendType: 'MMS',      sendStatus: 'SUCCESS' },
        { sendDttm: '2026-07-28 09:40:31', receiverNo: '010-8901-2345', sendType: 'SMS',      sendStatus: 'SUCCESS' },
        { sendDttm: '2026-07-28 09:36:02', receiverNo: '010-9012-3456', sendType: 'LMS',      sendStatus: 'SUCCESS' },
        { sendDttm: '2026-07-28 09:30:45', receiverNo: '010-1122-3344', sendType: 'SMS',      sendStatus: 'FAIL' },
        { sendDttm: '2026-07-28 09:25:18', receiverNo: '010-2233-4455', sendType: 'ALIMTALK', sendStatus: 'FAIL' },
        { sendDttm: '2026-07-28 09:18:09', receiverNo: '010-3344-5566', sendType: 'SMS',      sendStatus: 'FAIL' }
    ];

    const SEND_TYPE_BADGES = {
        labels: { SMS: 'SMS', LMS: 'LMS', MMS: 'MMS', ALIMTALK: '알림톡' },
        tones: { SMS: 'bg-primary', LMS: 'bg-info text-dark', MMS: 'bg-secondary', ALIMTALK: 'bg-success' }
    };

    document.addEventListener('DOMContentLoaded', init);

    // URL 에 포함된 상태값(ready/success/fail)으로 필터링해 PageResponseDTO 로 응답하는 mock 서버.
    function mockListResponse(url, config) {
        const params = (config && config.params) || new URLSearchParams();
        const page = parseInt(params.get('page'), 10) || 1;
        const size = parseInt(params.get('size'), 10) || 10;

        const status = statusFromUrl(url);
        const source = MOCK_SEND.filter(row => row.sendStatus === status);

        const totalPages = Math.max(1, Math.ceil(source.length / size));
        const safePage = Math.min(page, totalPages);
        const start = (safePage - 1) * size;
        const contents = source.slice(start, start + size);

        return { data: { contents, page: safePage, size, totalCount: source.length, totalPages } };
    }

    function statusFromUrl(url) {
        if (url.indexOf('/ready/') !== -1) return 'READY';
        if (url.indexOf('/success/') !== -1) return 'SUCCESS';
        if (url.indexOf('/fail/') !== -1) return 'FAIL';
        return '';
    }

    function init() {
        // endpoint 는 실제 존재하지 않으므로 프로토콜과 무관하게 mock 서버로 대체한다.
        axios.get = (url, config) => Promise.resolve(mockListResponse(url, config));

        // 세 그리드가 공유하는 컬럼 정의 — 화면에 따라 다르게 구성해도 된다.
        const columns = [
            { header: '발송일시', name: 'sendDttm', align: 'center', width: 150, formatter: TuiCommon.fmt.date },
            { header: '수신번호', name: 'receiverNo', align: 'center', width: 130 },
            { header: '유형', name: 'sendType', align: 'center', width: 80,
                formatter: TuiCommon.badgeByValue(SEND_TYPE_BADGES) }
        ];

        // ── 그리드 1: 발송대기 ──────────────────────────────────────────
        new TuiPageBuilder({
            el: 'readyGrid',
            apiUrl: API.ready,
            paginationId: 'readyPagination',
            totalCountSelector: '#readyTotalCount',
            pageSizeEl: 'readyPageSize',
            searchInputs: [],
            rowHeaders: ['rowNum'],
            columns: columns
        });

        // ── 그리드 2: 발송성공 ──────────────────────────────────────────
        new TuiPageBuilder({
            el: 'successGrid',
            apiUrl: API.success,
            paginationId: 'successPagination',
            totalCountSelector: '#successTotalCount',
            pageSizeEl: 'successPageSize',
            searchInputs: [],
            rowHeaders: ['rowNum'],
            columns: columns
        });

        // ── 그리드 3: 발송실패 ──────────────────────────────────────────
        new TuiPageBuilder({
            el: 'failGrid',
            apiUrl: API.fail,
            paginationId: 'failPagination',
            totalCountSelector: '#failTotalCount',
            pageSizeEl: 'failPageSize',
            searchInputs: [],
            rowHeaders: ['rowNum'],
            columns: columns
        });

        CommonUtils.refreshIcons();
    }
})();
