package ic2.core.block.kineticgenerator.tileentity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.Direction;
import ic2.api.energy.tile.IKineticSource;
import ic2.core.*;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlotConsumable;
import ic2.core.block.invslot.InvSlotConsumableId;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.block.kineticgenerator.container.ContainerSteamKineticGenerator;
import ic2.core.block.kineticgenerator.gui.GuSteamKineticGenerator;
import ic2.core.block.machine.tileentity.TileEntityCondenser;
import ic2.core.init.BlocksItems;
import ic2.core.init.InternalName;
import ic2.core.init.MainConfig;
import ic2.core.upgrade.IUpgradableBlock;
import ic2.core.upgrade.IUpgradeItem;
import ic2.core.upgrade.UpgradableProperty;
import ic2.core.util.ConfigUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.*;

import java.util.EnumSet;
import java.util.Set;

public class TileEntitySteamKineticGenerator extends TileEntityInventory
		implements IKineticSource, IFluidHandler, IHasGui, IUpgradableBlock
{
	private boolean isturbefilledupwithwater = false;
	private float condensationprogress = 0.0F;
	private int updateTicker = IC2.random.nextInt(this.getTickRate());
	private int kUoutput;
	protected final FluidTank SteamTank = new FluidTank(21000);
	protected final FluidTank distilledwaterTank = new FluidTank(1000);
	public final InvSlotUpgrade upgradeSlot = new InvSlotUpgrade(this, "upgrade", 1, 1);
	public final InvSlotConsumable turbineSlot = new InvSlotConsumableId(this, "Turbineslot", 0, 1, Ic2Items.steamturbine.getItem());
	private static final float outputModifier = ConfigUtil.getFloat(MainConfig.get(), "balance/energy/kineticgenerator/steam");

	@Override
	protected void updateEntityServer()
	{
		super.updateEntityServer();
		boolean needsInvUpdate = false;
		if (this.distilledwaterTank.getCapacity() - this.distilledwaterTank.getFluidAmount() >= 1 && this.isturbefilledupwithwater)
			this.isturbefilledupwithwater = false;

		if (this.SteamTank.getFluidAmount() > 10 && !this.isturbefilledupwithwater && !this.turbineSlot.isEmpty())
		{
			if (!this.getActive())
			{
				this.setActive(true);
				needsInvUpdate = true;
			}

			boolean turbinework = this.turbinework();
			if (this.updateTicker++ >= this.getTickRate())
			{
				if (turbinework)
					if (!this.isHotSteam())
						this.turbineSlot.damage(2, false);
					else
						this.turbineSlot.damage(1, false);

				this.updateTicker = 0;
			}
		}
		else if (this.getActive())
		{
			this.setActive(false);
			needsInvUpdate = true;
			this.kUoutput = 0;
		}

		for (int i = 0; i < this.upgradeSlot.size(); ++i)
		{
			ItemStack stack = this.upgradeSlot.get(i);
			if (stack != null && stack.getItem() instanceof IUpgradeItem && ((IUpgradeItem) stack.getItem()).onTick(stack, this))
				this.markDirty();
		}

		if (needsInvUpdate)
			this.markDirty();

	}

	private float Steamhandling(int amount)
	{
		float KUWorkbuffer = 0.0F;
		float Steamfactor = 1.0F;
		if (this.isHotSteam())
			Steamfactor = 2.0F;

		this.SteamTank.drain(amount, true);
		KUWorkbuffer = amount * 2 * Steamfactor;
		if (this.isHotSteam())
			this.Steamoutput(amount);
		else
		{
			this.condensationprogress += amount / 100.0F * 10.0F;
			this.Steamoutput(amount / 100.0F * 90.0F);
		}

		return KUWorkbuffer;
	}

	private boolean turbinework()
	{
		float KUWorkbuffer = 0.0F;
		int Steamaount = this.SteamTank.getFluidAmount();
		if (Steamaount > 18000)
			KUWorkbuffer = this.Steamhandling(1000);
		else if (Steamaount > 16000)
			KUWorkbuffer = this.Steamhandling(800);
		else if (Steamaount > 12000)
			KUWorkbuffer = this.Steamhandling(600);
		else if (Steamaount > 8000)
			KUWorkbuffer = this.Steamhandling(400);
		else if (Steamaount > 4000)
			KUWorkbuffer = this.Steamhandling(200);
		else if (Steamaount > 2000)
			KUWorkbuffer = this.Steamhandling(100);
		else if (Steamaount > 1000)
			KUWorkbuffer = this.Steamhandling(50);
		else if (Steamaount > 800)
			KUWorkbuffer = this.Steamhandling(40);
		else if (Steamaount > 600)
			KUWorkbuffer = this.Steamhandling(30);
		else if (Steamaount > 400)
			KUWorkbuffer = this.Steamhandling(20);
		else if (Steamaount > 10)
			KUWorkbuffer = this.Steamhandling(10);
		else
			KUWorkbuffer = 0.0F;

		if (this.condensationprogress >= 100.0F)
			if (this.distilledwaterTank.fill(new FluidStack(BlocksItems.getFluid(InternalName.fluidDistilledWater), 1), false) == 1)
			{
				this.condensationprogress -= 100.0F;
				this.distilledwaterTank.fill(new FluidStack(BlocksItems.getFluid(InternalName.fluidDistilledWater), 1), true);
			}
			else
				this.isturbefilledupwithwater = true;

		this.kUoutput = (int) (KUWorkbuffer * (100.0F - (float) this.distilledwaterTank.getFluidAmount() / (float) this.distilledwaterTank.getCapacity() * 100.0F) / 100.0F * outputModifier);
		return KUWorkbuffer > 0.0F;
	}

	private void Steamoutput(float amount)
	{
		for (Direction direction : Direction.directions)
		{
			TileEntity target = direction.applyToTileEntity(this);
			if (this.isHotSteam())
			{
				if (!(target instanceof TileEntityCondenser) && !(target instanceof TileEntitySteamKineticGenerator))
					continue;
			}
			else if (!(target instanceof TileEntityCondenser))
				continue;

			int transamount = ((IFluidHandler) target).fill(direction.toForgeDirection().getOpposite(), new FluidStack(BlocksItems.getFluid(InternalName.fluidSteam), (int) amount), false);
			if (transamount > 0)
			{
				if (amount > transamount)
				{
					((IFluidHandler) target).fill(direction.toForgeDirection().getOpposite(), new FluidStack(BlocksItems.getFluid(InternalName.fluidSteam), (int) amount), true);
					amount -= transamount;
				}
				else
				{
					((IFluidHandler) target).fill(direction.toForgeDirection().getOpposite(), new FluidStack(BlocksItems.getFluid(InternalName.fluidSteam), (int) amount), true);
					amount = 0.0F;
				}

				if (amount == 0.0F)
					break;
			}
		}

		if (amount > 0.0F && IC2.random.nextInt(10) == 0)
		{
			ExplosionIC2 explosion = new ExplosionIC2(this.worldObj, null, this.xCoord, this.yCoord, this.zCoord, 1.0F, 1.0F, ExplosionIC2.Type.Heat);

			// TODO gamerforEA code start
			explosion.fake.setParent(this.fake);
			// TODO gamerforEA code end

			explosion.doExplosion();
		}

	}

	public int gethUoutput()
	{
		return this.kUoutput;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound)
	{
		super.readFromNBT(nbttagcompound);
		this.condensationprogress = nbttagcompound.getFloat("condensationprogress");
		this.distilledwaterTank.readFromNBT(nbttagcompound.getCompoundTag("distilledwaterTank"));
		this.SteamTank.readFromNBT(nbttagcompound.getCompoundTag("SteamTank"));
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound)
	{
		super.writeToNBT(nbttagcompound);
		nbttagcompound.setFloat("condensationprogress", this.condensationprogress);
		NBTTagCompound fluidTankTag = new NBTTagCompound();
		this.distilledwaterTank.writeToNBT(fluidTankTag);
		nbttagcompound.setTag("distilledwaterTank", fluidTankTag);
		NBTTagCompound SteamTankTag = new NBTTagCompound();
		this.SteamTank.writeToNBT(fluidTankTag);
		nbttagcompound.setTag("SteamTank", SteamTankTag);
	}

	@Override
	public ContainerBase<TileEntitySteamKineticGenerator> getGuiContainer(EntityPlayer entityPlayer)
	{
		return new ContainerSteamKineticGenerator(entityPlayer, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getGui(EntityPlayer entityPlayer, boolean isAdmin)
	{
		return new GuSteamKineticGenerator(new ContainerSteamKineticGenerator(entityPlayer, this));
	}

	public boolean facingMatchesDirection(ForgeDirection direction)
	{
		return direction.ordinal() == this.getFacing();
	}

	@Override
	public boolean wrenchCanSetFacing(EntityPlayer entityPlayer, int side)
	{
		return side != 0 && side != 1 && this.getFacing() != side;
	}

	@Override
	public void setFacing(short side)
	{
		super.setFacing(side);
	}

	@Override
	public int maxrequestkineticenergyTick(ForgeDirection directionFrom)
	{
		return this.facingMatchesDirection(directionFrom) ? this.kUoutput : 0;
	}

	@Override
	public int requestkineticenergy(ForgeDirection directionFrom, int requestkineticenergy)
	{
		return this.facingMatchesDirection(directionFrom) ? this.kUoutput : 0;
	}

	@Override
	public String getInventoryName()
	{
		return "Steam Kinetic Generator";
	}

	@Override
	public void onGuiClosed(EntityPlayer entityPlayer)
	{
	}

	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill)
	{
		if (!this.canFill(from, resource.getFluid()))
			return 0;
		if (resource.getFluid() != BlocksItems.getFluid(InternalName.fluidSteam) && resource.getFluid() != BlocksItems.getFluid(InternalName.fluidSuperheatedSteam))
		{
			if (resource.getFluid() != FluidRegistry.WATER && resource.getFluid() != BlocksItems.getFluid(InternalName.fluidDistilledWater))
				return 0;
			if (this.distilledwaterTank.getFluid() != null && this.distilledwaterTank.getFluid().getFluid() != resource.getFluid())
				this.distilledwaterTank.drain(this.distilledwaterTank.getFluidAmount(), true);

			return this.distilledwaterTank.fill(resource, doFill);
		}
		if (this.SteamTank.getFluid() != null && this.SteamTank.getFluid().getFluid() != resource.getFluid())
			this.SteamTank.drain(this.SteamTank.getFluidAmount(), true);

		return this.SteamTank.fill(resource, doFill);
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
	{
		return resource != null && resource.isFluidEqual(this.distilledwaterTank.getFluid()) ? !this.canDrain(from, resource.getFluid()) ? null : this.distilledwaterTank.drain(resource.amount, doDrain) : null;
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
	{
		return this.distilledwaterTank.drain(maxDrain, doDrain);
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid)
	{
		return from.getOpposite().ordinal() != this.getFacing() && from.ordinal() != this.getFacing() && (fluid != BlocksItems.getFluid(InternalName.fluidSteam) && fluid != BlocksItems.getFluid(InternalName.fluidSuperheatedSteam) ? (fluid == FluidRegistry.WATER || fluid == BlocksItems.getFluid(InternalName.fluidDistilledWater)) && this.distilledwaterTank.getFluidAmount() < this.distilledwaterTank.getCapacity() : this.SteamTank.getFluidAmount() < this.SteamTank.getCapacity());
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid)
	{
		if (from.getOpposite().ordinal() != this.getFacing() && from.ordinal() != this.getFacing())
		{
			FluidStack fs = this.distilledwaterTank.getFluid();
			return fs != null && fs.getFluid() == fluid;
		}
		return false;
	}

	public int gaugeLiquidScaled(int i, int tank)
	{
		if (tank == 0)
		{
			int fluidAmount = this.distilledwaterTank.getFluidAmount();
			if (fluidAmount > 0)
				return fluidAmount * i / this.distilledwaterTank.getCapacity();
		}
		return 0;
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from)
	{
		return new FluidTankInfo[] { this.distilledwaterTank.getInfo(), this.SteamTank.getInfo() };
	}

	@Override
	public double getEnergy()
	{
		return 0.0D;
	}

	@Override
	public boolean useEnergy(double amount)
	{
		return false;
	}

	public int getdistilledwaterTank()
	{
		return this.distilledwaterTank.getFluidAmount();
	}

	public FluidTank getTank()
	{
		return this.distilledwaterTank;
	}

	public boolean isHotSteam()
	{
		return this.SteamTank.getFluid() != null && this.SteamTank.getFluid().getFluid() == BlocksItems.getFluid(InternalName.fluidSuperheatedSteam);
	}

	public boolean isturbine()
	{
		return !this.turbineSlot.isEmpty();
	}

	public boolean isturbefilledupwithwater()
	{
		return this.isturbefilledupwithwater;
	}

	public int getTickRate()
	{
		return 20;
	}

	@Override
	public Set<UpgradableProperty> getUpgradableProperties()
	{
		return EnumSet.of(UpgradableProperty.ItemConsuming, UpgradableProperty.FluidConsuming, UpgradableProperty.FluidProducing);
	}
}
