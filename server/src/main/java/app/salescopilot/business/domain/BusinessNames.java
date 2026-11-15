package app.salescopilot.business.domain;

public final class BusinessNames {

    public static final int MAX_LENGTH = 80;

    private BusinessNames() {
    }

    /** Nome de negócio ou de playbook: obrigatório, de 1 a 80 caracteres depois de tirar os espaços das pontas. */
    public static boolean isValid(String name) {
        if (name == null) {
            return false;
        }
        int length = name.strip().length();
        return length >= 1 && length <= MAX_LENGTH;
    }
}
