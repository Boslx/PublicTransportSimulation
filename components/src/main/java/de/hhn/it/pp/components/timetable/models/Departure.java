package de.hhn.it.pp.components.timetable.models;

import de.hhn.it.pp.components.timetable.events.ControllerCurrentTimeDelegate;
import de.hhn.it.pp.components.timetable.events.DelayListener;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class Departure {

  private final List<DelayListener> delayListeners = new ArrayList<>();
  /**
   * Line of the departure.
   */
  private final Line line;
  /**
   * The concrete stop, from which you want to depart.
   */
  private final Stop focusedStop;
  /**
   * The time of departure. Please note that delays must be added to this.
   */
  private final LocalDateTime concreteTime;

  private final ControllerCurrentTimeDelegate controllerCurrentTimeDelegate;

  /**
   * Constructor.
   *
   * @param line         Line of the departure
   * @param focusedStop  The concrete stop, from which you want to depart
   * @param concreteTime The time of departure. Please note that delays must be added to this.
   */
  public Departure(Line line, Stop focusedStop, LocalDateTime concreteTime,
      ControllerCurrentTimeDelegate controllerCurrentTimeDelegate) {
    this.line = line;
    this.focusedStop = focusedStop;
    this.concreteTime = concreteTime;
    this.controllerCurrentTimeDelegate = controllerCurrentTimeDelegate;
  }

  /**
   * Getter for the line of the departure.
   *
   * @return line object
   */
  public Line getLine() {
    return line;
  }

  /**
   * Getter for the line type.
   *
   * @return returns type of the transportation vehicle of the line
   */
  public String getLineType() {
    return line.getServingTransportationVehicle().getType().name();
  }

  /**
   * Getter for the line name.
   *
   * @return name of the line
   */
  public String getLineName() {
    return line.getName();
  }

  /**
   * Getter for the last stop name.
   *
   * @return name of the last stop
   */
  public String getLastStopStationName() {
    return line.getStops().get(line.getStops().size() - 1).getName();
  }

  /**
   * Getter for the arrival message.
   *
   * @return string of the arrive message
   */
  public String getArrivalMessage() {
    if (controllerCurrentTimeDelegate != null) {
      LocalDateTime currentTime = controllerCurrentTimeDelegate.getControllerCurrentTime();

      long plannedArrivalIn = Duration.between(currentTime, concreteTime).toMinutes();
      long delay = getDelay();

      // Set to 0 if negativ
      if (plannedArrivalIn < 0) {
        delay = delay + plannedArrivalIn;
        plannedArrivalIn = 0;
      }

      if (plannedArrivalIn / 60 == 0) {
        return delay > 0 ? plannedArrivalIn + " + " + delay + " min" : plannedArrivalIn + " min";
      } else {
        long daysDifference = ChronoUnit.DAYS
            .between(currentTime.toLocalDate(), concreteTime.toLocalDate());
        if (daysDifference == 0) {
          return delay > 0 ? String.format("%02d", concreteTime.getHour()) + ":" + String
              .format("%02d", concreteTime.getMinute()) + " + " + delay + " min"
              : String.format("%02d", concreteTime.getHour()) + ":" + String
                  .format("%02d", concreteTime.getMinute());
        } else {
          return delay > 0 ? "+" + daysDifference + " Tag(e) " + String
              .format("%02d", concreteTime.getHour()) + ":" + String
              .format("%02d", concreteTime.getMinute()) + " + " + delay + " min"
              : "+" + daysDifference + " Tag(e) " + String.format("%02d", concreteTime.getHour())
                  + ":" + String
                  .format("%02d", concreteTime.getMinute());
        }
      }
    }
    return focusedStop.getTime().toString();
  }

  /**
   * Getter for the focused stop.
   *
   * @return focused stop
   */
  public Stop getFocusedStop() {
    return focusedStop;
  }

  /**
   * Getter for the concrete time.
   *
   * @return returns concrete time
   */
  public LocalDateTime getConcreteTime() {
    return concreteTime;
  }

  /**
   * Getter for the Delay.
   *
   * @return returns the delay of the line
   */
  public int getDelay() {
    return line.getServingTransportationVehicle().getDelay();
  }
}
