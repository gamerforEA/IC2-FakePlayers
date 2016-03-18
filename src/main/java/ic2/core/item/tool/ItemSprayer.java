package ic2.core.item.tool;

import java.util.ArrayList;
import java.util.Random;

import com.gamerforea.eventhelper.util.EventUtils;
import com.gamerforea.ic2.EventConfig;
import com.gamerforea.ic2.ModUtils;

import ic2.api.item.IBoxable;
import ic2.core.IC2;
import ic2.core.Ic2Items;
import ic2.core.block.wiring.TileEntityCable;
import ic2.core.init.BlocksItems;
import ic2.core.init.InternalName;
import ic2.core.item.ItemIC2FluidContainer;
import ic2.core.item.armor.ItemArmorCFPack;
import ic2.core.util.LiquidUtil;
import ic2.core.util.StackUtil;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;

public class ItemSprayer extends ItemIC2FluidContainer implements IBoxable
{
	public ItemSprayer(InternalName internalName)
	{
		super(internalName, 8000);
		this.setMaxStackSize(1);
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)
	{
		if (IC2.platform.isSimulating() && IC2.keyboard.isModeSwitchKeyDown(player))
		{
			NBTTagCompound nbtData = StackUtil.getOrCreateNbtData(stack);
			int mode = nbtData.getInteger("mode");
			mode = mode == 0 ? 1 : 0;
			nbtData.setInteger("mode", mode);
			String sMode = mode == 0 ? "ic2.tooltip.mode.normal" : "ic2.tooltip.mode.single";
			IC2.platform.messagePlayer(player, "ic2.tooltip.mode", new Object[] { sMode });
		}

		return super.onItemRightClick(stack, world, player);
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float xOffset, float yOffset, float zOffset)
	{
		if (IC2.keyboard.isModeSwitchKeyDown(player))
			return false;
		else if (!IC2.platform.isSimulating())
			return true;
		else
		{
			MovingObjectPosition movingobjectposition = this.getMovingObjectPositionFromPlayer(world, player, true);
			if (movingobjectposition == null)
				return false;
			else
			{
				if (movingobjectposition.typeOfHit == MovingObjectType.BLOCK)
				{
					int fx = movingobjectposition.blockX;
					int fy = movingobjectposition.blockY;
					int fz = movingobjectposition.blockZ;
					Block block = world.getBlock(fx, fy, fz);
					if (block instanceof IFluidBlock)
					{
						IFluidBlock liquid = (IFluidBlock) block;
						if (liquid.canDrain(world, fx, fy, fz))
						{
							FluidStack fluid = liquid.drain(world, fx, fy, fz, false);
							int amount = LiquidUtil.fillContainerStack(stack, player, fluid, true);
							if (amount == fluid.amount)
							{
								LiquidUtil.fillContainerStack(stack, player, fluid, false);
								liquid.drain(world, fx, fy, fz, true);
								return true;
							}
						}
					}
				}

				int maxFoamBlocks = 0;
				FluidStack fluid = this.getFluid(stack);
				if (fluid != null && fluid.amount > 0)
					maxFoamBlocks += fluid.amount / this.getFluidPerFoam();

				ItemStack pack = player.inventory.armorInventory[2];
				if (pack != null && pack.getItem() == Ic2Items.cfPack.getItem())
				{
					fluid = ((ItemArmorCFPack) pack.getItem()).getFluid(pack);
					if (fluid != null && fluid.amount > 0)
						maxFoamBlocks += fluid.amount / this.getFluidPerFoam();
					else
						pack = null;
				}
				else
					pack = null;

				if (maxFoamBlocks == 0)
					return false;
				else
				{
					maxFoamBlocks = Math.min(maxFoamBlocks, this.getMaxFoamBlocks(stack));
					ChunkPosition pos = new ChunkPosition(x, y, z);
					ItemSprayer.Target target;
					if (canPlaceFoam(world, pos, ItemSprayer.Target.Scaffold))
						target = ItemSprayer.Target.Scaffold;
					else if (canPlaceFoam(world, pos, ItemSprayer.Target.Cable))
						target = ItemSprayer.Target.Cable;
					else
					{
						switch (side)
						{
							case 0:
								--y;
								break;
							case 1:
								++y;
								break;
							case 2:
								--z;
								break;
							case 3:
								++z;
								break;
							case 4:
								--x;
								break;
							case 5:
								++x;
								break;
							default:
								assert false;
						}

						target = ItemSprayer.Target.Any;
					}

					// TODO gamerforEA add EntityPlayer parameter
					int amount = this.sprayFoam(world, x, y, z, calculateDirectionsFromPlayer(player), target, maxFoamBlocks, player);
					amount = amount * this.getFluidPerFoam();
					if (amount > 0)
					{
						if (pack != null)
						{
							fluid = ((ItemArmorCFPack) pack.getItem()).drainfromCFpack(player, pack, amount);
							amount -= fluid.amount;
						}

						if (amount > 0)
							this.drain(stack, amount, true);

						return true;
					}
					else
						return false;
				}
			}
		}
	}

	public static boolean[] calculateDirectionsFromPlayer(EntityPlayer player)
	{
		float yaw = player.rotationYaw % 360.0F;
		float pitch = player.rotationPitch;
		boolean[] r = new boolean[] { true, true, true, true, true, true };
		if (pitch >= -65.0F && pitch <= 65.0F)
		{
			if (yaw >= 300.0F && yaw <= 360.0F || yaw >= 0.0F && yaw <= 60.0F)
				r[2] = false;

			if (yaw >= 30.0F && yaw <= 150.0F)
				r[5] = false;

			if (yaw >= 120.0F && yaw <= 240.0F)
				r[3] = false;

			if (yaw >= 210.0F && yaw <= 330.0F)
				r[4] = false;
		}

		if (pitch <= -40.0F)
			r[0] = false;

		if (pitch >= 40.0F)
			r[1] = false;

		return r;
	}

	// TODO gamerforEA code start
	public int sprayFoam(World world, int x, int y, int z, boolean[] directions, Target target, int maxFoamBlocks)
	{
		return this.sprayFoam(world, x, y, z, directions, target, maxFoamBlocks, ModUtils.getModFake(world));
	}
	// TODO gamerforEA code end

	// TODO gamerforEA add EntityPlayer parameter
	public int sprayFoam(World world, int x, int y, int z, boolean[] directions, ItemSprayer.Target target, int maxFoamBlocks, EntityPlayer player)
	{
		ChunkPosition startPos = new ChunkPosition(x, y, z);
		if (!canPlaceFoam(world, startPos, target))
			return 0;
		else
		{
			ArrayList<ChunkPosition> check = new ArrayList();
			ArrayList<ChunkPosition> place = new ArrayList();
			int foamBlocks = 0;
			check.add(new ChunkPosition(x, y, z));

			for (int i = 0; i < check.size() && foamBlocks < maxFoamBlocks; ++i)
			{
				ChunkPosition set = check.get(i);
				if (canPlaceFoam(world, set, target))
				{
					this.considerAddingCoord(set, place);
					this.addAdjacentSpacesOnList(set.chunkPosX, set.chunkPosY, set.chunkPosZ, check, directions, target != ItemSprayer.Target.Any);
					++foamBlocks;
				}
			}

			for (ChunkPosition pos : place)
			{
				// TODO gamerforEA code start
				if (EventConfig.sprayerEvent && EventUtils.cantBreak(player, pos.chunkPosX, pos.chunkPosY, pos.chunkPosZ))
					continue;
				// TODO gamerforEA code end

				Block targetBlock = world.getBlock(pos.chunkPosX, pos.chunkPosY, pos.chunkPosZ);
				if (StackUtil.equals(targetBlock, Ic2Items.scaffold))
				{
					StackUtil.getBlock(Ic2Items.scaffold).dropBlockAsItem(world, pos.chunkPosX, pos.chunkPosY, pos.chunkPosZ, world.getBlockMetadata(pos.chunkPosX, pos.chunkPosY, pos.chunkPosZ), 0);
					world.setBlock(pos.chunkPosX, pos.chunkPosY, pos.chunkPosZ, StackUtil.getBlock(Ic2Items.constructionFoam), 0, 3);
				}
				else if (StackUtil.equals(targetBlock, Ic2Items.ironScaffold))
					world.setBlock(pos.chunkPosX, pos.chunkPosY, pos.chunkPosZ, StackUtil.getBlock(Ic2Items.constructionreinforcedFoam), 0, 3);
				else if (StackUtil.equals(targetBlock, Ic2Items.copperCableBlock))
				{
					TileEntity te = world.getTileEntity(pos.chunkPosX, pos.chunkPosY, pos.chunkPosZ);
					if (te instanceof TileEntityCable)
						((TileEntityCable) te).changeFoam((byte) 1);
				}
				else
					world.setBlock(pos.chunkPosX, pos.chunkPosY, pos.chunkPosZ, StackUtil.getBlock(Ic2Items.constructionFoam), 0, 3);
			}

			return foamBlocks;
		}
	}

	public void addAdjacentSpacesOnList(int x, int y, int z, ArrayList<ChunkPosition> foam, boolean[] directions, boolean ignoreDirections)
	{
		int[] order = this.generateRngSpread(IC2.random);

		for (int element : order)
			if (ignoreDirections || directions[element])
				switch (element)
				{
					case 0:
						this.considerAddingCoord(new ChunkPosition(x, y - 1, z), foam);
						break;
					case 1:
						this.considerAddingCoord(new ChunkPosition(x, y + 1, z), foam);
						break;
					case 2:
						this.considerAddingCoord(new ChunkPosition(x, y, z - 1), foam);
						break;
					case 3:
						this.considerAddingCoord(new ChunkPosition(x, y, z + 1), foam);
						break;
					case 4:
						this.considerAddingCoord(new ChunkPosition(x - 1, y, z), foam);
						break;
					case 5:
						this.considerAddingCoord(new ChunkPosition(x + 1, y, z), foam);
				}

	}

	public void considerAddingCoord(ChunkPosition coord, ArrayList<ChunkPosition> list)
	{
		for (int i = 0; i < list.size(); ++i)
		{
			ChunkPosition entry = list.get(i);
			if (entry.chunkPosX == coord.chunkPosX && entry.chunkPosY == coord.chunkPosY && entry.chunkPosZ == coord.chunkPosZ)
				return;
		}

		list.add(coord);
	}

	public int[] generateRngSpread(Random random)
	{
		int[] re = new int[] { 0, 1, 2, 3, 4, 5 };

		for (int i = 0; i < 16; ++i)
		{
			int first = random.nextInt(6);
			int second = random.nextInt(6);
			int save = re[first];
			re[first] = re[second];
			re[second] = save;
		}

		return re;
	}

	protected int getMaxFoamBlocks(ItemStack stack)
	{
		NBTTagCompound nbtData = StackUtil.getOrCreateNbtData(stack);
		return nbtData.getInteger("mode") == 0 ? 10 : 1;
	}

	protected int getFluidPerFoam()
	{
		return 100;
	}

	@Override
	public boolean canBeStoredInToolbox(ItemStack itemstack)
	{
		return true;
	}

	@Override
	public boolean canfill(Fluid fluid)
	{
		return fluid == BlocksItems.getFluid(InternalName.fluidConstructionFoam);
	}

	private static boolean canPlaceFoam(World world, ChunkPosition pos, ItemSprayer.Target target)
	{
		int x = pos.chunkPosX;
		int y = pos.chunkPosY;
		int z = pos.chunkPosZ;
		switch (target)
		{
			case Any:
				return StackUtil.getBlock(Ic2Items.constructionFoam).canPlaceBlockAt(world, x, y, z);
			case Scaffold:
				Block block = world.getBlock(x, y, z);
				return StackUtil.equals(block, Ic2Items.scaffold) || StackUtil.equals(block, Ic2Items.ironScaffold);
			case Cable:
				Block block1 = world.getBlock(x, y, z);
				if (!StackUtil.equals(block1, Ic2Items.copperCableBlock))
					return false;

				TileEntity te = world.getTileEntity(x, y, z);
				if (te instanceof TileEntityCable)
					return !((TileEntityCable) te).isFoamed();
				break;
			default:
				assert false;
		}

		return false;
	}

	static enum Target
	{
		Any, Scaffold, Cable;
	}
}
