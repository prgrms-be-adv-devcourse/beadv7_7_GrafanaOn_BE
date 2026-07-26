package shop.dear.identity.scrap.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import shop.dear.common.exception.BusinessException;
import shop.dear.identity.scrap.application.ScrapService;
import shop.dear.identity.scrap.application.dto.ScrapDetail;
import shop.dear.identity.scrap.application.dto.ScrapInfo;
import shop.dear.identity.scrap.domain.exception.ScrapErrorCode;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScrapController.class)
class ScrapControllerTest {

    private static final String MEMBER_ID_HEADER = "X-Authenticated-Member-Id";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScrapService scrapService;

    @Test
    @DisplayName("스크랩 생성에 성공하면 상태코드 200을 반환한다")
    void addScrap_success() throws Exception {

        given(scrapService.addScrap(1L, 100L))
            .willReturn(new ScrapInfo(1L, 100L));

        final ResultActions result = mockMvc
            .perform(post("/api/scraps/100")
                .header(MEMBER_ID_HEADER, "1"));

        result
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("success"))
            .andExpect(jsonPath("$.data.productId").value(100));
    }

    @Test
    @DisplayName("스크랩 목록 조회에 성공하면 페이지 메타데이터와 함께 목록을 반환한다")
    void getScrapList_success() throws Exception {

        Page<ScrapDetail> scrapPage = new PageImpl<>(
            List.of(
                new ScrapDetail(200L, "상품200", "브랜드", BigDecimal.valueOf(10000), "thumb200.png", "ON_SALE"),
                new ScrapDetail(100L, "상품100", "브랜드", BigDecimal.valueOf(20000), "thumb100.png", "ON_SALE")
            ),
            PageRequest.of(0, 10),
            2
        );

        given(scrapService.getScrapList(1L, 0, 10))
            .willReturn(scrapPage);

        final ResultActions result = mockMvc
            .perform(get("/api/scraps")
                .header(MEMBER_ID_HEADER, "1")
                .param("page", "0")
                .param("size", "10"));

        result
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.scrapList.length()").value(2))
            .andExpect(jsonPath("$.data.scrapList[0].id").value(200))
            .andExpect(jsonPath("$.data.scrapList[0].name").value("상품200"))
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(10))
            .andExpect(jsonPath("$.data.totalElements").value(2))
            .andExpect(jsonPath("$.data.totalPages").value(1))
            .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("이미 스크랩한 상품을 다시 스크랩하면 상태코드 400과 SC-002 에러코드를 반환한다")
    void addScrap_duplicate() throws Exception {

        willThrow(new BusinessException(ScrapErrorCode.DUPLICATE_SCRAP))
            .given(scrapService).addScrap(eq(1L), eq(100L));

        final ResultActions result = mockMvc
            .perform(post("/api/scraps/100")
                .header(MEMBER_ID_HEADER, "1"));

        result
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ScrapErrorCode.DUPLICATE_SCRAP.getValue()))
            .andExpect(jsonPath("$.message").value(ScrapErrorCode.DUPLICATE_SCRAP.getMessage()));
    }

    @Test
    @DisplayName("스크랩 삭제에 성공하면 상태코드 200을 반환한다")
    void deleteScrap_success() throws Exception {

        final ResultActions result = mockMvc
            .perform(delete("/api/scraps/100")
                .header(MEMBER_ID_HEADER, "1"));

        result
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("success"));
    }

    @Test
    @DisplayName("존재하지 않는 스크랩을 삭제하면 상태코드 400과 SC-001 에러코드를 반환한다")
    void deleteScrap_notFound() throws Exception {

        willThrow(new BusinessException(ScrapErrorCode.SCRAP_NOT_FOUND))
            .given(scrapService).deleteScrap(eq(1L), eq(100L));

        final ResultActions result = mockMvc
            .perform(delete("/api/scraps/100")
                .header(MEMBER_ID_HEADER, "1"));

        result
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ScrapErrorCode.SCRAP_NOT_FOUND.getValue()))
            .andExpect(jsonPath("$.message").value(ScrapErrorCode.SCRAP_NOT_FOUND.getMessage()));
    }
}
