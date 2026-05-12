package movement;

import core.Coord;
import core.Settings;

public class RandomGridMovement extends MovementModel {
  private static final String MOVEMENT_MODEL_NS = "RandomGridMovement";
  private static final String HOME_X_SETTING = "homeX";
  private static final String HOME_Y_SETTING = "homeY";
  private static final String GRID_COUNT_X = "gridCountX";
  private static final String GRID_COUNT_Y = "gridCountY";

  private Coord lastWaypoint;
  private int currentGridX, currentGridY;

  private int homeX, homeY;
  private int gridCountX, gridCountY;
  private int gatheringX, gatheringY;

  public RandomGridMovement(Settings settings) {
    super(settings);

    Settings globalSettings = new Settings(MOVEMENT_MODEL_NS);

    if (globalSettings.contains(GRID_COUNT_X)) {
      gridCountX = globalSettings.getInt(GRID_COUNT_X);
    } else {
      gridCountX = 1;
    }

    if (globalSettings.contains(GRID_COUNT_Y)) {
      gridCountY = globalSettings.getInt(GRID_COUNT_Y);
    } else {
      gridCountY = 1;
    }

    if (settings.contains(HOME_X_SETTING)) {
      homeX = settings.getInt(HOME_X_SETTING);
    } else {
      homeX = rng.nextInt(gridCountX);
    }

    if (settings.contains(HOME_Y_SETTING)) {
      homeY = settings.getInt(HOME_Y_SETTING);
    } else {
      homeY = rng.nextInt(gridCountY);
    }

  }

  protected RandomGridMovement(RandomGridMovement grp) {
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
    getNextGrid();

    Coord c = randomCoordInGrid(currentGridX, currentGridY);

    this.lastWaypoint = c;

    return p;
  }

  @Override
  public RandomGridMovement replicate() {
    return new RandomGridMovement(this);
  }

  private Coord randomCoordInGrid(int gridX, int gridY) {
    double x = (rng.nextDouble() * getCoordX()) + (gridX * getCoordX());
    double y = (rng.nextDouble() * getCoordY()) + (gridY * getCoordY());
    return new Coord(x, y);
  }

  private double getCoordX() {
    return getMaxX() / (double) gridCountX;
  }

  private double getCoordY() {
    return getMaxY() / (double) gridCountY;
  }

  private void getNextGrid() {
    int rX, rY;
    do {
      rX = rng.nextInt(gridCountX);
      rY = rng.nextInt(gridCountY);
    } while (rX == currentGridX && rY == currentGridY);

    currentGridX = rX;
    currentGridY = rY;
  }

}
