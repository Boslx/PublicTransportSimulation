package de.hhn.it.pp.components.timetable;

import de.hhn.it.pp.components.timetable.exceptions.LineAlreadyServedException;
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

public class DemoTimetableUsage {

  public static void main(String[] args)
      throws LineAlreadyServedException, StationNotFoundException {
    SimpleTimetableService demoService = new SimpleTimetableService();

    // create stations
    Station station1 = new Station("University", false, true, false);
    Station station2 = new Station("Allee", false, true, true);
    Station station3 = new Station("Main Station", true, true, true);
    Station station4 = new Station("Neckarturm", false, true, true);

    int station1id = demoService.addStation(station1);
    int station2id = demoService.addStation(station2);
    demoService.addStation(station3);
    demoService.addStation(station4);

    // Get all Stations that have "Main" in the name
    System.out.println(demoService.getStation(station1id));
    System.out.println(demoService.getStationsByName("Uni", 10));


    // create a bus
    int busId = demoService
        .addTransportationVehicle(new TransportationVehicle(TransportationVehicleType.BUS));

    // create a Line for the bus
    Line line10 = new Line("Linie 10", EnumSet.of(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), demoService.getTransportationVehicle(busId));
    line10.addStops(List.of(
        new Stop(LocalTime.of(8, 0), station1, "University West"),
        new Stop(LocalTime.of(10, 1), station2, "Allee West"),
        new Stop(LocalTime.of(11, 0), station4, "Neckarturm West")
        // ...
    ));
    int line10Id = demoService.addLine(line10);

    // Create a strain
    int strainId = demoService
        .addTransportationVehicle(new TransportationVehicle(TransportationVehicleType.STRAIN));

    // create a Line for the strain
    Line line11 = new Line("Linie 11", EnumSet.of(
        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), demoService.getTransportationVehicle(strainId));
    line11.addStops(List.of(
        new Stop(LocalTime.of(8, 0), station1, "University Strain West"),
        new Stop(LocalTime.of(10, 15), station2, "Allee Strain West"),
        new Stop(LocalTime.of(10, 0), station3, "Main Station South"),
        new Stop(LocalTime.of(11, 0), station4, "Neckarturm Underground West")
        // ...
    ));
    int line11Id = demoService.addLine(line11);

    ArrayList<Departure> departures = demoService
        .getDepartures(station2id, LocalDateTime.of(2020, 4, 22, 12, 30), 2880, null);

    System.out.println(demoService.getLinesByName("Linie 10", 10));
    System.out.println(demoService.getTransportationVehicle(strainId));

    System.out.println("Finished!");

    // Delay Callback in case we want to be notified if delay
    //DemoDelayListener Listener;
    //demoService.addDelayCallback(line10Id, Listener);
  }

}