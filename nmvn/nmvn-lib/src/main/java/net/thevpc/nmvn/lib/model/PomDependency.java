package net.thevpc.nmvn.lib.model;

import java.nio.file.Path;
import java.util.Objects;

public class PomDependency {
    private final MavenCoord coord;
    private final String rawVersion;
    private String resolvedVersion;
    private final String scope;
    private final String type;
    private final boolean bomImport;
    private final boolean inDependencyManagement;
    private final DependencyEdgeType edgeType;
    private String versionPropertyName;
    private Path propertyDefiningPom;

    public PomDependency(String groupId, String artifactId, String rawVersion, String scope, String type,
                         boolean inDependencyManagement, DependencyEdgeType edgeType) {
        this.coord = new MavenCoord(groupId, artifactId, rawVersion);
        this.rawVersion = rawVersion;
        this.resolvedVersion = rawVersion;
        this.scope = scope != null ? scope.trim() : "compile";
        this.type = type != null ? type.trim() : "jar";
        this.inDependencyManagement = inDependencyManagement;
        this.edgeType = edgeType;
        this.bomImport = "pom".equalsIgnoreCase(this.type) && "import".equalsIgnoreCase(this.scope);

        if (rawVersion != null && rawVersion.startsWith("${") && rawVersion.endsWith("}")) {
            this.versionPropertyName = rawVersion.substring(2, rawVersion.length() - 1).trim();
        }
    }

    public MavenCoord getCoord() {
        return coord;
    }

    public String getGroupId() {
        return coord.getGroupId();
    }

    public String getArtifactId() {
        return coord.getArtifactId();
    }

    public MavenCoord toGa() {
        return coord.toGa();
    }

    public String getRawVersion() {
        return rawVersion;
    }

    public String getResolvedVersion() {
        return resolvedVersion;
    }

    public void setResolvedVersion(String resolvedVersion) {
        this.resolvedVersion = resolvedVersion;
    }

    public String getScope() {
        return scope;
    }

    public String getType() {
        return type;
    }

    public boolean isBomImport() {
        return bomImport;
    }

    public boolean isInDependencyManagement() {
        return inDependencyManagement;
    }

    public DependencyEdgeType getEdgeType() {
        return edgeType;
    }

    public boolean isPropertyIndirected() {
        return versionPropertyName != null;
    }

    public String getVersionPropertyName() {
        return versionPropertyName;
    }

    public Path getPropertyDefiningPom() {
        return propertyDefiningPom;
    }

    public void setPropertyDefiningPom(Path propertyDefiningPom) {
        this.propertyDefiningPom = propertyDefiningPom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PomDependency that = (PomDependency) o;
        return bomImport == that.bomImport &&
                inDependencyManagement == that.inDependencyManagement &&
                coord.equals(that.coord) &&
                Objects.equals(scope, that.scope) &&
                Objects.equals(type, that.type) &&
                edgeType == that.edgeType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(coord, scope, type, bomImport, inDependencyManagement, edgeType);
    }

    @Override
    public String toString() {
        return coord.toGaString() + ":" + (resolvedVersion != null ? resolvedVersion : rawVersion)
                + (bomImport ? " (BOM)" : "");
    }
}
