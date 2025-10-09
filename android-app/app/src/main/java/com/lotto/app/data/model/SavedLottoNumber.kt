package com.lotto.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * 저장된 로또 번호
 */
data class SavedLottoNumber(
    @SerializedName("id")
    val id: String,  // 고유 ID (타임스탬프 기반)
    
    @SerializedName("numbers")
    val numbers: List<Int>,  // 6개 번호
    
    @SerializedName("savedAt")
    val savedAt: Long,  // 저장 시각 (밀리초)
    
    @SerializedName("memo")
    val memo: String = "",  // 메모 (선택사항)
    
    @SerializedName("drawNumber")
    val drawNumber: Int? = null,  // 회차 번호 (선택사항)
    
    @SerializedName("isFavorite")
    val isFavorite: Boolean = false  // 즐겨찾기 여부
)
