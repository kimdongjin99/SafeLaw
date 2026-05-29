package com.example.safelaw.network

data class LawCaseRequest(val ids: List<Long>)
data class LawCaseResponse(val id: Long, val title: String, val content: String)