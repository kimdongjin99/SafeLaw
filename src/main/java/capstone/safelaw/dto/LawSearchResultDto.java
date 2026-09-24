package capstone.safelaw.dto;

import capstone.safelaw.domain.LegalData;

/**
 * 판례 검색 결과 한 건.
 * score는 ObjectBox HNSW 인덱스의 거리 값(기본: 유클리드 제곱 거리)으로, 낮을수록 검색 벡터와 유사하다.
 */
public record LawSearchResultDto(
        Long id,
        String caseName,
        String caseNum,
        String courtName,
        double score,
        String content
) {
    public static LawSearchResultDto of(LegalData data, double score) {
        return new LawSearchResultDto(data.id, data.caseName, data.caseNum, data.courtName, score, data.content);
    }
}
