package shop.dear.recommendation.infrastructure.inbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class RecommendationInboxJdbcRepository {

    private static final String INSERT_SQL = """
        insert into %s.recommendation_inbox (
            inserted_at, updated_at, event_id, aggregate_type, aggregate_id,
            event_type, payload, status, retry_count, occurred_at
        ) values (
            ?, ?, ?, ?, ?,
            ?, cast(? as jsonb), ?, ?, ?
        )
        on conflict (event_id) do nothing
        """;

    private final JdbcTemplate jdbcTemplate;
    private final String insertSql;

    public RecommendationInboxJdbcRepository(
        final JdbcTemplate jdbcTemplate,
        @Value("${spring.jpa.properties.hibernate.default_schema:recommendation}") final String schema
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.insertSql = INSERT_SQL.formatted(schema);
    }

    public void insertIgnoringDuplicate(final LocalDateTime now, final List<RecommendationInbox> inboxes) {
        if (inboxes.isEmpty()) {
            return;
        }

        final Timestamp nowTimestamp = Timestamp.valueOf(now);

        jdbcTemplate.batchUpdate(insertSql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(final PreparedStatement ps, final int i) throws SQLException {

                final RecommendationInbox inbox = inboxes.get(i);

                ps.setTimestamp(1, nowTimestamp);
                ps.setTimestamp(2, nowTimestamp);
                ps.setLong(3, inbox.getEventId());
                ps.setString(4, inbox.getAggregateType());
                ps.setString(5, inbox.getAggregateId());
                ps.setString(6, inbox.getEventType().name());
                ps.setString(7, inbox.getPayload());
                ps.setString(8, inbox.getStatus().name());
                ps.setInt(9, inbox.getRetryCount());
                ps.setTimestamp(10, Timestamp.valueOf(inbox.getOccurredAt()));
            }

            @Override
            public int getBatchSize() {
                return inboxes.size();
            }
        });
    }
}
