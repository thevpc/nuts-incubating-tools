package net.thevpc.nmvn.lib.service;

import net.thevpc.nmvn.lib.graph.MavenDependencyGraph;
import net.thevpc.nmvn.lib.model.MavenCoord;
import net.thevpc.nmvn.lib.model.PomArtifact;

import java.util.Collections;
import java.util.Map;

public class ScanResult {
    private final Map<MavenCoord, PomArtifact> artifacts;
    private final MavenDependencyGraph graph;

    public ScanResult(Map<MavenCoord, PomArtifact> artifacts, MavenDependencyGraph graph) {
        this.artifacts = artifacts;
        this.graph = graph;
    }

    public Map<MavenCoord, PomArtifact> getArtifacts() {
        return Collections.unmodifiableMap(artifacts);
    }

    public MavenDependencyGraph getGraph() {
        return graph;
    }
}
