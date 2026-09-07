package net.thevpc.nmvn.lib.model;

import java.util.Objects;

public class BumpInstruction {
    private final String groupId;
    private final String artifactId;
    private final String toVersion;

    public BumpInstruction(String groupId, String artifactId, String toVersion) {
        this.groupId = Objects.requireNonNull(groupId, "groupId cannot be null").trim();
        this.artifactId = Objects.requireNonNull(artifactId, "artifactId cannot be null").trim();
        this.toVersion = Objects.requireNonNull(toVersion, "toVersion cannot be null").trim();
    }

    public static BumpInstruction parse(String str) {
        if (str == null || !str.contains("=")) {
            throw new IllegalArgumentException("Invalid bump instruction format. Expected groupId:artifactId=version, got: " + str);
        }
        int eq = str.indexOf('=');
        String gaStr = str.substring(0, eq).trim();
        String ver = str.substring(eq + 1).trim();
        MavenCoord ga = MavenCoord.parse(gaStr);
        return new BumpInstruction(ga.getGroupId(), ga.getArtifactId(), ver);
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getToVersion() {
        return toVersion;
    }

    public MavenCoord toGa() {
        return new MavenCoord(groupId, artifactId);
    }

    public MavenCoord toGav() {
        return new MavenCoord(groupId, artifactId, toVersion);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BumpInstruction that = (BumpInstruction) o;
        return groupId.equals(that.groupId) &&
                artifactId.equals(that.artifactId) &&
                toVersion.equals(that.toVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, toVersion);
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + "=" + toVersion;
    }
}
