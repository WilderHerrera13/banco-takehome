package com.bp.account.controllers;

import com.bp.account.models.MovementType;
import com.bp.account.service.MovementService;
import com.bp.account.controllers.dto.MovementRequest;
import com.bp.shared.dto.MovementDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/movimientos")
public class MovementController {

    private final MovementService movementService;

    public MovementController(MovementService movementService) {
        this.movementService = movementService;
    }

    @PostMapping("/{accountNumber}")
    public ResponseEntity<MovementDto> register(@PathVariable String accountNumber,
            @Valid @RequestBody MovementRequest request) {
        MovementDto movement = movementService.register(accountNumber, request.date(),
                MovementType.valueOf(request.movementType()), request.amount());
        return ResponseEntity.status(HttpStatus.CREATED).body(movement);
    }

    @GetMapping
    public List<MovementDto> list(@RequestParam(required = false) String accountNumber) {
        return accountNumber == null || accountNumber.isBlank()
                ? movementService.listAll()
                : movementService.listByAccount(accountNumber);
    }

    @GetMapping("/{id}")
    public MovementDto get(@PathVariable Long id) {
        return movementService.get(id);
    }

    @PutMapping("/{id}")
    public MovementDto update(@PathVariable Long id, @Valid @RequestBody MovementRequest request) {
        return movementService.update(id, request.date(),
                MovementType.valueOf(request.movementType()), request.amount());
    }
}
