package de.hhn.it.pp.components.timetable.models;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A line that can has stops at different times. It represents the route a vehicle has to take.
 */
public class Line {

  /**
   * Name of the line.
   */
  private final String name;
  /**
   * Days on which the line operates.
   */
  private final EnumSet<DayOfWeek> servingDays;
  /**
   * All stops that the line will make.
   */
  private final ArrayList<Stop> stops;
  /**
   * The vehicle that serves the line.
   */
  private final TransportationVehicle servingTransportationVehicle;
  /**
   * When a station is added, this value is set to true.
   */
  private boolean isStationAdded = false;

  /**
   * Constructor.
   * @param name        Name of the line.
   * @param servingDays Days on which the line operates.
   * @param vehicle     Transportation vehicle that is driving for this line
   */
  public Line(String name,
      EnumSet<DayOfWeek> servingDays, TransportationVehicle vehicle) {
    this.name = name;
    this.servingDays = servingDays;
    this.servingTransportationVehicle = vehicle;
    stops = new ArrayList<>();

  }

  /**
   * Getter for the serving transportation vehicle.
   * @return servingTransportationVehicle
   */
  public TransportationVehicle getServingTransportationVehicle() {
    return servingTransportationVehicle;
  }

  /**
   * Adder for Stops.
   * @param stops List of stops
   */
  public void addStops(List<Stop> stops) {
    isStationAdded = true;
    for (int i = 0; i < stops.size() - 1; i++) {
      this.stops.add(stops.get(i));
    }
  }

  /**
   * Create a stop.
   * @param time time
   * @param station station
   * @param name name
   */
  public void createStop(LocalTime time, Station station, String name) {
    stops.add(new Stop(time, station, name));
  }

  /**
   * Removes a stop.
   * @param name name of the stop
   */
  //TODO Needs some form of ID
  public void removeStop(String name) {
    int stopGet = 0;
    for (int i = 0; i < stops.size() - 1; i++) {
      if (stops.get(i).getName().equals(name)) {
        stopGet = i;
      }
    }
    stops.remove(stopGet);
  }

  /**
   * Getter for the stops.
   * @return Stops sorted by time of arrival.
   */
  public ArrayList<Stop> getStops() {
    ArrayList<Stop> buffer = new ArrayList<>();
    if (isStationAdded) {
      buffer = this.stops.stream().sorted(Comparator.comparing(Stop::getTime))
          .collect(Collectors.toCollection(ArrayList::new));
    }
    return buffer;
  }

  /**
   * Getter for a stop.
   * @param name name of a stop
   * @return stop
   */
  //TODO Needs some form of ID
  public Stop getStop(String name) {
    int stopGet = 0;
    for (int i = 0; i < stops.size() - 1; i++) {
      if (stops.get(i).getName().equals(name)) {
        stopGet = i;
      }
    }
    return stops.get(stopGet);
  }

  /**
   * Getter of for the line name.
   * @return name
   */
  public String getName() {
    return name;
  }

  /**
   * Getter for the serving days.
   * @return serving days enum set
   */
  public EnumSet<DayOfWeek> getServingDays() {
    return servingDays;
  }
}
