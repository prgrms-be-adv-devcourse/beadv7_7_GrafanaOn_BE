package shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox;

//inbox 적재 결과
public record InboxSaveResult(Long id, InboxMessageStatus status) {

    public boolean isPending() {
        return this.status == InboxMessageStatus.PENDING;
    }
}
