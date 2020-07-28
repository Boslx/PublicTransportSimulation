package de.hhn.it.pp.components.timetable;

import de.hhn.it.pp.components.timetable.events.ControllerCurrentTimeDelegate;
import de.hhn.it.pp.components.timetable.exceptions.LineNotFoundException;
import de.hhn.it.pp.components.timetable.exceptions.StationNotFoundException;
import de.hhn.it.pp.components.timetable.models.Departure;
import de.hhn.it.pp.components.timetable.models.Line;
import de.hhn.it.pp.components.timetable.models.Station;
import de.hhn.it.pp.components.timetable.models.TransportationVehicle;
import java.time.LocalDateTime;
import java.util.List;

public interface TimetableService {

  /**
   * Returns a list of registered station with the entered name.
   *
   * @param name  Name of the station
   * @param limit The max number of stations to be returned
   * @return List of registered stations
   */
  List<Station> getStationsByName(String name, int limit) throws StationNotFoundException;

  /**
   * Returns the station with the given id.
   *
   * @param stationId id of the station
   * @return the station
   * @throws StationNotFoundException if the id does not exist
   */
  Station getStation(int stationId) throws StationNotFoundException;


  /**
   * Returns a list of registered lines with the entered name.
   *
   * @param name  Name of the line
   * @param limit The max number of stations to be returned
   * @return List of registered stations
   */
  List<Line> getLinesByName(String name, int limit) throws LineNotFoundException;

  /**
   * Returns the line with the given id.
   *
   * @param lineId id of the line
   * @return the station
   * @throws LineNotFoundException if the id does not exist
   */
  Line getLine(int lineId) throws LineNotFoundException;

  /**
   * Returns the TransportationVehicle with the given id.
   *
   * @param transportationVehicleId id of the TransportationVehicle
   * @return the TransportationVehicle
   */
  TransportationVehicle getTransportationVehicle(int transportationVehicleId);

  /**
   * All lines departing at the specified date from a specific line.
   *
   * @param stationId station from which the departures are requested.
   * @param dateTime  Date and Time from when
   * @param minutes   How many minutes maximum the departure may be in the future
   * @return All lines that operate in ascending order from the specified time
   */
  List<Departure> getDepartures(int stationId, LocalDateTime dateTime, long minutes,
      ControllerCurrentTimeDelegate currentTimeDelegate) throws StationNotFoundException;
}
