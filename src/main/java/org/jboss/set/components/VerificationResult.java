package org.jboss.set.components;

import java.util.ArrayList;
import java.util.List;

public class VerificationResult {

    private List<Violation> violations = new ArrayList<>();
    private List<Warning> warnings = new ArrayList<>();

    public VerificationResult() {
    }

    public void addWarning(Warning warning) {
        warnings.add(warning);
    }

    public void addViolation(Violation violation) {
        violations.add(violation);
    }

    public List<Violation> getViolations() {
        return violations;
    }

    public List<Warning> getWarnings() {
        return warnings;
    }
}
