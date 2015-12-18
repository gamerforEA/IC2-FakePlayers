package ic2.core.block.machine.tileentity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.Direction;
import ic2.api.energy.tile.IHeatSource;
import ic2.api.network.INetworkClientTileEntityEventListener;
import ic2.core.ContainerBase;
import ic2.core.ExplosionIC2;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.machine.container.ContainerSteamGenerator;
import ic2.core.block.machine.gui.GuiSteamGenerator;
import ic2.core.init.BlocksItems;
import ic2.core.init.InternalName;
import ic2.core.util.BiomUtil;
import ic2.core.util.LiquidUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

public class TileEntitySteamGenerator extends TileEntityInventory implements IHasGui, IFluidHandler, INetworkClientTileEntityEventListener
{
	private final int maxcalcification = 100000;
	private int calcification = 0;
	private int outputtyp;
	private final float maxsystemheat = 500.0F;
	private float systemheat;
	private int pressurevalve = 0;
	private int outputmb = 0;
	private int inputmb = 0;
	public FluidTank WaterTank = new FluidTank(10000);
	private int heatinput;
	private boolean newActive = false;

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound)
	{
		super.readFromNBT(nbttagcompound);
		this.WaterTank.readFromNBT(nbttagcompound.getCompoundTag("WaterTank"));
		this.inputmb = nbttagcompound.getInteger("inputmb");
		this.pressurevalve = nbttagcompound.getInteger("pressurevalve");
		this.systemheat = nbttagcompound.getFloat("systemheat");
		this.calcification = nbttagcompound.getInteger("calcification");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound)
	{
		super.writeToNBT(nbttagcompound);
		NBTTagCompound inputTankTag = new NBTTagCompound();
		this.WaterTank.writeToNBT(inputTankTag);
		nbttagcompound.setTag("WaterTank", inputTankTag);
		nbttagcompound.setInteger("inputmb", this.inputmb);
		nbttagcompound.setInteger("pressurevalve", this.pressurevalve);
		nbttagcompound.setFloat("systemheat", this.systemheat);
		nbttagcompound.setInteger("calcification", this.calcification);
	}

	@Override
	protected void updateEntityServer()
	{
		super.updateEntityServer();
		if (this.systemheat < BiomUtil.gerBiomTemperature(this.worldObj, this.xCoord, this.zCoord))
			this.systemheat = BiomUtil.gerBiomTemperature(this.worldObj, this.xCoord, this.zCoord);

		if (!this.iscalcified())
		{
			this.newActive = this.work();
			if (this.getActive() != this.newActive)
				this.setActive(this.newActive);
		}
		else if (this.getActive())
			this.setActive(false);

		if (!this.getActive())
			this.cooldown(0.01F);

	}

	private boolean work()
	{
		if (this.WaterTank.getFluidAmount() > 0 && this.inputmb > 0)
		{
			FluidStack outputfluid = this.getOutputfluid();
			if (outputfluid != null)
			{
				this.outputmb = outputfluid.amount;
				this.outputtyp = this.getoutputtyp(outputfluid);
				int amount = LiquidUtil.distribute(this, outputfluid, false);
				outputfluid.amount -= amount;
				if (outputfluid.amount > 0)
					if ((this.outputtyp == 2 || this.outputtyp == 3) && IC2.random.nextInt(10) == 0)
					{
						ExplosionIC2 explosion = new ExplosionIC2(this.worldObj, (Entity) null, this.xCoord, this.yCoord, this.zCoord, 1.0F, 1.0F, ExplosionIC2.Type.Heat);

						// TODO gamerforEA code start
						explosion.fake.profile = this.fake.profile;
						// TODO gamerforEA code end

						explosion.doExplosion();
					}
					else
						this.WaterTank.fill(outputfluid, true);

				return true;
			}
		}

		this.outputmb = 0;
		this.outputtyp = -1;
		this.heatinput = 0;
		return this.heatupmax();
	}

	private boolean heatupmax()
	{
		this.heatinput = this.requestHeat(1200);
		if (this.heatinput > 0)
		{
			this.heatup(this.heatinput);
			return true;
		}
		else
			return false;
	}

	private int getoutputtyp(FluidStack fluid)
	{
		return fluid.getFluid().equals(BlocksItems.getFluid(InternalName.fluidSuperheatedSteam)) ? 3 : fluid.getFluid().equals(BlocksItems.getFluid(InternalName.fluidSteam)) ? 2 : fluid.getFluid().equals(BlocksItems.getFluid(InternalName.fluidDistilledWater)) ? 1 : fluid.getFluid().equals(FluidRegistry.WATER) ? 0 : -1;
	}

	private FluidStack getOutputfluid()
	{
		boolean cancalcification = true;
		if (this.WaterTank.getFluid() == null)
			return null;
		else
		{
			Fluid fluidintank = this.WaterTank.getFluid().getFluid();
			if (fluidintank.equals(BlocksItems.getFluid(InternalName.fluidDistilledWater)))
				cancalcification = false;

			if (this.systemheat < 100.0F)
			{
				this.heatupmax();
				return this.WaterTank.drain(this.inputmb, true);
			}
			else
			{
				int hu_need = 100 + Math.round(this.pressurevalve / 220.0F * 100.0F);
				int TargetTemp = (int) (100L + Math.round(this.pressurevalve / 220.0F * 100.0F * 2.74D));
				if (Math.round(this.systemheat * 10.0F) / 10.0F == TargetTemp)
				{
					int heat = this.requestHeat(this.inputmb * hu_need);
					this.heatinput = heat;
					if (heat == this.inputmb * hu_need)
					{
						if (this.systemheat >= 374.0F)
						{
							if (cancalcification)
								++this.calcification;

							this.WaterTank.drain(this.inputmb, true);
							return new FluidStack(BlocksItems.getFluid(InternalName.fluidSuperheatedSteam), this.inputmb * 100);
						}
						else
						{
							if (cancalcification)
								++this.calcification;

							this.WaterTank.drain(this.inputmb, true);
							return new FluidStack(BlocksItems.getFluid(InternalName.fluidSteam), this.inputmb * 100);
						}
					}
					else
					{
						this.heatup(heat);
						return this.WaterTank.drain(this.inputmb, true);
					}
				}
				else if (this.systemheat <= TargetTemp)
				{
					this.heatupmax();
					return this.WaterTank.drain(this.inputmb, true);
				}
				else
				{
					this.heatinput = 0;
					int count = this.inputmb;

					while (this.systemheat > TargetTemp)
					{
						this.cooldown(0.1F);
						if (cancalcification)
							++this.calcification;

						--count;
						if (count == 0)
							break;
					}

					this.WaterTank.drain(this.inputmb - count, true);
					return new FluidStack(BlocksItems.getFluid(InternalName.fluidSteam), (this.inputmb - count) * 100);
				}
			}
		}
	}

	private void heatup(int heatinput)
	{
		this.systemheat += heatinput * 5.0E-4F;
		if (this.systemheat > this.maxsystemheat)
		{
			this.worldObj.setBlockToAir(this.xCoord, this.yCoord, this.zCoord);
			ExplosionIC2 explosion = new ExplosionIC2(this.worldObj, (Entity) null, this.xCoord, this.yCoord, this.zCoord, 10.0F, 0.01F, ExplosionIC2.Type.Heat);

			// TODO gamerforEA code start
			explosion.fake.profile = this.fake.profile;
			// TODO gamerforEA code end

			explosion.doExplosion();
		}

	}

	private void cooldown(float cool)
	{
		if (this.systemheat > BiomUtil.gerBiomTemperature(this.worldObj, this.xCoord, this.zCoord))
			this.systemheat -= cool;

	}

	private int requestHeat(int requestHeat)
	{
		int requestHeat_temp = requestHeat;

		for (Direction direction : Direction.directions)
		{
			TileEntity target = direction.applyToTileEntity(this);
			if (target instanceof IHeatSource)
			{
				int amount = ((IHeatSource) target).requestHeat(direction.toForgeDirection().getOpposite(), requestHeat_temp);
				if (amount > 0)
					requestHeat_temp -= amount;

				if (requestHeat_temp == 0)
					return requestHeat;
			}
		}

		return requestHeat - requestHeat_temp;
	}

	@Override
	public void onNetworkEvent(EntityPlayer player, int event)
	{
		if (event <= 2000 && event >= -2000)
		{
			this.inputmb += event;
			if (this.inputmb > 1000)
				this.inputmb = 1000;

			if (this.inputmb < 0)
				this.inputmb = 0;
		}
		else
		{
			if (event > 2000)
				this.pressurevalve += event - 2000;

			if (event < -2000)
				this.pressurevalve += event + 2000;

			if (this.pressurevalve > 300)
				this.pressurevalve = 300;

			if (this.pressurevalve < 0)
				this.pressurevalve = 0;
		}

	}

	public int gaugeHeatScaled(int i)
	{
		return (int) (i * this.systemheat / this.maxsystemheat);
	}

	public int gaugecalcificationScaled(int i)
	{
		return i * this.calcification / this.maxcalcification;
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
	{
		return null;
	}

	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill)
	{
		if (!this.canFill(from, resource.getFluid()))
			return 0;
		else
		{
			int amount = this.WaterTank.fill(resource, doFill);
			return amount;
		}
	}

	public int gaugeLiquidScaled(int i, int tank)
	{
		switch (tank)
		{
			case 0:
				if (this.WaterTank.getFluidAmount() <= 0)
					return 0;

				return this.WaterTank.getFluidAmount() * i / this.WaterTank.getCapacity();
			default:
				return 0;
		}
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
	{
		return null;
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid)
	{
		return false;
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid)
	{
		return fluid.equals(BlocksItems.getFluid(InternalName.fluidDistilledWater)) || fluid.equals(FluidRegistry.WATER);
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from)
	{
		return new FluidTankInfo[] { this.WaterTank.getInfo() };
	}

	@Override
	public ContainerBase<TileEntitySteamGenerator> getGuiContainer(EntityPlayer entityPlayer)
	{
		return new ContainerSteamGenerator(entityPlayer, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getGui(EntityPlayer entityPlayer, boolean isAdmin)
	{
		return new GuiSteamGenerator(new ContainerSteamGenerator(entityPlayer, this));
	}

	@Override
	public void onGuiClosed(EntityPlayer entityPlayer)
	{
	}

	@Override
	public String getInventoryName()
	{
		return "Steam Generator";
	}

	public int getoutputmb()
	{
		return this.outputmb;
	}

	public int getinputmb()
	{
		return this.inputmb;
	}

	public int getheatinput()
	{
		return this.heatinput;
	}

	public int getpressurevalve()
	{
		return this.pressurevalve;
	}

	public float getsystemheat()
	{
		return Math.round(this.systemheat * 10.0F) / 10.0F;
	}

	public float getcalcification()
	{
		return Math.round((float) this.calcification / (float) this.maxcalcification * 100.0F * 100.0F) / 100.0F;
	}

	private boolean iscalcified()
	{
		return this.calcification >= this.maxcalcification;
	}

	public String gtoutputfluid()
	{
		switch (this.outputtyp)
		{
			case 0:
				return StatCollector.translateToLocal("ic2.SteamGenerator.output.water");
			case 1:
				return StatCollector.translateToLocal("ic2.SteamGenerator.output.destiwater");
			case 2:
				return StatCollector.translateToLocal("ic2.SteamGenerator.output.steam");
			case 3:
				return StatCollector.translateToLocal("ic2.SteamGenerator.output.hotsteam");
			default:
				return "";
		}
	}
}
