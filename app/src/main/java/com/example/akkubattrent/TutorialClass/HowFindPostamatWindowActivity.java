package com.example.akkubattrent.TutorialClass;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.R;

public class HowFindPostamatWindowActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.how_find_postamat_window);

        Button nextButton = findViewById(R.id.findPostamatsButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HowFindPostamatWindowActivity.this, HowRentProductWindowActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right_login, 0);
                finish();
            }
        });
    }
}