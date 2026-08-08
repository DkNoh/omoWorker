#!/usr/bin/env node
'use strict';

/*
 * notice-popup.js throwing harness (v2 reconciliation, Todo 4).
 *
 * Proves that every async error path in notice-popup.js is wrapped in try/catch
 * and never leaks to postMessage / window.close / notifyParentAndClose.
 *
 * v2 contract pins:
 *   - action: 'noticeChanged' (NOT 'noticeSaved')
 *   - bounded operation payload: 'created' | 'updated' | 'deleted'
 *   - single postMessage call site with window.location.origin (no wildcard)
 *
 * Strategy: intercept addEventListener on button elements to capture
 * save/delete click handlers, then invoke them directly.
 *
 * Scenarios:
 *   1. collectFormData throws -> no postMessage, no close, no unhandled rejection
 *   2. editor.getMarkdown throws -> no postMessage, no close, no unhandled rejection
 *   3. ApiClient.post rejects -> no postMessage, no close, no unhandled rejection
 *   4. fillForm throws (detail load) -> no postMessage, no close, no unhandled rejection
 *   5. ApiClient.remove rejects (delete confirm callback) -> no postMessage, no close
 *   6. noticeId extraction throws inside delete confirm callback -> no postMessage, no close
 *   7. save button click handler throws -> no unhandled rejection
 *   8. HTTP detail failure (get rejects) -> no toast, save disabled (detailLoadFailed fail-closed)
 *   9. DOM/editor binding failure -> save disabled + delete hidden, one warning toast
 *  10. happy-path create -> single postMessage { action: 'noticeChanged', operation: 'created' }
 *  11. happy-path update -> single postMessage { action: 'noticeChanged', operation: 'updated' }
 *  12. happy-path delete -> single postMessage { action: 'noticeChanged', operation: 'deleted' }
 *
 * Exits non-zero (throws) on any assertion failure.
 */

const fs = require('fs');
const vm = require('vm');
const path = require('path');

const SRC_PATH = path.join(
    __dirname, '..', '..', '..', 'main', 'resources', 'static', 'js', 'basic', 'notice-popup.js'
);
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

// ─── helpers ──────────────────────────────────────────────────────────────────

function mkSandbox(opts) {
    const {
        noticePageAuth,
        omitNoticePageAuth,
        throwCollectFormData,
        throwGetMarkdown,
        throwFillForm,
        throwPost,
        throwRemove,
        throwNoticeIdExtract,
        isUpdateMode,
        renderSave,
        renderDelete,
        detailResponse,
        detailError,
    } = opts;

    const state = {
        postMessageCalls: [],
        closeCalls: [],
        toastCalls: [],
        confirmCb: null,
        saveHandler: null,
        deleteHandler: null,
    };

    const saveBtn = renderSave ? {
        style: { display: '' },
        disabled: false,
        _listeners: {},
        addEventListener(event, handler) {
            if (event === 'click') state.saveHandler = handler;
        },
    } : null;

    const delBtn = renderDelete ? {
        style: { display: '' },
        disabled: false,
        _listeners: {},
        addEventListener(event, handler) {
            if (event === 'click') state.deleteHandler = handler;
        },
    } : null;

    const documentMock = {
        getElementById(id) {
            if (id === 'btn-save') return saveBtn;
            if (id === 'btn-delete') return delBtn;
            if (id === 'noticeId') {
                if (throwNoticeIdExtract) throw new Error('noticeId getElementById throws');
                return { value: isUpdateMode ? '42' : '', addEventListener() {} };
            }
            if (id === 'beforeUpdDttm') return { value: '', addEventListener() {} };
            if (id === 'title') return { value: '', addEventListener() {} };
            if (id === 'noticeType') return { value: '', addEventListener() {} };
            if (id === 'useYn') return { value: 'Y', addEventListener() {} };
            if (id === 'viewCnt') return { value: '0', addEventListener() {} };
            if (id === 'startDt') return { value: '', addEventListener() {} };
            if (id === 'endDt') return { value: '', addEventListener() {} };
            if (id === 'content') return { value: '', addEventListener() {} };
            if (id === 'content-editor') return { querySelectorAll: () => [] };
            if (id === 'notice-form') {
                return {
                    addEventListener() {},
                };
            }
            if (id === 'popup-title') return { textContent: '', addEventListener() {} };
            return { value: '', textContent: '', style: { display: '' }, addEventListener() {} };
        },
        addEventListener(ev, handler) {
            if (ev === 'DOMContentLoaded') {
                handler();
            }
        },
        querySelector() {
            return null;
        },
    };

    const windowMock = Object.assign(
        {},
        omitNoticePageAuth ? {} : { noticePageAuth: noticePageAuth || {} },
        {
            location: {
                search: isUpdateMode ? '?noticeId=42' : '',
                origin: 'http://localhost',
            },
            opener: null,
            closed: false,
            close() {
                state.closeCalls.push('window.close()');
            },
            addEventListener() {},
            postMessage(msg, origin) {
                state.postMessageCalls.push({ msg, origin });
            },
        }
    );

    const toastuiMock = {
        Editor: class {
            constructor(_opts) {
                this.getMarkdown = () => {
                    if (throwGetMarkdown) throw new Error('editor.getMarkdown throws');
                    return '# test content';
                };
                this.setMarkdown = () => {
                    if (throwFillForm) throw new Error('editor.setMarkdown throws');
                };
            }
        },
    };

    const ApiClientMock = {
        get: async (_url, _params) => {
            if (detailError) throw detailError;
            return detailResponse === undefined ? null : detailResponse;
        },
        post: async (_url, _data) => {
            if (throwPost) throw new Error('ApiClient.post rejects');
            return {};
        },
        remove: async (_url, _params) => {
            if (throwRemove) throw new Error('ApiClient.remove rejects');
            return {};
        },
    };

    const CommonUtilsMock = {
        toast(msg, type) {
            state.toastCalls.push({ msg, type });
        },
        confirm(_msg, cb) {
            state.confirmCb = cb;
        },
    };

    const FormDataMock = class {
        constructor(_el) {}
        forEach(cb) {
            if (throwCollectFormData) throw new Error('FormData forEach throws');
            cb('test-title', 'title');
            cb('test-content', 'content');
            cb('2026-01-01', 'startDt');
            cb('2026-12-31', 'endDt');
        }
    };

    const sandbox = {
        window: windowMock,
        document: documentMock,
        console: { warn() {}, error() {} },
        ApiClient: ApiClientMock,
        CommonUtils: CommonUtilsMock,
        URLSearchParams: URLSearchParams,
        FormData: FormDataMock,
        Promise: Promise,
        Object: Object,
        _state: state,
        toastui: toastuiMock,
    };

    vm.createContext(sandbox);
    vm.runInNewContext(SRC, sandbox, { filename: 'notice-popup.js' });

    let unhandledRejectionCaught = false;
    const handler = (e) => {
        unhandledRejectionCaught = true;
        state.unhandledRejection = e.reason || e;
    };
    process.on('unhandledRejection', handler);

    return {
        state,
        sandbox,
        saveBtn,
        delBtn,
        hasUnhandledRejection: () => unhandledRejectionCaught,
        triggerSave: () => {
            if (state.saveHandler) {
                state.saveHandler({});
            }
        },
        triggerDeleteClick: () => {
            if (state.deleteHandler) {
                state.deleteHandler({});
            }
        },
        triggerDeleteConfirm: () => {
            if (state.confirmCb) {
                state.confirmCb();
            }
        },
        cleanup: () => {
            process.removeListener('unhandledRejection', handler);
        },
    };
}

// ─── Run scenarios ────────────────────────────────────────────────────────────

async function runAll() {
    // ─── SCENARIO 1: collectFormData throws ──────────────────────────────────
    console.log('\n  SCENARIO 1: collectFormData throws -> no postMessage, no close');
    {
        const harness = mkSandbox({
            noticePageAuth: { create: true },
            throwCollectFormData: true,
            isUpdateMode: false,
            renderSave: true,
            renderDelete: false,
        });
        harness.triggerSave();
        await new Promise(r => setTimeout(r, 100));

        check(harness.state.postMessageCalls.length === 0, 'postMessage called 0 times (collectFormData throw)');
        check(harness.state.closeCalls.length === 0, 'window.close called 0 times (collectFormData throw)');
        check(!harness.hasUnhandledRejection(), 'zero unhandledRejection (collectFormData throw)');
        harness.cleanup();
    }

    // ─── SCENARIO 2: editor.getMarkdown throws ───────────────────────────────
    console.log('\n  SCENARIO 2: editor.getMarkdown throws -> no postMessage, no close');
    {
        const harness = mkSandbox({
            noticePageAuth: { create: true },
            throwGetMarkdown: true,
            isUpdateMode: false,
            renderSave: true,
            renderDelete: false,
        });
        harness.triggerSave();
        await new Promise(r => setTimeout(r, 100));

        check(harness.state.postMessageCalls.length === 0, 'postMessage called 0 times (getMarkdown throw)');
        check(harness.state.closeCalls.length === 0, 'window.close called 0 times (getMarkdown throw)');
        check(!harness.hasUnhandledRejection(), 'zero unhandledRejection (getMarkdown throw)');
        harness.cleanup();
    }

    // ─── SCENARIO 3: ApiClient.post rejects ──────────────────────────────────
    console.log('\n  SCENARIO 3: ApiClient.post rejects -> no postMessage, no close');
    {
        const harness = mkSandbox({
            noticePageAuth: { create: true },
            throwPost: true,
            isUpdateMode: false,
            renderSave: true,
            renderDelete: false,
        });
        harness.triggerSave();
        await new Promise(r => setTimeout(r, 100));

        check(harness.state.postMessageCalls.length === 0, 'postMessage called 0 times (post reject)');
        check(harness.state.closeCalls.length === 0, 'window.close called 0 times (post reject)');
        check(!harness.hasUnhandledRejection(), 'zero unhandledRejection (post reject)');
        harness.cleanup();
    }

    // ─── SCENARIO 4: fillForm throws (detail load) ───────────────────────────
    console.log('\n  SCENARIO 4: fillForm throws (detail load) -> no postMessage, no close');
    {
        const harness = mkSandbox({
            noticePageAuth: { update: true },
            throwFillForm: true,
            detailResponse: { noticeId: 42, title: 'X', content: 'body', updDttm: '2026-07-17T10:00:00' },
            isUpdateMode: true,
            renderSave: true,
            renderDelete: false,
        });
        await new Promise(r => setTimeout(r, 100));

        check(harness.state.postMessageCalls.length === 0, 'postMessage called 0 times (fillForm throw on detail load)');
        check(harness.state.closeCalls.length === 0, 'window.close called 0 times (fillForm throw on detail load)');
        check(!harness.hasUnhandledRejection(), 'zero unhandledRejection (fillForm throw on detail load)');
        check(harness.state.toastCalls.length === 1, 'binding failure shows exactly one local warning toast');
        check(harness.state.toastCalls[0] && harness.state.toastCalls[0].type === 'warning', 'binding failure toast is warning type');
        harness.cleanup();
    }

    // ─── SCENARIO 5: ApiClient.remove rejects (delete confirm callback) ──────
    console.log('\n  SCENARIO 5: ApiClient.remove rejects (delete confirm callback)');
    {
        const harness = mkSandbox({
            noticePageAuth: { update: true, delete: true },
            throwRemove: true,
            isUpdateMode: true,
            renderSave: true,
            renderDelete: true,
        });
        harness.triggerDeleteClick();
        harness.triggerDeleteConfirm();
        await new Promise(r => setTimeout(r, 100));

        check(harness.state.postMessageCalls.length === 0, 'postMessage called 0 times (remove reject in delete callback)');
        check(harness.state.closeCalls.length === 0, 'window.close called 0 times (remove reject in delete callback)');
        check(!harness.hasUnhandledRejection(), 'zero unhandledRejection (remove reject in delete callback)');
        harness.cleanup();
    }

    // ─── SCENARIO 6: noticeId extraction throws inside delete confirm ────────
    console.log('\n  SCENARIO 6: noticeId extraction throws inside delete confirm callback');
    {
        const harness = mkSandbox({
            noticePageAuth: { update: true, delete: true },
            throwNoticeIdExtract: true,
            isUpdateMode: true,
            renderSave: true,
            renderDelete: true,
        });
        harness.triggerDeleteClick();
        harness.triggerDeleteConfirm();
        await new Promise(r => setTimeout(r, 100));

        check(harness.state.postMessageCalls.length === 0, 'postMessage called 0 times (noticeId extract throw)');
        check(harness.state.closeCalls.length === 0, 'window.close called 0 times (noticeId extract throw)');
        check(!harness.hasUnhandledRejection(), 'zero unhandledRejection (noticeId extract throw)');
        harness.cleanup();
    }

    // ─── SCENARIO 7: save button click handler throws ────────────────────────
    console.log('\n  SCENARIO 7: save button click handler throws -> no unhandled rejection');
    {
        const harness = mkSandbox({
            noticePageAuth: { create: true },
            throwPost: true,
            isUpdateMode: false,
            renderSave: true,
            renderDelete: false,
        });
        harness.triggerSave();
        await new Promise(r => setTimeout(r, 100));

        check(!harness.hasUnhandledRejection(), 'zero unhandledRejection (save button handler throws)');
        harness.cleanup();
    }

    // ─── SCENARIO 8: HTTP detail failure -> no toast, save disabled ──────────
    console.log('\n  SCENARIO 8: HTTP detail failure -> no duplicate toast, save disabled');
    {
        const harness = mkSandbox({
            noticePageAuth: { update: true, delete: true },
            detailError: new Error('500 Internal Server Error'),
            isUpdateMode: true,
            renderSave: true,
            renderDelete: true,
        });
        await new Promise(r => setTimeout(r, 100));

        check(harness.state.toastCalls.length === 0, 'HTTP failure shows NO local toast (interceptor handles it — no duplicate)');
        check(harness.state.postMessageCalls.length === 0, 'HTTP failure: no postMessage');
        check(harness.state.closeCalls.length === 0, 'HTTP failure: no window.close');
        check(sandboxSaveDisabled(harness), 'HTTP failure: save button disabled (detailLoadFailed fail-closed)');
        check(sandboxDeleteHidden(harness), 'HTTP failure: delete button hidden (detailLoadFailed fail-closed)');
        harness.cleanup();
    }

    // ─── SCENARIO 9: DOM/editor binding failure -> save disabled + hidden ────
    console.log('\n  SCENARIO 9: binding failure -> save disabled, delete hidden, one warning');
    {
        const harness = mkSandbox({
            noticePageAuth: { update: true, delete: true },
            throwFillForm: true,
            detailResponse: { noticeId: 42, title: 'X', content: 'body', updDttm: '2026-07-17T10:00:00' },
            isUpdateMode: true,
            renderSave: true,
            renderDelete: true,
        });
        await new Promise(r => setTimeout(r, 100));

        check(harness.state.toastCalls.length === 1, 'binding failure: exactly one local warning toast');
        check(harness.state.postMessageCalls.length === 0, 'binding failure: no postMessage');
        check(harness.state.closeCalls.length === 0, 'binding failure: no window.close');
        check(sandboxSaveDisabled(harness), 'binding failure: save button disabled');
        check(sandboxDeleteHidden(harness), 'binding failure: delete button hidden');
        harness.cleanup();
    }

    // ─── SCENARIO 10: happy-path CREATE -> bounded operation 'created' ───────
    console.log('\n  SCENARIO 10: happy-path CREATE -> single noticeChanged/created to exact origin');
    {
        const harness = mkSandbox({
            noticePageAuth: { create: true },
            isUpdateMode: false,
            renderSave: true,
            renderDelete: false,
        });
        // Provide an opener so postMessage fires
        harness.sandbox.window.opener = { postMessage(msg, origin) { state10.push({ msg, origin }); }, closed: false };
        const state10 = [];
        harness.sandbox.window.opener = { postMessage(msg, origin) { state10.push({ msg, origin }); }, closed: false };
        harness.triggerSave();
        await new Promise(r => setTimeout(r, 100));

        check(state10.length === 1, 'happy-path CREATE: postMessage called exactly once');
        if (state10.length === 1) {
            check(state10[0].msg.action === 'noticeChanged', 'happy-path CREATE: action is noticeChanged');
            check(state10[0].msg.operation === 'created', 'happy-path CREATE: operation is created');
            check(state10[0].origin === 'http://localhost', 'happy-path CREATE: targetOrigin is exact origin');
        }
        check(harness.state.closeCalls.length === 1, 'happy-path CREATE: window.close called once');
        harness.cleanup();
    }

    // ─── SCENARIO 11: happy-path UPDATE -> bounded operation 'updated' ───────
    console.log('\n  SCENARIO 11: happy-path UPDATE -> single noticeChanged/updated');
    {
        const state11 = [];
        const harness = mkSandbox({
            noticePageAuth: { update: true },
            isUpdateMode: true,
            renderSave: true,
            renderDelete: false,
        });
        harness.sandbox.window.opener = { postMessage(msg, origin) { state11.push({ msg, origin }); }, closed: false };
        harness.triggerSave();
        await new Promise(r => setTimeout(r, 100));

        check(state11.length === 1, 'happy-path UPDATE: postMessage called exactly once');
        if (state11.length === 1) {
            check(state11[0].msg.action === 'noticeChanged', 'happy-path UPDATE: action is noticeChanged');
            check(state11[0].msg.operation === 'updated', 'happy-path UPDATE: operation is updated');
        }
        harness.cleanup();
    }

    // ─── SCENARIO 12: happy-path DELETE -> bounded operation 'deleted' ───────
    console.log('\n  SCENARIO 12: happy-path DELETE -> single noticeChanged/deleted');
    {
        const state12 = [];
        const harness = mkSandbox({
            noticePageAuth: { update: true, delete: true },
            isUpdateMode: true,
            renderSave: true,
            renderDelete: true,
        });
        harness.sandbox.window.opener = { postMessage(msg, origin) { state12.push({ msg, origin }); }, closed: false };
        harness.triggerDeleteClick();
        harness.triggerDeleteConfirm();
        await new Promise(r => setTimeout(r, 100));

        check(state12.length === 1, 'happy-path DELETE: postMessage called exactly once');
        if (state12.length === 1) {
            check(state12[0].msg.action === 'noticeChanged', 'happy-path DELETE: action is noticeChanged');
            check(state12[0].msg.operation === 'deleted', 'happy-path DELETE: operation is deleted');
            check(state12[0].origin === 'http://localhost', 'happy-path DELETE: targetOrigin is exact origin');
        }
        harness.cleanup();
    }
}

function sandboxSaveDisabled(harness) {
    return harness.saveBtn ? harness.saveBtn.disabled === true : false;
}
function sandboxDeleteHidden(harness) {
    return harness.delBtn ? harness.delBtn.style.display === 'none' : false;
}

runAll().then(() => {
    console.log('\n  =========================================');
    if (failures === 0) {
        console.log('  notice-popup throwing harness: ALL SCENARIOS PASSED');
        process.exit(0);
    } else {
        console.error('  notice-popup throwing harness: FAILED ' + failures + ' assertion(s)');
        process.exit(1);
    }
}).catch(err => {
    console.error('  Harness error:', err);
    process.exit(1);
});
