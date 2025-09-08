package com.example.akkubattrent.AllCategories;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.MainMenuActivity;
import com.example.akkubattrent.R;

public class RouteToPostamat extends AppCompatActivity {

    private ImageView productImage;
    private TextView productName;
    private TextView productDescription;
    private Button createRouteButton;
    private Button closePostamatButton;
    private Button hourButton;
    private Button dayButton;
    private Button monthButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route_to_postamat);

        productImage = findViewById(R.id.productImage);
        productName = findViewById(R.id.productName);
        productDescription = findViewById(R.id.productDescription);
        createRouteButton = findViewById(R.id.createRoute);
        closePostamatButton = findViewById(R.id.closePostamatButton);
        hourButton = findViewById(R.id.hourButton);
        dayButton = findViewById(R.id.dayButton);
        monthButton = findViewById(R.id.monthButton);

        Intent intent = getIntent();
        String name = intent.getStringExtra("productName");
        String description = intent.getStringExtra("productDescription");

        double priceHour = intent.getDoubleExtra("price_per_hour", 0.0);
        double priceDay = intent.getDoubleExtra("price_per_day", 0.0);
        double priceMonth = intent.getDoubleExtra("price_per_month", 0.0);
        byte[] imageBytes = intent.getByteArrayExtra("productImage");
        int productId = intent.getIntExtra("productId", -1);
        double firstCoordinate = intent.getDoubleExtra("firstCoordinate", 0.0);
        double secondCoordinate = intent.getDoubleExtra("secondCoordinate", 0.0);


        productName.setText(name);
        productDescription.setText(description);

        if (imageBytes != null && imageBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            productImage.setImageBitmap(bitmap);
        } else {
            productImage.setImageResource(R.drawable.placeholder_image);
        }

        hourButton.setText(String.format("1 час\n%.0f ₽", priceHour));
        dayButton.setText(String.format("1 день\n%.0f ₽", priceDay));
        monthButton.setText(String.format("1 месяц\n%.0f ₽", priceMonth));

        hourButton.setEnabled(false);
        dayButton.setEnabled(false);
        monthButton.setEnabled(false);

        createRouteButton.setOnClickListener(v -> {
            Intent routeIntent = new Intent(RouteToPostamat.this, MainMenuActivity.class);
            routeIntent.putExtra("targetLatitude", firstCoordinate);
            routeIntent.putExtra("targetLongitude", secondCoordinate);
            routeIntent.putExtra("productName", name);
            startActivity(routeIntent);
        });

        closePostamatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(0, R.anim.slide_out_left); // ← Новое слева, текущее вправо

            }
        });
    }
}