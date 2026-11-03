package com.safelaw

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.safelaw.data.db.DbPrototypeRunner

/** 네 브랜치 전용 DB 테스트 화면: 질문을 입력하면 검색·캐시·섀도우 쿼리 과정을 보여줍니다. */
class MainActivity : AppCompatActivity(), DbPrototypeRunner.Listener {

    // DB 로직은 main 구조에 맞춰 DbPrototypeRunner로 분리
    private lateinit var dbPrototypeRunner: DbPrototypeRunner

    private lateinit var statusText: TextView
    private lateinit var promptInput: EditText
    private lateinit var searchButton: Button
    private lateinit var resultScroll: ScrollView
    private lateinit var resultText: TextView

    private var isReady = false
    private var isSearching = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        promptInput = findViewById(R.id.promptInput)
        searchButton = findViewById(R.id.searchButton)
        resultScroll = findViewById(R.id.resultScroll)
        resultText = findViewById(R.id.resultText)

        searchButton.setOnClickListener { submitPrompt() }
        promptInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                submitPrompt()
                true
            } else {
                false
            }
        }
        findViewById<Button>(R.id.clearButton).setOnClickListener { resultText.text = "" }

        dbPrototypeRunner = DbPrototypeRunner(this, this)
        dbPrototypeRunner.start()
    }

    private fun submitPrompt() {
        val prompt = promptInput.text.toString().trim()
        if (!isReady || isSearching || prompt.isEmpty()) {
            return
        }
        isSearching = true
        updateSearchButton()
        dbPrototypeRunner.ask(prompt)
    }

    override fun onStatus(status: String) {
        statusText.text = status
    }

    override fun onReady(caseCount: Long) {
        isReady = true
        statusText.text = "✅ 준비 완료: 판례 ${caseCount}개"
        updateSearchButton()
    }

    override fun onLog(message: String) {
        resultText.append(message + "\n")
        resultScroll.post { resultScroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    override fun onSearchFinished() {
        isSearching = false
        updateSearchButton()
    }

    private fun updateSearchButton() {
        searchButton.isEnabled = isReady && !isSearching
    }

    override fun onDestroy() {
        if (::dbPrototypeRunner.isInitialized) {
            dbPrototypeRunner.close()
        }
        super.onDestroy()
    }
}
