package kg.bakai.console.adapter;

import kg.bakai.product.port.ProductUsageChecker;
import kg.bakai.stopfactor.repository.StopFactorCheckRepository;
import kg.bakai.stopfactor.repository.StopFactorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tells core-online whether a product is used by stop factors or their checks, so it must not be deleted.
 * Lives in core-console because it is the only module that sees both: core-online must not import stopfactor,
 * and stopfactor must not import core-online.
 */
@Component
@RequiredArgsConstructor
class StopFactorProductUsageChecker implements ProductUsageChecker {

    private final StopFactorRepository stopFactorRepository;
    private final StopFactorCheckRepository stopFactorCheckRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isProductUsed(long productId) {
        return stopFactorRepository.isProductUsed(productId) || stopFactorCheckRepository.existsByProductId(productId);
    }
}
