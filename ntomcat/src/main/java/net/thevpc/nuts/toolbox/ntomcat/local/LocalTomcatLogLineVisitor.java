package net.thevpc.nuts.toolbox.ntomcat.local;

import net.thevpc.nuts.io.NPath;

class LocalTomcatLogLineVisitor {

    boolean outOfMemoryError;
    String startMessage;
    String shutdownMessage;
    Boolean started;
    String path;

    public LocalTomcatLogLineVisitor(String path, String startMessage, String shutdownMessage) {
        this.path = path;
        this.startMessage = startMessage;
        this.shutdownMessage = shutdownMessage;
    }

    public void visit() {
        NPath.of(path).lines()
                .forEach(this::nextLine);
    }

    public boolean nextLine(String line) {
        if (line.contains("OutOfMemoryError")) {
            outOfMemoryError = true;
        } else if (startMessage != null && line.contains(startMessage)) {
            started = true;
        } else if (shutdownMessage != null && line.contains(shutdownMessage)) {
            started = false;
        }
        return true;
    }
}
