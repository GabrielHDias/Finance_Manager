package io.github.gabrielhdias.financeManager.api.category;

import io.github.gabrielhdias.financeManager.config.security.SecurityConfig;
import io.github.gabrielhdias.financeManager.domain.category.Category;
import io.github.gabrielhdias.financeManager.domain.category.CategoryService;
import io.github.gabrielhdias.financeManager.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
class CategoryControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private AuthenticatedUser authenticatedUser;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldReturnUnauthorizedWhenRequestHasNoToken() throws Exception {
        mockMvc.perform(
                get("/categories")
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateCategoryWhenAuthenticated() throws Exception {
        Long userId = 1L;

        Category category = mock(Category.class);

        when(authenticatedUser.getId())
            .thenReturn(userId);

        when(category.getId())
            .thenReturn(10L);

        when(category.getName())
            .thenReturn("Alimentação");

        when(categoryService.create(
            userId,
            "Alimentação"
        )).thenReturn(category);

        mockMvc.perform(
                post("/categories")
                    .with(jwt().jwt(jwt ->
                        jwt.subject(userId.toString())
                    ))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "name": "Alimentação"
                                }
                                """)
            )
            .andExpect(status().isCreated())
            .andExpect(
                content().contentTypeCompatibleWith(
                    MediaType.APPLICATION_JSON
                )
            )
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.name")
                .value("Alimentação"));
    }

    @Test
    void shouldReturnBadRequestWhenCategoryNameIsBlank() throws Exception {
        when(authenticatedUser.getId())
            .thenReturn(1L);

        mockMvc.perform(
                post("/categories")
                    .with(jwt().jwt(jwt ->
                        jwt.subject("1")
                    ))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "name": ""
                                }
                                """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error")
                .value("Validation Error"))
            .andExpect(jsonPath("$.message")
                .value("Dados inválidos"))
            .andExpect(jsonPath("$.fields.name").exists());
    }
}
