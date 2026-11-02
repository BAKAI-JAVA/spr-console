package kg.bakai.stopfactor.calculator;

import kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents;
import kg.bakai.stopfactor.dto.scoring.StopFactorIntegerValueDto;
import kg.bakai.stopfactor.enums.CalculatorType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CalculatorConfig {

    @Bean
    StopFactorCalculator kibActiveDisputesCalculator() {
        return new SimpleValueCalculator(CalculatorType.KIB_ACTIVE_DISPUTES, StopFactorIntegerValueDto::numberOfActiveDisputes);
    }

    @Bean
    StopFactorCalculator kibUnjustifiedDisputesCalculator() {
        return new SimpleValueCalculator(CalculatorType.KIB_UNJUSTIFIED_DISPUTES, StopFactorIntegerValueDto::numberOfUnjustifiedDisputesInPast);
    }

    @Bean
    StopFactorCalculator currentOverdueDaysCalculator() {
        return new SimpleValueCalculator(CalculatorType.CURRENT_OVERDUE_DAYS, StopFactorIntegerValueDto::currentNumberOfDaysOverdue);
    }

    @Bean
    StopFactorCalculator currentOverdueAmountCalculator() {
        return new SimpleValueCalculator(CalculatorType.CURRENT_OVERDUE_AMOUNT, StopFactorIntegerValueDto::currentAmountOfOverdue);
    }

    @Bean
    StopFactorCalculator openCreditsCountCalculator() {
        return new SimpleValueCalculator(CalculatorType.OPEN_CREDITS_COUNT, StopFactorIntegerValueDto::totalOpenCreditContracts);
    }

    @Bean
    StopFactorCalculator bureauRequestsCountCalculator() {
        return new RecentEventsCountCalculator(CalculatorType.BUREAU_REQUESTS_COUNT,
                CreditHistoryEvents::bureauRequestsDaysAgo, 7, StopFactorIntegerValueDto::creditInquiriesLast7Days);
    }

    @Bean
    StopFactorCalculator newCreditsCountCalculator() {
        return new RecentEventsCountCalculator(CalculatorType.NEW_CREDITS_COUNT,
                CreditHistoryEvents::newCreditsDaysAgo, 30, StopFactorIntegerValueDto::totalNumberOfNewOpenLoansInTheLast30Days);
    }
}
