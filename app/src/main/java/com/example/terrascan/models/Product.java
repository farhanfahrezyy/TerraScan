package com.example.terrascan.models;

public class Product {
    public String productId, productName, productDesc, sellerName, sellerLocation, encodeProductImage;
    public int productPrice;

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
