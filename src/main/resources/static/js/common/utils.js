/**
 * 공통 자바스크립트 헬퍼 함수 모음
 */
const CommonUtils = {
    /**
     * 날짜 포맷팅 (YYYY-MM-DD HH:mm:ss)
     */
    formatDate: function(dateStr) {
        if (!dateStr) return "";
        const date = new Date(dateStr);
        const yyyy = date.getFullYear();
        const mm = String(date.getMonth() + 1).padStart(2, '0');
        const dd = String(date.getDate()).padStart(2, '0');
        const hh = String(date.getHours()).padStart(2, '0');
        const min = String(date.getMinutes()).padStart(2, '0');
        const ss = String(date.getSeconds()).padStart(2, '0');
        return `${yyyy}-${mm}-${dd} ${hh}:${min}:${ss}`;
    },
    
    /**
     * 숫자에 천단위 콤마 찍기
     */
    formatNumber: function(num) {
        if (num === null || num === undefined) return "0";
        return num.toString().replace(/\B(?=(\d3)+(?!\d))/g, ",");
    }
};
