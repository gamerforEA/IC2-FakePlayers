package ic2.core.util;

import com.gamerforea.ic2.EventConfig;
import ic2.api.Direction;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fluids.*;

import java.util.ArrayList;
import java.util.List;

public class LiquidUtil
{
	public static LiquidUtil.LiquidData getLiquid(World world, int x, int y, int z)
	{
		Block block = world.getBlock(x, y, z);
		Fluid liquid = null;
		boolean isSource = false;
		if (block instanceof IFluidBlock)
		{
			IFluidBlock fblock = (IFluidBlock) block;
			liquid = fblock.getFluid();
			isSource = fblock.canDrain(world, x, y, z);
		}
		else if (block != Blocks.water && block != Blocks.flowing_water)
		{
			if (block == Blocks.lava || block == Blocks.flowing_lava)
			{
				liquid = FluidRegistry.LAVA;
				isSource = world.getBlockMetadata(x, y, z) == 0;
			}
		}
		else
		{
			liquid = FluidRegistry.WATER;
			isSource = world.getBlockMetadata(x, y, z) == 0;
		}

		return liquid != null ? new LiquidUtil.LiquidData(liquid, isSource) : null;
	}

	public static int fillContainerStack(ItemStack stack, EntityPlayer player, FluidStack fluid, boolean simulate)
	{
		Item item = stack.getItem();
		if (!(item instanceof IFluidContainerItem))
			return 0;
		else
		{
			IFluidContainerItem container = (IFluidContainerItem) item;
			if (stack.stackSize == 1)
				return container.fill(stack, fluid, !simulate);
			else
			{
				ItemStack testStack = StackUtil.copyWithSize(stack, 1);
				int amount = container.fill(testStack, fluid, true);
				if (amount <= 0)
					return 0;
				else if (StackUtil.storeInventoryItem(testStack, player, simulate))
				{
					if (!simulate)
						--stack.stackSize;

					return amount;
				}
				else
					return 0;
			}
		}
	}

	public static FluidStack drainContainerStack(ItemStack stack, EntityPlayer player, int maxAmount, boolean simulate)
	{
		Item item = stack.getItem();
		if (!(item instanceof IFluidContainerItem))
			return null;
		else
		{
			IFluidContainerItem container = (IFluidContainerItem) item;
			if (stack.stackSize == 1)
				return container.drain(stack, maxAmount, !simulate);
			else
			{
				ItemStack testStack = StackUtil.copyWithSize(stack, 1);
				FluidStack ret = container.drain(testStack, maxAmount, true);
				if (ret != null && ret.amount > 0)
				{
					if (StackUtil.storeInventoryItem(testStack, player, simulate))
					{
						if (!simulate)
							--stack.stackSize;

						return ret;
					}
					else
						return null;
				}
				else
					return null;
			}
		}
	}

	public static List<LiquidUtil.AdjacentFluidHandler> getAdjacentHandlers(TileEntity source)
	{
		List<LiquidUtil.AdjacentFluidHandler> ret = new ArrayList();

		for (Direction dir : Direction.directions)
		{
			TileEntity te = dir.applyToTileEntity(source);
			if (te instanceof IFluidHandler)
				ret.add(new LiquidUtil.AdjacentFluidHandler((IFluidHandler) te, dir));
		}

		return ret;
	}

	public static int distribute(TileEntity source, FluidStack stack, boolean simulate)
	{
		int transferred = 0;

		for (LiquidUtil.AdjacentFluidHandler handler : getAdjacentHandlers(source))
		{
			int amount = distributeTo(handler.handler, stack, handler.dir, simulate);
			transferred += amount;
			stack.amount -= amount;
			if (stack.amount <= 0)
				break;
		}

		stack.amount += transferred;
		return transferred;
	}

	public static int distributeTo(IFluidHandler target, FluidStack stack, Direction dirTo, boolean simulate)
	{
		return target.fill(dirTo.getInverse().toForgeDirection(), stack, !simulate);
	}

	public static int distributeAll(IFluidHandler source, int amount)
	{
		if (!(source instanceof TileEntity))
			throw new IllegalArgumentException("source has to be a tile entity");
		else
		{
			TileEntity srcTe = (TileEntity) source;
			int transferred = 0;

			for (Direction dir : Direction.directions)
			{
				TileEntity te = dir.applyToTileEntity(srcTe);
				if (te instanceof IFluidHandler)
				{
					FluidStack stack = transfer(source, dir, (IFluidHandler) te, amount);
					if (stack != null)
					{
						amount -= stack.amount;
						transferred += stack.amount;
						if (amount <= 0)
							break;
					}
				}
			}

			return transferred;
		}
	}

	public static FluidStack transfer(IFluidHandler source, Direction dir, IFluidHandler target, int amount)
	{
		// TODO gamerforEA code replace, old code: while (true)
		for (int i = 0; i < 100; i++)
		// TODO gamerforEA code end
		{
			FluidStack ret = source.drain(dir.toForgeDirection(), amount, false);
			if (ret != null && ret.amount > 0)
			{
				// TODO gamerforEA add condition [1]
				if (EventConfig.liquidChecks && ret.amount > amount)
					throw new IllegalStateException("The fluid handler " + source + " drained more than the requested amount.");

				int cAmount = target.fill(dir.getInverse().toForgeDirection(), ret, false);

				// TODO gamerforEA add condition [1]
				if (EventConfig.liquidChecks && cAmount > amount)
					throw new IllegalStateException("The fluid handler " + target + " filled more than the requested amount.");

				amount = cAmount;
				if (cAmount != ret.amount && cAmount > 0)
					continue;

				if (cAmount <= 0)
					return null;

				ret = source.drain(dir.toForgeDirection(), cAmount, true);

				// TODO gamerforEA add condition [1]
				if (EventConfig.liquidChecks && ret.amount != cAmount)
					throw new IllegalStateException("The fluid handler " + source + " drained inconsistently. Expected " + cAmount + ", got " + ret.amount + ".");

				amount = target.fill(dir.getInverse().toForgeDirection(), ret, true);

				// TODO gamerforEA add condition [1]
				if (EventConfig.liquidChecks && amount != ret.amount)
					throw new IllegalStateException("The fluid handler " + target + " filled inconsistently. Expected " + ret.amount + ", got " + amount + ".");

				return ret;
			}

			return null;
		}

		// TODO gamerforEA code start
		return null;
		// TODO gamerforEA code end
	}

	public static boolean check(FluidStack fs)
	{
		return fs.getFluid() != null;
	}

	public static boolean placeFluid(FluidStack fs, World world, int x, int y, int z)
	{
		if (fs != null && fs.amount >= 1000)
		{
			Fluid fluid = fs.getFluid();
			Block block = world.getBlock(x, y, z);
			if ((block.isAir(world, x, y, z) || !block.getMaterial().isSolid()) && fluid.canBePlacedInWorld() && (block != fluid.getBlock() || !isFullFluidBlock(world, x, y, z, block)))
			{
				if (world.provider.isHellWorld && fluid == FluidRegistry.WATER)
				{
					world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.fizz", 0.5F, 2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);

					for (int i = 0; i < 8; ++i)
					{
						world.spawnParticle("largesmoke", x + Math.random(), y + Math.random(), z + Math.random(), 0.0D, 0.0D, 0.0D);
					}
				}
				else
				{
					if (!world.isRemote && !block.getMaterial().isSolid() && !block.getMaterial().isLiquid())
						world.func_147480_a(x, y, z, true);

					if (fluid == FluidRegistry.WATER)
						block = Blocks.flowing_water;
					else if (fluid == FluidRegistry.LAVA)
						block = Blocks.flowing_lava;
					else
						block = fluid.getBlock();

					int meta = block instanceof BlockFluidBase ? ((BlockFluidBase) block).getMaxRenderHeightMeta() : 0;
					if (!world.setBlock(x, y, z, block, meta, 3))
						return false;
				}

				fs.amount -= 1000;
				return true;
			}
			else
				return false;
		}
		else
			return false;
	}

	private static boolean isFullFluidBlock(World world, int x, int y, int z, Block block)
	{
		if (!(block instanceof IFluidBlock))
			return (block == Blocks.water || block == Blocks.flowing_water || block == Blocks.lava || block == Blocks.flowing_lava) && world.getBlockMetadata(x, y, z) == 0;
		else
		{
			IFluidBlock fBlock = (IFluidBlock) block;
			FluidStack drained = fBlock.drain(world, x, y, z, false);
			return drained != null && drained.amount >= 1000;
		}
	}

	public static class AdjacentFluidHandler
	{
		public final IFluidHandler handler;
		public final Direction dir;

		private AdjacentFluidHandler(IFluidHandler handler, Direction dir)
		{
			this.handler = handler;
			this.dir = dir;
		}
	}

	public static class LiquidData
	{
		public final Fluid liquid;
		public final boolean isSource;

		LiquidData(Fluid liquid1, boolean isSource1)
		{
			this.liquid = liquid1;
			this.isSource = isSource1;
		}
	}
}
