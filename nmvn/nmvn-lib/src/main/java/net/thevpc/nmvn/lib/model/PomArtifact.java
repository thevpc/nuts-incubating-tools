package net.thevpc.nmvn.lib.model;

import java.nio.file.Path;
import java.util.*;

public class PomArtifact {
    private final MavenCoord coord;
    private String resolvedVersion;
    private final MavenCoord parentCoord;
    private final Path path;
    private final Map<String, String> declaredProperties = new LinkedHashMap<>();
    private final Map<String, String> resolvedProperties = new LinkedHashMap<>();
    private final List<PomDependency> dependencies = new ArrayList<>();
    private final List<PomDependency> dependencyManagement = new ArrayList<>();
    private final List<PomDependency> pluginDependencies = new ArrayList<>();
    private final List<String> modules = new ArrayList<>();
    private String versionPropertyName;

    public PomArtifact(MavenCoord coord, MavenCoord parentCoord, Path path) {
        this.coord = coord;
        this.parentCoord = parentCoord;
        this.path = path;
        this.resolvedVersion = coord.getVersion();
        if (coord.getVersion() != null && coord.getVersion().startsWith("${") && coord.getVersion().endsWith("}")) {
            this.versionPropertyName = coord.getVersion().substring(2, coord.getVersion().length() - 1).trim();
        }
    }

    public MavenCoord getCoord() {
        return coord;
    }

    public MavenCoord toGa() {
        return coord.toGa();
    }

    public String getGroupId() {
        return coord.getGroupId();
    }

    public String getArtifactId() {
        return coord.getArtifactId();
    }

    public String getRawVersion() {
        return coord.getVersion();
    }

    public String getResolvedVersion() {
        return resolvedVersion;
    }

    public void setResolvedVersion(String resolvedVersion) {
        this.resolvedVersion = resolvedVersion;
    }

    public MavenCoord getParentCoord() {
        return parentCoord;
    }

    public Path getPath() {
        return path;
    }

    public Map<String, String> getDeclaredProperties() {
        return declaredProperties;
    }

    public Map<String, String> getResolvedProperties() {
        return resolvedProperties;
    }

    public List<PomDependency> getDependencies() {
        return dependencies;
    }

    public List<PomDependency> getDependencyManagement() {
        return dependencyManagement;
    }

    public List<PomDependency> getPluginDependencies() {
        return pluginDependencies;
    }

    public List<String> getModules() {
        return modules;
    }

    public boolean isVersionPropertyIndirected() {
        return versionPropertyName != null;
    }

    public String getVersionPropertyName() {
        return versionPropertyName;
    }

    public List<PomDependency> getAllReferences() {
        List<PomDependency> all = new ArrayList<>();
        if (parentCoord != null) {
            all.add(new PomDependency(parentCoord.getGroupId(), parentCoord.getArtifactId(),
                    parentCoord.getVersion(), null, "pom", false, DependencyEdgeType.PARENT));
        }
        all.addAll(dependencies);
        all.addAll(dependencyManagement);
        all.addAll(pluginDependencies);
        return Collections.unmodifiableList(all);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PomArtifact that = (PomArtifact) o;
        return coord.toGa().equals(that.coord.toGa()) && path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(coord.toGa(), path);
    }

    @Override
    public String toString() {
        return coord.toGaString() + ":" + (resolvedVersion != null ? resolvedVersion : coord.getVersion())
                + " [" + path + "]";
    }
}
