package de.hhn.it.pp.components.timetable.junit;

import de.hhn.it.pp.components.exceptions.IllegalParameterException;
import de.hhn.it.pp.components.timetable.events.DelayListener;
import de.hhn.it.pp.components.timetable.exceptions.LineAlreadyServedException;
import de.hhn.it.pp.components.timetable.exceptions.LineNotFoundException;
import de.hhn.it.pp.components.timetable.exceptions.StationNotFoundException;
import de.hhn.it.pp.components.timetable.models.Departure;
import de.hhn.it.pp.components.timetable.models.Line;
import de.hhn.it.pp.components.timetable.models.Station;
import de.hhn.it.pp.components.timetable.models.Stop;
import de.hhn.it.pp.components.timetable.models.TransportationVehicle;
import de.hhn.it.pp.components.timetable.models.TransportationVehicleType;
import de.hhn.it.pp.components.timetable.providers.SimpleTimetableService;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleTimetableServiceTest {

  SimpleTimetableService demoTimetableService;
  int idStation1;
  int idStation2;
  int idStation3;
  int idStation4;
  int idBus1;
  int idSTrain1;
  int idLine1;
  int idLine2;
  Station station1; //global station for testing
  TransportationVehicle bus1; //global transportationVehicle for testing
  Line line1; //global line for testing
  Stop stop1; //global stop for testing

  @BeforeEach
  void setup() {
    demoTimetableService = new SimpleTimetableService();

    // create stations and add them
    station1 = new Station("University", false, true, false);
    Station station2 = new Station("Allee", false, true, true);
    Station station3 = new Station("Main Station", true, true, true);
    Station station4 = new Station("Neckarturm", false, true, true);
    idStation1 = demoTimetableService.addStation(station1);
    idStation2 = demoTimetableService.addStation(station2);
    idStation3 = demoTimetableService.addStation(station3);
    idStation4 = demoTimetableService.addStation(station4);

    // create transportation vehicles and add them
    bus1 = new TransportationVehicle((TransportationVehicleType.BUS));
    TransportationVehicle strain1 = new TransportationVehicle(TransportationVehicleType.STRAIN);
    idBus1 = demoTimetableService.addTransportationVehicle(bus1);
    idSTrain1 = demoTimetableService.addTransportationVehicle(strain1);

    // create lines and add them
    line1 = new Line("Linie 1", EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
        demoTimetableService.getTransportationVehicle(idBus1));
    Line line2 = new Line("Linie 2", EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
        demoTimetableService.getTransportationVehicle(idSTrain1));
    try {
      idLine1 = demoTimetableService.addLine(line1);
      idLine2 = demoTimetableService.addLine(line2);
    } catch (LineAlreadyServedException e) {
      e.printStackTrace();
    }

    stop1 = new Stop(LocalTime.of(8, 0), demoTimetableService.getStation(idStation1),
        "University West");
    // add stops to the lines
    demoTimetableService.getLine(idLine1).addStops(List.of(
        stop1,
        new Stop(LocalTime.of(10, 1), demoTimetableService.getStation(idStation2), "Allee West"),
        new Stop(LocalTime.of(11, 0), demoTimetableService.getStation(idStation4),
            "Neckarturm West")
        // ...
    ));
    demoTimetableService.getLine(idLine2).addStops(List.of(
        new Stop(LocalTime.of(8, 0), demoTimetableService.getStation(idStation1),
            "University Strain West"),
        new Stop(LocalTime.of(10, 15), demoTimetableService.getStation(idStation2),
            "Allee Strain West"),
        new Stop(LocalTime.of(10, 0), demoTimetableService.getStation(idStation3),
            "Main Station South"),
        new Stop(LocalTime.of(11, 0), demoTimetableService.getStation(idStation4),
            "Neckarturm Underground West")
        // ...
    ));
  }

  @Test
  @DisplayName("Tests if getStationByName() returns the correct object")
  void getStationsByName() throws StationNotFoundException {
    assertEquals(station1, demoTimetableService.getStationsByName("University", 1).get(0));
  }

  @Test
  @DisplayName("Tests if the getStation() returns the correct object")
  void getStation() throws StationNotFoundException {
    assertEquals(station1, demoTimetableService.getStation(idStation1));
  }

  @Test
  @DisplayName("Tests if getLinesByName() returns the correct object")
  void getLinesByName() throws LineNotFoundException {
    assertEquals(line1, demoTimetableService.getLinesByName("Linie 1", 1).get(0));
  }

  @Test
  @DisplayName("Tests if getLine() returns the correct object")
  void getLine() throws LineNotFoundException {
    assertEquals(line1, demoTimetableService.getLine(idLine1));
  }

  @Test
  @DisplayName("Tests if getTransportationVehicle() returns the correct object")
  void getTransportationVehicle() {
    assertEquals(bus1, demoTimetableService.getTransportationVehicle(idBus1));
  }

  @Test
  @DisplayName("Tests if getDepartures() returns the correct list of objects")
  void getDepartures() {
    ArrayList<Departure> actual;
    actual = demoTimetableService.getDepartures(idStation1, LocalDateTime.of(2020,7,22,7,0), 120, null);

    assertEquals(line1, actual.get(0).getLine());
    assertEquals(stop1, actual.get(0).getFocusedStop());
    assertEquals(LocalDateTime.of(2020,7,22,8,0), actual.get(0).getConcreteTime());
  }

  @Test
  @DisplayName("Tests if addLine() returns the correct lineId")
  void addLine() {
    TransportationVehicle testBus = new TransportationVehicle(TransportationVehicleType.BUS);
    Line testLine = new Line("testLine", EnumSet.of(DayOfWeek.MONDAY), testBus);
    int testLineId = 0;
    try {
      testLineId = demoTimetableService.addLine(testLine);
    } catch (LineAlreadyServedException e) {
      e.printStackTrace();
    }
    assertEquals(2, testLineId);
  }

  @Test
  @DisplayName("Tests if removeLine() removes the correct line")
  void removeLine() {
    demoTimetableService.removeLine(idLine1);
    try {
      assertNotEquals(line1, demoTimetableService.getLine(idLine1));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  @DisplayName("Tests if addStation() returns the correct stationId")
  void addStation() {
    Station testStation = new Station("Test", false, true, true);
    int testStationId = demoTimetableService.addStation(testStation);
    assertEquals(4, testStationId);
  }

  @Test
  @DisplayName("Tests if removeStation() removes the correct station")
  void removeStation() {
    demoTimetableService.removeStation(idStation1);
    try {
      assertNotEquals(station1, demoTimetableService.getStation(idStation1));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  @DisplayName("Tests if addTransportationVehicle returns the correct transportationVehicleId")
  void addTransportationVehicle() {
    TransportationVehicle testBus = new TransportationVehicle(TransportationVehicleType.BUS);
    int testBusId = demoTimetableService.addTransportationVehicle(testBus);
    assertEquals(2, testBusId);
  }

  @Test
  @DisplayName("Tests if removeTransportationVehicle() removes the correct transportationVehicle")
  void removeTransportationVehicle() {
    demoTimetableService.removeTransportationVehicle(idBus1);
    assertNotEquals(bus1, demoTimetableService.getTransportationVehicle(idBus1));
  }

  @Test
  @DisplayName("Tests if checkDelayListeners method works properly")
  void checkDelayListeners() {
    Boolean[] hasBeenSet = {false};

    TransportationVehicle bus = new TransportationVehicle(TransportationVehicleType.BUS);
    DelayListener listener = (vehicle, delay) -> hasBeenSet[0] = true;

    try {
      bus.addDelayCallback(listener);
    } catch (IllegalParameterException e) {
      e.printStackTrace();
    }

    bus.setDelay(15);
    assertTrue(hasBeenSet[0]);
  }
}