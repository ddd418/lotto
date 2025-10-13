# Android 앱 빌드 오류 수정 스크립트

Write-Host "🔧 Android 앱 빌드 오류 수정 중..." -ForegroundColor Cyan

# 1. LoginScreen.kt에서 FeatureItem 함수 제거
Write-Host "`n1. LoginScreen.kt에서 중복된 FeatureItem 함수 제거..." -ForegroundColor Yellow

$loginScreenPath = "app\src\main\java\com\lotto\app\ui\screens\LoginScreen.kt"
$content = Get-Content $loginScreenPath -Raw

# FeatureItem 함수 정의 부분 제거 (주석 포함)
$pattern = '(?s)\r?\n/\*\*\r?\n \* 기능 소개 아이템\r?\n \*/\r?\n@Composable\r?\nprivate fun FeatureItem\([^)]+\)[^{]*\{[^}]*\{[^}]*\}[^}]*\}[^}]*\}'
$newContent = $content -replace $pattern, ''

Set-Content $loginScreenPath $newContent -NoNewline

Write-Host "✅ LoginScreen.kt 수정 완료" -ForegroundColor Green

Write-Host "`n🎉 모든 수정 완료!" -ForegroundColor Green
Write-Host "`n다음 명령어로 빌드를 다시 시도하세요:" -ForegroundColor Cyan
Write-Host ".\gradlew assembleDebug" -ForegroundColor White
