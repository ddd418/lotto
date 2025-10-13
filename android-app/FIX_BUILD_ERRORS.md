# 🔧 Android 앱 빌드 오류 수정 가이드

## 📋 발생한 오류

### 1. SubscriptionApiService import 오류

```
Unresolved reference: SubscriptionApiService
```

**원인:** SubscriptionApiService.kt에서 import 경로가 잘못됨
**해결:** ✅ 수정 완료

### 2. FeatureItem 함수 중복 정의

```
Conflicting overloads: public fun FeatureItem ... private fun FeatureItem
```

**원인:** LoginScreen.kt와 OnboardingScreen.kt 양쪽에 FeatureItem 함수가 정의됨
**해결:** ⏳ 수정 필요

---

## ✅ 수정 방법

### 방법 1: PowerShell 스크립트 실행 (추천)

```powershell
cd c:\projects\lotto\android-app

# FeatureItem 함수 제거 스크립트 실행
.\remove-feature-item.ps1

# 빌드 실행
.\gradlew assembleDebug
```

### 방법 2: 수동 수정

**파일:** `app/src/main/java/com/lotto/app/ui/screens/LoginScreen.kt`

파일 끝부분에 있는 다음 코드를 **삭제**하세요:

```kotlin
/**
 * 기능 소개 아이템
 */
@Composable
private fun FeatureItem(
    icon: String,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = icon,
            fontSize = 20.sp
        )

        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = NotionColors.TextPrimary
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = NotionColors.TextSecondary
            )
        }
    }
}
```

**LoginScreen.kt 파일이 다음과 같이 끝나야 합니다:**

```kotlin
                }
                else -> {}
            }
        }
    }
}
```

---

## 🚀 수정 후 빌드

```powershell
cd c:\projects\lotto\android-app

.\gradlew assembleDebug
```

---

## 📊 수정된 파일 목록

1. ✅ `SubscriptionApiService.kt` - import 경로 수정
2. ⏳ `LoginScreen.kt` - FeatureItem 함수 제거 필요

---

## 🎯 다음 단계

### 1. 빌드 성공 확인

```powershell
.\gradlew assembleDebug
```

### 2. APK 설치

```powershell
.\gradlew installDebug
```

### 3. 앱 실행 테스트

- 온보딩 화면 확인
- 무료 체험 시작
- PRO 구독 화면 확인
- 배너 광고 표시 확인

---

## 💡 왜 이런 오류가 발생했나요?

### FeatureItem 중복 문제

- **OnboardingScreen.kt**: `public fun FeatureItem` (전역 함수)
- **LoginScreen.kt**: `private fun FeatureItem` (파일 내부 함수)

Kotlin은 같은 패키지 내에서 **함수 오버로딩 충돌**을 감지합니다.

**해결 방법:**

1. LoginScreen의 private 함수 삭제 → OnboardingScreen의 public 함수 사용
2. 또는 두 함수 모두 private로 변경

우리는 **방법 1**을 선택했습니다 (코드 중복 제거).

---

## ✅ 최종 확인 체크리스트

빌드 성공 후:

- [ ] 온보딩 화면 표시
- [ ] "30일 무료 체험" 버튼 동작
- [ ] "PRO 구독하기" 버튼 동작
- [ ] 배너 광고 표시 (하단)
- [ ] 로그인 화면 표시
- [ ] 추천 화면 표시
- [ ] 통계 화면 표시

---

**지금 바로 `remove-feature-item.ps1` 스크립트를 실행하세요!** 🚀
