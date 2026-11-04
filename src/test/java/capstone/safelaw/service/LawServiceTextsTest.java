package capstone.safelaw.service;

import capstone.safelaw.domain.LegalData;
import capstone.safelaw.dto.LawTextDto;
import capstone.safelaw.exception.ApiException;
import capstone.safelaw.exception.ErrorCode;
import capstone.safelaw.repository.LegalDataRepository;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// 실제 DB 대신 mock 저장소로 판례 번호 조회 규칙을 확인한다.
class LawServiceTextsTest {

    private final LegalDataRepository repository = mock(LegalDataRepository.class);
    private final LawService service = new LawService(repository);

    @Test
    void returnsTextsSortedByIdAndSkipsMissing() {
        when(repository.findAllById(any())).thenReturn(List.of(legalData(30L), legalData(10L)));

        List<LawTextDto> results = service.findTexts(List.of(30L, 999L, 10L));

        assertEquals(List.of(10L, 30L), results.stream().map(LawTextDto::id).toList());
        assertEquals("사건10", results.get(0).caseName());
        assertEquals("본문10", results.get(0).content());
    }

    @Test
    void removesDuplicateIdsBeforeLookup() {
        when(repository.findAllById(any())).thenAnswer(call -> {
            Iterable<Long> ids = call.getArgument(0);
            assertEquals(1, ((Collection<?>) ids).size());
            return List.of(legalData(10L));
        });

        assertEquals(1, service.findTexts(List.of(10L, 10L)).size());
    }

    @Test
    void acceptsMaximumIds() {
        when(repository.findAllById(any())).thenReturn(List.of());

        assertEquals(0, service.findTexts(LongStream.rangeClosed(1, 30).boxed().toList()).size());
    }

    @Test
    void rejectsNullIds() {
        assertInvalidIds(null);
    }

    @Test
    void rejectsEmptyIds() {
        assertInvalidIds(List.of());
    }

    @Test
    void rejectsTooManyIds() {
        assertInvalidIds(LongStream.rangeClosed(1, 31).boxed().toList());
    }

    @Test
    void rejectsNullElement() {
        assertInvalidIds(Arrays.asList(1L, null));
    }

    private void assertInvalidIds(List<Long> ids) {
        ApiException e = assertThrows(ApiException.class, () -> service.findTexts(ids));
        assertEquals(ErrorCode.INVALID_IDS, e.getErrorCode());
    }

    private static LegalData legalData(long id) {
        LegalData data = new LegalData();
        data.id = id;
        data.caseName = "사건" + id;
        data.caseNum = "번호" + id;
        data.courtName = "법원" + id;
        data.content = "본문" + id;
        return data;
    }
}
