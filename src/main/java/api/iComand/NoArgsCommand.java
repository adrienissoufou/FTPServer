package api.iComand;

import javax.activation.CommandInfo;
import java.io.IOException;

public interface NoArgsCommand extends Command {

    void run() throws IOException;

    @Override
    default void run(String argument) throws IOException {
        run();
    }

    @Override
    default void run(CommandInfo info, String argument) throws IOException {
        run();
    }

}
