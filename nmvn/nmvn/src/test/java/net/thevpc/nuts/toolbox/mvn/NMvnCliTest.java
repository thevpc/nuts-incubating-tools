package net.thevpc.nuts.toolbox.mvn;

import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.core.NSession;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class NMvnCliTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Before
    public void setup() {
        Nuts.require();
    }

    private Path createPom(Path dir, String content) throws IOException {
        Files.createDirectories(dir);
        Path pomFile = dir.resolve("pom.xml");
        Files.write(pomFile, content.getBytes(StandardCharsets.UTF_8));
        return pomFile;
    }

    @Test
    public void testCliScanAndBumpDryRun() throws Exception {
        Path root = temp.newFolder("cli-test").toPath();
        Path mod1 = root.resolve("mod1");
        Path mod2 = root.resolve("mod2");

        createPom(mod1,
                "<project>\n" +
                "  <modelVersion>4.0.0</modelVersion>\n" +
                "  <groupId>com.cli</groupId>\n" +
                "  <artifactId>mod1</artifactId>\n" +
                "  <version>1.0.0-SNAPSHOT</version>\n" +
                "</project>");

        createPom(mod2,
                "<project>\n" +
                "  <modelVersion>4.0.0</modelVersion>\n" +
                "  <groupId>com.cli</groupId>\n" +
                "  <artifactId>mod2</artifactId>\n" +
                "  <version>1.0.0-SNAPSHOT</version>\n" +
                "  <dependencies>\n" +
                "    <dependency>\n" +
                "      <groupId>com.cli</groupId>\n" +
                "      <artifactId>mod1</artifactId>\n" +
                "      <version>1.0.0-SNAPSHOT</version>\n" +
                "    </dependency>\n" +
                "  </dependencies>\n" +
                "</project>");

        NSession session = NSession.of();
        MvnVersionCli cli = new MvnVersionCli(session);

        // Run scan
        int scanCode = cli.run(new String[]{"scan", "--root", root.toString()}, false);
        Assert.assertEquals(0, scanCode);

        // Run bump dry-run
        int bumpCode = cli.run(new String[]{"bump", "--root", root.toString(), "-a", "com.cli:mod1=1.1.0-SNAPSHOT", "--dry-run"}, false);
        Assert.assertEquals(0, bumpCode);

        // Dry-run should not modify files on disk
        String mod1Content = new String(Files.readAllBytes(mod1.resolve("pom.xml")), StandardCharsets.UTF_8);
        Assert.assertTrue(mod1Content.contains("<version>1.0.0-SNAPSHOT</version>"));

        // Run bump apply
        int applyCode = cli.run(new String[]{"bump", "--root", root.toString(), "-a", "com.cli:mod1=1.1.0-SNAPSHOT", "--apply"}, false);
        Assert.assertEquals(0, applyCode);

        // Now files on disk should be updated
        String mod1Updated = new String(Files.readAllBytes(mod1.resolve("pom.xml")), StandardCharsets.UTF_8);
        Assert.assertTrue(mod1Updated.contains("<version>1.1.0-SNAPSHOT</version>"));

        String mod2Updated = new String(Files.readAllBytes(mod2.resolve("pom.xml")), StandardCharsets.UTF_8);
        Assert.assertTrue(mod2Updated.contains("<version>1.1.0-SNAPSHOT</version>"));
    }

    @Test
    public void testCliCheckCleanAndDiscrepancy() throws Exception {
        Path root = temp.newFolder("cli-check-test").toPath();
        Path mod1 = root.resolve("mod1");
        Path mod2 = root.resolve("mod2");

        createPom(mod1,
                "<project>\n" +
                "  <modelVersion>4.0.0</modelVersion>\n" +
                "  <groupId>com.cli</groupId>\n" +
                "  <artifactId>mod1</artifactId>\n" +
                "  <version>1.0.0-SNAPSHOT</version>\n" +
                "</project>");

        createPom(mod2,
                "<project>\n" +
                "  <modelVersion>4.0.0</modelVersion>\n" +
                "  <groupId>com.cli</groupId>\n" +
                "  <artifactId>mod2</artifactId>\n" +
                "  <version>1.0.0-SNAPSHOT</version>\n" +
                "  <dependencies>\n" +
                "    <dependency>\n" +
                "      <groupId>com.cli</groupId>\n" +
                "      <artifactId>mod1</artifactId>\n" +
                "      <version>1.0.0-SNAPSHOT</version>\n" +
                "    </dependency>\n" +
                "  </dependencies>\n" +
                "</project>");

        NSession session = NSession.of();
        MvnVersionCli cli = new MvnVersionCli(session);

        // Clean workspace should return 0
        int checkClean = cli.run(new String[]{"check", "--root", root.toString()}, false);
        Assert.assertEquals(0, checkClean);

        // Now introduce a version discrepancy in mod2
        createPom(mod2,
                "<project>\n" +
                "  <modelVersion>4.0.0</modelVersion>\n" +
                "  <groupId>com.cli</groupId>\n" +
                "  <artifactId>mod2</artifactId>\n" +
                "  <version>1.0.0-SNAPSHOT</version>\n" +
                "  <dependencies>\n" +
                "    <dependency>\n" +
                "      <groupId>com.cli</groupId>\n" +
                "      <artifactId>mod1</artifactId>\n" +
                "      <version>2.0.0-SNAPSHOT</version>\n" + // Discrepancy! mod1 is 1.0.0-SNAPSHOT
                "    </dependency>\n" +
                "  </dependencies>\n" +
                "</project>");

        // Discrepant workspace should return 1
        int checkDiscrepancy = cli.run(new String[]{"check", "--root", root.toString()}, false);
        Assert.assertEquals(1, checkDiscrepancy);

        // JSON mode should also return 1
        int checkJson = cli.run(new String[]{"check", "--root", root.toString(), "--json"}, false);
        Assert.assertEquals(1, checkJson);
    }
}
