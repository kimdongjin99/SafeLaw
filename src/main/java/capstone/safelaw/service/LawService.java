package capstone.safelaw.service;

import capstone.safelaw.domain.LegalData;
import capstone.safelaw.domain.Precedent;
import capstone.safelaw.domain.Precedent_;
import capstone.safelaw.dto.LawSearchResultDto;
import capstone.safelaw.exception.ApiException;
import capstone.safelaw.exception.ErrorCode;
import capstone.safelaw.repository.LegalDataRepository;
import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.query.ObjectWithScore;
import io.objectbox.query.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LawService {

    private static final int VECTOR_DIMENSIONS = 384;
    private static final int DEFAULT_TOP_K = 3;
    private static final int MAX_TOP_K = 10;

    private final LegalDataRepository legalDataRepository;
    private final Box<Precedent> precedentBox;

    public LawService(LegalDataRepository legalDataRepository, BoxStore boxStore) {
        this.legalDataRepository = legalDataRepository;
        this.precedentBox = boxStore.boxFor(Precedent.class);
    }

    // 개인정보 보호: 검색 벡터 값은 절대 로그에 남기지 않는다.
    public List<LawSearchResultDto> findSimilarPrecedents(float[] queryVector, Integer requestedTopK) {
        validateVector(queryVector);
        int topK = resolveTopK(requestedTopK);

        // findWithScores()는 유사도(거리) 오름차순으로 반환된다.
        List<ObjectWithScore<Precedent>> scored;
        try (Query<Precedent> query = precedentBox.query()
                .nearestNeighbors(Precedent_.embedding, queryVector, topK)
                .build()) {
            scored = query.findWithScores();
        }

        List<Long> ids = scored.stream().map(s -> s.get().id).toList();
        Map<Long, LegalData> dataById = legalDataRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(d -> d.id, Function.identity()));

        // SQLite 조회 결과는 순서가 보장되지 않으므로 ObjectBox 점수 순서대로 다시 조립
        List<LawSearchResultDto> results = new ArrayList<>();
        for (ObjectWithScore<Precedent> s : scored) {
            LegalData data = dataById.get(s.get().id);
            if (data == null) {
                log.warn("ObjectBox에는 있으나 SQLite에 없는 판례 ID: {}", s.get().id);
                continue;
            }
            results.add(LawSearchResultDto.of(data, s.getScore()));
        }

        log.info("판례 검색 완료: topK={}, 결과={}건", topK, results.size());
        return results;
    }

    private void validateVector(float[] vector) {
        if (vector == null || vector.length != VECTOR_DIMENSIONS) {
            throw new ApiException(ErrorCode.INVALID_VECTOR);
        }
        for (float v : vector) {
            if (!Float.isFinite(v)) {
                throw new ApiException(ErrorCode.INVALID_VECTOR);
            }
        }
    }

    private int resolveTopK(Integer topK) {
        if (topK == null) {
            return DEFAULT_TOP_K;
        }
        if (topK < 1 || topK > MAX_TOP_K) {
            throw new ApiException(ErrorCode.INVALID_TOP_K);
        }
        return topK;
    }
}
