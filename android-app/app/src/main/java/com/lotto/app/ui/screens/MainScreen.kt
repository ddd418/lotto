package com.lotto.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lotto.app.ui.theme.NotionColors
import com.lotto.app.ui.components.NotionCard
import com.lotto.app.ui.components.NotionSectionHeader
import com.lotto.app.ui.components.NotionButton
import com.lotto.app.ui.components.NotionButtonVariant
import com.lotto.app.ui.components.NotionBadge
import com.lotto.app.ui.components.NotionDivider
import com.lotto.app.viewmodel.LottoViewModel
import com.lotto.app.viewmodel.UiState

/**
 * 메인 화면 (노션 스타일)
 */
@Composable
fun MainScreen(
    viewModel: LottoViewModel,
    onNavigateToRecommend: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSavedNumbers: () -> Unit,
    onNavigateToCheckWinning: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAnalysis: () -> Unit,
    onNavigateToVirtualDraw: () -> Unit
) {
    val latestDrawState by viewModel.latestDrawState.collectAsStateWithLifecycle()
    val isServerConnected by viewModel.isServerConnected.collectAsStateWithLifecycle()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // 헤더
            NotionSectionHeader(
                title = "로또 번호 추천",
                subtitle = "AI 기반 데이터 분석으로\n최적의 번호를 찾아보세요",
                icon = "🎯"
            )
            
            // 서버 상태 및 최신 데이터
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                NotionServerStatusCard(isConnected = isServerConnected)
                
                when (val currentState = latestDrawState) {
                    is UiState.Success -> {
                        NotionLatestDrawCard(
                            lastDraw = currentState.data.lastDraw,
                            generatedAt = currentState.data.generatedAt
                        )
                    }
                    is UiState.Error -> {
                        NotionCard(backgroundColor = NotionColors.Red100) {
                            Text(
                                text = "⚠️ 최신 회차 정보를 불러올 수 없습니다",
                                color = NotionColors.Error,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    else -> {}
                }
            }
            
            // 메인 기능 - AI 번호 추천
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                NotionCard(backgroundColor = NotionColors.Blue50) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "🤖", fontSize = 20.sp)
                                Text(
                                    text = "AI 번호 추천",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NotionColors.TextPrimary
                                )
                            }
                            NotionBadge(
                                text = "추천",
                                backgroundColor = NotionColors.Primary,
                                textColor = Color.White
                            )
                        }
                        
                        Text(
                            text = "1192회차 데이터 분석을 바탕으로 4가지 전략의 번호를 추천해드립니다",
                            fontSize = 14.sp,
                            color = NotionColors.TextSecondary,
                            lineHeight = 20.sp
                        )
                        
                        NotionButton(
                            text = "번호 추천 받기",
                            onClick = onNavigateToRecommend,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = "🎲"
                        )
                    }
                }
                
                // 기능 그리드
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NotionFeatureCard(
                            title = "당첨 확인",
                            description = "내 번호 당첨 여부",
                            icon = "🏆",
                            onClick = onNavigateToCheckWinning,
                            modifier = Modifier.weight(1f)
                        )
                        
                        NotionFeatureCard(
                            title = "통계 분석",
                            description = "번호별 출현 빈도",
                            icon = "📊",
                            onClick = onNavigateToStats,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NotionFeatureCard(
                            title = "저장한 번호",
                            description = "관심 번호 관리",
                            icon = "💾",
                            onClick = onNavigateToSavedNumbers,
                            modifier = Modifier.weight(1f)
                        )
                        
                        NotionFeatureCard(
                            title = "가상 추첨",
                            description = "재미있는 추첨 체험",
                            icon = "🎰",
                            onClick = onNavigateToVirtualDraw,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // 고급 기능
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "고급 분석",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NotionColors.TextSecondary
                )
                
                NotionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "📈", fontSize = 18.sp)
                            Text(
                                text = "패턴 분석 대시보드",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = NotionColors.TextPrimary
                            )
                        }
                        
                        Text(
                            text = "저장된 번호들의 패턴과 통계를 시각적으로 분석합니다",
                            fontSize = 14.sp,
                            color = NotionColors.TextSecondary,
                            lineHeight = 20.sp
                        )
                        
                        NotionButton(
                            text = "분석 대시보드 열기",
                            onClick = onNavigateToAnalysis,
                            modifier = Modifier.fillMaxWidth(),
                            variant = NotionButtonVariant.Secondary,
                            leadingIcon = "📊"
                        )
                    }
                }
            }
            
            // 설정
            NotionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "⚙️", fontSize = 18.sp)
                        Text(
                            text = "설정",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = NotionColors.TextPrimary
                        )
                    }
                    
                    NotionButton(
                        text = "열기",
                        onClick = onNavigateToSettings,
                        variant = NotionButtonVariant.Ghost
                    )
                }
            }
            
            // 하단 여백
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * 노션 스타일 서버 상태 카드
 */
@Composable
fun NotionServerStatusCard(isConnected: Boolean) {
    NotionCard(
        backgroundColor = if (isConnected) NotionColors.Highlight else NotionColors.Gray100
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isConnected) "🟢" else "🔴",
                fontSize = 16.sp
            )
            Text(
                text = if (isConnected) "서버 연결됨" else "서버 연결 안됨",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isConnected) NotionColors.Success else NotionColors.Error
            )
            
            if (isConnected) {
                Spacer(modifier = Modifier.weight(1f))
                NotionBadge(
                    text = "온라인",
                    backgroundColor = NotionColors.Success,
                    textColor = Color.White
                )
            }
        }
    }
}

/**
 * 노션 스타일 최신 회차 카드
 */
@Composable
fun NotionLatestDrawCard(lastDraw: Int, generatedAt: String) {
    NotionCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "📅", fontSize = 16.sp)
                Text(
                    text = "최신 데이터",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = NotionColors.TextPrimary
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${lastDraw}회차까지 업데이트됨",
                        fontSize = 14.sp,
                        color = NotionColors.TextSecondary
                    )
                    Text(
                        text = generatedAt.take(10), // YYYY-MM-DD 형식만
                        fontSize = 12.sp,
                        color = NotionColors.TextTertiary
                    )
                }
                
                NotionBadge(
                    text = "최신",
                    backgroundColor = NotionColors.Info,
                    textColor = Color.White
                )
            }
        }
    }
}

/**
 * 노션 스타일 기능 카드
 */
@Composable
fun NotionFeatureCard(
    title: String,
    description: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NotionCard(
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = icon,
                fontSize = 24.sp
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NotionColors.TextPrimary
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = NotionColors.TextSecondary,
                    lineHeight = 16.sp
                )
            }
            
            NotionButton(
                text = "열기",
                onClick = onClick,
                variant = NotionButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}