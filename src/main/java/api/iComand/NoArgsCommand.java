package api.iComand;

import java.io.IOException;

public interface NoArgsCommand extends Command {

    void run() throws IOException;

    @Override
    default void run(String argument) throws IOException {
        run();
    }

    @Override
    default void run(String command, String argument) throws IOException {
        run();
    }

}
