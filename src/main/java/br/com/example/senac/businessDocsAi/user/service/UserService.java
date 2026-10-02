package br.com.example.senac.businessDocsAi.user.service;

import br.com.example.senac.businessDocsAi.categories.entity.UsuarioCategoriaEntity;
import br.com.example.senac.businessDocsAi.categories.repository.IUsuarioCategoriaRepository;
import br.com.example.senac.businessDocsAi.exception.BadRequestException;
import br.com.example.senac.businessDocsAi.exception.NotFoundException;
import br.com.example.senac.businessDocsAi.user.dto.UserDTO;
import br.com.example.senac.businessDocsAi.user.entity.UserEntity;
import br.com.example.senac.businessDocsAi.user.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final IUserRepository IUserRepository;
    private final IUsuarioCategoriaRepository usuarioCategoriaRepository;
    private final PasswordEncoder passwordEncoder;


    //Aqui é usado para criar um novo usuário
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserDTO create(UserDTO userDTO) throws BadRequestException {

        if (IUserRepository.existsByEmail(userDTO.getEmail())) {
            throw new BadRequestException("Já existe um usuário cadastrado com este email: " + userDTO.getEmail());
        }

        if (userDTO.getPassword() == null || userDTO.getPassword().isBlank()) {
            throw new BadRequestException("A senha é obrigatória");
        }

        UserEntity user = new UserEntity();

        user.setName(userDTO.getName());
        user.setEmail(userDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setRole(userDTO.getRole());
        user.setPermissionId(userDTO.getPermissionId());

        user.setCreatedAt(LocalDateTime.now());
        user.setUpdateAt(LocalDateTime.now());

        user.setActive(true);

        UserEntity savedUser = IUserRepository.save(user);

        salvarCategoriasDoUsuario(savedUser.getId(), userDTO.getCategoriaIds());

        return convertToDTO(savedUser);
    }

    //Traz todos os usuários
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDTO> findAll() throws BadRequestException {

        return IUserRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    //Usado para buscar usuário pelo id
    @PreAuthorize("hasRole('ADMIN')")
    public UserDTO findById(Long id) throws NotFoundException {

        UserEntity user = IUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com o ID: " + id));

        return convertToDTO(user);
    }

    //Usado para buscar usuário pelo email
    @PreAuthorize("hasRole('ADMIN')")
    public UserDTO findByEmail(String email) throws NotFoundException {

        UserEntity user = IUserRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com o email: " + email));

        return convertToDTO(user);
    }

    //Atualiza o cadastro
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserDTO update(Long id, UserDTO userDTO) throws BadRequestException {

        UserEntity user = IUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não foi encontrado com este ID: " + id));

        if (!user.getEmail().equals(userDTO.getEmail()) && IUserRepository.existsByEmail(userDTO.getEmail())) {
            throw new BadRequestException("Já existe um usuário cadastrado com este email: " + userDTO.getEmail());
        }

        user.setName(userDTO.getName());
        user.setEmail(userDTO.getEmail());
        user.setRole(userDTO.getRole());
        user.setPermissionId(userDTO.getPermissionId());

        if (userDTO.getPassword() != null && !userDTO.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        user.setUpdateAt(LocalDateTime.now());

        UserEntity updatedUser = IUserRepository.save(user);

        salvarCategoriasDoUsuario(updatedUser.getId(), userDTO.getCategoriaIds());

        return convertToDTO(updatedUser);
    }

    //Deletar um cadastro
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(Long id) throws NotFoundException {

        UserEntity user = IUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com o ID: " + id));

        usuarioCategoriaRepository.deleteByUsuarioId(id);
        IUserRepository.delete(user);
    }

    //Ativar o cadastro
    @PreAuthorize("hasRole('ADMIN')")
    public UserDTO activate(Long id) throws NotFoundException {

        UserEntity user = IUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com o ID: " + id));

        user.setActive(true);
        user.setUpdateAt(LocalDateTime.now());

        UserEntity updatedUser = IUserRepository.save(user);

        return convertToDTO(updatedUser);
    }

    //Desativar o cadastro
    @PreAuthorize("hasRole('ADMIN')")
    public UserDTO deactivate(Long id) throws NotFoundException {

        UserEntity user = IUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com o ID: " + id));

        user.setActive(false);
        user.setUpdateAt(LocalDateTime.now());

        UserEntity updatedUser = IUserRepository.save(user);

        return convertToDTO(updatedUser);
    }

    // Substitui por completo o conjunto de categorias do usuário pelas informadas (lista
    // vazia ou nula remove todas).
    private void salvarCategoriasDoUsuario(Long usuarioId, List<Long> categoriaIds) {

        usuarioCategoriaRepository.deleteByUsuarioId(usuarioId);

        if (categoriaIds == null || categoriaIds.isEmpty()) {
            return;
        }

        List<UsuarioCategoriaEntity> vinculos = categoriaIds.stream()
                .distinct()
                .map(categoriaId -> new UsuarioCategoriaEntity(usuarioId, categoriaId))
                .toList();

        usuarioCategoriaRepository.saveAll(vinculos);
    }

    //Converte de entity para DTO
    private UserDTO convertToDTO(UserEntity user) throws BadRequestException {

        List<Long> categoriaIds = usuarioCategoriaRepository.findByUsuarioId(user.getId())
                .stream()
                .map(UsuarioCategoriaEntity::getCategoriaId)
                .toList();

        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .permissionId(user.getPermissionId())
                .categoriaIds(categoriaIds)
                .build();
    }
}