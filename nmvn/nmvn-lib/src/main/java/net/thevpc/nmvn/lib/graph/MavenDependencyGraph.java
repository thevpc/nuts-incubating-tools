package net.thevpc.nmvn.lib.graph;

import net.thevpc.nmvn.lib.exception.CycleDetectedException;
import net.thevpc.nmvn.lib.model.DependencyEdgeType;
import net.thevpc.nmvn.lib.model.MavenCoord;
import net.thevpc.nmvn.lib.model.PomArtifact;
import net.thevpc.nmvn.lib.model.PomDependency;

import java.util.*;

public class MavenDependencyGraph {
    private final Map<MavenCoord, PomArtifact> artifacts = new LinkedHashMap<>();
    private final Map<MavenCoord, List<DependencyEdge>> outgoingEdges = new LinkedHashMap<>();
    private final Map<MavenCoord, List<DependencyEdge>> incomingEdges = new LinkedHashMap<>();

    public MavenDependencyGraph(Map<MavenCoord, PomArtifact> artifacts) {
        this.artifacts.putAll(artifacts);
        for (MavenCoord ga : artifacts.keySet()) {
            outgoingEdges.put(ga, new ArrayList<>());
            incomingEdges.put(ga, new ArrayList<>());
        }
        buildEdges();
    }

    private void buildEdges() {
        for (PomArtifact artifact : artifacts.values()) {
            MavenCoord srcGa = artifact.toGa();

            // Parent edge
            if (artifact.getParentCoord() != null) {
                MavenCoord targetGa = artifact.getParentCoord().toGa();
                PomDependency parentDep = new PomDependency(targetGa.getGroupId(), targetGa.getArtifactId(),
                        artifact.getParentCoord().getVersion(), null, "pom", false, DependencyEdgeType.PARENT);
                addEdge(new DependencyEdge(srcGa, targetGa, DependencyEdgeType.PARENT, parentDep));
            }

            // Direct dependencies
            for (PomDependency dep : artifact.getDependencies()) {
                MavenCoord targetGa = dep.toGa();
                addEdge(new DependencyEdge(srcGa, targetGa, dep.getEdgeType(), dep));
            }

            // DependencyManagement (including BOM imports)
            for (PomDependency dep : artifact.getDependencyManagement()) {
                MavenCoord targetGa = dep.toGa();
                DependencyEdgeType edgeType = dep.isBomImport() ? DependencyEdgeType.BOM_IMPORT : DependencyEdgeType.DIRECT_DEPENDENCY;
                addEdge(new DependencyEdge(srcGa, targetGa, edgeType, dep));
            }

            // Plugins
            for (PomDependency dep : artifact.getPluginDependencies()) {
                MavenCoord targetGa = dep.toGa();
                addEdge(new DependencyEdge(srcGa, targetGa, DependencyEdgeType.PLUGIN, dep));
            }
        }
    }

    private void addEdge(DependencyEdge edge) {
        outgoingEdges.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge);
        incomingEdges.computeIfAbsent(edge.getTarget(), k -> new ArrayList<>()).add(edge);
    }

    public Map<MavenCoord, PomArtifact> getArtifacts() {
        return Collections.unmodifiableMap(artifacts);
    }

    public List<DependencyEdge> getOutgoingEdges(MavenCoord ga) {
        List<DependencyEdge> list = outgoingEdges.get(ga.toGa());
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    public List<DependencyEdge> getIncomingEdges(MavenCoord ga) {
        List<DependencyEdge> list = incomingEdges.get(ga.toGa());
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    /**
     * Returns all artifacts that reference the given targetGa (reverse dependencies).
     */
    public Set<MavenCoord> getDirectDependents(MavenCoord targetGa) {
        Set<MavenCoord> set = new LinkedHashSet<>();
        for (DependencyEdge edge : getIncomingEdges(targetGa)) {
            if (artifacts.containsKey(edge.getSource())) {
                set.add(edge.getSource());
            }
        }
        return set;
    }

    /**
     * Detects cycles in the workspace dependency graph.
     * Throws CycleDetectedException with full cycle path if cycle is found.
     */
    public void detectCycles() {
        List<List<String>> cycles = findCycles();
        if (!cycles.isEmpty()) {
            throw new CycleDetectedException(cycles.get(0));
        }
    }

    /**
     * Finds all cycles in the workspace dependency graph.
     * Returns a list of cycle paths (empty if no cycles).
     */
    public List<List<String>> findCycles() {
        Map<MavenCoord, Integer> state = new HashMap<>(); // 0: unvisited, 1: visiting, 2: visited
        List<MavenCoord> stack = new ArrayList<>();
        List<List<String>> cycles = new ArrayList<>();

        for (MavenCoord node : artifacts.keySet()) {
            if (state.getOrDefault(node, 0) == 0) {
                dfsFindCycles(node, state, stack, cycles);
            }
        }
        return cycles;
    }

    private void dfsFindCycles(MavenCoord node, Map<MavenCoord, Integer> state, List<MavenCoord> stack, List<List<String>> cycles) {
        state.put(node, 1);
        stack.add(node);

        for (DependencyEdge edge : getOutgoingEdges(node)) {
            MavenCoord target = edge.getTarget();
            // Only care about cycles within the scanned workspace
            if (artifacts.containsKey(target)) {
                int targetState = state.getOrDefault(target, 0);
                if (targetState == 1) {
                    // Cycle detected! Extract cycle path
                    int startIndex = stack.indexOf(target);
                    List<String> cyclePath = new ArrayList<>();
                    for (int i = startIndex; i < stack.size(); i++) {
                        cyclePath.add(stack.get(i).toGaString());
                    }
                    cyclePath.add(target.toGaString());
                    cycles.add(cyclePath);
                } else if (targetState == 0) {
                    dfsFindCycles(target, state, stack, cycles);
                }
            }
        }

        stack.remove(stack.size() - 1);
        state.put(node, 2);
    }

    /**
     * Returns artifacts in topological order (leaves first, dependents last).
     */
    public List<MavenCoord> topologicalOrder() {
        detectCycles();
        List<MavenCoord> order = new ArrayList<>();
        Set<MavenCoord> visited = new HashSet<>();

        for (MavenCoord node : artifacts.keySet()) {
            if (!visited.contains(node)) {
                dfsTopo(node, visited, order);
            }
        }
        return order;
    }

    private void dfsTopo(MavenCoord node, Set<MavenCoord> visited, List<MavenCoord> order) {
        visited.add(node);
        for (DependencyEdge edge : getOutgoingEdges(node)) {
            if (artifacts.containsKey(edge.getTarget()) && !visited.contains(edge.getTarget())) {
                dfsTopo(edge.getTarget(), visited, order);
            }
        }
        order.add(node);
    }
}
