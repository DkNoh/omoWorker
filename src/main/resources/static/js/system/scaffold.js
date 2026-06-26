document.addEventListener('DOMContentLoaded', function () {
    let results = {};
    let currentKey = null;
    let lastRequest = null;

    document.querySelector('#screenMode').addEventListener('change', syncModalOption);
    syncModalOption();

    document.querySelector('#btn-refresh-options').addEventListener('click', async () => {
        try {
            await renderOptionTables();
            CommonUtils.toast('조회조건/컬럼 옵션을 갱신했습니다.', 'info');
        } catch (error) {
            console.error('[scaffold] 옵션 갱신 실패', error);
        }
    });

    document.querySelector('#btn-generate').addEventListener('click', async () => {
        try {
            await ensureOptionsRendered();
            const request = buildRequest();
            const response = await axios.post('/system/scaffold/generate', request);
            const body = response.data;

            if (typeof body === 'string' || !body) {
                const finalUrl = response.request?.responseURL || '';
                console.error('[scaffold] 비정상 응답 — 최종 URL:', finalUrl);
                CommonUtils.toast(finalUrl.includes('/login')
                    ? '세션이 없어 로그인 페이지로 이동되었습니다. 다시 로그인하세요.'
                    : '응답이 올바르지 않습니다. F12 Console을 확인하세요.', 'error');
                return;
            }

            results = body;
            lastRequest = request;
            renderTabs();
            clearApplyResult();
            CommonUtils.toast('생성되었습니다. 코드를 검토 후 반영하세요.', 'success');
        } catch (error) {
            console.error('[scaffold] 생성 실패', error);
        }
    });

    document.querySelector('#btn-preview').addEventListener('click', async () => {
        if (!lastRequest) {
            CommonUtils.toast('먼저 코드를 생성하세요.', 'warning');
            return;
        }
        await previewApply(lastRequest);
    });

    document.querySelector('#btn-apply').addEventListener('click', async () => {
        if (!lastRequest || Object.keys(results).length === 0) {
            CommonUtils.toast('먼저 코드를 생성하세요.', 'warning');
            return;
        }

        const preview = await previewApply(lastRequest);
        if (!preview) {
            return;
        }

        const overwriteCount = preview.filter(item => item.status === 'OVERWRITE').length;
        const newCount = preview.filter(item => item.status === 'NEW').length;
        const message = `신규 ${newCount}건, 덮어쓰기 ${overwriteCount}건입니다. 적용할까요?`;
        if (!await confirmApply(message)) {
            return;
        }

        try {
            const response = await axios.post('/system/scaffold/apply', lastRequest);
            const appliedFiles = response.data;

            if (typeof appliedFiles === 'string' || !appliedFiles) {
                CommonUtils.toast('적용 응답이 올바르지 않습니다. F12 Console을 확인하세요.', 'error');
                return;
            }

            renderApplyResult(appliedFiles);
            CommonUtils.toast('생성 파일을 적용했습니다.', 'success');
        } catch (error) {
            console.error('[scaffold] 적용 실패', error);
        }
    });

    document.querySelector('#btn-copy').addEventListener('click', async () => {
        if (!currentKey) {
            return;
        }
        await navigator.clipboard.writeText(results[currentKey]);
        CommonUtils.toast(currentKey + ' 내용을 복사했습니다.', 'info');
    });

    function buildRequest() {
        const screenMode = document.querySelector('#screenMode').value;
        return {
            moduleName: document.querySelector('#moduleName').value.trim(),
            domainId: document.querySelector('#domainId').value.trim(),
            domainClass: document.querySelector('#domainClass').value.trim(),
            domainName: document.querySelector('#domainName').value.trim(),
            rawQuery: document.querySelector('#rawQuery').value,
            orderBy: document.querySelector('#orderBy').value.trim(),
            includeCreateUpdate: screenMode === 'CRUD',
            includeExcel: screenMode === 'EXCEL',
            includeModal: document.querySelector('#includeModal').checked,
            includePrivacy: document.querySelector('#includePrivacy').checked,
            screenMode: screenMode,
            targetTable: document.querySelector('#targetTable').value.trim(),
            pkColumn: readPkColumns()[0] || '',
            pkColumns: readPkColumns(),
            lockColumn: document.querySelector('#lockColumn').value,
            searchParamOptions: readSearchParamOptions(),
            columnOptions: readColumnOptions(),
            menuOption: {
                menuId: document.querySelector('#menuId').value.trim(),
                parentMenuId: document.querySelector('#parentMenuId').value.trim(),
                roleCode: document.querySelector('#roleCode').value.trim(),
                sortOrd: Number(document.querySelector('#sortOrd').value || 99)
            }
        };
    }

    function renderTabs() {
        const tabContainer = document.querySelector('#result-tabs');
        tabContainer.innerHTML = '';
        Object.keys(results).forEach((key, index) => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'btn btn-sm ' + (index === 0 ? 'btn-dark' : 'btn-outline-dark');
            button.textContent = key;
            button.addEventListener('click', () => selectTab(key, button));
            tabContainer.appendChild(button);
        });
        document.querySelector('#result-card').classList.remove('d-none');
        const firstKey = Object.keys(results)[0];
        if (firstKey) {
            selectTab(firstKey, tabContainer.querySelector('button'));
        }
    }

    function syncModalOption() {
        const screenMode = document.querySelector('#screenMode').value;
        const includeModal = document.querySelector('#includeModal');
        const forced = screenMode === 'DETAIL' || screenMode === 'CRUD';
        const wasForced = includeModal.dataset.forced === 'true';
        if (forced) {
            includeModal.checked = true;
            includeModal.disabled = true;
            includeModal.dataset.forced = 'true';
            return;
        }
        includeModal.disabled = false;
        if (wasForced) {
            includeModal.checked = false;
        }
        includeModal.dataset.forced = 'false';
    }

    function confirmApply(message) {
        return new Promise(resolve => {
            if (typeof CommonUtils !== 'undefined' && CommonUtils.confirm) {
                CommonUtils.confirm(message, () => resolve(true), '적용 확인', () => resolve(false));
            } else {
                resolve(window.confirm(message));
            }
        });
    }

    function selectTab(key, button) {
        currentKey = key;
        document.querySelectorAll('#result-tabs button').forEach(b => {
            b.className = 'btn btn-sm btn-outline-dark';
        });
        button.className = 'btn btn-sm btn-dark';
        document.querySelector('#result-content').textContent = results[key];
    }

    async function previewApply(request) {
        try {
            const response = await axios.post('/system/scaffold/preview', request);
            const preview = response.data;
            if (typeof preview === 'string' || !preview) {
                CommonUtils.toast('미리보기 응답이 올바르지 않습니다. F12 Console을 확인하세요.', 'error');
                return null;
            }
            renderApplyResult(preview);
            return preview;
        } catch (error) {
            console.error('[scaffold] 적용 미리보기 실패', error);
            return null;
        }
    }

    function renderApplyResult(files) {
        const resultEl = document.querySelector('#apply-result');
        const listEl = document.querySelector('#apply-paths');
        listEl.innerHTML = '';

        files.forEach(file => {
            const li = document.createElement('li');
            li.textContent = `[${file.statusLabel}] ${file.fileName} -> ${file.path}`;
            listEl.appendChild(li);
        });

        resultEl.classList.remove('d-none');
    }

    function clearApplyResult() {
        document.querySelector('#apply-result').classList.add('d-none');
        document.querySelector('#apply-paths').innerHTML = '';
    }

    async function ensureOptionsRendered() {
        if (document.querySelectorAll('#search-param-options tr[data-name]').length === 0
            && document.querySelectorAll('#column-options tr[data-column]').length === 0) {
            await renderOptionTables();
        }
    }

    async function renderOptionTables() {
        const analysis = await analyzeQuery();
        const searchVars = analysis.searchVars || [];
        const columns = analysis.columns || [];
        renderSearchParamOptions(searchVars);
        renderColumnOptions(columns);
        renderColumnSelectOptions(columns, analysis.pkColumns || []);
        renderTargetTableDefault(analysis.targetTable || '');
        renderMenuDefaults();
    }

    async function analyzeQuery() {
        const response = await axios.post('/system/scaffold/analyze', {
            rawQuery: document.querySelector('#rawQuery').value,
            targetTable: document.querySelector('#targetTable').value.trim()
        });
        return response.data || {};
    }

    function renderSearchParamOptions(searchVars) {
        const tbody = document.querySelector('#search-param-options');
        const previous = indexRows(tbody, 'name');
        tbody.innerHTML = '';
        if (searchVars.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-secondary">검색 파라미터가 없으면 searchKeyword가 생성됩니다.</td></tr>';
            return;
        }
        searchVars.forEach(name => {
            const prev = previous[name] || {};
            const inputType = prev.inputType || (isDateVar(name) ? 'DATE' : 'TEXT');
            const tr = document.createElement('tr');
            tr.dataset.name = name;
            tr.innerHTML = `
                <td><code>${name}</code></td>
                <td>
                    <select class="form-select form-select-sm" data-field="inputType">
                        ${option('TEXT', '텍스트', inputType)}
                        ${option('DATE', '날짜', inputType)}
                        ${option('SELECT', '콤보', inputType)}
                        ${option('RADIO', '라디오', inputType)}
                    </select>
                </td>
                <td>
                    <select class="form-select form-select-sm" data-field="defaultValue">
                        ${option('NONE', '없음', prev.defaultValue || 'NONE')}
                        ${option('TODAY', '오늘', prev.defaultValue)}
                        ${option('YESTERDAY', '어제', prev.defaultValue)}
                        ${option('RECENT_7_DAYS', '최근 7일', prev.defaultValue)}
                        ${option('THIS_MONTH', '이번 달', prev.defaultValue)}
                        ${option('CURRENT_MONTH_TO_TODAY', '현재월 1일~오늘', prev.defaultValue)}
                    </select>
                </td>
                <td><input class="form-control form-control-sm" data-field="optionsText" value="${escapeAttr(prev.optionsText || '')}" placeholder="SMS:SMS,LMS:LMS"></td>
            `;
            tbody.appendChild(tr);
        });
    }

    function renderColumnOptions(columns) {
        const tbody = document.querySelector('#column-options');
        const previous = indexRows(tbody, 'column');
        tbody.innerHTML = '';
        if (columns.length === 0) {
            tbody.innerHTML = '<tr><td colspan="9" class="text-secondary">SELECT alias를 추출하지 못했습니다.</td></tr>';
            return;
        }
        columns.forEach(column => {
            const prev = previous[column] || {};
            const visible = prev.visible === undefined ? true : prev.visible;
            const modalVisible = prev.modalVisible === undefined ? true : prev.modalVisible;
            const editable = prev.editable === undefined ? isEditableCandidate(column) : prev.editable;
            const tr = document.createElement('tr');
            tr.dataset.column = column;
            tr.innerHTML = `
                <td><input class="form-check-input" type="checkbox" data-field="visible" ${visible ? 'checked' : ''} title="그리드 표시"></td>
                <td><input class="form-check-input" type="checkbox" data-field="modalVisible" ${modalVisible ? 'checked' : ''} title="상세 모달 표시"></td>
                <td><input class="form-check-input" type="checkbox" data-field="editable" ${editable ? 'checked' : ''} title="수정 가능"></td>
                <td><code>${column}</code></td>
                <td><input class="form-control form-control-sm" data-field="headerName" value="${escapeAttr(prev.headerName || column)}"></td>
                <td><input class="form-control form-control-sm" type="number" data-field="width" value="${escapeAttr(prev.width || '150')}" min="60"></td>
                <td>
                    <select class="form-select form-select-sm" data-field="align">
                        ${option('center', 'center', prev.align || 'center')}
                        ${option('left', 'left', prev.align)}
                        ${option('right', 'right', prev.align)}
                    </select>
                </td>
                <td>
                    <select class="form-select form-select-sm" data-field="dateFormat">
                        ${option('AUTO', '자동', prev.dateFormat || 'AUTO')}
                        ${option('NONE', '없음', prev.dateFormat)}
                        ${option('DATE', 'YYYY-MM-DD', prev.dateFormat)}
                        ${option('DATETIME', 'YYYY-MM-DD HH:mm', prev.dateFormat)}
                    </select>
                </td>
                <td>
                    <select class="form-select form-select-sm" data-field="maskType">
                        ${option('NONE', '없음', prev.maskType || 'NONE')}
                        ${option('PHONE', '전화번호', prev.maskType)}
                        ${option('NAME', '이름', prev.maskType)}
                        ${option('RRN', '주민번호', prev.maskType)}
                        ${option('EMAIL', '이메일', prev.maskType)}
                    </select>
                </td>
            `;
            tbody.appendChild(tr);
        });
    }

    function renderColumnSelectOptions(columns, pkColumns) {
        const pkSelect = document.querySelector('#pkColumns');
        const lockSelect = document.querySelector('#lockColumn');
        const prevPk = selectedValues(pkSelect);
        const prevLock = lockSelect.value;
        const selectedPkColumns = prevPk.length > 0 ? prevPk : pkColumns;
        const pkSet = new Set((selectedPkColumns || []).map(col => String(col).toUpperCase()));
        const lockColumns = columns.filter(col => !pkSet.has(String(col).toUpperCase()));
        pkSelect.innerHTML = columns.map(col => `<option value="${col}">${col}</option>`).join('');
        lockSelect.innerHTML = '<option value="">선택 안 함</option>' + lockColumns.map(col => `<option value="${col}">${col}</option>`).join('');
        setSelectedValues(pkSelect, selectedPkColumns);
        lockSelect.value = lockColumns.includes(prevLock) ? prevLock : guessLock(lockColumns);
    }

    function renderTargetTableDefault(targetTable) {
        const targetTableEl = document.querySelector('#targetTable');
        if (!targetTableEl.value && targetTable) {
            targetTableEl.value = targetTable;
        }
    }

    function renderMenuDefaults() {
        const moduleName = document.querySelector('#moduleName').value.trim();
        const domainId = document.querySelector('#domainId').value.trim();
        const menuId = document.querySelector('#menuId');
        if (!menuId.value && moduleName && domainId) {
            // 백엔드 ScaffoldModel.menuId()와 동일 규약: 하이픈/슬래시 모두 밑줄로 치환한다.
            menuId.value = `${moduleName}_${domainId}`.toUpperCase().replace(/[-/]/g, '_');
        }
        if (!document.querySelector('#roleCode').value) {
            document.querySelector('#roleCode').value = 'ROLE_ADMIN';
        }
        if (!document.querySelector('#sortOrd').value) {
            document.querySelector('#sortOrd').value = '99';
        }
    }

    function readSearchParamOptions() {
        return Array.from(document.querySelectorAll('#search-param-options tr[data-name]')).map(row => ({
            name: row.dataset.name,
            inputType: value(row, 'inputType'),
            defaultValue: value(row, 'defaultValue'),
            optionsText: value(row, 'optionsText')
        }));
    }

    function readColumnOptions() {
        return Array.from(document.querySelectorAll('#column-options tr[data-column]')).map(row => ({
            columnName: row.dataset.column,
            visible: row.querySelector('[data-field="visible"]').checked,
            modalVisible: row.querySelector('[data-field="modalVisible"]').checked,
            editable: row.querySelector('[data-field="editable"]').checked,
            headerName: value(row, 'headerName'),
            width: Number(value(row, 'width') || 150),
            align: value(row, 'align'),
            dateFormat: value(row, 'dateFormat'),
            maskType: value(row, 'maskType')
        }));
    }

    function readPkColumns() {
        return selectedValues(document.querySelector('#pkColumns'));
    }

    function selectedValues(selectEl) {
        if (!selectEl) {
            return [];
        }
        return Array.from(selectEl.selectedOptions || [])
            .map(option => option.value)
            .filter(Boolean);
    }

    function setSelectedValues(selectEl, values) {
        const selected = new Set((values || []).map(value => String(value).toUpperCase()));
        Array.from(selectEl.options || []).forEach(option => {
            option.selected = selected.has(String(option.value).toUpperCase());
        });
    }

    function indexRows(tbody, keyName) {
        const rows = {};
        tbody.querySelectorAll(`tr[data-${keyName}]`).forEach(row => {
            const key = row.dataset[keyName];
            rows[key] = {};
            row.querySelectorAll('[data-field]').forEach(input => {
                rows[key][input.dataset.field] = input.type === 'checkbox' ? input.checked : input.value;
            });
        });
        return rows;
    }

    function value(row, field) {
        const input = row.querySelector(`[data-field="${field}"]`);
        return input ? input.value : '';
    }

    function isDateVar(name) {
        const lower = name.toLowerCase();
        return lower.endsWith('date') || lower.endsWith('dt') || lower.endsWith('at');
    }

    function guessLock(columns) {
        return columns.find(col => col === 'UPD_DTTM' || col === 'UPDATE_DTTM') || '';
    }

    function isEditableCandidate(column) {
        const upper = String(column || '').toUpperCase();
        return upper !== ''
            && !upper.endsWith('_ID')
            && upper !== 'REQUEST_ID'
            && upper !== 'REG_ID'
            && upper !== 'REG_DTTM'
            && upper !== 'UPD_ID'
            && upper !== 'UPD_DTTM'
            && upper !== 'CREATED_AT'
            && upper !== 'UPDATED_AT';
    }

    function option(value, label, selectedValue) {
        return `<option value="${value}" ${value === selectedValue ? 'selected' : ''}>${label}</option>`;
    }

    function escapeAttr(text) {
        return String(text).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }
});
