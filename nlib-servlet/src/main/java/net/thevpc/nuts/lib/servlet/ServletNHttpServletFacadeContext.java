package net.thevpc.nuts.lib.servlet;

import net.thevpc.nhttp.server.api.*;
import net.thevpc.nuts.format.NContentType;
import net.thevpc.nuts.io.NIOException;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NMsg;
import net.thevpc.nuts.util.NMsgCode;
import net.thevpc.nuts.util.NOptional;
import net.thevpc.nuts.util.NUnsafeRunnable;
import net.thevpc.nuts.web.NHttpCode;
import net.thevpc.nuts.web.NHttpMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;

class ServletNHttpServletFacadeContext implements NWebCallContext {
    private final HttpServletRequest req;
    private final HttpServletResponse resp;

    public ServletNHttpServletFacadeContext(HttpServletRequest req, HttpServletResponse resp) {
        this.req = req;
        this.resp = resp;
    }

    @Override
    public NWebHttpException wrapException(Throwable ex) {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public boolean isResponseSent() {
        return false;
    }

    @Override
    public String createTokenHash(NWebToken token) {
        return "";
    }

    @Override
    public String createTokenHash(NWebUser user) {
        return "";
    }

    @Override
    public NWebToken createAccessToken(NWebUser user) {
        return null;
    }

    @Override
    public void initializeConfig() {

    }

    @Override
    public URI getRequestURI() {
        try {
            String cp = req.getContextPath();
            String uri = req.getRequestURI();
            if (uri.startsWith(cp)) {
                uri = uri.substring(cp.length());
                if (uri.startsWith(req.getServletPath())) {
                    uri = uri.substring(req.getServletPath().length());
                }
            }
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new NIOException(e);
        }
    }

    @Override
    public OutputStream getResponseBody() {
        try {
            return resp.getOutputStream();
        } catch (IOException ex) {
            throw new NIOException(ex);
        }
    }

    public String getRequestMethod() {
        return req.getMethod();
    }


    public void sendError(int code, String msg) {
        try {
            resp.sendError(code, msg);
        } catch (IOException ex) {
            throw new NIOException(ex);
        }
    }

    public void sendResponseHeaders(int code, long length) {
        if (length > 0) {
            resp.setHeader("Content-length", Long.toString(length));
        }
        resp.setStatus(code);
    }

//        @Override
//        public Set<String> getRequestHeaderKeys(String header) {
//            return new HashSet<>(Collections.list(req.getHeaderNames()));
//        }

    @Override
    public NOptional<String> getRequestHeader(String header) {
        return NOptional.ofNamed(req.getHeader(header), header);
    }

    @Override
    public List<String> getRequestHeaders(String header) {
        return Collections.list(req.getHeaders(header));
    }

    @Override
    public InputStream getRequestBody() {
        try {
            return req.getInputStream();
        } catch (IOException ex) {
            throw new NIOException(ex);
        }
    }

    public Map<String, List<String>> getRequestParameters() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        for (String s : Collections.list(req.getParameterNames())) {
            for (String v : req.getParameterValues(s)) {
                List<String> li = m.computeIfAbsent(s, d -> new ArrayList<>());
                li.add(v);
            }
        }
        return m;//HttpUtils.queryToMap(getRequestURI().getQuery());
    }

    public NWebCallContext addResponseHeader(String name, String value) {
        resp.addHeader(name, value);
        return this;
    }

    /// //////////////////////////////////////
    // FIX ME


    @Override
    public String getFirstPath() {
        return "";
    }

    @Override
    public boolean isEmptyPath() {
        return false;
    }

    @Override
    public int getPathSize() {
        return 0;
    }

    @Override
    public String[] getPathParts() {
        return new String[0];
    }

    @Override
    public String getPathPart(int pos) {
        return "";
    }

    @Override
    public <T> T getRequestBodyAs(Class<T> cl) {
        return null;
    }

    @Override
    public <T> T getRequestBodyAs(Class<T> cl, NContentType contentType) {
        return null;
    }

    @Override
    public String getRequestBodyAsString() {
        return "";
    }

    @Override
    public String getPath() {
        return "";
    }

    @Override
    public NWebCallContext setResponseContentType(String contentType) {
        return null;
    }

    @Override
    public NWebCallContext setErrorCode(NMsgCode errorCode) {
        return null;
    }

    @Override
    public NWebCallContext sendResponseHeaders() {
        return null;
    }

    @Override
    public NHttpMethod getMethod() {
        return null;
    }

    @Override
    public NWebCallContext requireAuth() {
        return null;
    }

    @Override
    public NWebCallContext trace(Level level, NMsg msg) {
        return null;
    }

    @Override
    public NWebCallContext requireMethod(NHttpMethod... m) {
        return null;
    }

    @Override
    public NWebCallContext throwNoFound() {
        return null;
    }

    @Override
    public NWebPrincipal getPrincipal() {
        return null;
    }

    @Override
    public NOptional<NWebUser> getUser() {
        return null;
    }

    @Override
    public NWebCallContext setUser(NWebUser user) {
        return null;
    }

    @Override
    public NOptional<NWebToken> getToken() {
        return null;
    }

    @Override
    public NWebCallContext setToken(NWebToken token) {
        return null;
    }

    @Override
    public NWebCallContext runWithUnsafe(NUnsafeRunnable callable) throws Throwable {
        return null;
    }

    @Override
    public Map<String, List<String>> getQueryParams() {
        return Collections.emptyMap();
    }

    @Override
    public NOptional<String> getQueryParam(String queryParam) {
        return null;
    }

    @Override
    public boolean containsQueryParam(String queryParam) {
        return false;
    }

    @Override
    public NWebCallContext setResponseHeader(String name, String value) {
        return null;
    }

    @Override
    public NHttpCode getResponseCode() {
        return null;
    }

    @Override
    public NWebCallContext setResponseCode(NHttpCode responseCode) {
        return null;
    }

    @Override
    public Map<String, List<String>> getRequestHeaders() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, FormDataItem> getFormaDataMap() {
        return Collections.emptyMap();
    }

    @Override
    public NOptional<FormDataItem> getFormaData(String name) {
        return null;
    }

    @Override
    public boolean isMultipartRequest() {
        return false;
    }

    @Override
    public NOptional<String> getMultipartRequestBoundary() {
        return null;
    }

    @Override
    public NWebCallContext setTextResponse(String value) {
        return null;
    }

    @Override
    public NWebCallContext setXmlResponse(String value) {
        return null;
    }

    @Override
    public NWebCallContext setJsonResponse(Object value) {
        return null;
    }

    @Override
    public NWebCallContext setBytesResponse(byte[] value) {
        return null;
    }

    @Override
    public NWebCallContext setFileResponse(NPath value) {
        return null;
    }

    @Override
    public NWebCallContext setErrorResponse(NMsgCode errorCode) {
        return null;
    }

    @Override
    public NWebCallContext sendResponse() {
        return null;
    }

    @Override
    public NWebCallContext setErrorResponse(NWebHttpException ex) {
        return null;
    }

    @Override
    public NWebCallContext setErrorResponse(Throwable ex) {
        return null;
    }
}
