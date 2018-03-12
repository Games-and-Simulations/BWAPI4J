package org.openbw.bwapi4j.command;

import java.util.Optional;

public interface BlockingWrapper<T> {

  Optional<T> getResult();
}
