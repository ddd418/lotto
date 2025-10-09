# 🚀 Android Studio에서 직접 프로젝트 열기

현재 상황: Gradle 파일이 인식되지 않는 문제

## ✅ 해결 방법

### 방법 1: Android Studio에서 새 프로젝트 생성 (권장)

이 방법이 가장 확실합니다!

#### 1단계: 새 프로젝트 생성

1. Android Studio 실행
2. **File** → **New** → **New Project**
3. **Empty Activity** 선택 후 Next
4. 프로젝트 설정:
   - **Name**: `LottoApp`
   - **Package name**: `com.lotto.app`
   - **Save location**: 원하는 위치 (예: `C:\AndroidProjects\LottoApp`)
   - **Language**: `Kotlin`
   - **Minimum SDK**: `API 24`
   - **Build configuration**: `Kotlin DSL (build.gradle.kts)` 또는 `Groovy (build.gradle)`
5. **Finish** 클릭

#### 2단계: build.gradle 파일 수정

프로젝트가 생성되면 두 개의 build.gradle 파일이 있습니다:

**A. 프로젝트 레벨 build.gradle** (루트 폴더)

```groovy
// 기존 내용을 이것으로 교체
buildscript {
    ext {
        compose_version = '1.5.4'
        kotlin_version = '1.9.20'
    }
}

plugins {
    id 'com.android.application' version '8.1.4' apply false
    id 'com.android.library' version '8.1.4' apply false
    id 'org.jetbrains.kotlin.android' version '1.9.20' apply false
}
```

**B. 앱 모듈 build.gradle** (app 폴더 안)

```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.lotto.app'
    compileSdk 34

    defaultConfig {
        applicationId "com.lotto.app"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary true
        }
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
    }

    buildFeatures {
        compose true
    }

    composeOptions {
        kotlinCompilerExtensionVersion '1.5.4'
    }

    packaging {
        resources {
            excludes += '/META-INF/{AL2.0,LGPL2.1}'
        }
    }
}

dependencies {
    // Kotlin
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.6.2'

    // Compose
    implementation platform('androidx.compose:compose-bom:2023.10.01')
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.ui:ui-graphics'
    implementation 'androidx.compose.ui:ui-tooling-preview'
    implementation 'androidx.compose.material3:material3'
    implementation 'androidx.activity:activity-compose:1.8.1'

    // ViewModel
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2'
    implementation 'androidx.lifecycle:lifecycle-runtime-compose:2.6.2'

    // Retrofit (API 통신)
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'

    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'

    // Navigation
    implementation 'androidx.navigation:navigation-compose:2.7.5'

    // Icons Extended
    implementation 'androidx.compose.material:material-icons-extended:1.5.4'

    // Testing
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
    androidTestImplementation platform('androidx.compose:compose-bom:2023.10.01')
    androidTestImplementation 'androidx.compose.ui:ui-test-junit4'

    // Debug
    debugImplementation 'androidx.compose.ui:ui-tooling'
    debugImplementation 'androidx.compose.ui:ui-test-manifest'
}
```

#### 3단계: Gradle Sync

1. **File** → **Sync Project with Gradle Files** 클릭
2. 모든 의존성 다운로드 완료까지 대기

#### 4단계: Kotlin 파일 복사

`app/src/main/java/com/lotto/app/` 폴더 구조를 만들고 모든 .kt 파일들을 복사:

```
app/src/main/java/com/lotto/app/
├── MainActivity.kt
├── data/
│   ├── model/
│   │   └── Models.kt
│   ├── remote/
│   │   ├── LottoApiService.kt
│   │   └── RetrofitClient.kt
│   └── repository/
│       └── LottoRepository.kt
├── ui/
│   ├── components/
│   │   ├── LoadingDialog.kt
│   │   ├── LottoNumberBall.kt
│   │   └── LottoSetCard.kt
│   ├── screens/
│   │   ├── MainScreen.kt
│   │   ├── RecommendScreen.kt
│   │   └── StatsScreen.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── viewmodel/
    └── LottoViewModel.kt
```

#### 5단계: AndroidManifest.xml 수정

`app/src/main/AndroidManifest.xml` 파일을 다음과 같이 수정:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.LottoApp"
        android:usesCleartextTraffic="true">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.LottoApp">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

---

### 방법 2: 기존 폴더를 Android Studio에서 열기

1. Android Studio에서 **File** → **Open**
2. `c:\projects\lotto\android-app` 폴더 선택
3. **Trust Project** 클릭

하지만 이 방법은 폴더 구조가 완벽하지 않으면 실패할 수 있습니다.

---

## 🎯 체크리스트

완전한 Android 프로젝트가 되려면 다음 파일들이 필요합니다:

### 필수 Gradle 파일

- ✅ `settings.gradle` (생성됨)
- ✅ `gradle.properties` (생성됨)
- ✅ `build.gradle` (루트)
- ✅ `app/build.gradle` (앱 모듈)

### Gradle Wrapper (Android Studio가 자동 생성)

- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`

### 소스 코드

- ✅ `app/src/main/java/com/lotto/app/**/*.kt`
- ✅ `app/src/main/AndroidManifest.xml`
- ✅ `app/src/main/res/values/strings.xml`

---

## 💡 권장사항

**방법 1 (새 프로젝트 생성)을 강력히 권장합니다!**

이유:

- Android Studio가 모든 필수 파일을 자동 생성
- Gradle Wrapper 자동 설정
- 프로젝트 구조 보장
- 빌드 시스템 문제 최소화

시간: **약 10분**

---

## 🆘 여전히 안 되면?

1. **Android Studio 버전 확인**

   - 최신 버전 사용 권장 (2023.1 이상)

2. **JDK 확인**

   - File → Project Structure → SDK Location
   - JDK 17 이상 필요

3. **캐시 초기화**

   - File → Invalidate Caches / Restart

4. **Gradle 재다운로드**
   - 프로젝트 루트의 `.gradle` 폴더 삭제 후 Sync

---

새 프로젝트를 생성하고 파일들을 복사하는 것이 가장 확실한 방법입니다! 🚀
