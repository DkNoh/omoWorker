/**
 * @class TuiPageBuilder
 * @description 
 * 단순 CRUD(목록 조회 위주) 백오피스 화면에서 반복적으로 작성되는 TUI Grid 초기화, 페이징 로직, 비동기 통신(fetch), 
 * 그리고 공통 버튼 이벤트(조회, 초기화, 페이지 사이즈 변경)를 단일 진입점(Facade)에서 자동화해 주는 래퍼(Wrapper) 엔진입니다.
 * 이 클래스를 사용하면 각 화면별 JS 파일에서는 고유한 컬럼 정의와 업무별 상호작용만 작성하면 됩니다.
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
            paginationId: 'pagination',
            totalCountSelector: '#total-count',
            gridOptions: {},          // 화면별 그리드 옵션 예외 (gridDefaults 위에 덮어씀)
            searchDefaults: {},       // 검색조건 기본값. 예: { startDt: 'THIS_MONTH', endDt: 'TODAY' }
            datePickerInputs: null,   // 비우면 data-search-type="date" 검색조건을 자동 감지한다
            onGridUpdated: null
        }, config);

        // 2. 내부 상태 변수 초기화
        this.grid = null;           
        this.currentPage = 1;       
        this.currentSize = 10;      
        this.usesOffsetRowNo = false;
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

    }

    _toggleEmptyState(totalCount) {
        const shell = document.getElementById(this.config.el)?.parentElement;
        if (!shell) return;
        const empty = shell.querySelector('[data-empty-state]');
        if (!empty) return;
        const isEmpty = !totalCount || totalCount <= 0;
        empty.classList.toggle('is-visible', isEmpty);
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
     * 현재 마우스 클릭으로 포커싱된 단일 셀의 정보를 반환합니다.
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
