package kg.bakai.stopfactor.calculator;

import kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents;
import kg.bakai.stopfactor.dto.scoring.ReportValues;
import kg.bakai.stopfactor.enums.CalculatorType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CalculatorConfig {

    @Bean
    StopFactorCalculator<NoParams> kibActiveDisputesCalculator() {
        return new SimpleValueCalculator(CalculatorType.KIB_ACTIVE_DISPUTES, ReportValues::numberOfActiveDisputes);
    }

    @Bean
    StopFactorCalculator<NoParams> kibUnjustifiedDisputesCalculator() {
        return new SimpleValueCalculator(CalculatorType.KIB_UNJUSTIFIED_DISPUTES, ReportValues::numberOfUnjustifiedDisputesInPast);
    }

    @Bean
    StopFactorCalculator<NoParams> currentOverdueDaysCalculator() {
        return new SimpleValueCalculator(CalculatorType.CURRENT_OVERDUE_DAYS, ReportValues::currentNumberOfDaysOverdue);
    }

    @Bean
    StopFactorCalculator<NoParams> currentOverdueAmountCalculator() {
        return new SimpleValueCalculator(CalculatorType.CURRENT_OVERDUE_AMOUNT, ReportValues::currentAmountOfOverdue);
    }

    @Bean
    StopFactorCalculator<NoParams> openCreditsCountCalculator() {
        return new SimpleValueCalculator(CalculatorType.OPEN_CREDITS_COUNT, ReportValues::totalOpenCreditContracts);
    }

    @Bean
    StopFactorCalculator<RecentEventsCountCalculator.Period> bureauRequestsCountCalculator() {
        return new RecentEventsCountCalculator(CalculatorType.BUREAU_REQUESTS_COUNT,
                CreditHistoryEvents::bureauRequestsDaysAgo, 7, ReportValues::creditInquiriesLast7Days);
    }

    @Bean
    StopFactorCalculator<RecentEventsCountCalculator.Period> newCreditsCountCalculator() {
        return new RecentEventsCountCalculator(CalculatorType.NEW_CREDITS_COUNT,
                CreditHistoryEvents::newCreditsDaysAgo, 30, ReportValues::totalNumberOfNewOpenLoansInTheLast30Days);
    }
}
