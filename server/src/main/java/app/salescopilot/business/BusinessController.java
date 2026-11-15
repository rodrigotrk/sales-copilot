package app.salescopilot.business;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import app.salescopilot.auth.api.CurrentUser;
import app.salescopilot.business.api.BusinessCreated;
import app.salescopilot.business.domain.BusinessNames;
import app.salescopilot.shared.ApiException;
import app.salescopilot.shared.FieldProblem;
import app.salescopilot.shared.Tenancy;

@RestController
class BusinessController {

    static final String LAST_BUSINESS = "business.lastBusinessId";

    record BusinessView(UUID id, String name, String role) {
    }

    record UserView(String email) {
    }

    record Me(UserView user, List<BusinessView> businesses, UUID lastBusinessId, boolean canCreateBusiness) {
    }

    record NewBusiness(String businessName, String playbookName) {
    }

    private final Tenancy tenancy;
    private final BusinessStore store;
    private final ApplicationEventPublisher events;

    BusinessController(Tenancy tenancy, BusinessStore store, ApplicationEventPublisher events) {
        this.tenancy = tenancy;
        this.store = store;
        this.events = events;
    }

    @GetMapping("/api/me")
    Me me(CurrentUser user, HttpSession session) {
        return tenancy.asUser(user.id(), () -> {
            List<BusinessView> businesses = store.membershipsOf(user.id()).stream()
                    .map(m -> new BusinessView(m.businessId(), m.name(), m.role()))
                    .toList();
            UUID last = session.getAttribute(LAST_BUSINESS) instanceof String id ? UUID.fromString(id) : null;
            UUID lastStillValid = businesses.stream().anyMatch(b -> b.id().equals(last)) ? last : null;
            boolean canCreate = businesses.isEmpty() && store.isAllowedToSignUp(user.email());
            return new Me(new UserView(user.email()), businesses, lastStillValid, canCreate);
        });
    }

    @PostMapping("/api/businesses")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> create(CurrentUser user, @RequestBody NewBusiness request, HttpSession session) {
        UUID businessId = UUID.randomUUID();
        tenancy.runInBusiness(user.id(), businessId, () -> {
            store.lockUser(user.id());
            if (store.hasAnyMembership(user.id())) {
                throw new ApiException(HttpStatus.CONFLICT, "already_member", "Você já participa de um negócio.");
            }
            if (!store.isAllowedToSignUp(user.email())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "signup_not_allowed", "Seu acesso ainda não foi liberado.");
            }
            validate(request);
            store.create(businessId, request.businessName().strip(), user.id());
            events.publishEvent(new BusinessCreated(businessId, request.playbookName().strip()));
        });
        session.setAttribute(LAST_BUSINESS, businessId.toString());
        return Map.of("businessId", businessId);
    }

    private static void validate(NewBusiness request) {
        var errors = new java.util.ArrayList<FieldProblem>();
        if (!BusinessNames.isValid(request.businessName())) {
            errors.add(new FieldProblem(null, "businessName", "invalid_length", "Informe de 1 a 80 caracteres."));
        }
        if (!BusinessNames.isValid(request.playbookName())) {
            errors.add(new FieldProblem(null, "playbookName", "invalid_length", "Informe de 1 a 80 caracteres."));
        }
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "invalid_fields", "Corrija os campos destacados.", errors);
        }
    }
}
