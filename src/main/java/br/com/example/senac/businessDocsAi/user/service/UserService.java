package br.com.example.senac.businessDocsAi.user.service;

import br.com.example.senac.businessDocsAi.exception.BadRequestException;
import br.com.example.senac.businessDocsAi.exception.NotFoundException;
import br.com.example.senac.businessDocsAi.user.dto.UserDTO;
import br.com.example.senac.businessDocsAi.user.entity.UserEntity;
import br.com.example.senac.businessDocsAi.user.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final IUserRepository IUserRepository;


    //Aqui é usado para criar um novo usuário
    public UserDTO create(UserDTO userDTO) throws BadRequestException {

        if (IUserRepository.existsByEmail(userDTO.getEmail())) {
            throw new BadRequestException("Já existe um usuário cadastrado com este email: " + userDTO.getEmail());
        }

        UserEntity user = new UserEntity();

        user.setName(userDTO.getName());
        user.setEmail(userDTO.getEmail());
        user.setPermissionId(userDTO.getPermissionId());

        user.setCreatedAt(LocalDateTime.now());
        user.setUpdateAt(LocalDateTime.now());

        user.setActive(true);

        UserEntity savedUser = IUserRepository.save(user);

        return convertToDTO(savedUser);
    }

    //Traz todos os usuários
    public List<UserDTO> findAll() throws BadRequestException {

        return IUserRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    //Usado para buscar usuário pelo id
    public UserDTO findById(Long id) throws NotFoundException {

        UserEntity user = IUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com o ID: " + id));

        return convertToDTO(user);
    }

    //Usado para buscar usuário pelo email
    public UserDTO findByEmail(String email) throws NotFoundException {

        UserEntity user = IUserRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com o email: " + email));

        return convertToDTO(user);
    }

    //Atualiza o cadastro
    public UserDTO update(Long id, UserDTO userDTO) throws BadRequestException {

        UserEntity user = IUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não foi encontrado com este ID: " + id));

        if (!user.getEmail().equals(userDTO.getEmail()) && IUserRepository.existsByEmail(userDTO.getEmail())) {
            throw new BadRequestException("Já existe um usuário cadastrado com este email: " + userDTO.getEmail());
        }

        user.setName(userDTO.getName());
        user.setEmail(userDTO.getEmail());
        user.setPermissionId(userDTO.getPermissionId());

        user.setUpdateAt(LocalDateTime.now());

        UserEntity updatedUser = IUserRepository.save(user);

        return convertToDTO(updatedUser);
    }

    //Deletar um cadastro
    public void delete(Long id) throws NotFoundException {

        UserEntity user = IUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com o ID: " + id));

        IUserRepository.delete(user);
    }

    //Ativar o cadastro
    public UserDTO activate(Long id) throws NotFoundException {

        UserEntity user = IUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com o ID: " + id));

        user.setActive(true);
        user.setUpdateAt(LocalDateTime.now());

        UserEntity updatedUser = IUserRepository.save(user);

        return convertToDTO(updatedUser);
    }

    //Desativar o cadastro
    public UserDTO deactivate(Long id) throws NotFoundException {

        UserEntity user = IUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com o ID: " + id));

        user.setActive(false);
        user.setUpdateAt(LocalDateTime.now());

        UserEntity updatedUser = IUserRepository.save(user);

        return convertToDTO(updatedUser);
    }

    //Converte de entity para DTO
    private UserDTO convertToDTO(UserEntity user) throws BadRequestException {

        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .permissionId(user.getPermissionId())
                .build();
    }
}