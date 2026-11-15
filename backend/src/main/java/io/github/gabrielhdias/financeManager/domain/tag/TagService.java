package io.github.gabrielhdias.financeManager.domain.tag;

import io.github.gabrielhdias.financeManager.domain.exception.DuplicateResourceException;
import io.github.gabrielhdias.financeManager.domain.exception.ResourceNotFoundException;
import io.github.gabrielhdias.financeManager.domain.user.User;
import io.github.gabrielhdias.financeManager.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TagService {
    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    public TagService(TagRepository tagRepository, UserRepository userRepository) {
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Tag create(Long userId, String name) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        String normalized = name.trim();
        if (tagRepository.existsByUserIdAndName(userId, normalized)) {
            throw new DuplicateResourceException("Já existe uma etiqueta com esse nome para o usuário");
        }
        return tagRepository.save(new Tag(normalized, user));
    }

    @Transactional(readOnly = true)
    public List<Tag> listByUser(Long userId) {
        return tagRepository.findAllByUserIdOrderByNameAsc(userId);
    }

    @Transactional
    public Tag rename(Long userId, Long tagId, String name) {
        Tag tag = tagRepository.findByIdAndUserId(tagId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Etiqueta não encontrada"));
        String normalized = name.trim();
        if (!tag.getName().equals(normalized) && tagRepository.existsByUserIdAndName(userId, normalized)) {
            throw new DuplicateResourceException("Já existe uma etiqueta com esse nome para o usuário");
        }
        tag.rename(normalized);
        return tag;
    }

    @Transactional
    public void delete(Long userId, Long tagId) {
        Tag tag = tagRepository.findByIdAndUserId(tagId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Etiqueta não encontrada"));
        tagRepository.delete(tag);
    }
}
