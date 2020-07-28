package de.hhn.it.pp.javafx;

import de.hhn.it.pp.javafx.controllers.RootController;
import de.hhn.it.pp.javafx.modules.Module;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Main extends Application {

  private static final org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger(Main.class);
  private final int width = 1280;

  private final int height = 720;
  private RootController controller;
  private Map<String, Module> modules = new HashMap<>();

  /**
   * the main method.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {

    System.out.println("java version: " + System.getProperty("java.version"));
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Root2.fxml"));

    final Parent root = loader.load();
    controller = loader.getController();

    primaryStage.setTitle("JavaFX UI");
    Scene scene = new Scene(root, width, height);
    primaryStage.setMinWidth(width);
    primaryStage.setMinHeight(height);
    primaryStage.setScene(scene);
    primaryStage.show();

    addModule("Template");
    addModule("CoffeeMakerService");
    addModule("TimetableService");
  }

  @Override
  public void stop() {
    logger.info("stop: Shutting down");
  }

  private void addModule(String name) {
    try {
      URL url = getClass().getResource("/fxml/" + name + ".fxml");
      if (url == null) {
        logger.error("addModule: Fxml File for Module: \"" + name + "\" not found!");
      } else {
        logger.info("addModule: Loading Module: \"" + name + "\"!");
        Node content = FXMLLoader.load(url);
        controller.addModule(name, content);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
