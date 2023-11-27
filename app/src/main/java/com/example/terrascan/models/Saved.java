package com.example.terrascan.models;

public class Saved {
    public String savedId, soilImage, soilType, accuracy, climate, temperature, cropRecommendation;

    public String getSavedId() {
        return savedId;
    }

    public void setSavedId(String savedId) {
        this.savedId = savedId;
    }

    public String getSoilImage() {
        return soilImage;
    }

    public void setSoilImage(String soilImage) {
        this.soilImage = soilImage;
    }

    public String getSoilType() {
        return soilType;
    }

    public void setSoilType(String soilType) {
        this.soilType = soilType;
    }

    public String getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(String accuracy) {
        this.accuracy = accuracy;
    }

    public String getClimate() {
        return climate;
    }

    public void setClimate(String climate) {
        this.climate = climate;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getCropRecommendation() {
        return cropRecommendation;
    }

    public void setCropRecommendation(String cropRecommendation) {
        this.cropRecommendation = cropRecommendation;
    }
}
