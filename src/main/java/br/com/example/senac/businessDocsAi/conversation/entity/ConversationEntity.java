package br.com.example.senac.businessDocsAi.conversation.entity;

import br.com.example.senac.businessDocsAi.uploadFiles.entity.UploadFilesEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversation")
public class ConversationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private LocalDateTime createdAt;

    @OneToMany
    @JoinTable(name = "conversation_file", joinColumns = @JoinColumn(name = "conversation_id"), inverseJoinColumns = @JoinColumn(name = "file_id"))
    private List<UploadFilesEntity> uploadFilesEntities = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<UploadFilesEntity> getFiles() {
        return uploadFilesEntities;
    }

    public void setFiles(List<UploadFilesEntity> uploadFilesEntities) {
        this.uploadFilesEntities = uploadFilesEntities;
    }
}
