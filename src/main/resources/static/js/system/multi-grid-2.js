// 다중 그리드(2 그리드) 정적 샘플 — 부서 목록(마스터) + 부서원 목록(디테일)
//
// 화면 동작 요약
// 1. DOMContentLoaded 후 TuiPageBuilder 인스턴스 2개가 각자 고유 ID 로 그리드를 초기화한다.
//    - 부서 그리드: deptGrid / deptPagination / #deptTotalCount / deptPageSize
//    - 사원 그리드: empGrid  / empPagination  / #empTotalCount  / empPageSize
// 2. 부서 행을 클릭하면 숨겨진 검색조건(#searchDepId)에 부서코드를 넣고 사원 그리드를 재조회한다.
//
// 이 샘플의 endpoint 는 실제 존재하지 않는다. file://·서버(http) 어느 쪽으로 열어도
// 오류 모달이 뜨지 않도록 axios.get 을 URL 별로 분기하는 mock 서버로 항상 대체한다.
// 조회/페이징 흐름은 TuiPageBuilder 의 실제 코드 경로를 그대로 탄다.
// 실제 화면 전환 시 MOCK_* 데이터와 mockListResponse 를 실제 API 호출로 교체한다.
(function () {
    'use strict';

    const API = {
        dept: '/system/multi-grid-2/dept/data',
        emp: '/system/multi-grid-2/emp/data'
    };

    const MOCK_DEPTS = [
        { depId: 'D001', depCode: 'DEPT-1001', depNm: '인사팀',       empCnt: 12, regDttm: '2025-09-12 10:24:05' },
        { depId: 'D002', depCode: 'DEPT-1002', depNm: '재무회계팀',   empCnt: 8,  regDttm: '2025-09-12 10:31:48' },
        { depId: 'D003', depCode: 'DEPT-1003', depNm: 'IT운영팀',     empCnt: 15, regDttm: '2025-10-02 14:05:12' },
        { depId: 'D004', depCode: 'DEPT-1004', depNm: '마케팅팀',     empCnt: 9,  regDttm: '2025-11-21 09:47:30' },
        { depId: 'D005', depCode: 'DEPT-1005', depNm: '고객지원센터', empCnt: 20, regDttm: '2026-01-08 16:12:54' },
        { depId: 'D006', depCode: 'DEPT-1006', depNm: '감사실',       empCnt: 5,  regDttm: '2026-03-15 11:38:21' }
    ];

    const MOCK_EMPS = [
        { empId: 'E1001', empNm: '김민준', depCode: 'DEPT-1001', depNm: '인사팀',       position: '과장',   joinDt: '2018-03-02' },
        { empId: 'E1002', empNm: '이서연', depCode: 'DEPT-1001', depNm: '인사팀',       position: '대리',   joinDt: '2020-07-15' },
        { empId: 'E1003', empNm: '박지호', depCode: 'DEPT-1001', depNm: '인사팀',       position: '사원',   joinDt: '2023-01-09' },
        { empId: 'E2001', empNm: '최하은', depCode: 'DEPT-1002', depNm: '재무회계팀',   position: '차장',   joinDt: '2016-05-23' },
        { empId: 'E2002', empNm: '정우진', depCode: 'DEPT-1002', depNm: '재무회계팀',   position: '과장',   joinDt: '2019-09-30' },
        { empId: 'E3001', empNm: '한지민', depCode: 'DEPT-1003', depNm: 'IT운영팀',     position: '부장',   joinDt: '2014-02-17' },
        { empId: 'E3002', empNm: '서준혁', depCode: 'DEPT-1003', depNm: 'IT운영팀',     position: '과장',   joinDt: '2018-11-05' },
        { empId: 'E3003', empNm: '오세라', depCode: 'DEPT-1003', depNm: 'IT운영팀',     position: '대리',   joinDt: '2021-04-12' },
        { empId: 'E3004', empNm: '임도현', depCode: 'DEPT-1003', depNm: 'IT운영팀',     position: '사원',   joinDt: '2024-08-19' },
        { empId: 'E4001', empNm: '강다현', depCode: 'DEPT-1004', depNm: '마케팅팀',     position: '차장',   joinDt: '2017-06-08' },
        { empId: 'E4002', empNm: '조민재', depCode: 'DEPT-1004', depNm: '마케팅팀',     position: '대리',   joinDt: '2022-02-21' },
        { empId: 'E5001', empNm: '윤가은', depCode: 'DEPT-1005', depNm: '고객지원센터', position: '과장',   joinDt: '2019-03-11' },
        { empId: 'E5002', empNm: '장현우', depCode: 'DEPT-1005', depNm: '고객지원센터', position: '사원',   joinDt: '2023-10-02' },
        { empId: 'E6001', empNm: '신소율', depCode: 'DEPT-1006', depNm: '감사실',       position: '부장',   joinDt: '2015-01-26' }
    ];

    let deptBuilder = null;
    let empBuilder = null;

    document.addEventListener('DOMContentLoaded', init);

    // URL 로 부서/사원 요청을 구분해 PageResponseDTO 형태로 응답하는 mock 서버.
    // 사원 조회는 검색조건(searchDepId)이 있으면 해당 부서만 필터링한다.
    function mockListResponse(url, config) {
        const params = (config && config.params) || new URLSearchParams();
        const page = parseInt(params.get('page'), 10) || 1;
        const size = parseInt(params.get('size'), 10) || 10;

        const source = url.indexOf('/dept/') !== -1 ? MOCK_DEPTS : filterEmps(params.get('searchDepId'));

        const totalPages = Math.max(1, Math.ceil(source.length / size));
        const safePage = Math.min(page, totalPages);
        const start = (safePage - 1) * size;
        const contents = source.slice(start, start + size);

        return { data: { contents, page: safePage, size, totalCount: source.length, totalPages } };
    }

    function filterEmps(depId) {
        if (!depId) return MOCK_EMPS;
        return MOCK_EMPS.filter(emp => emp.depId === depId || emp.depCode === depId);
    }

    function init() {
        // endpoint 는 실제 존재하지 않으므로 프로토콜과 무관하게 mock 서버로 대체한다.
        axios.get = (url, config) => Promise.resolve(mockListResponse(url, config));

        // ── 그리드 1: 부서 목록 (마스터) ────────────────────────────────
        deptBuilder = new TuiPageBuilder({
            el: 'deptGrid',
            apiUrl: API.dept,
            paginationId: 'deptPagination',
            totalCountSelector: '#deptTotalCount',
            pageSizeEl: 'deptPageSize',
            searchInputs: [],
            rowHeaders: ['rowNum'],
            columns: [
                { header: '부서코드', name: 'depCode', align: 'center', width: 120 },
                { header: '부서명', name: 'depNm', align: 'left', minWidth: 140 },
                { header: '인원수', name: 'empCnt', align: 'center', width: 80 },
                { header: '등록일시', name: 'regDttm', align: 'center', width: 150, formatter: TuiCommon.fmt.date }
            ]
        });

        // ── 그리드 2: 부서원 목록 (디테일) ──────────────────────────────
        empBuilder = new TuiPageBuilder({
            el: 'empGrid',
            apiUrl: API.emp,
            paginationId: 'empPagination',
            totalCountSelector: '#empTotalCount',
            pageSizeEl: 'empPageSize',
            // 숨겨진 searchDepId 가 부서 행 클릭 시 주입되어 사원 조회의 필터가 된다.
            searchInputs: ['searchDepId'],
            rowHeaders: ['rowNum'],
            columns: [
                { header: '사번', name: 'empId', align: 'center', width: 90 },
                { header: '성명', name: 'empNm', align: 'center', width: 90 },
                { header: '부서', name: 'depNm', align: 'left', minWidth: 120 },
                { header: '직급', name: 'position', align: 'center', width: 80 },
                { header: '입사일', name: 'joinDt', align: 'center', width: 110, formatter: TuiCommon.fmt.date }
            ]
        });

        // 부서 행 클릭 → 선택 부서코드를 숨겨진 검색조건에 주입 → 사원 그리드 재조회
        const deptGrid = deptBuilder.getGrid();
        deptGrid.on('click', (ev) => {
            if (ev.rowKey === null || ev.rowKey === undefined) return;
            const row = deptGrid.getRow(ev.rowKey);
            if (!row) return;

            document.getElementById('searchDepId').value = row.depCode;
            document.getElementById('emp-grid-title').textContent = row.depNm + ' 사원';
            empBuilder.searchData(1);
        });

        CommonUtils.refreshIcons();
    }
})();
