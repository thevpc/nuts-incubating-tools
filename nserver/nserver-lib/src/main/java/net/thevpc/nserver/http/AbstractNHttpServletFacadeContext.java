/**
 * ====================================================================
 * Nuts : Network Updatable Things Service
 * (universal package manager)
 * <br>
 * is a new Open Source Package Manager to help install packages
 * and libraries for runtime execution. Nuts is the ultimate companion for
 * maven (and other build managers) as it helps installing all package
 * dependencies at runtime. Nuts is not tied to java and is a good choice
 * to share shell scripts and other 'things' . Its based on an extensible
 * architecture to help supporting a large range of sub managers / repositories.
 * <br>
 * <p>
 * Copyright [2020] [thevpc]
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3 (the "License");
 * you may  not use this file except in compliance with the License. You may obtain
 * a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 * <br>
 * ====================================================================
 */
package net.thevpc.nserver.http;

import net.thevpc.nhttp.server.api.FormDataItem;
import net.thevpc.nuts.elem.NElements;
import net.thevpc.nuts.format.NContentType;
import net.thevpc.nuts.io.*;
import net.thevpc.nserver.bundled._IOUtils;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.util.NMsg;
import net.thevpc.nuts.util.NOptional;
import net.thevpc.nuts.util.NStringMapFormat;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by vpc on 1/7/17.
 */
public abstract class AbstractNHttpServletFacadeContext implements NHttpServletFacadeContext {
    private static NStringMapFormat nStringMapFormat = NStringMapFormat.of("=", ";", "\\", false);
    private Map<String, FormDataItem> formData;

    public void sendResponseText(int code, String text) {
        byte[] bytes = text.getBytes();
        sendResponseHeaders(code, bytes.length);
        try {
            getResponseBody().write(bytes);
        } catch (IOException ex) {
            throw new NIOException(ex);
        }
    }

    public void sendResponseFile(int code, File file) {
        if (file != null && file.exists() && file.isFile()) {
            sendResponseHeaders(code, file.length());
            try {
                _IOUtils.copy(new FileInputStream(file), getResponseBody(), true, false);
            } catch (IOException ex) {
                throw new NIOException(ex);
            }
        } else {
            sendError(404, "File not found");
        }
    }

    public void sendResponseBytes(int code, byte[] bytes) {
        sendResponseHeaders(code, bytes.length);
        try {
            getResponseBody().write(bytes);
        } catch (IOException ex) {
            throw new NIOException(ex);
        }
    }

    @Override
    public void sendResponseFile(int code, Path file) {
        if (file != null && Files.isRegularFile(file)) {
            try {
                sendResponseHeaders(code, Files.size(file));
                Files.copy(file, getResponseBody());
            } catch (IOException ex) {
                throw new NIOException(ex);
            }
        } else {
            sendError(404, "File not found");
        }
    }

    @Override
    public void sendResponseFile(int code, NPath file) {
        if (file != null && file.isRegularFile()) {
            String contentType = file.getContentType();
            if (!NBlankable.isBlank(contentType)) {
                addResponseHeader("Content-Type", contentType);
            }
            sendResponseHeaders(code, file.contentLength());
            NCp.of().from(file).to(getResponseBody()).run();
        } else {
            sendError(404, "File not found");
        }
    }

    @Override
    public boolean isGetMethod() {
        return "GET".equalsIgnoreCase(getRequestMethod());
    }

    @Override
    public boolean isPostMethod() {
        return "POST".equalsIgnoreCase(getRequestMethod());
    }

    @Override
    public boolean isHeadMethod() {
        return "HEAD".equalsIgnoreCase(getRequestMethod());
    }

    @Override
    public String getRequestBodyAsString() throws NIOException {
        try (InputStream is = getRequestBody()) {
            return NInputSource.of(is).readString();
        } catch (IOException e) {
            throw new NIOException(e);
        }
    }

    @Override
    public byte[] getRequestBodyAsBytes() throws NIOException {
        try (InputStream is = getRequestBody()) {
            return NInputSource.of(is).readBytes();
        } catch (IOException e) {
            throw new NIOException(e);
        }
    }

    @Override
    public <T> T getRequestBodyAs(Class<T> type, NContentType contentType) throws NIOException {
        try (InputStream is = getRequestBody()) {
            return NElements.of().setContentType(contentType).parse(is, type);
        } catch (IOException e) {
            throw new NIOException(e);
        }
    }

    @Override
    public boolean isMultipartRequest() {
        for (String a : getRequestHeaders("Content-Type")) {
            a = a.trim();
            if (a.startsWith("multipart/form-data")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public NOptional<String> getMultipartRequestBoundary() {
        for (String value : getRequestHeaders("Content-Type")) {
            value = value.trim();
            if (value.startsWith("multipart/form-data; boundary=")) {
                return NOptional.of(value.split("boundary=")[1]);
            }
        }
        return NOptional.ofNamedEmpty("multipart");
    }


    @Override
    public NOptional<String> getRequestParameter(String name) {
        List<String> u = getRequestParameters().get(name);
        if (u == null || u.isEmpty()) {
            return null;
        }
        return NOptional.ofNamed(u.get(0),name);
    }

    @Override
    public List<String> getRequestParameters(String name) {
        List<String> u = getRequestParameters().get(name);
        if (u == null || u.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(u);
    }

    @Override
    public NOptional<FormDataItem> getFormaData(String name) {
        Map<String, FormDataItem> u = getFormaDataMap();
        return NOptional.ofNamed(u == null ? null : u.get(name), name);
    }

    @Override
    public Map<String, FormDataItem> getFormaDataMap() {
        if (formData != null) {
            return formData;
        }
        String multipartRequestBoundary = this.getMultipartRequestBoundary().orNull();
        if (NBlankable.isBlank(multipartRequestBoundary)) {
            return formData = new HashMap<>();
        }
        Map<String, FormDataItem> formData = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getRequestBody()))) {
            String line = null;
            line = br.readLine();
            if (!line.trim().endsWith(multipartRequestBoundary.trim())) {
                throw new IllegalArgumentException("Invalid boundaries");
            }
            while (true) {
                FormDataItem fd = null;
                while ((line = br.readLine()) != null) {
                    if (line.contains(":")) {
                        int sep = line.indexOf(':');
                        String k = line.substring(0, sep).trim();
                        String v = line.substring(sep + 1).trim();
                        if (fd == null) {
                            fd = new FormDataItem();
                            formData.put(k, fd);
                        }
                        fd.getHeaders().computeIfAbsent(k, e -> new ArrayList<>()).add(v);
                        switch (k) {
                            case "Content-Disposition": {
                                if (v.startsWith("form-data;")) {
                                    Map<String, List<String>> parsed = nStringMapFormat.parseDuplicates(line.substring("form-data;".length()).trim()).get();
                                    fd.setName(_get("name", parsed));
                                    fd.setFilename(_get("filename", parsed));
                                    fd.setProperties(parsed);
                                }
                                break;
                            }
                            case "Content-Type": {
                                fd.setContentType(v);
                                break;
                            }
                        }
                    } else if (line.trim().isEmpty()) {
                        break;
                    } else {
                        throw new NIOException(NMsg.ofC("Error reading request body : " + line));
                    }
                }
                if (fd != null && fd.getContentType() != null) {
                    if ("text/plain".equals(fd.getContentType())) {
                        NTempOutputStream outputStream = NIO.of().ofTempOutputStream();
                        PrintStream out = new PrintStream(outputStream);
                        br.read(new char[2048]);
                        LineAndNewLine ll;
                        while ((ll = readLineAndNewLine(br)) != null) {
                            if (ll.line.trim().endsWith(multipartRequestBoundary.trim())) {
                                break;
                            } else {
                                System.out.println(fd.getName() + " :: " + ll.line);
                                out.print(ll.line);
                                out.print(ll.newLine);
                            }
                        }
                        // do not close
                        fd.setSource(outputStream);
                    }
                } else if (fd != null) {
                    //okkay
                } else {
                    throw new NIOException(NMsg.ofC("Error reading request body : " + line));
                }
            }
        } catch (IOException e) {
            throw new NIOException(e);
        }
    }

    private LineAndNewLine readLineAndNewLine(BufferedReader reader) {
        StringBuilder sb = new StringBuilder();
        StringBuilder nl = new StringBuilder();
        try {
            while (true) {
                int c = 0;
                c = reader.read();
                if (c == -1) {
                    if (sb.length() == 0 && nl.length() == 0) {
                        return null;
                    }
                    return new LineAndNewLine(sb.toString(), nl.toString());
                } else if (c == '\r') {
                    nl.append('\r');
                    return new LineAndNewLine(sb.toString(), nl.toString());
                } else if (c == '\n') {
                    nl.append('\n');
                    reader.mark(1);
                    int x = reader.read();
                    if (x == -1) {
                        // do nothing
                    } else if (x == '\r') {
                        nl.append('\r');
                    } else {
                        reader.reset();
                    }
                    return new LineAndNewLine(sb.toString(), nl.toString());
                } else {
                    sb.append((char) c);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class LineAndNewLine {
        String line;
        String newLine;

        public LineAndNewLine(String line, String newLine) {
            this.line = line;
            this.newLine = newLine;
        }
    }

    private String _get(String k, Map<String, List<String>> map) {
        List<String> v = map.get(k);
        if (v != null && v.size() > 0) {
            return v.get(0);
        }
        return null;
    }

}
