package br.com.example.senac.businessDocsAi.user.dto;

import br.com.example.senac.businessDocsAi.user.Enum.UserEnum;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class UserDTO {

    private Long id;

    @NotBlank(message = "O nome é obrigatório")
    private String name;

    @NotBlank(message = "O email é obrigatório")
    @Email(message = "O email informado é inválido")
    private String email;

    // Só aceito na entrada (criação/atualização); nunca devolvido nas respostas.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @NotNull(message = "O perfil (role) é obrigatório")
    private UserEnum role;

    private Long permissionId;

    // Categorias às quais o usuário tem acesso (consulta/criação/atualização de
    // documentos). ADMIN acessa todas as categorias independentemente desta lista.
    @Builder.Default
    private List<Long> categoriaIds = List.of();

}
