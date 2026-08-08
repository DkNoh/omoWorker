/**
 * list-basic.js
 * 기본 목록 화면 정적 샘플 — TuiPageBuilder 목록 패턴 시연
 *
 * samples/list-basic.html 전용이다. 목록 endpoint는 실제 존재하지 않으므로
 * file://·서버(http) 어느 쪽으로 열어도 mock 데이터로 그리드/총 건수/페이징이 렌더링된다.
 * 실제 화면으로 전환할 때:
 *   1. API 주소를 실제 목록/엑셀 엔드포인트로 교체한다.
 *   2. MOCK_ROWS 와 데모용 스텁(stubListApiForDemo, injectMockData)을 제거한다.
 *      (TuiPageBuilder가 목록 조회/페이징/버튼 이벤트를 전부 담당한다.)
 */
(function () {
    'use strict';

    const API = {
        data: '/system/list-basic/data',
        excel: '/system/list-basic/excel'
    };

    const SEND_TYPE_BADGES = {
        labels: { SMS: 'SMS', LMS: 'LMS', MMS: 'MMS', ALIMTALK: '알림톡' },
        tones: { SMS: 'bg-primary', LMS: 'bg-info text-dark', MMS: 'bg-secondary', ALIMTALK: 'bg-success' }
    };

    const SEND_STATUS_BADGES = {
        labels: { READY: '발송대기', SENT: '발송중', SUCCESS: '성공', FAIL: '실패', CANCEL: '취소' },
        tones: { READY: 'bg-secondary', SENT: 'bg-info text-dark', SUCCESS: 'bg-success', FAIL: 'bg-danger', CANCEL: 'bg-warning text-dark' }
    };

    // 정적 샘플용 mock 데이터 — 실제 화면에서는 서버 PageResponseDTO가 대체한다.
    const MOCK_ROWS = [
        { rowNo: 1, sendDttm: '2026-07-28 09:12:05', receiverNo: '010-1234-5678', sendType: 'SMS',      sendStatus: 'SUCCESS', regNm: '김민준' },
        { rowNo: 2, sendDttm: '2026-07-28 09:05:41', receiverNo: '010-2345-6789', sendType: 'LMS',      sendStatus: 'SUCCESS', regNm: '이서연' },
        { rowNo: 3, sendDttm: '2026-07-28 08:47:19', receiverNo: '010-3456-7890', sendType: 'ALIMTALK', sendStatus: 'SENT',    regNm: '박지호' },
        { rowNo: 4, sendDttm: '2026-07-27 18:20:33', receiverNo: '010-4567-8901', sendType: 'SMS',      sendStatus: 'FAIL',    regNm: '김민준' },
        { rowNo: 5, sendDttm: '2026-07-27 15:02:58', receiverNo: '010-5678-9012', sendType: 'MMS',      sendStatus: 'SUCCESS', regNm: '최하은' },
        { rowNo: 6, sendDttm: '2026-07-27 11:36:12', receiverNo: '010-6789-0123', sendType: 'SMS',      sendStatus: 'CANCEL',  regNm: '박지호' },
        { rowNo: 7, sendDttm: '2026-07-26 14:55:47', receiverNo: '010-7890-1234', sendType: 'ALIMTALK', sendStatus: 'SUCCESS', regNm: '이서연' },
        { rowNo: 8, sendDttm: '2026-07-26 10:08:29', receiverNo: '010-8901-2345', sendType: 'LMS',      sendStatus: 'READY',   regNm: '최하은' }
    ];

    let pageBuilder = null;

    document.addEventListener('DOMContentLoaded', init);

    function init() {
        // 샘플의 목록 endpoint는 실제 존재하지 않는다. file://·서버(http) 어느 쪽으로
        // 열어도 존재하지 않는 API 호출로 전역 인터셉터의 오류 모달이 뜨지 않도록
        // 프로토콜과 무관하게 mock 응답으로 대체한다. TuiPageBuilder의 조회/페이징/버튼
        // 흐름은 실제 코드 경로 그대로 사용된다.
        stubListApiForDemo();

        pageBuilder = new TuiPageBuilder({
            el: 'grid',
            apiUrl: API.data,
            searchInputs: ['startDt', 'endDt', 'sendType', 'receiverNo'],
            searchDefaults: { startDt: 'THIS_MONTH', endDt: 'TODAY' },
            rowHeaders: ['rowNum'],
            columns: [
                { header: '발송일시', name: 'sendDttm', align: 'center', width: 160, formatter: TuiCommon.fmt.date },
                { header: '수신번호', name: 'receiverNo', align: 'center', width: 140 },
                { header: '메시지유형', name: 'sendType', align: 'center', width: 110,
                    formatter: TuiCommon.badgeByValue(SEND_TYPE_BADGES) },
                { header: '발송상태', name: 'sendStatus', align: 'center', width: 110,
                    formatter: TuiCommon.badgeByValue(SEND_STATUS_BADGES) },
                { header: '등록자', name: 'regNm', align: 'center', width: 100 }
            ]
        });

        // file:// 에서는 생성자 직후 동기적으로 그리드를 채워 즉시 렌더링한다.
        // 서버(http)로 연 경우는 stub 응답을 받은 TuiPageBuilder가 비동기로 그리드를 채운다.
        if (window.location.protocol === 'file:') {
            injectMockData(pageBuilder);
        }

        bindExcelButton();
        CommonUtils.refreshIcons();
    }

    /**
     * file:// 데모용 스텁 — 목록 API를 mock 응답으로 대체한다.
     * 서버가 없는 상태에서 TuiPageBuilder의 초기 자동 조회와 조회/초기화 버튼이
     * 실패 요청 대신 mock 페이지를 받아 정상 흐름 그대로 렌더링된다.
     * (실패 응답이 전역 axios 인터셉터의 오류 모달로 이어지는 것도 방지한다.)
     */
    function stubListApiForDemo() {
        axios.get = function () {
            return Promise.resolve({ data: buildMockPage() });
        };
    }

    /** TuiPageBuilder가 기대하는 PageResponseDTO 형태와 맞춘다. */
    function buildMockPage() {
        return {
            page: 1,
            size: 10,
            totalCount: MOCK_ROWS.length,
            totalPages: 1,
            contents: MOCK_ROWS
        };
    }

    /**
     * 그리드에 mock 데이터를 직접 주입하고 총 건수/페이징/빈 데이터 상태를 동기화한다.
     * 생성자 직후 동기적으로 실행되어 file:// 에서도 그리드가 즉시 채워진 상태로 열린다.
     */
    function injectMockData(builder) {
        const pageSize = builder.currentSize || 10;
        const totalCount = MOCK_ROWS.length;
        const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));

        builder.getGrid().resetData(MOCK_ROWS);
        TuiCommon.updateTotalCount(totalCount);
        TuiCommon.renderPagination(1, totalPages, function onMove(page) {
            // 정적 샘플은 서버 페이지가 없으므로 요청 페이지만 다시 그린다.
            TuiCommon.renderPagination(page, totalPages, onMove);
        });

        const empty = document.querySelector('.toast-grid-shell [data-empty-state]');
        if (empty) {
            empty.classList.remove('is-visible');
        }
    }

    function bindExcelButton() {
        const btnExcel = document.getElementById('btn-excel');
        if (!btnExcel) {
            return;
        }

        btnExcel.addEventListener('click', function () {
            if (!window.PAGE_AUTH || window.PAGE_AUTH.download !== true) {
                CommonUtils.toast('엑셀 다운로드 권한이 없습니다.', 'warning');
                return;
            }

            // 실제 화면에서는 현재 검색 조건을 쿼리스트링으로 내려준다.
            // const params = new URLSearchParams(pageBuilder.getSearchParams());
            // window.location.href = API.excel + '?' + params.toString();
            Notify.toast('엑셀 다운로드를 시작합니다. (정적 샘플)', 'success');
        });
    }
})();
