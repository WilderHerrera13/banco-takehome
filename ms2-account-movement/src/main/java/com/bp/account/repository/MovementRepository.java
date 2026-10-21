package com.bp.account.repository;

import com.bp.account.models.Movement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MovementRepository extends JpaRepository<Movement, Long> {

    List<Movement> findByAccountNumberOrderByDateAsc(String accountNumber);

    @Query("select m from Movement m join Account c on c.accountNumber = m.accountNumber "
            + "where c.clientId = :clientId and m.date between :from and :to order by m.date asc")
    List<Movement> findMovementsByClientAndPeriod(@Param("clientId") String clientId,
            @Param("from") LocalDate from, @Param("to") LocalDate to);
}
