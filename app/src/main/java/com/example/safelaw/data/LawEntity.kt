package com.example.safelaw.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

@Entity
data class LawEntity(
    @Id var id: Long = 0,
    var caseName: String? = null,
    var caseNum: String? = null,
    var courtName: String? = null,
    var sentenceDate: String? = null,
    @Index var output: String? = null,
    var embedding: FloatArray? = null
)