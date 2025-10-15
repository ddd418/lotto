package com.lotto.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * AdMob 배너 광고 컴포넌트
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = "ca-app-pub-9417535388801609/6347505860" // 실제 프로덕션 광고 ID
) {
    val context = LocalContext.current
    
    // 디버깅 로그
    android.util.Log.d("BannerAdView", "=== 광고 렌더링 시작 ===")
    android.util.Log.d("BannerAdView", "Ad Unit ID: $adUnitId")
    
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        factory = { ctx ->
            android.util.Log.d("BannerAdView", "AdView 생성 중...")
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                
                // 광고 리스너 추가
                adListener = object : com.google.android.gms.ads.AdListener() {
                    override fun onAdLoaded() {
                        android.util.Log.d("BannerAdView", "✅ 광고 로드 성공!")
                    }
                    
                    override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                        android.util.Log.e("BannerAdView", "❌ 광고 로드 실패: ${error.message}")
                        android.util.Log.e("BannerAdView", "Error Code: ${error.code}")
                        android.util.Log.e("BannerAdView", "Error Domain: ${error.domain}")
                    }
                    
                    override fun onAdOpened() {
                        android.util.Log.d("BannerAdView", "광고 클릭됨")
                    }
                }
                
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            android.util.Log.d("BannerAdView", "AdView 업데이트 - 광고 재로드")
            adView.loadAd(AdRequest.Builder().build())
        }
    )
}

/**
 * 무료 사용자에게만 표시되는 배너 광고
 */
@Composable
fun ConditionalBannerAd(
    showAd: Boolean,
    modifier: Modifier = Modifier
) {
    if (showAd) {
        BannerAdView(modifier = modifier)
    }
}
