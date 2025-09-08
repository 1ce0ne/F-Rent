package com.example.akkubattrent.UserProfile;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.R;

public class FAQActivity extends AppCompatActivity {

    private LinearLayout currentlyExpanded = null;
    private Animation slideDown;
    private Animation slideUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);

        // Инициализируем анимации
        slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down_faq);
        slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_faq);
        
        setupFAQButton(R.id.profileForgotPassword, R.id.profileForgotPasswordInfo);
        setupFAQButton(R.id.howDeleteCard, R.id.howDeleteCardInfo);
        setupFAQButton(R.id.howOffDopTime, R.id.howOffDopTimeInfo);
        setupFAQButton(R.id.privatePolicy, R.id.privatePolicyInfo);
        setupFAQButton(R.id.aboutOfApp, R.id.aboutOfAppInfo);
        setupFAQButton(R.id.howBackProduct, R.id.howBackProductInfo);
        setupFAQButton(R.id.howBackProductInPostamat, R.id.howBackProductInPostamatInfo);
        setupFAQButton(R.id.paymentSposob, R.id.paymentSposobInfo);
        setupFAQButton(R.id.returnMoney, R.id.returnMoneyInfo);

        findViewById(R.id.backToProfileButton).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, R.anim.slide_out_right_signup); // ← Новое слева, текущее вправо
        });
    }

    private void setupFAQButton(int buttonId, int infoLayoutId) {
        View button = findViewById(buttonId);
        LinearLayout infoLayout = findViewById(infoLayoutId);

        // Оптимизация отображения текста
        TextView infoText = (TextView) infoLayout.getChildAt(0);
        infoText.setLineSpacing(1.1f, 1.1f);
        infoLayout.setPadding(
                (int)(12 * getResources().getDisplayMetrics().density),
                (int)(8 * getResources().getDisplayMetrics().density),
                (int)(12 * getResources().getDisplayMetrics().density),
                (int)(8 * getResources().getDisplayMetrics().density)
        );

        button.setOnClickListener(v -> {
            if (infoLayout.getVisibility() == View.VISIBLE) {
                // Закрываем текущую карточку
                collapseInfoLayout(infoLayout);
            } else {
                // Закрываем предыдущую открытую карточку (если есть)
                if (currentlyExpanded != null && currentlyExpanded != infoLayout) {
                    collapseInfoLayout(currentlyExpanded);
                }
                // Открываем новую карточку
                expandInfoLayout(infoLayout);
            }
        });
    }

    private void expandInfoLayout(final LinearLayout infoLayout) {
        infoLayout.setVisibility(View.VISIBLE);
        infoLayout.startAnimation(slideDown);
        currentlyExpanded = infoLayout;
    }

    private void collapseInfoLayout(final LinearLayout infoLayout) {
        Animation localSlideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_faq);
        localSlideUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                infoLayout.setVisibility(View.GONE);
                if (currentlyExpanded == infoLayout) {
                    currentlyExpanded = null;
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        infoLayout.startAnimation(localSlideUp);
    }
}