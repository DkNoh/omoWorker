// Scaffold 생성(CRUD). 생성 후 개발자가 직접 수정해 소유한다.
//
// 화면 동작 요약
// 1. DOMContentLoaded 후 TuiPageBuilder가 그리드/검색/페이징을 초기화하고 목록을 자동 조회한다.
// 2. 그리드 행 클릭 시 별도 상세 API를 호출하지 않고, 그리드가 가진 행 데이터를 수정 폼에 바인딩한다.
// 3. 등록/수정 모달의 저장·삭제 버튼은 ModalManager가 이 파일의 save/remove 함수를 호출한다.
// 4. 저장 또는 삭제가 끝나면 모달을 닫고 사용자가 보고 있던 현재 페이지를 다시 조회한다.
document.addEventListener('DOMContentLoaded', function () {
    // customer-search.html과 modal-base.html이 만드는 모달 DOM id.
    // ModalManager는 이 값을 기준으로 모달과 저장/삭제 버튼을 찾는다.
    const MODAL_ID = 'customer-search-modal';

    // 등록/수정/삭제는 용도별 endpoint를 분리한다.
    // 목록 조회 endpoint는 아래 TuiPageBuilder의 apiUrl에서 별도로 지정한다.
    const API = {
        create: '/sms/customer-search/create',
        update: '/sms/customer-search/update',
        delete: '/sms/customer-search/delete'
    };

    // 등록 모드로 열 때 기존 수정값이 남지 않도록 폼에 주입하는 초기값이다.
    // 객체의 key는 detail-form 내부 입력 요소의 name과 일치해야 FormBinder가 값을 넣을 수 있다.
    const DEFAULT_FORM = { customerId: '', customerNm: '', mobileNo: '', email: '', birthDt: '', genderCd: '', agreeYn: '', useYn: '' };

    // 삭제 요청에 전달할 기본키 필드 목록. pkParams()가 이 목록을 기준으로 폼 값을 수집한다.
    const PK_FIELDS = ['customerId'];

    // 낙관적 잠금 컬럼 설정. 현재 화면에는 잠금 컬럼이 없으므로 null이며 applyLockSnapshot()은 즉시 종료한다.
    const LOCK = null;

    // 하나의 모달을 등록과 수정에 같이 사용하므로 현재 모드를 화면 상태로 보관한다.
    // selectedRow는 수정 모드에서 사용자가 선택한 그리드 행 원본을 보관한다.
    const state = {
        mode: 'create',
        selectedRow: null
    };

    // 공통 페이지 빌더가 그리드 생성, 검색 버튼/초기화/Enter, 페이지 크기와 페이징 이벤트를 담당한다.
    // 생성자 실행 마지막에 /sms/customer-search/data의 1페이지를 자동 조회한다.
    const pageBuilder = new TuiPageBuilder({
        el: 'grid',
        apiUrl: '/sms/customer-search/data',
        // 각 id를 가진 검색 입력값이 목록 조회의 query parameter로 전달된다.
        searchInputs: ['searchKeyword', 'agreeYn', 'useYn'],
        searchDefaults: {},
        // 실제 DOM에 없는 id를 전달해 빌더의 자동 등록 버튼 처리는 사용하지 않는다.
        // 이 화면의 #btn-create 이벤트는 파일 하단에서 직접 연결한다.
        btnCreate: 'crud-modal-auto-create-disabled',
        rowHeaders: ['rowNum'],
        columns: [
            { header: 'CUSTOMER_ID', name: 'customerId', align: 'center', width: 150 },
            { header: 'CUSTOMER_NM', name: 'customerNm', align: 'center', width: 150 },
            { header: 'MOBILE_NO', name: 'mobileNo', align: 'center', width: 150 },
            { header: 'EMAIL', name: 'email', align: 'center', width: 150 },
            { header: 'BIRTH_DT', name: 'birthDt', align: 'center', width: 150 },
            { header: 'GENDER_CD', name: 'genderCd', align: 'center', width: 150 },
            { header: 'AGREE_YN', name: 'agreeYn', align: 'center', width: 150 },
            { header: 'USE_YN', name: 'useYn', align: 'center', width: 150 },
            { header: 'REG_DTTM', name: 'regDttm', align: 'center', width: 150, formatter: TuiCommon.fmt.date }

        ]
    });

    // TuiPageBuilder가 생성한 실제 tui.Grid 인스턴스를 받아 화면 전용 행 클릭 동작을 추가한다.
    const grid = pageBuilder.getGrid();
    grid.on('click', (ev) => {
        // 컬럼 헤더 등 데이터 행이 아닌 영역을 클릭하면 rowKey가 없으므로 모달을 열지 않는다.
        if (ev.rowKey === null || ev.rowKey === undefined) return;

        // 클릭한 행은 이미 그리드에 적재된 데이터다. 여기서는 별도 상세조회 API를 호출하지 않는다.
        // openEdit()가 이 객체를 #detail-form에 채운 후 숨겨진 모달을 화면에 표시한다.
        openEdit(grid.getRow(ev.rowKey));
    });

    // 서버가 defaultLayout에 주입한 PAGE_AUTH와 현재 모드를 함께 검사한다.
    // 등록에는 create 권한, 수정에는 update 권한이 정확히 true여야 저장할 수 있다.
    const canSave = () => {
        const auth = window.PAGE_AUTH || {};
        return (state.mode === 'create' && auth.create === true)
            || (state.mode === 'update' && auth.update === true);
    };

    // 모달을 열기 직전에 모드와 페이지 권한에 맞춰 저장/삭제 버튼의 hidden 속성을 동기화한다.
    // 실제 API 접근 권한은 서버에서도 검증되며, 이 처리는 사용자에게 가능한 동작만 보여 주기 위한 UI 제어다.
    const syncActionButtons = () => {
        const modal = document.querySelector('#' + MODAL_ID);
        const saveBtn = modal.querySelector('#' + MODAL_ID + '-btn-save');
        const deleteBtn = modal.querySelector('#' + MODAL_ID + '-btn-delete');
        if (saveBtn) saveBtn.hidden = !canSave();
        if (deleteBtn) deleteBtn.hidden = state.mode !== 'update' || !(window.PAGE_AUTH || {}).delete;
    };

    // 잠금 컬럼이 설정된 화면에서는 조회 당시 값을 숨겨진 beforeField에 복사한다.
    // 서버는 이 이전 값으로 동시 수정 여부를 판단할 수 있다. 현재 LOCK=null이므로 아무 작업도 하지 않는다.
    const applyLockSnapshot = (row) => {
        if (!LOCK) return;
        const beforeLock = document.querySelector('#detail-form [name="' + LOCK.beforeField + '"]');
        if (beforeLock) beforeLock.value = row[LOCK.field] || '';
    };

    // 그리드 행 클릭으로 진입하는 수정 모드 처리.
    const openEdit = (row) => {
        state.mode = 'update';
        state.selectedRow = row;

        // row의 각 key와 동일한 name을 가진 #detail-form 필드에 값을 넣는다.
        // customer-search.html의 #modal-body는 이미 모달 안에 렌더링되어 있으므로,
        // 클릭 시 modal-body로 "이동"하는 것이 아니라 그 내부 폼의 값만 변경하는 것이다.
        // name이 없는 data-readonly-field 요소는 FormBinder의 자동 바인딩 대상이 아니다.
        FormBinder.bind('#detail-form', row);
        applyLockSnapshot(row);
        syncActionButtons();

        // 폼 값과 버튼 상태를 모두 준비한 뒤 CoreUI Modal 인스턴스의 show()를 호출한다.
        ModalManager.open(MODAL_ID);
    };

    // 화면 상단의 등록 버튼으로 진입하는 신규 등록 모드 처리.
    const openCreate = () => {
        state.mode = 'create';
        state.selectedRow = null;
        const form = document.querySelector('#detail-form');

        // 브라우저 기본 reset으로 이전 입력을 지운 뒤, 화면에서 정한 등록 기본값을 다시 바인딩한다.
        form.reset();
        FormBinder.bind('#detail-form', DEFAULT_FORM);
        syncActionButtons();
        ModalManager.open(MODAL_ID);

        // 모달이 열린 직후 사용자가 바로 입력할 수 있도록 첫 번째 표시 입력 요소에 포커스를 둔다.
        const firstInput = form.querySelector('input:not([type="hidden"]), select, textarea');
        if (firstInput) firstInput.focus();
    };

    // modal-base의 저장 버튼 클릭 시 ModalManager의 onSubmit 훅을 통해 호출된다.
    const save = async () => {
        // 클라이언트에서도 현재 모드에 필요한 권한이 없으면 요청을 보내지 않는다.
        if (!canSave()) return;
        const form = document.querySelector('#detail-form');

        // 필드 포맷/검증 모듈이 로드된 경우 모든 검증을 통과해야 payload를 생성한다.
        if (typeof FieldFormat !== 'undefined' && !await FieldFormat.validateForm(form)) return;

        // detail-form의 name 필드를 객체로 변환한다. 빈 문자열은 FormBinder 정책에 따라 null이 된다.
        const payload = FormBinder.toObject('#detail-form');

        // 동일한 폼을 사용하지만 현재 모드에 따라 등록 또는 수정 endpoint로 명확히 분기한다.
        if (state.mode === 'create') {
            await ApiClient.post(API.create, payload);
            CommonUtils.toast('등록되었습니다.', 'success');
        } else {
            await ApiClient.post(API.update, payload);
            CommonUtils.toast('수정되었습니다.', 'success');
        }

        // 성공한 경우에만 모달을 닫고, 사용자가 보던 현재 페이지를 서버에서 다시 조회한다.
        ModalManager.close(MODAL_ID);
        await pageBuilder.searchData(pageBuilder.currentPage || 1);
    };

    // 삭제 endpoint에 전달할 기본키 query parameter를 detail-form의 현재 값에서 만든다.
    const pkParams = () => {
        const params = {};
        PK_FIELDS.forEach(field => {
            const input = document.querySelector('#detail-form [name="' + field + '"]');
            params[field] = input ? input.value : null;
        });
        return params;
    };

    // modal-base의 삭제 버튼 클릭 시 ModalManager의 onDelete 훅을 통해 호출된다.
    const remove = () => {
        // 등록 중인 데이터는 아직 기본키가 확정되지 않았으므로 수정 모드에서만 삭제를 허용한다.
        if (state.mode !== 'update') return;
        CommonUtils.confirm('선택한 데이터를 삭제하시겠습니까?', async () => {
            // ApiClient.remove는 스캐폴드 삭제 계약에 맞춰 POST 요청과 query parameter를 사용한다.
            await ApiClient.remove(API.delete, pkParams());
            CommonUtils.toast('삭제되었습니다.', 'success');
            state.selectedRow = null;

            // 삭제 성공 후 모달을 닫고 현재 페이지를 재조회해 그리드에서 삭제된 행을 제거한다.
            ModalManager.close(MODAL_ID);
            await pageBuilder.searchData(pageBuilder.currentPage || 1);
        });
    };

    // 모달 lifecycle과 기본 footer 버튼을 화면 함수에 연결한다.
    // customer-search-modal-btn-save 클릭 -> save(), customer-search-modal-btn-delete 클릭 -> remove().
    ModalManager.init(MODAL_ID, {
        onSubmit: save,
        onDelete: remove
    });

    // 등록 버튼은 페이지 권한이 있을 때만 Thymeleaf가 렌더링하므로, 존재하는 경우에만 이벤트를 연결한다.
    const btnCreate = document.querySelector('#btn-create');
    if (btnCreate) btnCreate.addEventListener('click', openCreate);

    // data-format/data-mask 등의 선언이 있는 필드에 포맷터를 한 번 적용한다.
    if (typeof FieldFormat !== 'undefined') FieldFormat.applyFieldFormats(document.querySelector('#detail-form'));
});
