const MessageReviewer = {
    state: {
        title: "",
        content: "",
        purpose: "INFO",
        totalCount: 0,
        draftId: null,
        routingChannelIds: [],
        scheduleMode: "IMMEDIATE",
        selectedDate: "",
        selectedTime: "",
        routingChannels: []
    },

    init: function () {
        this.loadSessionData();
        this.renderCostDashboard();
        this.bindEvents();
        this.customizeActionBar();
    },

    loadSessionData: function () {
        this.state.title = sessionStorage.getItem("messageTitle") || "";
        this.state.content = sessionStorage.getItem("messageContent") || "";
        this.state.purpose = sessionStorage.getItem("messagePurpose") || (this.state.content.includes("(\uAD11\uACE0)") ? "AD" : "INFO");
        this.state.draftId = sessionStorage.getItem("draftId") || null;
        this.state.totalCount = parseInt(sessionStorage.getItem("draftTotalCount") || "0", 10);

        const channelStr = sessionStorage.getItem("routingChannelIds");
        if (channelStr) {
            try {
                this.state.routingChannelIds = JSON.parse(channelStr);
            } catch (e) {
                console.error("Failed to parse routingChannelIds", e);
            }
        }

        const routingChannelsStr = sessionStorage.getItem("routingChannels");
        if (routingChannelsStr) {
            try {
                this.state.routingChannels = JSON.parse(routingChannelsStr);
            } catch (e) {
                console.error("Failed to parse routingChannels", e);
            }
        }
    },

    renderCostDashboard: function () {
        $("#cardTargetCount").text((this.state.totalCount || 0).toLocaleString() + "명");
        $("#cardAdBlocked").text(this.state.purpose === "AD" ? "광고성" : "정보성");
        $("#cardSendCost").text("계산 중");
        $("#cardSavedCost").text("-");
        $("#cardSavedCostFormula").text("검토 단계 예상치");
        this.renderChannels({});
        this.loadEstimateCost();
    },

    loadEstimateCost: function () {
        if (!this.state.draftId) {
            return;
        }

        const self = this;
        $.ajax({
            url: `/api/campaigns/draft/${this.state.draftId}/estimate-cost`,
            type: "GET",
            data: { priorities: this.getPriorityNames() },
            traditional: true,
            success: function (res) {
                const distribution = self.normalizeDistribution(res.channelDistribution || {});
                const validCount = Number(res.totalValidRecipients || 0);
                const estimatedCost = Number(res.totalEstimatedCost || 0);

                const baselineCost = self.calculateBaselineCost(validCount);
                const savedCost = Math.max(0, baselineCost - estimatedCost);

                $("#cardTargetCount").text(validCount.toLocaleString() + "명");
                $("#cardSendCost").text(self.formatWon(estimatedCost));
                $("#cardSavedCost").text(self.formatWon(savedCost));
                $("#cardSavedCostFormula").text(`${self.formatWon(baselineCost)} - ${self.formatWon(estimatedCost)}`);
                self.renderChannels(distribution);
            },
            error: function (err) {
                console.error("estimate-cost failed", err);
                $("#cardSendCost").text("계산 실패");
            }
        });
    },

    renderChannels: function (distribution) {
        const $channelList = $("#cardChannels");
        $channelList.empty();

        const channelsToRender = (this.state.routingChannels && this.state.routingChannels.length > 0)
            ? this.state.routingChannels
            : this.getPriorityNames().map(name => ({ channelType: name }));

        channelsToRender.forEach((channel, index) => {
            const normalized = this.normalizeChannelType(channel.originalType || channel.channelType);
            const count = distribution[normalized];
            const countText = Number.isFinite(count) ? ` ${count.toLocaleString()}명` : "";
            $channelList.append(`
                <div class="channel-badge-item" style="margin-right: 0.75rem; display: inline-flex; align-items: center; gap: 0.25rem;">
                    <span class="channel-number-circle">${index + 1}</span> ${this.getChannelLabel(normalized)}${countText}
                </div>
            `);
        });

        Object.entries(distribution).forEach(([channelType, count]) => {
            if (channelType === "UNASSIGNED" && count > 0) {
                $channelList.append(`
                    <div class="channel-badge-item" style="margin-right: 0.75rem; display: inline-flex; align-items: center; gap: 0.25rem;">
                        <span class="channel-number-circle">-</span> 발송불가 ${count.toLocaleString()}명
                    </div>
                `);
            }
        });
    },

    bindEvents: function () {
        const self = this;

        $(".method-tab-btn").on("click", function () {
            $(".method-tab-btn").removeClass("active");
            $(this).addClass("active");

            self.state.scheduleMode = $(this).data("mode");
            const $detailArea = $("#reservationDetailArea");

            if (self.state.scheduleMode === "RESERVED") {
                $detailArea.addClass("active");
                self.setDateTimeToNowPlus(30);
            } else {
                $detailArea.removeClass("active");
                self.state.selectedDate = "";
                self.state.selectedTime = "";
            }
        });

        $(".quick-time-btn").on("click", function () {
            $(".quick-time-btn").removeClass("active");
            $(this).addClass("active");
            self.setQuickTime($(this).data("time"));
        });

        $("#inputDate, #inputTime").on("change", function () {
            $(".quick-time-btn").removeClass("active");
            self.state.selectedDate = $("#inputDate").val();
            self.state.selectedTime = $("#inputTime").val();
        });
    },

    customizeActionBar: function () {
        const self = this;
        const $nextBtn = $("#btnNextStep");

        if ($nextBtn.length > 0) {
            $nextBtn.html("발송 요청")
                .removeClass("ds-button--success")
                .css({
                    "background-color": "#10B981",
                    "color": "#FFFFFF",
                    "border": "none",
                    "box-shadow": "0 2px 8px rgba(16, 185, 129, 0.25)"
                });
        }

        if (typeof SendPage !== "undefined") {
            SendPage.handleNextStep = function () {
                self.handleFinalSend();
            };
        }
    },

    setDateTimeToNowPlus: function (minutes) {
        const dateObj = new Date();
        dateObj.setMinutes(dateObj.getMinutes() + minutes);
        this.applyDateTime(dateObj);
    },

    setQuickTime: function (mode) {
        const dateObj = new Date();
        if (mode === "today-10") {
            dateObj.setHours(10, 0, 0, 0);
        } else if (mode === "today-14") {
            dateObj.setHours(14, 0, 0, 0);
        } else if (mode === "tomorrow-9") {
            dateObj.setDate(dateObj.getDate() + 1);
            dateObj.setHours(9, 0, 0, 0);
        }
        this.applyDateTime(dateObj);
    },

    applyDateTime: function (dateObj) {
        const yyyy = dateObj.getFullYear();
        const mm = String(dateObj.getMonth() + 1).padStart(2, "0");
        const dd = String(dateObj.getDate()).padStart(2, "0");
        const hh = String(dateObj.getHours()).padStart(2, "0");
        const min = String(dateObj.getMinutes()).padStart(2, "0");

        this.state.selectedDate = `${yyyy}-${mm}-${dd}`;
        this.state.selectedTime = `${hh}:${min}`;

        $("#inputDate").val(this.state.selectedDate);
        $("#inputTime").val(this.state.selectedTime);
    },

    handleFinalSend: function () {
        if (this.state.totalCount === 0) {
            alert("\uBC1C\uC1A1 \uB300\uC0C1 \uC218\uC2E0\uC790\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4. 1\uB2E8\uACC4\uC5D0\uC11C \uB300\uC0C1\uC744 \uC9C0\uC815\uD574\uC8FC\uC138\uC694.");
            return;
        }
        this.state.title = (this.state.title || sessionStorage.getItem("messageTitle") || "").trim();
        this.state.content = (this.state.content || sessionStorage.getItem("messageContent") || "").trim();
        if (!this.state.title) {
            alert("\uBA54\uC2DC\uC9C0 \uC81C\uBAA9\uC774 \uC5C6\uC2B5\uB2C8\uB2E4. \uBA54\uC2DC\uC9C0 \uC791\uC131 \uB2E8\uACC4\uC5D0\uC11C \uC81C\uBAA9\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694.");
            window.location.href = "/send/message";
            return;
        }
        if (!this.state.content) {
            alert("\uBA54\uC2DC\uC9C0 \uB0B4\uC6A9\uC774 \uC5C6\uC2B5\uB2C8\uB2E4. \uBA54\uC2DC\uC9C0 \uC791\uC131 \uB2E8\uACC4\uC5D0\uC11C \uB0B4\uC6A9\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694.");
            window.location.href = "/send/message";
            return;
        }

        let scheduledAtStr = null;
        if (this.state.scheduleMode === "RESERVED") {
            const dateVal = $("#inputDate").val();
            const timeVal = $("#inputTime").val();
            if (!dateVal || !timeVal) {
                alert("\uC608\uC57D \uBC1C\uC1A1 \uB0A0\uC9DC\uC640 \uC2DC\uAC04\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694.");
                return;
            }

            const targetDt = new Date(`${dateVal}T${timeVal}`);
            if (targetDt <= new Date(Date.now() + 60 * 1000)) {
                alert("\uC608\uC57D \uBC1C\uC1A1\uC740 \uD604\uC7AC \uC2DC\uAC01 \uAE30\uC900 \uCD5C\uC18C 2\uBD84 \uC774\uD6C4\uBD80\uD130 \uC124\uC815\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4.");
                return;
            }
            scheduledAtStr = targetDt.toISOString();
        }

        if (!confirm(`\uCD1D ${this.state.totalCount.toLocaleString()}\uBA85\uC5D0\uAC8C \uBA54\uC2DC\uC9C0 \uBC1C\uC1A1\uC744 \uC694\uCCAD\uD558\uC2DC\uACA0\uC2B5\uB2C8\uAE4C?`)) {
            return;
        }

        const $nextBtn = $("#btnNextStep");
        $nextBtn.prop("disabled", true).text("\uCC98\uB9AC \uC911...");

        const urlMatches = this.state.content.match(/https?:\/\/[^\s]+/);
        const originalUrl = urlMatches ? urlMatches[0] : null;

        const payload = {
            draftId: this.state.draftId,
            templateId: null,
            title: this.state.title,
            content: this.state.content,
            purpose: this.state.purpose,
            priorities: this.getPriorityNames(),
            linkButtonName: originalUrl ? "\uC790\uC138\uD788 \uBCF4\uAE30" : null,
            linkUrl: originalUrl,
            linkPurpose: originalUrl ? "CLICK" : null,
            scheduledAt: scheduledAtStr
        };

        const self = this;
        $.ajax({
            url: "/api/send-requests",
            type: "POST",
            contentType: "application/json",
            data: JSON.stringify(payload),
            success: function () {
                alert("\uBC1C\uC1A1 \uC694\uCCAD\uC774 \uC811\uC218\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uBC1C\uC1A1 \uD604\uD669\uC740 \uB300\uC2DC\uBCF4\uB4DC \uBC0F \uD1B5\uACC4 \uBA54\uB274\uC5D0\uC11C \uD655\uC778\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4.");
                self.clearSessionAndDraft();
            },
            error: function (err) {
                const msg = err.responseJSON ? err.responseJSON.message : "발송 중 오류가 발생했습니다.";
                alert("\uBC1C\uC1A1 \uC2E4\uD328: " + msg);
                $nextBtn.prop("disabled", false).html("발송 요청");
            }
        });
    },

    clearSessionAndDraft: function () {
        const draftId = this.state.draftId;
        if (typeof SendPage !== "undefined") {
            SendPage.isNavigatingInternal = true;
            if (SendPage.state) {
                SendPage.state.draftId = null;
            }
        }

        [
            "draftId",
            "draftTotalCount",
            "messageTitle",
            "messageContent",
            "messagePurpose",
            "selectedKakaoFriendsUuids",
            "routingChannelIds",
            "routingChannels"
        ].forEach(key => sessionStorage.removeItem(key));

        if (draftId) {
            $.ajax({
                url: "/api/campaigns/draft/" + draftId,
                type: "DELETE",
                complete: function () {
                    window.location.href = "/send";
                }
            });
        } else {
            window.location.href = "/send";
        }
    },

    getPriorityNames: function () {
        if (this.state.routingChannels && this.state.routingChannels.length > 0) {
            return this.state.routingChannels.map(channel => this.normalizeChannelType(channel.originalType || channel.channelType));
        }
        return ["KAKAO", "SMS"];
    },

    normalizeDistribution: function (distribution) {
        const normalized = {};
        Object.entries(distribution || {}).forEach(([channelType, count]) => {
            const key = this.normalizeChannelType(channelType);
            normalized[key] = (normalized[key] || 0) + Number(count || 0);
        });
        return normalized;
    },

    normalizeChannelType: function (channelType) {
        const normalized = (channelType || "").trim().toUpperCase();
        if (normalized.includes("문자") || normalized.includes("SMS")) {
            return "SMS";
        }
        if (normalized.startsWith("KAKAO") || normalized.includes("카카오")) {
            return "KAKAO";
        }
        if (normalized.includes("EMAIL") || normalized.includes("이메일")) {
            return "EMAIL";
        }
        if (normalized.includes("LMS")) {
            return "LMS";
        }
        return normalized || "SMS";
    },

    getChannelLabel: function (channelType) {
        const normalized = this.normalizeChannelType(channelType);
        if (normalized === "KAKAO") return "카카오톡";
        if (normalized === "EMAIL") return "이메일";
        if (normalized === "LMS") return "LMS";
        if (normalized === "SMS") return "SMS";
        if (normalized === "UNASSIGNED") return "발송불가";
        return normalized;
    },

    calculateBaselineCost: function (validCount) {
        const unitCosts = (this.state.routingChannels || [])
            .map(channel => Number(channel.costPerMsg || channel.cost || 0))
            .filter(cost => Number.isFinite(cost) && cost > 0);
        const baselineUnitCost = unitCosts.length > 0 ? Math.max(...unitCosts) : 0;
        return Number(validCount || 0) * baselineUnitCost;
    },

    formatWon: function (amount) {
        return Number(amount || 0).toLocaleString(undefined, { maximumFractionDigits: 3 }) + "원";
    }
};

$(function () {
    MessageReviewer.init();
});
