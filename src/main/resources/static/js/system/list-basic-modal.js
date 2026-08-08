/**
 * list-basic-modal.js
 * 목록 + 모달 팝업 화면 정적 샘플 — TuiPageBuilder 목록 + 모달 안에 조회조건/그리드/페이징 패턴 시연
 *
 * samples/list-basic-modal.html 전용이다. 목록 endpoint는 실제 존재하지 않으므로
 * file://·서버(http) 어느 쪽으로 열어도 mock 데이터로 그리드/총 건수/페이징이 렌더링된다.
 * 메인 그리드 행 클릭 시 해당 행의 수신번호로 미리 조회된 모달(드릴다운)을 열고,
 * 이력 목록 버튼 클릭 시 전체 목록으로 모달을 연다.
 * 모달 그리드는 MOCK_ROWS 를 클라이언트에서 직접 검색/페이징하는 독립 목록이다.
 * 실제 화면으로 전환할 때:
 *   1. API 주소를 실제 목록/엑셀 엔드포인트로 교체한다.
 *   2. MOCK_ROWS 와 데모용 스텁(stubListApiForDemo, injectMockData)을 제거한다.
 *   3. 모달의 클라이언트 검색을 실제 상세조회 API 호출로 교체한다.
 */
(function () {
    'use strict';

    // list-basic-modal.html이 만드는 모달 DOM id — fragments/modal-base.html 규약과 일치한다.
    const MODAL_ID = 'sms-history-modal';

    // 모달 그리드는 서버 호출 없이 MOCK_ROWS 를 잘라서 보여주는 클라이언트 목록이다.
    const MODAL_PAGE_SIZE = 5;

    const API = {
        data: '/system/list-basic-modal/data',
        excel: '/system/list-basic-modal/excel'
    };

    const SEND_TYPE_BADGES = {
        labels: { SMS: 'SMS', LMS: 'LMS', MMS: 'MMS', ALIMTALK: '알림톡' },
        tones: { SMS: 'bg-primary', LMS: 'bg-info text-dark', MMS: 'bg-secondary', ALIMTALK: 'bg-success' }
    };

    const SEND_STATUS_BADGES = {
        labels: { READY: '발송대기', SENT: '발송중', SUCCESS: '성공', FAIL: '실패', CANCEL: '취소' },
        tones: { READY: 'bg-secondary', SENT: 'bg-info text-dark', SUCCESS: 'bg-success', FAIL: 'bg-danger', CANCEL: 'bg-warning text-dark' }
    };

    // 메인 그리드와 모달 그리드가 공통으로 사용하는 컬럼 정의다.
    const COLUMNS = [
        { header: '발송일시', name: 'sendDttm', align: 'center', width: 160, formatter: TuiCommon.fmt.date },
        { header: '수신번호', name: 'receiverNo', align: 'center', width: 140 },
        { header: '메시지유형', name: 'sendType', align: 'center', width: 110,
            formatter: TuiCommon.badgeByValue(SEND_TYPE_BADGES) },
        { header: '발송상태', name: 'sendStatus', align: 'center', width: 110,
            formatter: TuiCommon.badgeByValue(SEND_STATUS_BADGES) },
        { header: '등록자', name: 'regNm', align: 'center', width: 100 }
    ];

    // 정적 샘플용 mock 데이터 — 실제 화면에서는 서버 PageResponseDTO가 대체한다.
    // 수신번호 4개에 15건으로 나눠 모달 드릴다운과 페이징(5건/페이지)이 보이게 한다.
    const MOCK_ROWS = [
        { rowNo: 1,  sendDttm: '2026-07-28 09:12:05', receiverNo: '010-1234-5678', sendType: 'SMS',      sendStatus: 'SUCCESS', regNm: '김민준' },
        { rowNo: 2,  sendDttm: '2026-07-28 09:05:41', receiverNo: '010-2345-6789', sendType: 'LMS',      sendStatus: 'SUCCESS', regNm: '이서연' },
        { rowNo: 3,  sendDttm: '2026-07-28 08:47:19', receiverNo: '010-3456-7890', sendType: 'ALIMTALK', sendStatus: 'SENT',    regNm: '박지호' },
        { rowNo: 4,  sendDttm: '2026-07-28 08:30:02', receiverNo: '010-1234-5678', sendType: 'LMS',      sendStatus: 'SUCCESS', regNm: '김민준' },
        { rowNo: 5,  sendDttm: '2026-07-27 18:20:33', receiverNo: '010-4567-8901', sendType: 'SMS',      sendStatus: 'FAIL',    regNm: '김민준' },
        { rowNo: 6,  sendDttm: '2026-07-27 17:41:50', receiverNo: '010-1234-5678', sendType: 'ALIMTALK', sendStatus: 'SUCCESS', regNm: '최하은' },
        { rowNo: 7,  sendDttm: '2026-07-27 15:02:58', receiverNo: '010-2345-6789', sendType: 'MMS',      sendStatus: 'SUCCESS', regNm: '최하은' },
        { rowNo: 8,  sendDttm: '2026-07-27 11:36:12', receiverNo: '010-1234-5678', sendType: 'SMS',      sendStatus: 'CANCEL',  regNm: '박지호' },
        { rowNo: 9,  sendDttm: '2026-07-26 16:24:08', receiverNo: '010-3456-7890', sendType: 'SMS',      sendStatus: 'SUCCESS', regNm: '이서연' },
        { rowNo: 10, sendDttm: '2026-07-26 14:55:47', receiverNo: '010-2345-6789', sendType: 'ALIMTALK', sendStatus: 'SUCCESS', regNm: '이서연' },
        { rowNo: 11, sendDttm: '2026-07-26 10:08:29', receiverNo: '010-1234-5678', sendType: 'MMS',      sendStatus: 'READY',   regNm: '최하은' },
        { rowNo: 12, sendDttm: '2026-07-25 19:33:44', receiverNo: '010-4567-8901', sendType: 'LMS',      sendStatus: 'SUCCESS', regNm: '박지호' },
        { rowNo: 13, sendDttm: '2026-07-25 13:17:21', receiverNo: '010-3456-7890', sendType: 'MMS',      sendStatus: 'FAIL',    regNm: '김민준' },
        { rowNo: 14, sendDttm: '2026-07-25 09:48:36', receiverNo: '010-1234-5678', sendType: 'SMS',      sendStatus: 'SUCCESS', regNm: '이서연' },
        { rowNo: 15, sendDttm: '2026-07-24 15:55:12', receiverNo: '010-2345-6789', sendType: 'SMS',      sendStatus: 'SENT',    regNm: '박지호' }
    ];

    // 모달 목록의 현재 조회 상태 — filtered 는 검색 조건을 통과한 MOCK_ROWS 전체다.
    const modalState = {
        page: 1,
        filtered: []
    };

    let pageBuilder = null;
    let modalGrid = null;

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
            columns: COLUMNS
        });

        // file:// 에서는 생성자 직후 동기적으로 그리드를 채워 즉시 렌더링한다.
        // 서버(http)로 연 경우는 stub 응답을 받은 TuiPageBuilder가 비동기로 그리드를 채운다.
        if (window.location.protocol === 'file:') {
            injectMockData(pageBuilder);
        }

        initModalGrid();
        bindGridRowClick();
        bindButtons();

        // 이 모달은 조회 전용(footer 닫기만 존재)이므로 onSubmit/onDelete 훅을 넘기지 않는다.
        ModalManager.init(MODAL_ID, {
            // 숨겨진 상태에서 생성된 그리드는 폭이 0이라 모달이 완전히 열린 뒤 다시 배치한다.
            onOpen: function () {
                if (modalGrid) modalGrid.refreshLayout();
            }
        });

        CommonUtils.refreshIcons();
    }

    // ── 모달 그리드 ─────────────────────────────────────────────────────
    // 모달 그리드는 MOCK_ROWS 를 클라이언트에서 검색/페이징하는 독립 목록이라
    // TuiPageBuilder 대신 tui.Grid를 직접 생성한다. 공통 옵션은
    // TuiCommon.gridDefaults를 재사용하고 bodyHeight만 HTML의 #modal-grid 높이(320px)에 맞춘다.
    function initModalGrid() {
        modalGrid = new tui.Grid(Object.assign({}, TuiCommon.gridDefaults, {
            el: document.getElementById('modal-grid'),
            bodyHeight: 320,
            rowHeaders: [],
            columns: COLUMNS
        }));
    }

    // 모달 그리드 셸의 빈 데이터 안내를 데이터 유무에 맞춰 토글한다.
    function syncModalEmpty(visible) {
        const shell = document.getElementById('modal-grid').closest('.toast-grid-shell');
        const empty = shell && shell.querySelector('[data-empty-state]');
        if (empty) {
            empty.classList.toggle('is-visible', visible);
        }
    }

    // ── 모달 검색 / 페이징 ──────────────────────────────────────────────
    function readModalCondition() {
        return {
            receiverNo: document.getElementById('modal-receiverNo').value.trim(),
            sendType: document.getElementById('modal-sendType').value
        };
    }

    // 수신번호는 숫자만 비교하므로 하이픈 유무와 무관하게 부분 일치한다.
    function filterMockRows(condition) {
        const digits = String(condition.receiverNo || '').replace(/\D/g, '');
        return MOCK_ROWS.filter(function (row) {
            const hitNo = !digits || row.receiverNo.replace(/\D/g, '').includes(digits);
            const hitType = !condition.sendType || row.sendType === condition.sendType;
            return hitNo && hitType;
        });
    }

    // 검색 조건을 적용하고 1페이지부터 다시 그린다.
    function applyModalSearch(condition) {
        modalState.page = 1;
        modalState.filtered = filterMockRows(condition);
        renderModalPage();
    }

    // 현재 페이지 조각을 모달 그리드에 적재하고 총 건수/페이징/빈 데이터 상태를 동기화한다.
    function renderModalPage() {
        const totalCount = modalState.filtered.length;
        const totalPages = Math.ceil(totalCount / MODAL_PAGE_SIZE);
        modalState.page = Math.min(modalState.page, Math.max(1, totalPages));

        const start = (modalState.page - 1) * MODAL_PAGE_SIZE;
        modalGrid.resetData(modalState.filtered.slice(start, start + MODAL_PAGE_SIZE));
        syncModalEmpty(totalCount === 0);

        document.getElementById('modal-total-count').textContent = totalCount.toLocaleString();

        // totalPages 가 0이면 renderPagination 이 컨테이너를 비운다.
        TuiCommon.renderPagination(modalState.page, totalPages, function onMove(page) {
            modalState.page = page;
            renderModalPage();
        }, 'modal-pagination');
    }

    // ── 모달 열기 ───────────────────────────────────────────────────────
    // receiverNo 를 넘기면 해당 수신번호로 미리 조회된 모달(드릴다운)을 열고,
    // null 이면 조건 없이 전체 목록으로 연다.
    function openModalList(receiverNo) {
        document.getElementById('modal-receiverNo').value = receiverNo || '';
        document.getElementById('modal-sendType').value = '';
        applyModalSearch(readModalCondition());
        ModalManager.open(MODAL_ID);
    }

    // TuiPageBuilder가 생성한 실제 tui.Grid 인스턴스에 화면 전용 행 클릭 동작을 추가한다.
    function bindGridRowClick() {
        const grid = pageBuilder.getGrid();
        grid.on('click', ev => {
            // 컬럼 헤더 등 데이터 행이 아닌 영역을 클릭하면 rowKey가 없으므로 모달을 열지 않는다.
            if (ev.rowKey == null) return;

            const row = grid.getRow(ev.rowKey);
            openModalList(row && row.receiverNo);
        });
    }

    // ── 버튼 바인딩 ─────────────────────────────────────────────────────
    function bindButtons() {
        const btnOpenModal = document.getElementById('btn-open-modal');
        if (btnOpenModal) {
            btnOpenModal.addEventListener('click', function () {
                openModalList(null);
            });
        }

        const btnExcel = document.getElementById('btn-excel');
        if (btnExcel) {
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

        const btnModalSearch = document.getElementById('modal-btn-search');
        if (btnModalSearch) {
            btnModalSearch.addEventListener('click', function () {
                applyModalSearch(readModalCondition());
            });
        }

        const btnModalReset = document.getElementById('modal-btn-reset');
        if (btnModalReset) {
            btnModalReset.addEventListener('click', function () {
                document.getElementById('modal-receiverNo').value = '';
                document.getElementById('modal-sendType').value = '';
                applyModalSearch(readModalCondition());
            });
        }

        // 수신번호 입력에서 Enter 로도 조회된다(메인 검색 카드와 동일한 사용성).
        const modalReceiverNo = document.getElementById('modal-receiverNo');
        if (modalReceiverNo) {
            modalReceiverNo.addEventListener('keydown', function (ev) {
                if (ev.key === 'Enter') {
                    ev.preventDefault();
                    applyModalSearch(readModalCondition());
                }
            });
        }
    }

    // ── 데모용 mock 스텁 ────────────────────────────────────────────────
    /**
     * file:// 데모용 스텁 — 목록 API를 mock 응답으로 대체한다.
     * 서버가 없는 상태에서 TuiPageBuilder의 초기 자동 조회와 조회/초기화 버튼이
     * 실패 요청 대신 mock 페이지를 받아 정상 흐름 그대로 렌더링된다.
     * (실패 응답이 전역 axios 인터셉터의 오류 모달로 이어지는 것도 방지한다.)
     */
    function stubListApiForDemo() {
        axios.get = function (url, config) {
            return Promise.resolve({ data: buildMockPage(config) });
        };
    }

    /** TuiPageBuilder가 기대하는 PageResponseDTO 형태와 맞춘다.
     *  실제 서버처럼 요청 page/size 만큼만 잘라서 내려준다.
     *  (contents 가 size 보다 크면 TUI Grid 가 자체 perPage 기준으로 행을 잘라
     *  totalCount 와 화면 행 수가 어긋난다.) */
    function buildMockPage(config) {
        const params = new URLSearchParams(config && config.params);
        const page = Number(params.get('page')) || 1;
        const size = Number(params.get('size')) || 10;
        const totalCount = MOCK_ROWS.length;
        const start = (page - 1) * size;

        return {
            page: page,
            size: size,
            totalCount: totalCount,
            totalPages: Math.max(1, Math.ceil(totalCount / size)),
            contents: MOCK_ROWS.slice(start, start + size)
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
})();
