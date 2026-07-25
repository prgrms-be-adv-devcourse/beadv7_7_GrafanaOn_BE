package shop.dear.identity.scrap.application;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.common.exception.BusinessException;
import shop.dear.identity.scrap.application.dto.ScrapInfo;
import shop.dear.identity.scrap.domain.exception.ScrapErrorCode;
import shop.dear.identity.scrap.domain.model.Scrap;
import shop.dear.identity.scrap.domain.repository.ScrapRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScrapService {

    private final ScrapRepository scrapRepository;

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

    public Page<ScrapInfo> getScrapList(
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

        return scrapRepository.findByMemberId(memberId, pageRequest)
            .map(ScrapInfo::from);
    }

    @Transactional
    public void deleteScrap(final Long memberId, final Long productId){

        Scrap scrap = scrapRepository.findByMemberIdAndProductId(memberId, productId)
            .orElseThrow(() -> new BusinessException(ScrapErrorCode.SCRAP_NOT_FOUND));

        scrapRepository.deleteById(scrap.getId());
    }
}
