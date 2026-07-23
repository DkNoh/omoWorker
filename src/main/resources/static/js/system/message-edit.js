(function () {
    'use strict';

    const API = {
        detail: '/system/message/detail',
        update: '/system/message/update'
    };

    const MESSAGE_BYTE_LIMIT = 2600;
    let startDatePicker = null;

    document.addEventListener('DOMContentLoaded', init);

    async function init() {
        initStartDatePicker();
        FieldFormat.applyFieldFormats(document.getElementById('message-edit-form'));
        bindEvents();
        refreshMessageState();
        CommonUtils.refreshIcons();

        const messageCode = new URLSearchParams(window.location.search).get('messageCode');
        if (messageCode) {
            await loadDetail(messageCode);
        }
    }

    function initStartDatePicker() {
        const input = document.getElementById('startDate');
        const layer = document.getElementById('startDatePickerLayer');
        const initial = toDate(input.value);

        if (!window.tui || !window.tui.DatePicker) {
            return;
        }

        startDatePicker = new tui.DatePicker(layer, {
            language: 'ko',
            date: initial,
            input: {
                element: input,
                format: 'yyyy-MM-dd'
            },
            calendar: {
                showToday: true
            }
        });
    }

    function bindEvents() {
        const form = document.getElementById('message-edit-form');

        form.addEventListener('submit', updateMessage);
        document.getElementById('btn-cancel').addEventListener('click', () => history.back());
        document.getElementById('messageContent').addEventListener('input', refreshCounter);
        document.querySelectorAll('input[name="messagePurpose"]').forEach((radio) => {
            radio.addEventListener('change', refreshPurposeWarning);
        });
    }

    async function loadDetail(messageCode) {
        try {
            const data = await ApiClient.get(API.detail, { messageCode });
            if (!data) {
                return;
            }

            FormBinder.bind('#message-edit-form', data);
            document.getElementById('messageCodeDisplay').value = data.messageCode || '';

            if (startDatePicker && data.startDate) {
                const date = toDate(data.startDate);
                if (date) {
                    startDatePicker.setDate(date);
                }
            }
            refreshMessageState();
        } catch (error) {
            console.error('메시지 상세 조회 실패', error);
        }
    }

    async function updateMessage(event) {
        event.preventDefault();
        const form = event.currentTarget;

        if (!document.getElementById('messageCode').value) {
            CommonUtils.toast('수정할 메시지 정보를 먼저 조회하세요.', 'warning');
            return;
        }

        if (!form.reportValidity()) {
            return;
        }

        const valid = await FieldFormat.validateForm(form);
        if (!valid) {
            return;
        }

        const messageContent = document.getElementById('messageContent').value;
        if (utf8ByteLength(messageContent) > MESSAGE_BYTE_LIMIT) {
            CommonUtils.toast('메시지 내용은 2,600byte를 초과할 수 없습니다.', 'warning');
            return;
        }

        const purpose = document.querySelector('input[name="messagePurpose"]:checked');
        if (purpose && purpose.value === 'ADVERTISEMENT') {
            CommonUtils.toast('광고성 메시지는 광고성 메시지 관리 화면에서 등록하세요.', 'warning');
            return;
        }

        CommonUtils.confirm('메시지 정보를 저장하시겠습니까?', async function () {
            try {
                await ApiClient.post(API.update, FormBinder.toObject('#message-edit-form'));
                CommonUtils.toast('수정되었습니다.', 'success');
            } catch (error) {
                console.error('메시지 수정 실패', error);
            }
        });
    }

    function refreshMessageState() {
        refreshCounter();
        refreshPurposeWarning();
    }

    function refreshCounter() {
        const content = document.getElementById('messageContent').value;
        const byteLength = utf8ByteLength(content);
        const counter = document.getElementById('message-counter');

        document.getElementById('message-byte-count').textContent = byteLength.toLocaleString('ko-KR');
        counter.classList.toggle('is-over', byteLength > MESSAGE_BYTE_LIMIT);
    }

    function refreshPurposeWarning() {
        const selected = document.querySelector('input[name="messagePurpose"]:checked');
        const warning = document.getElementById('advertisement-warning');
        warning.classList.toggle('d-none', !selected || selected.value !== 'ADVERTISEMENT');
    }

    function toDate(value) {
        if (!value || typeof dayjs === 'undefined') {
            return null;
        }
        const parsed = dayjs(value);
        return parsed.isValid() ? parsed.toDate() : null;
    }

    function utf8ByteLength(value) {
        return new TextEncoder().encode(value).length;
    }
})();
