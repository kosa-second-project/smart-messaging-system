/* static/js/pages/send.js */

// ==========================================
// 1. 가상 테스트 데이터 정의 (고객 10명)
// ==========================================
const MOCK_CUSTOMERS = [
    { id: "C001", name: "김민준3", phone: "010-****-1592", type: "일반", tags: ["일반", "sms 동의", "이메일 동의", "남자", "30대"], sms: true, kakao: false, email: true },
    { id: "C002", name: "정도윤3", phone: "010-****-1740", type: "휴면", tags: ["휴면", "이메일 동의", "남자", "40대"], sms: false, kakao: false, email: true },
    { id: "C003", name: "한예준3", phone: "010-****-1814", type: "일반", tags: ["일반", "sms 동의", "카카오 동의", "이메일 동의", "생일 대상자", "남자", "20대"], sms: true, kakao: true, email: true },
    { id: "C004", name: "박지호4", phone: "010-****-1962", type: "신규", tags: ["신규", "sms 동의", "카카오 동의", "이메일 동의", "남자", "10대"], sms: true, kakao: true, email: true },
    { id: "C005", name: "이서연9", phone: "010-****-3405", type: "일반", tags: ["일반", "카카오 동의", "이메일 동의", "여자", "30대"], sms: false, kakao: true, email: true },
    { id: "C006", name: "윤지아9", phone: "010-****-3553", type: "일반", tags: ["일반", "sms 동의", "카카오 동의", "이메일 동의", "여자", "20대"], sms: true, kakao: true, email: true },
    { id: "C007", name: "오서윤9", phone: "010-****-3627", type: "신규", tags: ["신규", "sms 동의", "카카오 동의", "이메일 동의", "생일 대상자", "여자", "40대"], sms: true, kakao: true, email: true },
    { id: "C008", name: "최수아10", phone: "010-****-3775", type: "휴면", tags: ["휴면", "카카오 동의", "이메일 동의", "여자", "50대"], sms: false, kakao: true, email: true },
    { id: "C009", name: "강지원11", phone: "010-****-4819", type: "일반", tags: ["일반", "sms 동의", "카카오 동의", "이메일 동의", "여자", "60대"], sms: true, kakao: true, email: true },
    { id: "C010", name: "조예은12", phone: "010-****-5930", type: "일반", tags: ["일반", "sms 동의", "여자", "70대"], sms: true, kakao: false, email: false }
];

// ==========================================
// 2. 발송 화면 전역 상태 관리 객체
// ==========================================
const SendPage = {
    // 현재 전역 데이터 상태
    state: {
        currentStep: 1,
        selectedTags: JSON.parse(localStorage.getItem("selectedTags") || "[]"),        // 선택된 태그 목록 (String 배열)
        conditionMode: localStorage.getItem("conditionMode") || 'OR',     // 'OR' (하나라도) 또는 'AND' (모두)
        selectedUserIds: new Set(JSON.parse(localStorage.getItem("selectedUserIds") || "[]")), // 직접 체크박스로 선택한 고객 ID (Set)
        searchQuery: '',         // 우측 고객 검색어
        tagSearchQuery: '',      // 좌측 태그 검색어
        currentPage: 1,
        pageSize: 5,             // 한 페이지에 5명씩 렌더링
        activeTab: 'filtered'    // 'filtered'(태그후보), 'displayed'(현재표시), 'selected'(직접선택)
    },

    // 초기화
    init: function() {
        console.log("Send Page Manager Initialized.");

        // 현재 URL 경로를 통해 currentStep 파싱
        const path = window.location.pathname;
        if (path.includes("/send/recipients")) {
            this.state.currentStep = 1;
        } else if (path.includes("/send/message")) {
            this.state.currentStep = 2;
        } else if (path.includes("/send/review")) {
            this.state.currentStep = 3;
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
    bindGlobalEvents: function() {
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
    handleNextStep: function() {
        const current = this.state.currentStep;
        if (current === 1) {
            // 1단계 ➡️ 2단계 진입 시 수신자 유효성 검사 (0명이면 차단)
            const finalRecipients = RecipientSelector.getFinalRecipients();
            if (finalRecipients.length === 0) {
                alert("⚠️ 발송 대상 수신자가 0명입니다.\n태그를 선택하거나 테이블에서 수신 대상자를 선택해 주세요.");
                return;
            }

            // MPA 상태 유지를 위해 선택된 필터 정보 로컬스토리지에 저장
            localStorage.setItem("selectedUserIds", JSON.stringify(Array.from(this.state.selectedUserIds)));
            localStorage.setItem("selectedTags", JSON.stringify(this.state.selectedTags));
            localStorage.setItem("conditionMode", this.state.conditionMode);

            window.location.href = "/send/message";
        } else if (current === 2) {
            window.location.href = "/send/review";
        } else if (current === 3) {
            alert("🎉 스마트 메시징 발송 요청이 최종 완료되었습니다! (시연용 Mock)");
            
            // 데이터 클리어 후 대시보드로 복귀
            localStorage.removeItem("selectedUserIds");
            localStorage.removeItem("selectedTags");
            localStorage.removeItem("conditionMode");
            window.location.href = "/dashboard";
        }
    },

    // 이전 단계 이동 처리
    handlePrevStep: function() {
        const current = this.state.currentStep;
        if (current === 2) {
            window.location.href = "/send/recipients";
        } else if (current === 3) {
            window.location.href = "/send/message";
        }
    },

    // 상단 진행바 UI 갱신 (activeStep 파라미터 기반 흉내)
    updateStepBarUI: function(step) {
        const $stepWrapper = $("#sendStepsWrapper");
        
        // Thymeleaf가 렌더링한 구조를 JS로 동적 갱신
        $stepWrapper.find(".step-item").removeClass("active completed");
        
        $stepWrapper.find(".step-item").each(function() {
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
    init: function() {
        this.bindEvents();
        this.renderAll();
    },

    // 이벤트 바인딩
    bindEvents: function() {
        const self = this;

        // 1. 좌측 태그 버튼 클릭 시 토글 (실시간 반영 복원)
        $(document).on("click", ".tag-item-btn", function() {
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
        $(document).on("click", "#btnConditionOr", function() {
            $(this).addClass("active");
            $("#btnConditionAnd").removeClass("active");
            SendPage.state.conditionMode = 'OR';
            SendPage.state.currentPage = 1;
            self.renderAll();
        });

        $(document).on("click", "#btnConditionAnd", function() {
            $(this).addClass("active");
            $("#btnConditionOr").removeClass("active");
            SendPage.state.conditionMode = 'AND';
            SendPage.state.currentPage = 1;
            self.renderAll();
        });

        // 4. 태그 초기화 버튼 클릭
        $(document).on("click", "#btnResetTags", function() {
            SendPage.state.selectedTags = [];
            SendPage.state.currentPage = 1;
            localStorage.setItem("selectedTags", "[]");
            self.renderAll();
        });

        // 5. 좌측 태그 검색창 실시간 검색
        $("#tagSearchInput").on("input", function() {
            SendPage.state.tagSearchQuery = $(this).val().trim().toLowerCase();
            self.filterTagCloud();
        });

        // 5. 우측 회원 검색창 실시간 검색
        $("#customerSearchInput").on("input", function() {
            SendPage.state.searchQuery = $(this).val().trim().toLowerCase();
            SendPage.state.currentPage = 1;
            self.renderAll();
        });

        // 6. 테이블 개별 체크박스 토글 (직접 선택 추가/해제)
        $(document).on("change", ".customer-checkbox", function() {
            const userId = $(this).data("id");
            if (this.checked) {
                SendPage.state.selectedUserIds.add(userId);
            } else {
                SendPage.state.selectedUserIds.delete(userId);
            }
            self.updateSummaryCounts();
        });

        // 7. 테이블 전체 선택/해제 체크박스 (현재 페이지 목록만 일괄 체크/해제하도록 제약하여 자연스러운 UX 유도)
        $("#thCheckAll").on("change", function() {
            const isChecked = this.checked;
            const filteredList = self.getFilteredList();
            
            // 현재 활성화된 페이지 범위의 고객만 추출
            const startIndex = (SendPage.state.currentPage - 1) * SendPage.state.pageSize;
            const endIndex = Math.min(startIndex + SendPage.state.pageSize, filteredList.length);
            const pagedCustomers = filteredList.slice(startIndex, endIndex);
            
            pagedCustomers.forEach(customer => {
                if (isChecked) {
                    SendPage.state.selectedUserIds.add(customer.id);
                } else {
                    SendPage.state.selectedUserIds.delete(customer.id);
                }
            });
            self.renderTableOnly();
            self.updateSummaryCounts();
        });

        // 8. 페이지 크기 셀렉터 이벤트 바인딩
        $(document).on("change", "#pageSizeSelect", function() {
            SendPage.state.pageSize = parseInt($(this).val(), 10);
            SendPage.state.currentPage = 1;
            self.renderAll();
        });

        // 10. 요약 정보 탭 클릭 제어
        $(".summary-tab").on("click", function() {
            $(".summary-tab").removeClass("active");
            $(this).addClass("active");
            SendPage.state.activeTab = $(this).data("tab-type");
            SendPage.state.currentPage = 1;
            self.renderAll();
        });

        // 11. 페이징 버튼 클릭 처리
        $(document).on("click", ".page-link-btn", function() {
            const page = $(this).data("page");
            if (page) {
                SendPage.state.currentPage = page;
                self.renderTableOnly();
            }
        });
    },

    // 태그 클라우드 검색어 실시간 필터링
    filterTagCloud: function() {
        const query = SendPage.state.tagSearchQuery;
        $(".tag-item-btn").each(function() {
            const tagVal = $(this).data("tag").toLowerCase();
            if (tagVal.includes(query)) {
                $(this).show();
            } else {
                $(this).hide();
            }
        });
    },

    // 모든 렌더링 파이프라인 수행 (태그 칩, 버튼 활성화 상태, 테이블, 페이징 일괄 갱신)
    renderAll: function() {
        this.renderTagListUI();
        this.renderTableOnly();
        this.updateSummaryCounts();
    },

    // 1. 태그 클라우드 내 버튼 활성화/비활성화 상태 동기화
    renderTagListUI: function() {
        $(".tag-item-btn").removeClass("selected");
        SendPage.state.selectedTags.forEach(tag => {
            $(`.tag-item-btn[data-tag='${tag}']`).addClass("selected");
        });
    },

    // 3. 필터링된 고객 목록 테이블 + 페이징 UI 렌더링
    renderTableOnly: function() {
        const $tbody = $("#customerTableBody");
        $tbody.empty();

        const filteredList = this.getFilteredList(); // 현재 선택 탭에 맞는 최종 필터 리스트
        const totalCount = filteredList.length;

        // 페이징 계산
        const startIndex = (SendPage.state.currentPage - 1) * SendPage.state.pageSize;
        const endIndex = Math.min(startIndex + SendPage.state.pageSize, totalCount);
        const pagedList = filteredList.slice(startIndex, endIndex);

        // 테이블 행 렌더링
        if (pagedList.length === 0) {
            $tbody.append('<tr><td colspan="10" style="text-align: center; color: var(--muted-foreground); padding: 2rem;">검색 및 필터 조건에 부합하는 수신자가 없습니다.</td></tr>');
            this.updatePaginationInfo(0, 0, 0);
            this.renderPaginationControls(0);
            return;
        }

        pagedList.forEach(customer => {
            const isChecked = SendPage.state.selectedUserIds.has(customer.id);
            const gender = customer.tags.find(t => t === '남자' || t === '여자') || '-';
            const age = customer.tags.find(t => t.endsWith('대')) || '-';
            const isBirthday = customer.tags.includes('생일 대상자') ? '<span class="ds-badge ds-badge--primary" style="background-color: var(--accent); color: var(--primary); font-weight: var(--font-weight-bold);">대상</span>' : '-';

            const row = `
                <tr>
                    <td style="text-align: center;">
                        <input type="checkbox" class="ds-checkbox customer-checkbox" data-id="${customer.id}" ${isChecked ? 'checked' : ''}>
                    </td>
                    <td style="text-align: left;"><strong>${customer.name}</strong></td>
                    <td style="text-align: left;">${customer.phone}</td>
                    <td style="text-align: center;" class="hide-on-tablet">${customer.type}</td>
                    <td style="text-align: center;" class="hide-on-mobile">${gender}</td>
                    <td style="text-align: center;" class="hide-on-mobile">${age}</td>
                    <td style="text-align: center;" class="hide-on-mobile">${isBirthday}</td>
                    <!-- 수신 거부(X)인 셀에 .is-rejected 및 deny-badge 적용하여 시각적 경고 강조 -->
                    <td style="text-align: center;" class="${!customer.sms ? 'is-rejected' : ''}">
                        ${customer.sms ? '<span class="allow-badge">✓</span>' : '<span class="deny-badge">X</span>'}
                    </td>
                    <td style="text-align: center;" class="${!customer.kakao ? 'is-rejected' : ''}">
                        ${customer.kakao ? '<span class="allow-badge">✓</span>' : '<span class="deny-badge">X</span>'}
                    </td>
                    <td style="text-align: center;" class="${!customer.email ? 'is-rejected' : ''}">
                        ${customer.email ? '<span class="allow-badge">✓</span>' : '<span class="deny-badge">X</span>'}
                    </td>
                </tr>
            `;
            $tbody.append(row);
        });

        // 전체 선택 체크박스 상태 동기화
        const allChecked = pagedList.every(c => SendPage.state.selectedUserIds.has(c.id));
        $("#thCheckAll").prop("checked", allChecked);

        // 페이징 인포 및 콘트롤 그리기
        this.updatePaginationInfo(startIndex + 1, endIndex, totalCount);
        this.renderPaginationControls(totalCount);

        // 페이지 크기 셀렉터 상태 동기화
        $("#pageSizeSelect").val(SendPage.state.pageSize);
    },

    // 페이징 인포 갱신
    updatePaginationInfo: function(start, end, total) {
        $("#lblStartIdx").text(start);
        $("#lblEndIdx").text(end);
        $("#lblTotalIdx").text(total);
    },

    // 페이징 컨트롤 버튼 생성
    renderPaginationControls: function(totalCount) {
        const $controls = $("#paginationControls");
        $controls.empty();

        const totalPages = Math.max(1, Math.ceil(totalCount / SendPage.state.pageSize));

        // [이전] 버튼
        const prevDisabled = SendPage.state.currentPage === 1 ? 'disabled' : '';
        $controls.append(`<button type="button" class="page-link-btn" data-page="${SendPage.state.currentPage - 1}" ${prevDisabled}>이전</button>`);

        // 페이지 번호 버튼들
        for (let i = 1; i <= totalPages; i++) {
            const activeClass = SendPage.state.currentPage === i ? 'active' : '';
            $controls.append(`<button type="button" class="page-link-btn ${activeClass}" data-page="${i}">${i}</button>`);
        }

        // [다음] 버튼
        const nextDisabled = SendPage.state.currentPage === totalPages ? 'disabled' : '';
        $controls.append(`<button type="button" class="page-link-btn" data-page="${SendPage.state.currentPage + 1}" ${nextDisabled}>다음</button>`);
    },

    // 4. 요약 카드 카운터 정보 실시간 업데이트
    updateSummaryCounts: function() {
        const candidateCount = this.getFilteredListByTagsOnly().length;
        const selectedCount = SendPage.state.selectedUserIds.size;

        $("#lblCandidateCount").text(candidateCount);
        $("#lblSelectedCount").text(selectedCount);
    },

    // ==========================================
    // 데이터 필터링 헬퍼 메소드 (API 통신부 추상화 완료)
    // ==========================================

    // A. 태그 조건으로만 필터링한 리스트 반환 (태그 후보 카운트용)
    getFilteredListByTagsOnly: function() {
        const selectedTags = SendPage.state.selectedTags;
        const mode = SendPage.state.conditionMode;

        // 선택된 태그가 없으면 전체 리스트 반환
        if (selectedTags.length === 0) {
            return MOCK_CUSTOMERS;
        }

        return MOCK_CUSTOMERS.filter(customer => {
            if (mode === 'OR') {
                // 하나라도 포함 (OR)
                return selectedTags.some(tag => customer.tags.includes(tag));
            } else {
                // 모두 포함 (AND)
                return selectedTags.every(tag => customer.tags.includes(tag));
            }
        });
    },

    // B. 태그 필터 + 우측 검색어 필터가 모두 결합된 현재 최종 목록 반환 (API 호출부 대응 가능)
    getFilteredList: function() {
        const activeTab = SendPage.state.activeTab;

        // 1단계: 탭 조건에 맞춤
        let baseList = [];
        if (activeTab === 'selected') {
            // 직접 선택한 고객만 보기
            baseList = MOCK_CUSTOMERS.filter(c => SendPage.state.selectedUserIds.has(c.id));
        } else {
            // 태그 조건 적용
            baseList = this.getFilteredListByTagsOnly();
        }

        // 2단계: 우측 검색창 입력값 필터링
        const query = SendPage.state.searchQuery;
        if (!query) return baseList;

        return baseList.filter(customer => {
            return customer.name.toLowerCase().includes(query) ||
                   customer.phone.includes(query) ||
                   customer.type.toLowerCase().includes(query) ||
                   customer.tags.some(t => t.toLowerCase().includes(query));
        });
    },

    // 최종 발송용 수신 대상자 목록 가져오기 (Validation 방어 코드용)
    getFinalRecipients: function() {
        // 직접 체크박스를 선택한 사용자가 있다면 최우선, 없으면 현재 태그 필터링된 모든 사용자를 대상자로 수집
        if (SendPage.state.selectedUserIds.size > 0) {
            return MOCK_CUSTOMERS.filter(c => SendPage.state.selectedUserIds.has(c.id));
        }
        return this.getFilteredListByTagsOnly();
    }
};

// ==========================================
// 4. 로드 시 초기화 트리거
// ==========================================
$(function() {
    SendPage.init();
});
