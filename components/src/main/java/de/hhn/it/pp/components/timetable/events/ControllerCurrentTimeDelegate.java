package de.hhn.it.pp.components.timetable.events;

import java.time.LocalDateTime;

/**
 * Interface for the controller time delegate.
 */
public interface ControllerCurrentTimeDelegate {
  LocalDateTime getControllerCurrentTime();
}
