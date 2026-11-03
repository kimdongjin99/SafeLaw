package com.safelaw

import com.safelaw.data.db.SearchResultCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SearchResultCacheTest {

    @Test
    fun similarQuestion_reusesCachedCaseIds() {
        val cache = SearchResultCache()
        cache.putSearchResult("친구가 돈을 안 갚아요", listOf(1L, 2L, 3L))

        val hit = cache.findSimilarQuestion("친구가 돈을 안 갚아요 ㅠㅠ")

        assertNotNull(hit)
        assertEquals(listOf(1L, 2L, 3L), hit!!.caseIds)
        assertEquals("친구가 돈을 안 갚아요", hit.cachedQuestion)
    }

    @Test
    fun punctuationAndSpacing_areIgnored() {
        val cache = SearchResultCache()
        cache.putSearchResult("친구가 돈을 안 갚아요", listOf(1L))

        val hit = cache.findSimilarQuestion("친구가  돈을 안갚아요?")

        assertEquals(1.0, hit!!.similarity, 0.0)
    }

    @Test
    fun differentQuestion_isNotReused() {
        val cache = SearchResultCache()
        cache.putSearchResult("친구가 돈을 안 갚아요", listOf(1L))

        assertNull(cache.findSimilarQuestion("친구가 돈을 훔쳐갔어요"))
    }

    @Test
    fun groundingData_isCachedByCaseIds() {
        val cache = SearchResultCache()
        cache.putGroundingData(listOf(1L, 2L), "판례 원문")

        assertEquals("판례 원문", cache.findGroundingData(listOf(1L, 2L)))
        assertNull(cache.findGroundingData(listOf(2L, 1L)))
    }

    @Test
    fun oldestQuestion_isEvictedWhenFull() {
        val cache = SearchResultCache(maxSize = 2)
        cache.putSearchResult("교통사고 합의금", listOf(1L))
        cache.putSearchResult("명예훼손 고소 방법", listOf(2L))
        cache.putSearchResult("전세 보증금 반환", listOf(3L))

        assertNull(cache.findSimilarQuestion("교통사고 합의금"))
        assertNotNull(cache.findSimilarQuestion("전세 보증금 반환"))
    }

    @Test
    fun jaccard_matchesHandCalculation() {
        // {ab, bc} ∩ {bc, cd} = {bc} → 1 / 3
        val a = SearchResultCache.toBigrams("abc")
        val b = SearchResultCache.toBigrams("bcd")

        assertEquals(1.0 / 3, SearchResultCache.jaccard(a, b), 1e-9)
    }
}
