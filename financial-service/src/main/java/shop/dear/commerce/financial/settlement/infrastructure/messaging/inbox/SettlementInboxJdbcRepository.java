package shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SettlementInboxJdbcRepository {

    private static final String INSERT_SQL = """
            insert into settlement_inbox (
                inserted_at, updated_at, event_id, event_type, aggregate_type,
                aggregate_id, stream_name, payload, status, retry_count,
                last_error
            ) values (
                ?, ?, ?, ?, ?,
                ?, ?, cast(? as jsonb), ?, ?,
                ?
            )
            on conflict (event_id) do nothing
            returning id
            """;

    private final JdbcTemplate jdbcTemplate;

    public Optional<Long> insertIgnoringDuplicate(final SettlementInbox inbox) {
        final Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        final Long id = jdbcTemplate.query(
                INSERT_SQL,
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
                // on conflict do nothing 이면 returning 도 아무 행을 내지 않는다.
                rs -> rs.next() ? rs.getLong("id") : null
        );

        return Optional.ofNullable(id);
    }
}
