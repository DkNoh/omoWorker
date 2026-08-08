/**
 * @fileoverview tui-page-builder.js — 목록 화면의 TUI Grid 조회 생명주기 Facade.
 */

/**
 * 목록/CRUD 백오피스 화면의 공통 Grid 조회 흐름을 구성한다.
 *
 * 목록/CRUD 백오피스 화면에서 반복되는 TUI Grid 생성, 검색조건 직렬화, axios 조회,
 * offset 행 번호, 수동 페이징, 날짜 선택기, 공통 버튼 이벤트를 하나의 진입점으로 묶은 화면 Facade다.
 * 화면별 JS는 컬럼 정의와 업무 이벤트만 구성하고 공통 조회 생명주기는 이 클래스에 위임한다.
 *
 * 전역 의존:
 *   - tui.Grid, 선택적으로 tui.DatePicker
 *   - axios + http-client.js 전역 interceptor(ApiResponse 언래핑/스피너/공통 오류)
 *   - dayjs, TuiCommon, CommonUtils
 *   - window.PAGE_AUTH.download: Grid context menu의 다운로드 가능 범위 판정
 *
 * 서버 응답 계약:
 *   `{contents, page, size, totalCount, totalPages}` 형태의 PageResponseDTO를 사용한다.
 *
 * 생성자 부수효과:
 *   날짜/기본 검색값 적용 → DatePicker/Grid 생성 → 이벤트 연결 → 1페이지 자동 조회 순으로 즉시 실행된다.
 *
 * @class TuiPageBuilder
 */
class TuiPageBuilder {
    
    /**
     * TuiPageBuilder 인스턴스를 생성하고 내부 구성 요소들을 초기화합니다.
     * @param {Object} config - 화면별 고유 설정값
     * @param {string} config.el - TUI Grid가 렌더링될 DOM 요소의 ID (기본값: 'grid')
     * @param {string} config.apiUrl - 데이터를 조회할 백엔드 API URL (필수)
     * @param {Array<string>} config.searchInputs - 검색 조건으로 사용할 HTML input/select 요소들의 ID 배열
     * @param {Array<string>} config.requiredInputs - 조회 전 값이 반드시 필요한 검색 input ID 배열
     * @param {Array<string>} config.rowHeaders - 그리드 좌측 헤더 설정 (예: ['rowNum'], 다중선택시 ['checkbox', 'rowNum'])
     * @param {Array<Object>} config.columns - TUI Grid의 컬럼 메타데이터 배열
     * @param {string} config.pageSizeEl - 페이지당 건수를 조절하는 select 요소의 ID (기본값: 'pageSizeSelect')
     * @param {string} config.btnSearch - 검색 조회 버튼의 ID (기본값: 'btn-search')
     * @param {string} config.btnReset - 검색 조건 초기화 버튼의 ID (기본값: 'btn-reset')
     * @param {string} config.paginationId - 수동 pagination 컨테이너 ID
     * @param {string} config.totalCountSelector - 총 건수를 표시할 요소 selector
     * @param {Object} config.gridOptions - TuiCommon.gridDefaults를 덮어쓸 화면별 TUI Grid 옵션
     * @param {Object<string,string>} config.searchDefaults - 검색 input ID별 기본값 코드 또는 실제 값
     * @param {Array<string>|null} config.datePickerInputs - DatePicker 대상 ID. null이면 날짜 검색조건 자동 감지
     * @param {Function|null} config.onGridUpdated - 데이터 갱신 후 PageResponseDTO로 호출할 callback
     */
    constructor(config) {
        /* 화면 설정을 공통 기본값 위에 병합한다. */
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
            gridOptions: {},
            searchDefaults: {},
            datePickerInputs: null,
            onGridUpdated: null
        }, config);

        /* Grid, 페이지, DatePicker 상태는 builder 인스턴스별로 독립 관리한다. */
        this.grid = null;           
        this.currentPage = 1;       
        this.currentSize = 10;      
        this.usesOffsetRowNo = false;
        this.searchDatePickers = {};

        /* 페이지 크기 select의 초기 선택값을 첫 조회 size로 사용한다. */
        const sizeEl = document.getElementById(this.config.pageSizeEl);
        if (sizeEl) this.currentSize = parseInt(sizeEl.value, 10);

        /* 공통 날짜를 먼저 채운 뒤 화면별 searchDefaults를 적용한다. */
        if (typeof CommonUtils !== 'undefined' && typeof CommonUtils.setDefaultDateTime === 'function') {
            CommonUtils.setDefaultDateTime();
        }
        this._applySearchDefaults(false);
        this._initSearchDatePickers();

        /* Grid 생성 후 공통 화면 이벤트를 연결한다. */
        this._initGrid();
        this._bindEvents();

        /* 초기화가 끝나면 첫 페이지를 자동 조회한다. */
        this.searchData(1);
    }

    /**
     * 공통/화면별 옵션을 병합하고 TUI Grid 인스턴스를 생성한다.
     * `rowNum`은 TUI 기본 행 번호 대신 서버 페이지 offset을 반영한 데이터 컬럼으로 교체한다.
     * @private
     */
    _initGrid() {
        /* TuiCommon.gridDefaults를 기준으로 화면별 gridOptions를 덮어쓴다. */
        const defaults = (typeof TuiCommon !== 'undefined' && TuiCommon.gridDefaults)
            ? TuiCommon.gridDefaults
            : { scrollX: false, scrollY: false, minBodyHeight: 300 };

        /* rowNum 요청 여부를 기억한 뒤 TUI 기본 row header에서는 제거한다. */
        const requestedRowHeaders = this.config.rowHeaders || [];
        this.usesOffsetRowNo = requestedRowHeaders.some(rh => this._isRowNumHeader(rh));
        const rowHeaders = requestedRowHeaders.filter(rh => !this._isRowNumHeader(rh));
        /* 화면이 rowNo 컬럼을 직접 정의했다면 공통 컬럼을 중복 삽입하지 않는다. */
        const columns = this.usesOffsetRowNo && !this.config.columns.some(col => col.name === 'rowNo')
            ? [this._rowNoColumn()].concat(this.config.columns)
            : this.config.columns;

        const mergedOptions = Object.assign({}, defaults, this.config.gridOptions, {
            el: document.getElementById(this.config.el),
            rowHeaders: rowHeaders,
            columns: columns
        });

        /* DOWNLOAD 권한이 true가 아니면 화면 설정보다 copy-only context menu를 우선한다. */
        if (!this._hasDownloadPermission()) {
            mergedOptions.contextMenu = this._copyOnlyContextMenu();
        }

        this.grid = new tui.Grid(mergedOptions);

        this._toggleEmptyState(0);

    }

    /**
     * [내부 메서드] PAGE_AUTH.download === true 인지 strict하게 판단한다.
     * @returns {boolean}
     */
    _hasDownloadPermission() {
        return !!(window.PAGE_AUTH && window.PAGE_AUTH.download === true);
    }

    /**
     * [내부 메서드] 복사 전용 contextMenu 콜백을 생성한다.
     * TUI Grid 4.x MenuItem[][] 형식: [{name, label, action}]
     * @returns {Function}
     */
    _copyOnlyContextMenu() {
        return function () {
            return [
                [
                    { name: 'copy', label: '복사', action: 'copy' },
                    { name: 'copyColumns', label: '열 복사', action: 'copyColumns' },
                    { name: 'copyRows', label: '행 복사', action: 'copyRows' }
                ]
            ];
        };
    }

    /**
     * Grid 부모 안의 `[data-empty-state]` 안내 레이어 표시 여부를 총 건수와 동기화한다.
     * 해당 markup이 없는 화면에서는 아무 작업도 하지 않는다.
     * @param {number} totalCount 서버 totalCount 또는 현재 contents 길이
     * @private
     */
    _toggleEmptyState(totalCount) {
        const shell = document.getElementById(this.config.el)?.parentElement;
        if (!shell) return;
        const empty = shell.querySelector('[data-empty-state]');
        if (!empty) return;
        const isEmpty = !totalCount || totalCount <= 0;
        empty.classList.toggle('is-visible', isEmpty);
    }

    /**
     * [퍼블릭 메서드] 설정된 API URL로 검색 조건과 페이징 정보를 담아 axios GET 조회를 수행한다.
     * @param {number} page - 조회할 페이지 번호 (기본값: 1)
     * @returns {void} 결과는 비동기로 Grid에 반영한다. 검증 실패 시 요청하지 않는다.
     */
    searchData(page = 1) {
        /* 필수 검색조건이 비어 있으면 API 호출 전에 중단하고 해당 필드로 포커스를 이동한다. */
        if (this.config.requiredInputs && this.config.requiredInputs.length > 0) {
            for (const id of this.config.requiredInputs) {
                const el = document.getElementById(id);
                if (el && !el.value.trim()) {
                    if (typeof CommonUtils !== 'undefined') {
                        CommonUtils.toast('필수 검색 조건을 입력해 주세요.', 'warning');
                    } else if (window.Notify) {
                        window.Notify.alert('필수 검색 조건을 입력해 주세요.');
                    }
                    el.focus();
                    return;
                }
            }
        }

        /* 공통 시작일과 종료일이 모두 있으면 역전된 검색 범위를 차단한다. */
        const startDateEl = document.getElementById('startDate');
        const endDateEl = document.getElementById('endDate');
        if (startDateEl && endDateEl && startDateEl.value && endDateEl.value) {
            if (dayjs(startDateEl.value).isAfter(dayjs(endDateEl.value))) {
                if (typeof CommonUtils !== 'undefined') {
                    CommonUtils.toast('시작일은 종료일보다 클 수 없습니다.', 'warning');
                } else if (window.Notify) {
                    window.Notify.alert('시작일은 종료일보다 클 수 없습니다.');
                }
                startDateEl.focus();
                return;
            }
        }

        /* pagination callback과 재조회가 같은 기준을 사용하도록 요청 전에 현재 페이지를 갱신한다. */
        this.currentPage = page;
        
        /* 검색조건과 page/size를 URLSearchParams로 직렬화한다. */
        const params = this.getSearchParams({ includePaging: true });

        /* axios 전역 interceptor가 spinner, ApiResponse 언래핑, 오류 알림을 담당한다. */
        axios.get(this.config.apiUrl, { params })
        .then(response => {
            const page = response.data;
            /* 세션 만료 HTML은 interceptor가 처리하므로 PageResponseDTO 형태의 payload만 렌더링한다. */
            if (!page || typeof page !== 'object') return;

            const contents = this._withRowNo(page.contents || [], page.page || this.currentPage, page.size || this.currentSize);
            /* Grid pageState를 서버 응답과 맞춰 내부 페이징 상태도 일관되게 유지한다. */
            this.grid.resetData(contents, {
                pageState: {
                    page: page.page || this.currentPage,
                    totalCount: page.totalCount || contents.length,
                    perPage: page.size || this.currentSize
                }
            });

            this._toggleEmptyState(page.totalCount || contents.length);

            /* 결과가 없으면 빈 상태 표시와 함께 사용자에게 toast로 알린다. */
            if (contents.length === 0 && typeof CommonUtils !== 'undefined') {
                CommonUtils.toast('조회된 데이터가 없습니다.', 'info');
            }

            /* TuiCommon이 있으면 총 건수와 수동 pagination을 서버 응답에 맞춰 갱신한다. */
            if (typeof TuiCommon !== 'undefined') {
                TuiCommon.updateTotalCount(page.totalCount || 0, this.config.totalCountSelector);
                TuiCommon.renderPagination(
                    page.page || 1,
                    page.totalPages || 1,
                    (p) => this.searchData(p),
                    this.config.paginationId
                );
            }

            /* 버튼 상태나 행 이벤트 같은 화면별 후처리는 공통 렌더 완료 후 실행한다. */
            if (this.config.onGridUpdated) {
                this.config.onGridUpdated(page);
            }
        })
        .catch(err => {
            /* 사용자 오류 알림은 interceptor가 표시하므로 이 계층에서는 진단 로그만 남긴다. */
            console.error('[TuiPageBuilder] 목록 조회 실패', err);
        });
    }

    /**
     * [내부 메서드] 화면 내 공통 툴바 버튼들(조회, 초기화, 사이즈 변경)에 이벤트를 바인딩합니다.
     * @private
     */
    _bindEvents() {
        /* 조회 버튼은 검색조건을 유지한 채 첫 페이지를 조회한다. */
        const btnSearch = document.getElementById(this.config.btnSearch);
        if (btnSearch) {
            btnSearch.addEventListener('click', () => this.searchData(1));
        }

        /* 초기화 버튼은 검색조건, 기본값, DatePicker를 동기화한 뒤 첫 페이지를 조회한다. */
        const btnReset = document.getElementById(this.config.btnReset);
        if (btnReset) {
            btnReset.addEventListener('click', () => {
                /* builder에 등록된 input/select/radio를 각 control 유형의 초기 상태로 되돌린다. */
                this.config.searchInputs.forEach(id => {
                    const el = document.getElementById(id);
                    if (el && el.tagName !== 'SELECT') el.value = '';
                    if (el && el.tagName === 'SELECT') el.selectedIndex = 0;
                    document.querySelectorAll(`input[name="${id}"]`).forEach((radio, index) => {
                        radio.checked = index === 0;
                    });
                });
                this._applySearchDefaults(true);
                
                /* 레거시 공통 검색 필드가 있으면 CommonUtils 초기화도 이어서 수행한다. */
                if (typeof CommonUtils !== 'undefined' && typeof CommonUtils.resetFields === 'function') {
                    CommonUtils.resetFields();
                }
                this._syncSearchDatePickers();
                
                /* 초기화된 조건으로 첫 페이지를 다시 조회한다. */
                this.searchData(1);
            });
        }

        /* 페이지 크기가 바뀌면 현재 페이지를 버리고 첫 페이지부터 다시 조회한다. */
        const pageSizeEl = document.getElementById(this.config.pageSizeEl);
        if (pageSizeEl) {
            pageSizeEl.addEventListener('change', (e) => {
                this.currentSize = parseInt(e.target.value, 10);
                this.searchData(1);
            });
        }

        /* 검색 input의 Enter는 form submit 대신 첫 페이지 조회로 처리한다. */
        if (this.config.searchInputs && this.config.searchInputs.length > 0) {
            this.config.searchInputs.forEach(id => {
                const el = document.getElementById(id);
                if (el && el.tagName === 'INPUT') {
                    el.addEventListener('keypress', (e) => {
                        if (e.key === 'Enter') {
                            e.preventDefault();
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

    /**
     * 현재 검색 DOM 값을 URLSearchParams로 직렬화한다.
     * 날짜는 서버 검색 DTO 계약에 맞춰 구분자 없는 `YYYYMMDD`, datetime은 `YYYYMMDDHHmm`로 변환한다.
     *
     * @param {Object} [options]
     * @param {boolean} [options.includePaging=false] page/size 포함 여부
     * @returns {URLSearchParams} axios params로 바로 전달할 객체
     */
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

    /** 문자열/객체형 row header 설정이 offset 행 번호 요청인지 판단한다. @private */
    _isRowNumHeader(rowHeader) {
        return rowHeader === 'rowNum' || (rowHeader && rowHeader.type === 'rowNum');
    }

    /** 서버 페이지 offset을 반영한 `No.` 데이터 컬럼 정의를 만든다. @private */
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

    /**
     * rowNum을 요청한 화면에서 원본 row를 변경하지 않고 복사본에 1-based 전체 순번을 추가한다.
     * 예: 2페이지, size 10의 첫 행은 rowNo 11.
     * @private
     */
    _withRowNo(contents, page, size) {
        if (!this.usesOffsetRowNo) {
            return contents;
        }
        const offset = (Number(page || 1) - 1) * Number(size || this.currentSize || 10);
        return contents.map((row, index) => Object.assign({}, row, { rowNo: offset + index + 1 }));
    }

    /**
     * 검색 날짜 input과 `${id}PickerLayer`를 연결해 TUI DatePicker 인스턴스를 만든다.
     * 라이브러리나 필수 DOM이 없는 화면은 일반 input만 사용하도록 조용히 건너뛴다.
     * @private
     */
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

    /**
     * 명시적 datePickerInputs가 있으면 우선하고, 없으면 searchInputs의 날짜 필드를 자동 감지한다.
     * 중복 ID는 최초 항목만 유지한다.
     * @returns {Array<string>} DatePicker를 생성할 input ID 목록
     * @private
     */
    _datePickerInputIds() {
        const configured = Array.isArray(this.config.datePickerInputs) ? this.config.datePickerInputs : [];
        const source = configured.length > 0 ? configured : this.config.searchInputs;
        return source.filter((id, index, ids) =>
            ids.indexOf(id) === index && this._isDateSearchInput(id)
        );
    }

    /** native date type 또는 `data-search-type=date`인지 확인한다. @private */
    _isDateSearchInput(id) {
        const el = document.getElementById(id);
        return !!(el && (el.type === 'date' || el.dataset.searchType === 'date'));
    }

    /** input 문자열을 TUI DatePicker가 받는 Date로 변환하며 빈 값/잘못된 값은 null로 반환한다. @private */
    _toDatePickerDate(value) {
        if (!value || typeof dayjs === 'undefined') {
            return null;
        }
        const parsed = dayjs(value);
        return parsed.isValid() ? parsed.toDate() : null;
    }

    /**
     * 초기화 등으로 input.value를 직접 바꾼 뒤 DatePicker 내부 선택 날짜도 같은 값으로 맞춘다.
     * 빈 값에서는 지원 버전에 setNull이 있을 때만 선택을 해제한다.
     * @private
     */
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

    /**
     * ID 기반 input/select를 우선 읽고, 없으면 같은 name의 checked radio 값을 읽는다.
     * 날짜/일시는 서버 DTO용 compact 형식으로 정규화한다.
     * @returns {string} 필드가 없거나 선택되지 않았으면 빈 문자열
     * @private
     */
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

    /**
     * searchDefaults를 input/select/radio에 적용한다.
     * forceReset=false면 기존 사용자/서버 값을 보존하고 빈 input만 채운다.
     * @param {boolean} forceReset 기존 값을 기본값으로 덮어쓸지 여부
     * @private
     */
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

    /**
     * TODAY/YESTERDAY/RECENT_7_DAYS/THIS_MONTH 코드를 실제 날짜 문자열로 계산한다.
     * 알려지지 않은 코드는 select/radio 등 비날짜 기본값으로 보고 원문을 그대로 반환한다.
     * @private
     */
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

    /** ID naming convention으로 기간 검색의 종료 필드인지 판단한다. @private */
    _isRangeEnd(id) {
        return id.endsWith('To') || id.startsWith('end') || id.startsWith('to');
    }
}
