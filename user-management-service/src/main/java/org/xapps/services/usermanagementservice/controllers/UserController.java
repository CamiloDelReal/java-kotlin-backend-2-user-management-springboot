package org.xapps.services.usermanagementservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.xapps.services.usermanagementservice.dtos.*;
import org.xapps.services.usermanagementservice.entities.User;
import org.xapps.services.usermanagementservice.exceptions.DuplicityException;
import org.xapps.services.usermanagementservice.exceptions.InvalidCredentials;
import org.xapps.services.usermanagementservice.exceptions.NotFoundException;
import org.xapps.services.usermanagementservice.services.RoleService;
import org.xapps.services.usermanagementservice.services.UserService;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(path = "/users")
@Slf4j
public class UserController {
    private final UserService userService;
    private final RoleService roleService;

    @Autowired
    public UserController(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        ResponseEntity<LoginResponse> response = null;
        try {
            LoginResponse loginResponse = userService.login(loginRequest);
            response = ResponseEntity.ok(loginResponse);
        } catch (InvalidCredentials ex) {
            log.debug("Invalid credentials");
            response = new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        } catch (Exception ex) {
            log.error("Exception captured");
            response = ResponseEntity.internalServerError().build();
        }
        return response;
    }

    @GetMapping(path = "/roles", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        ResponseEntity<List<RoleResponse>> response = null;
        try {
            List<RoleResponse> roles = roleService.getAll();
            response = new ResponseEntity<>(roles, HttpStatus.OK);
        } catch (Exception ex) {
            response = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated() and hasAuthority('Administrator')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        ResponseEntity<List<UserResponse>> response = null;
        try {
            List<UserResponse> users = userService.getAll();
            response = ResponseEntity.ok(users);
        } catch (Exception ex) {
            log.error("Exception capture", ex);
            response = ResponseEntity.internalServerError().build();
        }
        return response;
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated() and hasAuthority('Administrator') or isAuthenticated() and principal.id == #id")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("id") Long id) {
        ResponseEntity<UserResponse> response = null;
        try {
            UserResponse user = userService.getById(id);
            response = ResponseEntity.ok(user);
        } catch (NotFoundException ex) {
            log.debug(ex.getMessage());
            response = ResponseEntity.notFound().build();
        } catch (Exception ex) {
            log.error("Exception captured", ex);
            response = ResponseEntity.internalServerError().build();
        }
        return response;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest userRequest) {
        ResponseEntity<UserResponse> response = null;
        try {
            boolean requestingCreateAdmin = userService.hasAdminRole(userRequest);
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if(!requestingCreateAdmin || (principal != null && principal instanceof User && userService.hasAdminRole(((User)principal)))) {
                UserResponse user = userService.create(userRequest);
                response = new ResponseEntity<>(user, HttpStatus.CREATED);
            } else {
                response = new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
        } catch (DuplicityException ex) {
            log.debug(ex.getMessage());
            response = ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            log.error("Exception captured", ex);
            response = ResponseEntity.internalServerError().build();
        }
        return response;
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated() and hasAuthority('Administrator') or isAuthenticated() and principal.id == #id")
    public ResponseEntity<UserResponse> editUser(@PathVariable("id") Long id, @Valid @RequestBody UserRequest userRequest) {
        ResponseEntity<UserResponse> response = null;
        try {
            boolean requestingCreateAdmin = userService.hasAdminRole(userRequest);
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if(!requestingCreateAdmin || (principal != null && principal instanceof User && userService.hasAdminRole(((User)principal)))) {
                UserResponse user = userService.edit(id, userRequest);
                response = ResponseEntity.ok(user);
            } else {
                response = new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
        } catch (NotFoundException ex) {
            log.debug(ex.getMessage());
            response = ResponseEntity.notFound().build();
        } catch (DuplicityException ex) {
            log.debug(ex.getMessage());
            response = ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            log.error("Exception captured", ex);
            response = ResponseEntity.internalServerError().build();
        }
        return response;
    }

    @DeleteMapping(path = "/{id}")
    @PreAuthorize("isAuthenticated() and hasAuthority('Administrator') or isAuthenticated() and principal.id == #id")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) {
        ResponseEntity<Void> response = null;
        try {
            userService.delete(id);
            response = ResponseEntity.ok().build();
        } catch (NotFoundException ex) {
            log.debug(ex.getMessage());
            response = ResponseEntity.notFound().build();
        } catch (Exception ex) {
            log.error("Exception captured", ex);
            response = ResponseEntity.internalServerError().build();
        }
        return response;
    }
}
