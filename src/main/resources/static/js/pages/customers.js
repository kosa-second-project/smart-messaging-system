let currentTab = 'members';     // members (고객 목록), rejects (수신 거부 목록)
let currentPage = 1;
let pageSize = 10;
let selectedType = '';     // 일반, 신규, 휴면, 전체('')
let searchKeyword = '';
let sortOrder = 'latest';

// 페이지 진입 시 최초 렌더링
document.addEventListener("DOMContentLoaded", function() {
    renderHeader();
    fetchData();
    fetchStats();
});

// 고객 유형 통계 실시간 조회 패치
function fetchStats() {
    fetch('/api/customers/stats')
        .then(res => res.json())
        .then(data => {
            document.getElementById('statTotal').innerText = `${(data.totalCount || 0).toLocaleString()} 명`;
            document.getElementById('statRegular').innerText = `${(data.regularCount || 0).toLocaleString()} 명`;
            document.getElementById('statNew').innerText = `${(data.newCount || 0).toLocaleString()} 명`;
            document.getElementById('statDormant').innerText = `${(data.dormantCount || 0).toLocaleString()} 명`;
        })
        .catch(err => {
            console.error("AJAX Stats Fetch Error:", err);
        });
}

// 탭 전환 기능
function switchTab(tab) {
    currentTab = tab;
    currentPage = 1;
    
    // 탭 버튼 스타일 갱신
    document.getElementById('tabMembersBtn').classList.toggle('active', tab === 'members');
    document.getElementById('tabRejectsBtn').classList.toggle('active', tab === 'rejects');

    // 유형 필터 보임/숨김 제어 (수신거부 탭에서도 유형 필터 상시 활성화)
    const typeFilterWrapper = document.getElementById('typeFilterWrapper');
    typeFilterWrapper.style.opacity = '1';
    typeFilterWrapper.style.pointerEvents = 'auto';

    // 인풋 및 필터 초기화
    document.getElementById('customerSearchInput').value = '';
    searchKeyword = '';
    selectedType = '';
    
    // 필터 버튼 활성 스타일 리셋
    const buttons = document.querySelectorAll('#typeFilterWrapper .filter-button');
    buttons.forEach((btn, idx) => {
        btn.classList.toggle('active', idx === 0);
    });

    renderHeader();
    fetchData();
}

// 테이블 헤더 그리기
function renderHeader() {
    const header = document.getElementById('tableHeader');
    if (currentTab === 'members') {
        header.innerHTML = `
            <tr>
                <th style="width: 120px;">고객명</th>
                <th style="width: 150px;">전화번호</th>
                <th style="width: 90px;">유형</th>
                <th>태그</th>
                <th style="width: 90px; text-align: center;">SMS</th>
                <th style="width: 90px; text-align: center;">카카오</th>
                <th style="width: 90px; text-align: center;">이메일</th>
                <th style="width: 120px;">가입일</th>
            </tr>
        `;
    } else {
        header.innerHTML = `
            <tr>
                <th style="width: 30%;">고객명</th>
                <th style="width: 30%;">전화번호</th>
                <th style="width: 40%;">수신거부 일시</th>
            </tr>
        `;
    }
}

// 검색 처리
function handleSearch(event) {
    searchKeyword = event.target.value.trim();
    currentPage = 1;
    fetchData();
}

// 고객 유형 필터 변경
function filterType(type, element) {
    selectedType = type;
    currentPage = 1;

    const buttons = document.querySelectorAll('#typeFilterWrapper .filter-button');
    buttons.forEach(btn => btn.classList.remove('active'));
    element.classList.add('active');

    fetchData();
}

// 정렬 순서 변경
function changeSortOrder(val) {
    sortOrder = val;
    currentPage = 1;
    fetchData();
}

// 데이터 페치 AJAX 호출
function fetchData() {
    const tbody = document.getElementById('tableBody');
    tbody.innerHTML = `<tr><td colspan="10" style="text-align: center; padding: 40px; color: var(--muted-foreground);">데이터를 불러오는 중입니다...</td></tr>`;

    // 검색어 형식 구분 (숫자만 들어왔을 때는 휴대전화 필터로, 텍스트가 섞이면 이름 필터로 매칭)
    let phoneParam = '';
    let nameParam = '';
    if (/^[0-9-]+$/.test(searchKeyword)) {
        phoneParam = searchKeyword;
    } else {
        nameParam = searchKeyword;
    }

    let url = '/api/customers/manage';
    let params = `page=${currentPage}&size=${pageSize}&sortOrder=${sortOrder}`;

    if (currentTab === 'members') {
        if (nameParam) params += `&name=${encodeURIComponent(nameParam)}`;
        if (phoneParam) params += `&phone=${encodeURIComponent(phoneParam)}`;
        if (selectedType) params += `&customerType=${encodeURIComponent(selectedType)}`;
    } else {
        url = '/api/customers/rejects';
        if (nameParam) params += `&name=${encodeURIComponent(nameParam)}`;
        if (phoneParam) params += `&phone=${encodeURIComponent(phoneParam)}`;
        if (selectedType) params += `&customerType=${encodeURIComponent(selectedType)}`;
    }

    fetch(`${url}?${params}`)
        .then(res => res.json())
        .then(data => {
            renderTable(data.list || []);
            renderPagination(data);
        })
        .catch(err => {
            console.error("AJAX Fetch Error:", err);
            tbody.innerHTML = `<tr><td colspan="10" style="text-align: center; padding: 40px; color: var(--destructive);">데이터를 불러오는 도중 오류가 발생했습니다.</td></tr>`;
        });
}

// 테이블 리스트 렌더링
function renderTable(list) {
    const tbody = document.getElementById('tableBody');
    tbody.innerHTML = '';

    if (list.length === 0) {
        const colSpan = currentTab === 'members' ? 8 : 4;
        tbody.innerHTML = `<tr><td colspan="${colSpan}" style="text-align: center; padding: 60px; color: var(--muted-foreground);">조건에 부합하는 고객 정보가 없습니다.</td></tr>`;
        return;
    }

    list.forEach(item => {
        const tr = document.createElement('tr');
        tr.onclick = function() { openDetailModal(item.customerId); };

        if (currentTab === 'members') {
            // 고객 유형 스타일 배지 셋팅
            let typeBadgeClass = 'badge-gray';
            if (item.customerType === '일반') typeBadgeClass = 'badge-blue';
            else if (item.customerType === '신규') typeBadgeClass = 'badge-green';
            else if (item.customerType === '휴면') typeBadgeClass = 'badge-purple';

            // 수신 동의 여부 체크 기호
            const smsHtml = item.smsConsent ? '<span class="consent-icon ok">✔</span>' : '<span class="consent-icon no">✖</span>';
            const kakaoHtml = item.kakaoConsent ? '<span class="consent-icon ok">✔</span>' : '<span class="consent-icon no">✖</span>';
            const emailHtml = item.emailConsent ? '<span class="consent-icon ok">✔</span>' : '<span class="consent-icon no">✖</span>';

            // 태그 렌더링 (최대 3개 노출)
            let tagsHtml = '';
            if (item.tags && item.tags.length > 0) {
                tagsHtml = item.tags.slice(0, 3).map(t => `<span class="tag-chip">${t}</span>`).join('');
            }

            tr.innerHTML = `
                <td style="font-weight: var(--font-weight-bold);">${escapeHtml(item.name)}</td>
                <td class="font-mono">${escapeHtml(maskPhoneNumber(item.phone))}</td>
                <td><span class="badge ${typeBadgeClass}">${escapeHtml(item.customerType)}</span></td>
                <td><div style="display: flex; flex-wrap: wrap; gap: 4px; max-width: 260px;">${tagsHtml}</div></td>
                <td style="text-align: center;">${smsHtml}</td>
                <td style="text-align: center;">${kakaoHtml}</td>
                <td style="text-align: center;">${emailHtml}</td>
                <td style="color: var(--muted-foreground);">${item.joinedAt || '-'}</td>
            `;
        } else {
            tr.innerHTML = `
                <td style="font-weight: var(--font-weight-bold);">${escapeHtml(item.name)}</td>
                <td class="font-mono">${escapeHtml(maskPhoneNumber(item.phone))}</td>
                <td style="color: var(--muted-foreground);">${item.rejectedAt || '-'}</td>
            `;
        }

        tbody.appendChild(tr);
    });
}

// 페이징 풋터 렌더링
function renderPagination(pageData) {
    const infoText = document.getElementById('pageInfoText');
    const btnContainer = document.getElementById('paginationButtons');

    const total = pageData.totalCount || 0;
    const current = pageData.page || 1;
    const size = pageData.size || 10;
    const totalPages = pageData.totalPages || 1;

    infoText.innerText = `총 ${total.toLocaleString()}명 중 ${(current - 1) * size + 1} - ${Math.min(current * size, total)}명 노출`;

    btnContainer.innerHTML = '';

    // 이전 페이지 버튼
    const prevBtn = document.createElement('button');
    prevBtn.className = 'page-btn';
    prevBtn.innerText = '‹';
    prevBtn.disabled = (current === 1);
    prevBtn.onclick = function() {
        if (currentPage > 1) {
            currentPage--;
            fetchData();
        }
    };
    btnContainer.appendChild(prevBtn);

    // 페이지 번호 리스트 출력 (앞뒤 3개 제한)
    let startPage = Math.max(1, current - 2);
    let endPage = Math.min(totalPages, startPage + 4);
    if (endPage - startPage < 4) {
        startPage = Math.max(1, endPage - 4);
    }

    for (let i = startPage; i <= endPage; i++) {
        const pageBtn = document.createElement('button');
        pageBtn.className = `page-btn ${i === current ? 'active' : ''}`;
        pageBtn.innerText = i;
        pageBtn.onclick = function() {
            currentPage = i;
            fetchData();
        };
        btnContainer.appendChild(pageBtn);
    }

    // 다음 페이지 버튼
    const nextBtn = document.createElement('button');
    nextBtn.className = 'page-btn';
    nextBtn.innerText = '›';
    nextBtn.disabled = (current === totalPages || totalPages === 0);
    nextBtn.onclick = function() {
        if (currentPage < totalPages) {
            currentPage++;
            fetchData();
        }
    };
    btnContainer.appendChild(nextBtn);
}

// 상세 모달 열기 및 데이터 조회
function openDetailModal(customerId) {
    const modal = document.getElementById('customerDetailModal');
    const body = document.getElementById('modalBodyContent');
    
    modal.classList.add('open');
    body.innerHTML = `<div style="text-align: center; padding: 50px; color: var(--muted-foreground);">상세 데이터를 가져오는 중입니다...</div>`;

    // 1. 고객 상세 카드 및 수신 이력 통합 호출
    Promise.all([
        // 탭 목록에서 해당 고객 정보를 찾아내기
        fetch(`/api/customers/manage?page=1&size=1000&name=&phone=`) // 간단히 상세 데이터는 고객 목록 API 필터로 대입 조회
            .then(res => res.json())
            .then(data => (data.list || []).find(c => c.customerId === customerId)),
        // 메시지 이력 API
        fetch(`/api/customers/${customerId}/history`).then(res => res.json())
    ])
    .then(([customer, history]) => {
        if (!customer) {
            body.innerHTML = `<div style="text-align: center; padding: 30px; color: var(--destructive);">해당 고객 정보를 조회할 수 없습니다.</div>`;
            return;
        }
        renderModalBody(customer, history);
    })
    .catch(err => {
        console.error("Detail API Load Fail:", err);
        body.innerHTML = `<div style="text-align: center; padding: 30px; color: var(--destructive);">상세 이력을 로드하는 데 실패했습니다.</div>`;
    });
}

// 상세 모달 본문 렌더링
function renderModalBody(customer, history) {
    const body = document.getElementById('modalBodyContent');

    // 동의 여부 배지 그리기
    const smsPill = customer.smsConsent ? '<span class="consent-pill agreed">✔ SMS 동의</span>' : '<span class="consent-pill rejected">✖ SMS 거부</span>';
    const kakaoPill = customer.kakaoConsent ? '<span class="consent-pill agreed">✔ 카카오 동의</span>' : '<span class="consent-pill rejected">✖ 카카오 거부</span>';
    const emailPill = customer.emailConsent ? '<span class="consent-pill agreed">✔ 이메일 동의</span>' : '<span class="consent-pill rejected">✖ 이메일 거부</span>';

    // 고객 유형 스타일 배지 셋팅
    let typeBadgeClass = 'badge-gray';
    if (customer.customerType === '일반') typeBadgeClass = 'badge-blue';
    else if (customer.customerType === '신규') typeBadgeClass = 'badge-green';
    else if (customer.customerType === '휴면') typeBadgeClass = 'badge-purple';

    // 태그 목록
    let tagsHtml = '';
    if (customer.tags && customer.tags.length > 0) {
        tagsHtml = customer.tags.map(t => `<span class="tag-chip" style="font-size:0.75rem; padding:4px 8px; border-radius:12px;">${escapeHtml(t)}</span>`).join('');
    } else {
        tagsHtml = `<span class="empty-text">등록된 타겟팅 태그가 없습니다.</span>`;
    }

    // 메시지 수신 이력 로우 조립
    let historyRowsHtml = '';
    if (history && history.length > 0) {
        history.forEach(row => {
            // 1. 성공/실패/진행중 한글화 및 클래스 분기
            const statusUpper = (row.status || '').toUpperCase();
            let statusText = '실패';
            let statusClass = 'badge-red';
            
            if (statusUpper === 'SUCCEEDED' || statusUpper === '성공') {
                statusText = '성공';
                statusClass = 'badge-green';
            } else if (statusUpper === 'SENDING' || statusUpper === '전송중') {
                statusText = '전송중';
                statusClass = 'badge-blue';
            } else if (statusUpper === 'PENDING' || statusUpper === '대기중') {
                statusText = '대기중';
                statusClass = 'badge-gray';
            }

            // 2. 채널별 배지 컬러 분기
            let channelClass = 'badge-blue'; // SMS, LMS, MMS 등 기본값
            const channelUpper = (row.channel || '').toUpperCase();
            if (channelUpper.includes('KAKAO') || channelUpper.includes('카카오') || channelUpper.includes('ALIM') || channelUpper.includes('FRIEND')) {
                channelClass = 'badge-kakao';
            } else if (channelUpper.includes('EMAIL') || channelUpper.includes('이메일')) {
                channelClass = 'badge-green';
            } else if (channelUpper.includes('RCS')) {
                channelClass = 'badge-orange';
            }

            // 3. 실패 시 UI 깨짐 방지를 위한 말풍선 툴팁 구조 적용
            const isFailed = (statusText === '실패');
            let statusBadgeHtml = `<span class="badge ${statusClass}">${statusText}</span>`;
            if (isFailed && row.failReason) {
                statusBadgeHtml = `
                    <div class="tooltip-wrapper">
                        <span class="badge ${statusClass}">${statusText}</span>
                        <span class="tooltip-content">사유: ${escapeHtml(row.failReason)}</span>
                    </div>
                `;
            }
            
            historyRowsHtml += `
                <tr>
                    <td class="font-mono" style="color:var(--muted-foreground);">${row.sentAt}</td>
                    <td style="font-weight:var(--font-weight-medium);">${escapeHtml(row.templateName)}</td>
                    <td><span class="badge ${channelClass}">${escapeHtml(row.channel)}</span></td>
                    <td>${statusBadgeHtml}</td>
                </tr>
            `;
        });
    } else {
        historyRowsHtml = `<tr><td colspan="4" style="text-align:center; padding:30px; color:var(--muted-foreground);">최근 90일 내의 발송/수신 기록이 없습니다.</td></tr>`;
    }

    body.innerHTML = `
        <!-- 상단 프로필 헤더 -->
        <div class="detail-profile">
            <div class="detail-avatar-info">
                <div class="avatar-circle">${escapeHtml(customer.name[0])}</div>
                <div class="detail-name-wrapper">
                    <div class="detail-name-row">
                        <span class="detail-name">${escapeHtml(customer.name)}</span>
                        <span class="badge ${typeBadgeClass}">${escapeHtml(customer.customerType)}</span>
                    </div>
                    <span class="detail-phone">${escapeHtml(maskPhoneNumber(customer.phone))}</span>
                </div>
            </div>
            <div class="detail-dates">
                <div>
                    <div class="date-item__label">가입일</div>
                    <div class="date-item__value">${customer.joinedAt || '-'}</div>
                </div>
                <div>
                    <div class="date-item__label">마지막 발송</div>
                    <div class="date-item__value">${customer.lastSend ? customer.lastSend.split(' ')[0] : '기록 없음'}</div>
                </div>
            </div>
        </div>

        <!-- 수신 동의 여부 -->
        <div class="consent-row">
            <span class="consent-label">수신 동의 현황</span>
            ${smsPill}
            ${kakaoPill}
            ${emailPill}
        </div>

        <!-- 태그 목록 섹션 -->
        <div class="detail-section">
            <div class="section-header">
                <span class="section-title">타겟팅 매핑 태그</span>
                <span class="section-badge">${customer.tags ? customer.tags.length : 0}개 매핑됨</span>
            </div>
            <div class="tag-list-box">
                <div style="display: flex; flex-wrap: wrap; gap: 6px;">
                    ${tagsHtml}
                </div>
            </div>
        </div>

        <!-- 메시지 이력 테이블 섹션 -->
        <div class="detail-section">
            <div class="section-header">
                <span class="section-title">최근 메시지 수신 내역</span>
                <span class="section-badge">${history ? history.length : 0}건</span>
            </div>
            <div class="history-table-container">
                <table class="history-table">
                    <thead>
                        <tr>
                            <th style="width: 140px;">발송일시</th>
                            <th>템플릿 제목</th>
                            <th style="width: 100px;">채널</th>
                            <th style="width: 100px;">결과</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${historyRowsHtml}
                    </tbody>
                </table>
            </div>
        </div>
    `;
}

// 모달 닫기
function closeDetailModal() {
    const modal = document.getElementById('customerDetailModal');
    modal.classList.remove('open');
}

// 백드롭 클릭 시 모달 닫기
function closeModalOnBackdrop(event) {
    if (event.target.id === 'customerDetailModal') {
        closeDetailModal();
    }
}

// HTML 이스케이프 유틸리티 (XSS 방지)
function escapeHtml(text) {
    if (!text) return '';
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, function(m) { return map[m]; });
}

// 전화번호 마스킹 유틸리티
function maskPhoneNumber(phone) {
    if (!phone) return '';
    const clean = phone.replace(/[^0-9]/g, '');
    if (clean.length === 11) {
        return `${clean.slice(0, 3)}-****-${clean.slice(7)}`;
    } else if (clean.length === 10) {
        return `${clean.slice(0, 3)}-***-${clean.slice(6)}`;
    }
    return phone.replace(/(\d{3})-?(\d{3,4})-?(\d{4})/, '$1-****-$3');
}
