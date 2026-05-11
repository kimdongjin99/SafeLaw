package com.example.safelaw.repository

import com.example.safelaw.data.LawEntity
import com.example.safelaw.data.ObjectBox
import io.objectbox.Box

class LawRepository {
    // DB 창고(Box) 연결
    private val lawBox: Box<LawEntity> by lazy {
        ObjectBox.store.boxFor(LawEntity::class.java)
    }

    // [중요] LawDataLoader에서 호출하는 바로 그 함수!
    fun insertAllLawData(lawList: List<LawEntity>) {
        // 대량의 데이터를 넣을 때는 runInTx(트랜잭션)를 써야 속도가 수십 배 빠릅니다.
        ObjectBox.store.runInTx {
            lawBox.put(lawList)
        }
    }

    // 데이터가 잘 들어갔는지 확인용
    fun getLawCount(): Long {
        return lawBox.count()
    }
}