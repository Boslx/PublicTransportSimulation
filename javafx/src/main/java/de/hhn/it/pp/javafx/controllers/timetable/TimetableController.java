package de.hhn.it.pp.javafx.controllers.timetable;

import de.hhn.it.pp.components.timetable.events.ControllerCurrentTimeDelegate;
import de.hhn.it.pp.components.timetable.exceptions.StationNotFoundException;
import de.hhn.it.pp.components.timetable.models.Departure;
import de.hhn.it.pp.components.timetable.models.Line;
import de.hhn.it.pp.components.timetable.models.Station;
import de.hhn.it.pp.components.timetable.models.Stop;
import de.hhn.it.pp.components.timetable.models.TransportationVehicle;
import de.hhn.it.pp.components.timetable.models.TransportationVehicleType;
import de.hhn.it.pp.components.timetable.providers.SimpleTimetableService;
import de.hhn.it.pp.javafx.controllers.Controller;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

/**
 * Controller of the timetable
 */
public class TimetableController extends Controller implements Initializable,
    ControllerCurrentTimeDelegate {

  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(TimetableController.class);

  @FXML
  private Button buttonDelay;
  @FXML
  private ChoiceBox<StationSelect> choiceBoxStation;
  @FXML
  private CheckBox checkBoxisRealtime;
  @FXML
  private Spinner<Integer> spinnerHour;
  @FXML
  private Spinner<Integer> spinnerMinute;
  @FXML
  private DatePicker datePickerDatum;
  @FXML
  private ImageView iconWheelchair;
  @FXML
  private ImageView iconInfo;
  @FXML
  private ImageView iconToilet;
  @FXML
  private TableView<Departure> table;
  @FXML
  private TableColumn<Departure, String> columnTyp;
  @FXML
  private TableColumn<Departure, String> columLinie;
  @FXML
  private TableColumn<Departure, String> columZiel;
  @FXML
  private TableColumn<Departure, String> columAbfahrtIn;
  private SimpleTimetableService demoService;
  private int observedStationId;
  private Stage timetableTransportationVehicleDelayManagerWindow;
  private ScheduledExecutorService realtimeUpdateScheduler;
  private ScheduledFuture realtimeUpdateSchedulerExecution;
  private boolean enableUserTimeInput = true;

  /**
   * Called to initialize a controller after its root element has been completely processed.
   *
   * @param location  The location used to resolve relative paths for the root object, or {@code
   *                  null} if the location is not known.
   * @param resources The resources used to localize the root object, or {@code null} if
   */
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    try {
      buildScenario();
      logger.info("Successfully build scenario!");
    } catch (Exception e) {
      logger.error("Failed to build scenario", e);
      return;
    }

    try {
      SpinnerValueFactory<Integer> hourValueFactory =
          new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 12);
      spinnerHour.setValueFactory(hourValueFactory);
      spinnerHour.valueProperty().addListener(this::onUpdateSpinnerHours);

      SpinnerValueFactory<Integer> minuteValueFactory =
          new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0);
      spinnerMinute.setValueFactory(minuteValueFactory);
      spinnerMinute.valueProperty().addListener(this::onUpdateSpinnerMinutes);

      datePickerDatum.setValue(LocalDate.now());
      datePickerDatum.valueProperty().addListener(this::onUpdateDatePicker);

      checkBoxisRealtime.selectedProperty().addListener(
          this::onCheckBoxChanged);

      logger.info("Successfully configured the controls!");
    } catch (Exception e) {
      logger.error("Failed to configured the controls!", e);
      return;
    }

    try {
      populateChoiceBox();
      logger.info("Successfully populated choiceBox!");
    } catch (Exception e) {
      logger.error("Failed to populated choiceBox!", e);
      return;
    }

    try {
      setDataPropertiesToColumns();
      logger.info("Successfully set DataProperties to Columns!");
    } catch (Exception e) {
      logger.error("Failed to set DataProperties to Columns!", e);
      return;
    }

    try {
      FXMLLoader fxmlLoader = new FXMLLoader();
      fxmlLoader.setLocation(
          getClass().getResource("/fxml/TimetableTransportationVehicleDelayManager.fxml"));
      TimetableTransportationVehicleDelayManagerController controller =
          new TimetableTransportationVehicleDelayManagerController(demoService);
      fxmlLoader
          .setController(controller);
      Scene scene = new Scene(fxmlLoader.load(), 600, 400);
      timetableTransportationVehicleDelayManagerWindow = new Stage();
      timetableTransportationVehicleDelayManagerWindow.setTitle("Verspätungsmanager");
      timetableTransportationVehicleDelayManagerWindow.setScene(scene);
      timetableTransportationVehicleDelayManagerWindow
          .setOnHidden(e -> controller.saveDelayToTransportationVehicle());

      logger.info("Successfully provide the timetableTransportationVehicleDelayManager!");
    } catch (Exception e) {
      logger.error("Failed to provide the timetableTransportationVehicleDelayManager!", e);
      return;
    }
    try {
      realtimeUpdateScheduler = Executors.newScheduledThreadPool(1, new DaemonThreadFactory());

    } catch (Exception e) {
      logger.error("Failed to create newSingleThreadScheduledExecutor!", e);
    }
  }


  private void populateChoiceBox() throws StationNotFoundException {
    for (int i = 0; i < demoService.getStationCount(); i++) {
      StationSelect stationSelect = new StationSelect();
      stationSelect.stationId = i;
      stationSelect.stationName = demoService.getStation(i).getName();

      choiceBoxStation.getItems().add(stationSelect);
    }

    choiceBoxStation.getSelectionModel().selectedIndexProperty()
        .addListener(this::onStationSelect);

    choiceBoxStation.getSelectionModel().selectFirst();
  }

  private void setObservedStation(int stationId) throws StationNotFoundException {
    observedStationId = stationId;

    updatePictograms();
    updateDepartures();
  }

  void updateDepartures() throws StationNotFoundException {
    synchronized (this) {
      ArrayList<Departure> departures = demoService
          .getDepartures(observedStationId, getControllerCurrentTime(), 10080, this);
      table.setItems(FXCollections.observableArrayList(departures));
    }
  }

  /**
   * Sets the columns to show the right information
   */
  private void setDataPropertiesToColumns() {
    columnTyp.setCellValueFactory(new PropertyValueFactory<Departure, String>("lineType"));
    columLinie.setCellValueFactory(new PropertyValueFactory<Departure, String>("lineName"));
    columZiel
        .setCellValueFactory(new PropertyValueFactory<Departure, String>("lastStopStationName"));
    columAbfahrtIn
        .setCellValueFactory(new PropertyValueFactory<Departure, String>("arrivalMessage"));

  }

  /**
   * Executed when the delaymanager Button is clicked
   * @throws StationNotFoundException When the current station is not found
   */
  @FXML
  private void onDelayManagerButtonClick(ActionEvent event) {
    timetableTransportationVehicleDelayManagerWindow.showAndWait();
    updateDepartures();
  }

  /**
   * Updates the pictograms to show the information of the current station
   * @throws StationNotFoundException If the current station is not found
   */
  private void updatePictograms() throws StationNotFoundException {
    Station station = demoService.getStation(observedStationId);

    iconWheelchair.setVisible(station.isStepFreeAccess());
    iconInfo.setVisible(station.isTravelCenter());
    iconToilet.setVisible(station.isToilets());
  }

  private void buildScenario() throws StationNotFoundException {
    demoService = new SimpleTimetableService(); // demo Object
    //create stations and add them
    Station station0 = new Station("Flein Gänsäcker", false, true, false);
    Station station1 = new Station("Flein Rathaus", false, false, false);
    Station station2 = new Station("Max-von-Laue-Straße", false, false, false);
    Station station3 = new Station("Sontheim Jörg-Ratgeb-Platz", false, false, false);
    Station station4 = new Station("Sontheim Hochschule", false, false, false);
    Station station5 = new Station("Südbahnhof Süd", false, false, false);
    Station station6 = new Station("Alle Post", false, false, false);
    Station station7 = new Station("Rathaus", false, false, false);
    Station station8 = new Station("Hauptbahnhof", true, true, true);
    Station station9 = new Station("Wilhelm-Leuschner-Straße", false, false, false);
    Station station10 = new Station("Schanz Nord", false, false, false);
    Station station11 = new Station("Frankenbach Maihalde", false, false, false);
    Station station12 = new Station("Böllinger Höfe Wannenäcker", false, false, false);
    Station station13 = new Station("Böllinger Höfe Mühlrainstraße", false, false, false);
    Station station14 = new Station("Kirchausen Schloßstraße", false, false, false);
    Station station15 = new Station("Wimpfener Weg", false, false, false);
    demoService.addStation(station0);
    demoService.addStation(station1);
    demoService.addStation(station2);
    demoService.addStation(station3);
    demoService.addStation(station4);
    demoService.addStation(station5);
    demoService.addStation(station6);
    demoService.addStation(station7);
    demoService.addStation(station8);
    demoService.addStation(station9);
    demoService.addStation(station10);
    demoService.addStation(station11);
    demoService.addStation(station12);
    demoService.addStation(station13);
    demoService.addStation(station14);
    demoService.addStation(station15);

    // create bus and add it
    TransportationVehicle bus0 = new TransportationVehicle(TransportationVehicleType.BUS);
    demoService.addTransportationVehicle(bus0);
    TransportationVehicle bus1 = new TransportationVehicle(TransportationVehicleType.BUS);
    demoService.addTransportationVehicle(bus1);
    TransportationVehicle bus2 = new TransportationVehicle(TransportationVehicleType.BUS);
    demoService.addTransportationVehicle(bus2);
    TransportationVehicle bus3 = new TransportationVehicle(TransportationVehicleType.BUS);
    demoService.addTransportationVehicle(bus3);

    // Add random delay to the busses
    for(int i=0; i< demoService.getTransportationVehicleCount(); i++){
      Random random = new Random();
      demoService.getTransportationVehicle(i).setDelay(random.nextInt(8));
    }

    // create lines and add them
    Line line61N0 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus0);
    line61N0.addStops(List.of(
        new Stop(LocalTime.of(5,6), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(5,7), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(5,12), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(5,13), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(5,14), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(5,18), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(5,25), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(5,26), station7, "Rathaus"),
        new Stop(LocalTime.of(5,29), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(5,33), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(5,35), station10, "Schanz Nord"),
        new Stop(LocalTime.of(5,41), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(5,48), station12, "Böllinger Höfe Wannenäckerstraße"),
        new Stop(LocalTime.of(5,51), station13, "Böllinger Höfe Mühlrainstraße"),
        new Stop(LocalTime.of(5,52), station12, "Böllinger Höfe Wannenäckerstraße"),
        new Stop(LocalTime.of(5,57), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(6,0), station15, "Wimpfener Weg")
        ));

    Line line61N1 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus1);
    line61N1.addStops(List.of(
        new Stop(LocalTime.of(5,41), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(5,42), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(5,47), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(5,48), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(5,49), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(5,53), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(6,0), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(6,1), station7, "Rathaus"),
        new Stop(LocalTime.of(6,4), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(6,8), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(6,10), station10, "Schanz Nord"),
        new Stop(LocalTime.of(6,16), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(6,23), station12, "Böllinger Höfe Wannenäckerstraße"),
        new Stop(LocalTime.of(6,26), station13, "Böllinger Höfe Mühlrainstraße"),
        new Stop(LocalTime.of(6,27), station12, "Böllinger Höfe Wannenäckerstraße"),
        new Stop(LocalTime.of(6,32), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(6,35), station15, "Wimpfener Weg")
    ));

    Line line61N2 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus2);
    line61N2.addStops(List.of(
        new Stop(LocalTime.of(6,31), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(6,32), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(6,37), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(6,38), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(6,39), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(6,43), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(6,50), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(6,51), station7, "Rathaus"),
        new Stop(LocalTime.of(6,54), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(6,58), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(7,0), station10, "Schanz Nord"),
        new Stop(LocalTime.of(7,6), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(7,12), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(7,17), station15, "Wimpfener Weg")
    ));

    Line line61N3 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus3);
    line61N3.addStops(List.of(
        new Stop(LocalTime.of(7,1), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(7,2), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(7,7), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(7,8), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(7,9), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(7,13), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(7,20), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(7,21), station7, "Rathaus"),
        new Stop(LocalTime.of(7,24), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(7,28), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(7,30), station10, "Schanz Nord"),
        new Stop(LocalTime.of(7,36), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(7,42), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(7,47), station15, "Wimpfener Weg")
    ));

    Line line61N4 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus0);
    line61N4.addStops(List.of(
        new Stop(LocalTime.of(7,31), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(7,32), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(7,37), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(7,38), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(7,39), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(7,43), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(7,50), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(7,51), station7, "Rathaus"),
        new Stop(LocalTime.of(7,54), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(7,58), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(8,0), station10, "Schanz Nord"),
        new Stop(LocalTime.of(8,6), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(8,12), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(8,17), station15, "Wimpfener Weg")
    ));

    Line line61N5 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus1);
    line61N5.addStops(List.of(
        new Stop(LocalTime.of(8,1), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(8,2), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(8,7), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(8,8), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(8,9), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(8,13), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(8,20), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(8,21), station7, "Rathaus"),
        new Stop(LocalTime.of(8,24), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(8,28), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(8,30), station10, "Schanz Nord"),
        new Stop(LocalTime.of(8,36), station11, "Frankenbach Maihalde")
    ));

    Line line61N6 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus2);
    line61N6.addStops(List.of(
        new Stop(LocalTime.of(8,16), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(8,17), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(8,22), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(8,23), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(8,24), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(8,28), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(8,35), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(8,36), station7, "Rathaus"),
        new Stop(LocalTime.of(8,39), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(8,43), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(8,47), station10, "Schanz Nord"),
        new Stop(LocalTime.of(8,52), station11, "Frankenbach Maihalde")
    ));

    Line line61N7 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus3);
    line61N7.addStops(List.of(
        new Stop(LocalTime.of(8,31), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(8,32), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(8,37), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(8,38), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(8,39), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(8,43), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(8,50), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(8,51), station7, "Rathaus"),
        new Stop(LocalTime.of(8,54), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(8,58), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(9,0), station10, "Schanz Nord"),
        new Stop(LocalTime.of(9,6), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(9,12), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(9,17), station15, "Wimpfener Weg")
    ));

    Line line61N8 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus0);
    line61N8.addStops(List.of(
        new Stop(LocalTime.of(9,7), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(9,8), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(9,9), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(9,12), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(9,19), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(9,20), station7, "Rathaus"),
        new Stop(LocalTime.of(9,23), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(9,27), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(9,30), station10, "Schanz Nord"),
        new Stop(LocalTime.of(9,35), station11, "Frankenbach Maihalde")
    ));

    Line line61N9 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus1);
    line61N9.addStops(List.of(
        new Stop(LocalTime.of(9,16), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(9,17), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(9,22), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(9,23), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(9,24), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(9,28), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(9,35), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(9,36), station7, "Rathaus"),
        new Stop(LocalTime.of(9,39), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(9,43), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(9,47), station10, "Schanz Nord"),
        new Stop(LocalTime.of(9,52), station11, "Frankenbach Maihalde")
    ));

    Line line61N10 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus2);
    line61N10.addStops(List.of(
        new Stop(LocalTime.of(9,37), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(9,38), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(9,39), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(9,42), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(9,49), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(9,50), station7, "Rathaus"),
        new Stop(LocalTime.of(9,54), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(9,58), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(10,0), station10, "Schanz Nord"),
        new Stop(LocalTime.of(10,6), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(10,12), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(10,17), station15, "Wimpfener Weg")
    ));

    Line line61N11 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus3);
    line61N11.addStops(List.of(
        new Stop(LocalTime.of(10,7), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(10,8), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(10,9), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(10,13), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(10,20), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(10,21), station7, "Rathaus"),
        new Stop(LocalTime.of(10,24), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(10,28), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(10,30), station10, "Schanz Nord"),
        new Stop(LocalTime.of(10,36), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(10,42), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(10,47), station15, "Wimpfener Weg")
    ));

    Line line61N12 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus0);
    line61N12.addStops(List.of(
        new Stop(LocalTime.of(10,16), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(10,17), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(10,22), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(10,23), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(10,24), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(10,28), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(10,35), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(10,36), station7, "Rathaus"),
        new Stop(LocalTime.of(10,39), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(10,43), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(10,47), station10, "Schanz Nord"),
        new Stop(LocalTime.of(10,52), station11, "Frankenbach Maihalde")
    ));

    Line line61N13 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus1);
    line61N13.addStops(List.of(
        new Stop(LocalTime.of(10,37), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(10,38), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(10,39), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(10,42), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(10,49), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(10,50), station7, "Rathaus"),
        new Stop(LocalTime.of(10,54), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(10,58), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(11,0), station10, "Schanz Nord"),
        new Stop(LocalTime.of(11,6), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(11,12), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(11,17), station15, "Wimpfener Weg")
    ));

    Line line61N14 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus2);
    line61N14.addStops(List.of(
        new Stop(LocalTime.of(11,7), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(11,8), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(11,9), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(11,13), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(11,20), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(11,21), station7, "Rathaus"),
        new Stop(LocalTime.of(11,24), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(11,28), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(11,30), station10, "Schanz Nord"),
        new Stop(LocalTime.of(11,36), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(11,42), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(11,47), station15, "Wimpfener Weg")
    ));

    Line line61N15 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus3);
    line61N15.addStops(List.of(
        new Stop(LocalTime.of(11,31), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(11,32), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(11,37), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(11,38), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(11,39), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(11,43), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(11,50), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(11,51), station7, "Rathaus"),
        new Stop(LocalTime.of(11,54), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(11,58), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(12,0), station10, "Schanz Nord"),
        new Stop(LocalTime.of(12,6), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(12,12), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(12,17), station15, "Wimpfener Weg")
    ));

    Line line61N16 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus0);
    line61N16.addStops(List.of(
        new Stop(LocalTime.of(12,1), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(12,2), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(12,7), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(12,8), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(12,9), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(12,13), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(12,20), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(12,21), station7, "Rathaus"),
        new Stop(LocalTime.of(12,24), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(12,28), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(12,30), station10, "Schanz Nord"),
        new Stop(LocalTime.of(12,36), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(12,42), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(12,47), station15, "Wimpfener Weg")
    ));

    Line line61N17 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus1);
    line61N17.addStops(List.of(
        new Stop(LocalTime.of(12,31), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(12,32), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(12,37), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(12,38), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(12,39), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(12,43), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(12,50), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(12,51), station7, "Rathaus"),
        new Stop(LocalTime.of(12,54), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(12,58), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(13,0), station10, "Schanz Nord"),
        new Stop(LocalTime.of(13,6), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(13,12), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(13,17), station15, "Wimpfener Weg")
    ));

    Line line61N18 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus2);
    line61N18.addStops(List.of(
        new Stop(LocalTime.of(13,1), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(13,2), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(13,7), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(13,8), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(13,9), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(13,13), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(13,20), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(13,21), station7, "Rathaus"),
        new Stop(LocalTime.of(13,24), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(13,28), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(13,30), station10, "Schanz Nord"),
        new Stop(LocalTime.of(13,36), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(13,42), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(13,47), station15, "Wimpfener Weg")
    ));

    Line line61N19 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus3);
    line61N19.addStops(List.of(
        new Stop(LocalTime.of(13,31), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(13,32), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(13,37), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(13,38), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(13,39), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(13,43), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(13,50), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(13,51), station7, "Rathaus"),
        new Stop(LocalTime.of(13,54), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(13,58), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(14,0), station10, "Schanz Nord"),
        new Stop(LocalTime.of(14,6), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(14,12), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(14,17), station15, "Wimpfener Weg")
    ));

    Line line61N20 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus0);
    line61N20.addStops(List.of(
        new Stop(LocalTime.of(13,46), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(13,47), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(13,52), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(13,53), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(13,54), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(13,58), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(14,5), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(14,6), station7, "Rathaus"),
        new Stop(LocalTime.of(14,9), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(14,13), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(14,17), station10, "Schanz Nord"),
        new Stop(LocalTime.of(14,22), station11, "Frankenbach Maihalde")
    ));

    Line line61N21 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus1);
    line61N21.addStops(List.of(
        new Stop(LocalTime.of(14,1), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(14,2), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(14,7), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(14,8), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(14,9), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(14,13), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(14,20), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(14,21), station7, "Rathaus"),
        new Stop(LocalTime.of(14,24), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(14,28), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(14,32), station10, "Schanz Nord"),
        new Stop(LocalTime.of(14,37), station11, "Frankenbach Maihalde")
    ));

    Line line61N22 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus2);
    line61N22.addStops(List.of(
        new Stop(LocalTime.of(14,31), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(14,32), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(14,37), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(14,38), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(14,39), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(14,43), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(14,50), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(14,51), station7, "Rathaus"),
        new Stop(LocalTime.of(14,54), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(14,58), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(15,0), station10, "Schanz Nord"),
        new Stop(LocalTime.of(15,6), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(15,12), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(15,17), station15, "Wimpfener Weg")
    ));

    Line line61N23 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus3);
    line61N23.addStops(List.of(
        new Stop(LocalTime.of(15,1), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(15,2), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(15,7), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(15,8), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(15,9), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(15,13), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(15,20), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(15,21), station7, "Rathaus"),
        new Stop(LocalTime.of(15,24), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(15,28), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(15,30), station10, "Schanz Nord"),
        new Stop(LocalTime.of(15,36), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(15,42), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(15,47), station15, "Wimpfener Weg")
    ));

    Line line61N24 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus0);
    line61N24.addStops(List.of(
        new Stop(LocalTime.of(15,31), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(15,32), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(15,37), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(15,38), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(15,39), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(15,43), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(15,50), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(15,51), station7, "Rathaus"),
        new Stop(LocalTime.of(15,54), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(15,58), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(16,0), station10, "Schanz Nord"),
        new Stop(LocalTime.of(16,6), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(16,12), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(16,17), station15, "Wimpfener Weg")
    ));

    Line line61N25 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus1);
    line61N25.addStops(List.of(
        new Stop(LocalTime.of(16,1), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(16,2), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(16,7), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(16,8), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(16,9), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(16,13), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(16,20), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(16,21), station7, "Rathaus"),
        new Stop(LocalTime.of(16,24), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(16,28), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(16,30), station10, "Schanz Nord"),
        new Stop(LocalTime.of(16,36), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(16,42), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(16,47), station15, "Wimpfener Weg")
    ));

    Line line61N26 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus2);
    line61N26.addStops(List.of(
        new Stop(LocalTime.of(16,31), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(16,32), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(16,37), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(16,38), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(16,39), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(16,43), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(16,50), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(16,51), station7, "Rathaus"),
        new Stop(LocalTime.of(16,54), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(16,58), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(17,0), station10, "Schanz Nord"),
        new Stop(LocalTime.of(17,6), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(17,12), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(17,17), station15, "Wimpfener Weg")
    ));

    Line line61N27 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus3);
    line61N27.addStops(List.of(
        new Stop(LocalTime.of(17,1), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(17,2), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(17,7), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(17,8), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(17,9), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(17,13), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(17,20), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(17,21), station7, "Rathaus"),
        new Stop(LocalTime.of(17,24), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(17,28), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(17,30), station10, "Schanz Nord"),
        new Stop(LocalTime.of(17,36), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(17,42), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(17,47), station15, "Wimpfener Weg")
    ));

    Line line61N28 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus0);
    line61N28.addStops(List.of(
        new Stop(LocalTime.of(17,31), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(17,32), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(17,37), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(17,38), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(17,39), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(17,43), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(17,50), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(17,51), station7, "Rathaus"),
        new Stop(LocalTime.of(17,54), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(17,58), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(18,0), station10, "Schanz Nord"),
        new Stop(LocalTime.of(18,6), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(18,12), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(18,17), station15, "Wimpfener Weg")
    ));

    Line line61N29 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus1);
    line61N29.addStops(List.of(
        new Stop(LocalTime.of(18,1), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(18,2), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(18,7), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(18,8), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(18,9), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(18,13), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(18,20), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(18,21), station7, "Rathaus"),
        new Stop(LocalTime.of(18,24), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(18,28), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(18,30), station10, "Schanz Nord"),
        new Stop(LocalTime.of(18,36), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(18,42), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(18,47), station15, "Wimpfener Weg")
    ));

    Line line61N30 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus2);
    line61N30.addStops(List.of(
        new Stop(LocalTime.of(18,16), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(18,17), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(18,22), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(18,23), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(18,24), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(18,28), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(18,35), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(18,36), station7, "Rathaus"),
        new Stop(LocalTime.of(18,39), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(18,43), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(18,47), station10, "Schanz Nord"),
        new Stop(LocalTime.of(18,52), station11, "Frankenbach Maihalde")
    ));

    Line line61N31 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus3);
    line61N31.addStops(List.of(
        new Stop(LocalTime.of(18,31), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(18,32), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(18,37), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(18,38), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(18,39), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(18,43), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(18,50), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(18,51), station7, "Rathaus"),
        new Stop(LocalTime.of(18,54), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(18,58), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(19,0), station10, "Schanz Nord"),
        new Stop(LocalTime.of(19,6), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(19,12), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(19,17), station15, "Wimpfener Weg")
    ));

    Line line61N32 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus0);
    line61N32.addStops(List.of(
        new Stop(LocalTime.of(18,46), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(18,47), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(18,52), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(18,53), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(18,54), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(18,58), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(19,5), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(19,6), station7, "Rathaus"),
        new Stop(LocalTime.of(19,9), station8, "Hauptbahnhof")
    ));

    Line line61N33 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus1);
    line61N33.addStops(List.of(
        new Stop(LocalTime.of(18,56), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(18,57), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(19,2), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(19,3), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(19,4), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(19,8), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(19,15), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(19,16), station7, "Rathaus"),
        new Stop(LocalTime.of(19,19), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(19,23), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(19,26), station10, "Schanz Nord"),
        new Stop(LocalTime.of(19,32), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(19,37), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(19,42), station15, "Wimpfener Weg")
    ));

    Line line61N34 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus2);
    line61N34.addStops(List.of(
        new Stop(LocalTime.of(19,56), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(19,57), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(20,2), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(20,3), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(20,4), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(20,8), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(20,15), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(20,16), station7, "Rathaus"),
        new Stop(LocalTime.of(20,19), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(20,23), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(20,26), station10, "Schanz Nord"),
        new Stop(LocalTime.of(20,32), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(20,37), station14, "Kirchausen Schloßstraße"),
        new Stop(LocalTime.of(20,42), station15, "Wimpfener Weg")
    ));

    Line line61N35 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus3);
    line61N35.addStops(List.of(
        new Stop(LocalTime.of(20,56), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(20,57), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(21,2), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(21,3), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(21,4), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(21,8), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(21,15), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(21,16), station7, "Rathaus"),
        new Stop(LocalTime.of(21,19), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(21,23), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(21,26), station10, "Schanz Nord"),
        new Stop(LocalTime.of(21,32), station11, "Frankenbach Maihalde")
    ));

    Line line61N36 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus0);
    line61N36.addStops(List.of(
        new Stop(LocalTime.of(21,56), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(21,57), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(22,2), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(22,3), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(22,4), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(22,8), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(22,15), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(22,16), station7, "Rathaus"),
        new Stop(LocalTime.of(22,19), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(22,23), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(22,26), station10, "Schanz Nord"),
        new Stop(LocalTime.of(22,32), station11, "Frankenbach Maihalde")
    ));

    Line line61N37 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus1);
    line61N37.addStops(List.of(
        new Stop(LocalTime.of(22,26), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(22,27), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(22,32), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(22,33), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(22,34), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(22,38), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(22,45), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(22,46), station7, "Rathaus"),
        new Stop(LocalTime.of(22,49), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(22,53), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(22,56), station10, "Schanz Nord"),
        new Stop(LocalTime.of(23,2), station11, "Frankenbach Maihalde")
    ));

    Line line61N38 = new Line("Linie 61 Nord", EnumSet
        .of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY), bus2);
    line61N38.addStops(List.of(
        new Stop(LocalTime.of(22,56), station0, "Flein Gänsäcker"),
        new Stop(LocalTime.of(22,57), station1, "Flein Rathaus"),
        new Stop(LocalTime.of(23,2), station2, "Max-von-Laue-Straße"),
        new Stop(LocalTime.of(23,3), station3, "Sontheim Jörg-Ratgeb-Platz"),
        new Stop(LocalTime.of(23,4), station4, "Sontheim Hochschule"),
        new Stop(LocalTime.of(23,8), station5, "Südbahnhof Süd"),
        new Stop(LocalTime.of(23,15), station6, "Allee Post Ost"),
        new Stop(LocalTime.of(23,16), station7, "Rathaus"),
        new Stop(LocalTime.of(23,19), station8, "Hauptbahnhof"),
        new Stop(LocalTime.of(23,23), station9, "Wilhelm-Leuschner-Straße Nord"),
        new Stop(LocalTime.of(23,26), station10, "Schanz Nord"),
        new Stop(LocalTime.of(23,32), station11, "Frankenbach Maihalde B39"),
        new Stop(LocalTime.of(23,37), station14, "Kirchausen Schloßstraße")
    ));

    try{
      demoService.addLines(List.of(
          line61N0, line61N1, line61N2, line61N3, line61N4, line61N5, line61N6, line61N7,
          line61N8, line61N9, line61N10, line61N11, line61N12, line61N13, line61N14, line61N15,
          line61N16, line61N17, line61N18, line61N19, line61N20, line61N21, line61N22, line61N23,
          line61N24, line61N25, line61N26, line61N27, line61N28, line61N29, line61N30, line61N31,
          line61N32, line61N33, line61N34, line61N35, line61N36, line61N37, line61N38
      ));
    } catch (Exception e){
      e.printStackTrace();
    }

  }

  /**
   * @return The currently selected time in the gui
   */
  @Override
  public LocalDateTime getControllerCurrentTime() {
    return datePickerDatum.getValue()
        .atTime(spinnerHour.getValue(), spinnerMinute.getValue());
  }

  /**
   * This method is executed when a minute in the simulation passes
   */
  private void onRealtimeMinutePassed() {
    enableUserTimeInput = false;
    LocalDateTime dateTime = getControllerCurrentTime().plusMinutes(1);
    spinnerHour.getValueFactory().setValue(dateTime.getHour());
    spinnerMinute.getValueFactory().setValue(dateTime.getMinute());
    datePickerDatum.setValue(dateTime.toLocalDate());

    updateDepartures();
    enableUserTimeInput = true;
  }

  /**
   * This method is executed when the minutes spinner is changed
   */
  private void onUpdateSpinnerMinutes(ObservableValue<? extends Integer> obs, Integer oldValue,
      Integer newValue) {
    if (!enableUserTimeInput) {
      return;
    }
    try {
      updateDepartures();
    } catch (Exception e) {
      logger.error("Can't update minutes", e);
    }
  }

  /**
   * This method is executed when the hour spinner is changed
   */
  private void onUpdateSpinnerHours(ObservableValue<? extends Integer> obs, Integer oldValue,
      Integer newValue) {
    if (!enableUserTimeInput) {
      return;
    }
    try {
      updateDepartures();
    } catch (Exception e) {
      logger.error("Can't update hours", e);
    }
  }

  /**
   * This method is executed when the date picker is changed
   */
  private void onUpdateDatePicker(ObservableValue<? extends LocalDate> obs, LocalDate oldValue,
      LocalDate newValue) {
    if (!enableUserTimeInput) {
      return;
    }
    try {
      updateDepartures();
    } catch (Exception e) {
      logger.error("Can't update date", e);
    }
  }

  /**
   * This method is executed when the simulation checkbox is checked or unchecked
   */
  private void onCheckBoxChanged(ObservableValue<? extends Boolean> observable,
      Boolean oldValue,
      Boolean newValue) {

    if (newValue) {
      realtimeUpdateSchedulerExecution = realtimeUpdateScheduler
          .scheduleAtFixedRate(() -> onRealtimeMinutePassed(), 1, 1, TimeUnit.SECONDS);
    } else {
      realtimeUpdateSchedulerExecution.cancel(true);
    }
  }

  /**
   * This method is executed when a station is selected.
   */
  private void onStationSelect(ObservableValue<? extends Number> observableValue, Number number,
      Number number2) {
    try {
      setObservedStation(choiceBoxStation.getItems().get((Integer) number2).stationId);
    } catch (StationNotFoundException e) {
      e.printStackTrace();
    }
  }

  /**
   * Since we unfortunately do not use tools like Guava, we need this helper class to create
   * ScheduledExecutorService, which creates daemon threads.
   */
  private static class DaemonThreadFactory implements ThreadFactory {

    /**
     * Constructs a new {@code Thread}.  Implementations may also initialize
     * priority, name, daemon status, {@code ThreadGroup}, etc.
     *
     * @param r a runnable to be executed by new thread instance
     * @return constructed thread, or {@code null} if the request to
     *         create a thread is rejected
     */
    public Thread newThread(Runnable r) {
      Thread thread = new Thread(r);
      thread.setDaemon(true);
      return thread;
    }
  }

  /**
   * This class is displayed in the TableView. It contains links to the actual stations.
   */
  private class StationSelect {

    int stationId;
    String stationName;

    @Override
    public String toString() {
      return stationName;
    }
  }
}


