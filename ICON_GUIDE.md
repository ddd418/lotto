# 🎨 로또 앱 아이콘 및 카카오톡 공유 이미지 생성 가이드

## 📋 목차

1. [앱 아이콤 생성](#1-앱-아이콘-생성)
2. [카카오톡 공유 이미지 생성](#2-카카오톡-공유-이미지-생성)
3. [이미지 배치](#3-이미지-배치)
4. [서버 설정](#4-서버-설정)

---

## 1. 앱 아이콘 생성

### 🤖 AI 이미지 생성 프롬프트

**영문 프롬프트 (DALL-E 3, Midjourney 등):**

```
A cute, friendly AI robot with big expressive eyes scratching a lottery ticket
with a shiny gold coin, looking excited and hopeful. The robot has a round
metallic blue head with a small antenna, holding a golden lottery ticket with
visible numbers (6, 9, 16, 19, 24, 28). Bright, cheerful colors (blue robot,
gold ticket, white background), modern flat design style, minimalist, perfect
for app icon, centered composition, clean white or transparent background,
playful and optimistic atmosphere, digital illustration, high quality
```

**한글 프롬프트 (국내 AI 도구):**

```
귀여운 AI 로봇이 동전으로 로또 복권을 긁고 있는 모습. 로봇은 파란색 금속 재질의
둥근 머리에 작은 안테나가 있고, 큰 눈망울로 행복하게 웃고 있음. 금색 로또 용지에는
숫자(6, 9, 16, 19, 24, 28)가 보임. 밝고 명랑한 색상(파란색, 금색, 흰색),
플랫 디자인, 미니멀리스트 스타일, 앱 아이콘용, 정중앙 배치,
깔끔한 흰색 또는 투명 배경, 즐겁고 낙천적인 분위기, 고품질 디지털 일러스트
```

### 📐 생성 사이즈

- **기본**: 1024x1024px (정사각형, 고해상도)
- **배경**: 투명 PNG 또는 단색

### 🔧 리사이징 도구

**온라인 도구:**

- [Android Asset Studio](https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html)
  - 1024x1024 이미지 업로드
  - 자동으로 모든 해상도 생성
  - ZIP 다운로드 후 압축 해제

**수동 리사이징 (필요시):**

- `mipmap-mdpi`: 48x48px
- `mipmap-hdpi`: 72x72px
- `mipmap-xhdpi`: 96x96px
- `mipmap-xxhdpi`: 144x144px
- `mipmap-xxxhdpi`: 192x192px

### 📁 배치 위치

```
android-app/app/src/main/res/
├── mipmap-mdpi/ic_launcher.png (48x48)
├── mipmap-hdpi/ic_launcher.png (72x72)
├── mipmap-xhdpi/ic_launcher.png (96x96)
├── mipmap-xxhdpi/ic_launcher.png (144x144)
└── mipmap-xxxhdpi/ic_launcher.png (192x192)
```

---

## 2. 카카오톡 공유 이미지 생성

### 🤖 AI 이미지 생성 프롬프트

**영문 프롬프트:**

```
A cheerful AI robot mascot enthusiastically scratching lottery tickets with
a gold coin, surrounded by floating colorful lottery balls with numbers
(1-45), golden coins raining down, and lucky four-leaf clovers. The robot
has a happy, excited expression with sparkles and shine effects around it.
Vibrant gradient background (blue to purple), modern flat design style,
wide banner format 2:1 ratio (800x400px), festive and lucky atmosphere,
space at top for text overlay "로또 AI 추천", playful and engaging
illustration for mobile sharing, high quality digital art
```

**한글 프롬프트:**

```
행복한 AI 로봇 마스코트가 금화로 로또 복권을 신나게 긁고 있고, 주변에
알록달록한 번호 공들(1-45)이 떠다니며, 금화가 비처럼 내리고, 네잎클로버가
있는 장면. 로봇은 매우 흥분하고 행복한 표情으로 반짝이는 효과와 함께 있음.
밝고 화려한 그라데이션 배경(파랑에서 보라), 현대적 플랫 디자인,
가로 배너 형식 2:1 비율(800x400px), 축제 같고 행운의 분위기,
상단에 "로또 AI 추천" 텍스트 공간, 모바일 공유용 즐겁고 매력적인 일러스트,
고품질 디지털 아트
```

### 📐 생성 사이즈

- **크기**: 800x400px (2:1 비율)
- **형식**: PNG 또는 JPG
- **용량**: 500KB 이하 권장 (카카오톡 최적화)

### 🎨 디자인 구성

```
┌─────────────────────────────────────┐
│ 상단 30%: 텍스트 공간                │
│ "로또 AI 추천 🤖" (선택사항)         │
├─────────────────────────────────────┤
│ 중앙 50%: 메인 캐릭터                │
│ AI 로봇 + 복권 + 번호 공들           │
├─────────────────────────────────────┤
│ 하단 20%: 장식 요소                  │
│ 동전, 별, 반짝임, 클로버             │
└─────────────────────────────────────┘
```

### 📁 배치 위치

```
lotto/static/kakao_share_image.png
```

---

## 3. 이미지 배치

### ✅ 체크리스트

**앱 아이콘:**

- [ ] 1024x1024 원본 이미지 생성 완료
- [ ] Android Asset Studio로 리사이징 완료
- [ ] 5개 해상도 파일을 `mipmap-*` 폴더에 배치
- [ ] 기존 `ic_launcher.png` 파일 백업 (선택사항)
- [ ] 새 아이콘으로 교체 완료

**카카오톡 공유 이미지:**

- [ ] 800x400 이미지 생성 완료
- [ ] 용량 확인 (500KB 이하)
- [ ] `lotto/static/` 폴더에 `kakao_share_image.png`로 저장
- [ ] 서버에서 접근 가능한지 테스트

---

## 4. 서버 설정

### 🖼️ 이미지 URL 설정

1. **로컬 테스트:**

   ```
   http://localhost:8000/kakao-share-image
   ```

2. **실제 서버 (공유 시 필수):**

   - `RecommendScreen.kt` 파일 열기
   - 400번째 줄 근처의 `imageUrl` 찾기
   - `http://your-server-ip:8000/kakao-share-image` 부분을
   - 실제 서버 IP 또는 도메인으로 변경

   **예시:**

   ```kotlin
   imageUrl = "http://123.456.789.012:8000/kakao-share-image"
   // 또는
   imageUrl = "https://yourdomain.com/kakao-share-image"
   ```

### 🧪 테스트 방법

1. **이미지 접근 테스트:**

   ```bash
   # 웹 브라우저에서
   http://your-server-ip:8000/kakao-share-image

   # 또는 curl
   curl -I http://your-server-ip:8000/kakao-share-image
   ```

2. **앱에서 테스트:**
   - 앱 빌드 및 설치
   - 번호 추천 화면에서 카카오톡 공유 버튼 클릭
   - 이미지가 정상적으로 표시되는지 확인

---

## 🎯 AI 이미지 생성 도구 추천

### 무료/저렴한 옵션:

1. **Leonardo AI** (무료 크레딧 제공)

   - https://leonardo.ai
   - 하루 150 크레딧 무료

2. **Bing Image Creator** (무료)

   - https://www.bing.com/create
   - DALL-E 3 기반

3. **Adobe Firefly** (무료 체험)
   - https://firefly.adobe.com

### 고품질 옵션:

1. **Midjourney** (유료, $10/월~)

   - 최고 품질
   - Discord 기반

2. **DALL-E 3** (유료)
   - ChatGPT Plus ($20/월)
   - OpenAI API

---

## 💡 팁

### 앱 아이콘:

- ✅ **DO**: 심플하고 인식하기 쉬운 디자인
- ✅ **DO**: 밝고 긍정적인 색상 사용
- ✅ **DO**: 작은 크기에서도 잘 보이는 디자인
- ❌ **DON'T**: 너무 복잡한 디테일
- ❌ **DON'T**: 가독성 낮은 텍스트

### 카카오톡 공유 이미지:

- ✅ **DO**: 눈에 띄는 메인 요소 (로봇)
- ✅ **DO**: 텍스트 추가 공간 확보
- ✅ **DO**: 모바일 화면에서 잘 보이는 크기
- ❌ **DON'T**: 너무 작은 글씨나 요소
- ❌ **DON'T**: 과도한 정보 밀집

---

## 🚀 다음 단계

이미지 생성 및 배치 완료 후:

1. **앱 아이콘 확인**

   ```bash
   cd C:\projects\lotto\android-app
   .\gradlew clean
   .\gradlew assembleDebug
   ```

2. **APK 설치 후 홈 화면에서 아이콘 확인**

3. **카카오톡 공유 테스트**

   - 앱에서 번호 추천
   - 카카오톡 공유 버튼 클릭
   - 이미지 및 텍스트 확인

4. **문제 발생 시**
   - 서버 로그 확인: `uvicorn api_server:app --reload`
   - 이미지 URL 다시 확인
   - 파일 권한 확인

---

## 📞 도움이 필요하면

- 앱 아이콘이 안 보이면: 캐시 삭제 후 재설치
- 카카오톡 이미지 안 보이면: 서버 URL 및 파일 경로 확인
- 빌드 에러: `.\gradlew clean` 후 재빌드

**행운을 빕니다! 🍀**
