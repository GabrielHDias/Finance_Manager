package io.github.gabrielhdias.financeManager.api.tag;

import io.github.gabrielhdias.financeManager.api.tag.dto.TagRequest;
import io.github.gabrielhdias.financeManager.api.tag.dto.TagResponse;
import io.github.gabrielhdias.financeManager.domain.tag.Tag;
import io.github.gabrielhdias.financeManager.domain.tag.TagService;
import io.github.gabrielhdias.financeManager.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tags")
@SecurityRequirement(name = "bearerAuth")
public class TagController {
    private final TagService tagService;
    private final AuthenticatedUser authenticatedUser;

    public TagController(TagService tagService, AuthenticatedUser authenticatedUser) {
        this.tagService = tagService;
        this.authenticatedUser = authenticatedUser;
    }

    @PostMapping
    public ResponseEntity<TagResponse> create(@Valid @RequestBody TagRequest request) {
        Tag tag = tagService.create(authenticatedUser.getId(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(tag));
    }

    @GetMapping
    public List<TagResponse> list() {
        return tagService.listByUser(authenticatedUser.getId()).stream().map(TagController::toResponse).toList();
    }

    @PutMapping("/{id}")
    public TagResponse rename(@PathVariable Long id, @Valid @RequestBody TagRequest request) {
        return toResponse(tagService.rename(authenticatedUser.getId(), id, request.name()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tagService.delete(authenticatedUser.getId(), id);
        return ResponseEntity.noContent().build();
    }

    private static TagResponse toResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }
}
