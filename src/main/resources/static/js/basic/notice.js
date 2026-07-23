// 수동 참조: scaffold 복사 후 커스터마이즈한 window.open CRUD 예제. 재생성하지 않고 직접 수정한다.
//
// 이 파일은 두 편집 경로를 단일 목록 화면에서 함께 다룬다(Todo 5 reconcile):
//   1) 행 클릭/Enter/Space → 간편 수정 모달(ModalManager + FormBinder + ApiClient).
//      상세 API 로 content/updDttm 을 온전히 채운 뒤 모달을 연다(낙관적 잠금 포함).
//   2) '게시판 팝업 예제' 버튼 → 선택된 행의 게시판 팝업(window.open).
//      미선택 시 warning 토스트.
//
// 팝업(자식 창)이 보내는 postMessage 는 v2 강화 규약을 그대로 보존한다:
//   - message 리스너는 1개
//   - ev.origin === window.location.origin (와일드카드 금지)
//   - ev.source 가 openedPopups 에 추적된 핸들(자식 창)인지 검사
//   - ev.data.action === 'noticeChanged' (v1 의 save-specific action 회귀 금지)
//   - operation 은 VALID_OPERATIONS Set 화이트리스트로 검증 (created/updated/deleted 만 허용)
//   - operation 별로 구분된 한글 성공 토스트
//   - 수락된 메시지 한 건당 정확히 1회 새로고침
document.addEventListener('DOMContentLoaded', function () {
    'use strict';

    const POPUP_URL = '/basic/notice/popup';
    const POPUP_NAME = 'noticeEditPopup';
    const POPUP_FEATURES = 'width=1100,height=850,resizable=yes,scrollbars=yes';

    const MODAL_ID = 'notice-modal';
    const FORM_SELECTOR = '#notice-modal-form';

    const API = {
        detail: '/basic/notice/detail',
        create: '/basic/notice/create',
        update: '/basic/notice/update',
        delete: '/basic/notice/delete'
    };

    const SUCCESS_MESSAGES = {
        created: '등록되었습니다.',
        updated: '수정되었습니다.',
        deleted: '삭제되었습니다.'
    };
    const VALID_OPERATIONS = new Set(['created', 'updated', 'deleted']);
    const openedPopups = new Set();

    // defaultLayout.html 이 주입하는 PAGE_AUTH. 누락 시 빈 객체 → 아래 === true 검사가 모두
    // false 로 폴백해 모든 쓰기를 차단한다(페일클로즈드).
    const AUTH = window.PAGE_AUTH || {};
    const canCreate = () => AUTH.create === true;
    const canUpdate = () => AUTH.update === true;
    const canDelete = () => AUTH.delete === true;

    const pageBuilder = new TuiPageBuilder({
        el: 'grid',
        apiUrl: '/basic/notice/data',
        searchInputs: ['startdt', 'enddt', 'useYn'],
        searchDefaults: {},
        btnCreate: 'notice-create-button-is-bound-manually',
        rowHeaders: ['rowNum'],
        columns: [
            { header: 'NOTICE_ID', name: 'noticeId', align: 'center', width: 120 },
            { header: 'TITLE', name: 'title', align: 'left', width: 360 },
            { header: 'NOTICE_TYPE', name: 'noticeType', align: 'center', width: 130 },
            { header: 'USE_YN', name: 'useYn', align: 'center', width: 100 },
            { header: 'START_DT', name: 'startDt', align: 'center', width: 150, formatter: TuiCommon.fmt.date },
            { header: 'END_DT', name: 'endDt', align: 'center', width: 150, formatter: TuiCommon.fmt.date },
            { header: 'VIEW_CNT', name: 'viewCnt', align: 'right', width: 110 }
        ]
    });

    let mode = 'create';
    let selectedRow = null;
    let selectedNoticeId = null;

    const refreshCurrentPage = () => pageBuilder.searchData(pageBuilder.currentPage || 1);

    // 모드 + 권한으로 저장/삭제 버튼 가시성을 동기화한다.
    // Thymeleaf th:if 로 인해 버튼이 아예 렌더링되지 않았을 수 있으므로 존재 여부를 먼저 검사한다.
    //   - 저장: (create 모드 && CREATE) 또는 (update 모드 && UPDATE)
    //   - 삭제: update 모드 && DELETE
    const syncActionButtons = () => {
        const saveBtn = document.getElementById(`${MODAL_ID}-btn-save`);
        if (saveBtn) {
            const showSave = (mode === 'create' && canCreate()) || (mode === 'update' && canUpdate());
            saveBtn.classList.toggle('d-none', !showSave);
        }
        const deleteBtn = document.getElementById(`${MODAL_ID}-btn-delete`);
        if (deleteBtn) {
            const showDelete = mode === 'update' && canDelete();
            deleteBtn.classList.toggle('d-none', !showDelete);
        }
    };

    const openPopup = (noticeId) => {
        const query = noticeId === null || noticeId === undefined
            ? ''
            : `?noticeId=${encodeURIComponent(noticeId)}`;
        const popup = window.open(`${POPUP_URL}${query}`, POPUP_NAME, POPUP_FEATURES);
        if (popup) {
            openedPopups.add(popup);
            popup.addEventListener('pagehide', () => openedPopups.delete(popup));
        }
        return popup;
    };

    const isKnownPopup = (source) => {
        openedPopups.forEach((win) => { if (win.closed) openedPopups.delete(win); });
        return source !== null && source !== undefined && openedPopups.has(source);
    };

    // 행 클릭 → 상세 조회로 content/updDttm 을 온전히 채운 뒤 모달을 연다.
    // 그리드 /data 행에는 content 가 없다. 상세 조회/바인딩/오픈 중 어느 하나라도 실패하면
    // 모달을 열지 않는다(HttpClient 인터셉터가 중앙 에러 다이얼로그를 표시한다).
    const openModalForUpdate = async (row) => {
        const noticeId = row ? row.noticeId : null;
        if (noticeId === null || noticeId === undefined) return;
        let detail;
        try {
            detail = await ApiClient.get(API.detail, { noticeId });
            if (!detail) {
                console.error('[notice] 상세 응답이 비어 모달을 열지 않습니다.', { noticeId });
                return;
            }
            selectedNoticeId = noticeId;
            selectedRow = detail;
            mode = 'update';
            // 낙관적 잠금: 상세 응답의 updDttm 을 beforeUpdDttm hidden 필드로 스냅샷한다.
            const formData = Object.assign({}, detail, { beforeUpdDttm: detail.updDttm });
            FormBinder.bind(FORM_SELECTOR, formData);
            syncActionButtons();
            ModalManager.open(MODAL_ID);
        } catch (err) {
            console.error('[notice] 상세 조회/바인딩/모달 열기 실패 — 모달을 열지 않습니다.', err);
        }
    };

    const openModalForCreate = () => {
        mode = 'create';
        selectedRow = null;
        selectedNoticeId = null;
        const formEl = document.querySelector(FORM_SELECTOR);
        if (formEl) formEl.reset();
        syncActionButtons();
        ModalManager.open(MODAL_ID);
    };

    const handleSave = async () => {
        try {
            const payload = FormBinder.toObject(FORM_SELECTOR);
            if (mode === 'create') {
                if (!canCreate()) return;
                await ApiClient.post(API.create, payload);
                CommonUtils.toast(SUCCESS_MESSAGES.created, 'success');
            } else {
                if (!canUpdate()) return;
                await ApiClient.post(API.update, payload);
                CommonUtils.toast(SUCCESS_MESSAGES.updated, 'success');
            }
            ModalManager.close(MODAL_ID);
            await refreshCurrentPage();
        } catch (err) {
            // 인터셉터가 중앙 에러 다이얼로그를 표시한다. 모달을 닫거나 성공 토스트/새로고침은 하지 않는다.
            console.error('[notice] 저장 실패 — 모달을 유지합니다.', err);
        }
    };

    const handleDelete = () => {
        if (!canDelete()) return;
        if (selectedNoticeId === null || selectedNoticeId === undefined) return;
        CommonUtils.confirm('선택한 공지사항을 삭제하시겠습니까?', async () => {
            try {
                await ApiClient.remove(API.delete, { noticeId: selectedNoticeId });
                CommonUtils.toast(SUCCESS_MESSAGES.deleted, 'success');
                ModalManager.close(MODAL_ID);
                await refreshCurrentPage();
            } catch (err) {
                // 인터셉터가 중앙 에러 다이얼로그를 표시한다. 닫기/성공 토스트/새로고침은 하지 않는다.
                console.error('[notice] 삭제 실패 — 모달을 유지합니다.', err);
            }
        });
    };

    ModalManager.init(MODAL_ID, {
        onSubmit: handleSave,
        onDelete: handleDelete
    });

    // 행 click → 간편 모달(수정). 상세 API 로 content/updDttm 을 온전히 채운다.
    // dblclick 은 사용하지 않는다 — 팝업 예제 버튼이 선택 행 팝업을 담당한다.
    // openModalForUpdate 는 내부에서 catch 한다.
    const grid = pageBuilder.getGrid();
    grid.on('click', (ev) => {
        if (ev.rowKey === null || ev.rowKey === undefined) return;
        const row = grid.getRow(ev.rowKey);
        if (row) void openModalForUpdate(row).catch(() => {});
    });

    // 키보드 활성화: tui-grid 의 합성 keydown 리스너는 native KeyboardEvent.key 를
    // 노출하지 않아 실제 브라우저에서 항상 죽는다. 대신 #grid 호스트를 JS 에서 직접 포커스
    // 가능(tabindex) + aria-label 부여하고 네이티브 DOM keydown 으로 포커스된
    // 행(getFocusedCell) 의 상세를 한 번 연다. 템플릿/TuiPageBuilder 는 변경하지 않는다.
    const gridHost = document.getElementById('grid');
    if (gridHost) {
        gridHost.tabIndex = 0;
        gridHost.setAttribute(
            'aria-label',
            '공지사항 목록. 행에서 Enter 또는 Space 키로 상세를 열 수 있습니다.'
        );
        gridHost.addEventListener('keydown', (ev) => {
            if (ev.key !== 'Enter' && ev.key !== ' ') return;
            const focused = grid.getFocusedCell();
            const rowKey = focused ? focused.rowKey : null;
            if (rowKey === null || rowKey === undefined) return;
            ev.preventDefault();
            const row = grid.getRow(rowKey);
            if (row) void openModalForUpdate(row).catch(() => {});
        });
    }

    const createButton = document.getElementById('btn-create');
    if (createButton) {
        createButton.addEventListener('click', openModalForCreate);
    }

    // 명시적 '게시판 팝업 예제' 버튼: 선택된 행이 있으면 추적 가능한 팝업으로 연다.
    // 미선택 시 warning 토스트를 띄우고 window.open 하지 않는다.
    const popupButton = document.getElementById('btn-popup-example');
    if (popupButton) {
        popupButton.addEventListener('click', () => {
            if (selectedNoticeId === null || selectedNoticeId === undefined) {
                CommonUtils.toast('선택된 행이 없습니다. 목록에서 행을 클릭하세요.', 'warning');
                return;
            }
            openPopup(selectedNoticeId);
        });
    }

    // 팝업(자식)이 보내는 postMessage 수신 — v2 강화 규약:
    //   1) 동일 origin 인지(ev.origin)
    //   2) 우리가 window.open 으로 연 팝업 핸들인지(ev.source ∈ openedPopups)
    //   3) action 이 'noticeChanged' 인지(v1 의 save-specific action 회귀 금지)
    //   4) operation 이 VALID_OPERATIONS 화이트리스트에 있는지
    // 네 가지를 모두 통과한 메시지에 한해 현재 페이지를 정확히 1회 새로고침하고 operation 전용
    // 한글 성공 토스트를 띄운다.
    window.addEventListener('message', async (ev) => {
        if (ev.origin !== window.location.origin) return;
        if (!isKnownPopup(ev.source)) return;
        if (!ev.data || ev.data.action !== 'noticeChanged') return;
        const operation = ev.data.operation;
        if (!VALID_OPERATIONS.has(operation)) return;

        try {
            await refreshCurrentPage();
            CommonUtils.toast(SUCCESS_MESSAGES[operation], 'success');
        } catch (err) {
            // 인터셉터가 중앙 에러 다이얼로그를 표시한다. 새로고침 실패 시 성공 토스트를 띄우지 않는다.
            console.error('[notice] 팝업 변경 알림 후 새로고침 실패.', err);
        }
    });
});
