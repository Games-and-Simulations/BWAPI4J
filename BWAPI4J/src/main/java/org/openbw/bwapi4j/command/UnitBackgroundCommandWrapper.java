package org.openbw.bwapi4j.command;


import org.openbw.bwapi4j.BW;
import org.openbw.bwapi4j.unit.Unit;

public class UnitBackgroundCommandWrapper implements GameCommand {

  private final UnitBackgroundCommand command;
  private final Unit unit;

  public UnitBackgroundCommandWrapper(UnitBackgroundCommand command, Unit unit) {
    this.command = command;
    this.unit = unit;
  }

  @Override
  public void run(BW game) {
    command.run(game, unit);
  }

  @FunctionalInterface
  public interface UnitBackgroundCommand {

    void run(BW game, Unit unit);
  }

}
