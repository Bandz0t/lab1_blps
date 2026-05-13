package com.example.youtubemonetization.eis.payment.jca;

import com.example.youtubemonetization.eis.payment.PaymentEisConnection;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.LocalTransaction;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionMetaData;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.transaction.xa.XAResource;
import javax.security.auth.Subject;

class PaymentEisManagedConnection implements ManagedConnection {

    private final List<ConnectionEventListener> listeners = new CopyOnWriteArrayList<>();
    private final PaymentEisConnection connection;
    private PrintWriter logWriter;

    PaymentEisManagedConnection(PaymentEisConnection connection) {
        this.connection = connection;
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo) {
        return connection;
    }

    @Override
    public void destroy() {
        try {
            connection.close();
        } catch (ResourceException ignored) {
        }
        listeners.clear();
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void associateConnection(Object connection) throws ResourceException {
        if (!(connection instanceof PaymentEisConnection)) {
            throw new ResourceException("Unsupported Payment EIS connection handle");
        }
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public XAResource getXAResource() {
        return null;
    }

    @Override
    public LocalTransaction getLocalTransaction() {
        return null;
    }

    @Override
    public ManagedConnectionMetaData getMetaData() {
        return new PaymentEisManagedConnectionMetaData();
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        this.logWriter = out;
    }

    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }
}
