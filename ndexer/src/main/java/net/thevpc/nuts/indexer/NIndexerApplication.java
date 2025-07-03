package net.thevpc.nuts.indexer;


import net.thevpc.nuts.NApp;
import net.thevpc.nuts.NApplication;
import net.thevpc.nuts.NMainArgs;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@NApp.Info
@EnableScheduling
@SpringBootApplication
public class NIndexerApplication {

    public static void main(String[] args) {
        NApp.builder(args).run();
    }

    @NApp.Main
    public void run() {

    }


}
