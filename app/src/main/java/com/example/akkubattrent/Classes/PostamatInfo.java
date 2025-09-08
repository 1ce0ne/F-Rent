package com.example.akkubattrent.Classes;

public class PostamatInfo {
    private String address;
    private int postamatId;
    private double firstCoordinate;
    private double secondCoordinate;
    private String qrCodeId;

    public PostamatInfo(String address, int postamatId, double firstCoordinate,
                        double secondCoordinate, String qrCodeId) {
        this.address = address;
        this.postamatId = postamatId;
        this.firstCoordinate = firstCoordinate;
        this.secondCoordinate = secondCoordinate;
        this.qrCodeId = qrCodeId;
    }

    // Геттеры
    public String getAddress() { return address; }
    public int getPostamatId() { return postamatId; }
    public double getFirstCoordinate() { return firstCoordinate; }
    public double getSecondCoordinate() { return secondCoordinate; }
    public String getQrCodeId() { return qrCodeId; }
}