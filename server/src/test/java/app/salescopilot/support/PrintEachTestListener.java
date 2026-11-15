package app.salescopilot.support;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

// Imprime uma linha por teste: as provas de checks.md procuram "PASSED [Cnn]" na saída.
public class PrintEachTestListener implements TestExecutionListener {

    @Override
    public void executionFinished(TestIdentifier test, TestExecutionResult result) {
        if (!test.isTest()) {
            return;
        }
        String status = switch (result.getStatus()) {
            case SUCCESSFUL -> "PASSED";
            case FAILED -> "FAILED";
            case ABORTED -> "ABORTED";
        };
        System.out.println(status + " " + test.getDisplayName());
    }
}
