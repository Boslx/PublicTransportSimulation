package de.hhn.it.pp.components.timetable.events;

import de.hhn.it.pp.components.timetable.models.TransportationVehicle;

/**
 * Interface for the delay listener.
 */
public interface DelayListener {

  /**
   * Notifies the listener.
   * @param vehicle vehicle to listen
   * @param delay delay
   */
  void notifyDelay(TransportationVehicle vehicle, int delay);
}
