package de.hhn.it.pp.components.timetable.models;

import java.time.LocalTime;

/**
 * A stop of a line.
 */
public class Stop {

  /**
   * Time at which the station is reached.
   */
  private final LocalTime time;

  /**
   * Station to be reached at this stop.
   */
  private Station station;

  /**
   * Name of the station.
   */
  private String name;

  /**
   * Constructor.
   * @param time    Time at which the station is reached
   * @param station Minute at which the station is reached.
   */
  public Stop(LocalTime time, Station station, String name) {
    this.time = time;
    this.station = station;
    this.name = name;
  }

  /**
   * Getter for the time.
   * @return time
   */
  public LocalTime getTime() {
    return time;
  }

  /**
   * Getter for the name.
   * @return name
   */
  public Station getStation() {
    return station;
  }

  public String getName() {
    return name;
  }
}
