package capstone.safelaw.service;

import capstone.safelaw.domain.LegalData;
import capstone.safelaw.dto.LawTextDto;
import capstone.safelaw.exception.ApiException;
import capstone.safelaw.exception.ErrorCode;
import capstone.safelaw.repository.LegalDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class LawService {

    private static final int MAX_TEXT_IDS = 30;

    private final LegalDataRepository legalDataRepository;

    // 개인정보 보호: 어떤 판례를 요청했는지 알 수 있으므로 판례 번호는 절대 로그에 남기지 않는다.
    // 없는 번호는 결과에서 빠진다. 요청 순서가 드러나지 않도록 번호 오름차순으로 반환한다.
    public List<LawTextDto> findTexts(List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > MAX_TEXT_IDS
                || ids.stream().anyMatch(Objects::isNull)) {
            throw new ApiException(ErrorCode.INVALID_IDS);
        }
        List<LawTextDto> results = legalDataRepository.findAllById(new HashSet<>(ids)).stream()
                .sorted(Comparator.comparing((LegalData data) -> data.id))
                .map(LawTextDto::of)
                .toList();

        log.info("판례 본문 조회 완료: 요청={}건, 결과={}건", ids.size(), results.size());
        return results;
    }
}
