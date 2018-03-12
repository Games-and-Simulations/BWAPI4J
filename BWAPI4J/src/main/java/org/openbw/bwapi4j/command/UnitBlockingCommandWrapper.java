package org.openbw.bwapi4j.command;


import org.openbw.bwapi4j.BW;
import org.openbw.bwapi4j.unit.Unit;

import java.util.Optional;

public class UnitBlockingCommandWrapper<T> implements GameCommand, BlockingWrapper<T> {

  private final UnitBlockingCommand<T> command;
  private final Unit unit;

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<T> result;

  public UnitBlockingCommandWrapper(UnitBlockingCommand<T> command, Unit unit) {
    this.command = command;
    this.unit = unit;
  }

  @Override
  public void run(BW game) {
    result = command.run(game, unit);
  }

  public Optional<T> getResult() {
    return result;
  }

  @FunctionalInterface
  public interface UnitBlockingCommand<T> {

    Optional<T> run(BW game, Unit unit);
  }
}
