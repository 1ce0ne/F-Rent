package com.example.f_rent.EntranceInApp;

import android.Manifest;
import android.content.Intent;
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.f_rent.MainActivity;
import com.example.f_rent.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class ChoosePostamatNearby extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private MapView mapView;
    private FrameLayout loginWithPhone;
    private TextView textLoginWithPhone;
    private LocationManager locationManager;
    private LocationListener locationListener;

    private ImageView buttonBack;

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
        buttonBack = findViewById(R.id.buttonBack);
    }

    private void setupClickListeners() {
        loginWithPhone.setOnClickListener(v -> handleContinueButtonClick());
        textLoginWithPhone.setOnClickListener(v -> handleContinueButtonClick());

        // Установка обработчика клика для кнопки назад
        if (buttonBack != null) {
            buttonBack.setOnClickListener(v -> {
                finish();
                overridePendingTransition(0, R.anim.slide_out_right_signup);
            });
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_right_signup);
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

        // Добавляем обработчик нажатия на маркер
        marker.setOnMarkerClickListener((marker1, mapView) -> {
            showPostamatDialog();
            return true;
        });

        mapView.getOverlays().add(marker);
        mapView.invalidate();

        // Дополнительно: анимируем к постамату чтобы убедиться что он виден
        mapView.getController().animateTo(postamatPoint);
    }

    private void showPostamatDialog() {
        // Создаем диалоговое окно
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.activity_podt_postamat, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // Находим элементы в диалоге
        FrameLayout frameLayout3 = dialogView.findViewById(R.id.frameLayout3);
        TextView textView9 = dialogView.findViewById(R.id.textView9);
        ImageView krestYobannyi = dialogView.findViewById(R.id.krestYobannyi);

        // Устанавливаем обработчики нажатий
        View.OnClickListener goToMainActivityListener = v -> {
            // Сохраняем user_login = true в user_prefs
            saveUserLoginStatus();

            // Переход к MainActivity
            Intent intent = new Intent(ChoosePostamatNearby.this, MainActivity.class);
            startActivity(intent);
            // Добавляем анимацию перехода
            overridePendingTransition(0, R.anim.slide_out_right_signup);
            dialog.dismiss();
            finish(); // Закрываем текущую активность
        };

        frameLayout3.setOnClickListener(goToMainActivityListener);
        textView9.setOnClickListener(goToMainActivityListener);

        krestYobannyi.setOnClickListener(v -> {
            // Закрываем диалог
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveUserLoginStatus() {
        // Получаем SharedPreferences с именем "user_prefs"
        android.content.SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = sharedPreferences.edit();

        // Сохраняем user_login = true
        editor.putBoolean("user_login", true);
        editor.apply(); // или editor.commit() для синхронного сохранения
//        Toast.makeText(this, "Данные сохранены", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(ChoosePostamatNearby.this, "Пожалуйста, включите GPS", Toast.LENGTH_LONG).show();
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