(function () {
    'use strict';

    /*
     * 트리 + 상세 분할 샘플 JS
     * 모든 데이터는 내장 mock — 서버 없이 file://에서 완전 동작한다.
     * 실전 참고: js/system/menu-manage.js (동일한 트리 렌더링 패턴)
     */

    // ─── mock 메뉴 데이터 (flat → buildTree로 계층화) ──────────────────
    const MOCK_MENUS = [
        { menuId: 'ROOT', menuNm: '전체 메뉴', menuType: 'G', parentMenuId: '', sortOrd: 0, useYn: 'Y' },
        { menuId: 'BASIC', menuNm: '기본메뉴', menuType: 'G', parentMenuId: 'ROOT', sortOrd: 1, useYn: 'Y' },
        { menuId: 'NOTICE', menuNm: '공지사항', menuType: 'M', parentMenuId: 'BASIC', sortOrd: 1, useYn: 'Y' },
        { menuId: 'SMS', menuNm: 'SMS발송조회', menuType: 'G', parentMenuId: 'ROOT', sortOrd: 2, useYn: 'Y' },
        { menuId: 'SMS_HISTORY', menuNm: '발송이력조회', menuType: 'M', parentMenuId: 'SMS', sortOrd: 1, useYn: 'Y' },
        { menuId: 'SMS_CAMPAIGN', menuNm: '캠페인SMS', menuType: 'M', parentMenuId: 'SMS', sortOrd: 2, useYn: 'Y' },
        { menuId: 'SYSTEM', menuNm: '시스템관리', menuType: 'G', parentMenuId: 'ROOT', sortOrd: 3, useYn: 'Y' },
        { menuId: 'MENU_MANAGE', menuNm: '메뉴관리', menuType: 'M', parentMenuId: 'SYSTEM', sortOrd: 1, useYn: 'Y' },
        { menuId: 'MSG_MANAGE', menuNm: '메시지관리', menuType: 'M', parentMenuId: 'SYSTEM', sortOrd: 2, useYn: 'N' }
    ];

    const state = { selectedMenuId: null };

    document.addEventListener('DOMContentLoaded', init);

    function init() {
        renderTree();
        bindEvents();
        CommonUtils.refreshIcons();
    }

    // ─── 트리 구성 (flat → roots) ─────────────────────────────────────
    function buildTree(flat) {
        const byId = new Map();
        flat.forEach(m => byId.set(m.menuId, Object.assign({}, m, { children: [] })));
        const roots = [];
        byId.forEach(node => {
            if (node.parentMenuId && byId.has(node.parentMenuId)) {
                byId.get(node.parentMenuId).children.push(node);
            } else {
                roots.push(node);
            }
        });
        return roots;
    }

    function renderTree() {
        const container = document.getElementById('menu-tree');
        container.innerHTML = '';
        const roots = buildTree(MOCK_MENUS);
        roots.forEach(node => renderNode(node, container, 0));
        CommonUtils.refreshIcons();
    }

    function renderNode(node, parentEl, depth) {
        const wrapper = document.createElement('div');
        wrapper.className = 'tree-node';

        const row = document.createElement('div');
        row.className = 'tree-node-row';
        row.dataset.menuId = node.menuId;
        row.tabIndex = 0;
        row.setAttribute('role', 'treeitem');
        if (state.selectedMenuId === node.menuId) {
            row.classList.add('is-selected');
        }

        const hasChildren = node.children && node.children.length > 0;
        const expanded = depth < 1;

        const toggle = document.createElement('button');
        toggle.type = 'button';
        toggle.className = 'tree-node-toggle';
        toggle.setAttribute('aria-label', hasChildren ? '하위 메뉴 펼치기' : '하위 메뉴 없음');
        toggle.innerHTML = '<i data-lucide="chevron-right"></i>';
        if (hasChildren) {
            if (expanded) toggle.classList.add('is-expanded');
        } else {
            toggle.classList.add('is-leaf');
        }

        const icon = document.createElement('i');
        icon.className = 'tree-node-icon';
        icon.setAttribute('data-lucide', node.menuType === 'G' ? 'folder' : 'file');
        icon.setAttribute('aria-hidden', 'true');

        const label = document.createElement('span');
        label.className = 'tree-node-label';
        label.textContent = node.menuNm || node.menuId;

        const typeBadge = document.createElement('span');
        typeBadge.className = 'tree-node-type';
        typeBadge.textContent = node.menuType || '?';

        row.appendChild(toggle);
        row.appendChild(icon);
        row.appendChild(label);
        row.appendChild(typeBadge);
        wrapper.appendChild(row);

        if (hasChildren) {
            const childWrap = document.createElement('div');
            childWrap.className = 'tree-node-children';
            node.children.forEach(child => renderNode(child, childWrap, depth + 1));
            wrapper.appendChild(childWrap);
            if (!expanded) childWrap.hidden = true;
            toggle.addEventListener('click', e => {
                e.stopPropagation();
                childWrap.hidden = !childWrap.hidden;
                toggle.classList.toggle('is-expanded', !childWrap.hidden);
            });
        }

        row.addEventListener('click', e => {
            if (e.target.closest('.tree-node-toggle')) return;
            selectMenu(node.menuId);
        });
        row.addEventListener('keydown', e => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                selectMenu(node.menuId);
            }
        });

        parentEl.appendChild(wrapper);
    }

    // ─── 노드 선택 → 상세 폼 바인딩 ────────────────────────────────────
    function selectMenu(menuId) {
        state.selectedMenuId = menuId;
        document.querySelectorAll('.tree-node-row').forEach(row => {
            row.classList.toggle('is-selected', row.dataset.menuId === menuId);
        });

        const node = MOCK_MENUS.find(m => m.menuId === menuId);
        if (!node) return;

        const form = document.getElementById('menu-detail-form');
        form.menuId.value = node.menuId;
        form.menuNm.value = node.menuNm || '';
        form.menuType.value = node.menuType || '';
        form.parentMenuId.value = node.parentMenuId || '';
        form.sortOrd.value = node.sortOrd == null ? '' : node.sortOrd;
        const useRadio = form.querySelector(`input[name="useYn"][value="${node.useYn || 'Y'}"]`);
        if (useRadio) useRadio.checked = true;
    }

    function bindEvents() {
        document.getElementById('btn-refresh').addEventListener('click', () => {
            renderTree();
            Notify.toast('트리를 새로고침했습니다. (샘플 데이터)', 'info');
        });

        document.getElementById('btn-create').addEventListener('click', () => {
            state.selectedMenuId = null;
            document.querySelectorAll('.tree-node-row').forEach(row => row.classList.remove('is-selected'));
            document.getElementById('menu-detail-form').reset();
            document.getElementById('menuId').value = '';
            Notify.toast('신규 메뉴를 입력하세요. (샘플)', 'info');
        });

        document.getElementById('btn-save').addEventListener('click', async () => {
            const form = document.getElementById('menu-detail-form');
            if (typeof FieldFormat !== 'undefined' && !(await FieldFormat.validateForm(form))) {
                return;
            }
            if (!form.menuId.value) {
                Notify.toast('트리의 메뉴를 먼저 선택하세요.', 'warning');
                return;
            }
            Notify.toast('저장되었습니다. (샘플 데이터)', 'success');
        });
    }
})();
