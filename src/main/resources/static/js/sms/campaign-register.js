(function () {
    'use strict';

    const API = {
        create: '/sms/campaign/create'
    };

    const MESSAGE_LIMIT = 1000;
    let sendDatePicker = null;

    document.addEventListener('DOMContentLoaded', init);

    function init() {
        initTimeSelects();
        initSendDatePicker();
        FieldFormat.applyFieldFormats(document.getElementById('campaign-form'));
        bindEvents();
        refreshPreview();
        CommonUtils.refreshIcons();
    }

    function initTimeSelects() {
        const initial = defaultSendAt();
        const hourSelect = document.getElementById('sendHour');
        const minuteSelect = document.getElementById('sendMinute');

        for (let hour = 0; hour < 24; hour += 1) {
            const value = String(hour).padStart(2, '0');
            hourSelect.add(new Option(value, value));
        }

        for (let minute = 0; minute < 60; minute += 5) {
            const value = String(minute).padStart(2, '0');
            minuteSelect.add(new Option(value, value));
        }

        hourSelect.value = initial.format('HH');
        minuteSelect.value = initial.format('mm');
    }

    function initSendDatePicker() {
        const input = document.getElementById('sendDate');
        const layer = document.getElementById('sendDatePickerLayer');
        const initial = defaultSendAt();

        input.value = initial.format('YYYY-MM-DD');

        if (!window.tui || !window.tui.DatePicker) {
            return;
        }

        sendDatePicker = new tui.DatePicker(layer, {
            language: 'ko',
            date: initial.toDate(),
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
        const form = document.getElementById('campaign-form');

        form.addEventListener('submit', submitCampaign);
        document.getElementById('btn-reset').addEventListener('click', resetForm);
        document.getElementById('messageTemplateId').addEventListener('change', applyTemplate);
        document.getElementById('messageContent').addEventListener('input', refreshPreview);
        document.getElementById('callbackNumber').addEventListener('input', refreshPreview);
        document.getElementById('sendDate').addEventListener('change', refreshPreview);
        document.getElementById('sendHour').addEventListener('change', refreshPreview);
        document.getElementById('sendMinute').addEventListener('change', refreshPreview);
    }

    function applyTemplate(event) {
        const selected = event.target.selectedOptions[0];
        const messageContent = document.getElementById('messageContent');
        const sendType = document.getElementById('sendType');

        if (selected && selected.dataset.content) {
            messageContent.value = selected.dataset.content;
        }
        sendType.value = selected && selected.dataset.sendType
            ? selected.dataset.sendType
            : 'SMS';
        refreshPreview();
    }

    function refreshPreview() {
        const content = document.getElementById('messageContent').value;
        const callback = document.getElementById('callbackNumber').value;
        const previewMessage = document.getElementById('preview-message');
        const counter = document.getElementById('message-counter');

        previewMessage.textContent = content || '템플릿을 선택하거나 메시지 내용을 입력하세요.';
        previewMessage.classList.toggle('is-empty', !content);
        document.getElementById('preview-callback').textContent = callback || '회신번호 미입력';
        document.getElementById('message-char-count').textContent = content.length.toLocaleString('ko-KR');
        document.getElementById('message-byte-count').textContent = utf8ByteLength(content).toLocaleString('ko-KR');
        counter.classList.toggle('is-over', content.length > MESSAGE_LIMIT);

        const sendAt = readSendDateTime();
        document.getElementById('preview-send-time').textContent = sendAt && sendAt.isValid()
            ? sendAt.format('YYYY-MM-DD HH:mm 발송 예정')
            : '발송 예정';
    }

    async function submitCampaign(event) {
        event.preventDefault();

        const form = event.currentTarget;
        syncSendAt();

        if (!form.reportValidity()) {
            return;
        }

        const valid = await FieldFormat.validateForm(form);
        if (!valid) {
            return;
        }

        const sendAt = readSendDateTime();
        if (!sendAt || !sendAt.isValid() || !sendAt.isAfter(dayjs())) {
            CommonUtils.toast('전송일시는 현재 시각 이후로 선택하세요.', 'warning');
            return;
        }

        CommonUtils.confirm('캠페인을 등록하시겠습니까?', async function () {
            try {
                await ApiClient.post(API.create, FormBinder.toObject('#campaign-form'));
                CommonUtils.toast('등록되었습니다.', 'success');
                resetForm();
            } catch (error) {
                console.error('캠페인 등록 실패', error);
            }
        });
    }

    function syncSendAt() {
        const sendAt = readSendDateTime();
        document.getElementById('sendAt').value = sendAt && sendAt.isValid()
            ? sendAt.format('YYYYMMDDHHmm')
            : '';
    }

    function readSendDateTime() {
        const date = document.getElementById('sendDate').value;
        const hour = document.getElementById('sendHour').value;
        const minute = document.getElementById('sendMinute').value;

        if (!date || hour === '' || minute === '') {
            return null;
        }
        return dayjs(`${date} ${hour}:${minute}`, 'YYYY-MM-DD HH:mm');
    }

    function resetForm() {
        const form = document.getElementById('campaign-form');
        const initial = defaultSendAt();

        form.reset();
        document.getElementById('sendDate').value = initial.format('YYYY-MM-DD');
        document.getElementById('sendHour').value = initial.format('HH');
        document.getElementById('sendMinute').value = initial.format('mm');
        document.getElementById('sendType').value = 'SMS';
        document.getElementById('sendAt').value = '';

        if (sendDatePicker) {
            sendDatePicker.setDate(initial.toDate());
        }
        refreshPreview();
    }

    function utf8ByteLength(value) {
        return new TextEncoder().encode(value).length;
    }

    function defaultSendAt() {
        const initial = dayjs().add(10, 'minute').second(0).millisecond(0);
        const remainder = initial.minute() % 5;
        return remainder === 0 ? initial : initial.add(5 - remainder, 'minute');
    }
})();
