package com.example.f_rent;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.f_rent.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class ChoosePostamatMain extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private MapView mapView;
    private FrameLayout loginWithPhone;
    private TextView textLoginWithPhone;
    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));
        setContentView(R.layout.activity_choose_nearby_postamat);

        initViews();
        setupClickListeners();
        setupMap();
        addPostamatMarker(); // Добавляем маркер постамата ДО получения геолокации
        checkLocationPermission();
    }

    private void initViews() {
        mapView = findViewById(R.id.mapView);
        loginWithPhone = findViewById(R.id.login_with_phone);
        textLoginWithPhone = findViewById(R.id.textLoginWithPhone);
    }

    private void setupClickListeners() {
        loginWithPhone.setOnClickListener(v -> handleContinueButtonClick());
        textLoginWithPhone.setOnClickListener(v -> handleContinueButtonClick());
    }

    private void handleContinueButtonClick() {
        // TODO: добавить логику
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);
        mapView.getController().setZoom(15.0);
        // Центрируем карту на постамат, а не на Москву
        mapView.getController().setCenter(new GeoPoint(44.555364, 34.314448));
    }

    private void addPostamatMarker() {
        GeoPoint postamatPoint = new GeoPoint(44.555364, 34.314448);
        Marker marker = new Marker(mapView);
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.pinmark_for_postamat);
        if (drawable != null) {
            int widthPx = dpToPx(45);
            int heightPx = dpToPx(45);
            Bitmap bitmap = getBitmapFromDrawable(drawable, widthPx, heightPx);
            marker.setIcon(new BitmapDrawable(getResources(), bitmap));
        }
        marker.setPosition(postamatPoint);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(marker);
        mapView.invalidate();

        // Дополнительно: анимируем к постамату чтобы убедиться что он виден
        mapView.getController().animateTo(postamatPoint);
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                updateMapWithLocation(location);
                locationManager.removeUpdates(locationListener);
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                Toast.makeText(ChoosePostamatMain.this, "Пожалуйста, включите GPS", Toast.LENGTH_LONG).show();
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }
    }

    private void updateMapWithLocation(Location location) {
        if (location != null) {
            GeoPoint userLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
            mapView.getController().animateTo(userLocation);
            mapView.getController().setZoom(16.0);

            Marker marker = new Marker(mapView);
            Drawable userIcon = ContextCompat.getDrawable(this, R.drawable.geolocation);
            if (userIcon != null) {
                int widthPx = dpToPx(67);
                int heightPx = dpToPx(75);
                Bitmap bitmap = getBitmapFromDrawable(userIcon, widthPx, heightPx);
                marker.setIcon(new BitmapDrawable(getResources(), bitmap));
            }
            marker.setPosition(userLocation);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle("Вы здесь");

            // Не очищаем все оверлеи, а просто добавляем новый
            mapView.getOverlays().add(marker);
            mapView.invalidate();
        }
    }

    private Bitmap getBitmapFromDrawable(Drawable drawable, int widthPx, int heightPx) {
        Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private int dpToPx(int dp) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        return Math.round(dp * (metrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            Toast.makeText(this, "Разрешение на доступ к геолокации необходимо для работы карты", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDetach();
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }
}