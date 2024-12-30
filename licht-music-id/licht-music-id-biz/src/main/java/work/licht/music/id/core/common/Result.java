package work.licht.music.id.core.common;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    private long id;
    private Status status;

    @Override
    public String toString() {
        return "Result{" + "id=" + id +
                ", status=" + status +
                '}';
    }
}
