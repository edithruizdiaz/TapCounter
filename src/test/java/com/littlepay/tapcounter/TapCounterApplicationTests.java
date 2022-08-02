package com.littlepay.tapcounter;

import com.littlepay.tapcounter.service.TripService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class TapCounterApplicationTests {

    TripService tripService;
    @BeforeEach
    void setUp() {
        tripService = new TripService();
    }

    @Test
    @DisplayName("Test costs")
    void contextLoads() {

        //Test a canceled trip
        assertEquals (new Double(0), tripService.calculateCost(1,1));

        //Test a completed trip
        assertEquals (new Double(3.25), tripService.calculateCost(1,2));
        assertEquals (new Double(5.5), tripService.calculateCost(2,3));
        assertEquals (new Double(7.3), tripService.calculateCost(3,1));

        // Test an incompleted trip
        assertEquals (new Double(7.3), tripService.calculateCost(3,-1));

    }

}
