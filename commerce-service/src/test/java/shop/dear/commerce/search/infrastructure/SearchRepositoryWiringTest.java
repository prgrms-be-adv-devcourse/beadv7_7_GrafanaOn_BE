package shop.dear.commerce.search.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import shop.dear.commerce.search.domain.SearchRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SearchRepository 주입 지점에 이중 쓰기 어댑터가 들어오는지 확인한다.
 * 구현체가 셋이라 @Primary 가 빠지면 주입이 어긋나거나 기동이 실패한다.
 */
@SpringBootTest
class SearchRepositoryWiringTest {

    @Autowired
    private SearchRepository searchRepository;

    @Test
    void injectsDualWriteAdapter() {
        assertThat(searchRepository).isInstanceOf(DualWriteSearchAdapter.class);
    }
}