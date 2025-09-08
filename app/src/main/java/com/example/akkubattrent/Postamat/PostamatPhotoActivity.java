package com.example.akkubattrent.Postamat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.akkubattrent.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PostamatPhotoActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageView productPhoto;
    private Button takePhotoButton;
    private Button toReviewButton;
    private String currentPhotoPath;
    private int productId;
    private int userId;
    private int postamatId;
    private String firstPhotoPath;
    private int cellId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postamat_photo);

        productPhoto = findViewById(R.id.productPhoto);
        takePhotoButton = findViewById(R.id.takePhotoButton);
        toReviewButton = findViewById(R.id.toReviewButton);

        // Получаем данные из предыдущей активности
        Intent intent = getIntent();
        productId = intent.getIntExtra("productId", -1);
        userId = intent.getIntExtra("userId", -1);
        postamatId = intent.getIntExtra("postamatId", -1);
        firstPhotoPath = intent.getStringExtra("photoPath");
        cellId = intent.getIntExtra("cellId", -1);

        takePhotoButton.setOnClickListener(v -> dispatchTakePictureIntent());

        toReviewButton.setOnClickListener(v -> {
            if (cellId == -1) {
                Toast.makeText(this, "Ошибка: не определена ячейка", Toast.LENGTH_SHORT).show();
                return;
            }

            // Переходим к ReviewActivity, передавая все необходимые данные
            Intent reviewIntent = new Intent(PostamatPhotoActivity.this, ReviewActivity.class);
            reviewIntent.putExtra("productId", productId);
            reviewIntent.putExtra("userId", userId);
            reviewIntent.putExtra("postamatId", postamatId);
            reviewIntent.putExtra("photoPath", firstPhotoPath);
            reviewIntent.putExtra("cellId", cellId); // Передаем ID ячейки
            startActivity(reviewIntent);
        });
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
                Bitmap photoBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
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
}