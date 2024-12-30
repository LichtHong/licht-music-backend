package work.licht.music.user.model.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import work.licht.music.common.validator.PhoneNumber;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindUserByPhoneReqDTO {

    // 手机号
    @NotBlank(message = "手机号不能为空")
    @PhoneNumber
    private String phone;

}