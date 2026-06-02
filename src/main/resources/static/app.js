const { useState, useEffect } = React;

// Main App Component
function App() {
    const [transactions, setTransactions] = useState([]);
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState(null);

    // Fetch transaction history
    const fetchTransactions = async () => {
        try {
            const response = await fetch('/api/payments/history');
            if (!response.ok) throw new Error('Failed to fetch transactions');
            const data = await response.json();
            setTransactions(data);
        } catch (error) {
            console.error('Error fetching transactions:', error);
        }
    };

    // Auto-refresh every 5 seconds
    useEffect(() => {
        fetchTransactions();
        const interval = setInterval(fetchTransactions, 5000);
        return () => clearInterval(interval);
    }, []);

    // Show message with auto-dismiss
    const showMessage = (text, type) => {
        setMessage({ text, type });
        setTimeout(() => setMessage(null), 5000);
    };

    return (
        <div className="container">
            <div className="header">
                <h1>💳 Payment Processing Demo</h1>
                <p>Mock Credit Card Payment System</p>
            </div>

            {message && (
                <div className={`message message-${message.type}`}>
                    {message.text}
                </div>
            )}

            <PaymentForm 
                onSuccess={() => {
                    fetchTransactions();
                    showMessage('Payment authorized successfully!', 'success');
                }}
                onError={(error) => showMessage(error, 'error')}
            />

            <TransactionHistory 
                transactions={transactions}
                loading={loading}
                onRefresh={fetchTransactions}
                onSuccess={(msg) => {
                    fetchTransactions();
                    showMessage(msg, 'success');
                }}
                onError={(error) => showMessage(error, 'error')}
            />
        </div>
    );
}

// Payment Form Component
function PaymentForm({ onSuccess, onError }) {
    const [formData, setFormData] = useState({
        cardNumber: '',
        expiryDate: '',
        cvv: '',
        amount: ''
    });
    const [submitting, setSubmitting] = useState(false);

    // Format card number with spaces (XXXX XXXX XXXX XXXX)
    const formatCardNumber = (value) => {
        const cleaned = value.replace(/\s/g, '');
        const chunks = cleaned.match(/.{1,4}/g) || [];
        return chunks.join(' ');
    };

    // Format expiry date (MM/YY)
    const formatExpiryDate = (value) => {
        const cleaned = value.replace(/\D/g, '');
        if (cleaned.length >= 2) {
            return cleaned.slice(0, 2) + '/' + cleaned.slice(2, 4);
        }
        return cleaned;
    };

    // Handle input changes
    const handleChange = (e) => {
        const { name, value } = e.target;
        
        if (name === 'cardNumber') {
            const cleaned = value.replace(/\s/g, '');
            if (cleaned.length <= 16 && /^\d*$/.test(cleaned)) {
                setFormData(prev => ({
                    ...prev,
                    [name]: formatCardNumber(cleaned)
                }));
            }
        } else if (name === 'expiryDate') {
            const cleaned = value.replace(/\D/g, '');
            if (cleaned.length <= 4) {
                setFormData(prev => ({
                    ...prev,
                    [name]: formatExpiryDate(cleaned)
                }));
            }
        } else if (name === 'cvv') {
            if (value.length <= 4 && /^\d*$/.test(value)) {
                setFormData(prev => ({ ...prev, [name]: value }));
            }
        } else if (name === 'amount') {
            if (/^\d*\.?\d{0,2}$/.test(value)) {
                setFormData(prev => ({ ...prev, [name]: value }));
            }
        }
    };

    // Validate form
    const validateForm = () => {
        const cardNumberClean = formData.cardNumber.replace(/\s/g, '');
        
        if (cardNumberClean.length < 13 || cardNumberClean.length > 16) {
            onError('Card number must be 13-16 digits');
            return false;
        }
        
        if (!/^\d{2}\/\d{2}$/.test(formData.expiryDate)) {
            onError('Expiry date must be in MM/YY format');
            return false;
        }
        
        if (formData.cvv.length < 3 || formData.cvv.length > 4) {
            onError('CVV must be 3-4 digits');
            return false;
        }
        
        const amount = parseFloat(formData.amount);
        if (isNaN(amount) || amount <= 0) {
            onError('Amount must be greater than 0');
            return false;
        }
        
        return true;
    };

    // Handle form submission
    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (!validateForm()) return;
        
        setSubmitting(true);
        
        try {
            // Parse expiry date (MM/YY) into month and year
            const [expiryMonth, expiryYear] = formData.expiryDate.split('/');
            
            const response = await fetch('/api/payments/authorize', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    cardNumber: formData.cardNumber.replace(/\s/g, ''),
                    expiryMonth: expiryMonth,
                    expiryYear: expiryYear,
                    cvv: formData.cvv,
                    amount: parseFloat(formData.amount),
                    currency: 'USD'
                })
            });
            
            const data = await response.json();
            
            if (response.ok && data.success) {
                // Clear form
                setFormData({
                    cardNumber: '',
                    expiryDate: '',
                    cvv: '',
                    amount: ''
                });
                onSuccess();
            } else {
                onError(data.message || 'Payment authorization failed');
            }
        } catch (error) {
            onError('Network error: ' + error.message);
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="card">
            <h2>Process Payment</h2>
            
            <div className="test-cards">
                <h3>🧪 Test Card Numbers:</h3>
                <ul>
                    <li>Visa: 4263970000005262</li>
                    <li>MasterCard: 5425230000004415</li>
                    <li>Amex: 374101000000608</li>
                </ul>
            </div>

            <form onSubmit={handleSubmit}>
                <div className="form-group">
                    <label>Card Number</label>
                    <input
                        type="text"
                        name="cardNumber"
                        value={formData.cardNumber}
                        onChange={handleChange}
                        placeholder="1234 5678 9012 3456"
                        disabled={submitting}
                        required
                    />
                </div>

                <div className="form-row">
                    <div className="form-group">
                        <label>Expiry Date</label>
                        <input
                            type="text"
                            name="expiryDate"
                            value={formData.expiryDate}
                            onChange={handleChange}
                            placeholder="MM/YY"
                            disabled={submitting}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label>CVV</label>
                        <input
                            type="text"
                            name="cvv"
                            value={formData.cvv}
                            onChange={handleChange}
                            placeholder="123"
                            disabled={submitting}
                            required
                        />
                    </div>
                </div>

                <div className="form-group">
                    <label>Amount (USD)</label>
                    <input
                        type="text"
                        name="amount"
                        value={formData.amount}
                        onChange={handleChange}
                        placeholder="100.00"
                        disabled={submitting}
                        required
                    />
                </div>

                <button 
                    type="submit" 
                    className="btn btn-primary"
                    disabled={submitting}
                >
                    {submitting ? 'Processing...' : 'Authorize Payment'}
                </button>
            </form>
        </div>
    );
}

// Transaction History Component
function TransactionHistory({ transactions, loading, onRefresh, onSuccess, onError }) {
    const [actionLoading, setActionLoading] = useState({});

    // Handle capture transaction
    const handleCapture = async (transactionId) => {
        setActionLoading(prev => ({ ...prev, [transactionId]: 'capture' }));
        
        try {
            // Find the transaction to get its details
            const transaction = transactions.find(t => t.id === transactionId);
            
            const response = await fetch('/api/payments/capture', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    transactionId: transactionId,
                    cardNumber: transaction.cardNumber,
                    amount: transaction.amount,
                    currency: transaction.currency
                })
            });
            
            const data = await response.json();
            
            if (response.ok) {
                onSuccess('Payment captured successfully!');
            } else {
                onError(data.message || 'Capture failed');
            }
        } catch (error) {
            onError('Network error: ' + error.message);
        } finally {
            setActionLoading(prev => {
                const newState = { ...prev };
                delete newState[transactionId];
                return newState;
            });
        }
    };

    // Handle refund transaction
    const handleRefund = async (transactionId) => {
        setActionLoading(prev => ({ ...prev, [transactionId]: 'refund' }));
        
        try {
            // Find the transaction to get its details
            const transaction = transactions.find(t => t.id === transactionId);
            
            const response = await fetch('/api/payments/refund', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    transactionId: transactionId,
                    cardNumber: transaction.cardNumber,
                    amount: transaction.amount,
                    currency: transaction.currency
                })
            });
            
            const data = await response.json();
            
            if (response.ok) {
                onSuccess('Payment refunded successfully!');
            } else {
                onError(data.message || 'Refund failed');
            }
        } catch (error) {
            onError('Network error: ' + error.message);
        } finally {
            setActionLoading(prev => {
                const newState = { ...prev };
                delete newState[transactionId];
                return newState;
            });
        }
    };

    // Get status badge class
    const getStatusClass = (status) => {
        const statusMap = {
            'AUTHORIZED': 'status-authorized',
            'CAPTURED': 'status-captured',
            'DECLINED': 'status-declined',
            'REFUNDED': 'status-refunded'
        };
        return statusMap[status] || '';
    };

    // Format date
    const formatDate = (dateString) => {
        const date = new Date(dateString);
        return date.toLocaleString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    // Format card number (show last 4 digits)
    const formatCardDisplay = (cardNumber) => {
        if (!cardNumber) return 'N/A';
        return `****${cardNumber.slice(-4)}`;
    };

    return (
        <div className="card">
            <h2>Transaction History</h2>
            
            {loading ? (
                <div className="loading">
                    <div className="spinner"></div>
                    <span className="loading-text">Loading transactions...</span>
                </div>
            ) : transactions.length === 0 ? (
                <div className="empty-state">
                    <div className="empty-state-icon">📋</div>
                    <p>No transactions yet. Process a payment to get started!</p>
                </div>
            ) : (
                <div className="table-container">
                    <table>
                        <thead>
                            <tr>
                                <th>Transaction ID</th>
                                <th>Card</th>
                                <th>Amount</th>
                                <th>Status</th>
                                <th>Auth Code</th>
                                <th>Date</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {transactions.map((transaction) => (
                                <tr key={transaction.id}>
                                    <td className="transaction-id">
                                        {transaction.id}
                                    </td>
                                    <td className="card-number">
                                        {formatCardDisplay(transaction.cardNumber)}
                                    </td>
                                    <td>${transaction.amount.toFixed(2)}</td>
                                    <td>
                                        <span className={`status-badge ${getStatusClass(transaction.status)}`}>
                                            {transaction.status}
                                        </span>
                                    </td>
                                    <td>{transaction.authorizationCode || 'N/A'}</td>
                                    <td>{formatDate(transaction.createdAt)}</td>
                                    <td>
                                        <div className="action-buttons">
                                            {transaction.status === 'AUTHORIZED' && (
                                                <button
                                                    className="btn btn-success btn-small"
                                                    onClick={() => handleCapture(transaction.id)}
                                                    disabled={actionLoading[transaction.id]}
                                                >
                                                    {actionLoading[transaction.id] === 'capture'
                                                        ? 'Capturing...'
                                                        : 'Capture'}
                                                </button>
                                            )}
                                            {transaction.status === 'CAPTURED' && (
                                                <button
                                                    className="btn btn-warning btn-small"
                                                    onClick={() => handleRefund(transaction.id)}
                                                    disabled={actionLoading[transaction.id]}
                                                >
                                                    {actionLoading[transaction.id] === 'refund'
                                                        ? 'Refunding...'
                                                        : 'Refund'}
                                                </button>
                                            )}
                                            {(transaction.status === 'DECLINED' || transaction.status === 'REFUNDED') && (
                                                <span style={{ color: '#999', fontSize: '0.85rem' }}>
                                                    No actions
                                                </span>
                                            )}
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
}

// Render the app
const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<App />);

// Made with Bob
