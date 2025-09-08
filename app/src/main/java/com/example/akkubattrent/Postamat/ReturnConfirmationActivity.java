package com.example.akkubattrent.Postamat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.akkubattrent.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ReturnConfirmationActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageView productPhoto;
    private Button takePhotoButton;
    private Button toReviewButton;
    private Bitmap photoBitmap;
    private int productId;
    private int userId;
    private int postamatId;
    private String currentPhotoPath;
    private boolean isProcessing = false;
    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return_confirmation_first);

        productPhoto = findViewById(R.id.productPhoto);
        takePhotoButton = findViewById(R.id.takePhotoButton);
        toReviewButton = findViewById(R.id.toReviewButton);

        Intent intent = getIntent();
        productId = intent.getIntExtra("productId", -1);
        userId = intent.getIntExtra("userId", -1);
        postamatId = intent.getIntExtra("postamatId", -1);

        if (productId == -1 || userId == -1 || postamatId == -1) {
            Log.e("ReturnConfirmationActivity", "productId, userId или postamatId не передан или равен -1");
        }

        takePhotoButton.setOnClickListener(v -> dispatchTakePictureIntent());

        toReviewButton.setOnClickListener(v -> {
            if (isProcessing) {
                Toast.makeText(ReturnConfirmationActivity.this, "Действие уже выполняется", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoBitmap == null) {
                Toast.makeText(ReturnConfirmationActivity.this, "Сфотографируйте товар", Toast.LENGTH_SHORT).show();
                return;
            }

            showLoadingDialog();
            isProcessing = true;

            new Handler().postDelayed(() -> {
                Bitmap compressedBitmap = compressImage(photoBitmap);

                // Изменено: теперь переходим в OpenPostamatActivity вместо ReviewActivity
                Intent openPostamatIntent = new Intent(ReturnConfirmationActivity.this, OpenPostamatActivity.class);
                openPostamatIntent.putExtra("productId", productId);
                openPostamatIntent.putExtra("userId", userId);
                openPostamatIntent.putExtra("postamatId", postamatId);
                openPostamatIntent.putExtra("photoPath", currentPhotoPath);

                dismissLoadingDialog();
                startActivity(openPostamatIntent);
                isProcessing = false;
            }, 2000);
        });
    }

    // Остальные методы остаются без изменений
    private void showLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        loadingDialog = builder.create();
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        loadingDialog.show();
    }

    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.akkubattrent.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            File imgFile = new File(currentPhotoPath);
            if (imgFile.exists()) {
                photoBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                productPhoto.setImageBitmap(photoBitmap);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private Bitmap compressImage(Bitmap image) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int quality = 100;
        image.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);

        while (outputStream.toByteArray().length > 1024 * 1024 && quality > 0) {
            outputStream.reset();
            quality -= 5;
            image.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
        }

        byte[] compressedImageBytes = outputStream.toByteArray();
        return BitmapFactory.decodeByteArray(compressedImageBytes, 0, compressedImageBytes.length);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissLoadingDialog();
    }
}