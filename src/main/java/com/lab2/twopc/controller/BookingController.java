package com.lab2.twopc.controller;

import com.lab2.twopc.JtaTransactionManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;

import static com.lab2.twopc.JtaTransactionManager.INSERT_INTO_FLY_BOOKING;
import static com.lab2.twopc.JtaTransactionManager.INSERT_INTO_HOTEL_BOOKING;

@RestController
public class BookingController {

    @GetMapping("/")
    public String runGT() throws SQLException {
            JtaTransactionManager test = new JtaTransactionManager();
            test.setup();
            test.runGlobalTransaction(INSERT_INTO_FLY_BOOKING, INSERT_INTO_HOTEL_BOOKING);
        return "Finished execution of distributed transaction\n";
    }

}
