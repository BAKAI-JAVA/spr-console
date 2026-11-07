package kg.bakai.stopfactor.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NameTemplateRendererTest {

    private static final String TEMPLATE = "Просрочка с {periodFromMonth} по {periodToMonth} месяц, где максимальная просрочка >= {minOverdueDays}[ и <= {maxOverdueDays}]";

    @Test
    void optionalSegmentIsKeptWhenItsParamsAreSet() {
        assertThat(NameTemplateRenderer.render(TEMPLATE,
                Map.of("periodFromMonth", 1, "periodToMonth", 3, "minOverdueDays", 1, "maxOverdueDays", 10)))
                .isEqualTo("Просрочка с 1 по 3 месяц, где максимальная просрочка >= 1 и <= 10");
    }

    @Test
    void optionalSegmentIsDroppedWhenAParamIsMissing() {
        assertThat(NameTemplateRenderer.render(TEMPLATE, Map.of("periodFromMonth", 1, "periodToMonth", 3, "minOverdueDays", 16)))
                .isEqualTo("Просрочка с 1 по 3 месяц, где максимальная просрочка >= 16");
    }

    @Test
    void placeholdersAreListedForValidation() {
        assertThat(NameTemplateRenderer.placeholders(TEMPLATE))
                .containsExactly("periodFromMonth", "periodToMonth", "minOverdueDays", "maxOverdueDays");
    }
}
