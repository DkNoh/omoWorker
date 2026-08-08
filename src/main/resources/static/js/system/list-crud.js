// 목록 + CRUD 모달 정적 샘플 (samples/list-crud.html)
//
// 화면 동작 요약
// 1. DOMContentLoaded 후 TuiPageBuilder가 그리드/검색/페이징을 초기화하고 목록을 자동 조회한다.
// 2. 그리드 행 클릭 시 별도 상세 API 없이 그리드가 가진 행 데이터를 등록/수정 폼에 바인딩해 모달을 연다.
// 3. 등록/수정 모달의 저장·삭제 버튼은 ModalManager가 이 파일의 save/remove 함수를 호출한다.
// 4. 저장 또는 삭제가 끝나면 모달을 닫고 사용자가 보고 있던 현재 페이지를 다시 조회한다.
//
// 이 샘플의 목록/등록/수정/삭제 endpoint는 실제 존재하지 않는다. file://·서버(http) 어느 쪽으로
// 열어도 오류 모달이 뜨지 않도록 axios.get 을 mock 서버로 항상 대체한다. 조회/검색/페이징 흐름은
// TuiPageBuilder의 실제 코드 경로를 그대로 타고, 저장/삭제는 MOCK_DEPTS 를 직접 갱신한다.
// 실제 화면 전환 시 mockListResponse 와 MOCK_DEPTS 갱신 부분을 실제 API 호출로 교체한다.
(function () {
    'use strict';

    // list-crud.html이 만드는 모달 DOM id — fragments/modal-base.html 규약과 일치한다.
    // ModalManager는 이 값을 기준으로 모달과 저장/삭제 버튼을 찾는다.
    const MODAL_ID = 'dept-modal';

    // 실제 화면 전환 시 사용할 endpoint — 샘플에는 컨트롤러가 없어 mock 으로만 응답한다.
    const API = {
        list: '/system/list-crud/data',
        create: '/system/list-crud/create',
        update: '/system/list-crud/update',
        delete: '/system/list-crud/delete'
    };

    // 등록 모드로 열 때 기존 입력값이 남지 않도록 폼에 주입하는 초기값이다.
    // key는 #dept-form 내부 입력 요소의 name과 일치해야 FormBinder가 값을 넣을 수 있다.
    const DEFAULT_FORM = { depId: '', depCode: '', depNm: '', useYn: 'Y', sortOrd: '' };

    // 데모용 모의 부서 데이터. 저장/삭제가 이 배열을 직접 갱신한다.
    let MOCK_DEPTS = [
        { depId: 'D001', depCode: 'DEPT-1001', depNm: '인사팀',       useYn: 'Y', sortOrd: 1, regDttm: '2025-09-12 10:24:05' },
        { depId: 'D002', depCode: 'DEPT-1002', depNm: '재무회계팀',   useYn: 'Y', sortOrd: 2, regDttm: '2025-09-12 10:31:48' },
        { depId: 'D003', depCode: 'DEPT-1003', depNm: 'IT운영팀',     useYn: 'Y', sortOrd: 3, regDttm: '2025-10-02 14:05:12' },
        { depId: 'D004', depCode: 'DEPT-1004', depNm: '마케팅팀',     useYn: 'Y', sortOrd: 4, regDttm: '2025-11-21 09:47:30' },
        { depId: 'D005', depCode: 'DEPT-1005', depNm: '고객지원센터', useYn: 'N', sortOrd: 5, regDttm: '2026-01-08 16:12:54' },
        { depId: 'D006', depCode: 'DEPT-1006', depNm: '감사실',       useYn: 'N', sortOrd: 6, regDttm: '2026-03-15 11:38:21' }
    ];

    // 하나의 모달을 등록과 수정에 같이 사용하므로 현재 모드를 화면 상태로 보관한다.
    // selectedRow는 수정 모드에서 사용자가 선택한 그리드 행 원본을 보관한다.
    const state = {
        mode: 'create',
        selectedRow: null
    };

    document.addEventListener('DOMContentLoaded', init);

    // TuiPageBuilder가 axios.get(url, { params }) 로 전달하는 검색/페이징 파라미터를 읽어
    // MOCK_DEPTS 를 필터링·페이징한 뒤 PageResponseDTO 형태로 응답하는 mock 서버다.
    // 전역 axios 인터셉터를 우회하므로 스피너/오류 모달이 동작하지 않는다.
    function mockListResponse(config) {
        const params = (config && config.params) || new URLSearchParams();
        const page = parseInt(params.get('page'), 10) || 1;
        const size = parseInt(params.get('size'), 10) || 10;
        const keyword = (params.get('searchDepNm') || '').trim().toLowerCase();
        const useYn = params.get('searchUseYn') || '';

        const filtered = MOCK_DEPTS.filter(row => {
            const matchKeyword = !keyword || String(row.depNm).toLowerCase().includes(keyword);
            const matchUse = !useYn || row.useYn === useYn;
            return matchKeyword && matchUse;
        });

        const totalPages = Math.max(1, Math.ceil(filtered.length / size));
        const safePage = Math.min(page, totalPages);
        const start = (safePage - 1) * size;
        const contents = filtered.slice(start, start + size);

        return {
            data: { contents, page: safePage, size, totalCount: filtered.length, totalPages }
        };
    }

    // 모의 데이터 추가 시 기존 최대 일련번호 다음 값을 부서 기본키로 채번한다.
    function nextDepId() {
        const maxSeq = MOCK_DEPTS.reduce((max, row) =>
            Math.max(max, parseInt(String(row.depId).replace(/\D/g, ''), 10) || 0), 0);
        return 'D' + String(maxSeq + 1).padStart(3, '0');
    }

    function init() {
    // 목록 endpoint는 실제 존재하지 않으므로 프로토콜과 무관하게 mock 서버로 대체한다.
    // TuiPageBuilder의 생성자 자동 조회와 조회/초기화/페이지 크기 버튼이 모두 이 mock 을 탄다.
    axios.get = (url, config) => Promise.resolve(mockListResponse(config));

    // 모달이 폼 기반(list-crud.html) 인지 그리드 기반(list-basic-modal.html) 인지 감지한다.
    const HAS_MODAL_FORM = !!document.querySelector('#dept-form');
    const HAS_MODAL_GRID = !!document.getElementById('modal-grid');

        // 공통 페이지 빌더가 그리드 생성, 검색 버튼/초기화/Enter, 페이지 크기와 페이징 이벤트를 담당한다.
        const pageBuilder = new TuiPageBuilder({
            el: 'grid',
            apiUrl: API.list,
            // 각 id 를 가진 검색 입력값이 목록 조회의 query parameter로 전달된다.
            searchInputs: ['searchDepNm', 'searchUseYn'],
            searchDefaults: {},
            rowHeaders: ['rowNum'],
            columns: [
                { header: '부서코드', name: 'depCode', align: 'center', width: 140 },
                { header: '부서명', name: 'depNm', align: 'left', minWidth: 200 },
                {
                    header: '사용여부', name: 'useYn', align: 'center', width: 100,
                    formatter: TuiCommon.badgeByValue({
                        labels: { Y: '사용', N: '미사용' },
                        tones: { Y: 'bg-success', N: 'bg-secondary' }
                    })
                },
                { header: '정렬순서', name: 'sortOrd', align: 'center', width: 100 },
                { header: '등록일시', name: 'regDttm', align: 'center', width: 170, formatter: TuiCommon.fmt.date }
            ]
        });

        // 그리드 기반 모달인 경우 모달 내 그리드를 초기화한다.
        if (HAS_MODAL_GRID) {
            modalGrid = new tui.Grid({
                el: document.getElementById('modal-grid'),
                data: () => [],
                columns: [
                    { header: '부서코드', name: 'depCode', align: 'center', width: 140 },
                    { header: '부서명', name: 'depNm', align: 'left', minWidth: 200 },
                    {
                        header: '사용여부', name: 'useYn', align: 'center', width: 100,
                        formatter: TuiCommon.badgeByValue({
                            labels: { Y: '사용', N: '미사용' },
                            tones: { Y: 'bg-success', N: 'bg-secondary' }
                        })
                    },
                    { header: '정렬순서', name: 'sortOrd', align: 'center', width: 100 },
                    { header: '등록일시', name: 'regDttm', align: 'center', width: 170, formatter: TuiCommon.fmt.date }
                ],
                scrollY: false,
                heightAdjustment: true
            });
        }

        // ── 모달 열기 ────────────────────────────────────────────────────
        // TuiPageBuilder가 생성한 실제 tui.Grid 인스턴스에 화면 전용 행 클릭 동작을 추가한다.
        const grid = pageBuilder.getGrid();
        grid.on('click', (ev) => {
            // 컬럼 헤더 등 데이터 행이 아닌 영역을 클릭하면 rowKey가 없으므로 모달을 열지 않는다.
            if (ev.rowKey === null || ev.rowKey === undefined) return;

            // 클릭한 행은 이미 그리드에 적재된 데이터다. 별도 상세조회 API를 호출하지 않는다.
            openEdit(grid.getRow(ev.rowKey));
        });

        // 서버가 defaultLayout에 주입한 PAGE_AUTH와 현재 모드를 함께 검사한다.
        // 등록에는 create 권한, 수정에는 update 권한이 정확히 true여야 저장할 수 있다.
        const canSave = () => {
            const auth = window.PAGE_AUTH || {};
            return (state.mode === 'create' && auth.create === true)
                || (state.mode === 'update' && auth.update === true);
        };

        // 모달을 열기 직전에 모드와 페이지 권한에 맞춰 저장/삭제 버튼의 d-none 클래스를 동기화한다.
        // 실제 API 접근 권한은 서버에서도 검증되며, 이 처리는 가능한 동작만 보여 주기 위한 UI 제어다.
        const syncActionButtons = () => {
            const auth = window.PAGE_AUTH || {};
            const modal = document.getElementById(MODAL_ID);
            const saveBtn = modal.querySelector('#' + MODAL_ID + '-btn-save');
            const deleteBtn = modal.querySelector('#' + MODAL_ID + '-btn-delete');
            if (saveBtn) saveBtn.classList.toggle('d-none', !canSave());
            if (deleteBtn) deleteBtn.classList.toggle('d-none', state.mode !== 'update' || auth.delete !== true);
        };

        // 모달 내 그리드 인스턴스 — HAS_MODAL_GRID 가 true 일 때만 초기화된다.
        let modalGrid = null;

        // 그리드 행 클릭으로 진입하는 수정 모드 처리.
        function openEdit(row) {
            state.mode = 'update';
            state.selectedRow = row;

            document.getElementById(MODAL_ID + '-title').textContent = '부서 수정';

            // 그리드 기반 모달인 경우 모달 그리드에 행 데이터를 채운다.
            if (HAS_MODAL_GRID && modalGrid) {
                modalGrid.setRows([row]);
            } else {
                // 폼 기반 모달은 기존 FormBinder 로직 유지
                FormBinder.bind('#dept-form', row);
            }
            syncActionButtons();

            // 폼 값과 버튼 상태를 모두 준비한 뒤 CoreUI Modal 인스턴스의 show()를 호출한다.
            ModalManager.open(MODAL_ID);
        }

        // 그리드 카드 헤더의 등록 버튼으로 진입하는 신규 등록 모드 처리.
        const openCreate = () => {
            state.mode = 'create';
            state.selectedRow = null;

            document.getElementById(MODAL_ID + '-title').textContent = '부서 등록';

            // 그리드 기반 모달인 경우 모달 그리드를 비운다.
            if (HAS_MODAL_GRID && modalGrid) {
                modalGrid.setRows([]);
            } else {
                // 폼 기반 모달은 기존 FormBinder 로직 유지
                const form = document.querySelector('#dept-form');
                form.reset();
                FormBinder.bind('#dept-form', DEFAULT_FORM);
            }
            syncActionButtons();
            ModalManager.open(MODAL_ID);

            // 폼 기반 모달만 첫 번째 입력 요소에 포커스를 둔다.
            if (!HAS_MODAL_GRID) {
                const firstInput = document.querySelector('#dept-form input:not([type="hidden"])');
                if (firstInput) firstInput.focus();
            }
        };

        // ── 저장 / 삭제 ──────────────────────────────────────────────────
        // 저장·삭제 후 모달을 닫고 사용자가 보던 현재 페이지를 다시 조회한다.
        const refreshList = () => pageBuilder.searchData(pageBuilder.currentPage || 1);

        // modal-base의 저장 버튼 클릭 시 ModalManager의 onSubmit 훅을 통해 호출된다.
        const save = async () => {
            // 클라이언트에서도 현재 모드에 필요한 권한이 없으면 요청을 보내지 않는다.
            if (!canSave()) return;

            // 그리드 기반 모달인 경우 현재 모달 그리드의 행을 payload 로 사용한다.
            const payload = HAS_MODAL_GRID && modalGrid ? modalGrid.getRow(modalGrid.getRowKey()) : FormBinder.toObject('#dept-form');

            // 샘플: mock 데이터를 갱신한다. 실제 화면에서는 API.create / API.update 를 호출한다.
            if (state.mode === 'create') {
                MOCK_DEPTS.unshift(Object.assign({}, payload, {
                    depId: nextDepId(),
                    useYn: payload.useYn || 'N',
                    regDttm: dayjs().format('YYYY-MM-DD HH:mm:ss')
                }));
                Notify.toast('등록되었습니다. (샘플 데이터)', 'success');
            } else {
                const index = MOCK_DEPTS.findIndex(row => row.depId === payload.depId);
                if (index !== -1) MOCK_DEPTS[index] = Object.assign({}, MOCK_DEPTS[index], payload);
                Notify.toast('수정되었습니다. (샘플 데이터)', 'success');
            }

            ModalManager.close(MODAL_ID);
            refreshList();
        };

        // 삭제 요청에 전달할 기본키를 모달의 현재 데이터에서 만든다.
        const pkParams = () => {
            if (HAS_MODAL_GRID && modalGrid) {
                const row = modalGrid.getRow(modalGrid.getRowKey());
                return { depId: row ? row.depId : null };
            }
            const input = document.querySelector('#dept-form [name="depId"]');
            return { depId: input ? input.value : null };
        };

        // modal-base의 삭제 버튼 클릭 시 ModalManager의 onDelete 훅을 통해 호출된다.
        const remove = () => {
            // 등록 중인 데이터는 아직 기본키가 확정되지 않았으므로 수정 모드에서만 삭제를 허용한다.
            if (state.mode !== 'update') return;

            Notify.confirm('선택한 부서를 삭제하시겠습니까?', () => {
                // 샘플: mock 데이터에서 제거한다. 실제 화면에서는 ApiClient.remove(API.delete, pkParams()) 호출.
                const depId = pkParams().depId;
                MOCK_DEPTS = MOCK_DEPTS.filter(row => row.depId !== depId);
                Notify.toast('삭제되었습니다. (샘플 데이터)', 'success');
                state.selectedRow = null;

                ModalManager.close(MODAL_ID);
                refreshList();
            });
        };

        // 모달 lifecycle과 기본 footer 버튼을 화면 함수에 연결한다.
        // dept-modal-btn-save 클릭 -> save(), dept-modal-btn-delete 클릭 -> remove().
        ModalManager.init(MODAL_ID, {
            onSubmit: save,
            onDelete: remove
        });

        // 등록 버튼은 PAGE_AUTH.create 권한이 있을 때만 노출한다.
        // (실제 화면에서는 Thymeleaf th:if="${pageAuth.create}" 가 렌더링을 담당한다.)
        const btnCreate = document.getElementById('btn-create');
        if (btnCreate) {
            if ((window.PAGE_AUTH || {}).create !== true) {
                btnCreate.hidden = true;
            } else {
                btnCreate.addEventListener('click', openCreate);
            }
        }

        // 폼 기반 모달만 data-validate 선언이 있는 필드에 검증기를 적용한다.
        if (!HAS_MODAL_GRID && typeof FieldFormat !== 'undefined') {
            FieldFormat.applyFieldFormats(document.querySelector('#dept-form'));
        }
    }
})();
