/**
 * Harness for TuiPageBuilder._initGrid() contextMenu permission enforcement.
 *
 * Loads the real tui-page-builder.js, stubs DOM / tui.Grid / TuiCommon,
 * calls _initGrid() with controlled PAGE_AUTH scenarios, and captures the
 * gridOptions passed to the constructor.
 *
 * Usage: node tui-page-builder-auth-harness.js
 * Exit 0 = all assertions passed.
 */

'use strict';

var assert = require('assert');
var fs = require('fs');
var path = require('path');
var vm = require('vm');

// ── helpers ──────────────────────────────────────────────────────────────────

var SRC = path.resolve(__dirname, '..', '..', '..', '..', 'src', 'main', 'resources', 'static', 'js', 'common', 'tui-page-builder.js');
var src = fs.readFileSync(SRC, 'utf8');

/**
 * Run the tui-page-builder source in a vm sandbox and return { TuiPageBuilder, gridOptionsCaptured }.
 */
function buildSandbox(pageAuth) {
  var gridOptionsCaptured = [];

  var sandbox = {
    document: {
      getElementById: function(id) {
        return {
          value: '',
          tagName: 'INPUT',
          dataset: {},
          classList: { toggle: function() {} },
          querySelector: function() { return null; },
          querySelectorAll: function() { return []; },
          addEventListener: function() {},
          focus: function() {},
        };
      },
    },
    window: {
      PAGE_AUTH: pageAuth,
      tui: {},
      location: { href: '' },
    },
    TuiCommon: {
      gridDefaults: { scrollX: false, scrollY: false, minBodyHeight: 300 },
      fmt: { date: function(obj) { return obj.value || '-'; } },
      formatDate: function() { return ''; },
      badgeByValue: function() { return function() { return ''; }; },
      maskValue: function() { return ''; },
      updateTotalCount: function() {},
      renderPagination: function() {},
    },
    CommonUtils: {
      setDefaultDateTime: function() {},
      toast: function() {},
      resetFields: function() {},
    },
    tui: {
      Grid: function(opts) {
        gridOptionsCaptured.push(Object.assign({}, opts));
      },
      DatePicker: null,
    },
    dayjs: {
      isValid: function() { return true; },
      format: function() { return '20260101'; },
      subtract: function() { return dayjs; },
      startOf: function() { return dayjs; },
      isAfter: function() { return false; },
    },
    console: { error: function() {} },
    URLSearchParams: URLSearchParams,
  };

  // stub axios
  sandbox.axios = {
    get: function() {
      var d = { then: function() { return d; }, catch: function() { return d; } };
      return d;
    },
  };

  // Wrap source so last expression returns TuiPageBuilder class
  var wrapped = '(function() {\n' + src + '\nreturn TuiPageBuilder;\n})()';
  var TuiPageBuilder = vm.runInNewContext(wrapped, sandbox, { timeout: 5000 });

  return { TuiPageBuilder: TuiPageBuilder, gridOptionsCaptured: gridOptionsCaptured };
}

/**
 * Create a TuiPageBuilder instance with the given gridOptions override
 * (caller may pass contextMenu, etc.) and call _initGrid().
 */
function initGridWithCallerOptions(pageAuth, callerGridOptions) {
  if (callerGridOptions === undefined) callerGridOptions = {};
  var sb = buildSandbox(pageAuth);
  var TuiPageBuilder = sb.TuiPageBuilder;
  var gridOptionsCaptured = sb.gridOptionsCaptured;

  var config = {
    el: 'grid',
    apiUrl: '/test/data',
    searchInputs: [],
    rowHeaders: ['rowNum'],
    columns: [{ header: 'ID', name: 'id', align: 'center', width: 80 }],
    pageSizeEl: 'pageSizeSelect',
    btnSearch: 'btn-search',
    btnReset: 'btn-reset',
    gridOptions: callerGridOptions,
  };

  var instance = Object.create(TuiPageBuilder.prototype);
  instance.config = config;
  instance.grid = null;
  instance.usesOffsetRowNo = false;
  instance._initGrid();

  return gridOptionsCaptured[0] || null;
}

/**
 * Resolve contextMenu: if it is a function, call it to get the MenuItem[][] array.
 */
function resolveContextMenu(opts) {
  if (opts.contextMenu === undefined || opts.contextMenu === null) {
    return opts.contextMenu;
  }
  if (typeof opts.contextMenu === 'function') {
    return opts.contextMenu();
  }
  return opts.contextMenu;
}

// ── test matrix ──────────────────────────────────────────────────────────────

var passed = 0;
var failed = 0;

function test(name, fn) {
  try {
    fn();
    console.log('  ✓ ' + name);
    passed++;
  } catch (e) {
    console.error('  ✗ ' + name);
    console.error('    ' + e.message);
    failed++;
  }
}

console.log('\n=== TuiPageBuilder contextMenu permission harness ===\n');

// 1. missing PAGE_AUTH → copy-only contextMenu
test('missing PAGE_AUTH → copy-only contextMenu', function() {
  var opts = initGridWithCallerOptions(undefined);
  assert.ok(opts.contextMenu, 'contextMenu must be injected when PAGE_AUTH is missing');
  var menu = resolveContextMenu(opts);
  var names = menu.flat().map(function(m) { return m.name; });
  assert.ok(names.includes('copy'), 'must contain copy');
  assert.ok(names.includes('copyColumns'), 'must contain copyColumns');
  assert.ok(names.includes('copyRows'), 'must contain copyRows');
  assert.ok(!names.includes('txtExport'), 'must NOT contain txtExport');
  assert.ok(!names.includes('csvExport'), 'must NOT contain csvExport');
  assert.ok(!names.includes('excelExport'), 'must NOT contain excelExport');
});

// 2. PAGE_AUTH with download=false → copy-only
test('PAGE_AUTH.download=false → copy-only contextMenu', function() {
  var opts = initGridWithCallerOptions({ download: false });
  assert.ok(opts.contextMenu, 'contextMenu must be injected');
  var menu = resolveContextMenu(opts);
  var names = menu.flat().map(function(m) { return m.name; });
  assert.ok(names.includes('copy'));
  assert.ok(!names.includes('excelExport'));
});

// 3. PAGE_AUTH.download='true' (string) → copy-only (strict !== true)
test("PAGE_AUTH.download='true' (string) → copy-only contextMenu", function() {
  var opts = initGridWithCallerOptions({ download: 'true' });
  assert.ok(opts.contextMenu, 'contextMenu must be injected for string true');
  var menu = resolveContextMenu(opts);
  var names = menu.flat().map(function(m) { return m.name; });
  assert.ok(names.includes('copy'));
  assert.ok(!names.includes('excelExport'));
});

// 4. PAGE_AUTH.download=true, no caller contextMenu → undefined (TUI built-in)
test('PAGE_AUTH.download=true, no caller contextMenu → contextMenu undefined', function() {
  var opts = initGridWithCallerOptions({ download: true });
  assert.strictEqual(opts.contextMenu, undefined, 'contextMenu must be undefined when permission granted and no caller menu');
});

// 5. PAGE_AUTH.download=true, caller provides contextMenu function → preserved
test('PAGE_AUTH.download=true, caller contextMenu function → preserved', function() {
  var callerMenu = function () { return [['custom']]; };
  var opts = initGridWithCallerOptions({ download: true }, { contextMenu: callerMenu });
  assert.strictEqual(opts.contextMenu, callerMenu, 'caller contextMenu function must be preserved');
});

// 6. PAGE_AUTH.download=true, caller provides explicit null → preserved
test('PAGE_AUTH.download=true, caller contextMenu=null → preserved', function() {
  var opts = initGridWithCallerOptions({ download: true }, { contextMenu: null });
  assert.strictEqual(opts.contextMenu, null, 'caller explicit null contextMenu must be preserved');
});

// 7. PAGE_AUTH.download=true, caller provides export menu → preserved (not overridden)
test('PAGE_AUTH.download=true, caller export menu → preserved', function() {
  var callerMenu = [
    [{ label: 'Export', subMenu: [{ label: 'Excel', action: 'excelExport' }] }],
  ];
  var opts = initGridWithCallerOptions({ download: true }, { contextMenu: callerMenu });
  assert.deepStrictEqual(opts.contextMenu, callerMenu, 'export menu must be preserved when permission granted');
});

// 8. PAGE_AUTH.download=false, caller provides export menu → overridden by copy-only
test('PAGE_AUTH.download=false, caller export menu → overridden by copy-only', function() {
  var callerMenu = [
    [{ label: 'Export', subMenu: [{ label: 'Excel', action: 'excelExport' }] }],
  ];
  var opts = initGridWithCallerOptions({ download: false }, { contextMenu: callerMenu });
  assert.ok(opts.contextMenu, 'copy-only contextMenu must override caller menu');
  var menu = resolveContextMenu(opts);
  var names = menu.flat().map(function(m) { return m.name; });
  assert.ok(names.includes('copy'));
  assert.ok(!names.includes('excelExport'), 'export must NOT be present when unauthorized');
});

// 9. PAGE_AUTH.download=false, caller provides custom menu → overridden
test('PAGE_AUTH.download=false, caller custom menu → overridden by copy-only', function() {
  var callerMenu = [
    [{ label: 'Custom Action', action: 'customAction' }],
  ];
  var opts = initGridWithCallerOptions({ download: false }, { contextMenu: callerMenu });
  var menu = resolveContextMenu(opts);
  var actions = menu.flat().map(function(m) { return m.action; });
  assert.ok(actions.includes('copy'));
  assert.ok(!actions.includes('customAction'), 'custom action must NOT be present when unauthorized');
});

// 10. copy-only menu items have correct {name, label, action} shape
test('copy-only menu items have correct shape {name, label, action}', function() {
  var opts = initGridWithCallerOptions(undefined);
  var menu = resolveContextMenu(opts);
  var items = menu.flat();
  var i;
  for (i = 0; i < items.length; i++) {
    var item = items[i];
    assert.ok(item.name, 'each item must have name');
    assert.ok(item.label, 'each item must have label');
    assert.ok(item.action, 'each item must have action');
  }
});

// ── summary ──────────────────────────────────────────────────────────────────

console.log('\n' + passed + ' passed, ' + failed + ' failed\n');
process.exit(failed > 0 ? 1 : 0);
