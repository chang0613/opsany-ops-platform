package com.opsany.replica.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.opsany.replica.domain.WorkOrderAttachment;
import com.opsany.replica.domain.WorkOrderAttachmentConfig;
import com.opsany.replica.repository.WorkOrderAttachmentRepository;
import com.opsany.replica.repository.WorkOrderAttachmentConfigRepository;
import com.opsany.replica.security.SessionUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderAttachmentService {

    private final WorkOrderAttachmentRepository attachmentRepository;
    private final WorkOrderAttachmentConfigRepository configRepository;

    @Value("${app.attachment.upload-path:./uploads/attachments}")
    private String uploadPath;

    private static final String DEFAULT_CONFIG_KEY = "default";

    @Transactional
    public WorkOrderAttachment uploadAttachment(MultipartFile file, String orderNo, SessionUser sessionUser) throws IOException {
        WorkOrderAttachmentConfig config = getAttachmentConfig();

        validateFile(file, config);

        validateTotalSize(orderNo, file.getSize(), config);

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String newFileName = UUID.randomUUID().toString() + "." + extension;

        Path uploadDir = Paths.get(uploadPath, orderNo);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        Path filePath = uploadDir.resolve(newFileName);
        file.transferTo(filePath.toFile());

        WorkOrderAttachment attachment = WorkOrderAttachment.builder()
            .orderNo(orderNo)
            .fileName(originalFilename)
            .filePath(filePath.toString())
            .fileSize(file.getSize())
            .fileType(file.getContentType())
            .fileExtension(extension)
            .uploadUsername(sessionUser.getUsername())
            .uploadDisplayName(sessionUser.getDisplayName())
            .createdAt(LocalDateTime.now())
            .build();

        attachmentRepository.insert(attachment);
        return attachment;
    }

    private void validateFile(MultipartFile file, WorkOrderAttachmentConfig config) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename).toLowerCase();

        List<String> allowedExtensions = Arrays.asList(
            config.getAllowedExtensions().replaceAll("\\s", "").split(",")
        );

        if (!allowedExtensions.contains(extension)) {
            throw new IOException("不支持的文件格式: " + extension + ", 仅支持: " + config.getAllowedExtensions());
        }

        long maxSizeBytes = config.getMaxFileSizeMb() * 1024 * 1024L;
        if (file.getSize() > maxSizeBytes) {
            throw new IOException("文件大小超过限制: " + config.getMaxFileSizeMb() + "MB");
        }
    }

    private void validateTotalSize(String orderNo, long fileSize, WorkOrderAttachmentConfig config) {
        long currentTotalSize = attachmentRepository.sumFileSizeByOrderNo(orderNo);
        long maxTotalSizeBytes = config.getMaxTotalSizeMb() * 1024 * 1024L;

        if (currentTotalSize + fileSize > maxTotalSizeBytes) {
            throw new RuntimeException("工单附件总大小超过限制: " + config.getMaxTotalSizeMb() + "MB");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    public WorkOrderAttachmentConfig getAttachmentConfig() {
        WorkOrderAttachmentConfig config = configRepository.findByConfigKey(DEFAULT_CONFIG_KEY);
        if (config == null) {
            config = createDefaultConfig();
        }
        return config;
    }

    private WorkOrderAttachmentConfig createDefaultConfig() {
        WorkOrderAttachmentConfig config = WorkOrderAttachmentConfig.builder()
            .configKey(DEFAULT_CONFIG_KEY)
            .allowedExtensions("jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,zip,rar,txt")
            .maxFileSizeMb(10)
            .maxTotalSizeMb(50)
            .enabled(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        configRepository.insert(config);
        return config;
    }

    public List<WorkOrderAttachment> getAttachmentsByOrderNo(String orderNo) {
        return attachmentRepository.findByOrderNo(orderNo);
    }

    public WorkOrderAttachment getAttachmentById(Long id) {
        return attachmentRepository.findById(id);
    }

    @Transactional
    public void deleteAttachment(Long id) throws IOException {
        WorkOrderAttachment attachment = attachmentRepository.findById(id);
        if (attachment != null) {
            File file = new File(attachment.getFilePath());
            if (file.exists()) {
                file.delete();
            }
            attachmentRepository.deleteById(id);
        }
    }

    @Transactional
    public void deleteAttachmentsByOrderNo(String orderNo) throws IOException {
        List<WorkOrderAttachment> attachments = attachmentRepository.findByOrderNo(orderNo);
        for (WorkOrderAttachment attachment : attachments) {
            File file = new File(attachment.getFilePath());
            if (file.exists()) {
                file.delete();
            }
        }
        attachmentRepository.deleteByOrderNo(orderNo);
    }

    public WorkOrderAttachmentConfig updateAttachmentConfig(WorkOrderAttachmentConfig config) {
        config.setUpdatedAt(LocalDateTime.now());
        configRepository.update(config);
        return config;
    }

    public List<WorkOrderAttachmentConfig> getAllAttachmentConfigs() {
        return configRepository.findAll();
    }
}
