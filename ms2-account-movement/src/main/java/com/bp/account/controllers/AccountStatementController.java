package com.bp.account.controllers;

import com.bp.account.service.AccountStatementService;
import com.bp.shared.dto.AccountStatementDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/reportes")
public class AccountStatementController {

    private final AccountStatementService statementService;

    public AccountStatementController(AccountStatementService statementService) {
        this.statementService = statementService;
    }

    @GetMapping
    public AccountStatementDto accountStatement(
            @RequestParam("fecha") String date,
            @RequestParam("cliente") String clientId) {
        DateRange range = DateRange.parse(date);
        return statementService.generate(clientId, range.from(), range.to());
    }

    record DateRange(LocalDate from, LocalDate to) {

        static DateRange parse(String text) {
            String[] parts = text.split("\\.\\.|,|/| a ");
            if (parts.length != 2) {
                throw new IllegalArgumentException(
                        "The date range must use the format from..to (for example 2022-02-01..2022-02-28)");
            }
            try {
                return new DateRange(LocalDate.parse(parts[0].trim()), LocalDate.parse(parts[1].trim()));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Dates must use the yyyy-MM-dd format: " + text);
            }
        }
    }
}
