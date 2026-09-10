package br.com.example.senac.businessDocsAi.user.service;

import br.com.example.senac.businessDocsAi.user.dto.UserDTO;
import br.com.example.senac.businessDocsAi.user.entity.UserEntity;
import br.com.example.senac.businessDocsAi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;


    //Aqui é usado para criar um novo usuário
    public UserDTO create(UserDTO userDTO) {

        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new RuntimeException(
                    "Já existe um usuário cadastrado com este email: "
                            + userDTO.getEmail()
            );
        }

        UserEntity user = new UserEntity();

        user.setName(userDTO.getName());
        user.setEmail(userDTO.getEmail());
        user.setPermissionId(userDTO.getPermissionId());

        user.setCreatedAt(LocalDateTime.now());
        user.setUpdateAt(LocalDateTime.now());

        user.setActive(true);

        UserEntity savedUser = userRepository.save(user);

        return convertToDTO(savedUser);
    }

    //Traz todos os usuários
    public List<UserDTO> findAll() {

        return userRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    //Usado para buscar usuário pelo id
    public UserDTO findById(Long id) {

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Usuário não encontrado com o ID: " + id
                        )
                );

        return convertToDTO(user);
    }

    //Usado para buscar usuário pelo email
    public UserDTO findByEmail(String email) {

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Usuário não encontrado com o email: " + email
                        )
                );

        return convertToDTO(user);
    }

    //Atualiza o cadastro
    public UserDTO update(Long id, UserDTO userDTO) {

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Usuário não foi encontrado com este ID: " + id
                        )
                );

        if (!user.getEmail().equals(userDTO.getEmail())
                && userRepository.existsByEmail(userDTO.getEmail())) {

            throw new RuntimeException(
                    "Já existe um usuário cadastrado com este email: " + userDTO.getEmail()
            );
        }

        user.setName(userDTO.getName());
        user.setEmail(userDTO.getEmail());
        user.setPermissionId(userDTO.getPermissionId());

        user.setUpdateAt(LocalDateTime.now());

        UserEntity updatedUser = userRepository.save(user);

        return convertToDTO(updatedUser);
    }

    //Deletar um cadastro
    public void delete(Long id) {

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Usuário não encontrado com o ID: " + id
                        )
                );

        userRepository.delete(user);
    }

    //Ativar o cadastro
    public UserDTO activate(Long id) {

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Usuário não encontrado com o ID: " + id
                        )
                );

        user.setActive(true);
        user.setUpdateAt(LocalDateTime.now());

        UserEntity updatedUser = userRepository.save(user);

        return convertToDTO(updatedUser);
    }

    //Desativar o cadastro
    public UserDTO deactivate(Long id) {

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Usuário não encontrado com o ID: " + id
                        )
                );

        user.setActive(false);
        user.setUpdateAt(LocalDateTime.now());

        UserEntity updatedUser = userRepository.save(user);

        return convertToDTO(updatedUser);
    }

    //Converte de entity para DTO
    private UserDTO convertToDTO(UserEntity user) {

        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .permissionId(user.getPermissionId())
                .build();
    }
}