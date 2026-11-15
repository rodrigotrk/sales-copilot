package app.salescopilot.shared;

import java.math.BigDecimal;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * BigDecimal só aparece como dinheiro neste produto, e sai em JSON como texto decimal ("4500.00",
 * AD-025): o JavaScript do app web leria um número JSON como ponto flutuante.
 */
@Configuration(proxyBeanMethods = false)
public class MoneyJsonConfiguration {

    @Bean
    JsonMapperBuilderCustomizer moneyAsText() {
        SimpleModule module = new SimpleModule("money-as-text");
        module.addSerializer(BigDecimal.class, new ValueSerializer<BigDecimal>() {
            @Override
            public void serialize(BigDecimal value, JsonGenerator generator, SerializationContext context) {
                generator.writeString(value.toPlainString());
            }
        });
        return builder -> builder.addModule(module);
    }
}
