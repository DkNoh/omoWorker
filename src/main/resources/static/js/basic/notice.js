// 수동 커스텀: scaffold 생성 후 window.open 수정 팝업을 추가한 예제다.
document.addEventListener('DOMContentLoaded', function () {
    'use strict';

    const POPUP_URL = '/basic/notice/popup';
    const POPUP_NAME = 'noticeEditPopup';
    const POPUP_FEATURES = 'width=1100,height=850,resizable=yes,scrollbars=yes';

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

    const openPopup = (noticeId) => {
        const query = noticeId === null || noticeId === undefined
            ? ''
            : `?noticeId=${encodeURIComponent(noticeId)}`;
        window.open(`${POPUP_URL}${query}`, POPUP_NAME, POPUP_FEATURES);
    };

    const grid = pageBuilder.getGrid();
    grid.on('dblclick', (ev) => {
        if (ev.rowKey === null || ev.rowKey === undefined) return;
        const row = grid.getRow(ev.rowKey);
        if (row && row.noticeId !== null && row.noticeId !== undefined) {
            openPopup(row.noticeId);
        }
    });

    const createButton = document.getElementById('btn-create');
    if (createButton) {
        createButton.addEventListener('click', () => openPopup(null));
    }

    window.addEventListener('message', async (ev) => {
        if (ev.origin !== window.location.origin) return;
        if (!ev.data || ev.data.action !== 'noticeSaved') return;

        await pageBuilder.searchData(pageBuilder.currentPage || 1);
        CommonUtils.toast('저장되었습니다.', 'success');
    });
});
