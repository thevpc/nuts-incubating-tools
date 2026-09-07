package net.thevpc.nmvn.lib.model;

import java.util.Objects;

public class MavenCoord implements Comparable<MavenCoord> {
    private final String groupId;
    private final String artifactId;
    private final String version;

    public MavenCoord(String groupId, String artifactId) {
        this(groupId, artifactId, null);
    }

    public MavenCoord(String groupId, String artifactId, String version) {
        this.groupId = Objects.requireNonNull(groupId, "groupId cannot be null").trim();
        this.artifactId = Objects.requireNonNull(artifactId, "artifactId cannot be null").trim();
        this.version = version != null ? version.trim() : null;
    }

    public static MavenCoord parse(String coordStr) {
        if (coordStr == null || coordStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Coordinate string cannot be null or empty");
        }
        String[] parts = coordStr.trim().split(":");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid coordinate format, expected groupId:artifactId[:version], got: " + coordStr);
        }
        String g = parts[0].trim();
        String a = parts[1].trim();
        String v = parts.length > 2 ? parts[2].trim() : null;
        return new MavenCoord(g, a, v);
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public MavenCoord withVersion(String newVersion) {
        return new MavenCoord(groupId, artifactId, newVersion);
    }

    public MavenCoord toGa() {
        return version == null ? this : new MavenCoord(groupId, artifactId, null);
    }

    public String toGaString() {
        return groupId + ":" + artifactId;
    }

    public String toGavString() {
        return version != null ? groupId + ":" + artifactId + ":" + version : toGaString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MavenCoord that = (MavenCoord) o;
        return groupId.equals(that.groupId) &&
                artifactId.equals(that.artifactId) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }

    @Override
    public String toString() {
        return toGavString();
    }

    @Override
    public int compareTo(MavenCoord o) {
        int c = groupId.compareTo(o.groupId);
        if (c != 0) return c;
        c = artifactId.compareTo(o.artifactId);
        if (c != 0) return c;
        if (version == null && o.version == null) return 0;
        if (version == null) return -1;
        if (o.version == null) return 1;
        return version.compareTo(o.version);
    }
}
