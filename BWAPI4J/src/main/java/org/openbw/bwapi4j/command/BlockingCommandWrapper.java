package org.openbw.bwapi4j.command;

import org.openbw.bwapi4j.BW;

import java.util.Optional;

public class BlockingCommandWrapper<T> implements GameCommand, BlockingWrapper<T> {

  private final BlockingCommand<T> command;

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<T> result;

  public BlockingCommandWrapper(BlockingCommand<T> command) {
    this.command = command;
  }


  @Override
  public void run(BW game) {
    this.result = command.run(game);
  }

  public Optional<T> getResult() {
    return result;
  }


  @FunctionalInterface
  public interface BlockingCommand<T> {

    Optional<T> run(BW game);
  }

}
