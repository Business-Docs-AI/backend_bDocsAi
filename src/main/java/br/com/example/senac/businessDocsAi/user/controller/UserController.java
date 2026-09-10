package br.com.example.senac.businessDocsAi.user.controller;

import br.com.example.senac.businessDocsAi.user.dto.UserDTO;
import br.com.example.senac.businessDocsAi.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @PostMapping
    public ResponseEntity<UserDTO> create(
            @RequestBody UserDTO userDTO
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.create(userDTO));
    }


    @GetMapping
    public ResponseEntity<List<UserDTO>> findAll() {

        return ResponseEntity.ok(
                userService.findAll()
        );
    }


    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> findById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                userService.findById(id)
        );
    }


    @GetMapping("/email/{email}")
    public ResponseEntity<UserDTO> findByEmail(
            @PathVariable String email
    ) {

        return ResponseEntity.ok(
                userService.findByEmail(email)
        );
    }


    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> update(
            @PathVariable Long id,
            @RequestBody UserDTO userDTO
    ) {

        return ResponseEntity.ok(
                userService.update(id, userDTO)
        );
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {

        userService.delete(id);

        return ResponseEntity.noContent().build();
    }


    @PatchMapping("/{id}/activate")
    public ResponseEntity<UserDTO> activate(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                userService.activate(id)
        );
    }


    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<UserDTO> deactivate(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                userService.deactivate(id)
        );
    }
}