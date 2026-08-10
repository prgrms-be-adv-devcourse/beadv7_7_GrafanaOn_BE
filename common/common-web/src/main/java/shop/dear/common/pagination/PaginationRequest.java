package shop.dear.common.pagination;

import lombok.Getter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Getter
public class PaginationRequest {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int DEFAULT_MAX_PAGE_SIZE = 20;
    private static final int MIN_PAGE_NO = 1;
    private static final int MIN_PAGE_SIZE = 1;

    private final int pageNo;
    private final int pageSize;

    public PaginationRequest(Integer pageNo, Integer pageSize) {
        this(pageNo, pageSize, DEFAULT_PAGE_SIZE, DEFAULT_MAX_PAGE_SIZE);
    }

    public PaginationRequest(Integer pageNo, Integer pageSize, int defaultPageSize, int maxPageSize) {
       this.pageNo = normalizePageNo(pageNo);
       this.pageSize = normalizePageSize(pageSize, defaultPageSize, maxPageSize);
    }

    public Pageable toPageable() {
        return PageRequest.of(pageNo - 1, pageSize);
    }

    private int normalizePageNo(Integer pageNo) {
      if (pageNo == null || pageNo < MIN_PAGE_NO) {
          return DEFAULT_PAGE_NO;
      }
      return pageNo;
    }

    private int normalizePageSize(Integer pageSize, int defaultPageSize, int maxPageSize) {
        if (pageSize == null || pageSize < MIN_PAGE_SIZE) {
            return defaultPageSize;
        }
        return Math.min(pageSize, maxPageSize);
    }

}
