package com.example.akkubattrent.TutorialClass;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.R;

public class HowAddCardWindowActivityDouble extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.how_add_card_window);

        Button nextButton = findViewById(R.id.nextStepButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HowAddCardWindowActivityDouble.this, HowAddPassportDataActivityDouble.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right_login, 0);
                finish();
            }
        });
    }
}