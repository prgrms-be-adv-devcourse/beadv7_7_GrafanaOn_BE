package shop.dear.identity.scrap.application;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import shop.dear.common.exception.BusinessException;
import shop.dear.identity.scrap.application.dto.ScrapDetail;
import shop.dear.identity.scrap.application.dto.ScrapInfo;
import shop.dear.identity.scrap.application.dto.external.ProductSummary;
import shop.dear.identity.scrap.application.port.ProductPort;
import shop.dear.identity.scrap.domain.exception.ScrapErrorCode;
import shop.dear.identity.scrap.domain.model.Scrap;
import shop.dear.identity.scrap.domain.repository.ScrapRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScrapServiceTest {

    @Mock
    private ScrapRepository scrapRepository;

    @Mock
    private ProductPort productPort;

    @InjectMocks
    private ScrapService scrapService;

    @Test
    @DisplayName("스크랩 생성 성공")
    void addScrapTest() {

        given(scrapRepository.existsByMemberIdAndProductId(1L, 2L))
            .willReturn(false);
        given(scrapRepository.save(any()))
            .willAnswer(invocation -> invocation.getArgument(0));

        ScrapInfo result = scrapService.addScrap(1L, 2L);

        Assertions.assertEquals(1L, result.memberId());
        Assertions.assertEquals(2L, result.productId());
        verify(scrapRepository).save(any());
    }

    @Test
    @DisplayName("스크랩 생성 실패 (이미 스크랩한 상품)")
    void addScrap_duplicate() {

        given(scrapRepository.existsByMemberIdAndProductId(1L, 2L))
            .willReturn(true);

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> scrapService.addScrap(1L, 2L));

        Assertions.assertEquals(ScrapErrorCode.DUPLICATE_SCRAP, exception.getErrorCode());
        verify(scrapRepository, never()).save(any());
    }

    @Test
    @DisplayName("스크랩 생성 실패 (동시 요청으로 인한 유니크 제약조건 위반)")
    void addScrap_concurrentDuplicate() {

        given(scrapRepository.existsByMemberIdAndProductId(1L, 2L))
            .willReturn(false);
        willThrow(new DataIntegrityViolationException("duplicate key"))
            .given(scrapRepository).save(any());

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> scrapService.addScrap(1L, 2L));

        Assertions.assertEquals(ScrapErrorCode.DUPLICATE_SCRAP, exception.getErrorCode());
    }

    @Test
    @DisplayName("스크랩 목록 조회 성공")
    void getScrapListTest() {

        Scrap scrap1 = Scrap.create(1L, 100L);
        Scrap scrap2 = Scrap.create(1L, 200L);

        Page<Scrap> scrapPage = new PageImpl<>(
            List.of(scrap2, scrap1),
            PageRequest.of(0, 10),
            2
        );

        given(scrapRepository.findByMemberId(eq(1L), any()))
            .willReturn(scrapPage);
        given(productPort.getProducts(any()))
            .willReturn(List.of(
                new ProductSummary(200L, "상품200", "브랜드", BigDecimal.valueOf(10000), "thumb200.png", "ON_SALE"),
                new ProductSummary(100L, "상품100", "브랜드", BigDecimal.valueOf(20000), "thumb100.png", "ON_SALE")
            ));

        Page<ScrapDetail> result = scrapService.getScrapList(1L, 0, 10);

        Assertions.assertEquals(2, result.getTotalElements());
        Assertions.assertEquals(1, result.getTotalPages());
        Assertions.assertEquals(200L, result.getContent().get(0).id());
        Assertions.assertEquals("상품200", result.getContent().get(0).name());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(scrapRepository).findByMemberId(eq(1L), pageableCaptor.capture());

        Pageable captured = pageableCaptor.getValue();
        Assertions.assertEquals(0, captured.getPageNumber());
        Assertions.assertEquals(10, captured.getPageSize());
        Assertions.assertEquals(
            Sort.Direction.DESC,
            captured.getSort().getOrderFor("insertedAt").getDirection()
        );
    }

    @Test
    @DisplayName("스크랩 목록 조회 시 삭제된 상품은 결과에서 제외된다")
    void getScrapListTest_productNotFound() {

        Scrap scrap = Scrap.create(1L, 100L);

        Page<Scrap> scrapPage = new PageImpl<>(
            List.of(scrap),
            PageRequest.of(0, 10),
            1
        );

        given(scrapRepository.findByMemberId(eq(1L), any()))
            .willReturn(scrapPage);
        given(productPort.getProducts(any()))
            .willReturn(List.of());

        Page<ScrapDetail> result = scrapService.getScrapList(1L, 0, 10);

        Assertions.assertTrue(result.getContent().isEmpty());
    }

    @Test
    @DisplayName("스크랩 삭제 성공")
    void deleteScrapTest() {

        Scrap scrap = Scrap.create(1L, 100L);

        given(scrapRepository.findByMemberIdAndProductId(1L, 100L))
            .willReturn(Optional.of(scrap));

        scrapService.deleteScrap(1L, 100L);

        verify(scrapRepository).deleteById(any());
    }

    @Test
    @DisplayName("스크랩 삭제 실패 (존재하지 않는 스크랩)")
    void deleteScrap_notFound() {

        given(scrapRepository.findByMemberIdAndProductId(1L, 100L))
            .willReturn(Optional.empty());

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> scrapService.deleteScrap(1L, 100L));

        Assertions.assertEquals(ScrapErrorCode.SCRAP_NOT_FOUND, exception.getErrorCode());
    }
}
