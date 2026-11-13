package com.safelaw.data.db.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.HnswIndex

@Entity
class LawCase {
    // 📍 assignable = true 옵션을 반드시 추가해 줍니다!
    @Id(assignable = true)
    var id: Long = 0

    // KR-SBERT 임베딩 차원. 모델을 바꾸면 LawCaseImporter.DATASET_VERSION도 올려야 DB가 새로 채워집니다.
    @HnswIndex(dimensions = 768)
    var embedding: FloatArray? = null
}