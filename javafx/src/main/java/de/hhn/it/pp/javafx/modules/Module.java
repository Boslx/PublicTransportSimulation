package de.hhn.it.pp.javafx.modules;


import de.hhn.it.pp.javafx.controllers.Controller;

public class Module {

  private String name;
  private Controller controller;

  public Module(String name, Controller controller) {
    this.name = name;
    this.controller = controller;
  }
}
