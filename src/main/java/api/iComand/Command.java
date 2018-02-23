package api.iComand;

import api.ResponseException;

import javax.activation.CommandInfo;
import java.io.IOException;
import java.sql.SQLException;

public interface Command {
    void run(String argument) throws IOException, SQLException;

    default void run(String command, String argument) throws IOException, SQLException {
        if(argument.isEmpty()) throw new ResponseException(501, "Missing parameters");

        run(argument);
    }
}

