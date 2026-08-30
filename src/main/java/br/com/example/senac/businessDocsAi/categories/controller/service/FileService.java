package br.com.example.senac.businessDocsAi.categories.controller.service;

import br.com.example.senac.businessDocsAi.categories.controller.entity.File;
import br.com.example.senac.businessDocsAi.categories.controller.repository.FileRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class FileService {

    private final FileRepository fileRepository;

    private final Path uploadDirectory = Paths.get("uploads");

    public FileService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public String processFile(MultipartFile file) {

        try {
            Files.createDirectories(uploadDirectory);

            String originalName = file.getOriginalFilename();
            String extension = "";

            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }

            String storedName = UUID.randomUUID() + extension;

            Path filePath = uploadDirectory.resolve(storedName);

            file.transferTo(filePath);

            File fileEntity = new File();

            fileEntity.setOriginalName(originalName);
            fileEntity.setStoredName(storedName);
            fileEntity.setMimeType(file.getContentType());
            fileEntity.setExtension(extension);
            fileEntity.setSize(file.getSize());
            fileEntity.setPath(filePath.toString());
            fileEntity.setUploadDate(LocalDateTime.now());

            fileRepository.save(fileEntity);

            return "PDF uploaded successfully: " + originalName;

        } catch (IOException e) {
            throw new RuntimeException("Error uploading PDF", e);
        }
    }
}