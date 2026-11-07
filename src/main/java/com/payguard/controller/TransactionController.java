package com.payguard.controller;

import com.payguard.dto.TransactionRequest;
import com.payguard.dto.TransactionResponse;
import com.payguard.entity.TransactionRecord;
import com.payguard.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Process a payment transaction (CREDIT or DEBIT).
     * POST /api/v1/transactions/process
     */
    @PostMapping("/process")
    public ResponseEntity<TransactionResponse> process(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.process(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve transaction history for a user.
     * GET /api/v1/transactions/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TransactionRecord>> getUserHistory(@PathVariable String userId) {
        List<TransactionRecord> history = transactionService.getTransactionHistoryByUserId(userId);
        return ResponseEntity.ok(history);
    }

    /**
     * Retrieve a specific transaction record by transaction ID.
     * GET /api/v1/transactions/{transactionId}
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionRecord> getTransactionById(@PathVariable String transactionId) {
        TransactionRecord record = transactionService.getTransactionByTransactionId(transactionId);
        return ResponseEntity.ok(record);
    }
}

