package api.iComand;

import api.ResponseException;

import javax.activation.CommandInfo;
import java.io.IOException;

public interface Command {
    void run(String argument) throws IOException;

    default void run(String command, String argument) throws IOException {
        if(argument.isEmpty()) throw new ResponseException(501, "Missing parameters");

        run(argument);
    }
}

