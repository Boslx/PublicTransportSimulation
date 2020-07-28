package de.hhn.it.pp.components.timetable.models;

/**
 * Information about a Station.
 */
public class Station {

  /**
   * Name of the station.
   */
  private final String name;
  /**
   * Tells if there' s a travel center at the station.
   */
  private final boolean travelCenter;
  /**
   * Tells if people with walking disabilities can enter the station.
   */
  private final boolean stepFreeAccess;
  /**
   * Tells if there are toilets at the station.
   */
  private final boolean toilets;

  /**
   * Constructor.
   * @param name           Name of the station.
   * @param travelCenter   Tells if there' s a travel center at the station.
   * @param stepFreeAccess Tells if people with walking disabilities can enter the station.
   * @param toilets        Tells if there are toilets at the station.
   */
  public Station(String name, boolean travelCenter, boolean stepFreeAccess,
      boolean toilets) {
    this.name = name;
    this.travelCenter = travelCenter;
    this.stepFreeAccess = stepFreeAccess;
    this.toilets = toilets;
  }

  /**
   * Getter for the name of the station.
   * @return name
   */
  public String getName() {
    return name;
  }

  /**
   * Checker if the station is a travel center.
   * @return bool of travel center
   */
  public boolean isTravelCenter() {
    return travelCenter;
  }

  /**
   * Checker if the station is step free access.
   * @return bool of step free access
   */
  public boolean isStepFreeAccess() {
    return stepFreeAccess;
  }

  /**
   * Checker if the station has toilet.
   * @return bool of toilets
   */
  public boolean isToilets() {
    return toilets;
  }

  /**
   * ToString Method.
   * @return name
   */
  @Override
  public String toString() {
    return getName();
  }
}
