package net.thevpc.nserver.cli;

import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.io.*;
import net.thevpc.nuts.util.*;
import net.thevpc.nuts.web.NWebCli;
import net.thevpc.nuts.web.NWebRequest;
import net.thevpc.nuts.web.NWebResponse;

import java.util.*;
import java.util.stream.Collectors;

public class NSHttpClient {
    private String url;
    private LoginResult loginResult;

    public String getUrl() {
        return url;
    }

    public NSHttpClient setUrl(String url) {
        this.url = url;
        return this;
    }

    public String version() {
        return NWebCli.of().POST(resolveUrl("version"))
                .doWith(this::prepareSecurity)
                .run().getContentAsString();
    }

    public NInputSource getFile(String remotePath) {
        return NWebCli.of().POST(resolveUrl("get-file"))
                .setJsonRequestBody(
                        NMapBuilder.ofLinked()
                                .put("path", remotePath)
                )
                .doWith(this::prepareSecurity)
                .run().getContent();
    }

    public void getFile(String remotePath, String localPath) {
        NFileInfo u = getFileInfo(remotePath);
        if (u != null) {
            switch (NUtils.firstNonNull(u.getPathType(), NPathType.NOT_FOUND)) {
                case DIRECTORY: {
                    NPath.of(localPath).mkdirs();
                    for (String s : listNames(u.getPath())) {
                        getFile(NStringUtils.pjoin("/", u.getPath(), s), NStringUtils.pjoin("/", localPath, s));
                    }
                    break;
                }
                case FILE: {
                    NPath.of(localPath).mkParentDirs().copyFromInputStreamProvider(getFile(remotePath));
                    break;
                }
            }
        }
    }

    public void putFile(String localPath, String remotePath) {
        NPath fromPathObj = NPath.of(localPath);
        NPath toPathObj = NPath.of(remotePath);
        if (!fromPathObj.isDirectory()) {
            List<NPath> directories = new ArrayList<>();
            fromPathObj.walk().filter(x -> x.isDirectory()).forEach(x -> {
                boolean found = false;
                for (NPath directory : directories) {
                    if (x.isEqOrDeepChildOf(directory)) {
                        //ignore
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    directories.add(x);
                }
            });
            for (NPath directory : directories) {
                NPath v = translate(directory, fromPathObj, toPathObj);
                v.mkdirs();
            }
            fromPathObj.walk().filter(x -> x.isRegularFile()).forEach(x -> {
                NPath toStr = translate(x, fromPathObj, toPathObj);

                NWebCli.of().POST(resolveUrl("put-file"))
                        .addFormData("content", x.toString())
                        .addFormData("path", toStr.toString())
                        .doWith(this::prepareSecurity)
                        .run().getContent();
            });
        } else if (fromPathObj.isRegularFile()) {
            NWebCli.of().POST(resolveUrl("put-file"))
                    .addFormData("content", fromPathObj.toString())
                    .addFormData("path", toPathObj.toString())
                    .doWith(this::prepareSecurity)
                    .run().getContent();
        }
    }

    private NPath translate(NPath fromBase, NPath toBase, NPath fromPath) {
        String u = fromPath.toRelative(fromBase).get();
        NPath v = toBase.resolve(u);
        return v;
    }

    public void putFile(NInputContentProvider localPath, String remotePath) {
        NWebCli.of().POST(resolveUrl("put-file"))
                .addFormData("content", localPath)
                .addFormData("path", remotePath)
                .doWith(this::prepareSecurity)
                .run().getContent();
    }

    public static class LoginResult {
        public String userId;
        public String userName;
        public String accessToken;
        public String refreshToken;
        public long lastValidityTime;
    }

    public boolean isValidToken() {
        if (this.loginResult != null) {
            if (this.loginResult.lastValidityTime != 0) {
                if (new Date().getTime() <= this.loginResult.lastValidityTime) {
                    return true;
                }
            }
        }
        return false;
    }

    public void login(String login, String password) {
        NWebResponse response = NWebCli.of().POST(resolveUrl("login"))
                .setJsonRequestBody(
                        NMapBuilder.ofLinked()
                                .put("userName", login)
                                .put("password", password)
                )
                .run();
        LoginResult rr = response.getContentAsJson(LoginResult.class);
        if (rr != null) {
            if (response.getCode() == 200 && NBlankable.isBlank(rr.accessToken)) {
                this.loginResult = rr;
            }
        }
    }

    private void prepareSecurity(NWebRequest r) {
        if (loginResult != null) {
            if (!NBlankable.isBlank(loginResult.accessToken)) {
                r.setAuthorizationBearer(loginResult.accessToken);
            }
        }
    }

    private String resolveUrl(String extra) {
        String url = this.url;
        if (NBlankable.isBlank(url)) {
            url = "localhost:8899";
        }
        if (!url.startsWith("http")) {
            url = "http://" + url;
        }
        if (NBlankable.isBlank(extra)) {
            url = NStringUtils.pjoin(
                    "/",
                    url,
                    extra);
        }
        return url;
    }

    public NInputSource exec(String... command) {
        return exec(command, null);
    }

    public NInputSource exec(String[] command, NInputContentProvider inputSource) {
        return NWebCli.of().POST(resolveUrl("exec"))
                .setFormData("command", NCmdLine.of(command == null ? new String[0] : command).toString())
                .setFormData(inputSource == null ? null : "in", inputSource)
                .doWith(this::prepareSecurity)
                .run().getContent();
    }

    public static class NFileInfo {
        private String path;
        private long contentLength;
        private String contentType;
        private NPathType pathType;
        private int directoryChildrenCount;

        public int getDirectoryChildrenCount() {
            return directoryChildrenCount;
        }

        public NFileInfo setDirectoryChildrenCount(int directoryChildrenCount) {
            this.directoryChildrenCount = directoryChildrenCount;
            return this;
        }

        public String getPath() {
            return path;
        }

        public NFileInfo setPath(String path) {
            this.path = path;
            return this;
        }

        public long getContentLength() {
            return contentLength;
        }

        public NFileInfo setContentLength(long contentLength) {
            this.contentLength = contentLength;
            return this;
        }

        public String getContentType() {
            return contentType;
        }

        public NFileInfo setContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public NPathType getPathType() {
            return pathType;
        }

        public NFileInfo setPathType(NPathType pathType) {
            this.pathType = pathType;
            return this;
        }
    }

    public NFileInfo getFileInfo(String remotePath) {
        return NWebCli.of().POST(resolveUrl("file-info"))
                .doWith(this::prepareSecurity)
                .setJsonRequestBody(
                        NMapBuilder.ofLinked()
                                .put("path", remotePath)
                )
                .run().getContentAsJson(NFileInfo.class);
    }

    public String[] listNames(String remotePath) {
        return NWebCli.of().POST(resolveUrl("directory-list-names"))
                .setJsonRequestBody(
                        NMapBuilder.ofLinked()
                                .put("path", remotePath)
                )
                .doWith(this::prepareSecurity)
                .run().getContentAsJson(String[].class);
    }

    public String digest(String remotePath, String algo) {
        Map<String, Object> path = (Map<String, Object>) NWebCli.of().POST(resolveUrl("file-digest"))
                .setJsonRequestBody(
                        NMapBuilder.ofLinked()
                                .put("path", remotePath)
                                .put("algo", algo)
                )
                .doWith(this::prepareSecurity)
                .run().getContentAsJson(Map.class);
        return path == null ? null : (String) path.get("hash");
    }


    public List<NPathChildStringDigestInfo> directoryListDigest(String remotePath, String algo) {
        Map<String, Object>[] res = (Map[]) NWebCli.of().POST(resolveUrl("directory-list-digest"))
                .setJsonRequestBody(
                        NMapBuilder.ofLinked()
                                .put("path", remotePath)
                                .put("algo", algo)
                )
                .doWith(this::prepareSecurity)
                .run().getContentAsJson(Map[].class);
        if (res == null) {
            return new ArrayList<>();
        }
        return Arrays.stream(res).map(x ->
                new NPathChildStringDigestInfo()
                        .setName((String) x.get("name"))
                        .setDigest((String) x.get("digest"))
        ).collect(Collectors.toList());
    }

    public NFileInfo[] listFileInfos(String remotePath) {
        return NWebCli.of().POST(resolveUrl("directory-list-infos"))
                .setJsonRequestBody(
                        NMapBuilder.ofLinked()
                                .put("path", remotePath)
                )
                .doWith(this::prepareSecurity)
                .run().getContentAsJson(NFileInfo[].class);
    }
}
