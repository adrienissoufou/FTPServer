package api.iComand;

import javax.activation.CommandInfo;
import java.io.IOException;

public interface ArgsArrayCommand extends Command {

    void run(String[] argument) throws IOException;

    @Override
    default void run(String argument) throws IOException {
        run(argument.split("\\s+"));
    }

    @Override
    default void run(CommandInfo info, String argument) throws IOException {
        run(argument.split("\\s+"));
    }

}
