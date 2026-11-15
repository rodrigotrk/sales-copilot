package app.salescopilot.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import app.salescopilot.support.IntegrationTest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProblemsTest extends IntegrationTest {

    @Test
    @DisplayName("[C97] erros do Spring saem em RFC 9457 com code e type")
    void springErrorsCarryCodeAndType() throws Exception {
        Gestor g = newGestor();

        mvc.perform(post("/api/businesses").cookie(g.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.code").value("malformed_request"))
                .andExpect(jsonPath("$.detail").value("O corpo da requisição não é um JSON válido."))
                .andExpect(jsonPath("$.type").value("/problems/malformed_request"))
                .andExpect(jsonPath("$.instance").value("/api/businesses"));
        mvc.perform(post("/api/businesses").cookie(g.session()).with(csrf()).contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("unsupported_media_type"))
                .andExpect(jsonPath("$.detail").value("Tipo de conteúdo não aceito."));
        mvc.perform(delete("/api/businesses").cookie(g.session()).with(csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("method_not_allowed"))
                .andExpect(jsonPath("$.detail").value("Método não permitido neste endereço."));
        mvc.perform(multipart("/api/businesses/" + g.businessId() + "/playbook/imports").file("file", "x".getBytes())
                .cookie(g.session()).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("missing_parameter"))
                .andExpect(jsonPath("$.detail").value("Falta um parâmetro obrigatório."));
    }
}
