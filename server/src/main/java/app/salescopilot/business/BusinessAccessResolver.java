package app.salescopilot.business;

import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import app.salescopilot.auth.api.CurrentUser;
import app.salescopilot.business.api.BusinessAccess;
import app.salescopilot.shared.ApiException;
import app.salescopilot.shared.Tenancy;

/**
 * Confere, a cada requisição, se a pessoa é membro do `businessId` do endereço (AD-045). Quem não é
 * recebe 404, para não revelar que o negócio existe.
 */
@Component
class BusinessAccessResolver implements HandlerMethodArgumentResolver {

    private final Tenancy tenancy;
    private final BusinessStore store;

    BusinessAccessResolver(Tenancy tenancy, BusinessStore store) {
        this.tenancy = tenancy;
        this.store = store;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == BusinessAccess.class;
    }

    @Override
    public BusinessAccess resolveArgument(MethodParameter parameter, ModelAndViewContainer mav, NativeWebRequest web,
            WebDataBinderFactory binders) throws Exception {
        HttpServletRequest request = web.getNativeRequest(HttpServletRequest.class);
        CurrentUser user = CurrentUser.require(request);
        @SuppressWarnings("unchecked")
        Map<String, String> variables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        UUID businessId;
        try {
            businessId = UUID.fromString(variables.get("businessId"));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw ApiException.notFound();
        }
        String role = tenancy.asUser(user.id(), () -> store.roleIn(user.id(), businessId)).orElseThrow(ApiException::notFound);
        request.getSession().setAttribute(BusinessController.LAST_BUSINESS, businessId.toString());
        return new BusinessAccess(user.id(), businessId, role);
    }

}
