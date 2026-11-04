package capstone.safelaw.dto;

import java.util.List;

// 앱이 기기 안에서 찾은 판례 번호에 가짜 번호를 섞어서 보낸다.
public record LawTextRequestDto(List<Long> ids) {
}
