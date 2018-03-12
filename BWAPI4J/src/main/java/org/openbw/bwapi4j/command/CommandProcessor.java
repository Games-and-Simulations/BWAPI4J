package org.openbw.bwapi4j.command;

import org.openbw.bwapi4j.BW;

import static org.openbw.bwapi4j.command.CommandMediator.CommandMediator;

public interface CommandProcessor {

    default void processCommands(int nCommnads) {
        CommandMediator.poll(nCommnads).forEach(this::runCommand);
    }

    default void processCommands() {
        CommandMediator.commands().forEach(this::runCommand);
    }

    default void runCommand(final GameCommand cmd) {
        try {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (cmd) {
                cmd.run(getGame());
                cmd.notifyAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    BW getGame();

}
