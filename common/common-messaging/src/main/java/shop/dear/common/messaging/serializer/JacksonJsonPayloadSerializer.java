package shop.dear.common.messaging.serializer;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import java.util.Objects;

@Component
public class JacksonJsonPayloadSerializer implements JsonPayloadSerializer {

    private final ObjectMapper objectMapper;

    public JacksonJsonPayloadSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String serialize(Object payload) {
        // payload가 null이면 JSON 문자열로 직렬화 없이 바로 NullPointerException 발생
        Objects.requireNonNull(payload, "메시지 페이로드는 null일 수 없습니다.");

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException exception) {
            throw new MessageSerializationException(
                    "메시지 페이로드를 JSON으로 직렬화하지 못했습니다.",
                    exception);
        }
    }
}
