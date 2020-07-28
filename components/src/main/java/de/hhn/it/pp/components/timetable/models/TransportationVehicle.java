package de.hhn.it.pp.components.timetable.models;

import de.hhn.it.pp.components.exceptions.IllegalParameterException;
import de.hhn.it.pp.components.timetable.events.DelayListener;
import java.util.ArrayList;
import java.util.List;

public class TransportationVehicle {

  /**
   * The type of transport vehicle.
   */
  private final TransportationVehicleType type;
  /**
   * Everyone who want to be informed of vehicle delays.
   */
  private final List<DelayListener> delayListeners = new ArrayList<DelayListener>();
  /**
   * The delay that the vehicle currently has.
   */
  private int delay;
  /**
   * Indicates whether the vehicle is functional.
   */
  private boolean isFunctional = true;

  /**
   * Constructor.
   * @param type The type of transport vehicle
   */
  public TransportationVehicle(TransportationVehicleType type) {
    this.type = type;
  }

  /**
   * Returns what kind of TransportationVehicle it is.
   * @return The Type of the TransportationVehicle
   */
  public TransportationVehicleType getType() {
    return type;
  }

  /**
   * Getter for the delay.
   * @return delay
   */
  public int getDelay() {
    return delay;
  }

  /**
   * Setter for the delay.
   * @param vehicleDelay vehicle delay
   * @return delay
   */
  public TransportationVehicle setDelay(int vehicleDelay) {
    this.delay = vehicleDelay;

    for (DelayListener listener : delayListeners) {
      listener.notifyDelay(this, delay);
    }
    return this;
  }

  /**
   * Checker if vehicle is functional.
   * @return bool of functional
   */
  public boolean isFunctional() {
    return isFunctional;
  }

  /**
   * Setter for functional.
   * @param functional functional
   * @return functional
   */
  public TransportationVehicle setFunctional(boolean functional) {
    isFunctional = functional;
    return this;
  }

  /**
   * Inserts callbacks for the event of a delay.
   *
   * @param listener The listener to be added
   * @throws IllegalParameterException when listener is already registered
   */
  public void addDelayCallback(DelayListener listener) throws IllegalParameterException {
    if (delayListeners.contains(listener)) {
      throw new IllegalParameterException("Listener already registered.");
    }

    delayListeners.add(listener);
  }

  /**
   * Removes a delay callback.
   *
   * @param listener The listener to be added
   * @throws IllegalParameterException when listener is not yet registered
   */
  public void removeDelayCallback(DelayListener listener) throws IllegalParameterException {
    if (!delayListeners.contains(listener)) {
      throw new IllegalParameterException("Listener is not registered:" + listener);
    }

    delayListeners.remove(listener);
  }
}
