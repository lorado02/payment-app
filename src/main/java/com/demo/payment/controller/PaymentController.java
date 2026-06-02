package com.demo.payment.controller;

import com.demo.payment.model.PaymentRequest;
import com.demo.payment.model.PaymentResponse;
import com.demo.payment.model.Transaction;
import com.demo.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/authorize")
    public ResponseEntity<PaymentResponse> authorize(@Valid @RequestBody PaymentRequest request) {
        try {
            PaymentResponse response = paymentService.authorize(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("Authorization failed: " + e.getMessage()));
        }
    }

    @PostMapping("/capture")
    public ResponseEntity<PaymentResponse> capture(@Valid @RequestBody PaymentRequest request) {
        try {
            if (request.getTransactionId() == null || request.getTransactionId().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(buildErrorResponse("Transaction ID is required"));
            }

            PaymentResponse response = paymentService.capture(request);

            if (response.getMessage().contains("not found") || response.getMessage().contains("cannot be captured")) {
                return ResponseEntity.badRequest().body(response);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("Capture failed: " + e.getMessage()));
        }
    }

    @PostMapping("/refund")
    public ResponseEntity<PaymentResponse> refund(@Valid @RequestBody PaymentRequest request) {
        try {
            if (request.getTransactionId() == null || request.getTransactionId().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(buildErrorResponse("Transaction ID is required"));
            }

            PaymentResponse response = paymentService.refund(request);

            if (response.getMessage().contains("not found") || response.getMessage().contains("cannot be refunded")) {
                return ResponseEntity.badRequest().body(response);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("Refund failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTransaction(@PathVariable String id) {
        try {
            Optional<Transaction> transaction = paymentService.getTransaction(id);

            if (!transaction.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(buildErrorResponse("Transaction not found"));
            }

            return ResponseEntity.ok(transaction.get());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("Failed to retrieve transaction: " + e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        try {
            List<Transaction> transactions = paymentService.getRecentTransactions();
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("Failed to retrieve transaction history: " + e.getMessage()));
        }
    }

    private PaymentResponse buildErrorResponse(String message) {
        return PaymentResponse.builder()
                .message(message)
                .build();
    }
}

// Made with Bob
