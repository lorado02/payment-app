# Unit Testing Strategy - Payment Application

## Table of Contents
1. [Project Overview](#project-overview)
2. [Architecture and Key Components](#architecture-and-key-components)
3. [Unit Testing Approach](#unit-testing-approach)
4. [Test File Structure and Naming Conventions](#test-file-structure-and-naming-conventions)
5. [Test Execution Commands](#test-execution-commands)
6. [Recommended Testing Frameworks and Libraries](#recommended-testing-frameworks-and-libraries)
7. [Test Quality Metrics and Coverage Thresholds](#test-quality-metrics-and-coverage-thresholds)
8. [Best Practices and Guidelines](#best-practices-and-guidelines)

---

## Project Overview

**Project Name:** Payment Application  
**Build Tool:** Maven  
**Java Version:** 17  
**Spring Boot Version:** 2.7.18  
**Database:** H2 (In-Memory)  
**Architecture:** Spring Boot REST API with JPA/Hibernate

### Purpose
Mock credit card payment processing application that handles authorization, capture, and refund operations with transaction management and caching capabilities.

---

## Architecture and Key Components

### 1. **Controller Layer**
- [`PaymentController`](src/main/java/com/demo/payment/controller/PaymentController.java) - Handles payment operations (authorize, capture, refund, transaction retrieval)
- [`AdminController`](src/main/java/com/demo/payment/controller/AdminController.java) - Manages administrative operations (cache management)

### 2. **Service Layer**
- [`PaymentService`](src/main/java/com/demo/payment/service/PaymentService.java) - Core business logic for payment processing
- [`CacheService`](src/main/java/com/demo/payment/service/CacheService.java) - Cache management operations

### 3. **Repository Layer**
- [`TransactionRepository`](src/main/java/com/demo/payment/repository/TransactionRepository.java) - JPA repository for transaction persistence

### 4. **Model Layer**
- [`Transaction`](src/main/java/com/demo/payment/model/Transaction.java) - JPA entity for transaction data
- [`PaymentRequest`](src/main/java/com/demo/payment/model/PaymentRequest.java) - Request DTO with validation
- [`PaymentResponse`](src/main/java/com/demo/payment/model/PaymentResponse.java) - Response DTO
- [`TransactionStatus`](src/main/java/com/demo/payment/model/TransactionStatus.java) - Enum for transaction states

### 5. **Configuration Layer**
- [`CacheConfig`](src/main/java/com/demo/payment/config/CacheConfig.java) - Caffeine cache configuration
- [`PaymentApplication`](src/main/java/com/demo/payment/PaymentApplication.java) - Spring Boot application entry point

---

## Unit Testing Approach

### 1. Controller Layer Testing

**Objective:** Test REST endpoints, request validation, response mapping, and error handling.

**Strategy:**
- Use `@WebMvcTest` for isolated controller testing
- Mock service layer dependencies with `@MockBean`
- Test HTTP status codes, response bodies, and error scenarios
- Validate request validation constraints
- Test CORS configuration

**Key Test Scenarios:**
- **PaymentController:**
  - Successful authorization with valid request
  - Authorization with invalid card number (validation)
  - Capture with valid transaction ID
  - Capture with missing/invalid transaction ID
  - Refund with valid transaction ID
  - Refund with invalid transaction status
  - Get transaction by ID (found/not found)
  - Get transaction history
  - Exception handling for all endpoints

- **AdminController:**
  - Successful cache clearing
  - Exception handling during cache operations

### 2. Service Layer Testing

**Objective:** Test business logic, transaction management, and data transformations.

**Strategy:**
- Use `@ExtendWith(MockitoExtension.class)` for pure unit tests
- Mock repository dependencies with `@Mock`
- Test business rules and edge cases
- Verify transaction state transitions
- Test caching behavior

**Key Test Scenarios:**
- **PaymentService:**
  - Authorization with test cards (always approve)
  - Authorization with random cards (90% approval rate)
  - Card number masking logic
  - Authorization code generation
  - Capture authorized transactions
  - Capture non-authorized transactions (should fail)
  - Refund captured transactions
  - Refund non-captured transactions (should fail)
  - Transaction retrieval with caching
  - Recent transactions retrieval
  - Decline reason assignment (insufficient funds, expired card, generic)
  - Processing delay simulation (optional - can be mocked)

- **CacheService:**
  - Clear all caches successfully
  - Clear specific cache by name
  - Handle null cache scenarios

### 3. Repository Layer Testing

**Objective:** Test data access operations and custom queries.

**Strategy:**
- Use `@DataJpaTest` for repository testing
- Use in-memory H2 database for tests
- Test custom query methods
- Verify entity lifecycle callbacks

**Key Test Scenarios:**
- **TransactionRepository:**
  - Save and retrieve transactions
  - Find top 50 transactions ordered by creation date
  - Verify UUID generation on persist
  - Verify timestamp updates on persist and update
  - Test transaction status updates

### 4. Model Layer Testing

**Objective:** Test entity behavior, validation, and builder patterns.

**Strategy:**
- Use plain JUnit tests for POJOs
- Test validation annotations with `Validator`
- Test builder patterns
- Test entity lifecycle callbacks

**Key Test Scenarios:**
- **Transaction:**
  - Builder pattern functionality
  - UUID generation in `@PrePersist`
  - Timestamp initialization in `@PrePersist`
  - Timestamp update in `@PreUpdate`
  - Getters and setters

- **PaymentRequest:**
  - Builder pattern functionality
  - Validation constraints (NotBlank, NotNull, Positive)
  - Invalid scenarios (null/empty card number, negative amount, null currency)

- **PaymentResponse:**
  - Builder pattern functionality
  - All field assignments

- **TransactionStatus:**
  - Enum values existence
  - Enum valueOf operations

### 5. Configuration Layer Testing

**Objective:** Test Spring configuration and bean creation.

**Strategy:**
- Use `@SpringBootTest` for integration-style configuration tests
- Verify bean creation and autowiring
- Test cache configuration properties

**Key Test Scenarios:**
- **CacheConfig:**
  - CacheManager bean creation
  - Caffeine cache configuration (max size, expiration, stats)
  - Cache name registration

- **PaymentApplication:**
  - Application context loads successfully
  - All required beans are created

---

## Test File Structure and Naming Conventions

### Directory Structure
```
src/
├── main/
│   └── java/
│       └── com/demo/payment/
│           ├── PaymentApplication.java
│           ├── config/
│           ├── controller/
│           ├── model/
│           ├── repository/
│           └── service/
└── test/
    ├── java/
    │   └── com/demo/payment/
    │       ├── PaymentApplicationTests.java
    │       ├── config/
    │       │   └── CacheConfigTest.java
    │       ├── controller/
    │       │   ├── PaymentControllerTest.java
    │       │   └── AdminControllerTest.java
    │       ├── model/
    │       │   ├── TransactionTest.java
    │       │   ├── PaymentRequestTest.java
    │       │   ├── PaymentResponseTest.java
    │       │   └── TransactionStatusTest.java
    │       ├── repository/
    │       │   └── TransactionRepositoryTest.java
    │       └── service/
    │           ├── PaymentServiceTest.java
    │           └── CacheServiceTest.java
    └── resources/
        └── application-test.properties
```

### Naming Conventions

1. **Test Class Names:**
   - Format: `{ClassName}Test.java`
   - Examples: `PaymentServiceTest.java`, `PaymentControllerTest.java`

2. **Test Method Names:**
   - Format: `{methodName}_{scenario}_{expectedResult}`
   - Use descriptive names that explain the test scenario
   - Examples:
     - `authorize_WithValidRequest_ReturnsAuthorizedResponse()`
     - `capture_WithInvalidTransactionId_ReturnsBadRequest()`
     - `maskCardNumber_WithValidCard_ReturnsLastFourDigits()`

3. **Test Data:**
   - Create test data builders or fixtures in separate classes
   - Use meaningful variable names
   - Example: `TestDataBuilder.java`, `PaymentRequestFixtures.java`

---

## Test Execution Commands

### Basic Test Execution

```bash
# Run all tests
mvn test

# Run tests with verbose output
mvn test -X

# Run tests and skip compilation
mvn surefire:test

# Clean and run all tests
mvn clean test
```

### Running Specific Tests

```bash
# Run a specific test class
mvn test -Dtest=PaymentServiceTest

# Run a specific test method
mvn test -Dtest=PaymentServiceTest#authorize_WithValidRequest_ReturnsAuthorizedResponse

# Run multiple test classes
mvn test -Dtest=PaymentServiceTest,PaymentControllerTest

# Run tests matching a pattern
mvn test -Dtest=*ServiceTest
```

### Test Execution with Coverage

```bash
# Run tests with JaCoCo coverage report
mvn clean test jacoco:report

# Run tests and generate coverage report in specific format
mvn clean test jacoco:report -Djacoco.outputDirectory=target/coverage-reports

# View coverage report (after running above command)
# Open: target/site/jacoco/index.html in browser
```

### Continuous Integration Commands

```bash
# Run tests in CI environment (fail fast)
mvn clean test -B -Dmaven.test.failure.ignore=false

# Run tests with coverage and fail if below threshold
mvn clean test jacoco:report jacoco:check

# Run tests and generate reports for CI
mvn clean test surefire-report:report
```

### Parallel Test Execution

```bash
# Run tests in parallel (faster execution)
mvn test -T 4

# Run tests with parallel execution at class level
mvn test -DforkCount=4 -DreuseForks=true
```

---

## Recommended Testing Frameworks and Libraries

### Core Testing Dependencies

Add these dependencies to your `pom.xml`:

```xml
<dependencies>
    <!-- Already included: Spring Boot Starter Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- JaCoCo for Code Coverage -->
    <dependency>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>0.8.11</version>
        <scope>test</scope>
    </dependency>

    <!-- AssertJ for fluent assertions (included in spring-boot-starter-test) -->
    <!-- Mockito for mocking (included in spring-boot-starter-test) -->
    <!-- JUnit 5 (Jupiter) (included in spring-boot-starter-test) -->

    <!-- Optional: REST Assured for API testing -->
    <dependency>
        <groupId>io.rest-assured</groupId>
        <artifactId>rest-assured</artifactId>
        <version>5.3.2</version>
        <scope>test</scope>
    </dependency>

    <!-- Optional: Testcontainers for integration tests -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>

    <!-- Optional: Awaitility for async testing -->
    <dependency>
        <groupId>org.awaitility</groupId>
        <artifactId>awaitility</artifactId>
        <version>4.2.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Maven Plugins Configuration

```xml
<build>
    <plugins>
        <!-- Surefire Plugin for Test Execution -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.5</version>
            <configuration>
                <includes>
                    <include>**/*Test.java</include>
                    <include>**/*Tests.java</include>
                </includes>
                <argLine>@{argLine} -Xmx1024m</argLine>
            </configuration>
        </plugin>

        <!-- JaCoCo Plugin for Code Coverage -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.11</version>
            <executions>
                <execution>
                    <id>prepare-agent</id>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
                <execution>
                    <id>check</id>
                    <goals>
                        <goal>check</goal>
                    </goals>
                    <configuration>
                        <rules>
                            <rule>
                                <element>PACKAGE</element>
                                <limits>
                                    <limit>
                                        <counter>LINE</counter>
                                        <value>COVEREDRATIO</value>
                                        <minimum>0.80</minimum>
                                    </limit>
                                </limits>
                            </rule>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>

        <!-- Surefire Report Plugin -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-report-plugin</artifactId>
            <version>3.2.5</version>
        </plugin>
    </plugins>
</build>
```

### Testing Framework Stack

| Framework/Library | Purpose | Version |
|------------------|---------|---------|
| **JUnit 5 (Jupiter)** | Core testing framework | 5.9.3 (via Spring Boot) |
| **Mockito** | Mocking framework | 4.11.0 (via Spring Boot) |
| **AssertJ** | Fluent assertions | 3.24.2 (via Spring Boot) |
| **Spring Test** | Spring testing support | 5.3.31 (via Spring Boot) |
| **MockMvc** | Controller testing | 5.3.31 (via Spring Boot) |
| **H2 Database** | In-memory database for tests | 2.1.214 |
| **JaCoCo** | Code coverage | 0.8.11 |
| **REST Assured** | API testing (optional) | 5.3.2 |
| **Hamcrest** | Matchers for assertions | 2.2 (via Spring Boot) |

---

## Test Quality Metrics and Coverage Thresholds

### Coverage Targets

| Component | Line Coverage | Branch Coverage | Priority |
|-----------|--------------|-----------------|----------|
| **Service Layer** | ≥ 90% | ≥ 85% | Critical |
| **Controller Layer** | ≥ 85% | ≥ 80% | High |
| **Repository Layer** | ≥ 80% | ≥ 75% | High |
| **Model Layer** | ≥ 70% | ≥ 65% | Medium |
| **Configuration Layer** | ≥ 60% | ≥ 55% | Low |
| **Overall Project** | ≥ 80% | ≥ 75% | Critical |

### Quality Metrics

#### 1. **Code Coverage Metrics**
- **Line Coverage:** Percentage of executable lines covered by tests
- **Branch Coverage:** Percentage of decision branches covered by tests
- **Method Coverage:** Percentage of methods invoked by tests
- **Class Coverage:** Percentage of classes with at least one test

#### 2. **Test Quality Indicators**

**Test Effectiveness:**
- Mutation testing score: ≥ 70% (using PIT or similar)
- Test-to-code ratio: 1:1 to 1:1.5
- Average assertions per test: 2-5
- Test execution time: < 30 seconds for unit tests

**Test Maintainability:**
- Test code duplication: < 5%
- Test method length: < 50 lines
- Setup complexity: < 20 lines per test
- Test dependencies: Minimal external dependencies

**Test Reliability:**
- Flaky test rate: 0%
- Test failure rate: < 1% (excluding intentional failures)
- Test isolation: 100% (no test interdependencies)

#### 3. **Reporting and Monitoring**

**Coverage Reports:**
```bash
# Generate HTML coverage report
mvn clean test jacoco:report

# View report at: target/site/jacoco/index.html
```

**Coverage Report Sections:**
- Overall coverage summary
- Package-level coverage
- Class-level coverage
- Method-level coverage
- Missed lines and branches

**CI/CD Integration:**
- Fail build if coverage drops below 80%
- Generate coverage trends over time
- Publish coverage reports to SonarQube or similar
- Block PRs with insufficient test coverage

### Coverage Exclusions

Exclude the following from coverage requirements:
- Configuration classes (unless complex logic)
- DTOs with only getters/setters
- Main application class
- Generated code
- Exception classes with only constructors

```xml
<!-- JaCoCo exclusions configuration -->
<configuration>
    <excludes>
        <exclude>**/PaymentApplication.class</exclude>
        <exclude>**/model/*Builder.class</exclude>
        <exclude>**/config/CacheConfig.class</exclude>
    </excludes>
</configuration>
```

---

## Best Practices and Guidelines

### 1. **Test Organization**

**AAA Pattern (Arrange-Act-Assert):**
```java
@Test
void authorize_WithValidRequest_ReturnsAuthorizedResponse() {
    // Arrange
    PaymentRequest request = PaymentRequest.builder()
        .cardNumber("4263970000005262")
        .amount(new BigDecimal("100.00"))
        .currency("USD")
        .build();
    
    // Act
    PaymentResponse response = paymentService.authorize(request);
    
    // Assert
    assertThat(response.getStatus()).isEqualTo(TransactionStatus.AUTHORIZED);
    assertThat(response.getAuthorizationCode()).isNotNull();
}
```

### 2. **Mocking Best Practices**

**Use appropriate mocking strategies:**
```java
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @InjectMocks
    private PaymentService paymentService;
    
    @Test
    void capture_WithValidTransactionId_UpdatesStatus() {
        // Arrange
        Transaction transaction = Transaction.builder()
            .status(TransactionStatus.AUTHORIZED)
            .build();
        
        when(transactionRepository.findById(anyString()))
            .thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act & Assert
        PaymentResponse response = paymentService.capture(request);
        
        verify(transactionRepository).save(argThat(t -> 
            t.getStatus() == TransactionStatus.CAPTURED));
    }
}
```

### 3. **Controller Testing**

**Use MockMvc for controller tests:**
```java
@WebMvcTest(PaymentController.class)
class PaymentControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private PaymentService paymentService;
    
    @Test
    void authorize_WithValidRequest_ReturnsOk() throws Exception {
        // Arrange
        PaymentResponse response = PaymentResponse.builder()
            .status(TransactionStatus.AUTHORIZED)
            .build();
        
        when(paymentService.authorize(any())).thenReturn(response);
        
        // Act & Assert
        mockMvc.perform(post("/api/payments/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cardNumber\":\"4263970000005262\",\"amount\":100.00,\"currency\":\"USD\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("AUTHORIZED"));
    }
}
```

### 4. **Repository Testing**

**Use @DataJpaTest for repository tests:**
```java
@DataJpaTest
class TransactionRepositoryTest {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Test
    void findTop50ByOrderByCreatedAtDesc_ReturnsOrderedTransactions() {
        // Arrange
        Transaction t1 = createTransaction();
        Transaction t2 = createTransaction();
        entityManager.persist(t1);
        entityManager.persist(t2);
        entityManager.flush();
        
        // Act
        List<Transaction> transactions = transactionRepository
            .findTop50ByOrderByCreatedAtDesc();
        
        // Assert
        assertThat(transactions).hasSize(2);
        assertThat(transactions.get(0).getCreatedAt())
            .isAfterOrEqualTo(transactions.get(1).getCreatedAt());
    }
}
```

### 5. **Test Data Management**

**Create test data builders:**
```java
public class TestDataBuilder {
    
    public static PaymentRequest.Builder validPaymentRequest() {
        return PaymentRequest.builder()
            .cardNumber("4263970000005262")
            .amount(new BigDecimal("100.00"))
            .currency("USD")
            .cvv("123")
            .expiryMonth("12")
            .expiryYear("2025");
    }
    
    public static Transaction.Builder authorizedTransaction() {
        return Transaction.builder()
            .cardNumber("****5262")
            .amount(new BigDecimal("100.00"))
            .currency("USD")
            .status(TransactionStatus.AUTHORIZED)
            .authorizationCode("123456");
    }
}
```

### 6. **Assertion Best Practices**

**Use AssertJ for fluent assertions:**
```java
// Good - Fluent and readable
assertThat(response.getStatus())
    .isEqualTo(TransactionStatus.AUTHORIZED);

assertThat(response.getAmount())
    .isGreaterThan(BigDecimal.ZERO)
    .isLessThanOrEqualTo(new BigDecimal("1000.00"));

assertThat(transactions)
    .hasSize(3)
    .extracting(Transaction::getStatus)
    .containsOnly(TransactionStatus.AUTHORIZED);

// Avoid - Less readable
assertEquals(TransactionStatus.AUTHORIZED, response.getStatus());
assertTrue(response.getAmount().compareTo(BigDecimal.ZERO) > 0);
```

### 7. **Exception Testing**

**Test exception scenarios:**
```java
@Test
void capture_WithNonExistentTransaction_ThrowsException() {
    // Arrange
    when(transactionRepository.findById(anyString()))
        .thenReturn(Optional.empty());
    
    // Act & Assert
    assertThatThrownBy(() -> paymentService.capture(request))
        .isInstanceOf(TransactionNotFoundException.class)
        .hasMessageContaining("Transaction not found");
}
```

### 8. **Parameterized Tests**

**Use parameterized tests for multiple scenarios:**
```java
@ParameterizedTest
@ValueSource(strings = {"4263970000005262", "5425230000004415", "374101000000608"})
void authorize_WithTestCards_AlwaysApproves(String cardNumber) {
    // Arrange
    PaymentRequest request = PaymentRequest.builder()
        .cardNumber(cardNumber)
        .amount(new BigDecimal("100.00"))
        .currency("USD")
        .build();
    
    // Act
    PaymentResponse response = paymentService.authorize(request);
    
    // Assert
    assertThat(response.getStatus()).isEqualTo(TransactionStatus.AUTHORIZED);
}
```

### 9. **Test Isolation**

**Ensure tests are independent:**
- Use `@BeforeEach` for test setup
- Use `@AfterEach` for cleanup
- Avoid shared mutable state
- Reset mocks between tests
- Use separate test databases

### 10. **Performance Testing**

**Add performance assertions where relevant:**
```java
@Test
void authorize_CompletesWithinTimeLimit() {
    // Arrange
    PaymentRequest request = validPaymentRequest().build();
    
    // Act & Assert
    assertTimeout(Duration.ofSeconds(1), () -> {
        paymentService.authorize(request);
    });
}
```

---

## Implementation Checklist

- [ ] Add JaCoCo plugin to `pom.xml`
- [ ] Add Surefire plugin configuration
- [ ] Create test directory structure
- [ ] Create test data builders
- [ ] Implement controller tests
- [ ] Implement service tests
- [ ] Implement repository tests
- [ ] Implement model tests
- [ ] Configure test properties
- [ ] Set up CI/CD pipeline with coverage checks
- [ ] Document test execution procedures
- [ ] Review and achieve coverage targets
- [ ] Set up coverage reporting dashboard

---

## Additional Resources

### Documentation
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)

### Tools
- **SonarQube:** Code quality and coverage analysis
- **IntelliJ IDEA:** Built-in test runner and coverage tools
- **Maven Surefire Report:** HTML test reports
- **JaCoCo:** Coverage reports and enforcement

---

**Document Version:** 1.0  
**Last Updated:** 2026-06-02  
**Maintained By:** Development Team