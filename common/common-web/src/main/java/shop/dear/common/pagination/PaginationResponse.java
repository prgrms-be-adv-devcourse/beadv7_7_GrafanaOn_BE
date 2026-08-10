package shop.dear.common.pagination;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginationResponse<T> {

    private final List<T> content;
    private final PageInfo pagination;

    @Builder(access = AccessLevel.PRIVATE)
    private PaginationResponse(List<T> content, PageInfo pagination) {
        this.content = content;
        this.pagination = pagination;
    }

    public static <T, R> PaginationResponse<R> of(
            Page<T> page,
            List<R> content
    ) {
        return PaginationResponse.<R>builder()
                .content(content)
                .pagination(PageInfo.of(page))
                .build();
    }

    public static <T> PaginationResponse<T> of(
            List<T> content,
            int pageNo,
            int pageSize,
            long totalItems,
            int totalPages
    ) {
        return PaginationResponse.<T>builder()
                .content(content)
                .pagination(PageInfo.of(pageNo, pageSize, totalItems, totalPages))
                .build();
    }

    public record PageInfo(
        int currentPage,
        int totalPages,
        long totalItems,
        int pageSize,
        boolean first,
        boolean last,
        boolean hasNext,
        boolean hasPrevious
    ) {

        @Builder(access = AccessLevel.PRIVATE)
        private static PageInfo create(
                int currentPage,
                int totalPages,
                long totalItems,
                int pageSize,
                boolean first,
                boolean last,
                boolean hasNext,
                boolean hasPrevious
        ) {
            return new PageInfo(
                    currentPage,
                    totalPages,
                    totalItems,
                    pageSize,
                    first,
                    last,
                    hasNext,
                    hasPrevious
            );
        }

        public static PageInfo of(Page<?> page) {
            return PageInfo.builder()
                    .currentPage(toDisplayPageNumber(page.getNumber()))
                    .totalPages(page.getTotalPages())
                    .totalItems(page.getTotalElements())
                    .pageSize(page.getSize())
                    .first(page.isFirst())
                    .last(page.isLast())
                    .hasNext(page.hasNext())
                    .hasPrevious(page.hasPrevious())
                    .build();
        }

        public static PageInfo of(
                int pageNo,
                int pageSize,
                long totalItems,
                int totalPages
        ) {
            return PageInfo.builder()
                    .currentPage(pageNo)
                    .totalPages(totalPages)
                    .totalItems(totalItems)
                    .pageSize(pageSize)
                    .first(pageNo == 1)
                    .last(pageNo >= totalPages)
                    .hasNext(pageNo < totalPages)
                    .hasPrevious(pageNo > 1)
                    .build();
        }

        private static int toDisplayPageNumber(int zeroBasedPageNumber) {
            return zeroBasedPageNumber + 1;
        }
    }


}
