package de.hhn.it.pp.components.timetable.providers;

import de.hhn.it.pp.components.timetable.exceptions.LineAlreadyServedException;
import de.hhn.it.pp.components.timetable.exceptions.LineNotFoundException;
import de.hhn.it.pp.components.timetable.exceptions.StationNotFoundException;
import de.hhn.it.pp.components.timetable.exceptions.TransportationVehicleBusyException;
import de.hhn.it.pp.components.timetable.models.Line;
import de.hhn.it.pp.components.timetable.models.Station;
import de.hhn.it.pp.components.timetable.models.TransportationVehicle;
import java.util.List;

/**
 * Admin interface for the SimpleTimetableService to add or remove stations, lines and their
 * departures from the service.
 */
public interface AdminTimetableService {

  /**
   * Adds a line.
   *
   * @param line The line to add.
   * @return Id of the Line
   * @throws LineAlreadyServedException when line already was added
   */
  int addLine(Line line) throws LineAlreadyServedException;

  /**
   * Adds multiple lines.
   * @param lines list of lines to add
   */
  void addLines(List<Line> lines);

  /**
   * Removes a line.
   *
   * @param lineId id of the station.
   * @throws LineNotFoundException when line wasn't found
   */
  void removeLine(int lineId) throws LineNotFoundException;

  /**
   * Adds a station.
   *
   * @param station The station to add.
   * @return id of the Station
   */
  int addStation(Station station);

  /**
   * Removes a station.
   *
   * @param stationId id of the station to be removed.
   * @throws StationNotFoundException when station wasn't found
   */
  void removeStation(int stationId) throws StationNotFoundException;

  /**
   * Adds a TransportationVehicle.
   *
   * @param transportationVehicle The TransportationVehicle to add.
   * @return Id of the TransportationVehicle
   */
  int addTransportationVehicle(TransportationVehicle transportationVehicle);

  /**
   * Removes a TransportationVehicle.
   *
   * @param transportationVehicleId id of the transportationVehicle.
   */
  void removeTransportationVehicle(int transportationVehicleId);
}
