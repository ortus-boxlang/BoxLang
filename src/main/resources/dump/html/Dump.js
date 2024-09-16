window.__BoxLang = {
    init: function () {
        if (!window.__BoxLang.isInit) {
            window.__BoxLang.addEventListeners('[data-bx-toggle="siblings"]', "click", window.__BoxLang.toggleSiblings);
            window.__BoxLang.addEventListeners('[data-bx-toggle="siblings"]', "keyup", window.__BoxLang.onKeyup);
            window.__BoxLang.isInit = true;
        } else {
            return;
        }
    },
    addEventListeners: function (selector, event, cb) {
        document.querySelectorAll(selector).forEach(function (el) {
            el.addEventListener(event, cb);
        });
    },
    onKeyup: function (e) {
        if (e.key === 'Enter') {
            window.__BoxLang.toggleSiblings(e);
        }
    },
    toggleSiblings: function (e) {
        var siblings = n => [...n.parentElement.children].filter(c => c != n);
        var el = e.target.closest('caption');
        el.toggleAttribute('open');
        siblings(el).forEach(s => s.classList.toggle('d-none'));
    }
}

document.addEventListener("DOMContentLoaded", __BoxLang.init)