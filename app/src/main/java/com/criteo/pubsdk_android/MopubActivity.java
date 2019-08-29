package com.criteo.pubsdk_android;

import static com.mopub.common.logging.MoPubLog.LogLevel.DEBUG;
import static com.mopub.common.logging.MoPubLog.LogLevel.INFO;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import com.criteo.publisher.Criteo;
import com.criteo.publisher.model.AdSize;
import com.criteo.publisher.model.BannerAdUnit;
import com.criteo.publisher.model.InterstitialAdUnit;
import com.mopub.common.MoPub;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.SdkInitializationListener;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;
import com.mopub.mobileads.MoPubInterstitial.InterstitialAdListener;
import com.mopub.mobileads.MoPubView;

public class MopubActivity extends AppCompatActivity {

    private static final String TAG = MopubActivity.class.getSimpleName();
    private static final String SDK_BUILD_ID = "b195f8dd8ded45fe847ad89ed1d016da";
    public static final String MOPUB_BANNER_ADUNIT_ID_HB = "d2f3ed80e5da4ae1acde0971eac30fa4";
    public static final String MOPUB_INTERSTITIAL_ADUNIT_ID_HB = "83a2996696284da881edaf1a480e5d7c";
    private MoPubView publisherAdView;
    private MoPubInterstitial mInterstitial;
    private LinearLayout linearLayout;
    private Criteo criteo;
    private InterstitialAdListener interstitialAdListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mopub);
        criteo = Criteo.getInstance();
        createInterstitialAdListener();

        final SdkConfiguration.Builder configBuilder = new SdkConfiguration.Builder(SDK_BUILD_ID);
        if (BuildConfig.DEBUG) {
            configBuilder.withLogLevel(DEBUG);
        } else {
            configBuilder.withLogLevel(INFO);
        }
        MoPub.initializeSdk(this, configBuilder.build(), new SdkInitializationListener() {
            @Override
            public void onInitializationFinished() {
                Log.d(TAG, "Mopub initialization completed");
            }
        });

        linearLayout = ((LinearLayout) findViewById(R.id.adViewHolder));
        findViewById(R.id.buttonBanner).setOnClickListener((View v) -> {
            onBannerClick();
        });

        findViewById(R.id.buttonInterstitial).setOnClickListener((View v) -> {
            onInterstitialClick();
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void createInterstitialAdListener() {
        interstitialAdListener = new InterstitialAdListener() {
            @Override
            public void onInterstitialLoaded(MoPubInterstitial interstitial) {
                Log.d(TAG, "Mopub ad loaded");
                mInterstitial.show();

            }

            @Override
            public void onInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode errorCode) {
                // Code to be executed when an ad request fails
                Log.d(TAG, "Mopub ad failed:" + errorCode);
            }

            @Override
            public void onInterstitialShown(MoPubInterstitial interstitial) {
                Log.d("TAG", "ad shown");
            }

            @Override
            public void onInterstitialClicked(MoPubInterstitial interstitial) {
                Log.d(TAG, "Mopub ad clicked");

            }

            @Override
            public void onInterstitialDismissed(MoPubInterstitial interstitial) {
                Log.d(TAG, "Mopub ad dismissed");

            }
        };
    }

    private void onBannerClick() {
        BannerAdUnit moPub = new BannerAdUnit(MOPUB_BANNER_ADUNIT_ID_HB, new AdSize(320, 50));
        linearLayout.setBackgroundColor(Color.RED);
        linearLayout.removeAllViews();
        linearLayout.setVisibility(View.VISIBLE);
        publisherAdView = new MoPubView(this);
        criteo.setBidsForAdUnit(publisherAdView, moPub);
        publisherAdView.setAdUnitId(MOPUB_BANNER_ADUNIT_ID_HB);
        publisherAdView.loadAd();
        linearLayout.addView(publisherAdView);
    }

    private void onInterstitialClick() {
        InterstitialAdUnit moPubInterstitialAdUnit = new InterstitialAdUnit(
                MOPUB_INTERSTITIAL_ADUNIT_ID_HB);
        mInterstitial = new MoPubInterstitial(this, MOPUB_INTERSTITIAL_ADUNIT_ID_HB);
        criteo.setBidsForAdUnit(mInterstitial, moPubInterstitialAdUnit);
        mInterstitial.setInterstitialAdListener(interstitialAdListener);
        mInterstitial.load();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (publisherAdView != null) {
            publisherAdView.destroy();
        }
    }
}
