(function() {
    const DEFAULT_SIZES = [10, 20, 50, 100];

    function button(label, options = {}) {
        const element = document.createElement("button");
        element.type = "button";
        element.className = "ds-page-button";
        element.textContent = label;
        if (options.active) element.classList.add("is-active");
        if (options.disabled) element.disabled = true;
        if (options.onClick) element.addEventListener("click", options.onClick);
        return element;
    }

    function sizeSelector(currentSize, sizes, onChange) {
        const wrapper = document.createElement("label");
        wrapper.className = "ds-pagination__size";
        wrapper.innerHTML = "<span>페이지 당 보기</span>";

        const select = document.createElement("select");
        select.className = "ds-select ds-pagination__size-select";
        (sizes || DEFAULT_SIZES).forEach(size => {
            const option = document.createElement("option");
            option.value = size;
            option.textContent = `${size}개`;
            option.selected = Number(size) === Number(currentSize);
            select.appendChild(option);
        });
        select.addEventListener("change", function() {
            onChange?.(Number(this.value));
        });

        wrapper.appendChild(select);
        return wrapper;
    }

    function renderOffset(container, config) {
        const root = typeof container === "string" ? document.querySelector(container) : container;
        if (!root) return;

        const total = Number(config.total || 0);
        const page = Math.max(Number(config.page || 1), 1);
        const size = Math.max(Number(config.size || 10), 1);
        const totalPages = Number(config.totalPages || Math.ceil(total / size) || 0);
        const from = total === 0 ? 0 : ((page - 1) * size) + 1;
        const to = Math.min(page * size, total);
        const start = Math.max(1, Math.min(page - 2, Math.max(totalPages - 4, 1)));
        const end = Math.min(totalPages, start + 4);

        root.className = "ds-pagination";
        root.replaceChildren();

        const summary = document.createElement("span");
        summary.className = "ds-pagination__summary";
        summary.textContent = config.summary || `${total.toLocaleString()}건 중 ${from.toLocaleString()}-${to.toLocaleString()}`;

        const pages = document.createElement("div");
        pages.className = "ds-pagination__pages";
        pages.appendChild(button("이전", {
            disabled: page <= 1,
            onClick: () => config.onPageChange?.(page - 1)
        }));

        for (let pageNumber = start; pageNumber <= end; pageNumber++) {
            pages.appendChild(button(String(pageNumber), {
                active: pageNumber === page,
                onClick: () => config.onPageChange?.(pageNumber)
            }));
        }

        pages.appendChild(button("다음", {
            disabled: totalPages === 0 || page >= totalPages,
            onClick: () => config.onPageChange?.(page + 1)
        }));

        const actions = document.createElement("div");
        actions.className = "ds-pagination__actions";
        actions.appendChild(pages);
        actions.appendChild(sizeSelector(size, config.sizes, config.onPageSizeChange));

        root.append(summary, actions);
    }

    function renderCursor(container, config) {
        const root = typeof container === "string" ? document.querySelector(container) : container;
        if (!root) return;

        const page = Math.max(Number(config.page || 1), 1);
        const knownPages = Math.max(Number(config.knownPages || page), page);
        const canOpenNextPage = Boolean(config.hasNext) && page >= knownPages;
        const maxPage = Math.max(knownPages + (canOpenNextPage ? 1 : 0), page);
        const start = Math.max(1, Math.min(page - 2, Math.max(maxPage - 4, 1)));
        const end = Math.min(maxPage, start + 4);

        root.className = "ds-pagination";
        root.replaceChildren();

        const summary = document.createElement("span");
        summary.className = "ds-pagination__summary";
        summary.textContent = config.summary || "";

        const pages = document.createElement("div");
        pages.className = "ds-pagination__pages";
        pages.appendChild(button("이전", {
            disabled: !config.hasPrevious,
            onClick: config.onPrevious
        }));

        for (let pageNumber = start; pageNumber <= end; pageNumber++) {
            const isKnownPage = pageNumber <= knownPages;
            const isNextPage = pageNumber === page + 1 && canOpenNextPage;
            pages.appendChild(button(String(pageNumber), {
                active: pageNumber === page,
                disabled: !isKnownPage && !isNextPage,
                onClick: () => config.onPageChange?.(pageNumber)
            }));
        }

        pages.appendChild(button("다음", {
            disabled: !config.hasNext,
            onClick: config.onNext
        }));

        const actions = document.createElement("div");
        actions.className = "ds-pagination__actions";
        actions.appendChild(pages);
        actions.appendChild(sizeSelector(config.size || 20, config.sizes || [20, 50, 100], config.onPageSizeChange));

        root.append(summary, actions);
    }

    window.DsPagination = {
        renderOffset,
        renderCursor
    };
})();
