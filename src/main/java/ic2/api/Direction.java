package ic2.api;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.EnumSet;
import java.util.Set;

/**
 * Represents the 6 possible directions along the axis of a block.
 */
public enum Direction
{
	/**
	 * -X
	 */
	XN,
	/**
	 * +X
	 */
	XP,

	/**
	 * -Y
	 */
	YN,
	//MC-Code starts with 0 here
	/**
	 * +Y
	 */
	YP,
	// 1...

	/**
	 * -Z
	 */
	ZN,
	/**
	 * +Z
	 */
	ZP;

	Direction()
	{
		int side = this.ordinal() / 2;
		int sign = this.getSign();

		this.xOffset = side == 0 ? sign : 0;
		this.yOffset = side == 1 ? sign : 0;
		this.zOffset = side == 2 ? sign : 0;
	}

	public static Direction fromSideValue(int side)
	{
		return directions[(side + 2) % 6];
	}

	public static Direction fromForgeDirection(ForgeDirection dir)
	{
		if (dir == ForgeDirection.UNKNOWN)
			return null;

		return fromSideValue(dir.ordinal());
	}

	/**
	 * Get the tile entity next to a tile entity following this direction.
	 *
	 * @param te tile entity to check
	 * @return Adjacent tile entity or null if none exists
	 */
	public TileEntity applyToTileEntity(TileEntity te)
	{
		return this.applyTo(te.getWorldObj(), te.xCoord, te.yCoord, te.zCoord);
	}

	/**
	 * Get the tile entity next to a position following this direction.
	 *
	 * @param world World to check
	 * @param x     X coordinate to check from
	 * @param y     Y coordinate to check from
	 * @param z     Z coordinate to check from
	 * @return Adjacent tile entity or null if none exists
	 */
	public TileEntity applyTo(World world, int x, int y, int z)
	{
		int[] coords = { x, y, z };

		coords[this.ordinal() / 2] += this.getSign();

		if (world != null && world.blockExists(coords[0], coords[1], coords[2]))
			try
			{
				return world.getTileEntity(coords[0], coords[1], coords[2]);
			}
			catch (Exception e)
			{
				// TODO gamerforEA add Throwable parameter
				throw new RuntimeException("error getting TileEntity at dim " + world.provider.dimensionId + " " + coords[0] + "/" + coords[1] + "/" + coords[2], e);
			}

		return null;
	}

	/**
	 * Get the inverse of this direction (XN -> XP, XP -> XN, etc.)
	 *
	 * @return Inverse direction
	 */
	public Direction getInverse()
	{
		return directions[this.ordinal() ^ 1];
	}

	/**
	 * Convert this direction to a Minecraft side value.
	 *
	 * @return Minecraft side value
	 */
	public int toSideValue()
	{
		return (this.ordinal() + 4) % 6;
	}

	/**
	 * Determine direction sign (N for negative or P for positive).
	 *
	 * @return -1 if the direction is negative, +1 if the direction is positive
	 */
	private int getSign()
	{
		return this.ordinal() % 2 * 2 - 1;
	}

	public ForgeDirection toForgeDirection()
	{
		return ForgeDirection.getOrientation(this.toSideValue());
	}

	public final int xOffset;
	public final int yOffset;
	public final int zOffset;

	public static final Direction[] directions = Direction.values();
	public static final Set<Direction> noDirections = EnumSet.noneOf(Direction.class);
	public static final Set<Direction> allDirections = EnumSet.allOf(Direction.class);}

