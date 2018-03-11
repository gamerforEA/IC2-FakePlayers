package ic2.core.block.machine.tileentity;

import com.gamerforea.ic2.EventConfig;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.Direction;
import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.audio.AudioSource;
import ic2.core.audio.PositionSpec;
import ic2.core.block.TileEntityLiquidTankElectricMachine;
import ic2.core.block.invslot.*;
import ic2.core.block.machine.container.ContainerPump;
import ic2.core.block.machine.gui.GuiPump;
import ic2.core.upgrade.IUpgradableBlock;
import ic2.core.upgrade.IUpgradeItem;
import ic2.core.upgrade.UpgradableProperty;
import ic2.core.util.PumpUtil;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.*;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.EnumSet;
import java.util.Set;

public class TileEntityPump extends TileEntityLiquidTankElectricMachine implements IHasGui, IUpgradableBlock
{
	public final int defaultTier;
	public int energyConsume;
	public int operationsPerTick;
	public final int defaultEnergyStorage;
	public final int defaultEnergyConsume;
	public final int defaultOperationLength;
	private AudioSource audioSource;
	private TileEntityMiner miner = null;
	public boolean redstonePowered = false;
	public final InvSlotCharge chargeSlot = new InvSlotCharge(this, 0, 1);
	public final InvSlotConsumableLiquid containerSlot = new InvSlotConsumableLiquid(this, "containerSlot", 1, InvSlot.Access.I, 1, InvSlot.InvSide.TOP, InvSlotConsumableLiquid.OpType.Fill);
	public final InvSlotOutput outputSlot = new InvSlotOutput(this, "output", 2, 1);
	public final InvSlotUpgrade upgradeSlot = new InvSlotUpgrade(this, "upgrade", 3, 4);
	public short progress = 0;
	public int operationLength;
	public float guiProgress;

	public TileEntityPump()
	{
		super(20, 1, 1, 8);
		this.defaultEnergyConsume = this.energyConsume = 1;
		this.defaultOperationLength = this.operationLength = 20;
		this.defaultTier = 1;
		this.defaultEnergyStorage = 1 * this.operationLength;
	}

	@Override
	public void onUnloaded()
	{
		if (IC2.platform.isRendering() && this.audioSource != null)
		{
			IC2.audioManager.removeSources(this);
			this.audioSource = null;
		}

		this.miner = null;
		super.onUnloaded();
	}

	@Override
	protected void updateEntityServer()
	{
		super.updateEntityServer();
		boolean needsInvUpdate = false;
		if (this.canoperate() && this.energy >= this.energyConsume * this.operationLength)
			if (this.progress < this.operationLength)
			{
				++this.progress;
				this.energy -= this.energyConsume;
			}
			else
			{
				this.progress = 0;
				this.operate(false);
			}

		MutableObject<ItemStack> output = new MutableObject();
		if (this.containerSlot.transferFromTank(this.fluidTank, output, true) && (output.getValue() == null || this.outputSlot.canAdd(output.getValue())))
		{
			this.containerSlot.transferFromTank(this.fluidTank, output, false);
			if (output.getValue() != null)
				this.outputSlot.add(output.getValue());
		}

		for (int i = 0; i < this.upgradeSlot.size(); ++i)
		{
			ItemStack stack = this.upgradeSlot.get(i);
			if (stack != null && stack.getItem() instanceof IUpgradeItem && ((IUpgradeItem) stack.getItem()).onTick(stack, this))
				needsInvUpdate = true;
		}

		this.guiProgress = (float) this.progress / (float) this.operationLength;
		if (needsInvUpdate)
			super.markDirty();

	}

	@Override
	public String getInventoryName()
	{
		return "Pump";
	}

	public boolean canoperate()
	{
		return this.operate(true);
	}

	public boolean operate(boolean sim)
	{
		if (this.miner == null || this.miner.isInvalid())
		{
			this.miner = null;

			for (Direction dir : Direction.directions)
			{
				if (dir != Direction.YP)
				{
					TileEntity te = dir.applyToTileEntity(this);
					if (te instanceof TileEntityMiner)
					{
						this.miner = (TileEntityMiner) te;
						break;
					}
				}
			}
		}

		FluidStack liquid = null;
		if (this.miner != null)
		{
			if (this.miner.canProvideLiquid)
				liquid = this.pump(this.miner.liquidX, this.miner.liquidY, this.miner.liquidZ, sim, this.miner);
		}
		else
		{
			ForgeDirection dir = ForgeDirection.getOrientation(this.getFacing());
			liquid = this.pump(this.xCoord + dir.offsetX, this.yCoord + dir.offsetY, this.zCoord + dir.offsetZ, sim, this.miner);
		}

		if (liquid != null && this.getFluidTank().fill(liquid, false) > 0)
		{
			if (!sim)
				this.getFluidTank().fill(liquid, true);

			return true;
		}
		else
			return false;
	}

	public FluidStack pump(int x, int y, int z, boolean sim, TileEntity miner)
	{
		// TODO gamerforEA code start
		if (EventConfig.pumpEvent && this.fake.cantBreak(x, y, z))
			return null;
		// TODO gamerforEA code end

		FluidStack ret = null;
		TileEntity te = null;
		int freespace = this.fluidTank.getCapacity() - this.fluidTank.getFluidAmount();
		if (miner == null && freespace > 0 && (te = this.worldObj.getTileEntity(x, y, z)) instanceof IFluidHandler)
		{
			if (freespace > 1000)
				freespace = 1000;

			ret = ((IFluidHandler) te).drain(ForgeDirection.getOrientation(this.getFacing()), freespace, false);
			if (ret != null)
			{
				if (!((IFluidHandler) te).canDrain(ForgeDirection.getOrientation(this.getFacing()), ret.getFluid()))
					return null;

				if (sim)
					ret = ((IFluidHandler) te).drain(ForgeDirection.getOrientation(this.getFacing()), freespace, false);
				else
					ret = ((IFluidHandler) te).drain(ForgeDirection.getOrientation(this.getFacing()), freespace, true);
			}

			return ret;
		}
		else
		{
			if (freespace >= 1000)
			{
				int[] cood = PumpUtil.searchFluidSource(this.worldObj, x, y, z);
				if (cood.length > 0)
				{
					Block block = this.worldObj.getBlock(cood[0], cood[1], cood[2]);
					if (block instanceof IFluidBlock)
					{
						IFluidBlock liquid = (IFluidBlock) block;
						if (liquid.canDrain(this.worldObj, cood[0], cood[1], cood[2]))
							if (!sim)
							{
								ret = liquid.drain(this.worldObj, cood[0], cood[1], cood[2], true);
								this.worldObj.setBlockToAir(cood[0], cood[1], cood[2]);
							}
							else
								ret = new FluidStack(liquid.getFluid(), 1000);
					}
					else if (block != Blocks.water && block != Blocks.flowing_water)
					{
						if (block == Blocks.lava || block == Blocks.flowing_lava)
						{
							if (this.worldObj.getBlockMetadata(cood[0], cood[1], cood[2]) != 0)
								return null;

							ret = new FluidStack(FluidRegistry.LAVA, 1000);
							if (!sim)
								this.worldObj.setBlockToAir(cood[0], cood[1], cood[2]);
						}
					}
					else
					{
						if (this.worldObj.getBlockMetadata(cood[0], cood[1], cood[2]) != 0)
							return null;

						ret = new FluidStack(FluidRegistry.WATER, 1000);
						if (!sim)
							this.worldObj.setBlockToAir(cood[0], cood[1], cood[2]);
					}
				}
			}

			return ret;
		}
	}

	public boolean facingMatchesDirection(ForgeDirection direction)
	{
		return direction.ordinal() == this.getFacing();
	}

	@Override
	public boolean wrenchCanSetFacing(EntityPlayer entityPlayer, int side)
	{
		return this.getFacing() != side;
	}

	@Override
	public void setFacing(short side)
	{
		super.setFacing(side);
	}

	@Override
	public void onLoaded()
	{
		super.onLoaded();
		if (IC2.platform.isSimulating())
			this.setUpgradestat();

	}

	@Override
	public void markDirty()
	{
		super.markDirty();
		if (IC2.platform.isSimulating())
			this.setUpgradestat();

	}

	public void setUpgradestat()
	{
		int extraProcessTime = 0;
		double processTimeMultiplier = 1.0D;
		int extraEnergyDemand = 0;
		double energyDemandMultiplier = 1.0D;
		int extraEnergyStorage = 0;
		double energyStorageMultiplier = 1.0D;
		int extraTier = 0;

		for (int i = 0; i < this.upgradeSlot.size(); ++i)
		{
			ItemStack stack = this.upgradeSlot.get(i);
			if (stack != null && stack.getItem() instanceof IUpgradeItem)
			{
				IUpgradeItem upgrade = (IUpgradeItem) stack.getItem();
				extraProcessTime += upgrade.getExtraProcessTime(stack, this) * stack.stackSize;
				processTimeMultiplier *= Math.pow(upgrade.getProcessTimeMultiplier(stack, this), stack.stackSize);
				extraEnergyDemand += upgrade.getExtraEnergyDemand(stack, this) * stack.stackSize;
				energyDemandMultiplier *= Math.pow(upgrade.getEnergyDemandMultiplier(stack, this), stack.stackSize);
				extraEnergyStorage += upgrade.getExtraEnergyStorage(stack, this) * stack.stackSize;
				energyStorageMultiplier *= Math.pow(upgrade.getEnergyStorageMultiplier(stack, this), stack.stackSize);
				extraTier += upgrade.getExtraTier(stack, this) * stack.stackSize;
			}
		}

		double previousProgress = (double) this.progress / (double) this.operationLength;
		double stackOpLen = ((double) this.defaultOperationLength + (double) extraProcessTime) * 64.0D * processTimeMultiplier;
		this.operationsPerTick = (int) Math.min(Math.ceil(64.0D / stackOpLen), 2.147483647E9D);
		this.operationLength = (int) Math.round(stackOpLen * this.operationsPerTick / 64.0D);
		this.energyConsume = applyModifier(this.defaultEnergyConsume, extraEnergyDemand, energyDemandMultiplier);
		this.setTier(applyModifier(this.defaultTier, extraTier, 1.0D));
		this.maxEnergy = applyModifier(this.defaultEnergyStorage, extraEnergyStorage + this.operationLength * this.energyConsume, energyStorageMultiplier);
		if (this.operationLength < 1)
			this.operationLength = 1;

		this.progress = (short) (int) Math.floor(previousProgress * this.operationLength + 0.1D);
	}

	private static int applyModifier(int base, int extra, double multiplier)
	{
		double ret = Math.round(((double) base + (double) extra) * multiplier);
		return ret > 2.147483647E9D ? Integer.MAX_VALUE : (int) ret;
	}

	@Override
	public double getEnergy()
	{
		return this.energy;
	}

	@Override
	public boolean useEnergy(double amount)
	{
		if (this.energy >= amount)
		{
			this.energy -= amount;
			return true;
		}
		else
			return false;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound)
	{
		super.readFromNBT(nbttagcompound);
		this.progress = nbttagcompound.getShort("progress");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound)
	{
		super.writeToNBT(nbttagcompound);
		nbttagcompound.setShort("progress", this.progress);
	}

	@Override
	public ContainerBase<TileEntityPump> getGuiContainer(EntityPlayer entityPlayer)
	{
		return new ContainerPump(entityPlayer, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getGui(EntityPlayer entityPlayer, boolean isAdmin)
	{
		return new GuiPump(new ContainerPump(entityPlayer, this));
	}

	@Override
	public void onGuiClosed(EntityPlayer entityPlayer)
	{
	}

	@Override
	public void onNetworkUpdate(String field)
	{
		if (field.equals("active") && this.prevActive != this.getActive())
		{
			if (this.audioSource == null)
				this.audioSource = IC2.audioManager.createSource(this, PositionSpec.Center, "Machines/PumpOp.ogg", true, false, IC2.audioManager.getDefaultVolume());

			if (this.getActive())
			{
				if (this.audioSource != null)
					this.audioSource.play();
			}
			else if (this.audioSource != null)
				this.audioSource.stop();
		}

		super.onNetworkUpdate(field);
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid)
	{
		return false;
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid)
	{
		return true;
	}

	@Override
	public Set<UpgradableProperty> getUpgradableProperties()
	{
		return EnumSet.of(UpgradableProperty.Processing, UpgradableProperty.Transformer, UpgradableProperty.EnergyStorage, UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing, UpgradableProperty.FluidProducing);
	}
}
