package com.example.safelaw.repository

import com.example.safelaw.data.LawCase
import com.example.safelaw.data.ObjectBox
import io.objectbox.Box

class LawRepository {
    // 💡 [수정] LawEntity를 지웠으므로, 새 초경량 모델인 LawCase 박스로 연결합니다.
    private val lawBox: Box<LawCase> by lazy {
        ObjectBox.store.boxFor(LawCase::class.java)
    }

    /**
     * 현재 DB에 저장된 총 데이터 개수를 반환합니다.
     * MainActivity에서 에셋 DB가 제대로 복사되어 탑재됐는지 검증할 때 쓰입니다.
     */
    fun getLawCount(): Long {
        return lawBox.count()
    }

}