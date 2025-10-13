# LoginScreen.kt 수정 스크립트
$file = "app\src\main\java\com\lotto\app\ui\screens\LoginScreen.kt"

# 전체 내용 읽기
$lines = Get-Content $file

# FeatureItem 함수 시작 줄 찾기 (끝에서부터)
$featureItemStart = -1
for ($i = $lines.Count - 1; $i -ge 0; $i--) {
    if ($lines[$i] -match '^\s*private fun FeatureItem') {
        # 주석 시작 부분 찾기 (위로 올라가면서)
        for ($j = $i - 1; $j -ge 0; $j--) {
            if ($lines[$j] -match '^\s*/\*\*') {
                $featureItemStart = $j
                break
            }
        }
        break
    }
}

if ($featureItemStart -gt 0) {
    # FeatureItem 함수와 주석 제거
    $newLines = $lines[0..($featureItemStart - 1)]
    
    # 파일에 쓰기
    $newLines | Set-Content $file
    
    Write-Host "✅ LoginScreen.kt에서 FeatureItem 함수 제거 완료" -ForegroundColor Green
} else {
    Write-Host "⚠️ FeatureItem 함수를 찾을 수 없습니다" -ForegroundColor Yellow
}
