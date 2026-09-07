package net.thevpc.nmvn.lib.exception;

import java.nio.file.Path;
import java.util.List;

public class AmbiguousArtifactException extends NMvnException {
    private final String groupId;
    private final String artifactId;
    private final List<Path> definingPoms;

    public AmbiguousArtifactException(String groupId, String artifactId, List<Path> definingPoms) {
        super(String.format("Ambiguous artifact %s:%s found in multiple locations: %s",
                groupId, artifactId, definingPoms));
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.definingPoms = definingPoms;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public List<Path> getDefiningPoms() {
        return definingPoms;
    }
}
