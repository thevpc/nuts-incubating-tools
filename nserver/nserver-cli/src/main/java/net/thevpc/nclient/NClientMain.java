package net.thevpc.nclient;

import net.thevpc.nuts.*;
import net.thevpc.nuts.cmdline.NCmdLine;

import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

public class NClientMain implements NApplication {

    public static final Pattern HOST_PATTERN = Pattern.compile("((<?protocol>(http|https|admin))://)?(<host>[a-zA-Z0-9_-]+)(<port>:[0-9]+)?");

    public static void main(String[] args) {
        new NClientMain().main(NMainArgs.ofExit(args));
    }

    private CountDownLatch lock = new CountDownLatch(1);

    @Override
    public void run() {
        NSession session = NSession.get().get();
        NCmdLine cmdLine = NApp.of().getCmdLine().setCommandName("nuts-server");
        cmdLine.setCommandName("nuts-server");
    }


}
