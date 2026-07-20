package shop.deal.commerce.trade.order.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.deal.commerce.trade.order.domain.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
