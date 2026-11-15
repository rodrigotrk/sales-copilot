package app.salescopilot.business.api;

import java.util.UUID;

import app.salescopilot.shared.ApiException;

/**
 * Negócio do endereço da requisição, já conferido contra a associação da pessoa (AD-045). Um
 * controller que recebe este parâmetro só roda para membros do negócio.
 */
public record BusinessAccess(UUID userId, UUID businessId, String role) {

    public void requireGestor() {
        if (!"gestor".equals(role)) {
            throw ApiException.forbidden();
        }
    }
}
