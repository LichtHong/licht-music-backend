package work.licht.music.oss.service;

import org.springframework.web.multipart.MultipartFile;
import work.licht.music.common.response.Response;

public interface FileService {

    // 上传文件
    Response<?> uploadFile(MultipartFile file);
}