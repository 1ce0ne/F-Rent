package com.example.akkubattrent.TutorialClass;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.R;

public class GreetingWindowActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.greeting_window);

        ImageView imageView = findViewById(R.id.imageView);
        TextView textGreeting = findViewById(R.id.textGreeting);
        TextView textUpdate = findViewById(R.id.textViewUpdate);
        Button nextButton = findViewById(R.id.nextStepButton);

        // Анимация для логотипа
        Animation logoAnim = AnimationUtils.loadAnimation(this, R.anim.logo_move_up);
        imageView.startAnimation(logoAnim);

        // Анимация для текста (исчезновение)
        Animation textFadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out_tutorial);
        textGreeting.startAnimation(textFadeOut);
        textUpdate.startAnimation(textFadeOut);

        nextButton.setOnClickListener(v -> {
            Intent intent = new Intent(GreetingWindowActivity.this, HowAddCardWindowActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
            finish();
        });
    }
}