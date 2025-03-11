package net.thevpc.nuts.indexer;


import net.thevpc.nuts.NApplication;
import net.thevpc.nuts.NMainArgs;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class NIndexerApplication implements NApplication {

    public static void main(String[] args) {
        new NIndexerApplication().main(NMainArgs.ofExit(args));
    }

    @Override
    public void run() {

    }


}
