package movement;

import core.Coord;
import core.Settings;

public class RandomWaypointGrid extends MovementModel {
  private static final int PATH_LENGTH = 1;
  private static final String GRID_BOUND = "bound";

  private Coord lastWaypoint;

  private int minX, maxX, minY, maxY;

  public RandomWaypointGrid(Settings settings) {
    super(settings);

    if (settings.contains(GRID_BOUND)) {
      int[] bound = settings.getCsvInts(GRID_BOUND, 4);
      this.minX = bound[0];
      this.maxX = bound[1];
      this.minY = bound[2];
      this.maxY = bound[3];
    } else {
      this.maxX = getMaxX();
      this.maxY = getMaxY();
    }
  }

  protected RandomWaypointGrid(RandomWaypointGrid rwpG) {
    super(rwpG);
    this.minX = rwpG.minX;
    this.maxX = rwpG.maxX;
    this.minY = rwpG.minY;
    this.maxY = rwpG.maxY;
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
  public MovementModel replicate() {
    return new RandomWaypointGrid(this);
  }

  protected Coord randomCoord() {
    return new Coord(minX + rng.nextDouble() * (maxX - minX), minY + rng.nextDouble() * (maxY - minY));
  }
}
