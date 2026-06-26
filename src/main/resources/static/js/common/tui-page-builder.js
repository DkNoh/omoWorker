/**
 * @class TuiPageBuilder
 * @description 
 * 단순 CRUD(목록 조회 위주) 백오피스 화면에서 반복적으로 작성되는 TUI Grid 초기화, 페이징 로직, 비동기 통신(fetch), 
 * 그리고 공통 버튼 이벤트(조회, 초기화, 페이지 사이즈 변경)를 단일 진입점(Facade)에서 자동화해 주는 래퍼(Wrapper) 엔진입니다.
 * 이 클래스를 사용하면 각 화면별 JS 파일에서는 고유한 컬럼 정의와 상세 모달 로직만 작성하면 됩니다.
 */
class TuiPageBuilder {
    
    /**
     * TuiPageBuilder 인스턴스를 생성하고 내부 구성 요소들을 초기화합니다.
     * @param {Object} config - 화면별 고유 설정값
     * @param {string} config.el - TUI Grid가 렌더링될 DOM 요소의 ID (기본값: 'grid')
     * @param {string} config.apiUrl - 데이터를 조회할 백엔드 API URL (필수)
     * @param {Array<string>} config.searchInputs - 검색 조건으로 사용할 HTML input/select 요소들의 ID 배열
     * @param {Array<string>} config.rowHeaders - 그리드 좌측 헤더 설정 (예: ['rowNum'], 다중선택시 ['checkbox', 'rowNum'])
     * @param {Array<Object>} config.columns - TUI Grid의 컬럼 메타데이터 배열
     * @param {string} config.pageSizeEl - 페이지당 건수를 조절하는 select 요소의 ID (기본값: 'pageSizeSelect')
     * @param {string} config.btnSearch - 검색 조회 버튼의 ID (기본값: 'btn-search')
     * @param {string} config.btnReset - 검색 조건 초기화 버튼의 ID (기본값: 'btn-reset')
     * @param {Function} config.onGridDblClick - 그리드 행 더블클릭 시 실행될 콜백 함수 (행 데이터 반환)
     * @param {Function} config.onGridUpdated - 데이터 갱신이 완료된 후 실행될 콜백 함수
     */
    constructor(config) {
        // 1. 기본 설정값과 사용자가 넘겨준 config를 병합(Merge)합니다.
        this.config = Object.assign({
            el: 'grid',               
            apiUrl: '',               
            searchInputs: [],         
            requiredInputs: [],       
            rowHeaders: ['rowNum'],   
            columns: [],              
            pageSizeEl: 'pageSizeSelect',
            btnSearch: 'btn-search',
            btnReset: 'btn-reset',
            btnCreate: 'btn-create',
            paginationId: 'pagination',
            totalCountSelector: '#total-count',
            gridOptions: {},          // 화면별 그리드 옵션 예외 (gridDefaults 위에 덮어씀)
            searchDefaults: {},       // 검색조건 기본값. 예: { startDt: 'THIS_MONTH', endDt: 'TODAY' }
            datePickerInputs: null,   // 비우면 data-search-type="date" 검색조건을 자동 감지한다
            onGridUpdated: null,
            // [API 단건 조회(패턴B) 옵션]
            detailApiUrl: null,       // 예: '/sample/detail'
            detailParamKey: null,     // 예: 'empId'
            onDetailLoaded: null,     // API 통신 성공 후 DTO를 받을 콜백
            // [단순 뷰어(패턴C) 자동 생성 옵션]
            autoModal: false,         // true 설정 시 그리드 컬럼 메타를 이용해 자동 모달 띄움
            autoModalTitle: '상세 정보',
            autoModalCreateTitle: null,
            modalActions: null        // { updateUrl, deleteUrl, pkFields|pkField, lockField, beforeLockField, editable }
        }, config);

        // 2. 내부 상태 변수 초기화
        this.grid = null;           
        this.currentPage = 1;       
        this.currentSize = 10;      
        this.usesOffsetRowNo = false;
        this.currentModalRow = null;
        this.currentModalMode = 'detail';
        this.searchDatePickers = {};

        // 3. 페이지 로드 시 콤보박스 동기화
        const sizeEl = document.getElementById(this.config.pageSizeEl);
        if (sizeEl) this.currentSize = parseInt(sizeEl.value, 10);

        // 4. 날짜 기본 세팅
        if (typeof CommonUtils !== 'undefined' && typeof CommonUtils.setDefaultDateTime === 'function') {
            CommonUtils.setDefaultDateTime();
        }
        this._applySearchDefaults(false);
        this._initSearchDatePickers();

        // 5. 그리드 렌더링 및 이벤트 바인딩
        this._initGrid();
        this._bindEvents();

        // 6. 렌더링 완료 후 1페이지 자동 조회 실행
        this.searchData(1);
    }

    _initGrid() {
        // 그리드 공통 옵션은 TuiCommon.gridDefaults가 단일 통제점이다.
        // 화면별 예외는 config.gridOptions로 넘긴다.
        const defaults = (typeof TuiCommon !== 'undefined' && TuiCommon.gridDefaults)
            ? TuiCommon.gridDefaults
            : { scrollX: false, scrollY: false, minBodyHeight: 300 };

        const requestedRowHeaders = this.config.rowHeaders || [];
        this.usesOffsetRowNo = requestedRowHeaders.some(rh => this._isRowNumHeader(rh));
        const rowHeaders = requestedRowHeaders.filter(rh => !this._isRowNumHeader(rh));
        const columns = this.usesOffsetRowNo && !this.config.columns.some(col => col.name === 'rowNo')
            ? [this._rowNoColumn()].concat(this.config.columns)
            : this.config.columns;

        this.grid = new tui.Grid(Object.assign({}, defaults, this.config.gridOptions, {
            el: document.getElementById(this.config.el),
            rowHeaders: rowHeaders,
            columns: columns
        }));

        this._toggleEmptyState(0);

        // 더블클릭 이벤트 바인딩 (하이브리드 모달 로직 적용)
        this.grid.on('dblclick', (ev) => {
            if (ev.rowKey !== null && ev.rowKey !== undefined) {
                const row = this.grid.getRow(ev.rowKey);
                
                // [패턴 B] API 단건 조회 모드 (HTTP 호출은 axios로 통일한다)
                if (this.config.detailApiUrl && this.config.detailParamKey && this.config.onDetailLoaded) {
                    const paramVal = row[this.config.detailParamKey];
                    if (!paramVal) return;
                    axios.get(this.config.detailApiUrl, { params: { [this.config.detailParamKey]: paramVal } })
                         .then(res => this.config.onDetailLoaded(res.data))
                         .catch(err => console.error('API 상세 조회 실패', err));
                }
                // [패턴 C] 컬럼 메타데이터 기반 단순 뷰어 자동 생성 모드
                else if (this.config.autoModal) {
                    this._showAutoModal(row);
                }
                // [패턴 A] 사용자 정의 콜백 모드
                else if (this.config.onGridDblClick) {
                    this.config.onGridDblClick(row);
                }
            }
        });
    }

    /**
     * [내부 메서드] TUI Grid 컬럼 정의를 기반으로 자동 팝업 DOM을 생성하고 데이터를 바인딩합니다.
     */
    _showAutoModal(row) {
        const modal = this._ensureAutoModal();
        this.currentModalMode = 'detail';
        this.currentModalRow = row;
        document.getElementById('tui-auto-modal-title').textContent = this.config.autoModalTitle;
        
        const formEl = document.getElementById('tui-auto-modal-form');
        formEl.innerHTML = ''; // 초기화

        // 컬럼 정의(this.config.columns)를 순회하며 행 데이터를 바인딩
        this.config.columns.forEach(col => {
            if (col.modalVisible === false) {
                return;
            }
            const rawVal = row[col.name];
            const formValue = rawVal === null || rawVal === undefined ? '' : rawVal;

            const rowDiv = document.createElement('div');
            rowDiv.className = 'detail-row';
            const labelDiv = document.createElement('div');
            labelDiv.className = 'detail-label';
            labelDiv.textContent = col.header;

            const valueDiv = document.createElement('div');
            valueDiv.className = 'detail-value';
            if (this._isAutoModalEditable() && col.editable === true) {
                const input = document.createElement('input');
                input.type = 'text';
                input.name = col.name;
                input.className = 'form-control form-control-sm detail-input';
                input.value = formValue;
                if (col.inputMask) input.dataset.mask = col.inputMask;
                if (col.validate) input.dataset.validate = col.validate;
                valueDiv.appendChild(input);
            } else if (col.formatter && typeof col.formatter === 'function') {
                valueDiv.innerHTML = `<span class="detail-readonly">${col.formatter({ value: rawVal, row: row }) || '-'}</span>`;
            } else {
                const readonly = document.createElement('span');
                readonly.className = 'detail-readonly';
                readonly.textContent = formValue || '-';
                valueDiv.appendChild(readonly);
            }

            rowDiv.appendChild(labelDiv);
            rowDiv.appendChild(valueDiv);
            formEl.appendChild(rowDiv);
        });
        this._appendActionHiddenFields(formEl, row);
        this._syncAutoModalActionButtons();

        this._showModalElement(modal);
        this._refreshIcons();
        this._applyFieldFormats(formEl);
    }

    _ensureAutoModal() {
        let modal = document.getElementById('tui-auto-modal');
        if (!modal) {
            modal = document.createElement('div');
            modal.id = 'tui-auto-modal';
            modal.className = 'modal fade';
            modal.tabIndex = -1;
            modal.setAttribute('aria-hidden', 'true');
            modal.setAttribute('aria-labelledby', 'tui-auto-modal-title');
            modal.innerHTML = `
                <div class="modal-dialog modal-dialog-centered modal-lg">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title" id="tui-auto-modal-title"></h5>
                            <button type="button" class="btn-close" id="tui-auto-modal-close" aria-label="닫기"></button>
                        </div>
                        <div class="modal-body tui-auto-modal-body">
                            <form id="tui-auto-modal-form"></form>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-primary d-none" id="tui-auto-modal-create">
                                <i data-lucide="plus"></i><span>등록</span>
                            </button>
                            <button type="button" class="btn btn-danger d-none" id="tui-auto-modal-delete">
                                <i data-lucide="trash-2"></i><span>삭제</span>
                            </button>
                            <button type="button" class="btn btn-primary d-none" id="tui-auto-modal-update">
                                <i data-lucide="save"></i><span>수정</span>
                            </button>
                            <button type="button" class="btn btn-secondary" id="tui-auto-modal-cancel">
                                <i data-lucide="x"></i><span>닫기</span>
                            </button>
                        </div>
                    </div>
                </div>
            `;
            document.body.appendChild(modal);
            document.getElementById('tui-auto-modal-close').addEventListener('click', () => this._closeAutoModal());
            document.getElementById('tui-auto-modal-cancel').addEventListener('click', () => this._closeAutoModal());
            document.getElementById('tui-auto-modal-create').addEventListener('click', () => this._createAutoModalRow());
            document.getElementById('tui-auto-modal-update').addEventListener('click', () => this._updateAutoModalRow());
            document.getElementById('tui-auto-modal-delete').addEventListener('click', () => this._deleteAutoModalRow());
            modal.addEventListener('click', e => {
                if (e.target.id === 'tui-auto-modal') {
                    this._closeAutoModal();
                }
            });
        }
        return modal;
    }

    _showCreateModal() {
        const actions = this.config.modalActions || {};
        if (!actions.createUrl || !this._hasPagePermission('create')) {
            return;
        }
        const modal = this._ensureAutoModal();
        this.currentModalMode = 'create';
        this.currentModalRow = null;
        document.getElementById('tui-auto-modal-title').textContent =
            this.config.autoModalCreateTitle || this.config.autoModalTitle.replace(/\s*상세\s*$/, '') + ' 등록';

        const formEl = document.getElementById('tui-auto-modal-form');
        formEl.innerHTML = '';
        let editableCount = 0;

        this.config.columns.forEach(col => {
            if (col.modalVisible === false || col.editable !== true) {
                return;
            }
            editableCount++;
            const rowDiv = document.createElement('div');
            rowDiv.className = 'detail-row';
            const labelDiv = document.createElement('div');
            labelDiv.className = 'detail-label';
            labelDiv.textContent = col.header;

            const valueDiv = document.createElement('div');
            valueDiv.className = 'detail-value';
            const input = document.createElement('input');
            input.type = 'text';
            input.name = col.name;
            input.className = 'form-control form-control-sm detail-input';
            if (col.inputMask) input.dataset.mask = col.inputMask;
            if (col.validate) input.dataset.validate = col.validate;
            valueDiv.appendChild(input);

            rowDiv.appendChild(labelDiv);
            rowDiv.appendChild(valueDiv);
            formEl.appendChild(rowDiv);
        });
        if (editableCount === 0) {
            formEl.innerHTML = '<div class="text-secondary py-3">등록 가능한 컬럼이 없습니다.</div>';
        }
        this._syncAutoModalActionButtons();

        this._showModalElement(modal);
        this._refreshIcons();
        this._applyFieldFormats(formEl);
    }

    _closeAutoModal() {
        this._hideModalElement(document.getElementById('tui-auto-modal'));
    }

    _isAutoModalEditable() {
        return !!(this.config.modalActions && this.config.modalActions.editable && this._hasPagePermission('update'));
    }

    _appendActionHiddenFields(formEl, row) {
        const actions = this.config.modalActions;
        if (!actions) {
            return;
        }
        this._pkFields(actions).forEach(pkField => this._appendHidden(formEl, pkField, row[pkField]));
        if (actions.beforeLockField) {
            const lockValue = actions.lockField ? row[actions.lockField] : '';
            this._appendHidden(formEl, actions.beforeLockField, lockValue);
        }
    }

    _appendHidden(formEl, name, value) {
        if (!name) {
            return;
        }
        const hidden = document.createElement('input');
        hidden.type = 'hidden';
        hidden.name = name;
        hidden.value = value === null || value === undefined ? '' : value;
        formEl.appendChild(hidden);
    }

    _syncAutoModalActionButtons() {
        const actions = this.config.modalActions || {};
        const createBtn = document.getElementById('tui-auto-modal-create');
        const updateBtn = document.getElementById('tui-auto-modal-update');
        const deleteBtn = document.getElementById('tui-auto-modal-delete');
        const createMode = this.currentModalMode === 'create';
        if (createBtn) {
            createBtn.classList.toggle('d-none', !createMode || !actions.createUrl || !this._hasPagePermission('create'));
        }
        if (updateBtn) {
            updateBtn.classList.toggle('d-none', createMode || !actions.updateUrl || !this._hasPagePermission('update'));
        }
        if (deleteBtn) {
            deleteBtn.classList.toggle('d-none', createMode || !actions.deleteUrl || !this._hasPagePermission('delete'));
        }
    }

    async _createAutoModalRow() {
        const actions = this.config.modalActions || {};
        if (!actions.createUrl || !this._hasPagePermission('create')) {
            return;
        }
        if (!(await this._validateModalForm())) {
            return;
        }
        const payload = this._readAutoModalForm();
        axios.post(actions.createUrl, payload)
            .then(() => {
                this._closeAutoModal();
                if (typeof CommonUtils !== 'undefined') {
                    CommonUtils.toast('등록되었습니다.', 'success');
                }
                this.searchData(1);
            })
            .catch(err => console.error('[TuiPageBuilder] 등록 실패', err));
    }

    async _updateAutoModalRow() {
        const actions = this.config.modalActions || {};
        if (!actions.updateUrl || !this._hasPagePermission('update')) {
            return;
        }
        if (!(await this._validateModalForm())) {
            return;
        }
        const payload = this._readAutoModalForm();
        axios.post(actions.updateUrl, payload)
            .then(() => {
                this._closeAutoModal();
                if (typeof CommonUtils !== 'undefined') {
                    CommonUtils.toast('수정되었습니다.', 'success');
                }
                this.searchData(this.currentPage);
            })
            .catch(err => console.error('[TuiPageBuilder] 수정 실패', err));
    }

    _deleteAutoModalRow() {
        const actions = this.config.modalActions || {};
        const pkFields = this._pkFields(actions);
        if (!actions.deleteUrl || !this._hasPagePermission('delete') || pkFields.length === 0 || !this.currentModalRow) {
            return;
        }
        const params = {};
        const missingPk = pkFields.some(pkField => {
            const pkValue = this.currentModalRow[pkField];
            params[pkField] = pkValue;
            return pkValue === null || pkValue === undefined || pkValue === '';
        });
        if (missingPk) {
            if (typeof CommonUtils !== 'undefined') {
                CommonUtils.toast('삭제 기준 PK 값이 없습니다.', 'warning');
            }
            return;
        }

        const runDelete = () => {
            axios.post(actions.deleteUrl, null, { params })
                .then(() => {
                    this._closeAutoModal();
                    if (typeof CommonUtils !== 'undefined') {
                        CommonUtils.toast('삭제되었습니다.', 'success');
                    }
                    this.searchData(this.currentPage);
                })
                .catch(err => console.error('[TuiPageBuilder] 삭제 실패', err));
        };

        if (typeof CommonUtils !== 'undefined' && CommonUtils.confirm) {
            CommonUtils.confirm('삭제하시겠습니까?', runDelete, '삭제 확인');
        } else if (window.confirm('삭제하시겠습니까?')) {
            runDelete();
        }
    }

    _readAutoModalForm() {
        if (typeof FormBinder !== 'undefined') {
            return FormBinder.toObject('#tui-auto-modal-form');
        }
        const result = {};
        document.querySelectorAll('#tui-auto-modal-form input[name], #tui-auto-modal-form select[name], #tui-auto-modal-form textarea[name]')
            .forEach(field => {
                result[field.name] = field.value === '' ? null : field.value;
        });
        return result;
    }

    _pkFields(actions) {
        if (!actions) {
            return [];
        }
        if (Array.isArray(actions.pkFields) && actions.pkFields.length > 0) {
            return actions.pkFields.filter(Boolean);
        }
        return actions.pkField ? [actions.pkField] : [];
    }

    _refreshIcons() {
        if (typeof CommonUtils !== 'undefined' && CommonUtils.refreshIcons) {
            CommonUtils.refreshIcons();
        } else if (window.lucide && typeof window.lucide.createIcons === 'function') {
            window.lucide.createIcons();
        }
    }

    _toggleEmptyState(totalCount) {
        const shell = document.getElementById(this.config.el)?.parentElement;
        if (!shell) return;
        const empty = shell.querySelector('[data-empty-state]');
        if (!empty) return;
        const isEmpty = !totalCount || totalCount <= 0;
        empty.classList.toggle('is-visible', isEmpty);
    }

    // 모달 폼에 입력 마스크(IMask) + 검증(JustValidate)을 부착한다 (field-format.js)
    _applyFieldFormats(formEl) {
        if (window.FieldFormat && formEl) {
            FieldFormat.applyFieldFormats(formEl);
        }
    }

    // 모달 폼 검증을 실행한다. 검증기가 없으면 통과(true). 서버 @Valid가 최종 권위다.
    _validateModalForm() {
        if (!window.FieldFormat) {
            return Promise.resolve(true);
        }
        return FieldFormat.validateForm(document.getElementById('tui-auto-modal-form'));
    }

    _hasPagePermission(permission) {
        if (!window.PAGE_AUTH || typeof window.PAGE_AUTH !== 'object') {
            return false;
        }
        return window.PAGE_AUTH[permission] === true;
    }

    hasPagePermission(permission) {
        return this._hasPagePermission(permission);
    }

    _getFrameworkModal(modal) {
        if (!modal) return null;
        if (window.coreui && window.coreui.Modal) {
            return window.coreui.Modal.getOrCreateInstance(modal);
        }
        if (window.bootstrap && window.bootstrap.Modal) {
            return window.bootstrap.Modal.getOrCreateInstance(modal);
        }
        return null;
    }

    _showModalElement(modal) {
        const instance = this._getFrameworkModal(modal);
        if (instance) {
            instance.show();
            return;
        }
        modal.style.display = 'block';
        modal.removeAttribute('aria-hidden');
        modal.setAttribute('aria-modal', 'true');
        modal.classList.add('show');
        document.body.classList.add('modal-open');
    }

    _hideModalElement(modal) {
        if (!modal) return;
        const instance = this._getFrameworkModal(modal);
        if (instance) {
            instance.hide();
            return;
        }
        modal.classList.remove('show');
        modal.setAttribute('aria-hidden', 'true');
        modal.removeAttribute('aria-modal');
        modal.style.display = 'none';
        document.body.classList.remove('modal-open');
    }

    /**
     * [퍼블릭 메서드] 설정된 API URL로 검색 조건과 페이징 정보를 담아 비동기 조회(Fetch)를 수행합니다.
     * @param {number} page - 조회할 페이지 번호 (기본값: 1)
     */
    searchData(page = 1) {
        // 0. 필수값 검증 (Validation)
        if (this.config.requiredInputs && this.config.requiredInputs.length > 0) {
            for (const id of this.config.requiredInputs) {
                const el = document.getElementById(id);
                if (el && !el.value.trim()) {
                    if (typeof CommonUtils !== 'undefined') {
                        CommonUtils.toast('필수 검색 조건을 입력해 주세요.', 'warning');
                    } else {
                        alert('필수 검색 조건을 입력해 주세요.');
                    }
                    el.focus();
                    return; // 검증 실패 시 API 호출 중단
                }
            }
        }

        // 0.1. 날짜 논리 검증 (시작일 > 종료일 방지)
        const startDateEl = document.getElementById('startDate');
        const endDateEl = document.getElementById('endDate');
        if (startDateEl && endDateEl && startDateEl.value && endDateEl.value) {
            if (dayjs(startDateEl.value).isAfter(dayjs(endDateEl.value))) {
                if (typeof CommonUtils !== 'undefined') {
                    CommonUtils.toast('시작일은 종료일보다 클 수 없습니다.', 'warning');
                } else {
                    alert('시작일은 종료일보다 클 수 없습니다.');
                }
                startDateEl.focus();
                return; // 검증 실패 시 API 호출 중단
            }
        }

        this.currentPage = page; // 현재 페이지 상태 업데이트
        
        // 1. GET 요청 파라미터 조합 (URLSearchParams 활용)
        const params = this.getSearchParams({ includePaging: true });

        // 2. 서버 연동 — HTTP 호출은 axios로 통일한다 (screen-convention.md)
        //    전역 인터셉터가 스피너, ApiResponse 언래핑, 오류 모달을 담당한다.
        axios.get(this.config.apiUrl, { params })
        .then(response => {
            const page = response.data; // 인터셉터가 언래핑한 PageResponseDTO
            // 세션 만료(HTML 응답)는 전역 응답 인터셉터가 /login 으로 전환한다. 정상 페이로드만 렌더한다.
            if (!page || typeof page !== 'object') return;

            const contents = this._withRowNo(page.contents || [], page.page || this.currentPage, page.size || this.currentSize);
            this.grid.resetData(contents, {
                pageState: {
                    page: page.page || this.currentPage,
                    totalCount: page.totalCount || contents.length,
                    perPage: page.size || this.currentSize
                }
            });

            this._toggleEmptyState(page.totalCount || contents.length);

            // 데이터가 0건일 때 토스트 알림
            if (contents.length === 0 && typeof CommonUtils !== 'undefined') {
                CommonUtils.toast('조회된 데이터가 없습니다.', 'info');
            }

            // 공통 페이징 함수(TuiCommon)가 로드되어 있다면, 텍스트 갱신 및 페이징 버튼 렌더링 호출
            if (typeof TuiCommon !== 'undefined') {
                TuiCommon.updateTotalCount(page.totalCount || 0, this.config.totalCountSelector);
                TuiCommon.renderPagination(
                    page.page || 1,
                    page.totalPages || 1,
                    (p) => this.searchData(p),
                    this.config.paginationId
                );
            }

            // 외부에서 주입한 데이터 갱신 완료 콜백이 있다면 실행
            if (this.config.onGridUpdated) {
                this.config.onGridUpdated(page);
            }
        })
        .catch(err => {
            // 오류 알림은 common-utils 전역 인터셉터가 모달로 표시한다. 여기서는 기록만 남긴다.
            console.error('[TuiPageBuilder] 목록 조회 실패', err);
        });
    }

    /**
     * [내부 메서드] 화면 내 공통 툴바 버튼들(조회, 초기화, 사이즈 변경)에 이벤트를 바인딩합니다.
     * @private
     */
    _bindEvents() {
        // 1. 조회 버튼 바인딩
        const btnSearch = document.getElementById(this.config.btnSearch);
        if (btnSearch) {
            btnSearch.addEventListener('click', () => this.searchData(1));
        }

        const btnCreate = document.getElementById(this.config.btnCreate);
        if (btnCreate) {
            const actions = this.config.modalActions || {};
            const canCreate = !!actions.createUrl && this._hasPagePermission('create');
            btnCreate.classList.toggle('d-none', !canCreate);
            if (canCreate) {
                btnCreate.addEventListener('click', () => this._showCreateModal());
            }
        }

        // 2. 초기화 버튼 바인딩
        const btnReset = document.getElementById(this.config.btnReset);
        if (btnReset) {
            btnReset.addEventListener('click', () => {
                // TuiPageBuilder 자체적으로 등록된 검색 조건들을 모두 빈 값으로 초기화합니다.
                this.config.searchInputs.forEach(id => {
                    const el = document.getElementById(id);
                    if (el && el.tagName !== 'SELECT') el.value = '';
                    if (el && el.tagName === 'SELECT') el.selectedIndex = 0;
                    document.querySelectorAll(`input[name="${id}"]`).forEach((radio, index) => {
                        radio.checked = index === 0;
                    });
                });
                this._applySearchDefaults(true);
                
                // 만약 화면 특화 초기화 로직이 있다면 추가 실행
                if (typeof CommonUtils !== 'undefined' && typeof CommonUtils.resetFields === 'function') {
                    CommonUtils.resetFields();
                }
                this._syncSearchDatePickers();
                
                // 초기화 후 1페이지 재조회
                this.searchData(1);
            });
        }

        // 3. 페이지 사이즈(10건, 20건...) 셀렉트 박스 바인딩
        const pageSizeEl = document.getElementById(this.config.pageSizeEl);
        if (pageSizeEl) {
            pageSizeEl.addEventListener('change', (e) => {
                this.currentSize = parseInt(e.target.value, 10);
                this.searchData(1); // 사이즈 변경 시 1페이지로 돌아가서 재조회
            });
        }

        // 4. 검색창 Enter 키 입력 시 자동 조회 바인딩
        if (this.config.searchInputs && this.config.searchInputs.length > 0) {
            this.config.searchInputs.forEach(id => {
                const el = document.getElementById(id);
                if (el && el.tagName === 'INPUT') {
                    el.addEventListener('keypress', (e) => {
                        if (e.key === 'Enter') {
                            e.preventDefault(); // 기본 폼 제출 방지
                            this.searchData(1);
                        }
                    });
                }
            });
        }
    }

    /**
     * 외부(화면별 JS)에서 내부의 TUI Grid 원본 객체에 직접 접근할 필요가 있을 때 호출합니다.
     * @returns {Object} TUI Grid 인스턴스
     */
    getGrid() { return this.grid; }

    /**
     * 좌측 체크박스가 활성화된 행(Row)들의 데이터를 배열로 반환합니다. 일괄 처리(예: 일괄 승인) 시 유용합니다.
     * @returns {Array<Object>} 체크된 행 데이터 배열
     */
    getCheckedRows() { return this.grid.getCheckedRows(); }

    /**
     * 현재 마우스 클릭으로 포커싱된 단일 셀의 정보를 반환합니다. 단건 상세 조회 시 주로 쓰입니다.
     * @returns {Object|null} 포커스된 셀 정보 객체
     */
    getFocusedCell() { return this.grid.getFocusedCell(); }

    /**
     * 현재 유지 중인 페이지 번호를 반환합니다. 데이터 수정/삭제 후 현재 페이지를 재조회할 때 활용합니다.
     * @returns {number} 현재 페이지 번호
     */
    getCurrentPage() { return this.currentPage; }

    getSearchParams(options = {}) {
        const opts = Object.assign({ includePaging: false }, options);
        const params = new URLSearchParams();
        if (opts.includePaging) {
            params.append('page', this.currentPage);
            params.append('size', this.currentSize);
        }
        this.config.searchInputs.forEach(id => {
            params.append(id, this._readSearchValue(id));
        });
        return params;
    }

    _isRowNumHeader(rowHeader) {
        return rowHeader === 'rowNum' || (rowHeader && rowHeader.type === 'rowNum');
    }

    _rowNoColumn() {
        return {
            header: 'No.',
            name: 'rowNo',
            align: 'center',
            width: 60,
            sortable: false,
            formatter: ({ value }) => value || '-'
        };
    }

    _withRowNo(contents, page, size) {
        if (!this.usesOffsetRowNo) {
            return contents;
        }
        const offset = (Number(page || 1) - 1) * Number(size || this.currentSize || 10);
        return contents.map((row, index) => Object.assign({}, row, { rowNo: offset + index + 1 }));
    }

    _initSearchDatePickers() {
        if (!window.tui || !window.tui.DatePicker) {
            return;
        }
        this._datePickerInputIds().forEach(id => {
            const input = document.getElementById(id);
            const layer = document.getElementById(`${id}PickerLayer`);
            if (!input || !layer) {
                return;
            }
            this.searchDatePickers[id] = new tui.DatePicker(layer, {
                language: 'ko',
                date: this._toDatePickerDate(input.value),
                input: { element: input, format: 'yyyy-MM-dd' },
                calendar: { showToday: true }
            });
        });
    }

    _datePickerInputIds() {
        const configured = Array.isArray(this.config.datePickerInputs) ? this.config.datePickerInputs : [];
        const source = configured.length > 0 ? configured : this.config.searchInputs;
        return source.filter((id, index, ids) =>
            ids.indexOf(id) === index && this._isDateSearchInput(id)
        );
    }

    _isDateSearchInput(id) {
        const el = document.getElementById(id);
        return !!(el && (el.type === 'date' || el.dataset.searchType === 'date'));
    }

    _toDatePickerDate(value) {
        if (!value || typeof dayjs === 'undefined') {
            return null;
        }
        const parsed = dayjs(value);
        return parsed.isValid() ? parsed.toDate() : null;
    }

    _syncSearchDatePickers() {
        Object.keys(this.searchDatePickers || {}).forEach(id => {
            const picker = this.searchDatePickers[id];
            const input = document.getElementById(id);
            const date = this._toDatePickerDate(input ? input.value : '');
            if (date) {
                picker.setDate(date, true);
            } else if (picker && typeof picker.setNull === 'function') {
                picker.setNull();
            }
        });
    }

    _readSearchValue(id) {
        const el = document.getElementById(id);
        if (el && el.value !== undefined) {
            let value = el.value;
            if (el.type === 'date' || el.dataset.searchType === 'date') {
                value = value ? dayjs(value).format('YYYYMMDD') : '';
            } else if (el.type === 'datetime-local') {
                value = value ? dayjs(value).format('YYYYMMDDHHmm') : '';
            }
            return value;
        }
        const checked = document.querySelector(`input[name="${id}"]:checked`);
        return checked ? checked.value : '';
    }

    _applySearchDefaults(forceReset) {
        if (!this.config.searchDefaults) {
            return;
        }
        Object.keys(this.config.searchDefaults).forEach(id => {
            const defaultCode = this.config.searchDefaults[id];
            if (!defaultCode || defaultCode === 'NONE') {
                return;
            }
            const el = document.getElementById(id);
            const defaultValue = this._resolveDefaultValue(id, defaultCode);
            if (el && (forceReset || !el.value)) {
                if (el.tagName === 'SELECT') {
                    el.value = defaultValue;
                } else {
                    el.value = defaultValue;
                }
            }
            const radios = document.querySelectorAll(`input[name="${id}"]`);
            if (radios.length > 0) {
                radios.forEach(radio => {
                    radio.checked = radio.value === defaultValue;
                });
            }
        });
    }

    _resolveDefaultValue(id, defaultCode) {
        if (typeof dayjs === 'undefined') {
            return '';
        }
        const today = dayjs();
        switch (defaultCode) {
            case 'TODAY':
                return today.format('YYYY-MM-DD');
            case 'YESTERDAY':
                return today.subtract(1, 'day').format('YYYY-MM-DD');
            case 'RECENT_7_DAYS':
                return this._isRangeEnd(id)
                    ? today.format('YYYY-MM-DD')
                    : today.subtract(6, 'day').format('YYYY-MM-DD');
            case 'THIS_MONTH':
            case 'CURRENT_MONTH_TO_TODAY':
                return this._isRangeEnd(id)
                    ? today.format('YYYY-MM-DD')
                    : today.startOf('month').format('YYYY-MM-DD');
            default:
                return defaultCode;
        }
    }

    _isRangeEnd(id) {
        return id.endsWith('To') || id.startsWith('end') || id.startsWith('to');
    }
}
