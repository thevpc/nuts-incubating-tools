package net.thevpc.nmvn.lib.graph;

import net.thevpc.nmvn.lib.model.DependencyEdgeType;
import net.thevpc.nmvn.lib.model.MavenCoord;
import net.thevpc.nmvn.lib.model.PomDependency;

import java.util.Objects;

public class DependencyEdge {
    private final MavenCoord source;
    private final MavenCoord target;
    private final DependencyEdgeType edgeType;
    private final PomDependency dependency;

    public DependencyEdge(MavenCoord source, MavenCoord target, DependencyEdgeType edgeType, PomDependency dependency) {
        this.source = source.toGa();
        this.target = target.toGa();
        this.edgeType = edgeType;
        this.dependency = dependency;
    }

    public MavenCoord getSource() {
        return source;
    }

    public MavenCoord getTarget() {
        return target;
    }

    public DependencyEdgeType getEdgeType() {
        return edgeType;
    }

    public PomDependency getDependency() {
        return dependency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DependencyEdge that = (DependencyEdge) o;
        return source.equals(that.source) &&
                target.equals(that.target) &&
                edgeType == that.edgeType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target, edgeType);
    }

    @Override
    public String toString() {
        return source.toGaString() + " -[" + edgeType + "]-> " + target.toGaString();
    }
}
