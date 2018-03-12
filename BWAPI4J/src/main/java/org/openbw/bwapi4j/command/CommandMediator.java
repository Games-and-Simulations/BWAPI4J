package org.openbw.bwapi4j.command;


import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

public enum CommandMediator {
  CommandMediator; // static singleton

  private final ConcurrentLinkedQueue<GameCommand> commands;

  CommandMediator() {
    commands = new ConcurrentLinkedQueue<>();
  }

  /**
   * Non-blocking publishing of messages to the game.
   *
   * The message will be delivered within current frame.
   */
  public void publishBackground(GameCommand command) {
    commands.add(command);
  }

  /**
   * Blocking publishing of messages to the game, which also returns value.
   *
   * It flattens chain of Optional to simplify later code.
   *
   * Optional.empty means that the value could not be retrieved
   * (due to unlikely InterruptedException) or because
   * the BlockingCommand itself returned Optional.empty
   */
  public <T>
  Optional<T> publishBlocking(BlockingCommandWrapper.BlockingCommand<T> command) {
    BlockingCommandWrapper<T> wrapper = new BlockingCommandWrapper<>(command);
    return publishBlockingWrapper(wrapper);
  }

  /**
   * Blocking publishing of messages to the game, which also returns value.
   *
   * This can be useful for generic blocking game calls, that want also other
   * parameters in the lambda (like bwapi.Unit)
   */
  public <T, V extends BlockingWrapper<T> & GameCommand>
  Optional<T> publishBlockingWrapper(V wrapper) {
    //noinspection SynchronizationOnLocalVariableOrMethodParameter
    synchronized (wrapper) {
      commands.add(wrapper);
      try {
        wrapper.wait();
        return wrapper.getResult();

      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return Optional.empty();
  }


  public Optional<GameCommand> poll() {
    return Optional.ofNullable(commands.poll());
  }

  public Stream<GameCommand> poll(int numCommands) {
    List<GameCommand> s = new LinkedList<>();
    for (int i = 0; i < numCommands; i++) {
      GameCommand command = commands.poll();
      if (command == null) {
        break;
      }

      s.add(command);
    }
    return s.stream();
  }

  public Iterable<GameCommand> commands() {
    return commands;
  }
}
