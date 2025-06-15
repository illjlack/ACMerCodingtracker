package com.codingtracker.infrastructure.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 头像存储服务
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Service
@Slf4j
public class AvatarStorageService {

    private final String uploadDir;
    private final String baseUrl;
    private final long maxFileSize;
    private final List<String> allowedExtensions;

    public AvatarStorageService(
            @Value("${app.upload.avatar.dir:uploads/avatars}") String uploadDir,
            @Value("${app.upload.avatar.base-url:http://localhost:8080}") String baseUrl,
            @Value("${app.upload.avatar.max-size:2097152}") long maxFileSize) {
        this.uploadDir = uploadDir;
        this.baseUrl = baseUrl;
        this.maxFileSize = maxFileSize;
        this.allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "gif");

        // 确保上传目录存在
        createUploadDirectory();
    }

    /**
     * 存储头像文件
     */
    public String store(MultipartFile file) throws IOException {
        log.info("开始存储头像文件: {}", file.getOriginalFilename());

        // 验证文件
        validateFile(file);

        // 生成唯一文件名
        String fileName = generateFileName(file);

        // 创建目标路径
        Path targetPath = Paths.get(uploadDir).resolve(fileName);

        // 确保父目录存在
        Files.createDirectories(targetPath.getParent());

        // 复制文件
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // 生成访问URL
        String avatarUrl = generateAvatarUrl(fileName);

        log.info("头像文件存储成功: {}", avatarUrl);
        return avatarUrl;
    }

    /**
     * 删除头像文件
     */
    public void delete(String avatarUrl) {
        if (!StringUtils.hasText(avatarUrl)) {
            return;
        }

        try {
            String fileName = extractFileNameFromUrl(avatarUrl);
            if (StringUtils.hasText(fileName)) {
                Path filePath = Paths.get(uploadDir).resolve(fileName);
                Files.deleteIfExists(filePath);
                log.info("头像文件删除成功: {}", fileName);
            }
        } catch (Exception e) {
            log.error("删除头像文件失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 检查文件是否存在
     */
    public boolean exists(String avatarUrl) {
        if (!StringUtils.hasText(avatarUrl)) {
            return false;
        }

        try {
            String fileName = extractFileNameFromUrl(avatarUrl);
            if (StringUtils.hasText(fileName)) {
                Path filePath = Paths.get(uploadDir).resolve(fileName);
                return Files.exists(filePath);
            }
        } catch (Exception e) {
            log.error("检查头像文件存在性失败: {}", e.getMessage(), e);
        }

        return false;
    }

    /**
     * 获取文件大小
     */
    public long getFileSize(String avatarUrl) {
        if (!StringUtils.hasText(avatarUrl)) {
            return 0;
        }

        try {
            String fileName = extractFileNameFromUrl(avatarUrl);
            if (StringUtils.hasText(fileName)) {
                Path filePath = Paths.get(uploadDir).resolve(fileName);
                return Files.size(filePath);
            }
        } catch (Exception e) {
            log.error("获取头像文件大小失败: {}", e.getMessage(), e);
        }

        return 0;
    }

    // ==================== 私有方法 ====================

    /**
     * 创建上传目录
     */
    private void createUploadDirectory() {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("创建头像上传目录: {}", uploadPath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("创建头像上传目录失败: {}", e.getMessage(), e);
            throw new RuntimeException("无法创建头像上传目录", e);
        }
    }

    /**
     * 验证上传文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 检查文件大小
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("文件大小不能超过 " + (maxFileSize / 1024 / 1024) + "MB");
        }

        // 检查文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件类型，只允许: " + String.join(", ", allowedExtensions));
        }

        // 检查MIME类型
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("文件必须是图片类型");
        }
    }

    /**
     * 生成唯一文件名
     */
    private String generateFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);

        // 生成格式: 日期/UUID.扩展名
        String dateFolder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uniqueId = UUID.randomUUID().toString().replace("-", "");

        return dateFolder + "/" + uniqueId + "." + extension;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }

        return filename.substring(lastDotIndex + 1);
    }

    /**
     * 生成头像访问URL
     */
    private String generateAvatarUrl(String fileName) {
        return baseUrl + "/api/files/avatars/" + fileName;
    }

    /**
     * 从URL中提取文件名
     */
    private String extractFileNameFromUrl(String avatarUrl) {
        if (!StringUtils.hasText(avatarUrl)) {
            return null;
        }

        // 提取 /api/files/avatars/ 之后的部分
        String prefix = "/api/files/avatars/";
        int prefixIndex = avatarUrl.indexOf(prefix);
        if (prefixIndex != -1) {
            return avatarUrl.substring(prefixIndex + prefix.length());
        }

        return null;
    }
}