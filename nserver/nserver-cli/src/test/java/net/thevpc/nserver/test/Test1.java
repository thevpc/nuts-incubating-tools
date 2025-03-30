package net.thevpc.nserver.test;

import net.thevpc.nuts.NWorkspace;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.web.NWebCli;
import org.junit.jupiter.api.Test;

public class Test1 {
    @Test
    public void test1(){
        NWorkspace ws = Nuts.openInheritedWorkspace().share();
        NWebCli wc = NWebCli.of();
        String r = wc.POST("http://localhost:8899/exec")
                .setBody("version")
                .run().getContentAsString();
        System.out.println(r);
    }
}
