package com.example.safelaw.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.HnswIndex

@Entity
data class LawCase(
    @Id var id: Long = 0,
    var caseName: String? = null,
    var caseNum: String? = null,
    var courtName: String? = null,
    var sentenceDate: String? = null,
    var content: String? = null,

    @HnswIndex(dimensions = 384)
    var featureVector: FloatArray? = null
)