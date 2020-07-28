package de.hhn.it.pp.components.timetable.providers;

import de.hhn.it.pp.components.timetable.TimetableService;
import de.hhn.it.pp.components.timetable.events.ControllerCurrentTimeDelegate;
import de.hhn.it.pp.components.timetable.exceptions.LineAlreadyServedException;
import de.hhn.it.pp.components.timetable.exceptions.LineNotFoundException;
import de.hhn.it.pp.components.timetable.exceptions.StationNotFoundException;
import de.hhn.it.pp.components.timetable.models.Departure;
import de.hhn.it.pp.components.timetable.models.Line;
import de.hhn.it.pp.components.timetable.models.Station;
import de.hhn.it.pp.components.timetable.models.TransportationVehicle;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimpleTimetableService implements AdminTimetableService, TimetableService {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
      .getLogger(SimpleTimetableService.class);

  private final HashMap<Integer, Line> lines = new HashMap<>();
  private final HashMap<Integer, TransportationVehicle> vehicles = new HashMap<>();
  private final HashMap<Integer, Station> stations = new HashMap<>();

  /**
   * Returns a list of registered station containing the entered name.
   *
   * @param name  Name of the station
   * @param limit The max number of stations to be returned
   * @return List of registered stations
   */
  @Override
  public List<Station> getStationsByName(String name, int limit) throws StationNotFoundException {
    logger.info("getStationsByName requested with name {}, limit {}", name, limit);
    Stream<Station> stream = stations.values().stream()
        .filter(station -> station.getName().contains(name));

    List<Station> checkList = stream.limit(limit).collect(Collectors.toList());
    if (checkList.size() == 0) {
      throw new StationNotFoundException(); //check if we got something from our stream
    }
    return checkList;
  }

  /**
   * Returns the station with the given id.
   *
   * @param stationId id of the station
   * @return the station
   */
  @Override
  public Station getStation(int stationId) throws StationNotFoundException {
    logger.info("getStation requested with id {}", stationId);
    Station tempStation = stations.get(stationId);
    if (tempStation == null) {
      throw new StationNotFoundException();
    }
    return tempStation;
  }

  /**
   * Getter for the station count.
   *
   * @return The total number of stations that exist
   */
  public int getStationCount() {
    return stations.size();
  }

  /**
   * Returns a list of registered lines with the entered name.
   *
   * @param name  Name of the line
   * @param limit The max number of stations to be returned
   * @return List of registered stations
   */
  @Override
  public List<Line> getLinesByName(String name, int limit) throws LineNotFoundException {
    logger.info("getLinesByName requested with name {} and limit {}", name, limit);
    Stream<Line> stream = lines.values().stream().filter(line -> line.getName().contains(name));

    List<Line> checkLine = stream.limit(limit).collect(Collectors.toList());
    if (checkLine.size() == 0) {
      throw new LineNotFoundException();
    }
    return checkLine;
  }

  /**
   * Returns the line with the given id.
   *
   * @param lineId id of the line
   * @return the station
   * @throws LineNotFoundException if the id does not exist
   */
  @Override
  public Line getLine(int lineId) throws LineNotFoundException {
    logger.info("getLine requested with id {}", lineId);
    Line tempLine = lines.get(lineId);
    if (tempLine == null) {
      throw new LineNotFoundException();
    }
    return tempLine;
  }

  /**
   * Getter for the lines count.
   *
   * @return The total number of lines that exist
   */
  public int getLinesCount() {
    return lines.size();
  }

  /**
   * Returns the TransportationVehicle with the given id.
   *
   * @param transportationVehicleId id of the TransportationVehicle
   * @return the TransportationVehicle
   */
  @Override
  public TransportationVehicle getTransportationVehicle(int transportationVehicleId) {
    logger.info("getTransportationVehicle requested with id {}", transportationVehicleId);
    return vehicles.get(transportationVehicleId);
  }

  /**
   * Getter for the transportation vehicle count.
   *
   * @return The total number of lines that exist
   */
  public int getTransportationVehicleCount() {
    return vehicles.size();
  }

  /**
   * All lines departing at the specified date from a specific line.
   *
   * @param stationId      station from which the departures are requested.
   * @param date           Date from when
   * @param rangeInMinutes range in minutes
   * @return All lines that operate in ascending order from the specified time
   */
  @Override
  public ArrayList<Departure> getDepartures(int stationId, LocalDateTime date, long rangeInMinutes,
      ControllerCurrentTimeDelegate currentTimeDelegate)
      throws StationNotFoundException {
    logger.info("getDepartures requested with stationId {}, date {}, rangeInMinutes {}", stationId,
        date,
        rangeInMinutes);
    ArrayList<Departure> departures = new ArrayList<>();

    Station station = getStation(stationId);

    for (var focusedLine : lines.values()) {
      if (focusedLine.getServingTransportationVehicle().isFunctional()) {
        // Gets the stop with the stop Id in the line
        var stop = focusedLine.getStops().stream().filter(s -> s.getStation().equals(station))
            .findAny()
            .orElse(null);

        // If the stop isn't in the line, go on with the next one
        if (stop != null) {
          // TODO: Don't look on days which are latter than the time limit
          for (DayOfWeek day : focusedLine.getServingDays()) {
            LocalDateTime time = date.toLocalDate().with(TemporalAdjusters.nextOrSame(day))
                .atTime(stop.getTime());

            if (// Make sure the time is after the planned time + delay
                date
                    .isBefore(
                        time.plusMinutes(focusedLine.getServingTransportationVehicle().getDelay()))
                    // Make sure the departure is within the range in minutes
                    && ChronoUnit.MINUTES.between(date, time) <= rangeInMinutes) {
              departures.add(new Departure(focusedLine, stop, time, currentTimeDelegate));
            }
          }
        }
      }
    }

    // Sort in ascending order according to the concrete departure time
    departures.sort(Comparator.comparing(o -> o.getConcreteTime()));

    return departures;
  }

  /**
   * Adds a line.
   *
   * @param line The line to add.
   * @throws LineAlreadyServedException when line was added
   * @return Id of the Line
   */
  @Override
  public int addLine(Line line) throws LineAlreadyServedException {
    logger.info("addLine with line {}", line);
    try {
      for (Line checkLine : lines.values()) {
        if (checkLine.getName().equals(line.getName())) {
          throw new LineAlreadyServedException();
        }
      }
    } catch (LineAlreadyServedException e) {
      e.printStackTrace();
      logger.error("addLine threw LineAlreadyServedException");
      throw e;
    }

    int id = lines.size();
    lines.put(id, line);
    return id;
  }

  /**
   * Adds multiple lines.
   * @param line list of lines to add
   */
  public void addLines(List<Line> line) {
    logger.info("addLines with lines {}", line);
    for (int i = 0; i < line.size(); i++) {
      lines.put(i, line.get(i));
    }
  }

  /**
   * Removes a line.
   * @param lineId id of the station.
   */
  @Override
  public void removeLine(int lineId) throws LineNotFoundException {
    logger.info("removeline with line {}", lineId);
    if (!lines.containsKey(lineId)) {
      throw new LineNotFoundException();
    } else {
      lines.remove(lineId);
    }
  }

  /**
   * Adds a station.
   *
   * @param station The station to add.
   * @return id of the Station
   */
  @Override
  public int addStation(Station station) {
    logger.info("addStation with station", station);
    int id = stations.size();
    stations.put(id, station);
    return id;
  }

  /**
   * Removes a station.
   *
   * @param stationId id of the station to be removed.
   */
  @Override
  public void removeStation(int stationId) throws StationNotFoundException {
    logger.info("removeStation with id {}", stationId);
    if (!stations.containsKey(stationId)) {
      throw new StationNotFoundException();
    } else {
      stations.remove(stationId);
    }
  }

  /**
   * Adds a TransportationVehicle.
   *
   * @param transportationVehicle The TransportationVehicle to add.
   * @return Id of the TransportationVehicle
   */
  @Override
  public int addTransportationVehicle(TransportationVehicle transportationVehicle) {
    logger.info("addTransportationVehicle with vehicle {}", transportationVehicle);
    int id = vehicles.size();
    vehicles.put(id, transportationVehicle);
    return id;
  }

  /**
   * Removes a TransportationVehicle.
   *
   * @param transportationVehicleId id of the transportationVehicle.
   */
  @Override
  public void removeTransportationVehicle(int transportationVehicleId) {
    logger.info("removeTransportationVehicle with vehicle {}", transportationVehicleId);
    vehicles.remove(transportationVehicleId);
  }
}
