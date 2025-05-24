package com.codingtracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class AvatarStorageService {

    private final String uploadDir;
    private final String urlPrefix;

    public AvatarStorageService(
            @Value("${app.upload-dir.windows:C:\\avatars\\}") String windowsDir,
            @Value("${app.upload-dir.linux:/var/www/avatars/}") String linuxDir,
            @Value("${app.url-prefix:/avatars/}") String urlPrefix
    ) {
        this.urlPrefix = urlPrefix;
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            this.uploadDir = windowsDir;
        } else {
            this.uploadDir = linuxDir;
        }
    }

    /**
     * 保存上传的头像文件，并返回可访问的 URL
     *
     * @param file MultipartFile 头像文件
     * @return 返回头像访问路径，例如 "/avatars/uuid-filename.jpg"
     * @throws IOException 读写异常
     */
    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("上传文件为空");
        }

        String originalFilename = Paths.get(file.getOriginalFilename()).getFileName().toString(); // 安全文件名
        String suffix = "";
        if (originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + suffix;

        // 创建目录（如果不存在）
        File dir = new File(uploadDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("无法创建目录: " + uploadDir);
        }

        // 构造完整路径
        File dest = Paths.get(uploadDir, filename).toFile();
        file.transferTo(dest);

        return urlPrefix + filename;
    }
}
