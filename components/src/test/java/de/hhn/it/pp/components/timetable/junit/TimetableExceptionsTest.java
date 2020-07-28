package de.hhn.it.pp.components.timetable.junit;

import static org.junit.jupiter.api.Assertions.assertThrows;

import de.hhn.it.pp.components.timetable.exceptions.LineAlreadyServedException;
import de.hhn.it.pp.components.timetable.exceptions.LineNotFoundException;
import de.hhn.it.pp.components.timetable.exceptions.StationNotFoundException;
import de.hhn.it.pp.components.timetable.models.Line;
import de.hhn.it.pp.components.timetable.models.Station;
import de.hhn.it.pp.components.timetable.models.Stop;
import de.hhn.it.pp.components.timetable.models.TransportationVehicle;
import de.hhn.it.pp.components.timetable.models.TransportationVehicleType;
import de.hhn.it.pp.components.timetable.providers.SimpleTimetableService;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TimetableExceptionsTest {

  SimpleTimetableService timetableService;

  @BeforeEach
  void init() {
    timetableService = new SimpleTimetableService();

    // create stations and add them
    Station station1 = new Station("University", false, true, false);
    Station station2 = new Station("Allee", false, true, true);
    timetableService.addStation(station1);
    timetableService.addStation(station2);

    // create transportation vehicle and add it
    int busId = timetableService
        .addTransportationVehicle(new TransportationVehicle(TransportationVehicleType.BUS));

    // create line and link it with transportation vehicle
    Line line1 = new Line("Linie 1",
        EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
        timetableService.getTransportationVehicle(busId));

    line1.addStops(List.of(
        new Stop(LocalTime.of(8, 0), station1, "Main Station"),
        new Stop(LocalTime.of(9, 0), station2, "University")));

    try {
      int line1Id = timetableService.addLine(line1);
    } catch (LineAlreadyServedException e) {
      e.printStackTrace();
    }
  }

  @Test
  @DisplayName("Tests if LineNotFoundException is thrown by requesting the wrong line name")
  void throwLineNotFoundExceptionTest1() {
    LineNotFoundException thrown = assertThrows(
            LineNotFoundException.class, () -> {
              timetableService.getLinesByName("Linie 2", 10); //this should throw
            }
    );
  }

  @Test
  @DisplayName("Tests if LineNotFoundException is thrown by requesting the wrong line id")
  void throwLineNotFoundExceptionTest2(){
    LineNotFoundException thrown = assertThrows(LineNotFoundException.class, () -> {
      timetableService.getLine(3); //this should throw
    });
  }

  @Test
  @DisplayName("Tests if LineAlreadyServedException is thrown by adding the same Line again")
  void throwLineAlreadyServedException(){
    LineAlreadyServedException thrown = assertThrows(LineAlreadyServedException.class, () -> {
      Line line1 = new Line("Linie 1",
              EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                      DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
              timetableService.getTransportationVehicle(0));

      line1.addStops(List.of(
              new Stop(LocalTime.of(8, 0), timetableService.getStation(0), "Main Station"),
              new Stop(LocalTime.of(9, 0), timetableService.getStation(1), "University")));

      int line1Id = timetableService.addLine(line1);
    });
  }

  @Test
  @DisplayName("Tests if StationNotFoundException is thrown by requesting the wrong station name")
  void throwStationNotFoundExceptionTest1() {
    StationNotFoundException thrown = assertThrows(StationNotFoundException.class, () -> {
      timetableService.getStationsByName("Postamt", 10);
    });
  }

  @Test
  @DisplayName("Tests if StationNotFoundException is thrown by requesting the wrong station id")
  void throwStationNotFoundExceptionTest2(){
    StationNotFoundException thrown = assertThrows(StationNotFoundException.class, () -> {
      timetableService.getStation(3);
    });
  }

  //TODO TransportationVehicleBusyException missing
}
