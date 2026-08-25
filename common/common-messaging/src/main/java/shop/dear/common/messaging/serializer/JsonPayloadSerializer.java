package shop.dear.common.messaging.serializer;

public interface JsonPayloadSerializer {

    // 서비스의 이벤트를 받아 JSON 문자열로 변환
    String serialize(Object payload);
}
