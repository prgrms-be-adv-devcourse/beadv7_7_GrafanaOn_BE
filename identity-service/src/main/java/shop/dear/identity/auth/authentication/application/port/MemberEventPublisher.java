package shop.dear.identity.auth.authentication.application.port;

import shop.dear.common.event.member.MemberCreatedEvent;

public interface MemberEventPublisher {
    void publish(final MemberCreatedEvent event);
}
