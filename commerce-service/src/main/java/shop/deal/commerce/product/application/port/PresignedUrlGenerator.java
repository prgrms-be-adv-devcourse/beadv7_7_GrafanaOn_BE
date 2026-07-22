package shop.deal.commerce.product.application.port;

public interface PresignedUrlGenerator {
    String generate(final int sortOrder, final String uploadFileType);
}
