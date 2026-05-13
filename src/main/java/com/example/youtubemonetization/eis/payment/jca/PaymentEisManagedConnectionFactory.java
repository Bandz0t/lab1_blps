package com.example.youtubemonetization.eis.payment.jca;

import com.example.youtubemonetization.eis.payment.PaymentEisConnectionFactory;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Set;
import javax.security.auth.Subject;

public class PaymentEisManagedConnectionFactory implements ManagedConnectionFactory, Serializable {

    private PrintWriter logWriter;
    private final PaymentEisConnectionProvider connectionProvider;

    public PaymentEisManagedConnectionFactory() {
        this(PaymentEisConnectionImpl::new);
    }

    public PaymentEisManagedConnectionFactory(PaymentEisConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Object createConnectionFactory(ConnectionManager connectionManager) {
        return new PaymentEisConnectionFactoryImpl(this, connectionManager);
    }

    @Override
    public Object createConnectionFactory() {
        return new PaymentEisConnectionFactoryImpl(this, null);
    }

    public PaymentEisConnectionFactory paymentConnectionFactory() {
        return (PaymentEisConnectionFactory) createConnectionFactory();
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo)
            throws ResourceException {
        return new PaymentEisManagedConnection(connectionProvider.createConnection());
    }

    @Override
    public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo connectionRequestInfo)
            throws ResourceException {
        for (Object candidate : connectionSet) {
            if (candidate instanceof PaymentEisManagedConnection managedConnection) {
                return managedConnection;
            }
        }
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        this.logWriter = out;
    }

    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    @Override
    public boolean equals(Object other) {
        return other != null && other.getClass().equals(getClass());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
