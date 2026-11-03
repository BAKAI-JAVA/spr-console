package kg.bakai.product.port;

/**
 * Whether other modules still refer to a product, so it must not be deleted.
 * <p>
 * Declared here, implemented by modules that depend on core-online (stopfactor).
 * core-online must not depend on stopfactor (stopfactor depends on core-online), so it asks through this interface.
 */
public interface ProductUsageChecker {

    /**
     * @return true if the product is referenced and must not be deleted
     */
    boolean isProductUsed(long productId);
}
