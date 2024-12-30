package work.licht.music.oss.strategy;

import org.springframework.web.multipart.MultipartFile;

public interface FileStrategy {

    String uploadFile(MultipartFile file, String bucketName);
}
