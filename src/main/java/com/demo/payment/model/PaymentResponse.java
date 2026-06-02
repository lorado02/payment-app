package com.demo.payment.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResponse {

    private String transactionId;
    private TransactionStatus status;
    private String authorizationCode;
    private BigDecimal amount;
    private String currency;
    private String cardNumberMasked;
    private String message;
    private LocalDateTime timestamp;

    // Constructors
    public PaymentResponse() {
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private PaymentResponse response = new PaymentResponse();

        public Builder transactionId(String transactionId) {
            response.transactionId = transactionId;
            return this;
        }

        public Builder status(TransactionStatus status) {
            response.status = status;
            return this;
        }

        public Builder authorizationCode(String authorizationCode) {
            response.authorizationCode = authorizationCode;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            response.amount = amount;
            return this;
        }

        public Builder currency(String currency) {
            response.currency = currency;
            return this;
        }

        public Builder cardNumberMasked(String cardNumberMasked) {
            response.cardNumberMasked = cardNumberMasked;
            return this;
        }

        public Builder message(String message) {
            response.message = message;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            response.timestamp = timestamp;
            return this;
        }

        public PaymentResponse build() {
            return response;
        }
    }

    // Getters and Setters
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCardNumberMasked() {
        return cardNumberMasked;
    }

    public void setCardNumberMasked(String cardNumberMasked) {
        this.cardNumberMasked = cardNumberMasked;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

// Made with Bob
