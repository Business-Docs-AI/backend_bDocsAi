package br.com.example.senac.businessDocsAi.uploadFiles.repository;

import br.com.example.senac.businessDocsAi.uploadFiles.entity.UploadFilesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IUploadFilesRepository extends JpaRepository<UploadFilesEntity, Long> {
}
