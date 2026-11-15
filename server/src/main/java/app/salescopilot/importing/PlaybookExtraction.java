package app.salescopilot.importing;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel.ResponseFormat;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import app.salescopilot.importing.domain.ExtractedPlaybook;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pede ao Claude, pelo OpenRouter, os blocos do playbook em JSON (AD-007, AD-027). Toda chamada vai
 * com retenção zero de dados obrigatória (`provider.zdr`).
 */
@Component
class PlaybookExtraction {

    static final int ATTEMPTS = 3;

    private static final String INSTRUCTIONS = """
            Você extrai o playbook de vendas de um documento de uma empresa. Responda só com o JSON pedido.
            Regras:
            - Use apenas o que está escrito no documento; nunca invente etapas, perguntas, objeções, planos ou valores.
            - stages: as etapas da call de venda, na ordem do documento.
            - discovery: o que o vendedor precisa descobrir na call (information) e a pergunta sugerida (question).
            - objections: a objeção (name), como o lead costuma dizer (leadSays, vazio se o documento não disser) e a resposta aprovada (approvedResponse).
            - priceTable.plans: cada plano com nome, o valor de uma coluna extra se houver (extraValue, ex.: número de encontros), preço à vista (cash), número de parcelas e valor de cada parcela, como números com ponto decimal, exatamente como escritos.
            - excerpt: o trecho literal do documento de onde saiu o preço do plano ou o desconto.
            - priceTable.extraColumnName: o nome da coluna extra da tabela de preços, se houver.
            - priceTable.maxDiscount: o desconto máximo em reais permitido na call, se o documento disser.
            - Use null para o que o documento não traz.
            """;

    private static final String SCHEMA = """
            {"type":"object","additionalProperties":false,"required":["stages","discovery","objections","priceTable"],
             "properties":{
              "stages":{"type":"array","items":{"type":"object","additionalProperties":false,"required":["name"],
                "properties":{"name":{"type":"string"}}}},
              "discovery":{"type":"array","items":{"type":"object","additionalProperties":false,"required":["information","question"],
                "properties":{"information":{"type":"string"},"question":{"type":"string"}}}},
              "objections":{"type":"array","items":{"type":"object","additionalProperties":false,"required":["name","leadSays","approvedResponse"],
                "properties":{"name":{"type":"string"},"leadSays":{"type":["string","null"]},"approvedResponse":{"type":"string"}}}},
              "priceTable":{"type":"object","additionalProperties":false,"required":["extraColumnName","maxDiscount","maxDiscountExcerpt","plans"],
                "properties":{"extraColumnName":{"type":["string","null"]},"maxDiscount":{"type":["number","null"]},
                  "maxDiscountExcerpt":{"type":["string","null"]},
                  "plans":{"type":"array","items":{"type":"object","additionalProperties":false,
                    "required":["name","extraValue","cash","installmentsCount","installmentsValue","excerpt"],
                    "properties":{"name":{"type":"string"},"extraValue":{"type":["string","null"]},"cash":{"type":["number","null"]},
                      "installmentsCount":{"type":["integer","null"]},"installmentsValue":{"type":["number","null"]},
                      "excerpt":{"type":["string","null"]}}}}}}}}
            """;

    private final ChatModel chat;
    private final JsonMapper json;

    PlaybookExtraction(ChatModel chat, JsonMapper json) {
        this.chat = chat;
        this.json = json;
    }

    /** Resposta fora do schema pedido é falha da tentativa, como um erro HTTP (critério 81). */
    ExtractedPlaybook parse(String answer) {
        JsonNode root = json.readTree(answer);
        // Tipo e nulidade são conferidos no JSON bruto: o record transforma bloco nulo em lista vazia.
        boolean shaped = root != null && root.isObject()
                && List.of("stages", "discovery", "objections").stream().allMatch(b -> root.path(b).isArray())
                && root.path("priceTable").isObject() && root.path("priceTable").path("plans").isArray();
        if (!shaped) {
            throw new IllegalArgumentException("resposta fora do schema");
        }
        ExtractedPlaybook extracted = json.readerFor(ExtractedPlaybook.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(root);
        if (!extracted.isWellFormed()) {
            throw new IllegalArgumentException("item sem campo obrigatório do schema");
        }
        return extracted;
    }

    ExtractedPlaybook extract(String documentText) throws ImportFailure {
        var options = OpenAiChatOptions.builder()
                .responseFormat(ResponseFormat.builder()
                        .type(ResponseFormat.Type.JSON_SCHEMA)
                        .jsonSchema(SCHEMA)
                        .strict(true)
                        .build())
                .extraBody(Map.of("provider", Map.of("zdr", true)))
                .build();
        var prompt = new Prompt(List.of(new SystemMessage(INSTRUCTIONS), new UserMessage(documentText)), options);
        RuntimeException last = null;
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                String answer = chat.call(prompt).getResult().getOutput().getText();
                return parse(answer);
            } catch (RuntimeException e) {
                last = e;
            }
        }
        throw new ImportFailure("model_failed", last);
    }
}
