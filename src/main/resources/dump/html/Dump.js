window.__BoxLang = {
    init: function () {
        if (!window.__BoxLang.isInit) {
            window.__BoxLang.addEventListeners('[data-bx-toggle="siblings"]', "click", window.__BoxLang.toggleSiblings);
            window.__BoxLang.addEventListeners('[data-bx-toggle="onoff"]', "click", window.__BoxLang.toggleOnOff);
            window.__BoxLang.addEventListeners('[data-bx-toggle="siblings"]', "keyup", window.__BoxLang.onKeyup);
            window.__BoxLang.addEventListeners('[data-bx-toggle="onoff"]', "keyup", window.__BoxLang.onKeyup);
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
        if (e.key === 'Enter' && e.target.dataset.bxToggle) {
            if (e.target.dataset.bxToggle === 'siblings') {
                window.__BoxLang.toggleSiblings(e);
            } else if (e.target.dataset.bxToggle === 'onoff') {
                window.__BoxLang.toggleOnOff(e);
            }
        }
    },
    toggleOnOff: function (e) {
        var thEl = e.target.closest('th');
        var tdEl = thEl.nextElementSibling;
        var hasMsgEl = false;
        for (const child of tdEl.children) {
            if (child.classList.contains("bx-onoff-message")) hasMsgEl = true;
        }
        if (!hasMsgEl) window.__BoxLang.appendOffMessage(tdEl);

        window.__BoxLang.setAriaAttributes(thEl);
        thEl.toggleAttribute('off');
        tdEl.toggleAttribute('off');
        tdEl.querySelectorAll('.bx-onoff').forEach(s => s.classList.toggle('d-none'));
    },
    toggleSiblings: function (e) {
        const siblings = n => [...n.parentElement.children].filter(c => c != n);
        const el = e.target.closest('caption');
        window.__BoxLang.setAriaAttributes(el);
        el.toggleAttribute('open');
        siblings(el).forEach(s => s.classList.toggle('d-none'));
    },
    appendOffMessage: function (el) {
        const offEl = document.createElement("div");
        const message = document.createTextNode("Click row heading to show content.");
        offEl.classList.add('bx-onoff', 'bx-onoff-message', 'd-none');
        offEl.appendChild(message);
        el.appendChild(offEl);
    },
    setAriaAttributes(el) {
        const isExpanded = el.getAttribute('aria-expanded') === 'true';
        el.setAttribute('aria-expanded', !isExpanded);
    }
}

document.addEventListener("DOMContentLoaded", __BoxLang.init)