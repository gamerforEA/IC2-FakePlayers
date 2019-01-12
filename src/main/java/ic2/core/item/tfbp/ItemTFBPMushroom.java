package ic2.core.item.tfbp;

import com.gamerforea.ic2.EventConfig;
import com.gamerforea.ic2.ITerraformingBPFakePlayer;
import com.gamerforea.ic2.ModUtils;
import ic2.core.block.machine.tileentity.TileEntityTerra;
import ic2.core.init.InternalName;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockMushroom;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

public class ItemTFBPMushroom extends ItemTFBP
		implements ITerraformingBPFakePlayer // TODO gamerforEA implement ITerraformingBPFakePlayer
{
	public ItemTFBPMushroom(InternalName internalName)
	{
		super(internalName);
	}

	@Override
	public int getConsume()
	{
		return 8000;
	}

	@Override
	public int getRange()
	{
		return 25;
	}

	// TODO gamerforEA code start
	@Override
	public boolean terraform(World world, int x, int z, int yCoord)
	{
		return this.terraform(world, x, z, yCoord, ModUtils.NEXUS_FACTORY.getFake(world));
	}

	public boolean growBlockWithDependancy(World world, int x, int y, int z, Block target, Block dependancy)
	{
		return this.growBlockWithDependancy(world, x, y, z, target, dependancy, ModUtils.NEXUS_FACTORY.getFake(world));
	}
	// TODO gamerforEA code end

	@Override
	public boolean terraform(World world, int x, int z, int yCoord, EntityPlayer player) // TODO gamerforEA add EntityPlayer
	{
		int y = TileEntityTerra.getFirstSolidBlockFrom(world, x, z, yCoord + 20);
		return y != -1 && this.growBlockWithDependancy(world, x, y, z, Blocks.brown_mushroom_block, Blocks.brown_mushroom, player); // TODO gamerforEA add EntityPlayer
	}

	public boolean growBlockWithDependancy(World world, int x, int y, int z, Block target, Block dependancy, EntityPlayer player) // TODO gamerforEA add EntityPlayer
	{
		int xm;
		int zm;
		Block block;
		for (int base = x - 1; dependancy != null && base < x + 1; ++base)
		{
			for (xm = z - 1; xm < z + 1; ++xm)
			{
				for (zm = y + 5; zm > y - 2; --zm)
				{
					block = world.getBlock(base, zm, xm);
					if (dependancy == Blocks.mycelium)
					{
						if (block == dependancy || block == Blocks.brown_mushroom_block || block == Blocks.red_mushroom_block)
							break;

						if (!block.isAir(world, base, zm, xm) && (block == Blocks.dirt || block == Blocks.grass))
						{
							// TODO gamerforEA code start
							if (EventConfig.terraEvent && ModUtils.cantBreakOrNotInPrivate(player, base, zm, xm))
								return false;
							// TODO gamerforEA code end

							world.setBlock(base, zm, xm, dependancy, 0, 7);
							TileEntityTerra.setBiomeAt(world, x, z, BiomeGenBase.mushroomIsland);
							return true;
						}
					}
					else if (dependancy == Blocks.brown_mushroom)
					{
						if (block == Blocks.brown_mushroom || block == Blocks.red_mushroom)
							break;

						if (!block.isAir(world, base, zm, xm) && this.growBlockWithDependancy(world, base, zm, xm, Blocks.brown_mushroom, Blocks.mycelium, player))
							return true;
					}
				}
			}
		}

		Block var11;
		if (target == Blocks.brown_mushroom)
		{
			var11 = world.getBlock(x, y, z);
			if (var11 != Blocks.mycelium)
			{
				if (var11 != Blocks.brown_mushroom_block && var11 != Blocks.red_mushroom_block)
					return false;

				// TODO gamerforEA code start
				if (!EventConfig.terraEvent || !ModUtils.cantBreakOrNotInPrivate(player, x, y, z))
					// TODO gamerforEA code end
					world.setBlock(x, y, z, Blocks.mycelium, 0, 7);
			}

			Block var12 = world.getBlock(x, y + 1, z);
			if (!var12.isAir(world, x, y + 1, z) && var12 != Blocks.tallgrass)
				return false;
			else
			{
				// TODO gamerforEA code start
				if (EventConfig.terraEvent && ModUtils.cantBreakOrNotInPrivate(player, x, y + 1, z))
					return false;
				// TODO gamerforEA code end

				BlockBush var13 = world.rand.nextBoolean() ? Blocks.brown_mushroom : Blocks.red_mushroom;
				world.setBlock(x, y + 1, z, var13, 0, 7);
				return true;
			}
		}
		else
		{
			if (target == Blocks.brown_mushroom_block)
			{
				var11 = world.getBlock(x, y + 1, z);
				if (var11 != Blocks.brown_mushroom && var11 != Blocks.red_mushroom)
					return false;

				if (((BlockMushroom) var11).func_149884_c(world, x, y + 1, z, world.rand))
				{
					for (xm = x - 1; xm < x + 1; ++xm)
					{
						for (zm = z - 1; zm < z + 1; ++zm)
						{
							block = world.getBlock(xm, y + 1, zm);
							if (block == Blocks.brown_mushroom || block == Blocks.red_mushroom)
							{
								// TODO gamerforEA code start
								if (EventConfig.terraEvent && ModUtils.cantBreakOrNotInPrivate(player, x, y + 1, z))
									continue;
								// TODO gamerforEA code end

								world.setBlockToAir(xm, y + 1, zm);
							}
						}
					}

					return true;
				}
			}

			return false;
		}
	}
}
