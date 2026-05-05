package movement;

import core.Coord;
import core.Settings;

public class GridProphetMovement extends MovementModel {
  private static final String HOME_X_SETTING = "homeX";
  private static final String HOME_Y_SETTING = "homeY";
  private static final String GRID_COUNT_X = "gridCountX";
  private static final String GRID_COUNT_Y = "gridCountY";
  private static final String GATHERING_X = "gatheringX";
  private static final String GATHERING_Y = "gatheringY";

  private Coord lastWaypoint;
  private int currentGridX, currentGridY;

  private int homeX, homeY;
  private int gridCountX, gridCountY;
  private int gatheringX, gatheringY;

  public GridProphetMovement(Settings settings) {
    super(settings);

    if (settings.contains(HOME_X_SETTING)) {
      homeX = settings.getInt(HOME_X_SETTING);
    } else {
      homeX = 0;
    }

    if (settings.contains(HOME_Y_SETTING)) {
      homeY = settings.getInt(HOME_Y_SETTING);
    } else {
      homeY = 0;
    }

    if (settings.contains(GRID_COUNT_X)) {
      gridCountX = settings.getInt(GRID_COUNT_X);
    } else {
      gridCountX = 1;
    }

    if (settings.contains(GRID_COUNT_Y)) {
      gridCountY = settings.getInt(GRID_COUNT_Y);
    } else {
      gridCountY = 1;
    }

    if (settings.contains(GATHERING_X)) {
      gatheringX = settings.getInt(GATHERING_X);
    } else {
      gatheringX = 0;
    }

    if (settings.contains(GATHERING_Y)) {
      gatheringY = settings.getInt(GATHERING_Y);
    } else {
      gatheringY = 0;
    }
  }

  protected GridProphetMovement(GridProphetMovement grp) {
    super(grp);
    this.homeX = grp.homeX;
    this.homeY = grp.homeY;
    this.gridCountX = grp.gridCountX;
    this.gridCountY = grp.gridCountY;
    this.gatheringX = grp.gatheringX;
    this.gatheringY = grp.gatheringY;
  }

  @Override
  public Coord getInitialLocation() {
    assert rng != null : "MovementModel not initialized!";
    currentGridX = homeX;
    currentGridY = homeY;

    Coord c = randomCoordInGrid(currentGridX, currentGridY);

    this.lastWaypoint = c;
    return c;
  }

  @Override
  public Path getPath() {
    Path p;
    p = new Path(generateSpeed());
    p.addWaypoint(lastWaypoint.clone());
    Coord c = randomCoordInGrid(currentGridX, currentGridY);
    double rollProb = rng.nextDouble();

    if (currentGridX == homeX && currentGridY == homeY) {
      if (rollProb <= 0.8) {
        setNextGridGathering();
      } else {
        setNextGridElsewhere();
      }
    } else {
      if (rollProb <= 0.9) {
        setNextGridHome();
      } else {
        setNextGridElsewhere();
      }
    }

    this.lastWaypoint = c;

    return p;
  }

  @Override
  public GridProphetMovement replicate() {
    return new GridProphetMovement(this);
  }

  protected Coord randomCoordInGrid(int gridX, int gridY) {
    return new Coord(
        (rng.nextDouble() * getCoordX()) + (gridX * getCoordX()),
        (rng.nextDouble() * getCoordY()) + (gridY * getCoordY()));
  }

  private double getCoordX() {
    return getMaxX() / (double) gridCountX;
  }

  private double getCoordY() {
    return getMaxY() / (double) gridCountY;
  }

  private void setNextGridHome() {
    currentGridX = homeX;
    currentGridY = homeY;
  }

  private void setNextGridGathering() {
    currentGridX = gatheringX;
    currentGridY = gatheringY;
  }

  private void setNextGridElsewhere() {
    int rX, rY;
    do {
      rX = rng.nextInt(gridCountX);
      rY = rng.nextInt(gridCountY);
    } while ((rX == homeX && rY == homeY) || (rY == gatheringX && rY == gatheringY));

    currentGridX = rX;
    currentGridY = rY;
  }

}
