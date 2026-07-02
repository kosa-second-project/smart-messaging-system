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
        currentPage: 1,
        pageSize: 20,                // 한 페이지에 20명씩 렌더링 (기본값)
        activeTab: 'filtered',       // 'filtered'(태그후보), 'selected'(직접선택)
        renderSeq: 0                 // AJAX 요청 순번: 이전 응답 덮어쓰기(Race Condition) 방지
    },

    // 초기화
    init: function () {


        // 현재 URL 경로를 통해 currentStep 파싱
        const path = window.location.pathname;
        if (path.includes("/send/recipients")) {
            this.state.currentStep = 1;
            // 1단계 최초 진입 시: 이전 찌꺼기 완전 청소
            localStorage.removeItem("draftId");
            localStorage.removeItem("draftTotalCount");
        } else if (path.includes("/send/message")) {
            this.state.currentStep = 2;
            // 2단계 진입: 1단계에서 저장해 둔 draftId 복원
            this.state.draftId = localStorage.getItem("draftId") || null;
            this.state.draftTotalCount = parseInt(localStorage.getItem("draftTotalCount") || "0");
        } else if (path.includes("/send/review")) {
            this.state.currentStep = 3;
            this.state.draftId = localStorage.getItem("draftId") || null;
            this.state.draftTotalCount = parseInt(localStorage.getItem("draftTotalCount") || "0");
        } else {
            this.state.currentStep = 1;
        }

        this.bindGlobalEvents();

        // 현재 단계에 맞춘 초기 UI 동기화
        this.updateStepBarUI(this.state.currentStep);

        if (this.state.currentStep === 1) {
            RecipientSelector.init();
        }
    },

    // 전역 이벤트 바인딩 (이전/다음 화면 전환 등)
    bindGlobalEvents: function () {
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

            // MPA 상태 유지를 위해 draftId를 로컬스토리지에 저장하고 다음 단계로 이동
            localStorage.setItem("draftId", this.state.draftId || '');
            localStorage.setItem("draftTotalCount", String(this.state.draftTotalCount));

            window.location.href = "/send/message";
        } else if (current === 2) {
            window.location.href = "/send/review";
        } else if (current === 3) {
            alert("🎉 스마트 메시징 발송 요청이 최종 완료되었습니다! (시연용 Mock)");

            // 발송 완료 시 Redis Draft 정리 + 로컬스토리지 초기화
            const draftId = this.state.draftId;
            if (draftId) {
                DraftApi.delete(draftId);
            }
            localStorage.removeItem("draftId");
            localStorage.removeItem("draftTotalCount");
            window.location.href = "/dashboard";
        }
    },

    // 이전 단계 이동 처리
    handlePrevStep: function () {
        const current = this.state.currentStep;
        if (current === 2) {
            window.location.href = "/send/recipients";
        } else if (current === 3) {
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
// 3. 1단계: 수신자 선택 기능 제어 객체
// ==========================================
const RecipientSelector = {
    init: function () {
        this.bindEvents();
        this.renderAll();
    },

    // API 요청용 파라미터 빌드
    getQueryParams: function (includePage = true) {
        // 선택한 태그 명칭 -> DB 태그 ID
        const tagIds = SendPage.state.selectedTags.map(tag => TAG_MAP[tag]).filter(Boolean);

        const params = {
            keyword: SendPage.state.searchQuery || '',
            matchType: SendPage.state.conditionMode
        };

        if (tagIds.length > 0) {
            params.tagIds = tagIds; // jQuery ajax는 배열을 자동으로 tagIds=1&tagIds=2 형태로 직렬화함
        }

        params.activeTab = SendPage.state.activeTab;

        if (SendPage.state.draftId) {
            params.draftId = SendPage.state.draftId;
        }

        // 'selected' 탭 활성화 시
        if (SendPage.state.activeTab === 'selected') {
            if (!SendPage.state.draftId) {
                // draftId가 없으면 결과 없음 처리
                params.customerIds = "-1";
            }
        }

        if (includePage) {
            params.page = SendPage.state.currentPage;
            params.size = SendPage.state.pageSize;
        }


        return params;
    },

    // 이벤트 바인딩
    bindEvents: function () {
        const self = this;

        // 1. 좌측 태그 버튼 클릭 시 토글 (실시간 반영 복원)
        $(document).on("click", ".tag-item-btn", function () {
            const tag = $(this).data("tag");
            const index = SendPage.state.selectedTags.indexOf(tag);

            if (index > -1) {
                SendPage.state.selectedTags.splice(index, 1);
            } else {
                SendPage.state.selectedTags.push(tag);
            }

            SendPage.state.currentPage = 1; // 페이지 초기화
            self.renderAll();
        });

        // 2. 조건 버튼 (AND/OR) 토글 클릭 (실시간 반영 복원)
        $(document).on("click", "#btnConditionOr", function () {
            $(this).addClass("active");
            $("#btnConditionAnd").removeClass("active");
            SendPage.state.conditionMode = 'OR';
            SendPage.state.currentPage = 1;
            self.renderAll();
        });

        $(document).on("click", "#btnConditionAnd", function () {
            $(this).addClass("active");
            $("#btnConditionOr").removeClass("active");
            SendPage.state.conditionMode = 'AND';
            SendPage.state.currentPage = 1;
            self.renderAll();
        });

        // 3. 태그 초기화 버튼 클릭
        $(document).on("click", "#btnResetTags", function () {
            SendPage.state.selectedTags = [];
            SendPage.state.currentPage = 1;
            self.renderAll();
        });

        // 4. 우측 회원 검색창 실시간 검색 (Debounce 300ms)
        let searchTimer;
        $("#customerSearchInput").on("input", function () {
            SendPage.state.searchQuery = $(this).val().trim().toLowerCase();
            clearTimeout(searchTimer);
            searchTimer = setTimeout(function () {
                SendPage.state.currentPage = 1;
                self.renderAll();
            }, 300);
        });

        // 5. 테이블 개별 체크박스 토글 → Redis Draft와 즉시 동기화
        $(document).on("change", ".customer-checkbox", function () {
            const $chk = $(this);
            const userId = Number($chk.data("id"));
            const action = this.checked ? "ADD" : "REMOVE";

            self.ensureDraft(function (draftId) {
                DraftApi.patchRecipients(draftId, [{ customerId: userId, action: action }])
                    .done(function (res) {
                        if (res && res.totalCount !== undefined) {
                            SendPage.state.draftTotalCount = res.totalCount;
                        }
                        self.updateSelectedCount();
                    })
                    .fail(function () {
                        alert("선택 상태 동기화에 실패했습니다. 다시 시도해주세요.");
                        $chk.prop("checked", !$chk.prop("checked"));
                    });
            }, function () {
                alert("수신자 저장소 초기화 중 오류가 발생했습니다.");
                $chk.prop("checked", !$chk.prop("checked"));
            });
        });

        // 6. 테이블 전체 선택/해제 체크박스 (현재 페이지 기준)
        $("#thCheckAll").on("change", function () {
            const isChecked = this.checked;
            const items = [];

            $(".customer-checkbox").each(function () {
                const userId = Number($(this).data("id"));
                $(this).prop("checked", isChecked);
                items.push({ customerId: userId, action: isChecked ? "ADD" : "REMOVE" });
            });

            if (items.length === 0) return;

            self.ensureDraft(function (draftId) {
                DraftApi.patchRecipients(draftId, items)
                    .done(function (res) {
                        if (res && res.totalCount !== undefined) {
                            SendPage.state.draftTotalCount = res.totalCount;
                        }
                        self.updateSelectedCount();
                    })
                    .fail(function () {
                        alert("전체 선택 동기화에 실패했습니다. 다시 시도해주세요.");
                        $(".customer-checkbox").each(function () {
                            $(this).prop("checked", !isChecked);
                        });
                        $("#thCheckAll").prop("checked", !isChecked);
                    });
            });
        });

        // 7. 페이지 크기 셀렉터 이벤트 바인딩
        $(document).on("change", "#pageSizeSelect", function () {
            SendPage.state.pageSize = parseInt($(this).val(), 10);
            SendPage.state.currentPage = 1;
            self.renderAll();
        });

        // 8. 토글 스위치 변경 제어
        $("#toggleShowSelected").on("change", function () {
            SendPage.state.activeTab = this.checked ? "selected" : "filtered";
            SendPage.state.currentPage = 1;
            self.renderAll();
        });

        // 9. 페이징 버튼 클릭 처리
        $(document).on("click", ".page-link-btn", function () {
            const page = $(this).data("page");
            if (page) {
                SendPage.state.currentPage = page;
                self.renderTableOnly();
            }
        });

        // 10. "현재 회원 모두 추가" 버튼 클릭 → POST /api/campaigns/draft 로 Redis에 저장
        $(document).on("click", "#btnAddAllFiltered", function () {
            const params = self.getQueryParams(false);

            DraftApi.addAllFiltered(params)
                .done(function (res) {
                    if (!res || !res.draftId) {
                        alert("현재 필터 조건에 해당하는 회원이 없습니다.");
                        return;
                    }
                    // 삭제하지 않음. 백엔드에서 동일한 draftId에 이어서(Append) 담아줌.
                    SendPage.state.draftId = res.draftId;
                    SendPage.state.draftTotalCount = res.totalCount || 0;
                    self.renderAll();
                    alert(`총 ${res.totalCount || '?'}명의 회원이 선택 목록에 추가되었습니다.`);
                })
                .fail(function () {
                    alert("수신자 목록 저장에 실패했습니다.");
                });
        });

        // 11. "선택 회원 모두 제거" 버튼 클릭 → Redis Draft 삭제
        $(document).on("click", "#btnRemoveAllSelected", function () {
            if (!SendPage.state.draftId) {
                alert("현재 선택된 회원이 없습니다.");
                return;
            }
            if (confirm("선택된 회원 전체를 목록에서 제거하시겠습니까?")) {
                const draftId = SendPage.state.draftId;
                DraftApi.delete(draftId)
                    .done(function () {
                        SendPage.state.draftId = null;
                        SendPage.state.draftTotalCount = 0;
                        self.renderAll();
                    });
            }
        });
    },

    // 모든 렌더링 파이프라인 수행 (태그 칩, 버튼 활성화 상태, 테이블, 페이징 일괄 갱신)
    renderAll: function () {
        this.renderTagListUI();

        // 토글 스위치 상태 동기화 및 버튼 노출 분기 처리
        const isSelectedView = SendPage.state.activeTab === 'selected';
        $("#toggleShowSelected").prop("checked", isSelectedView);

        if (isSelectedView) {
            $("#btnAddAllFiltered").hide();
            $("#btnRemoveAllSelected").show();
        } else {
            $("#btnAddAllFiltered").show();
            $("#btnRemoveAllSelected").hide();
        }

        this.renderTableOnly();      // 테이블 + 후보 카운트(filtered 탭 한정) 갱신
        this.updateSelectedCount();  // 선택 카운트만 즉시 갱신 (API 호출 없음)

        // selected 탭일 때만 별도 API로 후보 카운트 조회
        if (isSelectedView) {
            this.updateCandidateCount();
        }
    },

    // 1. 태그 클라우드 내 버튼 활성화/비활성화 상태 동기화
    renderTagListUI: function () {
        $(".tag-item-btn").removeClass("selected");
        SendPage.state.selectedTags.forEach(tag => {
            $(`.tag-item-btn[data-tag='${tag}']`).addClass("selected");
        });
    },

    // Draft ID 보장 헬퍼: draftId가 없으면 빈 Draft 생성 후 콜백 실행
    ensureDraft: function (callback, onFail) {
        if (SendPage.state.draftId) {
            callback(SendPage.state.draftId);
            return;
        }
        DraftApi.createEmpty()
            .done(function (res) {
                if (res && res.draftId) {
                    SendPage.state.draftId = res.draftId;
                    SendPage.state.draftTotalCount = res.totalCount || 0;
                    callback(res.draftId);
                } else if (onFail) {
                    onFail();
                }
            })
            .fail(function () {
                if (onFail) onFail();
            });
    },

    // 3. 필터링된 고객 목록 테이블 + 페이징 UI 렌더링 (서버 API 비동기 연동)
    renderTableOnly: function () {
        const self = this;
        const $tbody = $("#customerTableBody");
        $tbody.empty();

        // Race Condition 방지: 요청 순번을 매겨 이전 응답이 늦게 도착해도 무시하도록 처리
        const mySeq = ++SendPage.state.renderSeq;

        const params = this.getQueryParams(true);

        CustomerApi.search(params)
            .done(function (response) {
                // 이 응답이 오는 사이에 새 요청이 발생했으면 (순번이 바뀌었으면) 무시
                if (mySeq !== SendPage.state.renderSeq) return;

                const pagedList = response.content || [];
                // totalCount는 항상 서버 응답값 사용
                // - selected 탭: 백엔드가 Redis의 전체 인원 수를 반환함
                // - filtered 탭: 백엔드가 Oracle 전체 카운트를 반환함
                const totalCount = response.totalCount || 0;

                // 테이블 행 렌더링
                if (pagedList.length === 0) {
                    $tbody.empty();
                    $tbody.append('<tr><td colspan="10" style="text-align: center; color: var(--muted-foreground); padding: 2rem;">검색 및 필터 조건에 부합하는 수신자가 없습니다.</td></tr>');
                    self.updatePaginationInfo(0, 0, 0);
                    self.renderPaginationControls(0);
                    $("#thCheckAll").prop("checked", false);
                    return;
                }

                pagedList.forEach(customer => {
                    // selected 탭: 항상 체크된 상태
                    // filtered 탭: Redis 내 존재 여부(customer.isInDraft)에 따라 체크
                    const isChecked = SendPage.state.activeTab === 'selected' || customer.isInDraft;
                    const gender = customer.tags.find(t => t === '남자' || t === '여자') || '-';
                    const age = customer.tags.find(t => t.endsWith('대')) || '-';
                    const type = customer.tags.find(t => t === '일반' || t === '신규') || '-';
                    const isBirthday = customer.tags.includes('생일 대상자') ? '<span class="ds-badge ds-badge--primary" style="background-color: var(--accent); color: var(--primary); font-weight: var(--font-weight-bold);">대상</span>' : '-';

                    const hasSms = customer.tags.includes('sms 동의');
                    const hasKakao = customer.tags.includes('카카오 동의');
                    const hasEmail = customer.tags.includes('이메일 동의');

                    // XSS 방지를 위해 사용자 데이터 이스케이프 처리
                    const safeName = escapeHtml(customer.name);
                    const safePhone = escapeHtml(customer.phone);

                    const row = `
                        <tr>
                            <td style="text-align: center;">
                                <input type="checkbox" class="ds-checkbox customer-checkbox" data-id="${customer.id}" ${isChecked ? 'checked' : ''}>
                            </td>
                            <td style="text-align: left;"><strong>${safeName}</strong></td>
                            <td style="text-align: left;">${safePhone}</td>
                            <td style="text-align: center;" class="hide-on-tablet">${type}</td>
                            <td style="text-align: center;" class="hide-on-mobile">${gender}</td>
                            <td style="text-align: center;" class="hide-on-mobile">${age}</td>
                            <td style="text-align: center;" class="hide-on-mobile">${isBirthday}</td>
                            <td style="text-align: center;" class="${!hasSms ? 'is-rejected' : ''}">
                                ${hasSms ? '<span class="allow-badge">✓</span>' : '<span class="deny-badge">X</span>'}
                            </td>
                            <td style="text-align: center;" class="${!hasKakao ? 'is-rejected' : ''}">
                                ${hasKakao ? '<span class="allow-badge">✓</span>' : '<span class="deny-badge">X</span>'}
                            </td>
                            <td style="text-align: center;" class="${!hasEmail ? 'is-rejected' : ''}">
                                ${hasEmail ? '<span class="allow-badge">✓</span>' : '<span class="deny-badge">X</span>'}
                            </td>
                        </tr>
                    `;
                    $tbody.append(row);
                });

                // 전체 선택 체크박스 상태 동기화
                // selected 탭: 모두 체크된 상태
                // filtered 탭: 현재 페이지의 모든 회원이 draft에 포함되어 있는 경우 체크
                const allChecked = SendPage.state.activeTab === 'selected' ||
                    (pagedList.length > 0 && pagedList.every(c => c.isInDraft));
                $("#thCheckAll").prop("checked", allChecked);

                // 페이징 정보 계산
                const startIndex = (SendPage.state.currentPage - 1) * SendPage.state.pageSize;
                const endIndex = Math.min(startIndex + SendPage.state.pageSize, totalCount);

                self.updatePaginationInfo(totalCount > 0 ? startIndex + 1 : 0, endIndex, totalCount);
                self.renderPaginationControls(totalCount);

                // 페이지 크기 셀렉터 상태 동기화
                $("#pageSizeSelect").val(SendPage.state.pageSize);

                // filtered 탭: 검색 응답의 totalCount를 후보 카운트로 직접 사용 (별도 API 호출 절약)
                if (SendPage.state.activeTab === 'filtered') {
                    $("#lblCandidateCount").text(totalCount);
                }
            })
            .fail(function () {
                $tbody.append('<tr><td colspan="10" style="text-align: center; color: var(--muted-foreground); padding: 2rem;">데이터를 불러오는 중 오류가 발생했습니다.</td></tr>');
            });
    },

    // 페이징 인포 갱신
    updatePaginationInfo: function (start, end, total) {
        $("#lblStartIdx").text(start);
        $("#lblEndIdx").text(end);
        $("#lblTotalIdx").text(total);
    },

    // 페이징 컨트롤 버튼 생성
    renderPaginationControls: function (totalCount) {
        const $controls = $("#paginationControls");
        $controls.empty();

        const totalPages = Math.max(1, Math.ceil(totalCount / SendPage.state.pageSize));

        // [이전] 버튼
        const prevDisabled = SendPage.state.currentPage === 1 ? 'disabled' : '';
        $controls.append(`<button type="button" class="page-link-btn" data-page="${SendPage.state.currentPage - 1}" ${prevDisabled}>이전</button>`);

        // 페이지 노출 계산 (최대 5개만 출력)
        let startPage = Math.max(1, SendPage.state.currentPage - 2);
        let endPage = Math.min(totalPages, startPage + 4);

        // 만약 마지막 페이지 근처라서 5개가 다 안 채워지면 시작 페이지를 앞으로 당겨서 5개 유지
        if (endPage - startPage < 4) {
            startPage = Math.max(1, endPage - 4);
        }

        // 페이지 번호 버튼들 생성
        for (let i = startPage; i <= endPage; i++) {
            const activeClass = SendPage.state.currentPage === i ? 'active' : '';
            $controls.append(`<button type="button" class="page-link-btn ${activeClass}" data-page="${i}">${i}</button>`);
        }

        // [다음] 버튼
        const nextDisabled = SendPage.state.currentPage === totalPages ? 'disabled' : '';
        $controls.append(`<button type="button" class="page-link-btn" data-page="${SendPage.state.currentPage + 1}" ${nextDisabled}>다음</button>`);
    },

    // 4. 선택 카운트만 갱신 (API 호출 없음)
    updateSelectedCount: function () {
        $("#lblSelectedCount").text(SendPage.state.draftTotalCount || 0);
    },

    // 5. 후보 카운트 갱신 (selected 탭에서만 별도 API 호출)
    updateCandidateCount: function () {
        const params = this.getQueryParams(false);
        delete params.customerIds;

        CustomerApi.getIds(params)
            .done(function (ids) {
                $("#lblCandidateCount").text(ids ? ids.length : 0);
            })
            .fail(function () {
                $("#lblCandidateCount").text("0");
            });
    }
};

// ==========================================
// 4. 로드 시 초기화 트리거
// ==========================================
$(function () {
    SendPage.init();
});

