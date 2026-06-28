/**
 * menu-manage.js — /system/menu-manage
 *
 * 트리 UI + 우측 상세/권한 매트릭스.
 * - GET /system/menu-manage/tree        → 평탄 메뉴 목록 → 트리 렌더
 * - GET /system/menu-manage/detail?menuId=... → 메뉴 1건 + 활성 역할 + 현재 권한 행
 * - POST /system/menu-manage/create     → 메뉴 신규 등록
 * - POST /system/menu-manage/update     → 메뉴 수정 + (옵션) 권한 행 교체(authRows)
 * - POST /system/menu-manage/delete     → 메뉴 삭제
 *
 * 권한 행 교체는 authRows를 null로 보내지 않는 한 일괄 replace 이다. 화면에서
 * "저장"을 누르면 항상 authRows를 보내 현재 매트릭스 상태로 덮어쓴다.
 */
(function () {
    'use strict';

    const API = {
        tree: '/system/menu-manage/tree',
        detail: '/system/menu-manage/detail',
        create: '/system/menu-manage/create',
        update: '/system/menu-manage/update',
        delete: '/system/menu-manage/delete'
    };

    /** 매트릭스에 표시할 권한 플래그 순서. 체크박스 name = DTO 필드명. */
    const AUTH_FLAGS = [
        { field: 'canRead',     label: '조회' },
        { field: 'canCreate',   label: '등록' },
        { field: 'canUpdate',   label: '수정' },
        { field: 'canDelete',   label: '삭제' },
        { field: 'canApprove',  label: '승인' },
        { field: 'canCancel',   label: '취소' },
        { field: 'canDownload', label: '다운로드' },
        { field: 'canMaskView', label: '마스크' }
    ];

    const state = {
        tree: [],            // 평탄 메뉴 배열
        selectedMenuId: null,
        mode: 'view',        // 'view' | 'create'
        activeRoles: [],     // 마지막 detail 응답의 활성 역할 목록
        authRows: []         // 마지막 detail 응답의 현재 권한 행
    };

    const els = {};

    const initEls = () => {
        els.tree = document.getElementById('menu-tree');
        els.treeEmpty = document.getElementById('menu-tree-empty');
        els.treeCount = document.getElementById('tree-count');
        els.treeFilter = document.getElementById('tree-filter');
        els.btnRefresh = document.getElementById('btn-refresh');
        els.btnCreate = document.getElementById('btn-create');
        els.btnSave = document.getElementById('btn-save');
        els.btnDelete = document.getElementById('btn-delete');
        els.detailPanel = document.getElementById('menu-detail-panel');
        els.emptyPanel = document.getElementById('menu-empty-panel');
        els.detailMenuId = document.getElementById('detail-menu-id');
        els.form = document.getElementById('menu-detail-form');
        els.fSystemYn = document.getElementById('f-systemYn');
        els.fMenuId = document.getElementById('f-menuId');
        els.fMenuType = document.getElementById('f-menuType');
        els.fParentMenuId = document.getElementById('f-parentMenuId');
        els.authTbody = document.getElementById('menu-auth-tbody');
        els.authSection = document.getElementById('menu-auth-section');
        els.authRowCount = document.getElementById('auth-row-count');
    };

    // ─── 트리 구성 ──────────────────────────────────────────────────
    const buildTree = (flat) => {
        const byId = new Map();
        flat.forEach(m => byId.set(m.menuId, { ...m, children: [] }));
        const roots = [];
        byId.forEach(node => {
            if (node.parentMenuId && byId.has(node.parentMenuId)) {
                byId.get(node.parentMenuId).children.push(node);
            } else {
                roots.push(node);
            }
        });
        return roots;
    };

    const matchesFilter = (node, kw) => {
        if (!kw) return true;
        const lower = kw.toLowerCase();
        return (node.menuId || '').toLowerCase().includes(lower)
            || (node.menuNm || '').toLowerCase().includes(lower);
    };

    /** 필터 키워드에 매칭되는 노드를 서브트리에 포함시키며 렌더. */
    const renderNode = (node, kw, parentEl, depth) => {
        const selfMatch = matchesFilter(node, kw);
        let childMatchedAny = false;
        const wrapper = document.createElement('div');
        wrapper.className = 'menu-node';
        wrapper.dataset.menuId = node.menuId;

        const row = document.createElement('div');
        row.className = 'menu-node-row';
        row.dataset.menuId = node.menuId;
        row.tabIndex = 0;
        row.setAttribute('role', 'treeitem');
        row.setAttribute('aria-selected', state.selectedMenuId === node.menuId ? 'true' : 'false');
        if (state.selectedMenuId === node.menuId) {
            row.classList.add('is-selected');
        }

        const hasChildren = node.children && node.children.length > 0;
        // 필터가 있으면 매칭 자손을 모두 보이게, 없으면 기본 접힘/펼침
        const expanded = !!kw || depth < 2;

        const toggle = document.createElement('button');
        toggle.type = 'button';
        toggle.className = 'menu-node-toggle';
        toggle.setAttribute('aria-label', hasChildren ? '하위 메뉴 펼치기' : '하위 메뉴 없음');
        if (hasChildren) {
            if (expanded) toggle.classList.add('is-expanded');
            toggle.innerHTML = '<i data-lucide="chevron-right"></i>';
        } else {
            toggle.classList.add('is-leaf');
            toggle.innerHTML = '<i data-lucide="chevron-right"></i>';
        }

        const icon = document.createElement('i');
        icon.className = 'menu-node-icon';
        const iconName = node.menuType === 'G' ? 'folder' : (node.menuType === 'M' ? 'file' : 'circle');
        icon.setAttribute('data-lucide', iconName);
        icon.setAttribute('aria-hidden', 'true');

        const label = document.createElement('span');
        label.className = 'menu-node-label';
        label.textContent = node.menuNm || node.menuId;

        const typeBadge = document.createElement('span');
        typeBadge.className = 'menu-node-type';
        typeBadge.textContent = node.menuType || '?';

        row.appendChild(toggle);
        row.appendChild(icon);
        row.appendChild(label);
        row.appendChild(typeBadge);
        wrapper.appendChild(row);

        if (hasChildren) {
            const childWrap = document.createElement('div');
            childWrap.className = 'menu-node-children';
            node.children.forEach(child => {
                const rendered = renderNode(child, kw, childWrap, depth + 1);
                if (rendered.matched) childMatchedAny = true;
            });
            wrapper.appendChild(childWrap);
            if (!expanded && !kw) childWrap.hidden = true;
            toggle.addEventListener('click', (e) => {
                e.stopPropagation();
                childWrap.hidden = !childWrap.hidden;
                toggle.classList.toggle('is-expanded', !childWrap.hidden);
            });
        }

        row.addEventListener('click', (e) => {
            if (e.target.closest('.menu-node-toggle')) return;
            selectMenu(node.menuId);
        });
        row.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                selectMenu(node.menuId);
            }
        });

        const matched = selfMatch || childMatchedAny;
        if (kw && !matched) {
            // 매칭하지 않으면 렌더하지 않는다 (상위에서 이미 추가되었을 수 있으므로 여기서 반환만)
            return { matched: false, el: wrapper };
        }

        parentEl.appendChild(wrapper);
        return { matched: true, el: wrapper };
    };

    const renderTree = () => {
        els.tree.innerHTML = '';
        const kw = (els.treeFilter.value || '').trim();
        const roots = buildTree(state.tree);
        let visibleCount = 0;
        const tempWrap = document.createElement('div');
        roots.forEach(root => {
            const res = renderNode(root, kw, tempWrap, 0);
            if (res.matched) visibleCount++;
        });
        if (tempWrap.children.length === 0) {
            els.tree.appendChild(els.treeEmpty);
            els.treeEmpty.style.display = '';
        } else {
            while (tempWrap.firstChild) {
                els.tree.appendChild(tempWrap.firstChild);
            }
        }
        els.treeCount.textContent = String(state.tree.length);
        if (window.lucide && window.lucide.createIcons) {
            window.lucide.createIcons();
        }
    };

    // ─── 상세/권한 패널 ─────────────────────────────────────────────
    const fillParentMenuSelect = (excludeMenuId) => {
        els.fParentMenuId.innerHTML = '<option value="">없음(최상위)</option>';
        const excluded = new Set();
        if (excludeMenuId) {
            excluded.add(excludeMenuId);
            // 하위 노드 수집
            const stack = [excludeMenuId];
            while (stack.length) {
                const cur = stack.pop();
                state.tree.forEach(m => {
                    if (m.parentMenuId === cur && !excluded.has(m.menuId)) {
                        excluded.add(m.menuId);
                        stack.push(m.menuId);
                    }
                });
            }
        }
        const candidates = state.tree
            .filter(m => m.menuType === 'G' && !excluded.has(m.menuId))
            .sort((a, b) => (a.menuLevel - b.menuLevel) || String(a.menuId).localeCompare(String(b.menuId)));
        candidates.forEach(m => {
            const opt = document.createElement('option');
            opt.value = m.menuId;
            const indent = '— '.repeat(Math.max(0, m.menuLevel - 1));
            opt.textContent = `${indent}${m.menuNm} (${m.menuId})`;
            els.fParentMenuId.appendChild(opt);
        });
    };

    const renderAuthMatrix = (activeRoles, authRows, editable) => {
        state.activeRoles = activeRoles || [];
        state.authRows = authRows || [];
        els.authTbody.innerHTML = '';

        if (state.activeRoles.length === 0) {
            const row = document.createElement('tr');
            row.innerHTML = '<td colspan="9" class="text-center text-body-secondary py-3">활성 역할이 없습니다.</td>';
            els.authTbody.appendChild(row);
            els.authRowCount.textContent = '';
            return;
        }

        const byRole = new Map();
        state.authRows.forEach(r => byRole.set(r.roleCd, r));

        state.activeRoles.forEach(role => {
            const current = byRole.get(role.roleCd) || { roleCd: role.roleCd };
            const tr = document.createElement('tr');
            tr.dataset.roleCd = role.roleCd;

            const tdRole = document.createElement('td');
            tdRole.classList.add('auth-role-name');
            const nm = document.createElement('span');
            nm.textContent = role.roleNm || role.roleCd;
            const code = document.createElement('span');
            code.className = 'auth-role-code';
            code.textContent = role.roleCd;
            tdRole.appendChild(nm);
            tdRole.appendChild(code);
            tr.appendChild(tdRole);

            AUTH_FLAGS.forEach(flag => {
                const td = document.createElement('td');
                td.classList.add('text-center');
                const wrap = document.createElement('div');
                wrap.className = 'form-check';
                const cb = document.createElement('input');
                cb.type = 'checkbox';
                cb.className = 'form-check-input';
                cb.name = flag.field;
                cb.value = 'Y';
                cb.checked = (current[flag.field] === 'Y');
                cb.disabled = !editable;
                cb.setAttribute('aria-label', `${role.roleNm || role.roleCd} ${flag.label}`);
                wrap.appendChild(cb);
                td.appendChild(wrap);
                tr.appendChild(td);
            });

            els.authTbody.appendChild(tr);
        });
        els.authRowCount.textContent = `활성 역할 ${state.activeRoles.length}건 / 현재 권한 행 ${state.authRows.length}건`;
    };

    const collectAuthRows = () => {
        if (state.mode !== 'view') {
            // create 모드에서는 권한 행을 보내지 않는다. 메뉴가 먼저 있어야 권한 행이 의미가 있다.
            return null;
        }
        const rows = [];
        if (state.activeRoles.length === 0) return [];
        state.activeRoles.forEach(role => {
            const tr = els.authTbody.querySelector(`tr[data-role-cd="${CSS.escape(role.roleCd)}"]`);
            if (!tr) return;
            const row = { roleCd: role.roleCd };
            AUTH_FLAGS.forEach(flag => {
                const cb = tr.querySelector(`input[name="${flag.field}"]`);
                row[flag.field] = cb && cb.checked ? 'Y' : 'N';
            });
            rows.push(row);
        });
        return rows;
    };

    const showDetailPanel = (show) => {
        els.detailPanel.hidden = !show;
        els.emptyPanel.hidden = show;
    };

    const bindForm = (data) => {
        Object.keys(data || {}).forEach(name => {
            const field = els.form.querySelector(`[name="${name}"]`);
            if (!field) return;
            if (field.type === 'checkbox') {
                field.checked = (data[name] === 'Y' || data[name] === true);
            } else {
                field.value = (data[name] === null || data[name] === undefined) ? '' : data[name];
            }
        });
    };

    const selectMenu = async (menuId) => {
        if (!menuId) return;
        state.selectedMenuId = menuId;
        // 트리의 선택 상태 표시 갱신
        els.tree.querySelectorAll('.menu-node-row').forEach(r => {
            const sel = r.dataset.menuId === menuId;
            r.classList.toggle('is-selected', sel);
            r.setAttribute('aria-selected', sel ? 'true' : 'false');
        });
        state.mode = 'view';
        try {
            const res = await axios.get(API.detail, { params: { menuId } });
            const detail = res.data || {};
            const menu = detail.menu || {};
            state.activeRoles = detail.activeRoles || [];
            state.authRows = detail.authRows || [];

            els.fMenuId.value = menu.menuId || '';
            els.fMenuId.readOnly = true;
            els.detailMenuId.textContent = menu.menuId || '';
            els.fSystemYn.value = menu.systemYn || 'N';
            fillParentMenuSelect(menu.menuId);
            bindForm(menu);
            const editable = !!els.btnSave; // 저장 버튼이 화면에 있으면 편집 가능
            renderAuthMatrix(state.activeRoles, state.authRows, editable);
            els.authSection.hidden = false;
            showDetailPanel(true);
            if (window.lucide && window.lucide.createIcons) window.lucide.createIcons();
        } catch (e) {
            // axios 인터셉터가 이미 알림을 표시한다
        }
    };

    // ─── 신규 메뉴 (create) ─────────────────────────────────────────
    const startCreate = () => {
        state.mode = 'create';
        state.selectedMenuId = null;
        els.tree.querySelectorAll('.menu-node-row').forEach(r => {
            r.classList.remove('is-selected');
            r.setAttribute('aria-selected', 'false');
        });
        const blank = {
            menuId: '',
            parentMenuId: '',
            menuNm: '',
            menuUrl: '',
            menuLevel: 1,
            sortOrd: 10,
            menuType: 'M',
            iconNm: '',
            displayYn: 'Y',
            useYn: 'Y',
            systemYn: 'N',
            remark: ''
        };
        fillParentMenuSelect(null);
        bindForm(blank);
        els.fMenuId.readOnly = false;
        els.fSystemYn.value = 'N';
        els.detailMenuId.textContent = '(신규)';
        els.authSection.hidden = true;
        showDetailPanel(true);
        if (window.lucide && window.lucide.createIcons) window.lucide.createIcons();
        els.fMenuId.focus();
    };

    // ─── 저장 (create 또는 update) ──────────────────────────────────
    const collectMenuPayload = () => {
        const payload = {};
        Array.from(els.form.querySelectorAll('input[name], select[name], textarea[name]')).forEach(f => {
            if (f.disabled) return;
            const name = f.name;
            if (f.type === 'checkbox') {
                payload[name] = f.checked ? 'Y' : 'N';
            } else if (f.readOnly && name === 'menuId') {
                payload[name] = f.value;
            } else {
                const raw = f.value;
                if (name === 'menuLevel' || name === 'sortOrd') {
                    payload[name] = raw === '' ? 0 : Number(raw);
                } else {
                    payload[name] = raw === '' ? null : raw;
                }
            }
        });
        if (state.mode === 'view') {
            payload.authRows = collectAuthRows();
        }
        return payload;
    };

    const save = async () => {
        const payload = collectMenuPayload();
        if (state.mode === 'create') {
            if (!payload.menuId) {
                CommonUtils.alert('menuId를 입력하세요.');
                els.fMenuId.focus();
                return;
            }
            try {
                await axios.post(API.create, payload);
                CommonUtils.toast('등록되었습니다.', 'success');
                await reloadTree();
                await selectMenu(payload.menuId);
            } catch (e) { /* 인터셉터 처리 */ }
        } else {
            payload.menuId = state.selectedMenuId || payload.menuId;
            try {
                await axios.post(API.update, payload);
                CommonUtils.toast('수정되었습니다.', 'success');
                await reloadTree();
                await selectMenu(payload.menuId);
            } catch (e) { /* 인터셉터 처리 */ }
        }
    };

    // ─── 삭제 ───────────────────────────────────────────────────────
    const removeMenu = () => {
        const menuId = state.selectedMenuId;
        if (!menuId) return;
        CommonUtils.confirm(`메뉴 [${menuId}] 를 삭제하시겠습니까? 권한 행도 함께 삭제됩니다.`, async () => {
            try {
                await axios.post(API.delete, null, { params: { menuId } });
                CommonUtils.toast('삭제되었습니다.', 'success');
                state.selectedMenuId = null;
                await reloadTree();
                showDetailPanel(false);
            } catch (e) { /* 인터셉터 처리 */ }
        });
    };

    // ─── 트리 새로고침 ──────────────────────────────────────────────
    const reloadTree = async () => {
        try {
            const res = await axios.get(API.tree);
            state.tree = Array.isArray(res.data) ? res.data : [];
            renderTree();
        } catch (e) { /* 인터셉터 처리 */ }
    };

    // ─── 초기화 ─────────────────────────────────────────────────────
    const init = () => {
        initEls();
        if (els.btnRefresh) els.btnRefresh.addEventListener('click', reloadTree);
        if (els.btnCreate) els.btnCreate.addEventListener('click', startCreate);
        if (els.btnSave) els.btnSave.addEventListener('click', save);
        if (els.btnDelete) els.btnDelete.addEventListener('click', removeMenu);
        if (els.treeFilter) {
            let timer;
            els.treeFilter.addEventListener('input', () => {
                clearTimeout(timer);
                timer = setTimeout(renderTree, 200);
            });
        }
        reloadTree();
    };

    document.addEventListener('DOMContentLoaded', init);
})();
