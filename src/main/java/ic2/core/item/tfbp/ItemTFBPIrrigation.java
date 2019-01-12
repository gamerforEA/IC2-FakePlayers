package ic2.core.item.tfbp;

import com.gamerforea.ic2.EventConfig;
import com.gamerforea.ic2.ITerraformingBPFakePlayer;
import com.gamerforea.ic2.ModUtils;
import ic2.core.Ic2Items;
import ic2.core.block.BlockRubSapling;
import ic2.core.block.machine.tileentity.TileEntityTerra;
import ic2.core.init.InternalName;
import ic2.core.util.StackUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSapling;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class ItemTFBPIrrigation extends ItemTFBP
		implements ITerraformingBPFakePlayer // TODO gamerforEA implement ITerraformingBPFakePlayer
{
	public ItemTFBPIrrigation(InternalName internalName)
	{
		super(internalName);
	}

	@Override
	public int getConsume()
	{
		return 3000;
	}

	@Override
	public int getRange()
	{
		return 60;
	}

	// TODO gamerforEA code start
	@Override
	public boolean terraform(World world, int x, int z, int yCoord)
	{
		return this.terraform(world, x, z, yCoord, ModUtils.NEXUS_FACTORY.getFake(world));
	}

	public void createLeaves(World world, int x, int y, int z, Block oldBlock, int meta)
	{
		this.createLeaves(world, x, y, z, oldBlock, meta, ModUtils.NEXUS_FACTORY.getFake(world));
	}

	public boolean spreadGrass(World world, int x, int y, int z)
	{
		return this.spreadGrass(world, x, y, z, ModUtils.NEXUS_FACTORY.getFake(world));
	}
	// TODO gamerforEA code end

	@Override
	public boolean terraform(World world, int x, int z, int yCoord, EntityPlayer player) // TODO gamerforEA add EntityPlayer
	{
		if (world.rand.nextInt('뮀') == 0)
		{
			world.getWorldInfo().setRaining(true);
			return true;
		}
		else
		{
			int y = TileEntityTerra.getFirstBlockFrom(world, x, z, yCoord + 10);
			if (y == -1)
				return false;
			else if (TileEntityTerra.switchGround(world, Blocks.sand, Blocks.dirt, x, y, z, true, player)) // TODO gamerforEA add EntityPlayer
			{
				TileEntityTerra.switchGround(world, Blocks.sand, Blocks.dirt, x, y, z, true, player); // TODO gamerforEA add EntityPlayer
				return true;
			}
			else
			{
				Block block = world.getBlock(x, y, z);
				if (block != Blocks.tallgrass)
				{
					if (block == Blocks.sapling)
					{
						((BlockSapling) Blocks.sapling).func_149878_d(world, x, y, z, world.rand);
						return true;
					}
					else if (StackUtil.equals(block, Ic2Items.rubberSapling))
					{
						((BlockRubSapling) StackUtil.getBlock(Ic2Items.rubberSapling)).func_149878_d(world, x, y, z, world.rand);
						return true;
					}
					else if (block == Blocks.log)
					{
						// TODO gamerforEA code start
						if (EventConfig.terraEvent && ModUtils.cantBreakOrNotInPrivate(player, x, y + 1, z))
							return false;
						// TODO gamerforEA code end

						int meta = world.getBlockMetadata(x, y, z);
						world.setBlock(x, y + 1, z, Blocks.log, meta, 7);
						this.createLeaves(world, x, y + 2, z, block, meta, player); // TODO gamerforEA add EntityPlayer
						this.createLeaves(world, x + 1, y + 1, z, block, meta, player); // TODO gamerforEA add EntityPlayer
						this.createLeaves(world, x - 1, y + 1, z, block, meta, player); // TODO gamerforEA add EntityPlayer
						this.createLeaves(world, x, y + 1, z + 1, block, meta, player); // TODO gamerforEA add EntityPlayer
						this.createLeaves(world, x, y + 1, z - 1, block, meta, player); // TODO gamerforEA add EntityPlayer
						return true;
					}
					else if (block == Blocks.wheat)
					{
						// TODO gamerforEA code start
						if (EventConfig.terraEvent && ModUtils.cantBreakOrNotInPrivate(player, x, y, z))
							return false;
						// TODO gamerforEA code end

						world.setBlockMetadataWithNotify(x, y, z, 7, 7);
						return true;
					}
					else if (block == Blocks.fire)
					{
						// TODO gamerforEA code start
						if (EventConfig.terraEvent && ModUtils.cantBreakOrNotInPrivate(player, x, y, z))
							return false;
						// TODO gamerforEA code end

						world.setBlockToAir(x, y, z);
						return true;
					}
					else
						return false;
				}
				else
					return this.spreadGrass(world, x + 1, y, z, player) || this.spreadGrass(world, x - 1, y, z, player) || this.spreadGrass(world, x, y, z + 1, player) || this.spreadGrass(world, x, y, z - 1, player); // TODO gamerforEA add EntityPlayer
			}
		}
	}

	public void createLeaves(World world, int x, int y, int z, Block oldBlock, int meta, EntityPlayer player) // TODO gamerforEA add EntityPlayer
	{
		if (oldBlock.isAir(world, x, y, z))
		{
			// TODO gamerforEA code start
			if (EventConfig.terraEvent && ModUtils.cantBreakOrNotInPrivate(player, x, y, z))
				return;
			// TODO gamerforEA code end

			world.setBlock(x, y, z, Blocks.leaves, meta, 7);
		}
	}

	public boolean spreadGrass(World world, int x, int y, int z, EntityPlayer player) // TODO gamerforEA add EntityPlayer
	{
		if (world.rand.nextBoolean())
			return false;
		else
		{
			y = TileEntityTerra.getFirstBlockFrom(world, x, z, y);
			Block block = world.getBlock(x, y, z);
			if (block == Blocks.dirt)
			{
				// TODO gamerforEA code start
				if (EventConfig.terraEvent && ModUtils.cantBreakOrNotInPrivate(player, x, y, z))
					return false;
				// TODO gamerforEA code end

				world.setBlock(x, y, z, Blocks.grass, 0, 7);
				return true;
			}
			else if (block == Blocks.grass)
			{
				// TODO gamerforEA code start
				if (EventConfig.terraEvent && ModUtils.cantBreakOrNotInPrivate(player, x, y + 1, z))
					return false;
				// TODO gamerforEA code end

				world.setBlock(x, y + 1, z, Blocks.tallgrass, 1, 7);
				return true;
			}
			else
				return false;
		}
	}
}
