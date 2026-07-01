/**
 * 글로벌 UI 이벤트 핸들링 (모달 제어, 네비게이션 활성화 등)
 */
$(function() {
    console.log("Global UI Module Loaded.");
    
    // GNB/LNB 현재 메뉴 활성화 자동 처리 예시
    const currentPath = window.location.pathname;
    $(".list-group-item").each(function() {
        const href = $(this).attr("href");
        if (currentPath === href) {
            $(this).addClass("active").removeClass("bg-light");
        }
    });
});
