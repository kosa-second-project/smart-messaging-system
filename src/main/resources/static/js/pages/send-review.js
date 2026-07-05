// static/js/pages/send-review.js - 캡처 이미지 디자인 및 동작을 완벽히 구현한 JS

const MessageReviewer = {
    state: {
        title: "",
        content: "",
        totalCount: 0,
        draftId: null,
        routingChannelIds: [2, 1], // 기본값: 2=카카오톡, 1=SMS
        scheduleMode: "IMMEDIATE", // IMMEDIATE 또는 RESERVED
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

    // 1. 세션 데이터 로딩
    loadSessionData: function () {
        this.state.title = sessionStorage.getItem("messageTitle") || "";
        this.state.content = sessionStorage.getItem("messageContent") || "";
        this.state.draftId = sessionStorage.getItem("draftId") || null;
        this.state.totalCount = parseInt(sessionStorage.getItem("draftTotalCount") || "0");

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
                console.error("Failed to parse routingChannels from session", e);
            }
        }
    },

    // 2. 비용 계산 대시보드 동적 렌더링
    renderCostDashboard: function () {
        // A. 대상 인원
        $("#cardTargetCount").text(this.state.totalCount.toLocaleString() + "명");

        // B. 광고 여부 (본문 내 '(광고)'가 있는지 판단)
        const isAd = this.state.content.includes("(광고)");
        $("#cardAdBlocked").text(isAd ? "📢 광고성" : "ℹ️ 정보성");

        // C. 채널 배지 목록
        const $channelList = $("#cardChannels");
        $channelList.empty();

        const numberCircles = ["❶", "❷", "❸", "❹"];

        // 세션에 보관된 채널 오브젝트 리스트 사용, 없으면 가상 디폴트 매핑
        const channelsToRender = (this.state.routingChannels && this.state.routingChannels.length > 0)
            ? this.state.routingChannels
            : [
                { id: 2, channelType: "카카오톡", costPerMsg: 7.0 },
                { id: 1, channelType: "문자메시지(sms/lms)", costPerMsg: 10.0 }
            ];

        channelsToRender.forEach((ch, index) => {
            const numIcon = numberCircles[index] || `${index + 1}`;
            // 문자메시지의 경우 가시성을 위해 타입 이름 단축 치환
            let chName = ch.channelType || `채널 ${ch.id}`;
            if (chName.includes("문자메시지") || chName.toLowerCase() === "sms") {
                chName = "문자메시지";
            }
            
            const badgeHtml = `
                <div class="channel-badge-item" style="margin-right: 0.75rem; display: inline-flex; align-items: center; gap: 0.25rem;">
                    <span class="channel-number-circle">${index + 1}</span> ${chName}
                </div>
            `;
            $channelList.append(badgeHtml);
        });

        // D. 발송 비용 & 절감액 계산 (실제 드래그앤드랍 반영 동적 연산)
        const primaryChannel = channelsToRender[0] || { costPerMsg: 10.0 };
        const primaryUnit = parseFloat(primaryChannel.costPerMsg || 10.0);

        // 예상 비용 = 총원 * 1순위 단가
        const sendCost = this.state.totalCount * primaryUnit;
        $("#cardSendCost").text(sendCost.toLocaleString() + "원");

        // 기준 비용 (기본 SMS 문자 단가 기준인 10원, 혹은 1순위가 문자(SMS)보다 비싸면 그에 맞춰 보정)
        const baseUnit = Math.max(10, primaryUnit);
        const baseCost = this.state.totalCount * baseUnit;

        // 예상 절감액 = 기준 비용 - 예상 비용 (0보다 작으면 0원으로 보정)
        const savedCost = Math.max(0, baseCost - sendCost);
        $("#cardSavedCost").text(savedCost.toLocaleString() + "원");
        
        // 상세 계산 공식 텍스트
        $("#cardSavedCostFormula").text(`${baseCost.toLocaleString()}원 - ${sendCost.toLocaleString()}원`);
    },

    // 3. 페이지 내 버튼 이벤트 바인딩
    bindEvents: function () {
        const self = this;

        // 즉시/예약 발송 탭 전환
        $(".method-tab-btn").on("click", function () {
            $(".method-tab-btn").removeClass("active");
            $(this).addClass("active");

            self.state.scheduleMode = $(this).data("mode");
            const $detailArea = $("#reservationDetailArea");

            if (self.state.scheduleMode === "RESERVED") {
                $detailArea.addClass("active");
                // 날짜와 시간 인풋 초기화
                self.setDateTimeToNowPlus(30); // 기본 30분 뒤
            } else {
                $detailArea.removeClass("active");
                self.state.selectedDate = "";
                self.state.selectedTime = "";
            }
        });

        // 퀵 시간 선택 단축 버튼 클릭
        $(".quick-time-btn").on("click", function () {
            $(".quick-time-btn").removeClass("active");
            $(this).addClass("active");

            const timeMode = $(this).data("time");
            self.setQuickTime(timeMode);
        });

        // 사용자가 날짜/시간 직접 변경 시 퀵 단축 버튼 해제
        $("#inputDate, #inputTime").on("change", function () {
            $(".quick-time-btn").removeClass("active");
            self.state.selectedDate = $("#inputDate").val();
            self.state.selectedTime = $("#inputTime").val();
        });
    },

    // 4. 하단 공통 액션바 커스터마이징 (텍스트를 '발송 요청'으로 변경)
    customizeActionBar: function () {
        const self = this;
        const $nextBtn = $("#btnNextStep");

        if ($nextBtn.length > 0) {
            $nextBtn.html("✈️ 발송 요청")
                    .removeClass("ds-button--success")
                    .css({
                        "background-color": "#10B981", // 초록색
                        "color": "#FFFFFF",
                        "border": "none",
                        "box-shadow": "0 2px 8px rgba(16, 185, 129, 0.25)"
                    });
        }

        // send.js의 스텝 이동 함수를 오버라이드하여 우리의 발송 함수 호출하도록 지정
        if (typeof SendPage !== "undefined") {
            SendPage.handleNextStep = function () {
                self.handleFinalSend();
            };
        }
    },

    // 5. 퀵 예약 날짜/시간 세팅 헬퍼
    setDateTimeToNowPlus: function (minutes) {
        const dateObj = new Date();
        dateObj.setMinutes(dateObj.getMinutes() + minutes);

        const yyyy = dateObj.getFullYear();
        const mm = String(dateObj.getMonth() + 1).padStart(2, '0');
        const dd = String(dateObj.getDate()).padStart(2, '0');
        const hh = String(dateObj.getHours()).padStart(2, '0');
        const min = String(dateObj.getMinutes()).padStart(2, '0');

        this.state.selectedDate = `${yyyy}-${mm}-${dd}`;
        this.state.selectedTime = `${hh}:${min}`;

        $("#inputDate").val(this.state.selectedDate);
        $("#inputTime").val(this.state.selectedTime);
    },

    // 퀵 선택 시 날짜/시간 변환
    setQuickTime: function (mode) {
        const now = new Date();
        if (mode === "today-10") {
            now.setHours(10, 0, 0, 0);
        } else if (mode === "today-14") {
            now.setHours(14, 0, 0, 0);
        } else if (mode === "tomorrow-9") {
            now.setDate(now.getDate() + 1);
            now.setHours(9, 0, 0, 0);
        }

        const yyyy = now.getFullYear();
        const mm = String(now.getMonth() + 1).padStart(2, '0');
        const dd = String(now.getDate()).padStart(2, '0');
        const hh = String(now.getHours()).padStart(2, '0');
        const min = String(now.getMinutes()).padStart(2, '0');

        this.state.selectedDate = `${yyyy}-${mm}-${dd}`;
        this.state.selectedTime = `${hh}:${min}`;

        $("#inputDate").val(this.state.selectedDate);
        $("#inputTime").val(this.state.selectedTime);
    },

    // 6. 최종 발송 수행
    handleFinalSend: function () {
        if (this.state.totalCount === 0) {
            alert("⚠️ 발송 대상 수신자가 없습니다. 1단계에서 대상을 지정해주세요.");
            return;
        }

        let scheduledAtStr = null;
        if (this.state.scheduleMode === "RESERVED") {
            const dateVal = $("#inputDate").val();
            const timeVal = $("#inputTime").val();
            if (!dateVal || !timeVal) {
                alert("⚠️ 예약 발송 날짜와 시간을 입력해 주세요.");
                return;
            }

            // 브라우저의 시간대에 맞추어 ISO 포맷 생성
            const targetDt = new Date(`${dateVal}T${timeVal}`);
            const now = new Date();
            if (targetDt <= new Date(now.getTime() + 1 * 60 * 1000)) {
                alert("⚠️ 예약 발송은 현재 시각 기준 최소 2분 이후부터 설정이 가능합니다.");
                return;
            }
            scheduledAtStr = targetDt.toISOString();
        }

        if (confirm(`📢 총 ${this.state.totalCount.toLocaleString()} 명에게 메시지 일괄 발송을 시작하시겠습니까?`)) {
            const self = this;
            const $nextBtn = $("#btnNextStep");
            $nextBtn.prop("disabled", true).text("처리 중...");

            // URL 추출
            const urlMatches = this.state.content.match(/https?:\/\/[^\s]+/);
            const originalUrl = urlMatches ? urlMatches[0] : null;

            // 통합 대량 발송 API 호출 (MessageSendRequestDTO 규격 연동)
            const payload = {
                title: this.state.title,
                content: this.state.content,
                originalUrl: originalUrl,
                isUrlIncluded: !!originalUrl,
                scheduledAt: scheduledAtStr,
                draftId: this.state.draftId, // 바디로 통합 전송
                purpose: this.state.content.includes("(광고)") ? "AD" : "INFO",
                templateId: null,
                targetCustomerIds: null, // Redis의 draftId를 통해 백엔드에서 일괄 발송 처리
                routingChannelIds: this.state.routingChannelIds
            };

            $.ajax({
                url: '/api/send/campaign',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(payload),
                success: function (res) {
                    alert("🎉 발송 요청이 성공적으로 대기열 큐에 등록되었습니다.");
                    self.clearSessionAndDraft();
                },
                error: function (err) {
                    const msg = err.responseJSON ? err.responseJSON.message : "발송 중 오류가 발생했습니다.";
                    alert("❌ 발송 실패: " + msg);
                    $nextBtn.prop("disabled", false).html("✈️ 발송 요청");
                }
            });
        }
    },

    // 7. 세션 클리어 후 복귀
    clearSessionAndDraft: function () {
        const draftId = this.state.draftId;
        if (draftId) {
            sessionStorage.removeItem("draftId");
            sessionStorage.removeItem("draftTotalCount");
            sessionStorage.removeItem("messageTitle");
            sessionStorage.removeItem("messageContent");
            sessionStorage.removeItem("selectedKakaoFriendsUuids");
            sessionStorage.removeItem("routingChannelIds");
            
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
    }
};

$(function () {
    MessageReviewer.init();
});
