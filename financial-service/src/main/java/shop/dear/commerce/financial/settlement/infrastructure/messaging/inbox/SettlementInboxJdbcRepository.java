package shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class SettlementInboxJdbcRepository {

    private static final String UPSERT_SQL = """
        insert into settlement_inbox (
            inserted_at, updated_at, event_id, event_type, aggregate_type,
            aggregate_id, stream_name, payload, status, retry_count,
            last_error
        ) values (
            ?, ?, ?, ?, ?,
            ?, ?, cast(? as jsonb), ?, ?,
            ?
        )
        on conflict (event_id) do update set event_id = excluded.event_id
        returning id, status
    """;

    private final JdbcTemplate jdbcTemplate;

    public InboxSaveResult insertOrGetExisting(final SettlementInbox inbox) {
        final Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        final InboxSaveResult result = jdbcTemplate.query(
                UPSERT_SQL,
                ps -> {
                    ps.setTimestamp(1, now);
                    ps.setTimestamp(2, now);
                    ps.setString(3, inbox.getEventId());
                    ps.setString(4, inbox.getEventType());
                    ps.setString(5, inbox.getAggregateType());

                    // 파싱 실패 시 null 이므로 타입을 명시해 driver 가 추론에 실패하지 않게 한다.
                    if (inbox.getAggregateId() == null) {
                        ps.setNull(6, Types.BIGINT);
                    } else {
                        ps.setLong(6, inbox.getAggregateId());
                    }

                    ps.setString(7, inbox.getStreamName());
                    ps.setString(8, inbox.getPayload());
                    ps.setString(9, inbox.getStatus().name());
                    ps.setInt(10, inbox.getRetryCount());
                    ps.setString(11, inbox.getLastError());
                },
                rs -> rs.next()
                        ? new InboxSaveResult(
                                rs.getLong("id"),
                                InboxMessageStatus.valueOf(rs.getString("status"))
                        )
                        : null
        );

        if (result == null) {
            throw new IllegalStateException(
                    "settlement_inbox 적재 결과를 확인하지 못했습니다. eventId=" + inbox.getEventId()
            );
        }

        return result;
    }
}
