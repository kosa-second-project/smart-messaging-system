const SendReview = {
    estimation: null,
    isSubmitting: false,

    init: function () {
        this.renderMessage();
        this.loadEstimate();
    },

    buildPayload: function () {
        let channelPriorityIds = [];
        try {
            channelPriorityIds = JSON.parse(sessionStorage.getItem("channelPriorityIds") || "[]");
        } catch (e) {
            channelPriorityIds = [];
        }

        const advertising = sessionStorage.getItem("messageAdvertising") === "true";
        return {
            draftId: SendPage.state.draftId,
            title: sessionStorage.getItem("messageTitle") || "",
            content: sessionStorage.getItem("messageContent") || "",
            purpose: advertising ? "AD" : "INFO",
            advertising: advertising,
            scheduledAt: sessionStorage.getItem("scheduledAt") || null,
            linkUrl: sessionStorage.getItem("messageLinkUrl") || null,
            channelPriorityIds: channelPriorityIds
        };
    },

    loadEstimate: function () {
        const payload = this.buildPayload();
        if (!payload.draftId || !payload.title || !payload.content || !payload.channelPriorityIds.length) {
            this.setState("검토에 필요한 정보가 없습니다. 이전 단계에서 다시 확인해주세요.", true);
            return;
        }

        this.setState("예상 비용을 계산하는 중입니다.", false);
        SendApi.estimateCost(payload)
            .done((response) => {
                this.estimation = response;
                this.renderEstimate(response);
                this.setState("", false);
            })
            .fail((xhr) => {
                const message = xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : "예상 비용 계산에 실패했습니다.";
                this.setState(message, true);
            });
    },

    queueCampaign: function () {
        if (this.isSubmitting) return;
        const payload = this.buildPayload();
        this.isSubmitting = true;
        $("#btnNextStep").prop("disabled", true).text("큐 적재 중...");

        SendApi.queueCampaign(payload)
            .done((response) => {
                SendPage.clearSessionAfterQueued();
                alert(response.message || "발송 요청이 큐에 등록되었습니다.");
                window.location.href = "/history";
            })
            .fail((xhr) => {
                const message = xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : "발송 요청 등록에 실패했습니다.";
                alert(message);
            })
            .always(() => {
                this.isSubmitting = false;
                $("#btnNextStep").prop("disabled", false).text("발송하기");
            });
    },

    renderMessage: function () {
        const title = sessionStorage.getItem("messageTitle") || "";
        const content = sessionStorage.getItem("messageContent") || "";
        const advertising = sessionStorage.getItem("messageAdvertising") === "true";
        const linkUrl = sessionStorage.getItem("messageLinkUrl") || "";
        $("#reviewMessageTitle").text(title || "-");
        $("#reviewMessageContent").text(content || "-");
        $("#reviewMessageMeta").text([advertising ? "광고성" : "정보성", linkUrl].filter(Boolean).join(" · "));
    },

    renderEstimate: function (estimate) {
        $("#reviewTotalTargets").text(formatNumber(estimate.totalTargetCount));
        $("#reviewSendableTargets").text(formatNumber(estimate.sendableTargetCount));
        $("#reviewSkippedTargets").text(formatNumber(estimate.skippedTargetCount));
        $("#reviewEstimatedCost").text(formatWon(estimate.estimatedCost));

        const scheduleText = estimate.advertisingSendAt ? formatDateTime(estimate.advertisingSendAt) : "즉시 발송";
        $("#reviewScheduleText").text(scheduleText);
        $("#reviewScheduleHelp").text(estimate.advertisingRestricted ? "광고성 메시지 제한 시간대라 예약 발송으로 전환됩니다." : "");

        const channels = estimate.channels || [];
        if (!channels.length) {
            $("#reviewChannelRows").html("<div class=\"review-empty\">발송 가능한 채널 대상이 없습니다.</div>");
            return;
        }

        $("#reviewChannelRows").html(channels.map((channel) => `
            <div class="review-channel-row">
                <span>${escapeHtml(channel.channelType || "-")}</span>
                <strong>${formatNumber(channel.targetCount)}명 · ${formatWon(channel.estimatedCost)}</strong>
            </div>
        `).join(""));
    },

    setState: function (message, isError) {
        $("#reviewState")
            .toggleClass("is-error", Boolean(isError))
            .text(message || "");
    }
};

function formatNumber(value) {
    return Number(value || 0).toLocaleString();
}

function formatWon(value) {
    return `${Number(value || 0).toLocaleString()}원`;
}

function formatDateTime(value) {
    if (!value) return "즉시 발송";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return date.toLocaleString("ko-KR", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit"
    });
}
