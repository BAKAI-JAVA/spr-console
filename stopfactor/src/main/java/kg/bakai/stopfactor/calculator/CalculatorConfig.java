package kg.bakai.stopfactor.calculator;

import kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents;
import kg.bakai.stopfactor.dto.scoring.LegacyCounters;
import kg.bakai.stopfactor.dto.scoring.ReportValues;
import kg.bakai.stopfactor.enums.CalculatorType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The calculators that differ only by the value they read; the others are @Component classes. */
@Configuration
public class CalculatorConfig {

    @Bean
    public StopFactorCalculator<NoParams> kibActiveDisputesCalculator() {
        return new SimpleValueCalculator(CalculatorType.KIB_ACTIVE_DISPUTES, ReportValues::activeDisputes);
    }

    @Bean
    public StopFactorCalculator<NoParams> kibUnjustifiedDisputesCalculator() {
        return new SimpleValueCalculator(CalculatorType.KIB_UNJUSTIFIED_DISPUTES, ReportValues::unjustifiedDisputes);
    }

    @Bean
    public StopFactorCalculator<NoParams> currentOverdueDaysCalculator() {
        return new SimpleValueCalculator(CalculatorType.CURRENT_OVERDUE_DAYS, ReportValues::currentOverdueDays);
    }

    @Bean
    public StopFactorCalculator<NoParams> currentOverdueAmountCalculator() {
        return new SimpleValueCalculator(CalculatorType.CURRENT_OVERDUE_AMOUNT, ReportValues::currentOverdueAmount);
    }

    @Bean
    public StopFactorCalculator<NoParams> openCreditsCountCalculator() {
        return new SimpleValueCalculator(CalculatorType.OPEN_CREDITS_COUNT, ReportValues::openCredits);
    }

    @Bean
    public StopFactorCalculator<RecentEventsCountCalculator.Period> bureauRequestsCountCalculator() {
        return new RecentEventsCountCalculator(CalculatorType.BUREAU_REQUESTS_COUNT,
                CreditHistoryEvents::bureauRequestsDaysAgo, LegacyCounters::bureauRequests);
    }

    @Bean
    public StopFactorCalculator<RecentEventsCountCalculator.Period> newCreditsCountCalculator() {
        return new RecentEventsCountCalculator(CalculatorType.NEW_CREDITS_COUNT,
                CreditHistoryEvents::newCreditsDaysAgo, LegacyCounters::newCredits);
    }
}
