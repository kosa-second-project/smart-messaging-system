// ==========================================
// 0. HTML 이스케이프 유틸리티 (XSS 방지)
// ==========================================
function escapeHtml(str) {
    if (str == null) return '';
    const div = document.createElement('div');
    div.textContent = String(str);
    return div.innerHTML;
}

// ==========================================
// 1. DB 태그 ID 매핑 맵 정의
// ==========================================
const TAG_MAP = {
    "일반": 23, "신규": 24, "휴면": 25, "생일 대상자": 26,
    "카카오 동의": 27, "sms 동의": 28, "이메일 동의": 29,
    "남자": 30, "여자": 31,
    "10대": 32, "20대": 33, "30대": 34, "40대": 35, "50대": 36, "60대": 37, "70대": 38
};

// ==========================================
// 2. 발송 화면 전역 상태 관리 객체
// ==========================================
const SendPage = {
    // 현재 전역 데이터 상태
    state: {
        currentStep: 1,
        selectedTags: [],            // 항상 빈 태그 상태로 시작
        conditionMode: 'OR',         // 기본 OR 모드
        draftId: null,               // Redis에 저장된 수신자 목록의 식별자 (draftId)
        draftTotalCount: 0,          // Redis에 저장된 수신자 총 인원 수
        searchQuery: '',             // 우측 고객 검색어
        cursorHistory: [null],       // 페이징 커서 히스토리 (index 0 = 1페이지)
        currentCursorIndex: 0,       // 현재 보고 있는 커서 히스토리의 인덱스
        nextCursorId: null,          // 서버에서 응답받은 다음 페이지 커서
        hasNext: false,              // 다음 페이지 존재 여부
        pageSize: 20,                // 한 페이지에 20명씩 렌더링 (기본값)
        activeTab: 'filtered',       // 'filtered'(태그후보), 'selected'(직접선택)
        renderSeq: 0                 // AJAX 요청 순번: 이전 응답 덮어쓰기(Race Condition) 방지
    },

    isNavigatingInternal: false,     // 정상적인 이전/다음 단계 이동 여부 플래그

    // 초기화
    init: function () {


        // 현재 URL 경로를 통해 currentStep 파싱
        const path = window.location.pathname;
        if (path.includes("/send/recipients")) {
            this.state.currentStep = 1;
            // 1단계 진입 시에도 이전 단계에서 이동해 온 경우를 위해 복원
            this.state.draftId = sessionStorage.getItem("draftId") || null;
            this.state.draftTotalCount = parseInt(sessionStorage.getItem("draftTotalCount") || "0");
        } else if (path.includes("/send/message")) {
            this.state.currentStep = 2;
            // 2단계 진입: 1단계에서 저장해 둔 draftId 복원
            this.state.draftId = sessionStorage.getItem("draftId") || null;
            this.state.draftTotalCount = parseInt(sessionStorage.getItem("draftTotalCount") || "0");
        } else if (path.includes("/send/review")) {
            this.state.currentStep = 3;
            this.state.draftId = sessionStorage.getItem("draftId") || null;
            this.state.draftTotalCount = parseInt(sessionStorage.getItem("draftTotalCount") || "0");
        } else {
            this.state.currentStep = 1;
        }

        this.bindGlobalEvents();

        // 현재 단계에 맞춘 초기 UI 동기화
        this.updateStepBarUI(this.state.currentStep);

        if (this.state.currentStep === 1) {
            if (typeof RecipientSelector !== "undefined") RecipientSelector.init();
        } else if (this.state.currentStep === 2) {
            if (typeof MessageComposer !== "undefined") MessageComposer.init();
        }
    },

    // 전역 이벤트 바인딩 (이전/다음 화면 전환 등)
    bindGlobalEvents: function () {
        const self = this;

        // [다음] 또는 [발송하기] 버튼 클릭
        $(document).on("click", "[data-action='next-step']", (e) => {
            e.preventDefault();
            this.handleNextStep();
        });

        // [이전] 버튼 클릭
        $(document).on("click", "[data-action='prev-step']", (e) => {
            e.preventDefault();
            this.handlePrevStep();
        });

        // 브라우저 이탈(새로고침, 탭 닫기, 외부 링크 클릭) 방지 알림
        window.addEventListener("beforeunload", function (e) {
            // draftId가 있고(수신자 선택 진행중) 정상적인 내부 이동이 아닐 경우 경고
            if (self.state.draftId && !self.isNavigatingInternal) {
                e.preventDefault();
                e.returnValue = "이 페이지를 벗어나면 선택된 수신자 정보가 초기화됩니다.";
            }
        });

        // 브라우저 이탈 또는 백그라운드 전환 시 (이탈이 확정된 시점) sessionStorage 파기 및 Redis 정리
        document.addEventListener("visibilitychange", function () {
            if (document.visibilityState === 'hidden' && self.state.draftId && !self.isNavigatingInternal) {
                sessionStorage.removeItem("draftId");
                sessionStorage.removeItem("draftTotalCount");
                if (navigator.sendBeacon) {
                    // beacon은 무조건 POST를 사용하므로 cleanup 전용 엔드포인트 호출
                    navigator.sendBeacon("/api/campaigns/draft/" + self.state.draftId + "/cleanup");
                }
            }
        });

        // 진행 중인 Draft 통신이 있을 때 [다음] 버튼 블록 처리 (Race Condition 및 롤백 방어)
        self.state.pendingDraftRequests = 0;
        $(document).ajaxSend(function(event, jqXHR, ajaxOptions) {
            if (self.isDraftMutationRequest(ajaxOptions)) {
                self.state.pendingDraftRequests++;
                const $btn = $("[data-action='next-step']");
                if (!$btn.data("original-text")) {
                    $btn.data("original-text", $btn.text());
                }
                $btn.prop("disabled", true).text("동기화 중...");
            }
        });

        $(document).ajaxComplete(function(event, jqXHR, ajaxOptions) {
            if (self.isDraftMutationRequest(ajaxOptions)) {
                self.state.pendingDraftRequests = Math.max(0, self.state.pendingDraftRequests - 1);
                if (self.state.pendingDraftRequests === 0) {
                    const $btn = $("[data-action='next-step']");
                    $btn.prop("disabled", false).text($btn.data("original-text") || "다음 단계");
                }
            }
        });
    },

    isDraftMutationRequest: function (ajaxOptions) {
        const url = ajaxOptions && typeof ajaxOptions.url === "string" ? ajaxOptions.url : "";
        const method = ((ajaxOptions && (ajaxOptions.type || ajaxOptions.method)) || "GET").toUpperCase();
        return url.includes("/api/campaigns/draft") && method !== "GET";
    },

    // 다음 단계 이동 처리
    handleNextStep: function () {
        const current = this.state.currentStep;
        if (current === 1) {
            // draftId가 없거나 선택된 수신자가 0명이면 차단
            if (!this.state.draftId || this.state.draftTotalCount === 0) {
                alert("⚠️ 발송 대상 수신자가 0명입니다.\n테이블에서 수신 대상자를 선택해 주세요.");
                return;
            }

            // MPA 상태 유지를 위해 draftId를 세션스토리지에 저장하고 다음 단계로 이동
            sessionStorage.setItem("draftId", this.state.draftId || '');
            sessionStorage.setItem("draftTotalCount", String(this.state.draftTotalCount));

            this.isNavigatingInternal = true;
            window.location.href = "/send/message";
        } else if (current === 2) {
            // 메시지 작성 화면: 입력값 세션 저장
            sessionStorage.setItem("messageTitle", $("#messageTitle").val() || "");
            sessionStorage.setItem("messageContent", $("#messageContent").val() || "");
            sessionStorage.setItem("messagePurpose", $(".purpose-btn.active").data("val") || "INFO");
            sessionStorage.setItem("messagePriorities", JSON.stringify(this.getCurrentChannelPriorities()));
            sessionStorage.setItem("linkButtonName", $("#linkButtonName").val() || "");
            sessionStorage.setItem("linkUrl", $("#linkUrl").val() || "");
            sessionStorage.setItem("linkPurpose", $(".link-purpose-btn.active").data("purpose") || "CLICK");

            // 추가: 채널 우선순위 ID 리스트 저장
            const channelIds = [];
            $("#channelSortableList li.channel-card").each(function() {
                const id = $(this).data("id");
                if (id) channelIds.push(parseInt(id));
            });
            sessionStorage.setItem("routingChannelIds", JSON.stringify(channelIds));

            this.isNavigatingInternal = true;
            window.location.href = "/send/review";

        } else if (current === 3) {
            // 리뷰 화면: 공통 발송 작업 메시지 생성 요청
            const draftId = sessionStorage.getItem("draftId") || this.state.draftId;
            const title = (sessionStorage.getItem("messageTitle") || "알림").trim();
            const content = (sessionStorage.getItem("messageContent") || "").trim();
            const purpose = sessionStorage.getItem("messagePurpose") || "INFO";
            const linkButtonName = sessionStorage.getItem("linkButtonName") || "";
            const linkUrl = sessionStorage.getItem("linkUrl") || "";
            const linkPurpose = sessionStorage.getItem("linkPurpose") || "CLICK";
            let priorities = [];
            try {
                priorities = JSON.parse(sessionStorage.getItem("messagePriorities") || "[]");
            } catch (e) {
                console.error("Failed to parse channel priorities.");
            }

            if (!draftId) {
                alert("발송 대상 정보가 없습니다. 수신자 선택 단계부터 다시 진행해주세요.");
                this.clearSendSession();
                window.location.href = "/send/recipients";
                return;
            }
            if (!content) {
                alert("메시지 내용을 입력해주세요.");
                window.location.href = "/send/message";
                return;
            }
            if (priorities.length === 0) {
                priorities = ["KAKAO", "EMAIL", "SMS"];
            }

            $.ajax({
                url: '/api/send-requests',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({
                    draftId: draftId,
                    title: title,
                    content: content,
                    purpose: purpose,
                    priorities: priorities,
                    linkButtonName: linkButtonName,
                    linkUrl: linkUrl,
                    linkPurpose: linkPurpose
                }),
                success: (res) => {
                    alert("발송 요청이 생성되었습니다. 준비 대상 " + (res.preparedTargetCount || 0) + "명");
                    this.clearSessionAndDraft();
                },
                error: (err) => {
                    const msg = err.responseJSON ? err.responseJSON.message : "알 수 없는 오류가 발생했습니다.";
                    alert("발송 요청 생성 실패: " + msg);
                }
            });
        }
    },

    getCurrentChannelPriorities: function () {
        if (typeof MessageComposer !== "undefined" && Array.isArray(MessageComposer.channels)) {
            return MessageComposer.channels
                .map(ch => ch.originalType || ch.channelType)
                .filter(Boolean);
        }

        const priorities = [];
        $(".channel-card").each(function () {
            const type = $(this).data("type");
            if (type) priorities.push(type);
        });
        return priorities;
    },

    // 발송 완료 또는 초기화 시 세션/Draft 정리 헬퍼
    clearSessionAndDraft: function () {
        const draftId = this.state.draftId;
        this.isNavigatingInternal = true;
        if (draftId) {
            this.clearSendSession();
            // API 호출로 Redis Draft 비우기
            $.ajax({
                url: '/api/campaigns/draft/' + draftId,
                type: 'DELETE',
                success: function () {
                    window.location.href = "/send/recipients";
                },
                error: function () {
                    window.location.href = "/send/recipients";
                }
            });
        } else {
            window.location.href = "/send/recipients";
        }
    },

    clearSendSession: function () {
        sessionStorage.removeItem("draftId");
        sessionStorage.removeItem("draftTotalCount");
        sessionStorage.removeItem("messageTitle");
        sessionStorage.removeItem("messageContent");
        sessionStorage.removeItem("messagePurpose");
        sessionStorage.removeItem("messagePriorities");
        sessionStorage.removeItem("linkButtonName");
        sessionStorage.removeItem("linkUrl");
        sessionStorage.removeItem("linkPurpose");
        sessionStorage.removeItem("selectedTemplateTitle");
        sessionStorage.removeItem("selectedTemplateContent");
        this.state.draftId = null;
        this.state.draftTotalCount = 0;
    },

    // 이전 단계 이동 처리
    handlePrevStep: function () {
        const current = this.state.currentStep;
        if (current === 2) {
            this.isNavigatingInternal = true;
            window.location.href = "/send/recipients";
        } else if (current === 3) {
            this.isNavigatingInternal = true;
            window.location.href = "/send/message";
        }
    },

    // 상단 진행바 UI 갱신 (activeStep 파라미터 기반 흉내)
    updateStepBarUI: function (step) {
        const $stepWrapper = $("#sendStepsWrapper");

        // Thymeleaf가 렌더링한 구조를 JS로 동적 갱신
        $stepWrapper.find(".step-item").removeClass("active completed");

        $stepWrapper.find(".step-item").each(function () {
            const currentStepNum = parseInt($(this).data("step"));
            if (currentStepNum === step) {
                $(this).addClass("active");
            } else if (currentStepNum < step) {
                $(this).addClass("completed");
            }
        });
    }
};

// ==========================================
// 5. 로드 시 초기화 트리거
// ==========================================
$(function () {
    SendPage.init();
});
