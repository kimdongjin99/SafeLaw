package capstone.safelaw.domain;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.HnswIndex;

@Entity
public class Precedent {
    // 📍 1.  ID를 수동으로 넣을 수 있게 허용
    @Id(assignable = true)
    public long id;

    // 📍 2. 차원 수를 정확히 384로 맞춤
    @HnswIndex(dimensions = 384)
    public float[] embedding;
}