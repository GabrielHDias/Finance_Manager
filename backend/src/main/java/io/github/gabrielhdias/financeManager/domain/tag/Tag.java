package io.github.gabrielhdias.financeManager.domain.tag;

import io.github.gabrielhdias.financeManager.domain.user.User;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected Tag() {
    }

    public Tag(String name, User user) {
        this.name = validateName(name);
        this.user = Objects.requireNonNull(user, "O usuário da etiqueta não pode ser nulo");
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public User getUser() {
        return user;
    }

    public void rename(String name) {
        this.name = validateName(name);
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome da etiqueta não pode estar vazio");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 40) {
            throw new IllegalArgumentException("O nome da etiqueta deve possuir no máximo 40 caracteres");
        }
        return trimmed;
    }
}
