package shop.dear.commerce.order.offer.presentation.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import shop.dear.commerce.order.offer.domain.constant.OfferStatus;
import shop.dear.common.exception.BusinessException;
import shop.dear.common.exception.CommonErrorCode;

@Component
public class StringToOfferStatusConverter implements Converter<String, OfferStatus> {

    @Override
    public OfferStatus convert(final String source) {
        if (source == null || source.isBlank()) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        try {
            return OfferStatus.valueOf(source.toUpperCase());
        } catch (final IllegalArgumentException e) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
    }
}
