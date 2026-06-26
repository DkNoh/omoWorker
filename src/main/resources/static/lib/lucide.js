(function (global) {
    const SVG_NS = 'http://www.w3.org/2000/svg';
    const DEFAULT_ATTRS = {
        xmlns: SVG_NS,
        width: '24',
        height: '24',
        viewBox: '0 0 24 24',
        fill: 'none',
        stroke: 'currentColor',
        'stroke-width': '2',
        'stroke-linecap': 'round',
        'stroke-linejoin': 'round'
    };

    const icons = {
        search: [['circle', { cx: '11', cy: '11', r: '8' }], ['path', { d: 'm21 21-4.3-4.3' }]],
        'rotate-ccw': [['path', { d: 'M3 12a9 9 0 1 0 3-6.7' }], ['path', { d: 'M3 3v6h6' }]],
        'refresh-cw': [['path', { d: 'M21 12a9 9 0 0 0-15-6.7L3 8' }], ['path', { d: 'M3 3v5h5' }], ['path', { d: 'M3 12a9 9 0 0 0 15 6.7l3-2.7' }], ['path', { d: 'M21 21v-5h-5' }]],
        download: [['path', { d: 'M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4' }], ['path', { d: 'M7 10l5 5 5-5' }], ['path', { d: 'M12 15V3' }]],
        copy: [['rect', { x: '9', y: '9', width: '13', height: '13', rx: '2', ry: '2' }], ['path', { d: 'M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1' }]],
        check: [['path', { d: 'M20 6 9 17l-5-5' }]],
        'circle-check': [['circle', { cx: '12', cy: '12', r: '10' }], ['path', { d: 'm9 12 2 2 4-4' }]],
        'circle-x': [['circle', { cx: '12', cy: '12', r: '10' }], ['path', { d: 'm15 9-6 6' }], ['path', { d: 'm9 9 6 6' }]],
        info: [['circle', { cx: '12', cy: '12', r: '10' }], ['path', { d: 'M12 16v-4' }], ['path', { d: 'M12 8h.01' }]],
        'triangle-alert': [['path', { d: 'm21.7 18-8.5-14.7a1.4 1.4 0 0 0-2.4 0L2.3 18a1.4 1.4 0 0 0 1.2 2.1h17a1.4 1.4 0 0 0 1.2-2.1z' }], ['path', { d: 'M12 9v4' }], ['path', { d: 'M12 17h.01' }]],
        eye: [['path', { d: 'M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z' }], ['circle', { cx: '12', cy: '12', r: '3' }]],
        save: [['path', { d: 'M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z' }], ['path', { d: 'M17 21v-8H7v8' }], ['path', { d: 'M7 3v5h8' }]],
        pencil: [['path', { d: 'M17 3a2.9 2.9 0 0 1 4 4L7.5 20.5 2 22l1.5-5.5Z' }], ['path', { d: 'm15 5 4 4' }]],
        'trash-2': [['path', { d: 'M3 6h18' }], ['path', { d: 'M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2' }], ['path', { d: 'M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6' }], ['path', { d: 'M10 11v6' }], ['path', { d: 'M14 11v6' }]],
        x: [['path', { d: 'M18 6 6 18' }], ['path', { d: 'm6 6 12 12' }]],
        sparkles: [['path', { d: 'M12 3l1.5 4.5L18 9l-4.5 1.5L12 15l-1.5-4.5L6 9l4.5-1.5Z' }], ['path', { d: 'M5 3v4' }], ['path', { d: 'M3 5h4' }], ['path', { d: 'M19 17v4' }], ['path', { d: 'M17 19h4' }]],
        'panel-left-close': [['rect', { x: '3', y: '3', width: '18', height: '18', rx: '2' }], ['path', { d: 'M9 3v18' }], ['path', { d: 'm16 15-3-3 3-3' }]],
        'panel-left-open': [['rect', { x: '3', y: '3', width: '18', height: '18', rx: '2' }], ['path', { d: 'M9 3v18' }], ['path', { d: 'm14 9 3 3-3 3' }]],
        'log-out': [['path', { d: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4' }], ['path', { d: 'M16 17l5-5-5-5' }], ['path', { d: 'M21 12H9' }]],
        home: [['path', { d: 'm3 10 9-7 9 7' }], ['path', { d: 'M5 10v10h14V10' }], ['path', { d: 'M9 20v-6h6v6' }]],
        'message-square-search': [['path', { d: 'M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z' }], ['circle', { cx: '11', cy: '10', r: '2.5' }], ['path', { d: 'm13 12 2 2' }]],
        send: [['path', { d: 'm22 2-7 20-4-9-9-4Z' }], ['path', { d: 'M22 2 11 13' }]],
        'sliders-horizontal': [['path', { d: 'M21 4h-7' }], ['path', { d: 'M10 4H3' }], ['path', { d: 'M21 12h-9' }], ['path', { d: 'M8 12H3' }], ['path', { d: 'M21 20h-5' }], ['path', { d: 'M12 20H3' }], ['circle', { cx: '12', cy: '4', r: '2' }], ['circle', { cx: '10', cy: '12', r: '2' }], ['circle', { cx: '14', cy: '20', r: '2' }]],
        users: [['path', { d: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2' }], ['circle', { cx: '9', cy: '7', r: '4' }], ['path', { d: 'M22 21v-2a4 4 0 0 0-3-3.9' }], ['path', { d: 'M16 3.1a4 4 0 0 1 0 7.8' }]],
        'chart-no-axes-combined': [['path', { d: 'M12 16v5' }], ['path', { d: 'M16 14v7' }], ['path', { d: 'M20 10v11' }], ['path', { d: 'm22 3-8.6 8.6a2 2 0 0 1-2.8 0L9.4 10.4a2 2 0 0 0-2.8 0L2 15' }]],
        list: [['path', { d: 'M8 6h13' }], ['path', { d: 'M8 12h13' }], ['path', { d: 'M8 18h13' }], ['path', { d: 'M3 6h.01' }], ['path', { d: 'M3 12h.01' }], ['path', { d: 'M3 18h.01' }]],
        file: [['path', { d: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z' }], ['path', { d: 'M14 2v6h6' }]]
    };

    function createElement(name, attrs) {
        const element = document.createElementNS(SVG_NS, name);
        Object.keys(attrs || {}).forEach(key => element.setAttribute(key, attrs[key]));
        return element;
    }

    function copyAttributes(source, target) {
        Array.from(source.attributes).forEach(attr => {
            if (attr.name === 'class') {
                return;
            }
            target.setAttribute(attr.name, attr.value);
        });
    }

    function createSvg(name, source, attrs) {
        const svg = createElement('svg', Object.assign({}, DEFAULT_ATTRS, attrs || {}));
        copyAttributes(source, svg);
        svg.setAttribute('data-lucide', name);
        svg.setAttribute('class', ['lucide', 'lucide-' + name, source.getAttribute('class') || ''].join(' ').trim());

        (icons[name] || icons.file).forEach(([tag, iconAttrs]) => {
            svg.appendChild(createElement(tag, iconAttrs));
        });
        return svg;
    }

    function createIcons(options) {
        const attrs = options && options.attrs ? options.attrs : {};
        document.querySelectorAll('[data-lucide]').forEach(element => {
            const name = element.getAttribute('data-lucide');
            element.replaceWith(createSvg(name, element, attrs));
        });
    }

    global.lucide = {
        createIcons,
        icons
    };
})(window);
