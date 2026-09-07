package net.thevpc.nmvn.lib.diagnostic;

import net.thevpc.nmvn.lib.model.MavenCoord;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class DiagnosticIssue {
    private final DiagnosticRule rule;
    private final DiagnosticSeverity severity;
    private final MavenCoord targetCoord;
    private final Path sourcePom;
    private final String message;
    private final Map<String, String> details;

    public DiagnosticIssue(DiagnosticRule rule, DiagnosticSeverity severity, MavenCoord targetCoord,
                           Path sourcePom, String message) {
        this(rule, severity, targetCoord, sourcePom, message, Collections.emptyMap());
    }

    public DiagnosticIssue(DiagnosticRule rule, DiagnosticSeverity severity, MavenCoord targetCoord,
                           Path sourcePom, String message, Map<String, String> details) {
        this.rule = rule;
        this.severity = severity;
        this.targetCoord = targetCoord;
        this.sourcePom = sourcePom;
        this.message = message;
        this.details = details != null ? new LinkedHashMap<>(details) : Collections.emptyMap();
    }

    public DiagnosticRule getRule() {
        return rule;
    }

    public DiagnosticSeverity getSeverity() {
        return severity;
    }

    public MavenCoord getTargetCoord() {
        return targetCoord;
    }

    public Path getSourcePom() {
        return sourcePom;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getDetails() {
        return Collections.unmodifiableMap(details);
    }

    @Override
    public String toString() {
        return "[" + severity + "] " + rule + ": " + message + (sourcePom != null ? " (" + sourcePom + ")" : "");
    }
}
