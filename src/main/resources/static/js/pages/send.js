// ==========================================
// 0. HTML escape utility
// ==========================================
function escapeHtml(str) {
    if (str == null) return '';
    const div = document.createElement('div');
    div.textContent = String(str);
    return div.innerHTML;
}

// ==========================================
// 1. DB tag id map loaded from server
// ==========================================
let TAG_MAP = {};

// ==========================================
// 2. Send page global state
// ==========================================
const SendPage = {
    state: {
        currentStep: 1,
        selectedTags: [],
        conditionMode: 'OR',
        draftId: null,
        draftTotalCount: 0,
        searchQuery: '',
        cursorHistory: [null],
        currentCursorIndex: 0,
        nextCursorId: null,
        hasNext: false,
        pageSize: 20,
        activeTab: 'filtered',
        renderSeq: 0
    },

    isNavigatingInternal: false,

    init: function () {
        const path = window.location.pathname;
        if (path.includes("/send/recipients")) {
            this.state.currentStep = 1;
            this.state.draftId = sessionStorage.getItem("draftId") || null;
            this.state.draftTotalCount = parseInt(sessionStorage.getItem("draftTotalCount") || "0");
        } else if (path.includes("/send/message")) {
            this.state.currentStep = 2;
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
        this.updateStepBarUI(this.state.currentStep);

        if (this.state.currentStep === 1) {
            if (typeof RecipientSelector !== "undefined") RecipientSelector.init();
        } else if (this.state.currentStep === 2) {
            if (typeof MessageComposer !== "undefined") MessageComposer.init();
        }
    },

    bindGlobalEvents: function () {
        const self = this;

        $(document).on("click", "[data-action='next-step']", (e) => {
            e.preventDefault();
            this.handleNextStep();
        });

        $(document).on("click", "[data-action='prev-step']", (e) => {
            e.preventDefault();
            this.handlePrevStep();
        });

        window.addEventListener("beforeunload", function (e) {
            if (self.state.draftId && !self.isNavigatingInternal) {
                e.preventDefault();
                e.returnValue = "이 페이지를 벗어나면 선택된 수신자 정보가 초기화됩니다.";
            }
        });

        // Do not delete draft automatically during page navigation or tab visibility changes.
        // Draft cleanup is performed only after successful send or explicit reset.
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
            this.clearMessageDraftSession();
            sessionStorage.setItem("messageEntrySource", "recipients");

            this.isNavigatingInternal = true;
            window.location.href = "/send/message";
        } else if (current === 2) {
            const messageTitle = ($("#messageTitle").val() || "").trim();
            const messageContent = ($("#messageContent").val() || "").trim();
            if (!messageTitle) {
                alert("\uBA54\uC2DC\uC9C0 \uC81C\uBAA9\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694.");
                $("#messageTitle").focus();
                return;
            }
            if (!messageContent) {
                alert("\uBA54\uC2DC\uC9C0 \uB0B4\uC6A9\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694.");
                $("#messageContent").focus();
                return;
            }
            if (typeof MessageComposer !== "undefined" && !MessageComposer.validateMessageLength()) {
                return;
            }
            sessionStorage.setItem("messageTitle", messageTitle);
            sessionStorage.setItem("messageContent", messageContent);
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
        this.isNavigatingInternal = true;
        this.clearSendSession();
        window.location.href = "/send/recipients";
    },

    clearSendSession: function () {
        sessionStorage.removeItem("draftId");
        sessionStorage.removeItem("draftTotalCount");
        this.clearMessageDraftSession();
        this.state.draftId = null;
        this.state.draftTotalCount = 0;
    },

    clearMessageDraftSession: function () {
        sessionStorage.removeItem("messageTitle");
        sessionStorage.removeItem("messageContent");
        sessionStorage.removeItem("messagePurpose");
        sessionStorage.removeItem("messagePriorities");
        sessionStorage.removeItem("linkButtonName");
        sessionStorage.removeItem("linkUrl");
        sessionStorage.removeItem("linkPurpose");
        sessionStorage.removeItem("selectedTemplateTitle");
        sessionStorage.removeItem("selectedTemplateContent");
        sessionStorage.removeItem("messageEntrySource");
    },

    // 이전 단계 이동 처리
    handlePrevStep: function () {
        const current = this.state.currentStep;
        if (current === 2) {
            this.isNavigatingInternal = true;
            window.location.href = "/send/recipients";
        } else if (current === 3) {
            this.isNavigatingInternal = true;
            sessionStorage.setItem("messageEntrySource", "review");
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
