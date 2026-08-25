package br.com.example.senac.businessDocsAi.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class UserDTO {

    private Long id;
    private String name;
    private String email;
    private Long permissionId;

}
