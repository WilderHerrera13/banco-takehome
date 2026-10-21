package com.bp.account;

import com.bp.account.models.Account;
import com.bp.account.models.AccountType;
import com.bp.account.models.ClientProjection;
import com.bp.account.models.MovementType;
import com.bp.account.repository.AccountRepository;
import com.bp.account.repository.ClientProjectionRepository;
import com.bp.account.service.AccountStatementService;
import com.bp.account.service.MovementService;
import com.bp.shared.dto.AccountStatementDto;
import com.bp.shared.dto.MovementDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"clients", "movements"})
@DisplayName("F6 - Integration test: movement and account statement report")
class AccountStatementIntegrationTest {

    @Autowired
    private MovementService movementService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ClientProjectionRepository clientProjectionRepository;

    @Autowired
    private AccountStatementService statementService;

    @Test
    @DisplayName("registers a movement, updates the balance and shows it in the report")
    void shouldRegisterMovementAndGenerateReport() {
        clientProjectionRepository.save(new ClientProjection("cli-1", "Marianela Montalvo", "1712345679",
                Instant.now()));
        accountRepository.save(new Account("225487", AccountType.CURRENT, new BigDecimal("100.00"), true, "cli-1"));

        MovementDto movement = movementService.register("225487", LocalDate.of(2022, 2, 10), MovementType.DEPOSIT,
                new BigDecimal("600"));

        assertThat(movement.balance()).isEqualByComparingTo("700.00");

        AccountStatementDto report = statementService.generate("cli-1", LocalDate.of(2022, 2, 1),
                LocalDate.of(2022, 2, 28));

        assertThat(report.client()).isEqualTo("Marianela Montalvo");
        assertThat(report.accounts()).hasSize(1);
        assertThat(report.accounts().get(0).currentBalance()).isEqualByComparingTo("700.00");
        assertThat(report.movements()).hasSize(1);
        assertThat(report.movements().get(0).amount()).isEqualByComparingTo("600.00");
    }
}
