package kg.bakai.stopfactor.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static kg.bakai.common.enums.ProductSegment.MSB;
import static kg.bakai.common.enums.ProductSegment.RETAIL;
import static org.assertj.core.api.Assertions.assertThat;

class StopFactorTest {

    @Test
    void stopFactorWithoutSegmentAndProductsAppliesToEveryApplication() {
        StopFactor stopFactor = new StopFactor("any", null);

        assertThat(stopFactor.appliesTo(7L, RETAIL)).isTrue();
        assertThat(stopFactor.appliesTo(7L, MSB)).isTrue();
        assertThat(stopFactor.appliesTo(null, RETAIL)).isTrue();
    }

    @Test
    void segmentStopFactorAppliesOnlyToItsSegment() {
        StopFactor msb = new StopFactor("msb", null);
        msb.setSegment(MSB);

        assertThat(msb.appliesTo(7L, MSB)).isTrue();
        assertThat(msb.appliesTo(7L, RETAIL)).isFalse();
        assertThat(msb.appliesTo(null, RETAIL)).isFalse();
    }

    @Test
    void productLimitsApplyTogetherWithTheSegment() {
        StopFactor stopFactor = new StopFactor("limited", null);
        stopFactor.setSegment(RETAIL);
        stopFactor.getProductIds().add(5L);

        assertThat(stopFactor.appliesTo(5L, RETAIL)).isTrue();
        assertThat(stopFactor.appliesTo(6L, RETAIL)).isFalse();
        assertThat(stopFactor.appliesTo(5L, MSB)).isFalse();
        assertThat(stopFactor.appliesTo(null, RETAIL)).isTrue();   // scoring without product ignores product limits
    }

    @Test
    void newVersionKeepsTheSegment() {
        StopFactor first = new StopFactor("first", null);
        first.setId(40L);
        first.setSegment(MSB);
        first.setParams(Map.of("days", 30));
        first.getProductIds().add(5L);

        StopFactor next = first.newVersion(LocalDateTime.now(), "reason");

        assertThat(next.getSegment()).isEqualTo(MSB);
        assertThat(next.getRootId()).isEqualTo(40L);
        assertThat(next.getProductIds()).containsExactly(5L);
    }
}
