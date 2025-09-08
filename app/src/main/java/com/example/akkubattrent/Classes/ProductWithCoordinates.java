package com.example.akkubattrent.Classes;

import android.os.Parcel;
import android.os.Parcelable;

public class ProductWithCoordinates extends Product implements Parcelable {
    private double firstCoordinate;
    private double secondCoordinate;

    public ProductWithCoordinates(int id, String name, String description, double price_per_hour,
                                  double price_per_day, double price_per_month, byte[] image,
                                  String productUuid, String address, int cellId,
                                  double firstCoordinate, double secondCoordinate) {
        super(id, name, description, price_per_hour, price_per_day, price_per_month,
                image, productUuid, address, cellId);
        this.firstCoordinate = firstCoordinate;
        this.secondCoordinate = secondCoordinate;
    }

    protected ProductWithCoordinates(Parcel in) {
        super(in);
        firstCoordinate = in.readDouble();
        secondCoordinate = in.readDouble();
    }

    public static final Creator<ProductWithCoordinates> CREATOR = new Creator<ProductWithCoordinates>() {
        @Override
        public ProductWithCoordinates createFromParcel(Parcel in) {
            return new ProductWithCoordinates(in);
        }

        @Override
        public ProductWithCoordinates[] newArray(int size) {
            return new ProductWithCoordinates[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeDouble(firstCoordinate);
        dest.writeDouble(secondCoordinate);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public double getFirstCoordinate() {
        return firstCoordinate;
    }

    public double getSecondCoordinate() {
        return secondCoordinate;
    }
}