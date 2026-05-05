package movement;

import core.Coord;
import core.Settings;

public class GridMovement extends MovementModel {

  private static final int PATH_LENGTH = 1;
  private static final String MOVEMENT_MODEL_NS = "GridMovement";
  private static final String HOME_X_SETTING = "homeX";
  private static final String HOME_Y_SETTING = "homeY";
  private static final String GRID_COUNT_X = "gridCountX";
  private static final String GRID_COUNT_Y = "gridCountY";

  private Coord lastWaypoint;

  private int homeX, homeY;
  private int gridCountX, gridCountY;

  public GridMovement(Settings settings) {
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

  }

  protected GridMovement(GridMovement rwpG) {
    super(rwpG);
    this.homeX = rwpG.homeX;
    this.homeY = rwpG.homeY;
    this.gridCountX = rwpG.gridCountX;
    this.gridCountY = rwpG.gridCountY;
  }

  @Override
  public Coord getInitialLocation() {
    assert rng != null : "MovementModel not initialized!";
    Coord c = randomCoord();

    this.lastWaypoint = c;
    return c;
  }

  @Override
  public Path getPath() {
    Path p;
    p = new Path(generateSpeed());
    p.addWaypoint(lastWaypoint.clone());
    Coord c = lastWaypoint;

    for (int i = 0; i < PATH_LENGTH; i++) {
      c = randomCoord();
      p.addWaypoint(c);
    }

    this.lastWaypoint = c;
    return p;
  }

  @Override
  public GridMovement replicate() {
    return new GridMovement(this);
  }

  protected Coord randomCoord() {
    return new Coord(
        (rng.nextDouble() * getCoordX()) + (homeX * getCoordX()),
        (rng.nextDouble() * getCoordY()) + (homeY * getCoordY()));
  }

  private double getCoordX() {
    return getMaxX() / (double) gridCountX;
  }

  private double getCoordY() {
    return getMaxY() / (double) gridCountY;
  }

}
