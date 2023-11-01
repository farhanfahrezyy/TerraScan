package com.example.terrascan.models;

public class Product {
    public String productId, productName, productDesc, sellerName, sellerLocation, sellerPhoneNumber, encodeProductImage;
    public int productPrice;

    public String getProductId() {
        return productId;
    }

    public String getSellerPhoneNumber() {
        return sellerPhoneNumber;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductDesc() {
        return productDesc;
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getSellerLocation() {
        return sellerLocation;
    }

    public String getEncodeProductImage() {
        return encodeProductImage;
    }

    public int getProductPrice() {
        return productPrice;
    }
}
