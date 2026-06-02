package com.demo.payment.controller;

import com.demo.payment.model.PaymentRequest;
import com.demo.payment.model.PaymentResponse;
import com.demo.payment.model.Transaction;
import com.demo.payment.model.TransactionStatus;
import com.demo.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for PaymentController
 * Tests REST endpoints, request validation, response mapping, and error handling
 */
@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    private PaymentRequest validAuthRequest;
    private PaymentRequest validCaptureRequest;
    private PaymentRequest validRefundRequest;
    private PaymentResponse authorizedResponse;
    private PaymentResponse capturedResponse;
    private PaymentResponse refundedResponse;
    private Transaction sampleTransaction;

    @BeforeEach
    void setUp() {
        // Setup valid authorization request
        validAuthRequest = PaymentRequest.builder()
                .cardNumber("4263970000005262")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .cvv("123")
                .expiryMonth("12")
                .expiryYear("2025")
                .build();

        // Setup valid capture request
        validCaptureRequest = PaymentRequest.builder()
                .transactionId("test-transaction-id")
                .cardNumber("4263970000005262")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .build();

        // Setup valid refund request
        validRefundRequest = PaymentRequest.builder()
                .transactionId("test-transaction-id")
                .cardNumber("4263970000005262")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .build();

        // Setup authorized response
        authorizedResponse = PaymentResponse.builder()
                .transactionId("test-transaction-id")
                .status(TransactionStatus.AUTHORIZED)
                .authorizationCode("123456")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .cardNumberMasked("****5262")
                .message("Transaction authorized successfully")
                .timestamp(LocalDateTime.now())
                .build();

        // Setup captured response
        capturedResponse = PaymentResponse.builder()
                .transactionId("test-transaction-id")
                .status(TransactionStatus.CAPTURED)
                .authorizationCode("123456")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .cardNumberMasked("****5262")
                .message("Transaction captured successfully")
                .timestamp(LocalDateTime.now())
                .build();

        // Setup refunded response
        refundedResponse = PaymentResponse.builder()
                .transactionId("test-transaction-id")
                .status(TransactionStatus.REFUNDED)
                .authorizationCode("123456")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .cardNumberMasked("****5262")
                .message("Transaction refunded successfully")
                .timestamp(LocalDateTime.now())
                .build();

        // Setup sample transaction
        sampleTransaction = Transaction.builder()
                .cardNumber("****5262")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(TransactionStatus.AUTHORIZED)
                .authorizationCode("123456")
                .build();
    }

    // ==================== AUTHORIZE ENDPOINT TESTS ====================

    @Test
    void authorize_WithValidRequest_ReturnsOkAndAuthorizedResponse() throws Exception {
        // Arrange
        when(paymentService.authorize(any(PaymentRequest.class))).thenReturn(authorizedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/payments/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAuthRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("test-transaction-id"))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.authorizationCode").value("123456"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.cardNumberMasked").value("****5262"))
                .andExpect(jsonPath("$.message").value("Transaction authorized successfully"));

        verify(paymentService, times(1)).authorize(any(PaymentRequest.class));
    }

    @Test
    void authorize_WithInvalidCardNumber_ReturnsBadRequest() throws Exception {
        // Arrange
        PaymentRequest invalidRequest = PaymentRequest.builder()
                .cardNumber("") // Invalid: empty card number
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/payments/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).authorize(any(PaymentRequest.class));
    }

    @Test
    void authorize_WithNullAmount_ReturnsBadRequest() throws Exception {
        // Arrange
        PaymentRequest invalidRequest = PaymentRequest.builder()
                .cardNumber("4263970000005262")
                .amount(null) // Invalid: null amount
                .currency("USD")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/payments/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).authorize(any(PaymentRequest.class));
    }

    @Test
    void authorize_WithNegativeAmount_ReturnsBadRequest() throws Exception {
        // Arrange
        PaymentRequest invalidRequest = PaymentRequest.builder()
                .cardNumber("4263970000005262")
                .amount(new BigDecimal("-100.00")) // Invalid: negative amount
                .currency("USD")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/payments/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).authorize(any(PaymentRequest.class));
    }

    @Test
    void authorize_WithEmptyCurrency_ReturnsBadRequest() throws Exception {
        // Arrange
        PaymentRequest invalidRequest = PaymentRequest.builder()
                .cardNumber("4263970000005262")
                .amount(new BigDecimal("100.00"))
                .currency("") // Invalid: empty currency
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/payments/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).authorize(any(PaymentRequest.class));
    }

    @Test
    void authorize_WithServiceException_ReturnsInternalServerError() throws Exception {
        // Arrange
        when(paymentService.authorize(any(PaymentRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(post("/api/payments/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAuthRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(containsString("Authorization failed")));

        verify(paymentService, times(1)).authorize(any(PaymentRequest.class));
    }

    // ==================== CAPTURE ENDPOINT TESTS ====================

    @Test
    void capture_WithValidRequest_ReturnsOkAndCapturedResponse() throws Exception {
        // Arrange
        when(paymentService.capture(any(PaymentRequest.class))).thenReturn(capturedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/payments/capture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCaptureRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("test-transaction-id"))
                .andExpect(jsonPath("$.status").value("CAPTURED"))
                .andExpect(jsonPath("$.message").value("Transaction captured successfully"));

        verify(paymentService, times(1)).capture(any(PaymentRequest.class));
    }

    @Test
    void capture_WithMissingTransactionId_ReturnsBadRequest() throws Exception {
        // Arrange
        PaymentRequest invalidRequest = PaymentRequest.builder()
                .transactionId(null) // Missing transaction ID
                .cardNumber("4263970000005262")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/payments/capture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Transaction ID is required"));

        verify(paymentService, never()).capture(any(PaymentRequest.class));
    }

    @Test
    void capture_WithEmptyTransactionId_ReturnsBadRequest() throws Exception {
        // Arrange
        PaymentRequest invalidRequest = PaymentRequest.builder()
                .transactionId("") // Empty transaction ID
                .cardNumber("4263970000005262")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/payments/capture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Transaction ID is required"));

        verify(paymentService, never()).capture(any(PaymentRequest.class));
    }

    @Test
    void capture_WithNonExistentTransaction_ReturnsBadRequest() throws Exception {
        // Arrange
        PaymentResponse errorResponse = PaymentResponse.builder()
                .transactionId("non-existent-id")
                .message("Transaction not found")
                .build();

        when(paymentService.capture(any(PaymentRequest.class))).thenReturn(errorResponse);

        // Act & Assert
        mockMvc.perform(post("/api/payments/capture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCaptureRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Transaction not found"));

        verify(paymentService, times(1)).capture(any(PaymentRequest.class));
    }

    @Test
    void capture_WithInvalidTransactionStatus_ReturnsBadRequest() throws Exception {
        // Arrange
        PaymentResponse errorResponse = PaymentResponse.builder()
                .transactionId("test-transaction-id")
                .message("Transaction cannot be captured. Current status: DECLINED")
                .build();

        when(paymentService.capture(any(PaymentRequest.class))).thenReturn(errorResponse);

        // Act & Assert
        mockMvc.perform(post("/api/payments/capture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCaptureRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("cannot be captured")));

        verify(paymentService, times(1)).capture(any(PaymentRequest.class));
    }

    @Test
    void capture_WithServiceException_ReturnsInternalServerError() throws Exception {
        // Arrange
        when(paymentService.capture(any(PaymentRequest.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(post("/api/payments/capture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCaptureRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(containsString("Capture failed")));

        verify(paymentService, times(1)).capture(any(PaymentRequest.class));
    }

    // ==================== REFUND ENDPOINT TESTS ====================

    @Test
    void refund_WithValidRequest_ReturnsOkAndRefundedResponse() throws Exception {
        // Arrange
        when(paymentService.refund(any(PaymentRequest.class))).thenReturn(refundedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/payments/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRefundRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("test-transaction-id"))
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.message").value("Transaction refunded successfully"));

        verify(paymentService, times(1)).refund(any(PaymentRequest.class));
    }

    @Test
    void refund_WithMissingTransactionId_ReturnsBadRequest() throws Exception {
        // Arrange
        PaymentRequest invalidRequest = PaymentRequest.builder()
                .transactionId(null) // Missing transaction ID
                .cardNumber("4263970000005262")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/payments/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Transaction ID is required"));

        verify(paymentService, never()).refund(any(PaymentRequest.class));
    }

    @Test
    void refund_WithEmptyTransactionId_ReturnsBadRequest() throws Exception {
        // Arrange
        PaymentRequest invalidRequest = PaymentRequest.builder()
                .transactionId("") // Empty transaction ID
                .cardNumber("4263970000005262")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/payments/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Transaction ID is required"));

        verify(paymentService, never()).refund(any(PaymentRequest.class));
    }

    @Test
    void refund_WithNonExistentTransaction_ReturnsBadRequest() throws Exception {
        // Arrange
        PaymentResponse errorResponse = PaymentResponse.builder()
                .transactionId("non-existent-id")
                .message("Transaction not found")
                .build();

        when(paymentService.refund(any(PaymentRequest.class))).thenReturn(errorResponse);

        // Act & Assert
        mockMvc.perform(post("/api/payments/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRefundRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Transaction not found"));

        verify(paymentService, times(1)).refund(any(PaymentRequest.class));
    }

    @Test
    void refund_WithInvalidTransactionStatus_ReturnsBadRequest() throws Exception {
        // Arrange
        PaymentResponse errorResponse = PaymentResponse.builder()
                .transactionId("test-transaction-id")
                .message("Transaction cannot be refunded. Current status: AUTHORIZED")
                .build();

        when(paymentService.refund(any(PaymentRequest.class))).thenReturn(errorResponse);

        // Act & Assert
        mockMvc.perform(post("/api/payments/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRefundRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("cannot be refunded")));

        verify(paymentService, times(1)).refund(any(PaymentRequest.class));
    }

    @Test
    void refund_WithServiceException_ReturnsInternalServerError() throws Exception {
        // Arrange
        when(paymentService.refund(any(PaymentRequest.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(post("/api/payments/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRefundRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(containsString("Refund failed")));

        verify(paymentService, times(1)).refund(any(PaymentRequest.class));
    }

    // ==================== GET TRANSACTION ENDPOINT TESTS ====================

    @Test
    void getTransaction_WithValidId_ReturnsOkAndTransaction() throws Exception {
        // Arrange
        String transactionId = "test-transaction-id";
        when(paymentService.getTransaction(transactionId)).thenReturn(Optional.of(sampleTransaction));

        // Act & Assert
        mockMvc.perform(get("/api/payments/{id}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber").value("****5262"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.authorizationCode").value("123456"));

        verify(paymentService, times(1)).getTransaction(transactionId);
    }

    @Test
    void getTransaction_WithNonExistentId_ReturnsNotFound() throws Exception {
        // Arrange
        String transactionId = "non-existent-id";
        when(paymentService.getTransaction(transactionId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/payments/{id}", transactionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transaction not found"));

        verify(paymentService, times(1)).getTransaction(transactionId);
    }

    @Test
    void getTransaction_WithServiceException_ReturnsInternalServerError() throws Exception {
        // Arrange
        String transactionId = "test-transaction-id";
        when(paymentService.getTransaction(transactionId))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/payments/{id}", transactionId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(containsString("Failed to retrieve transaction")));

        verify(paymentService, times(1)).getTransaction(transactionId);
    }

    // ==================== GET HISTORY ENDPOINT TESTS ====================

    @Test
    void getHistory_WithTransactions_ReturnsOkAndTransactionList() throws Exception {
        // Arrange
        Transaction transaction1 = Transaction.builder()
                .cardNumber("****5262")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(TransactionStatus.AUTHORIZED)
                .authorizationCode("123456")
                .build();

        Transaction transaction2 = Transaction.builder()
                .cardNumber("****4415")
                .amount(new BigDecimal("200.00"))
                .currency("EUR")
                .status(TransactionStatus.CAPTURED)
                .authorizationCode("789012")
                .build();

        List<Transaction> transactions = Arrays.asList(transaction1, transaction2);
        when(paymentService.getRecentTransactions()).thenReturn(transactions);

        // Act & Assert
        mockMvc.perform(get("/api/payments/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].cardNumber").value("****5262"))
                .andExpect(jsonPath("$[0].amount").value(100.00))
                .andExpect(jsonPath("$[0].status").value("AUTHORIZED"))
                .andExpect(jsonPath("$[1].cardNumber").value("****4415"))
                .andExpect(jsonPath("$[1].amount").value(200.00))
                .andExpect(jsonPath("$[1].status").value("CAPTURED"));

        verify(paymentService, times(1)).getRecentTransactions();
    }

    @Test
    void getHistory_WithEmptyList_ReturnsOkAndEmptyList() throws Exception {
        // Arrange
        when(paymentService.getRecentTransactions()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/payments/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(paymentService, times(1)).getRecentTransactions();
    }

    @Test
    void getHistory_WithServiceException_ReturnsInternalServerError() throws Exception {
        // Arrange
        when(paymentService.getRecentTransactions())
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/payments/history"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(containsString("Failed to retrieve transaction history")));

        verify(paymentService, times(1)).getRecentTransactions();
    }

    // ==================== CORS CONFIGURATION TESTS ====================

    @Test
    void authorize_WithCorsHeaders_AllowsOrigin() throws Exception {
        // Arrange
        when(paymentService.authorize(any(PaymentRequest.class))).thenReturn(authorizedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/payments/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAuthRequest))
                        .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }
}

// Made with Bob