package com.example.exp_app;

public class Product {

    private String name;
    private String expirationDate;
    private String disposalInfo;
    private long createdAt;

    public Product(String name, String expirationDate, String disposalInfo) {
        this.name = name;
        this.expirationDate = expirationDate;
        this.disposalInfo = disposalInfo;
        this.createdAt = System.currentTimeMillis();
    }

    public String getName() {
        return name;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public String getDisposalInfo() {
        return disposalInfo;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
