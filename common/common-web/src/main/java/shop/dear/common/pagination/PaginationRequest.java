package shop.dear.common.pagination;

import lombok.Getter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Getter
public class PaginationRequest {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 20;
    private static final int MIN_PAGE_NO = 1;
    private static final int MIN_PAGE_SIZE = 1;

    private final int pageNo;
    private final int pageSize;

    public PaginationRequest(Integer pageNo, Integer pageSize) {
       this.pageNo = normalizePageNo(pageNo);
       this.pageSize = normalizePageSize(pageSize);
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

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < MIN_PAGE_SIZE) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

}
