-- 메시지 발송 성능 개선용 인덱스
-- 운영 DB에 이미 같은 목적의 인덱스가 있는지 확인한 뒤 적용하세요.

-- 발송 완료 여부 확인 및 성공/실패 건수 집계용
CREATE INDEX IDX_SEND_TARGET_HISTORY_STATUS
    ON send_target (send_history_id, status, is_deleted);

-- 캠페인 전개 후 user_uuid 기준으로 생성된 send_target 재조회용
CREATE INDEX IDX_SEND_TARGET_USER_UUID
    ON send_target (user_uuid, is_deleted);

-- 실제 성공 채널 기준 비용/절감액 집계용
CREATE INDEX IDX_SEND_ATTEMPT_TARGET_SUCCESS
    ON send_attempt (send_target_id, is_succeeded, is_deleted, channel_id);

-- 완료 대상 send_history 탐색용
CREATE INDEX IDX_SEND_HISTORY_STATUS_DELETED
    ON send_history (status, is_deleted, id);