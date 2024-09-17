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
        if (e.key === 'Enter') {
            if (e.target.dataset.bxToggle && e.target.dataset.bxToggle === 'siblings') {
                window.__BoxLang.toggleSiblings(e);
            } else if (e.target.dataset.bxToggle && e.target.dataset.bxToggle === 'onoff') {
                window.__BoxLang.toggleOnOff(e);
            }

        }
    },
    toggleOnOff: function (e) {
        var tdEl = e.target.closest('th').nextElementSibling;
        var message = tdEl.querySelector('.bx-onoff-message');
        if (!message) {
            window.__BoxLang.appendOffMessage(tdEl);
        }
        tdEl.toggleAttribute('off');
        tdEl.querySelectorAll('.bx-onoff').forEach(s => s.classList.toggle('d-none'));
    },
    toggleSiblings: function (e) {
        var siblings = n => [...n.parentElement.children].filter(c => c != n);
        var el = e.target.closest('caption');
        el.toggleAttribute('open');
        siblings(el).forEach(s => s.classList.toggle('d-none'));
    },
    appendOffMessage: function (el) {
        const offEl = document.createElement("div");
        const message = document.createTextNode("Click row heading to show content.");
        offEl.classList.add('bx-onoff');
        offEl.classList.add('bx-onoff-message');
        offEl.classList.add('d-none');
        offEl.appendChild(message);
        el.appendChild(offEl);
    }
}

document.addEventListener("DOMContentLoaded", __BoxLang.init)