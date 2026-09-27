package br.com.example.senac.businessDocsAi.uploadFiles.service;

import br.com.example.senac.businessDocsAi.uploadFiles.entity.UploadFilesEntity;
import br.com.example.senac.businessDocsAi.uploadFiles.repository.IUploadFilesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UploadFilesService {

    private static final Logger log = LoggerFactory.getLogger(UploadFilesService.class);

    private final IUploadFilesRepository uploadFilesRepository;

    private final Path uploadDirectory = Paths.get("uploads");

    public UploadFilesService(IUploadFilesRepository uploadFilesRepository) {
        this.uploadFilesRepository = uploadFilesRepository;
    }

    public String processFile(MultipartFile file) {

        String originalName = file.getOriginalFilename();
        String extension = "";

        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }

        String storedName = UUID.randomUUID() + extension;
        Path filePath;

        try {
            Files.createDirectories(uploadDirectory);

            filePath = uploadDirectory.resolve(storedName);

            file.transferTo(filePath);
        } catch (IOException e) {
            log.error("Falha ao gravar o arquivo enviado no disco", e);
            throw new UncheckedIOException("Não foi possível processar o arquivo enviado", e);
        }

        UploadFilesEntity uploadFilesEntity = new UploadFilesEntity();

        uploadFilesEntity.setOriginalName(originalName);
        uploadFilesEntity.setStoredName(storedName);
        uploadFilesEntity.setMimeType(file.getContentType());
        uploadFilesEntity.setExtension(extension);
        uploadFilesEntity.setSize(file.getSize());
        uploadFilesEntity.setPath(filePath.toString());
        uploadFilesEntity.setUploadDate(LocalDateTime.now());

        try {
            uploadFilesRepository.save(uploadFilesEntity);
        } catch (RuntimeException e) {
            deleteQuietly(filePath);
            throw e;
        }

        return "Arquivo enviado com sucesso: " + originalName;
    }

    private void deleteQuietly(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Não foi possível remover o arquivo órfão {}", filePath, e);
        }
    }
}
