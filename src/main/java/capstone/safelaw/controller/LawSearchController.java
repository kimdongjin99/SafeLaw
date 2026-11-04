package capstone.safelaw.controller;

import capstone.safelaw.dto.LawTextDto;
import capstone.safelaw.dto.LawTextRequestDto;
import capstone.safelaw.service.LawService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/laws")
@CrossOrigin(origins = "*")
@Tag(name = "Law", description = "판례 본문 조회 API")
@RequiredArgsConstructor
public class LawSearchController {

    private final LawService lawService;

    // 에러 응답은 GlobalExceptionHandler에서 JSON으로 처리
    @Operation(summary = "판례 번호로 본문 조회", description = "앱이 기기 안에서 찾은 판례 번호에 가짜 번호를 섞어 보내면 해당 판례 본문을 반환합니다. 번호는 1~30개, 없는 번호는 결과에서 빠지며 번호 오름차순으로 반환합니다.")
    @PostMapping("/texts")
    public ResponseEntity<List<LawTextDto>> findTexts(@RequestBody LawTextRequestDto request) {
        return ResponseEntity.ok(lawService.findTexts(request.ids()));
    }
}
