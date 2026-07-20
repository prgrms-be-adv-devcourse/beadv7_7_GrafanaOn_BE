package shop.deal.commerce.order.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.deal.commerce.order.domain.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
