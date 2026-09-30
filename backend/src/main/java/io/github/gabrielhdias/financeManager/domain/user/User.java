package io.github.gabrielhdias.financeManager.domain.user;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    protected User() {
    }

    public User(String name, String email, String password) {
        this.name = validateName(name);
        this.email = validateEmail(email);
        this.password = validatePassword(password);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void rename(String name) {
        this.name = validateName(name);
    }

    public void changeEmail(String email) {
        this.email = validateEmail(email);
    }

    public void changePassword(String password) {
        this.password = validatePassword(password);
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                "O nome do usuário não pode estar vazio"
            );
        }

        return name.trim();
    }

    private String validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                "O email não pode estar vazio"
            );
        }

        return email.trim().toLowerCase();
    }

    private String validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                "A senha não pode estar vazia"
            );
        }

        return password;
    }
}
