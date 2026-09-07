package net.thevpc.nmvn.lib.scanner;

import net.thevpc.nmvn.lib.config.NMvnConfig;
import net.thevpc.nmvn.lib.exception.AmbiguousArtifactException;
import net.thevpc.nmvn.lib.model.MavenCoord;
import net.thevpc.nmvn.lib.model.PomArtifact;
import net.thevpc.nmvn.lib.parser.PomParser;
import net.thevpc.nmvn.lib.parser.PropertyResolver;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class PomScanner {

    private final PomParser pomParser = new PomParser();

    public Map<MavenCoord, PomArtifact> scan(NMvnConfig config, Path workingDir) throws IOException {
        List<String> roots = config.getRoots();
        if (roots == null || roots.isEmpty()) {
            roots = Collections.singletonList(".");
        }

        Map<MavenCoord, List<PomArtifact>> collectedByGa = new LinkedHashMap<>();

        for (String rootStr : roots) {
            Path rootPath = workingDir.resolve(rootStr).normalize().toAbsolutePath();
            if (!Files.exists(rootPath)) {
                continue;
            }

            // Combine global excludes + root-specific excludes
            List<String> combinedExcludes = new ArrayList<>(config.getExcludes());
            if (config.getRootExcludes() != null && config.getRootExcludes().containsKey(rootStr)) {
                combinedExcludes.addAll(config.getRootExcludes().get(rootStr));
            }

            List<PathMatcher> matchers = new ArrayList<>();
            FileSystem fs = rootPath.getFileSystem();
            for (String exc : combinedExcludes) {
                try {
                    matchers.add(fs.getPathMatcher("glob:" + exc));
                } catch (Exception ignored) {
                }
            }

            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    Path rel = rootPath.relativize(dir);
                    if (isExcluded(rel, matchers)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if ("pom.xml".equals(file.getFileName().toString())) {
                        Path rel = rootPath.relativize(file);
                        if (!isExcluded(rel, matchers)) {
                            try {
                                PomArtifact artifact = pomParser.parse(file);
                                MavenCoord ga = artifact.toGa();
                                List<PomArtifact> list = collectedByGa.computeIfAbsent(ga, k -> new ArrayList<>());
                                list.add(artifact);
                            } catch (Exception e) {
                                // Ignore non-maven or malformed helper poms unless critical
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        // Check for ambiguous artifacts (same GA found in multiple places)
        Map<MavenCoord, PomArtifact> result = new LinkedHashMap<>();
        for (Map.Entry<MavenCoord, List<PomArtifact>> entry : collectedByGa.entrySet()) {
            List<PomArtifact> list = entry.getValue();
            if (list.size() > 1) {
                List<Path> paths = new ArrayList<>();
                for (PomArtifact pa : list) {
                    paths.add(pa.getPath());
                }
                throw new AmbiguousArtifactException(entry.getKey().getGroupId(), entry.getKey().getArtifactId(), paths);
            }
            result.put(entry.getKey(), list.get(0));
        }

        // Resolve property-indirected versions and inheritance
        PropertyResolver.resolveAll(result);

        return result;
    }

    private boolean isExcluded(Path path, List<PathMatcher> matchers) {
        String pathStr = path.toString().replace('\\', '/');
        if (pathStr.isEmpty()) {
            return false;
        }
        for (PathMatcher matcher : matchers) {
            if (matcher.matches(path) || matcher.matches(Paths.get(pathStr))) {
                return true;
            }
            // Check substring pattern match (e.g. **/target/**)
            if (pathStr.contains("/target/") || pathStr.endsWith("/target") || "target".equals(pathStr)
                    || pathStr.contains("/build/") || pathStr.endsWith("/build") || "build".equals(pathStr)
                    || pathStr.contains("/.git/") || pathStr.endsWith("/.git") || ".git".equals(pathStr)) {
                return true;
            }
        }
        return false;
    }
}
