package br.com.example.senac.businessDocsAi.uploadFiles.controller;

import br.com.example.senac.businessDocsAi.uploadFiles.service.UploadFilesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
public class UploadFilesController {

    private final UploadFilesService uploadFilesService;

    public UploadFilesController(UploadFilesService uploadFilesService) {
        this.uploadFilesService = uploadFilesService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("File cannot be empty");
        }

        String contentType = file.getContentType();

        if (!"application/pdf".equalsIgnoreCase(contentType)
                && !"application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                .equalsIgnoreCase(contentType)) {

            return ResponseEntity.badRequest()
                    .body("Only PDF and DOCX files are allowed");
        }

        return ResponseEntity.ok(
                uploadFilesService.processFile(file)
        );
    }
}