package net.thevpc.nuts.indexer;


import net.thevpc.nuts.app.NApp;
import net.thevpc.nuts.app.NAppDefinition;
import net.thevpc.nuts.app.NAppRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@NAppDefinition
@EnableScheduling
@SpringBootApplication
public class NIndexerApplication {

    public static void main(String[] args) {
        NApp.builder(args).run();
    }

    @NAppRunner
    public void run() {

    }


}
