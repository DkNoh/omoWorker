// Scaffold 생성(LIST). 생성 후 개발자가 직접 수정해 소유한다.
document.addEventListener('DOMContentLoaded', function () {
    const pageBuilder = new TuiPageBuilder({
        el: 'grid',
        apiUrl: '/basic/code/data',
        searchInputs: ['searchkeyword', 'noticetype'],
        searchDefaults: {},
        rowHeaders: ['rowNum'],
        columns: [
            { header: 'ID', name: 'noticeId', align: 'center', width: 100 },
            { header: '제목', name: 'title', align: 'left', width: 250 },
            { header: '유형', name: 'noticeType', align: 'center', width: 100, formatter: TuiCommon.badgeByValue({ labels: { NOT: '공지', FAQ: 'FAQ' } }) },
            { header: '사용', name: 'useYn', align: 'center', width: 80, formatter: TuiCommon.badgeByValue({ labels: { Y: '사용', N: '미사용' } }) },
            { header: '등록일시', name: 'regDttm', align: 'center', width: 170, formatter: ({ value }) => TuiCommon.formatDate(value, 'YYYY-MM-DD HH:mm') }

        ]
    });
});
