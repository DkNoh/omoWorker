/**
 * list-form-modal.js
 * 목록 + 폼 모달 화면 정적 샘플 — TuiPageBuilder 목록 + ModalManager 폼 모달 패턴 시연
 *
 * samples/list-form-modal.html 전용이다. 목록 endpoint는 실제 존재하지 않으므로
 * file://·서버(http) 어느 쪽으로 열어도 mock 데이터로 그리드/총 건수/페이징이 렌더링된다.
 * 메인 그리드 행 클릭 시 선택 행을 폼에 바인딩(FormBinder)해 수정 모달을 열고,
 * 등록 버튼 클릭 시 빈 폼(등록 모드) 모달을 연다.
 * 실제 화면으로 전환할 때:
 *   1. API 주소를 실제 목록 엔드포인트로 교체한다.
 *   2. MOCK_ROWS 와 데모용 스텁(stubListApiForDemo, injectMockData)을 제거한다.
 *   3. save/remove 의 샘플 토스트 처리를 실제 저장/삭제 API 호출로 교체한다.
 */
(function () {
    'use strict';

    // list-form-modal.html이 만드는 모달 DOM id — fragments/modal-base.html 규약과 일치한다.
    // ModalManager는 이 값을 기준으로 모달과 저장/삭제 버튼을 찾는다.
    const MODAL_ID = 'sms-history-form-modal';

    // 폼 필드의 name 은 mock 행(=서버 응답)의 데이터 키와 1:1 대응한다 (FormBinder 계약).
    const FORM_SELECTOR = '#' + MODAL_ID + '-form';

    const API = {
        data: '/system/list-form-modal/data'
    };

    const SEND_TYPE_BADGES = {
        labels: { SMS: 'SMS', LMS: 'LMS', MMS: 'MMS', ALIMTALK: '알림톡' },
        tones: { SMS: 'bg-primary', LMS: 'bg-info text-dark', MMS: 'bg-secondary', ALIMTALK: 'bg-success' }
    };

    const SEND_STATUS_BADGES = {
        labels: { READY: '발송대기', SENT: '발송중', SUCCESS: '성공', FAIL: '실패', CANCEL: '취소' },
        tones: { READY: 'bg-secondary', SENT: 'bg-info text-dark', SUCCESS: 'bg-success', FAIL: 'bg-danger', CANCEL: 'bg-warning text-dark' }
    };

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
    // sendDttm 은 datetime-local 입력값 형식(YYYY-MM-DDTHH:mm:ss)으로 보관해
    // 그리드(TuiCommon.fmt.date)와 폼 바인딩(FormBinder)에 그대로 사용한다.
    const MOCK_ROWS = [
        { rowNo: 1, smsHistoryId: 'H-1001', sendDttm: '2026-07-28T09:12:05', receiverNo: '010-1234-5678', sendType: 'SMS',      sendStatus: 'SUCCESS', msgCont: '[샘플상사] 주문이 완료되었습니다. 주문번호 20260728-1001', regNm: '김민준' },
        { rowNo: 2, smsHistoryId: 'H-1002', sendDttm: '2026-07-28T09:05:41', receiverNo: '010-2345-6789', sendType: 'LMS',      sendStatus: 'SUCCESS', msgCont: '[샘플상사] 결제금액 15,000원이 정상 결제되었습니다. 일시불', regNm: '이서연' },
        { rowNo: 3, smsHistoryId: 'H-1003', sendDttm: '2026-07-28T08:47:19', receiverNo: '010-3456-7890', sendType: 'ALIMTALK', sendStatus: 'SENT',    msgCont: '[샘플상사] 배송이 시작되었습니다. 운송장번호 1234-5678-9012', regNm: '박지호' },
        { rowNo: 4, smsHistoryId: 'H-1004', sendDttm: '2026-07-27T18:20:33', receiverNo: '010-4567-8901', sendType: 'SMS',      sendStatus: 'FAIL',    msgCont: '[샘플상사] 인증번호는 8842입니다. 정확히 입력해 주세요.', regNm: '김민준' },
        { rowNo: 5, smsHistoryId: 'H-1005', sendDttm: '2026-07-27T15:02:58', receiverNo: '010-5678-9012', sendType: 'MMS',      sendStatus: 'SUCCESS', msgCont: '[샘플상사] 7월 29일 오후 2시 예약이 확정되었습니다. 방문 전 확인 바랍니다.', regNm: '최하은' },
        { rowNo: 6, smsHistoryId: 'H-1006', sendDttm: '2026-07-27T11:36:12', receiverNo: '010-6789-0123', sendType: 'SMS',      sendStatus: 'CANCEL',  msgCont: '[샘플상사] 회원님의 포인트(3,200P)가 7일 후 소멸됩니다.', regNm: '박지호' },
        { rowNo: 7, smsHistoryId: 'H-1007', sendDttm: '2026-07-26T14:55:47', receiverNo: '010-7890-1234', sendType: 'ALIMTALK', sendStatus: 'SUCCESS', msgCont: '[샘플상사] 문의하신 내용이 접수되었습니다. 24시간 내 답변드리겠습니다.', regNm: '이서연' },
        { rowNo: 8, smsHistoryId: 'H-1008', sendDttm: '2026-07-26T10:08:29', receiverNo: '010-8901-2345', sendType: 'LMS',      sendStatus: 'READY',   msgCont: '[샘플상사] 신규 이벤트가 시작되었습니다. 앱에서 혜택을 확인하세요.', regNm: '최하은' }
    ];

    // 등록 모드 기본값 — 등록자는 세션 사용자(readonly), 유형/상태는 첫 옵션으로 초기화한다.
    // SESSION_INFO는 defaultLayout(샘플은 head 스텁)이 const로 선언하므로 typeof 로만 확인한다.
    const DEFAULT_FORM = {
        sendType: 'SMS',
        sendStatus: 'READY',
        regNm: (typeof SESSION_INFO !== 'undefined' && SESSION_INFO.empNm) || ''
    };

    // 하나의 모달을 등록과 수정에 같이 사용하므로 현재 모드를 화면 상태로 보관한다.
    // selectedRow는 수정 모드에서 사용자가 선택한 그리드 행 원본을 보관한다.
    const state = {
        mode: 'create',
        selectedRow: null
    };

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
            columns: COLUMNS
        });

        // file:// 에서는 생성자 직후 동기적으로 그리드를 채워 즉시 렌더링한다.
        // 서버(http)로 연 경우는 stub 응답을 받은 TuiPageBuilder가 비동기로 그리드를 채운다.
        if (window.location.protocol === 'file:') {
            injectMockData(pageBuilder);
        }

        bindGridRowClick();
        bindButtons();

        // 모달 lifecycle과 기본 footer 버튼을 화면 함수에 연결한다.
        // sms-history-form-modal-btn-save 클릭 -> save(), sms-history-form-modal-btn-delete 클릭 -> remove().
        ModalManager.init(MODAL_ID, {
            onSubmit: save,
            onDelete: remove
        });

        CommonUtils.refreshIcons();
    }

    // ── 모달 열기 ───────────────────────────────────────────────────────
    // TuiPageBuilder가 생성한 실제 tui.Grid 인스턴스에 화면 전용 행 클릭 동작을 추가한다.
    function bindGridRowClick() {
        const grid = pageBuilder.getGrid();
        grid.on('click', ev => {
            // 컬럼 헤더 등 데이터 행이 아닌 영역을 클릭하면 rowKey가 없으므로 모달을 열지 않는다.
            if (ev.rowKey == null) return;

            // 클릭한 행은 이미 그리드에 적재된 데이터다. 별도 상세조회 API를 호출하지 않는다.
            openEdit(grid.getRow(ev.rowKey));
        });
    }

    // 서버가 defaultLayout에 주입한 PAGE_AUTH와 현재 모드를 함께 검사한다.
    // 등록에는 create 권한, 수정에는 update 권한이 정확히 true여야 저장할 수 있다.
    function canSave() {
        const auth = window.PAGE_AUTH || {};
        return (state.mode === 'create' && auth.create === true)
            || (state.mode === 'update' && auth.update === true);
    }

    // 모달을 열기 직전에 모드와 페이지 권한에 맞춰 저장/삭제 버튼의 d-none 클래스를 동기화한다.
    // 실제 API 접근 권한은 서버에서도 검증되며, 이 처리는 가능한 동작만 보여 주기 위한 UI 제어다.
    function syncActionButtons() {
        const auth = window.PAGE_AUTH || {};
        const modal = document.getElementById(MODAL_ID);
        const saveBtn = modal.querySelector('#' + MODAL_ID + '-btn-save');
        const deleteBtn = modal.querySelector('#' + MODAL_ID + '-btn-delete');
        if (saveBtn) saveBtn.classList.toggle('d-none', !canSave());
        if (deleteBtn) deleteBtn.classList.toggle('d-none', state.mode !== 'update' || auth.delete !== true);
    }

    // 그리드 행 클릭으로 진입하는 수정 모드 처리.
    function openEdit(row) {
        state.mode = 'update';
        state.selectedRow = row;

        document.getElementById(MODAL_ID + '-title').textContent = '발송 이력 수정';

        // 행 데이터를 폼에 바인딩한다. name 이 일치하는 필드만 채워진다.
        FormBinder.bind(FORM_SELECTOR, row);
        syncActionButtons();

        // 폼 값과 버튼 상태를 모두 준비한 뒤 모달을 연다.
        ModalManager.open(MODAL_ID);
    }

    // 그리드 카드 헤더의 등록 버튼으로 진입하는 신규 등록 모드 처리.
    function openCreate() {
        state.mode = 'create';
        state.selectedRow = null;

        document.getElementById(MODAL_ID + '-title').textContent = '발송 이력 등록';

        // 이전 행 데이터가 남지 않도록 폼을 비우고 등록 모드 기본값만 채운다.
        const form = document.querySelector(FORM_SELECTOR);
        form.reset();
        FormBinder.bind(FORM_SELECTOR, DEFAULT_FORM);
        syncActionButtons();
        ModalManager.open(MODAL_ID);

        const firstInput = form.querySelector('input:not([type="hidden"])');
        if (firstInput) firstInput.focus();
    }

    // ── 저장 / 삭제 ─────────────────────────────────────────────────────
    // modal-base의 저장 버튼 클릭 시 ModalManager의 onSubmit 훅을 통해 호출된다.
    // 샘플: 토스트만 표시한다. 실제 화면에서는 저장 API 호출 후 목록을 다시 조회한다.
    function save() {
        if (!canSave()) return;

        Notify.toast(state.mode === 'create'
            ? '등록되었습니다. (샘플 데이터)'
            : '수정되었습니다. (샘플 데이터)', 'success');
        ModalManager.close(MODAL_ID);
    }

    // modal-base의 삭제 버튼 클릭 시 ModalManager의 onDelete 훅을 통해 호출된다.
    // 샘플: 확인 후 토스트만 표시한다. 실제 화면에서는 삭제 API 호출 후 목록을 다시 조회한다.
    function remove() {
        // 등록 중인 데이터는 아직 기본키가 확정되지 않았으므로 수정 모드에서만 삭제를 허용한다.
        if (state.mode !== 'update') return;

        Notify.confirm('선택한 데이터를 삭제하시겠습니까?', () => {
            Notify.toast('삭제되었습니다. (샘플 데이터)', 'success');
            state.selectedRow = null;
            ModalManager.close(MODAL_ID);
        });
    }

    // ── 버튼 바인딩 ─────────────────────────────────────────────────────
    function bindButtons() {
        const btnCreate = document.getElementById('btn-create');
        if (btnCreate) {
            btnCreate.addEventListener('click', openCreate);
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
})();
