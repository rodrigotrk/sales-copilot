package app.salescopilot.playbook.api;

import java.math.BigDecimal;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/** Inteiro do JSON sem truncar: 6 e 6.0 viram 6; 60.9 é recusado em vez de virar 60. */
public class WholeNumber extends ValueDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) {
        if (!parser.currentToken().isNumeric()) {
            return (Integer) context.handleUnexpectedToken(Integer.class, parser);
        }
        BigDecimal value = parser.getDecimalValue();
        if (value.stripTrailingZeros().scale() > 0) {
            throw context.weirdNumberException(value, Integer.class, "precisa ser um número inteiro");
        }
        try {
            return value.intValueExact();
        } catch (ArithmeticException e) {
            throw context.weirdNumberException(value, Integer.class, "fora do intervalo de um inteiro");
        }
    }
}
