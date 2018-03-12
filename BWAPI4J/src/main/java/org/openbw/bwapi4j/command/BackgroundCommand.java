package org.openbw.bwapi4j.command;

import org.openbw.bwapi4j.BW;

@FunctionalInterface
public interface BackgroundCommand extends GameCommand {

  void run(BW game);
}
