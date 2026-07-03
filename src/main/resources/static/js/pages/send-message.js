// ==========================================
// 4. 2단계: 메시지 작성 및 채널 라우팅 기능 제어 객체
// ==========================================
const MessageComposer = {
    channels: [],
    draggedItem: null,
    smsMaxLength: 90, // DB 연동 전 기본값

    init: function () {
        this.loadChannels();
        this.bindEvents();
        // 1단계에서 가져온 총 대상 수 렌더링
        $("#totalTargetCount").text(SendPage.state.draftTotalCount || 0);
    },

    loadChannels: function () {
        const self = this;
        $.ajax({
            url: "/api/channels/active",
            type: "GET",
            success: function (data) {
                let channels = data || [];
                let sms = channels.find(c => c.channelType && c.channelType.toUpperCase() === 'SMS');
                let lms = channels.find(c => c.channelType && c.channelType.toUpperCase() === 'LMS');

                if (sms && lms) {
                    self.smsMaxLength = sms.maxLength || 90; // DB의 SMS 최대 길이값 저장
                    sms.channelType = "문자메시지(sms/lms)";
                    const smsCost = sms.costPerMsg ? sms.costPerMsg.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 3 }) : "0";
                    const lmsCost = lms.costPerMsg ? lms.costPerMsg.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 3 }) : "0";
                    sms.displayCost = `${smsCost}원 / ${lmsCost}원`;

                    // LMS 삭제
                    channels = channels.filter(c => c.id !== lms.id);
                } else if (sms) {
                    self.smsMaxLength = sms.maxLength || 90;
                    sms.channelType = "문자메시지(sms/lms)";
                }

                self.channels = channels;
                self.renderChannels();
            },
            error: function () {
                alert("채널 정보를 불러오는 데 실패했습니다.");
            }
        });
    },

    renderChannels: function () {
        const $list = $("#channelSortableList");
        $list.empty();

        if (this.channels.length === 0) {
            $list.append("<li style='padding:1rem; text-align:center; color:var(--muted-foreground);'>활성화된 채널이 없습니다.</li>");
            return;
        }

        this.channels.forEach((ch, index) => {
            const safeName = escapeHtml(ch.channelType);
            const basicCost = ch.costPerMsg ? ch.costPerMsg.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 3 }) : "0";
            const safeCostStr = ch.displayCost ? `<b>${escapeHtml(ch.displayCost)}</b>` : `<b>${basicCost}</b>원`;

            // 우선순위 뱃지 (1순위, 2순위, 3순위 ...)
            let badgeClass = "priority-disabled";
            if (index === 0) badgeClass = "priority-1";
            else if (index === 1) badgeClass = "priority-2";
            else if (index === 2) badgeClass = "priority-3";

            const html = `
                <li class="channel-card" draggable="true" data-index="${index}" data-id="${ch.id}">
                    <div class="channel-info">
                        <div class="drag-handle">☰</div>
                        <div class="channel-name">${safeName}</div>
                        <div class="channel-meta">건당 ${safeCostStr}</div>
                    </div>
                    <div class="priority-badge ${badgeClass}">${index + 1}순위</div>
                </li>
            `;
            $list.append(html);
        });

        this.bindDragEvents();
    },

    bindDragEvents: function () {
        const self = this;
        const $cards = $(".channel-card");

        $cards.on("dragstart", function (e) {
            self.draggedItem = this;
            $(this).addClass("dragging");
            e.originalEvent.dataTransfer.effectAllowed = "move";
        });

        $cards.on("dragover", function (e) {
            e.preventDefault();
            e.originalEvent.dataTransfer.dropEffect = "move";

            const target = $(this).closest(".channel-card")[0];
            if (target && target !== self.draggedItem) {
                const bounding = target.getBoundingClientRect();
                const offset = bounding.y + (bounding.height / 2);
                if (e.clientY - offset > 0) {
                    $(target).after(self.draggedItem);
                } else {
                    $(target).before(self.draggedItem);
                }
            }
        });

        $cards.on("dragend", function () {
            $(this).removeClass("dragging");
            self.draggedItem = null;
            self.updateChannelOrder();
        });
    },

    updateChannelOrder: function () {
        const newOrderIds = [];
        $(".channel-card").each(function () {
            newOrderIds.push($(this).data("id"));
        });

        // channels 배열 재배치
        const reordered = [];
        newOrderIds.forEach(id => {
            const found = this.channels.find(c => c.id === id);
            if (found) reordered.push(found);
        });

        this.channels = reordered;
        this.renderChannels(); // 뱃지 재렌더링
    },

    bindEvents: function () {
        const self = this;

        // 최저가 자동정렬 버튼
        $("#btnSmartSort").on("click", function () {
            self.channels.sort((a, b) => (a.costPerMsg || 0) - (b.costPerMsg || 0));
            self.renderChannels();
        });

        // 미리보기 탭 스위칭 이벤트
        $(".preview-tab-btn").on("click", function () {
            $(".preview-tab-btn").removeClass("active");
            $(this).addClass("active");

            const target = $(this).data("target"); // sms, kakao, email
            const $box = $("#phonePreviewBox");
            $box.removeClass("mode-sms mode-kakao mode-email");
            $box.addClass("mode-" + target);
        });

        // 제목 입력 실시간 미리보기 동기화
        $("#messageTitle").on("input", function () {
            const title = $(this).val() || "메시지 제목";
            $("#previewSmsTitle").text(title);
            $("#previewKakaoTitle").text(title);
            $("#previewEmailTitle").text(title);
        });

        // 텍스트 바이트 수 계산 및 미리보기 화면 텍스트 동기화
        $("#messageContent").on("input", function () {
            const text = $(this).val() || "전송할 메시지 내용을 입력해주세요.";
            let byteCount = 0;
            const actualText = $(this).val();

            // 미리보기 화면 3곳 모두 실시간 동기화 (개행 유지)
            $("#previewSmsContent").text(text);
            $("#previewKakaoContent").text(text);
            $("#previewEmailContent").text(text);

            for (let i = 0; i < actualText.length; i++) {
                const charCode = actualText.charCodeAt(i);
                if (charCode <= 0x00007F) byteCount += 1;
                else byteCount += 2; // 한글 등 EUC-KR에서는 2바이트
            }

            $("#currentBytes").text(byteCount);

            const $badge = $("#msgTypeBadge");
            // 기존 하드코딩된 90 대신 DB에서 불러온 동적 최대 길이(smsMaxLength) 사용
            if (byteCount > MessageComposer.smsMaxLength) {
                $badge.text("LMS").removeClass("ds-badge--primary").addClass("ds-badge--secondary");
                $badge.css({ "background-color": "#8E54E9", "color": "white" });
            } else {
                $badge.text("SMS").removeClass("ds-badge--secondary").addClass("ds-badge--primary");
                $badge.css({ "background-color": "", "color": "" });
            }
        });

        // 템플릿 카드 클릭 시 제목과 내용 자동 완성 및 미리보기 연동
        $(document).on("click", ".template-card", function () {
            const title = $(this).attr("data-title") || "";
            const content = $(this).attr("data-template") || "";

            // 입력 필드 세팅
            $("#messageTitle").val(title);
            $("#messageContent").val(content);

            // 실시간 미리보기 업데이트
            $("#messageTitle").trigger("input");
            $("#messageContent").trigger("input");

            // 시각적 피드백 (선택 효과)
            $(".template-card").removeClass("active-card");
            $(this).addClass("active-card");
        });

        // 카카오 나에게 보내기
        $("#btnSendKakaoMe").on("click", function () {
            const title = $("#messageTitle").val().trim() || "알림";
            const content = $("#messageContent").val().trim();
            
            if (!content) {
                alert("카카오톡으로 보낼 메시지 내용을 입력해주세요.");
                return;
            }

            const payload = {
                title: title,
                description: content
            };

            const $btn = $(this);
            const originalText = $btn.html();
            $btn.prop("disabled", true).text("발송 중...");

            $.ajax({
                url: '/api/send/kakao/me',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(payload),
                success: function (res) {
                    $btn.prop("disabled", false).html(originalText);
                    alert("나에게 기본 템플릿 발송에 성공했습니다!");
                },
                error: function (err) {
                    $btn.prop("disabled", false).html(originalText);
                    console.error("카카오 나에게 발송 실패:", err);
                    alert("카카오톡 나에게 발송에 실패했습니다. (로그인이 필요하거나 권한이 없을 수 있습니다)");
                }
            });
        });

        // 카카오톡으로 전체 다이렉트 발송
        $("#btnOpenKakaoFriendsModal").on("click", function () {
            const title = $("#messageTitle").val().trim() || "알림";
            const content = $("#messageContent").val().trim();
            
            if (!content) {
                alert("카카오톡으로 보낼 메시지 내용을 입력해주세요.");
                return;
            }

            // Fetch friends and render modal
            $.ajax({
                url: '/api/send/kakao/friends',
                type: 'GET',
                success: function (friends) {
                    if (!friends || friends.length === 0) {
                        alert("카카오톡 친구 목록이 없습니다. (메시지 수신 동의 필요)");
                        return;
                    }
                    
                    let modalHtml = `
                        <div class="modal-overlay" id="kakaoFriendsOverlay"></div>
                        <div class="kakao-friends-modal" id="kakaoFriendsModal">
                            <div class="modal-header">
                                <h3>카카오 친구 선택</h3>
                                <button type="button" class="btn-close-modal" id="btnCloseKakaoModal">✕</button>
                            </div>
                            <div class="modal-body" style="max-height:300px; overflow-y:auto; padding:15px;">
                                <ul style="list-style:none; padding:0; margin:0;">
                    `;
                    
                    friends.forEach(f => {
                        modalHtml += `
                            <li style="padding:10px; border-bottom:1px solid #eee; display:flex; align-items:center;">
                                <input type="radio" name="selectedKakaoFriend" value="${f.uuid}" id="kf_${f.uuid}" style="margin-right:10px;">
                                <label for="kf_${f.uuid}" style="cursor:pointer;">${escapeHtml(f.profile_nickname) || '이름 없음'}</label>
                            </li>
                        `;
                    });
                    
                    modalHtml += `
                                </ul>
                            </div>
                            <div class="modal-footer" style="padding:15px; border-top:1px solid #eee; text-align:right;">
                                <button type="button" class="ds-button ds-button--primary" id="btnSubmitKakaoFriend">선택 발송</button>
                            </div>
                        </div>
                    `;
                    
                    $("body").append(modalHtml);
                    
                    // Bind events for modal
                    $("#btnCloseKakaoModal, #kakaoFriendsOverlay").on("click", function() {
                        $("#kakaoFriendsOverlay, #kakaoFriendsModal").remove();
                    });
                    
                    $("#btnSubmitKakaoFriend").on("click", function() {
                        const selectedUuid = $("input[name='selectedKakaoFriend']:checked").val();
                        if (!selectedUuid) {
                            alert("발송할 친구를 선택해주세요.");
                            return;
                        }
                        
                        if (!confirm("선택한 친구에게 다이렉트로 메시지를 발송하시겠습니까?")) {
                            return;
                        }
                        
                        const payload = {
                            title: title,
                            description: content,
                            targetUuids: [selectedUuid]
                        };
                        
                        const $btnSubmit = $("#btnSubmitKakaoFriend");
                        const originalText = $btnSubmit.html();
                        $btnSubmit.prop("disabled", true).text("발송 중...");
                        
                        $.ajax({
                            url: '/api/send/kakao',
                            type: 'POST',
                            contentType: 'application/json',
                            data: JSON.stringify(payload),
                            success: function (res) {
                                $btnSubmit.prop("disabled", false).html(originalText);
                                alert(res.message || "카카오 친구에게 발송을 성공했습니다!");
                                $("#kakaoFriendsOverlay, #kakaoFriendsModal").remove();
                            },
                            error: function (err) {
                                $btnSubmit.prop("disabled", false).html(originalText);
                                console.error("카카오 발송 실패:", err);
                                alert(err.responseJSON && err.responseJSON.message ? err.responseJSON.message : "카카오톡 발송에 실패했습니다.");
                            }
                        });
                    });
                },
                error: function () {
                    alert("카카오 친구 목록을 불러오는 데 실패했습니다.");
                }
            });
        });
    }
};

