package capstone.safelaw.controller;

import capstone.safelaw.dto.LawSearchResultDto;
import capstone.safelaw.dto.SearchRequestDto;
import capstone.safelaw.service.LawService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/laws")
@CrossOrigin(origins = "*")
@Tag(name = "Law Search", description = "판례검색 API")
@RequiredArgsConstructor
public class LawSearchController {

    private final LawService lawService;

    // 에러 응답은 GlobalExceptionHandler에서 JSON으로 처리
    @PostMapping("/search")
    public ResponseEntity<List<LawSearchResultDto>> searchLaw(@RequestBody SearchRequestDto request) {
        return ResponseEntity.ok(lawService.findSimilarPrecedents(request.getVector(), request.getTopK()));
    }
}
