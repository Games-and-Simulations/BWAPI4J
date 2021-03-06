package bwem.area;

import bwem.area.typedef.AreaId;
import bwem.tile.MiniTile;
import bwem.tile.MiniTileImpl;
import bwem.typedef.Altitude;
import org.openbw.bwapi4j.WalkPosition;

// Helper class for void Map::ComputeAreas()
// Maintains some information about an area being computed
// A TempAreaInfo is not Valid() in two cases:
//   - a default-constructed TempAreaInfo instance is never Valid (used as a dummy value to simplify the algorithm).
//   - any other instance becomes invalid when absorbed (see Merge)
/**
 * Helper class for void Map::ComputeAreas()
 */
public class TempAreaInfo {

    private boolean isValid;
    private final AreaId id;
    private final WalkPosition walkPositionWithHighestAltitude;
    private final Altitude highestAltitude;
    private int size;

    public TempAreaInfo() {
        this.isValid = false;
        this.id = new AreaId(0);
        this.walkPositionWithHighestAltitude = new WalkPosition(0, 0);
        this.highestAltitude = new Altitude(0);

//        bwem_assert(!valid());
        if (isValid()) {
            throw new IllegalStateException();
        }
    }

    public TempAreaInfo(final boolean isValid, final AreaId areaId, final WalkPosition walkPositionWithHighestAltitude, final Altitude highestAltitude, final int size) {
        this.isValid = isValid;
        this.id = new AreaId(areaId);
        this.walkPositionWithHighestAltitude = walkPositionWithHighestAltitude;
        this.highestAltitude = new Altitude(highestAltitude);
        this.size = size;
    }

    public TempAreaInfo(final AreaId id, final MiniTile miniTile, final WalkPosition walkPositionWithHighestAltitude) {
        this.isValid = true;
        this.id = id;
        this.walkPositionWithHighestAltitude = walkPositionWithHighestAltitude;
        this.size = 0;
        this.highestAltitude = miniTile.getAltitude();

        add(miniTile);

//        { bwem_assert(valid()); }
        if (!isValid()) {
            throw new IllegalStateException();
        }
    }

    public boolean isValid() {
        return this.isValid;
    }

	public AreaId getId() {
//        bwem_assert(valid());
        if (!isValid()) {
            throw new IllegalStateException();
        }
        return this.id;
    }

    public WalkPosition getWalkPositionWithHighestAltitude() {
//        { bwem_assert(valid());
        if (!isValid()) {
            throw new IllegalStateException();
        }
        return this.walkPositionWithHighestAltitude;
    }

    public int getSize() {
//        bwem_assert(valid());
        if (!isValid()) {
            throw new IllegalStateException();
        }
        return this.size;
    }

	public Altitude getHighestAltitude() {
//        bwem_assert(valid());
        if (!isValid()) {
            throw new IllegalStateException();
        }
        return this.highestAltitude;
    }

    public void add(final MiniTile miniTile) {
//        bwem_assert(valid());
        if (!isValid()) {
            throw new IllegalStateException();
        }
        ++this.size;
        ((MiniTileImpl) miniTile).setAreaId(id);
    }

	// Left to caller : m.SetAreaId(this->id()) for each MiniTile m in absorbed
	public void merge(final TempAreaInfo absorbed) {
        if (!(isValid() && absorbed.isValid())) {
//            bwem_assert(valid() && absorbed.isValid());
            throw new IllegalStateException();
        } else if (!(this.size >= absorbed.size)) {
//            bwem_assert (size >= absorbed.size);
            throw new IllegalStateException();
        }
        this.size += absorbed.size;
        absorbed.isValid = false;
    }

}
