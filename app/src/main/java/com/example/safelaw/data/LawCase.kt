package com.example.safelaw.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.HnswIndex

@Entity
class LawCase {
    // 📍 assignable = true 옵션을 반드시 추가해 줍니다!
    @Id(assignable = true)
    var id: Long = 0

    @HnswIndex(dimensions = 128)
    var embedding: FloatArray? = null
}