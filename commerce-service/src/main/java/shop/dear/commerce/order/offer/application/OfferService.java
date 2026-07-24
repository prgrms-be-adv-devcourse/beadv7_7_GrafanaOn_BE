package shop.dear.commerce.order.offer.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.order.offer.domain.constant.OfferStatus;
import shop.dear.commerce.order.offer.domain.repository.OfferRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OfferService {

  private static final List<OfferStatus> ACTIVE_STATUSES = List.of(
      OfferStatus.PENDING,
      OfferStatus.ACCEPTED
  );

  private final OfferRepository offerRepository;

  @Transactional(readOnly = true)
  public boolean existsActiveOfferByProductId(final Long productId) {
    return offerRepository.existsByProductIdAndStatusIn(productId, ACTIVE_STATUSES);
  }
}
