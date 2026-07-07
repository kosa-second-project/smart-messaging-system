(function() {
    function selectedOptions(root) {
        return Array.from(root.querySelectorAll("input[type='checkbox']:checked"));
    }

    function updateLabel(root) {
        const label = root.querySelector("[data-multiselect-label]");
        if (!label) return;

        const placeholder = root.dataset.placeholder || "전체";
        const checked = selectedOptions(root);
        if (checked.length === 0) {
            label.textContent = placeholder;
            return;
        }

        const labels = checked.map(input => input.dataset.label || input.closest("label")?.innerText.trim() || input.value);
        label.textContent = checked.length === 1 ? labels[0] : `${labels[0]} 외 ${checked.length - 1}`;
    }

    function closeAll(except) {
        document.querySelectorAll("[data-multiselect].is-open").forEach(root => {
            if (root !== except) root.classList.remove("is-open");
        });
    }

    function init(root) {
        const trigger = root.querySelector("[data-multiselect-trigger]");
        const menu = root.querySelector("[data-multiselect-menu]");
        if (!trigger || !menu) return;

        if (root.dataset.multiselectReady !== "true") {
            root.dataset.multiselectReady = "true";
            trigger.addEventListener("click", function(event) {
                event.preventDefault();
                const willOpen = !root.classList.contains("is-open");
                closeAll(root);
                root.classList.toggle("is-open", willOpen);
            });

            root.addEventListener("keydown", function(event) {
                if (event.key === "Escape") {
                    root.classList.remove("is-open");
                    trigger.focus();
                }
            });
        }

        root.querySelectorAll("input[type='checkbox']").forEach(input => {
            if (input.dataset.multiselectBound === "true") return;
            input.dataset.multiselectBound = "true";
            input.addEventListener("change", function() {
                updateLabel(root);
                root.dispatchEvent(new CustomEvent("multiselect:change", { bubbles: true }));
            });
        });

        updateLabel(root);
    }

    document.addEventListener("click", function(event) {
        if (!event.target.closest("[data-multiselect]")) {
            closeAll();
        }
    });

    window.DsMultiselect = {
        initAll(scope) {
            (scope || document).querySelectorAll("[data-multiselect]").forEach(init);
        },
        refresh(root) {
            updateLabel(root);
        }
    };

    document.addEventListener("DOMContentLoaded", function() {
        window.DsMultiselect.initAll(document);
    });
})();
