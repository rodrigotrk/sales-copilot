package app.salescopilot.shared;

/** Um erro de campo dentro de `errors` (AD-057). `itemId` é nulo para campos fora das listas. */
public record FieldProblem(String itemId, String field, String code, String message) {
}
