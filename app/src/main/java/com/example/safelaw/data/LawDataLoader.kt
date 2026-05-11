package com.example.safelaw.data

import android.content.Context
import android.util.Log
import com.example.safelaw.repository.LegalSearchManager
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import io.objectbox.Box
import kotlin.concurrent.thread

class LawDataLoader(private val context: Context) {
    private val lawCaseBox: Box<LawCase> = ObjectBox.store.boxFor(LawCase::class.java)

    // AI 임베딩 엔진
    private val searchManager = LegalSearchManager(context, lawCaseBox)

    fun loadCsvToDatabase() {
        thread {
            try {
                val existingCount = lawCaseBox.count().toInt()
                val targetLoadCount = 4000
                var currentLoadCount = 0
                var indexOffset = 0
                var batchList = mutableListOf<LawCase>() // var로 변경하여 초기화 용이하게 수정

                Log.d("SafeLaw", "작업 시작: 현재 DB에 $existingCount 개 있음.")

                val inputStream = context.assets.open("law_data.csv")

                csvReader().open(inputStream) {
                    readAllWithHeaderAsSequence()
                        .drop(existingCount)
                        .take(targetLoadCount)
                        .forEach { row: Map<String, String> ->

                            val contentValue = row["content"]?.toString() ?: ""
                            val caseName = contentValue.trim()

                            val idValue = row["id"]?.toString() ?: ""
                            val caseId = if (idValue.isEmpty()) (existingCount + indexOffset).toString() else idValue.trim()

                            // [수정] 글자 수 제한 (10자 미만 무시, 500자 초과 시 에러 방지를 위해 필터링)
                            // ONNX 모델 에러(ORT_RUNTIME_EXCEPTION)를 막는 핵심 구간입니다.
                            if (caseName.length in 10..500) {
                                try {
                                    val vector = searchManager.getEmbedding(caseName)
                                    if (vector.isNotEmpty()) {
                                        batchList.add(LawCase(
                                            caseName = caseName,
                                            caseNum = caseId,
                                            featureVector = vector
                                        ))
                                        currentLoadCount++
                                    }
                                } catch (e: Exception) {
                                    // 특정 행에서 모델 에러가 나도 로그만 찍고 다음 행으로 넘어갑니다.
                                    Log.e("SafeLaw", "벡터화 실패(ID: $caseId): ${e.message}")
                                }
                            }

                            // 500개마다 DB 저장
                            if (batchList.size >= 500) {
                                lawCaseBox.put(batchList)
                                batchList = mutableListOf() // 메모리 해제를 위해 새로 생성
                                Log.d("SafeLaw", "진행 중: ${existingCount + currentLoadCount}개 누적 저장 완료")
                            }
                            indexOffset++
                        }
                }

                // 남은 데이터 저장
                if (batchList.isNotEmpty()) {
                    lawCaseBox.put(batchList)
                }
                Log.d("SafeLaw", "이번 회차 성공! 현재 DB 총 개수: ${lawCaseBox.count()}")

            } catch (e: Exception) {
                Log.e("SafeLaw", "파일 로드 오류: ${e.message}")
            }
        }
    }
}