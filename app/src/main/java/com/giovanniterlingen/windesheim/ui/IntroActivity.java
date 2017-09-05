package com.giovanniterlingen.windesheim.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;

import com.giovanniterlingen.windesheim.R;
import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.github.paolorotolo.appintro.model.SliderPage;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class IntroActivity extends AppIntro {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SliderPage sliderPage1 = new SliderPage();
        sliderPage1.setBgColor(ContextCompat.getColor(IntroActivity.this, R.color.colorPrimary));
        sliderPage1.setTitle(getResources().getString(R.string.intro_1_title));
        sliderPage1.setDescription(getResources().getString(R.string.intro_1_description));
        sliderPage1.setImageDrawable(R.drawable.intro_1);
        addSlide(AppIntroFragment.newInstance(sliderPage1));

        SliderPage sliderPage2 = new SliderPage();
        sliderPage2.setBgColor(ContextCompat.getColor(IntroActivity.this, R.color.colorPrimary));
        sliderPage2.setTitle(getResources().getString(R.string.intro_2_title));
        sliderPage2.setDescription(getResources().getString(R.string.intro_2_description));
        sliderPage2.setImageDrawable(R.drawable.intro_2);
        addSlide(AppIntroFragment.newInstance(sliderPage2));

        SliderPage sliderPage3 = new SliderPage();
        sliderPage3.setBgColor(ContextCompat.getColor(IntroActivity.this, R.color.colorPrimary));
        sliderPage3.setTitle(getResources().getString(R.string.intro_3_title));
        sliderPage3.setDescription(getResources().getString(R.string.intro_3_description));
        sliderPage3.setImageDrawable(R.drawable.intro_3);
        addSlide(AppIntroFragment.newInstance(sliderPage3));

        SliderPage sliderPage4 = new SliderPage();
        sliderPage4.setBgColor(ContextCompat.getColor(IntroActivity.this, R.color.colorPrimary));
        sliderPage4.setTitle(getResources().getString(R.string.intro_4_title));
        sliderPage4.setDescription(getResources().getString(R.string.intro_4_description));
        sliderPage4.setImageDrawable(R.drawable.intro_4);
        addSlide(AppIntroFragment.newInstance(sliderPage4));

        SliderPage sliderPage5 = new SliderPage();
        sliderPage5.setBgColor(ContextCompat.getColor(IntroActivity.this, R.color.colorPrimary));
        sliderPage5.setTitle(getResources().getString(R.string.intro_5_title));
        sliderPage5.setDescription(getResources().getString(R.string.intro_5_description));
        sliderPage5.setImageDrawable(R.drawable.intro_5);
        addSlide(AppIntroFragment.newInstance(sliderPage5));

        SliderPage sliderPage6 = new SliderPage();
        sliderPage6.setBgColor(ContextCompat.getColor(IntroActivity.this, R.color.colorPrimary));
        sliderPage6.setTitle(getResources().getString(R.string.intro_6_title));
        sliderPage6.setDescription(getResources().getString(R.string.intro_6_description));
        sliderPage6.setImageDrawable(R.drawable.intro_6);
        addSlide(AppIntroFragment.newInstance(sliderPage6));

        SliderPage sliderPage7 = new SliderPage();
        sliderPage7.setBgColor(ContextCompat.getColor(IntroActivity.this, R.color.colorPrimary));
        sliderPage7.setTitle(getResources().getString(R.string.intro_7_title));
        sliderPage7.setDescription(getResources().getString(R.string.intro_7_description));
        sliderPage7.setImageDrawable(R.drawable.intro_7);
        addSlide(AppIntroFragment.newInstance(sliderPage7));

        showSkipButton(false);
        setProgressButtonEnabled(true);

        setVibrate(true);
        setVibrateIntensity(30);
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);

        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(IntroActivity.this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("intro_finished", true);
        editor.commit();

        Intent intent = new Intent(IntroActivity.this, LaunchActivity.class);
        startActivity(intent);
        finish();
    }
}