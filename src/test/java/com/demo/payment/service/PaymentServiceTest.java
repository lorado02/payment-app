package com.demo.payment.service;

import com.demo.payment.model.*;
import com.demo.payment.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService
 * Tests business logic, transaction management, and data transformations
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest validRequest;
    private Transaction authorizedTransaction;
    private Transaction capturedTransaction;

    @BeforeEach
    void setUp() {
        // Setup valid payment request
        validRequest = PaymentRequest.builder()
                .cardNumber("4263970000005262")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .cvv("123")
                .expiryMonth("12")
                .expiryYear("2025")
                .build();

        // Setup authorized transaction
        authorizedTransaction = Transaction.builder()
                .cardNumber("****5262")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(TransactionStatus.AUTHORIZED)
                .authorizationCode("123456")
                .build();
        authorizedTransaction.setId("test-transaction-id");

        // Setup captured transaction
        capturedTransaction = Transaction.builder()
                .cardNumber("****5262")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(TransactionStatus.CAPTURED)
                .authorizationCode("123456")
                .build();
        capturedTransaction.setId("test-transaction-id");
    }

    // ==================== Authorization Tests ====================

    @Test
    void authorize_WithValidRequest_ReturnsAuthorizedResponse() {
        // Arrange
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction t = invocation.getArgument(0);
                    t.setId("generated-id");
                    return t;
                });

        // Act
        PaymentResponse response = paymentService.authorize(validRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.AUTHORIZED);
        assertThat(response.getTransactionId()).isEqualTo("generated-id");
        assertThat(response.getAuthorizationCode()).isNotNull();
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(response.getCurrency()).isEqualTo("USD");
        assertThat(response.getCardNumberMasked()).isEqualTo("****5262");
        assertThat(response.getMessage()).isEqualTo("Transaction authorized successfully");
        assertThat(response.getTimestamp()).isNotNull();

        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"4263970000005262", "5425230000004415", "374101000000608"})
    void authorize_WithTestCards_AlwaysApproves(String cardNumber) {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
                .cardNumber(cardNumber)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .build();

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction t = invocation.getArgument(0);
                    t.setId("test-id");
                    return t;
                });

        // Act
        PaymentResponse response = paymentService.authorize(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.AUTHORIZED);
        assertThat(response.getAuthorizationCode()).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Transaction authorized successfully");
    }

    @Test
    void authorize_MasksCardNumberCorrectly() {
        // Arrange
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        paymentService.authorize(validRequest);

        // Assert
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();
        assertThat(savedTransaction.getCardNumber()).isEqualTo("****5262");
    }

    @Test
    void authorize_GeneratesAuthorizationCode() {
        // Arrange
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PaymentResponse response = paymentService.authorize(validRequest);

        // Assert
        assertThat(response.getAuthorizationCode()).isNotNull();
        assertThat(response.getAuthorizationCode()).hasSize(6);
        assertThat(response.getAuthorizationCode()).matches("\\d{6}");
    }

    @Test
    void authorize_SavesTransactionWithCorrectData() {
        // Arrange
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        paymentService.authorize(validRequest);

        // Assert
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();
        assertThat(savedTransaction.getCardNumber()).isEqualTo("****5262");
        assertThat(savedTransaction.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(savedTransaction.getCurrency()).isEqualTo("USD");
        assertThat(savedTransaction.getStatus()).isEqualTo(TransactionStatus.AUTHORIZED);
        assertThat(savedTransaction.getAuthorizationCode()).isNotNull();
    }

    // ==================== Capture Tests ====================

    @Test
    void capture_WithValidTransactionId_ReturnsSuccessResponse() {
        // Arrange
        PaymentRequest captureRequest = PaymentRequest.builder()
                .transactionId("test-transaction-id")
                .build();

        when(transactionRepository.findById("test-transaction-id"))
                .thenReturn(Optional.of(authorizedTransaction));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PaymentResponse response = paymentService.capture(captureRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.CAPTURED);
        assertThat(response.getTransactionId()).isEqualTo("test-transaction-id");
        assertThat(response.getMessage()).isEqualTo("Transaction captured successfully");
        assertThat(response.getAuthorizationCode()).isEqualTo("123456");
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(response.getCurrency()).isEqualTo("USD");

        verify(transactionRepository, times(1)).findById("test-transaction-id");
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void capture_UpdatesTransactionStatus() {
        // Arrange
        PaymentRequest captureRequest = PaymentRequest.builder()
                .transactionId("test-transaction-id")
                .build();

        when(transactionRepository.findById("test-transaction-id"))
                .thenReturn(Optional.of(authorizedTransaction));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        paymentService.capture(captureRequest);

        // Assert
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();
        assertThat(savedTransaction.getStatus()).isEqualTo(TransactionStatus.CAPTURED);
    }

    @Test
    void capture_WithNonExistentTransaction_ReturnsErrorResponse() {
        // Arrange
        PaymentRequest captureRequest = PaymentRequest.builder()
                .transactionId("non-existent-id")
                .build();

        when(transactionRepository.findById("non-existent-id"))
                .thenReturn(Optional.empty());

        // Act
        PaymentResponse response = paymentService.capture(captureRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(response.getMessage()).isEqualTo("Transaction not found");
        assertThat(response.getTransactionId()).isEqualTo("non-existent-id");

        verify(transactionRepository, times(1)).findById("non-existent-id");
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void capture_WithNonAuthorizedTransaction_ReturnsErrorResponse() {
        // Arrange
        Transaction declinedTransaction = Transaction.builder()
                .status(TransactionStatus.DECLINED)
                .build();
        declinedTransaction.setId("declined-id");

        PaymentRequest captureRequest = PaymentRequest.builder()
                .transactionId("declined-id")
                .build();

        when(transactionRepository.findById("declined-id"))
                .thenReturn(Optional.of(declinedTransaction));

        // Act
        PaymentResponse response = paymentService.capture(captureRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(response.getMessage()).contains("Transaction cannot be captured");
        assertThat(response.getMessage()).contains("DECLINED");

        verify(transactionRepository, times(1)).findById("declined-id");
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void capture_WithCapturedTransaction_ReturnsErrorResponse() {
        // Arrange
        PaymentRequest captureRequest = PaymentRequest.builder()
                .transactionId("test-transaction-id")
                .build();

        when(transactionRepository.findById("test-transaction-id"))
                .thenReturn(Optional.of(capturedTransaction));

        // Act
        PaymentResponse response = paymentService.capture(captureRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(response.getMessage()).contains("Transaction cannot be captured");
        assertThat(response.getMessage()).contains("CAPTURED");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    // ==================== Refund Tests ====================

    @Test
    void refund_WithValidTransactionId_ReturnsSuccessResponse() {
        // Arrange
        PaymentRequest refundRequest = PaymentRequest.builder()
                .transactionId("test-transaction-id")
                .build();

        when(transactionRepository.findById("test-transaction-id"))
                .thenReturn(Optional.of(capturedTransaction));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PaymentResponse response = paymentService.refund(refundRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.REFUNDED);
        assertThat(response.getTransactionId()).isEqualTo("test-transaction-id");
        assertThat(response.getMessage()).isEqualTo("Transaction refunded successfully");
        assertThat(response.getAuthorizationCode()).isEqualTo("123456");
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(response.getCurrency()).isEqualTo("USD");

        verify(transactionRepository, times(1)).findById("test-transaction-id");
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void refund_UpdatesTransactionStatus() {
        // Arrange
        PaymentRequest refundRequest = PaymentRequest.builder()
                .transactionId("test-transaction-id")
                .build();

        when(transactionRepository.findById("test-transaction-id"))
                .thenReturn(Optional.of(capturedTransaction));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        paymentService.refund(refundRequest);

        // Assert
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();
        assertThat(savedTransaction.getStatus()).isEqualTo(TransactionStatus.REFUNDED);
    }

    @Test
    void refund_WithNonExistentTransaction_ReturnsErrorResponse() {
        // Arrange
        PaymentRequest refundRequest = PaymentRequest.builder()
                .transactionId("non-existent-id")
                .build();

        when(transactionRepository.findById("non-existent-id"))
                .thenReturn(Optional.empty());

        // Act
        PaymentResponse response = paymentService.refund(refundRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(response.getMessage()).isEqualTo("Transaction not found");
        assertThat(response.getTransactionId()).isEqualTo("non-existent-id");

        verify(transactionRepository, times(1)).findById("non-existent-id");
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void refund_WithNonCapturedTransaction_ReturnsErrorResponse() {
        // Arrange
        PaymentRequest refundRequest = PaymentRequest.builder()
                .transactionId("test-transaction-id")
                .build();

        when(transactionRepository.findById("test-transaction-id"))
                .thenReturn(Optional.of(authorizedTransaction));

        // Act
        PaymentResponse response = paymentService.refund(refundRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(response.getMessage()).contains("Transaction cannot be refunded");
        assertThat(response.getMessage()).contains("AUTHORIZED");

        verify(transactionRepository, times(1)).findById("test-transaction-id");
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void refund_WithDeclinedTransaction_ReturnsErrorResponse() {
        // Arrange
        Transaction declinedTransaction = Transaction.builder()
                .status(TransactionStatus.DECLINED)
                .build();
        declinedTransaction.setId("declined-id");

        PaymentRequest refundRequest = PaymentRequest.builder()
                .transactionId("declined-id")
                .build();

        when(transactionRepository.findById("declined-id"))
                .thenReturn(Optional.of(declinedTransaction));

        // Act
        PaymentResponse response = paymentService.refund(refundRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(response.getMessage()).contains("Transaction cannot be refunded");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    // ==================== Get Transaction Tests ====================

    @Test
    void getTransaction_WithValidId_ReturnsTransaction() {
        // Arrange
        when(transactionRepository.findById("test-transaction-id"))
                .thenReturn(Optional.of(authorizedTransaction));

        // Act
        Optional<Transaction> result = paymentService.getTransaction("test-transaction-id");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("test-transaction-id");
        assertThat(result.get().getStatus()).isEqualTo(TransactionStatus.AUTHORIZED);

        verify(transactionRepository, times(1)).findById("test-transaction-id");
    }

    @Test
    void getTransaction_WithInvalidId_ReturnsEmpty() {
        // Arrange
        when(transactionRepository.findById("invalid-id"))
                .thenReturn(Optional.empty());

        // Act
        Optional<Transaction> result = paymentService.getTransaction("invalid-id");

        // Assert
        assertThat(result).isEmpty();

        verify(transactionRepository, times(1)).findById("invalid-id");
    }

    // ==================== Get Recent Transactions Tests ====================

    @Test
    void getRecentTransactions_ReturnsListOfTransactions() {
        // Arrange
        Transaction t1 = Transaction.builder()
                .cardNumber("****1111")
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .status(TransactionStatus.AUTHORIZED)
                .build();
        t1.setId("id-1");

        Transaction t2 = Transaction.builder()
                .cardNumber("****2222")
                .amount(new BigDecimal("75.00"))
                .currency("EUR")
                .status(TransactionStatus.CAPTURED)
                .build();
        t2.setId("id-2");

        List<Transaction> transactions = Arrays.asList(t1, t2);

        when(transactionRepository.findTop50ByOrderByCreatedAtDesc())
                .thenReturn(transactions);

        // Act
        List<Transaction> result = paymentService.getRecentTransactions();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("id-1");
        assertThat(result.get(1).getId()).isEqualTo("id-2");

        verify(transactionRepository, times(1)).findTop50ByOrderByCreatedAtDesc();
    }

    @Test
    void getRecentTransactions_WithNoTransactions_ReturnsEmptyList() {
        // Arrange
        when(transactionRepository.findTop50ByOrderByCreatedAtDesc())
                .thenReturn(Arrays.asList());

        // Act
        List<Transaction> result = paymentService.getRecentTransactions();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(transactionRepository, times(1)).findTop50ByOrderByCreatedAtDesc();
    }
}

// Made with Bob