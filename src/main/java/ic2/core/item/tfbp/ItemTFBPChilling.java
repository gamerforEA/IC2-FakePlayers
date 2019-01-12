package ic2.core.item.tfbp;

import com.gamerforea.ic2.EventConfig;
import com.gamerforea.ic2.ITerraformingBPFakePlayer;
import com.gamerforea.ic2.ModUtils;
import ic2.core.block.machine.tileentity.TileEntityTerra;
import ic2.core.init.InternalName;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class ItemTFBPChilling extends ItemTFBP
		implements ITerraformingBPFakePlayer // TODO gamerforEA implement ITerraformingBPFakePlayer
{
	public ItemTFBPChilling(InternalName internalName)
	{
		super(internalName);
	}

	@Override
	public int getConsume()
	{
		return 2000;
	}

	@Override
	public int getRange()
	{
		return 50;
	}

	// TODO gamerforEA code start
	@Override
	public boolean terraform(World world, int x, int z, int yCoord)
	{
		return this.terraform(world, x, z, yCoord, ModUtils.NEXUS_FACTORY.getFake(world));
	}

	public boolean isSurroundedBySnow(World world, int x, int y, int z)
	{
		return this.isSurroundedBySnow(world, x, y, z, ModUtils.NEXUS_FACTORY.getFake(world));
	}

	public boolean isSnowHere(World world, int x, int y, int z)
	{
		return this.isSnowHere(world, x, y, z, ModUtils.NEXUS_FACTORY.getFake(world));
	}
	// TODO gamerforEA code end

	@Override
	public boolean terraform(World world, int x, int z, int yCoord, EntityPlayer player) // TODO gamerforEA add EntityPlayer
	{
		int y = TileEntityTerra.getFirstBlockFrom(world, x, z, yCoord + 10);
		if (y == -1)
			return false;
		else
		{
			Block block = world.getBlock(x, y, z);
			if (block != Blocks.water && block != Blocks.flowing_water)
			{
				if (block == Blocks.ice)
				{
					Block blockBelow = world.getBlock(x, y - 1, z);
					if (blockBelow == Blocks.water || blockBelow == Blocks.flowing_water)
					{
						// TODO gamerforEA code start
						if (EventConfig.terraEvent && ModUtils.cantBreakOrNotInPrivate(player, x, y - 1, z))
							return false;
						// TODO gamerforEA code end

						world.setBlock(x, y - 1, z, Blocks.ice, 0, 7);
						return true;
					}
				}
				else if (block == Blocks.snow_layer && this.isSurroundedBySnow(world, x, y, z, player)) // TODO gamerforEA add EntityPlayer
				{
					// TODO gamerforEA code start
					if (EventConfig.terraEvent && ModUtils.cantBreakOrNotInPrivate(player, x, y, z))
						return false;
					// TODO gamerforEA code end

					world.setBlock(x, y, z, Blocks.snow, 0, 7);
					return true;
				}

				if (!Blocks.snow_layer.canPlaceBlockAt(world, x, y + 1, z) && block != Blocks.ice)
					return false;
				else
				{
					// TODO gamerforEA code start
					if (EventConfig.terraEvent && ModUtils.cantBreakOrNotInPrivate(player, x, y + 1, z))
						return false;
					// TODO gamerforEA code end

					world.setBlock(x, y + 1, z, Blocks.snow_layer, 0, 7);
					return true;
				}
			}
			else
			{
				// TODO gamerforEA code start
				if (EventConfig.terraEvent && ModUtils.cantBreakOrNotInPrivate(player, x, y, z))
					return false;
				// TODO gamerforEA code end

				world.setBlock(x, y, z, Blocks.ice, 0, 7);
				return true;
			}
		}
	}

	public boolean isSurroundedBySnow(World world, int x, int y, int z, EntityPlayer player) // TODO gamerforEA add EntityPlayer
	{
		return this.isSnowHere(world, x + 1, y, z, player) && this.isSnowHere(world, x - 1, y, z, player) && this.isSnowHere(world, x, y, z + 1, player) && this.isSnowHere(world, x, y, z - 1, player); // TODO gamerforEA add EntityPlayer
	}

	public boolean isSnowHere(World world, int x, int y, int z, EntityPlayer player) // TODO gamerforEA add EntityPlayer
	{
		int saveY = y;
		y = TileEntityTerra.getFirstBlockFrom(world, x, z, y + 16);
		if (saveY > y)
			return false;
		else
		{
			Block block = world.getBlock(x, y, z);
			if (block != Blocks.snow && block != Blocks.snow_layer)
			{
				if (Blocks.snow_layer.canPlaceBlockAt(world, x, y + 1, z) || block == Blocks.ice)
				{
					// TODO gamerforEA code start
					if (EventConfig.terraEvent && ModUtils.cantBreakOrNotInPrivate(player, x, y + 1, z))
						return false;
					// TODO gamerforEA code end

					world.setBlock(x, y + 1, z, Blocks.snow_layer, 0, 7);
				}

				return false;
			}
			else
				return true;
		}
	}
}
