package com.urbancompany.clone.service;

import com.urbancompany.clone.model.ServiceProvider;

public class ProviderWithDistance {
    private ServiceProvider provider;
    private Double distance;

    public ProviderWithDistance() {}

    public ProviderWithDistance(ServiceProvider provider, Double distance) {
        this.provider = provider;
        this.distance = distance;
    }

    public ServiceProvider getProvider() {
        return provider;
    }

    public void setProvider(ServiceProvider provider) {
        this.provider = provider;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }
}
