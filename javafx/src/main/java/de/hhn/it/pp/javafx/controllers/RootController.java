package de.hhn.it.pp.javafx.controllers;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.AnchorPane;

public class RootController extends Controller implements Initializable {
  private static final org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger(RootController.class);

  @FXML
  private MenuBar menuBar;

  @FXML
  private ListView<String> listView;

  @FXML
  private AnchorPane modulePane;


  private Map<String, Node> moduleMap;

  private Node firstModule = null;


  public RootController() {
    logger.debug("RootController created.");
    moduleMap = new HashMap<>();
  }

  /**
   * Called to initialize a controller after its root element has been
   * completely processed.
   *
   * @param location  The location used to resolve relative paths for the root object, or
   *                  null if the location is not known.
   * @param resources The resources used to localize the root object, or null if
   */
  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    listView.getSelectionModel().selectedItemProperty().addListener(
            (ObservableValue<? extends String> observable,
             String oldValue, String newValue) -> {
              logger.info("Selected item: " + newValue);
              modulePane.getChildren().clear();
              modulePane.getChildren().add(moduleMap.get(newValue));
            }
    );

  }

  public void addModule(String name, Node content) {
    listView.getItems().add(name);
    moduleMap.put(name, content);
    if (firstModule == null) {
      firstModule = content;
      listView.getSelectionModel().selectFirst();
    }
  }


}
