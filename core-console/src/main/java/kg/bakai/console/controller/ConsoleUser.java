package kg.bakai.console.controller;

/** The console user of a request, from the X-User header. */
final class ConsoleUser {

    static final String HEADER = "X-User";
    private static final String UNKNOWN = "unknown";

    private ConsoleUser() {
    }

    static String orUnknown(String user) {
        return user == null || user.isBlank() ? UNKNOWN : user.trim();
    }
}
