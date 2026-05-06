package com.example.youtubemonetization.eis.payment.jca;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ManagedConnectionMetaData;

class PaymentEisManagedConnectionMetaData implements ManagedConnectionMetaData {

    @Override
    public String getEISProductName() {
        return "Corporate Payment EIS";
    }

    @Override
    public String getEISProductVersion() {
        return "1.0";
    }

    @Override
    public int getMaxConnections() {
        return 0;
    }

    @Override
    public String getUserName() throws ResourceException {
        return "youtube-monetization";
    }
}
