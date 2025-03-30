package net.thevpc.nserver.test;

import net.thevpc.nserver.cli.NSHttpClient;
import net.thevpc.nuts.NWorkspace;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.web.NWebCli;
import org.junit.jupiter.api.Test;

public class Test1 {
    @Test
    public void test1(){
        System.out.println(
                new NSHttpClient()
                        .version()
        );
    }
}
