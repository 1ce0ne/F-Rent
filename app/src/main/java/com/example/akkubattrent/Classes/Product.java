package com.example.akkubattrent.Classes;

import android.os.Parcel;
import android.os.Parcelable;

public class Product implements Parcelable {
    private int id;
    private String name;
    private String description;
    private double price_per_hour;
    private double price_per_day;
    private double price_per_month;
    private byte[] image;
    private String productUuid;
    private String address; // Добавлено поле address
    private int cellId;

    private boolean isReserved;

    public Product(int id, String name, String description, double price_per_hour, double price_per_day,
                   double price_per_month, byte[] image, String productUuid, String address, int cellId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price_per_hour = price_per_hour;
        this.price_per_day = price_per_day;
        this.price_per_month = price_per_month;
        this.image = image;
        this.productUuid = productUuid;
        this.address = address;
        this.cellId = cellId;
    }

    public Product(int id, String name, byte[] image) {
        this.id = id;
        this.name = name;
        this.image = image;
    }

    // Геттер для address
    public String getAddress() {
        return address;
    }

    public String getSize() {
        return address; // Для обратной совместимости, если где-то используется getSize()
    }

    public int getCellsId() {
        return cellId;
    }

    protected Product(Parcel in) {
        id = in.readInt();
        name = in.readString();
        description = in.readString();
        price_per_hour = in.readDouble();
        price_per_day = in.readDouble();
        price_per_month = in.readDouble();
        image = in.createByteArray();
        productUuid = in.readString();
        address = in.readString();
        cellId = in.readInt();
    }

    public static final Creator<Product> CREATOR = new Creator<Product>() {
        @Override
        public Product createFromParcel(Parcel in) {
            return new Product(in);
        }

        @Override
        public Product[] newArray(int size) {
            return new Product[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeDouble(price_per_hour);
        dest.writeDouble(price_per_day);
        dest.writeDouble(price_per_month);
        dest.writeByteArray(image);
        dest.writeString(productUuid);
        dest.writeString(address);
        dest.writeInt(cellId);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getPriceHour() {
        return price_per_hour;
    }

    public double getPriceDay() {
        return price_per_day;
    }

    public double getPriceMonth() {
        return price_per_month;
    }

    public byte[] getImage() {
        return image;
    }

    public String getProductUuid() {
        return productUuid;
    }

    public boolean isReserved() {
        return isReserved;
    }

    public void setReserved(boolean reserved) {
        isReserved = reserved;
    }
}