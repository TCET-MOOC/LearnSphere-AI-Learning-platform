package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Entity.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    StoredFile storeResource(MultipartFile file, Long ownerId);
    StoredFile storeVideo(MultipartFile file, Long ownerId);
    StoredFile getMetadata(String fileKey);
    Resource loadAsResource(String fileKey);
    void delete(String fileKey);
}
