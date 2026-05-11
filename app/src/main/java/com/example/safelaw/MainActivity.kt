package com.example.safelaw

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.safelaw.data.LawDataLoader
import com.example.safelaw.repository.LawRepository
import com.example.safelaw.data.LawCase
import com.example.safelaw.data.ObjectBox
import com.example.safelaw.repository.LegalSearchManager
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    // Lazy 초기화로 필요한 시점에 생성
    private val repository by lazy { LawRepository() }
    private var searchManager: LegalSearchManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI 스레드 차단을 막기 위해 백그라운드 스레드에서 실행
        thread {
            try {
                // 1. DB 초기 상태 확인
                val currentCount = repository.getLawCount()
                Log.d("SafeLaw", "현재 DB에 저장된 데이터 개수: $currentCount")

                // 2. 검색 매니저 초기화 (ONNX 모델 로딩 포함)
                // ObjectBox.store를 통해 LawCase 박스 주입
                searchManager = LegalSearchManager(this, ObjectBox.store.boxFor(LawCase::class.java))

                // 3. 데이터 존재 여부에 따른 분기 처리
                if (currentCount > 0L) {
                    Log.d("SafeLaw", "데이터가 이미 존재합니다. 검색을 테스트합니다.")

                    // 9만 개 데이터 사이에서 벡터 유사도 검색 실행
                    val results = searchManager?.search("전세 사기 관련 판례 찾아줘")

                    // 앱이 죽지 않도록 상위 5개만 로그 출력
                    results?.take(5)?.forEach {
                        Log.d("SafeLaw", "검색된 판례명: ${it.caseName}")
                        Log.d("SafeLaw", "판례 번호: ${it.caseNum}")
                    }
                } else {
                    // 데이터가 하나도 없을 때만 CSV 로딩 실행
                    Log.d("SafeLaw", "데이터가 비어 있습니다. CSV 주입을 시작합니다...")
                    LawDataLoader(this).loadCsvToDatabase()
                }

            } catch (e: Exception) {
                // 9만 개 처리 중 발생할 수 있는 메모리/파일 에러 캐치
                Log.e("SafeLaw", "오류 발생: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 앱 종료 시 AI 모델 및 리소스 해제
        thread {
            searchManager?.close()
        }
    }
}