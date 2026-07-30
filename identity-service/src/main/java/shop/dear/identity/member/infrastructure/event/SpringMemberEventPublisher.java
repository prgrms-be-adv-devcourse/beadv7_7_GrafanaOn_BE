package shop.dear.identity.member.infrastructure.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import shop.dear.common.event.member.MemberCreatedEvent;
import shop.dear.identity.auth.authentication.application.port.MemberEventPublisher;

@Slf4j
@RequiredArgsConstructor
@Component
public class SpringMemberEventPublisher implements MemberEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(MemberCreatedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
