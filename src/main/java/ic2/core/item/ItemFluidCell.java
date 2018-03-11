package ic2.core.item;

import com.gamerforea.eventhelper.util.EventUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.core.IC2;
import ic2.core.Ic2Items;
import ic2.core.init.InternalName;
import ic2.core.util.LiquidUtil;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.*;

import java.util.List;

public class ItemFluidCell extends ItemIC2FluidContainer
{
	public ItemFluidCell(InternalName internalName)
	{
		super(internalName, 1000);
	}

	@Override
	public String getTextureFolder()
	{
		return "cell";
	}

	@Override
	public String getTextureName(int index)
	{
		switch (index)
		{
			case 0:
				return this.getUnlocalizedName().substring(4);
			case 1:
				return this.getUnlocalizedName().substring(4) + ".window";
			default:
				return null;
		}
	}

	@SideOnly(Side.CLIENT)
	IIcon getWindowIcon()
	{
		return this.textures[1];
	}

	@Override
	public boolean isRepairable()
	{
		return false;
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float xOffset, float yOffset, float zOffset)
	{
		// TODO gamerforEA code start
		if (player instanceof FakePlayer)
			return false;
		// TODO gamerforEA code end

		if (!IC2.platform.isSimulating())
			return false;
		else if (this.interactWithTank(stack, player, world, x, y, z, side))
			return true;
		else
		{
			MovingObjectPosition position = this.getMovingObjectPositionFromPlayer(world, player, true);
			if (position == null)
				return false;
			else
			{
				if (position.typeOfHit == MovingObjectType.BLOCK)
				{
					x = position.blockX;
					y = position.blockY;
					z = position.blockZ;
					if (!world.canMineBlock(player, x, y, z))
						return false;

					if (!player.canPlayerEdit(x, y, z, position.sideHit, stack))
						return false;

					// TODO gamerforEA code start
					if (EventUtils.cantBreak(player, x, y, z))
						return false;
					// TODO gamerforEA code end

					if (this.collectFluidBlock(stack, player, world, x, y, z))
						return true;

					ForgeDirection dir = ForgeDirection.VALID_DIRECTIONS[position.sideHit];

					// TODO gamerforEA code start
					if (EventUtils.cantBreak(player, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ))
						return false;
					// TODO gamerforEA code end

					FluidStack fs = LiquidUtil.drainContainerStack(stack, player, 1000, true);

					if (LiquidUtil.placeFluid(fs, world, x, y, z) || player.canPlayerEdit(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ, position.sideHit, stack) && LiquidUtil.placeFluid(fs, world, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ))
					{
						if (!player.capabilities.isCreativeMode)
							LiquidUtil.drainContainerStack(stack, player, 1000, false);

						return true;
					}
				}

				return false;
			}
		}
	}

	@Override
	public boolean canfill(Fluid fluid)
	{
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs par2CreativeTabs, List itemList)
	{
		itemList.add(Ic2Items.FluidCell.copy());

		for (Fluid fluid : FluidRegistry.getRegisteredFluids().values())
		{
			if (fluid != null)
			{
				ItemStack stack = Ic2Items.FluidCell.copy();
				this.fill(stack, new FluidStack(fluid, Integer.MAX_VALUE), true);
				itemList.add(stack);
			}
		}

	}

	private boolean interactWithTank(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side)
	{
		if (!IC2.platform.isSimulating())
			return false;
		else
		{
			TileEntity te = world.getTileEntity(x, y, z);
			if (!(te instanceof IFluidHandler))
				return false;
			else
			{
				IFluidHandler handler = (IFluidHandler) te;
				ForgeDirection dir = ForgeDirection.getOrientation(side);
				FluidStack fs = this.getFluid(stack);
				if (fs == null || player.isSneaking() && fs.amount < this.capacity)
				{
					int amount = fs == null ? this.capacity : this.capacity - fs.amount;
					FluidStack input = handler.drain(dir, amount, false);
					if (input != null && input.amount > 0)
					{
						amount = LiquidUtil.fillContainerStack(stack, player, input, false);
						if (amount <= 0)
							return true;
						else
						{
							handler.drain(dir, amount, true);
							return true;
						}
					}
					else
						return true;
				}
				else
				{
					int amount = handler.fill(dir, fs, false);
					if (amount <= 0)
						return true;
					else
					{
						fs = LiquidUtil.drainContainerStack(stack, player, amount, false);
						if (fs != null && fs.amount > 0)
						{
							handler.fill(dir, fs, true);
							return true;
						}
						else
							return true;
					}
				}
			}
		}
	}

	private boolean collectFluidBlock(ItemStack stack, EntityPlayer player, World world, int x, int y, int z)
	{
		Block block = world.getBlock(x, y, z);
		if (block instanceof IFluidBlock)
		{
			IFluidBlock liquid = (IFluidBlock) block;
			if (liquid.canDrain(world, x, y, z))
			{
				FluidStack fluid = liquid.drain(world, x, y, z, false);
				int amount = LiquidUtil.fillContainerStack(stack, player, fluid, true);
				if (amount == fluid.amount)
				{
					LiquidUtil.fillContainerStack(stack, player, fluid, false);
					liquid.drain(world, x, y, z, true);
					return true;
				}
			}
		}
		else if (world.getBlockMetadata(x, y, z) == 0)
		{
			FluidStack fluid = null;
			if (block != Blocks.water && block != Blocks.flowing_water)
			{
				if (block == Blocks.lava || block == Blocks.flowing_lava)
					fluid = new FluidStack(FluidRegistry.LAVA, 1000);
			}
			else
				fluid = new FluidStack(FluidRegistry.WATER, 1000);

			if (fluid != null)
			{
				int amount = LiquidUtil.fillContainerStack(stack, player, fluid, true);
				if (amount == fluid.amount)
				{
					LiquidUtil.fillContainerStack(stack, player, fluid, false);
					world.setBlockToAir(x, y, z);
					return true;
				}
			}
		}

		return false;
	}

	public static ItemStack getUniversalFluidCell(FluidStack fluidStack)
	{
		ItemStack stack = Ic2Items.FluidCell.copy();
		((ItemFluidCell) Ic2Items.FluidCell.getItem()).fill(stack, new FluidStack(fluidStack.getFluid(), fluidStack.amount), true);
		return stack;
	}
}
