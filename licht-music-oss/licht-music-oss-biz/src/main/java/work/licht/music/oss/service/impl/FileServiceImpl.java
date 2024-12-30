package work.licht.music.oss.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import work.licht.music.common.response.Response;
import work.licht.music.oss.service.FileService;
import work.licht.music.oss.strategy.FileStrategy;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Resource
    private FileStrategy fileStrategy;

    private static final String BUCKET_NAME = "licht-music";

    @Override
    public Response<?> uploadFile(MultipartFile file) {
        // 上传文件到
        String url = fileStrategy.uploadFile(file, BUCKET_NAME);
        return Response.success(url);
    }
}
