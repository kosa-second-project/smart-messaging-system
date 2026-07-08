/**
 * 발송 화면 전용 API 레이어
 *
 * [의존]
 *  - api-client.js : CSRF 자동 주입 + 전역 에러 핸들링(401/403/500)이 이미 $.ajaxSetup()으로 등록되어 있어야 합니다.
 *
 * [구조]
 *  1. _request    : traditional:true / patch / postForm 을 지원하는 내부 공통 요청 함수
 *  2. CustomerApi : 고객 조회 관련 API
 *  3. DraftApi    : 수신자 Draft(Redis) 관련 API
 */

// ─────────────────────────────────────────────
// 1. 내부 공통 요청 함수 (api-client.js 위에 thin-wrapper)
// ─────────────────────────────────────────────
const _request = {

    /**
     * 일반 JSON 요청 (GET / POST / PUT / PATCH / DELETE)
     *  - GET  → data를 쿼리스트링으로 전송, traditional:true 로 배열 파라미터 정상 직렬화
     *  - 나머지 → data를 JSON body로 전송
     */
    json: function(method, url, data) {
        return $.ajax({
            url:         url,
            type:        method,
            contentType: 'application/json; charset=utf-8',
            data:        method === 'GET' ? data : JSON.stringify(data),
            traditional: true   // 배열(tagIds, customerIds 등)을 key=1&key=2 형식으로 직렬화
        });
    },

    /**
     * 폼 데이터 요청 (POST, x-www-form-urlencoded)
     *  - "현재 회원 모두 추가" 처럼 배열을 포함한 파라미터를 POST body로 전송할 때 사용
     */
    form: function(url, data) {
        return $.ajax({
            url:         url,
            type:        'POST',
            contentType: 'application/x-www-form-urlencoded; charset=UTF-8',
            data:        data,
            traditional: true
        });
    }
};

// ─────────────────────────────────────────────
// 2. CustomerApi — 고객 조회 API
// ─────────────────────────────────────────────
const CustomerApi = {

    /** 고객 목록 조회 (페이징 + 태그/키워드 필터) */
    getTags: function() {
        return _request.json('GET', '/api/customers/tags', {});
    },

    search: function(params) {
        return _request.json('GET', '/api/customers', params);
    },

    /** 필터 조건에 맞는 고객 ID 전체 조회 */
    getIds: function(params) {
        return _request.json('GET', '/api/customers/ids', params);
    }
};

// ─────────────────────────────────────────────
// 3. DraftApi — 수신자 Draft(Redis) 관련 API
// ─────────────────────────────────────────────
const DraftApi = {

    /** 빈 Draft 생성 (draftId 발급) */
    createEmpty: function() {
        return _request.json('POST', '/api/campaigns/draft/empty');
    },

    /** 현재 필터 조건에 맞는 고객 전체를 Draft에 추가 */
    addAllFiltered: function(params) {
        return _request.form('/api/campaigns/draft', params);
    },

    /** 현재 필터 조건에 맞는 고객 수만 조회 */
    getCandidateCount: function(params) {
        return _request.json('GET', '/api/campaigns/draft/candidate-count', params);
    },

    /** Draft 백그라운드 적재 상태 조회 */
    getStatus: function(draftId) {
        return _request.json('GET', `/api/campaigns/draft/${draftId}/status`);
    },

    /** Draft의 수신자 개별 추가/제거 (체크박스 토글) */
    patchRecipients: function(draftId, items) {
        return _request.json('PATCH', `/api/campaigns/draft/${draftId}/recipients`, { items: items });
    },

    /** Draft 전체 삭제 (선택 초기화) */
    delete: function(draftId) {
        return _request.json('DELETE', `/api/campaigns/draft/${draftId}`);
    }
};
