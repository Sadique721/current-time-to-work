package com.xcess.ocs.service;

import com.xcess.ocs.entity.Agreement;
import com.xcess.ocs.entity.BillingType;
import com.xcess.ocs.entity.WeeklyDay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class BillingCycleCalculatorServiceTest {

    private BillingCycleCalculatorService calculator;

    @BeforeEach
    void setUp() {
        calculator = new BillingCycleCalculatorService();
    }

    @Nested
    @DisplayName("DAYS Billing Type Tests")
    class DaysTests {

        @Test
        @DisplayName("DAYS - 30 days cycle")
        void testDays30() {
            Agreement agreement = new Agreement();
            agreement.setBillingType(BillingType.DAYS);
            agreement.setBillingCyclePeriod(30);

            LocalDate start = LocalDate.of(2027, 1, 1);
            BillingCycleResult result = calculator.calculate(start, agreement);

            assertEquals(LocalDate.of(2027, 1, 1), result.cycleStart());
            assertEquals(LocalDate.of(2027, 1, 30), result.cycleEnd());
            assertEquals(LocalDate.of(2027, 1, 31), result.nextCycleStart());
        }

        @Test
        @DisplayName("DAYS - 1 day cycle")
        void testDays1() {
            Agreement agreement = new Agreement();
            agreement.setBillingType(BillingType.DAYS);
            agreement.setBillingCyclePeriod(1);

            LocalDate start = LocalDate.of(2027, 1, 1);
            BillingCycleResult result = calculator.calculate(start, agreement);

            assertEquals(LocalDate.of(2027, 1, 1), result.cycleStart());
            assertEquals(LocalDate.of(2027, 1, 1), result.cycleEnd());
            assertEquals(LocalDate.of(2027, 1, 2), result.nextCycleStart());
        }

        @Test
        @DisplayName("DAYS - null period throws IllegalArgumentException")
        void testDaysNullPeriod() {
            Agreement agreement = new Agreement();
            agreement.setBillingType(BillingType.DAYS);
            agreement.setBillingCyclePeriod(null);

            assertThrows(IllegalArgumentException.class, () ->
                    calculator.calculate(LocalDate.of(2027, 1, 1), agreement));
        }

        @Test
        @DisplayName("DAYS - default fallback when billingType is null")
        void testNullBillingTypeDefaultsToDays() {
            Agreement agreement = new Agreement();
            agreement.setBillingType(null);
            agreement.setBillingCyclePeriod(15);

            LocalDate start = LocalDate.of(2027, 1, 1);
            BillingCycleResult result = calculator.calculate(start, agreement);

            assertEquals(LocalDate.of(2027, 1, 1), result.cycleStart());
            assertEquals(LocalDate.of(2027, 1, 15), result.cycleEnd());
            assertEquals(LocalDate.of(2027, 1, 16), result.nextCycleStart());
        }
    }

    @Nested
    @DisplayName("WEEKLY Billing Type Tests")
    class WeeklyTests {

        @ParameterizedTest(name = "WEEKLY for start date {0}")
        @CsvSource({
                "2027-01-01, 2027-01-07, 2027-01-08", // Friday start
                "2027-01-04, 2027-01-10, 2027-01-11", // Monday start
                "2027-12-27, 2028-01-02, 2028-01-03"  // Year boundary
        })
        void testWeeklyCycles(String startStr, String expectedEndStr, String expectedNextStr) {
            Agreement agreement = new Agreement();
            agreement.setBillingType(BillingType.WEEKLY);
            agreement.setWeeklyDay(WeeklyDay.MON);

            LocalDate start = LocalDate.parse(startStr);
            BillingCycleResult result = calculator.calculate(start, agreement);

            assertEquals(LocalDate.parse(startStr), result.cycleStart());
            assertEquals(LocalDate.parse(expectedEndStr), result.cycleEnd());
            assertEquals(LocalDate.parse(expectedNextStr), result.nextCycleStart());
        }
    }

    @Nested
    @DisplayName("FORTNIGHTLY Billing Type Tests")
    class FortnightlyTests {

        @Test
        @DisplayName("Case 1: Anchor 1st - Jan 01 to Jan 15, Jan 16 to Jan 31")
        void testFortnightlyCase1() {
            Agreement agreement = new Agreement();
            agreement.setBillingType(BillingType.FORTNIGHTLY);
            agreement.setBillingCycleStartDate(LocalDate.of(2027, 1, 1));

            // Leg 1: 01-Jan to 15-Jan
            BillingCycleResult leg1 = calculator.calculate(LocalDate.of(2027, 1, 1), agreement);
            assertEquals(LocalDate.of(2027, 1, 1), leg1.cycleStart());
            assertEquals(LocalDate.of(2027, 1, 15), leg1.cycleEnd());
            assertEquals(LocalDate.of(2027, 1, 16), leg1.nextCycleStart());

            // Leg 2: 16-Jan to 31-Jan
            BillingCycleResult leg2 = calculator.calculate(leg1.nextCycleStart(), agreement);
            assertEquals(LocalDate.of(2027, 1, 16), leg2.cycleStart());
            assertEquals(LocalDate.of(2027, 1, 31), leg2.cycleEnd());
            assertEquals(LocalDate.of(2027, 2, 1), leg2.nextCycleStart());
        }

        @Test
        @DisplayName("Case 2: Anchor 6th - Apr 06 to Apr 20, Apr 21 to May 05")
        void testFortnightlyCase2() {
            Agreement agreement = new Agreement();
            agreement.setBillingType(BillingType.FORTNIGHTLY);
            agreement.setBillingCycleStartDate(LocalDate.of(2027, 4, 6));

            BillingCycleResult leg1 = calculator.calculate(LocalDate.of(2027, 4, 6), agreement);
            assertEquals(LocalDate.of(2027, 4, 6), leg1.cycleStart());
            assertEquals(LocalDate.of(2027, 4, 20), leg1.cycleEnd());
            assertEquals(LocalDate.of(2027, 4, 21), leg1.nextCycleStart());

            BillingCycleResult leg2 = calculator.calculate(leg1.nextCycleStart(), agreement);
            assertEquals(LocalDate.of(2027, 4, 21), leg2.cycleStart());
            assertEquals(LocalDate.of(2027, 5, 5), leg2.cycleEnd());
            assertEquals(LocalDate.of(2027, 5, 6), leg2.nextCycleStart());
        }

        @Test
        @DisplayName("Case 3: Anchor 5th - Apr 05 to Apr 19, Apr 20 to May 04")
        void testFortnightlyCase3() {
            Agreement agreement = new Agreement();
            agreement.setBillingType(BillingType.FORTNIGHTLY);
            agreement.setBillingCycleStartDate(LocalDate.of(2027, 4, 5));

            BillingCycleResult leg1 = calculator.calculate(LocalDate.of(2027, 4, 5), agreement);
            assertEquals(LocalDate.of(2027, 4, 5), leg1.cycleStart());
            assertEquals(LocalDate.of(2027, 4, 19), leg1.cycleEnd());
            assertEquals(LocalDate.of(2027, 4, 20), leg1.nextCycleStart());

            BillingCycleResult leg2 = calculator.calculate(leg1.nextCycleStart(), agreement);
            assertEquals(LocalDate.of(2027, 4, 20), leg2.cycleStart());
            assertEquals(LocalDate.of(2027, 5, 4), leg2.cycleEnd());
            assertEquals(LocalDate.of(2027, 5, 5), leg2.nextCycleStart());
        }

        @Test
        @DisplayName("Case 4: Anchor 6th Feb - Feb 06 to Feb 20, Feb 21 to Mar 05 (13 days)")
        void testFortnightlyCase4() {
            Agreement agreement = new Agreement();
            agreement.setBillingType(BillingType.FORTNIGHTLY);
            agreement.setBillingCycleStartDate(LocalDate.of(2027, 2, 6));

            BillingCycleResult leg1 = calculator.calculate(LocalDate.of(2027, 2, 6), agreement);
            assertEquals(LocalDate.of(2027, 2, 6), leg1.cycleStart());
            assertEquals(LocalDate.of(2027, 2, 20), leg1.cycleEnd());
            assertEquals(LocalDate.of(2027, 2, 21), leg1.nextCycleStart());

            BillingCycleResult leg2 = calculator.calculate(leg1.nextCycleStart(), agreement);
            assertEquals(LocalDate.of(2027, 2, 21), leg2.cycleStart());
            assertEquals(LocalDate.of(2027, 3, 5), leg2.cycleEnd());
            assertEquals(LocalDate.of(2027, 3, 6), leg2.nextCycleStart());
        }
    }

    @Nested
    @DisplayName("MONTHLY Billing Type Tests")
    class MonthlyTests {

        @ParameterizedTest(name = "MONTHLY start {0} -> end {1} -> next {2}")
        @CsvSource({
                "2027-01-01, 2027-01-31, 2027-02-01",
                "2027-01-05, 2027-02-04, 2027-02-05",
                "2027-01-15, 2027-02-14, 2027-02-15",
                "2027-01-20, 2027-02-19, 2027-02-20",
                "2027-01-28, 2027-02-27, 2027-02-28",
                "2027-01-31, 2027-02-27, 2027-02-28",
                "2027-02-01, 2027-02-28, 2027-03-01",
                "2028-02-01, 2028-02-29, 2028-03-01", // Leap year Feb
                "2028-01-31, 2028-02-28, 2028-02-29", // Leap year Jan 31
                "2027-12-31, 2028-01-30, 2028-01-31"  // Dec to Jan
        })
        void testMonthlyCycles(String startStr, String expectedEndStr, String expectedNextStr) {
            Agreement agreement = new Agreement();
            agreement.setBillingType(BillingType.MONTHLY);

            LocalDate start = LocalDate.parse(startStr);
            BillingCycleResult result = calculator.calculate(start, agreement);

            assertEquals(LocalDate.parse(startStr), result.cycleStart());
            assertEquals(LocalDate.parse(expectedEndStr), result.cycleEnd());
            assertEquals(LocalDate.parse(expectedNextStr), result.nextCycleStart());
            assertEquals(result.nextCycleStart(), result.cycleEnd().plusDays(1));
        }

        @Test
        @DisplayName("MONTHLY - Consecutive 6 cycles from Jan 31 have zero gaps/overlaps")
        void testMonthlyConsecutiveCyclesFromJan31() {
            Agreement agreement = new Agreement();
            agreement.setBillingType(BillingType.MONTHLY);

            LocalDate current = LocalDate.of(2027, 1, 31);
            for (int i = 0; i < 6; i++) {
                BillingCycleResult result = calculator.calculate(current, agreement);
                assertEquals(current, result.cycleStart());
                assertEquals(result.nextCycleStart(), result.cycleEnd().plusDays(1));
                current = result.nextCycleStart();
            }
        }
    }
}
