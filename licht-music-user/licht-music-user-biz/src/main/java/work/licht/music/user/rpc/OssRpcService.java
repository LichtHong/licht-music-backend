package work.licht.music.user.rpc;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import work.licht.music.common.response.Response;
import work.licht.music.oss.api.FileFeignApi;

@Component
public class OssRpcService {

    @Resource
    private FileFeignApi fileFeignApi;

    public String uploadFile(MultipartFile file) {
        // 调用对象存储服务上传文件
        Response<?> response = fileFeignApi.uploadFile(file);
        if (!response.isSuccess()) {
            return null;
        }
        // 返回图片访问链接
        return (String) response.getData();
    }
}