package com.example.youtubemonetization.eis.payment.jca;

import com.example.youtubemonetization.eis.payment.PaymentEisConnection;
import com.example.youtubemonetization.eis.payment.PaymentEisConnectionFactory;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ManagedConnection;

class PaymentEisConnectionFactoryImpl implements PaymentEisConnectionFactory {

    private final PaymentEisManagedConnectionFactory managedConnectionFactory;
    private final ConnectionManager connectionManager;

    PaymentEisConnectionFactoryImpl(
            PaymentEisManagedConnectionFactory managedConnectionFactory,
            ConnectionManager connectionManager
    ) {
        this.managedConnectionFactory = managedConnectionFactory;
        this.connectionManager = connectionManager;
    }

    @Override
    public PaymentEisConnection getConnection() throws ResourceException {
        if (connectionManager == null) {
            ManagedConnection managedConnection = managedConnectionFactory.createManagedConnection(null, null);
            return (PaymentEisConnection) managedConnection.getConnection(null, null);
        }
        return (PaymentEisConnection) connectionManager.allocateConnection(managedConnectionFactory, null);
    }
}
