/**
 * 공통 API 클라이언트 모듈 (jQuery 기반)
 * CSRF 토큰 자동 주입 및 글로벌 에러(401, 403, 500 등) 공통 헨들링
 */
$(function() {
    // 1. 모든 AJAX 요청 시 CSRF 토큰 자동 헤더 세팅 (Spring Security 방어용)
    const token = $("meta[name='_csrf']").attr("content");
    const header = $("meta[name='_csrf_header']").attr("content");

    if (token && header) {
        $(document).ajaxSend(function(event, xhr, options) {
            xhr.setRequestHeader(header, token);
        });
    }

    // 2. AJAX 글로벌 기본 세팅 및 에러 제어
    $.ajaxSetup({
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        error: function(xhr, status, error) {
            console.error("AJAX Error:", xhr);
            
            if (xhr.status === 401) {
                alert("세션이 만료되었습니다. 다시 로그인해 주세요.");
                location.href = "/auth/login";
            } else if (xhr.status === 403) {
                alert("요청 권한이 없습니다.");
            } else {
                const errorMsg = xhr.responseJSON && xhr.responseJSON.message 
                    ? xhr.responseJSON.message 
                    : "서버 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
                alert(errorMsg);
            }
        }
    });
});

/**
 * 전역 API 호출 헬퍼 객체
 */
const ApiClient = {
    get: function(url, params) {
        return $.ajax({
            url: url,
            type: "GET",
            data: params
        });
    },
    post: function(url, body) {
        return $.ajax({
            url: url,
            type: "POST",
            data: JSON.stringify(body)
        });
    },
    put: function(url, body) {
        return $.ajax({
            url: url,
            type: "PUT",
            data: JSON.stringify(body)
        });
    },
    delete: function(url) {
        return $.ajax({
            url: url,
            type: "DELETE"
        });
    }
};
