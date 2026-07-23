#!/usr/bin/env node
'use strict';

/*
 * notice-popup.js applyMode regression harness (v2 reconciliation, Todo 4).
 *
 * WHY this exists: the static Java contract scans pin the *intent* of applyMode
 * at the source level, but a popup IIFE has no JS test runner. This harness
 * actually executes src/main/resources/static/js/basic/notice-popup.js inside a
 * controlled vm sandbox per scenario and asserts the resulting button DOM state.
 *
 * v2 contract under test (mode x permission, fail-closed default):
 *   - missing window.noticePageAuth defaults to { create: false, update: false, delete: false }
 *   - save button disabled = !(mode permission):
 *       CREATE mode -> noticePageAuth.create, UPDATE mode -> noticePageAuth.update
 *   - delete button visible only in UPDATE mode AND noticePageAuth.delete
 *   - missing buttons (Thymeleaf th:if omitted them) must NOT crash init
 *   - repeated init() must NOT crash and must keep state stable
 *
 * Exits non-zero (throws) on any assertion failure.
 */

const fs = require('fs');
const vm = require('vm');
const path = require('path');

const SRC_PATH = path.join(
    __dirname, '..', '..', '..', 'main', 'resources', 'static', 'js', 'basic', 'notice-popup.js');
const SRC = fs.readFileSync(SRC_PATH, 'utf8');

let failures = 0;

function check(cond, msg) {
  if (cond) {
    console.log('    pass: ' + msg);
  } else {
    failures++;
    console.error('    FAIL: ' + msg);
  }
}

function mkBtn() {
  return { style: { display: '' }, disabled: false, addEventListener() {} };
}

function genericEl() {
  return {
    textContent: '',
    value: '',
    style: { display: '' },
    disabled: false,
    addEventListener() {},
  };
}

function runScenario(opts) {
  const { name, noticePageAuth, omitNoticePageAuth, search, renderSave, renderDelete, callInitTwice } = opts;
  console.log('\n  SCENARIO: ' + name);

  const saveBtn = renderSave ? mkBtn() : null;
  const delBtn = renderDelete ? mkBtn() : null;

  let domReady = null;
  const documentMock = {
    getElementById(id) {
      if (id === 'btn-save') return saveBtn;
      if (id === 'btn-delete') return delBtn;
      return genericEl();
    },
    addEventListener(ev, handler) {
      if (ev === 'DOMContentLoaded') domReady = handler;
    },
    querySelector() {
      return null;
    },
  };
  const windowMock = Object.assign(
      {},
      omitNoticePageAuth ? {} : { noticePageAuth: noticePageAuth },
      {
        location: { search: search, origin: 'http://localhost' },
        opener: null,
        closed: false,
        close() {},
        addEventListener() {},
      }
  );
  const sandbox = {
    window: windowMock,
    document: documentMock,
    console: console,
    ApiClient: {
      get: async () => null,
      post: async () => ({}),
      remove: async () => ({}),
    },
    CommonUtils: {
      toast() {},
      confirm(_msg, _cb) {
        // Do not auto-invoke the callback; delete confirm must stay user-gated.
      },
    },
    URLSearchParams: URLSearchParams,
    FormData: typeof FormData !== 'undefined' ? FormData : class { constructor() {} },
  };

  vm.createContext(sandbox);
  vm.runInNewContext(SRC, sandbox, { filename: 'notice-popup.js' });

  if (!domReady) {
    throw new Error('notice-popup.js did not register a DOMContentLoaded handler');
  }
  domReady();
  if (callInitTwice) {
    domReady();
  }
  return { saveBtn: saveBtn, delBtn: delBtn };
}

// --- permission x mode matrix --------------------------------------------

// create-only + CREATE mode
(function () {
  const r = runScenario({
    name: 'create-only perm, CREATE mode',
    noticePageAuth: { create: true },
    search: '',
    renderSave: true,
    renderDelete: true,
  });
  check(r.saveBtn.disabled === false, 'CREATE + create perm -> save enabled');
  check(r.delBtn.style.display === 'none', 'CREATE mode -> delete hidden');
})();

// create-only + UPDATE mode (has create but not update)
(function () {
  const r = runScenario({
    name: 'create-only perm, UPDATE mode',
    noticePageAuth: { create: true },
    search: '?noticeId=5',
    renderSave: true,
    renderDelete: true,
  });
  check(r.saveBtn.disabled === true, 'UPDATE + create-only -> save disabled (needs update perm)');
  check(r.delBtn.style.display === 'none', 'no delete perm -> delete hidden');
})();

// update-only + UPDATE mode
(function () {
  const r = runScenario({
    name: 'update-only perm, UPDATE mode',
    noticePageAuth: { update: true },
    search: '?noticeId=5',
    renderSave: true,
    renderDelete: true,
  });
  check(r.saveBtn.disabled === false, 'UPDATE + update perm -> save enabled');
  check(r.delBtn.style.display === 'none', 'no delete perm -> delete hidden');
})();

// update-only + CREATE mode (has update but not create)
(function () {
  const r = runScenario({
    name: 'update-only perm, CREATE mode',
    noticePageAuth: { update: true },
    search: '',
    renderSave: true,
    renderDelete: true,
  });
  check(r.saveBtn.disabled === true, 'CREATE + update-only -> save disabled (needs create perm)');
  check(r.delBtn.style.display === 'none', 'CREATE mode -> delete hidden');
})();

// read-only + CREATE mode
(function () {
  const r = runScenario({
    name: 'read-only perm, CREATE mode',
    noticePageAuth: {},
    search: '',
    renderSave: true,
    renderDelete: true,
  });
  check(r.saveBtn.disabled === true, 'read-only + CREATE -> save disabled (fail-closed)');
  check(r.delBtn.style.display === 'none', 'read-only -> delete hidden');
})();

// read-only + UPDATE mode
(function () {
  const r = runScenario({
    name: 'read-only perm, UPDATE mode',
    noticePageAuth: {},
    search: '?noticeId=5',
    renderSave: true,
    renderDelete: true,
  });
  check(r.saveBtn.disabled === true, 'read-only + UPDATE -> save disabled (fail-closed)');
  check(r.delBtn.style.display === 'none', 'read-only -> delete hidden');
})();

// full perms + UPDATE mode
(function () {
  const r = runScenario({
    name: 'full perm, UPDATE mode',
    noticePageAuth: { create: true, update: true, delete: true },
    search: '?noticeId=5',
    renderSave: true,
    renderDelete: true,
  });
  check(r.saveBtn.disabled === false, 'UPDATE + update perm -> save enabled');
  check(r.delBtn.style.display === '', 'UPDATE + delete perm -> delete visible');
})();

// --- missing noticePageAuth entirely: fail-closed default (v2 reconciliation) ---

(function () {
  const r = runScenario({
    name: 'missing noticePageAuth entirely, CREATE mode (fail-closed default)',
    omitNoticePageAuth: true,
    search: '',
    renderSave: true,
    renderDelete: true,
  });
  check(r.saveBtn.disabled === true, 'missing noticePageAuth + CREATE -> save disabled (fail-closed)');
  check(r.delBtn.style.display === 'none', 'missing noticePageAuth -> delete hidden');
})();

(function () {
  const r = runScenario({
    name: 'missing noticePageAuth entirely, UPDATE mode (fail-closed default)',
    omitNoticePageAuth: true,
    search: '?noticeId=5',
    renderSave: true,
    renderDelete: true,
  });
  check(r.saveBtn.disabled === true, 'missing noticePageAuth + UPDATE -> save disabled (fail-closed)');
  check(r.delBtn.style.display === 'none', 'missing noticePageAuth -> delete hidden');
})();

// --- missing buttons: no crash when th:if omits them ---------------------

(function () {
  const r = runScenario({
    name: 'both buttons missing, CREATE mode',
    noticePageAuth: { create: true },
    search: '',
    renderSave: false,
    renderDelete: false,
  });
  check(r.saveBtn === null && r.delBtn === null, 'missing buttons stay null');
  check(true, 'init completed without throwing');
})();

(function () {
  runScenario({
    name: 'save button missing only, UPDATE mode',
    noticePageAuth: { update: true, delete: true },
    search: '?noticeId=5',
    renderSave: false,
    renderDelete: true,
  });
  check(true, 'init completed without throwing with save missing');
})();

// --- repeated init: idempotent, no crash ---------------------------------

(function () {
  const r = runScenario({
    name: 'init called twice, UPDATE mode full perm',
    noticePageAuth: { create: true, update: true, delete: true },
    search: '?noticeId=5',
    renderSave: true,
    renderDelete: true,
    callInitTwice: true,
  });
  check(r.saveBtn.disabled === false, 'after 2x init save state stable (enabled)');
  check(r.delBtn.style.display === '', 'after 2x init delete state stable (visible)');
})();

// --- summary -------------------------------------------------------------

console.log('\n  =========================================');
if (failures === 0) {
  console.log('  apply-mode harness: ALL SCENARIOS PASSED');
  process.exit(0);
} else {
  console.error('  apply-mode harness: FAILED ' + failures + ' assertion(s)');
  process.exit(1);
}
