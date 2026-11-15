package app.salescopilot.importing;

/** Falha que encerra a tarefa em `failed` com o código que o app web traduz em mensagem. */
class ImportFailure extends Exception {

    private final String code;

    ImportFailure(String code) {
        super(code);
        this.code = code;
    }

    ImportFailure(String code, Throwable cause) {
        super(code, cause);
        this.code = code;
    }

    String code() {
        return code;
    }
}
