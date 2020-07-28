package de.hhn.it.pp.javafx.controllers.timetable;

import de.hhn.it.pp.components.timetable.models.TransportationVehicle;
import de.hhn.it.pp.components.timetable.providers.SimpleTimetableService;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class TimetableTransportationVehicleDelayManagerController implements Initializable {

  private final SimpleTimetableService demoService;
  @FXML
  private TableView<TransportationVehicleSelect> tableViewTransportationVehicle;
  @FXML
  private TableColumn<TransportationVehicleSelect, String> columnTransportationVehicleName;
  @FXML
  private Spinner<Integer> spinnerDelay;
  @FXML
  private CheckBox checkBoxIsDysfunktional;

  private TransportationVehicle focusedTransportationVehicle;

  public TimetableTransportationVehicleDelayManagerController(
      SimpleTimetableService demoService) {
    this.demoService = demoService;
  }

  /**
   * Called to initialize a controller after its root element has been completely processed.
   *
   * @param location  The location used to resolve relative paths for the root object, or {@code
   *                  null} if the location is not known.
   * @param resources The resources used to localize the root object, or {@code null} if
   */
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    SpinnerValueFactory<Integer> delayMinutesValueFactory =
        new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 720, 0);
    spinnerDelay.setValueFactory(delayMinutesValueFactory);

    columnTransportationVehicleName
        .setCellValueFactory(new PropertyValueFactory<TransportationVehicleSelect, String>("name"));

    populateTableView();

    tableViewTransportationVehicle.setOnMouseClicked(this::transportationVehicleChange);
  }

  private void setFocusedTransportationVehicle(TransportationVehicle toFocus) {
    focusedTransportationVehicle = toFocus;

    if (focusedTransportationVehicle != null) {
      spinnerDelay.getValueFactory().setValue(focusedTransportationVehicle.getDelay());
      checkBoxIsDysfunktional.setSelected(!focusedTransportationVehicle.isFunctional());
    }
  }

  private void populateTableView() {
    ArrayList<TransportationVehicleSelect> transportationVehicles = new ArrayList<>();
    for (int i = 0; i < demoService.getTransportationVehicleCount(); i++) {
      TransportationVehicle vehicle = demoService.getTransportationVehicle(i);
      String name = vehicle.getType().name() + " " + i;
      transportationVehicles.add(new TransportationVehicleSelect(name, vehicle));
    }

    tableViewTransportationVehicle
        .setItems(FXCollections.observableArrayList(transportationVehicles));

    focusedTransportationVehicle=transportationVehicles.get(0).getVehicle();
  }

  private void transportationVehicleChange(MouseEvent e) {
    if (e.getButton() == MouseButton.PRIMARY) {
      saveDelayToTransportationVehicle();

      setFocusedTransportationVehicle(tableViewTransportationVehicle.getSelectionModel()
          .getSelectedItem().getVehicle());
    }
  }

  void saveDelayToTransportationVehicle() {
    focusedTransportationVehicle.setDelay(spinnerDelay.getValue());
    focusedTransportationVehicle.setFunctional(!checkBoxIsDysfunktional.isSelected());
  }

  public class TransportationVehicleSelect {

    private final String name;
    private final TransportationVehicle vehicle;

    public TransportationVehicleSelect(String name,
        TransportationVehicle vehicle) {
      this.name = name;
      this.vehicle = vehicle;
    }

    public String getName() {
      return name;
    }

    public TransportationVehicle getVehicle() {
      return vehicle;
    }
  }
}
