/**
 * 글로벌 UI 이벤트 핸들링 (모달 제어, 네비게이션 활성화 등)
 */
$(function () {


    // GNB/LNB 현재 메뉴 활성화 자동 처리 예시
    const currentPath = window.location.pathname === "/" ? "/dashboard" : window.location.pathname;
    $(".list-group-item").each(function () {
        const href = $(this).attr("href");
        if (currentPath === href) {
            $(this).addClass("active").removeClass("bg-light");
        }
    });

    $(".app-nav__item").each(function () {
        const href = $(this).attr("href");
        if (href && currentPath === href.split("#")[0]) {
            $(this).addClass("is-active");
        }
    });

    $(".app-nav__subitem").each(function () {
        const href = $(this).attr("href");
        if (href && currentPath === href.split("#")[0]) {
            $(this).addClass("is-active");
        }
    });

    const $statsNav = $("[data-stats-nav]");
    const $statsToggle = $("[data-stats-toggle]");
    const isStatsPage = currentPath.startsWith("/stats/");

    function setStatsOpen(open) {
        $statsNav.toggleClass("is-open", open);
        $statsToggle.toggleClass("is-active", isStatsPage);
        $statsToggle.attr("aria-expanded", open ? "true" : "false");
    }

    setStatsOpen(isStatsPage);

    $statsToggle.on("click", function () {
        setStatsOpen(!$statsNav.hasClass("is-open"));
    });

    const $sidebar = $("#appSidebar");
    const $backdrop = $("[data-sidebar-close]");

    $("[data-sidebar-open]").on("click", function () {
        $sidebar.addClass("is-open");
        $backdrop.addClass("is-open");
    });

    $("[data-sidebar-close], .app-nav__item").on("click", function () {
        $sidebar.removeClass("is-open");
        $backdrop.removeClass("is-open");
    });

    const $profileMenu = $("[data-profile-menu]");
    const $profileToggle = $("[data-profile-toggle]");

    function closeProfileMenu() {
        $profileMenu.removeClass("is-open");
        $profileToggle.attr("aria-expanded", "false");
    }

    $profileToggle.on("click", function (event) {
        event.stopPropagation();
        const isOpen = $profileMenu.hasClass("is-open");
        $profileMenu.toggleClass("is-open", !isOpen);
        $profileToggle.attr("aria-expanded", !isOpen ? "true" : "false");
    });

    $(document).on("click", function (event) {
        if (!$(event.target).closest("[data-profile-menu]").length) {
            closeProfileMenu();
        }
    });

    $(document).on("keydown", function (event) {
        if (event.key === "Escape") {
            closeProfileMenu();
        }
    });
});
