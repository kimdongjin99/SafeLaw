package capstone.safelaw.dto;

import capstone.safelaw.domain.LegalData;

// 판례 번호로 조회한 판례 본문 한 건.
public record LawTextDto(
        Long id,
        String caseName,
        String caseNum,
        String courtName,
        String content
) {
    public static LawTextDto of(LegalData data) {
        return new LawTextDto(data.id, data.caseName, data.caseNum, data.courtName, data.content);
    }
}
