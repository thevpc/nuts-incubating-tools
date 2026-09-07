package net.thevpc.nmvn.lib.service;

import net.thevpc.nmvn.lib.model.MavenCoord;
import net.thevpc.nmvn.lib.model.PomChange;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BumpResult {
    private final List<PomChange> changes;
    private final Map<MavenCoord, String> bumpedArtifacts;

    public BumpResult(List<PomChange> changes, Map<MavenCoord, String> bumpedArtifacts) {
        this.changes = changes;
        this.bumpedArtifacts = bumpedArtifacts;
    }

    public List<PomChange> getChanges() {
        return Collections.unmodifiableList(changes);
    }

    public Map<MavenCoord, String> getBumpedArtifacts() {
        return Collections.unmodifiableMap(bumpedArtifacts);
    }

    public boolean hasChanges() {
        for (PomChange c : changes) {
            if (c.hasChanges()) {
                return true;
            }
        }
        return false;
    }
}
