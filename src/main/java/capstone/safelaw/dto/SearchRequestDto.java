package capstone.safelaw.dto;

import lombok.Data;

@Data
public class SearchRequestDto {
    private float[] vector; // 앱에서 보내는 384차원 임베딩 벡터
    private Integer topK;   // 반환할 판례 개수 (생략 시 3, 최대 10)
}
