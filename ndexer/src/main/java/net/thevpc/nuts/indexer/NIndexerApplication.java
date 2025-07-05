package net.thevpc.nuts.indexer;


import net.thevpc.nuts.NApp;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@NApp.Definition
@EnableScheduling
@SpringBootApplication
public class NIndexerApplication {

    public static void main(String[] args) {
        NApp.builder(args).run();
    }

    @NApp.Runner
    public void run() {

    }


}
