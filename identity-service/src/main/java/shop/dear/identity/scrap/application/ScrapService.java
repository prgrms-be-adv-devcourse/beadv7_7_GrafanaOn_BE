package shop.dear.identity.scrap.application;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.common.exception.BusinessException;
import shop.dear.identity.scrap.application.dto.ScrapDetail;
import shop.dear.identity.scrap.application.dto.ScrapInfo;
import shop.dear.identity.scrap.application.dto.external.ProductSummary;
import shop.dear.identity.scrap.application.port.ProductPort;
import shop.dear.identity.scrap.domain.exception.ScrapErrorCode;
import shop.dear.identity.scrap.domain.model.Scrap;
import shop.dear.identity.scrap.domain.repository.ScrapRepository;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final ProductPort productPort;

    @Transactional
    public ScrapInfo addScrap(final Long memberId, final Long productId){

        if (scrapRepository.existsByMemberIdAndProductId(memberId, productId)) {
            throw new BusinessException(ScrapErrorCode.DUPLICATE_SCRAP);
        }

        Scrap scrap = Scrap.create(memberId, productId);

        try {
            return ScrapInfo.from(scrapRepository.save(scrap));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ScrapErrorCode.DUPLICATE_SCRAP);
        }
    }

    public Page<ScrapDetail> getScrapList(
        final Long memberId,
        final int page,
        final int size
    ){

        PageRequest pageRequest = PageRequest.of(
            page,
            size,
            Sort.by("insertedAt")
                .descending()
        );

        Page<Scrap> scraps = scrapRepository.findByMemberId(memberId, pageRequest);

        List<Long> productIds = scraps.getContent().stream()
            .map(Scrap::getProductId)
            .distinct()
            .toList();

        Map<Long, ProductSummary> productsById = productPort.getProducts(productIds)
            .stream()
            .collect(Collectors.toMap(ProductSummary::id, Function.identity()));

        return scraps.map(scrap -> ScrapDetail.of(scrap, productsById.get(scrap.getProductId())));
    }

    @Transactional
    public void deleteScrap(final Long memberId, final Long productId){

        Scrap scrap = scrapRepository.findByMemberIdAndProductId(memberId, productId)
            .orElseThrow(() -> new BusinessException(ScrapErrorCode.SCRAP_NOT_FOUND));

        scrapRepository.deleteById(scrap.getId());
    }
}
