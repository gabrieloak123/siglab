package br.ufrn.dimap.siglab.controller;

import br.ufrn.dimap.siglab.exception.ResourceNotFoundException;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.service.UserService;
import br.ufrn.dimap.siglab.service.SessionUser;
import br.ufrn.dimap.siglab.service.AuthService;
import br.ufrn.dimap.siglab.dto.RegistrationRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST responsável pelos endpoints de gerenciamento de Usuários.
 *
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final SessionUser sessionUser;
    private final AuthService authService;

    public UserController(UserService userService, SessionUser sessionUser, AuthService authService) {
        this.userService = userService;
        this.sessionUser = sessionUser;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.findAll();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));
    }

    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        User createdUser = authService.register(new RegistrationRequest(
                user.getName(), user.getEmail(), user.getPassword(), user.getAuthorId(), user.getInterest_areas()));
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User userDetails, HttpServletRequest request) {
        if (!sessionUser.require(request).getId().equals(id)) throw new SecurityException("Só é possível editar o próprio perfil.");
        User updatedUser = userService.update(id, userDetails);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            HttpServletRequest request,
            @RequestParam(value = "password", required = false) String password) {
        if (!sessionUser.require(request).getId().equals(id)) throw new SecurityException("Só é possível excluir a própria conta.");
        userService.deleteById(id, password);
        return ResponseEntity.noContent().build();
    }
}
