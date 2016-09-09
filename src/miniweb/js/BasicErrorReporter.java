package miniweb.js;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

public class BasicErrorReporter implements ErrorReporter {

    private final String location;

    public BasicErrorReporter(String location) {
        this.location = location;
    }
    
    @Override
    public void warning(String message, String sourceName,
            int line, String lineSource, int lineOffset) {
        // Ignore
    }

    @Override
    public void error(String message, String sourceName,
            int line, String lineSource, int lineOffset) {
        System.err.println("[ERROR] in " + location);
        if (line < 0) {
            System.err.println("  " + message);
        } else {
            System.err.println("  " + line + ':' + lineOffset + ':' + message);
        }
    }

    @Override
    public EvaluatorException runtimeError(String message, String sourceName,
            int line, String lineSource, int lineOffset) {
        error(message, sourceName, line, lineSource, lineOffset);
        return new EvaluatorException(message);
    }
}
