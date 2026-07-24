package shop.dear.identity.scrap.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.common.exception.BusinessException;
import shop.dear.identity.scrap.application.dto.ScrapInfo;
import shop.dear.identity.scrap.domain.exception.ScrapErrorCode;
import shop.dear.identity.scrap.domain.model.Scrap;
import shop.dear.identity.scrap.domain.repository.ScrapRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScrapService {

    private final ScrapRepository scrapRepository;

    @Transactional
    public ScrapInfo addScrap(final Long memberId, final Long productId){

        Scrap scrap = Scrap.create(memberId, productId);

        return ScrapInfo.from(scrapRepository.save(scrap));
    }

    public List<ScrapInfo> getScrapList(final Long memberId){

        return scrapRepository.findByMemberIdOrderByInsertedAt(memberId)
            .stream()
            .map(ScrapInfo::from)
            .toList();
    }

    @Transactional
    public void deleteScrap(final Long memberId, final Long productId){

        Scrap scrap = scrapRepository.findByMemberIdAndProductId(memberId, productId)
            .orElseThrow(() -> new BusinessException(ScrapErrorCode.SCRAP_NOT_FOUND));

        scrapRepository.deleteById(scrap.getId());
    }
}
