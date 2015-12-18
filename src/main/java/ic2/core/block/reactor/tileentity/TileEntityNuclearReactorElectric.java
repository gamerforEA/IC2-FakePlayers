package ic2.core.block.reactor.tileentity;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.logging.log4j.Level;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.Direction;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.energy.tile.IMetaDelegate;
import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorComponent;
import ic2.api.recipe.RecipeOutput;
import ic2.core.ContainerBase;
import ic2.core.ExplosionIC2;
import ic2.core.IC2;
import ic2.core.IC2DamageSource;
import ic2.core.IHasGui;
import ic2.core.Ic2Items;
import ic2.core.audio.AudioSource;
import ic2.core.audio.PositionSpec;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.generator.block.BlockGenerator;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotConsumableLiquidByList;
import ic2.core.block.invslot.InvSlotConsumableLiquidByTank;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotReactor;
import ic2.core.block.reactor.block.BlockReactorAccessHatch;
import ic2.core.block.reactor.block.BlockReactorChamber;
import ic2.core.block.reactor.block.BlockReactorFluidPort;
import ic2.core.block.reactor.block.BlockReactorRedstonePort;
import ic2.core.block.reactor.block.BlockReactorVessel;
import ic2.core.block.reactor.container.ContainerNuclearReactor;
import ic2.core.block.reactor.gui.GuiNuclearReactor;
import ic2.core.init.BlocksItems;
import ic2.core.init.InternalName;
import ic2.core.init.MainConfig;
import ic2.core.item.reactor.ItemReactorHeatStorage;
import ic2.core.util.ConfigUtil;
import ic2.core.util.LogCategory;
import ic2.core.util.Util;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.material.Material;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

public class TileEntityNuclearReactorElectric extends TileEntityInventory implements IHasGui, IReactor, IEnergySource, IMetaDelegate, IFluidHandler
{
	public float output = 0.0F;
	public int updateTicker = IC2.random.nextInt(this.getTickRate());
	public int heat = 0;
	public int maxHeat = 10000;
	public float hem = 1.0F;
	private int EmitHeatbuffer = 0;
	public int EmitHeat = 0;
	private boolean redstone = false;
	private boolean fluidcoolreactor = false;
	public AudioSource audioSourceMain;
	public AudioSource audioSourceGeiger;
	private float lastOutput = 0.0F;
	public Block[][][] surroundings = new Block[5][5][5];
	public final FluidTank inputTank = new FluidTank(10000);
	public final FluidTank outputTank = new FluidTank(10000);
	private List<TileEntity> subTiles;
	public final InvSlotReactor reactorSlot = new InvSlotReactor(this, "reactor", 0, 54);
	public final InvSlotOutput coolantoutputSlot;
	public final InvSlotOutput hotcoolantoutputSlot;
	public final InvSlotConsumableLiquidByList coolantinputSlot = new InvSlotConsumableLiquidByList(this, "coolantinputSlot", 55, InvSlot.Access.I, 1, InvSlot.InvSide.ANY, InvSlotConsumableLiquid.OpType.Drain, new Fluid[] { BlocksItems.getFluid(InternalName.fluidCoolant) });
	public final InvSlotConsumableLiquidByTank hotcoolinputSlot;
	public boolean addedToEnergyNet = false;
	private static final float huOutputModifier = 2.0F * ConfigUtil.getFloat(MainConfig.get(), "balance/energy/FluidReactor/outputModifier");

	public TileEntityNuclearReactorElectric()
	{
		this.hotcoolinputSlot = new InvSlotConsumableLiquidByTank(this, "hotcoolinputSlot", 56, InvSlot.Access.I, 1, InvSlot.InvSide.ANY, InvSlotConsumableLiquid.OpType.Fill, this.outputTank);
		this.coolantoutputSlot = new InvSlotOutput(this, "coolantoutputSlot", 57, 1);
		this.hotcoolantoutputSlot = new InvSlotOutput(this, "hotcoolantoutputSlot", 58, 1);
	}

	@Override
	public void onLoaded()
	{
		super.onLoaded();
		if (IC2.platform.isSimulating() && !this.isFluidCooled())
		{
			MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
			this.addedToEnergyNet = true;
		}

	}

	@Override
	public void onUnloaded()
	{
		if (IC2.platform.isRendering())
		{
			IC2.audioManager.removeSources(this);
			this.audioSourceMain = null;
			this.audioSourceGeiger = null;
		}

		if (IC2.platform.isSimulating() && this.addedToEnergyNet)
		{
			MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
			this.addedToEnergyNet = false;
		}

		super.onUnloaded();
	}

	@Override
	public String getInventoryName()
	{
		return "Nuclear Reactor";
	}

	public int gaugeHeatScaled(int i)
	{
		return i * this.heat / (this.maxHeat / 100 * 85);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound)
	{
		super.readFromNBT(nbttagcompound);
		this.heat = nbttagcompound.getInteger("heat");
		this.inputTank.readFromNBT(nbttagcompound.getCompoundTag("inputTank"));
		this.outputTank.readFromNBT(nbttagcompound.getCompoundTag("outputTank"));
		this.output = nbttagcompound.getShort("output");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound)
	{
		super.writeToNBT(nbttagcompound);
		NBTTagCompound inputTankTag = new NBTTagCompound();
		this.inputTank.writeToNBT(inputTankTag);
		nbttagcompound.setTag("inputTank", inputTankTag);
		NBTTagCompound outputTankTag = new NBTTagCompound();
		this.outputTank.writeToNBT(outputTankTag);
		nbttagcompound.setTag("outputTank", outputTankTag);
		nbttagcompound.setInteger("heat", this.heat);
		nbttagcompound.setShort("output", (short) (int) this.getReactorEnergyOutput());
	}

	@Override
	public void setRedstoneSignal(boolean redstone)
	{
		this.redstone = redstone;
	}

	@Override
	public void drawEnergy(double amount)
	{
	}

	public float sendEnergy(float send)
	{
		return 0.0F;
	}

	@Override
	public boolean emitsEnergyTo(TileEntity receiver, ForgeDirection direction)
	{
		return true;
	}

	@Override
	public double getOfferedEnergy()
	{
		return this.getReactorEnergyOutput() * 5.0F * ConfigUtil.getFloat(MainConfig.get(), "balance/energy/generator/nuclear");
	}

	@Override
	public int getSourceTier()
	{
		return 4;
	}

	@Override
	public double getReactorEUEnergyOutput()
	{
		return this.getOfferedEnergy();
	}

	@Override
	public List<TileEntity> getSubTiles()
	{
		if (this.subTiles == null)
		{
			this.subTiles = new ArrayList();
			this.subTiles.add(this);

			for (Direction dir : Direction.directions)
			{
				TileEntity te = dir.applyToTileEntity(this);
				if (te instanceof TileEntityReactorChamberElectric && !te.isInvalid())
					this.subTiles.add(te);
			}
		}

		return this.subTiles;
	}

	private void processfluidsSlots()
	{
		RecipeOutput outputinputSlot = this.processInputSlot(true);
		if (outputinputSlot != null)
		{
			this.processInputSlot(false);
			List<ItemStack> processResult = outputinputSlot.items;
			this.coolantoutputSlot.add(processResult);
		}

		RecipeOutput outputoutputSlot = this.processOutputSlot(true);
		if (outputoutputSlot != null)
		{
			this.processOutputSlot(false);
			List<ItemStack> processResult = outputoutputSlot.items;
			this.hotcoolantoutputSlot.add(processResult);
		}

	}

	public void refreshChambers()
	{
		if (this.addedToEnergyNet)
			MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));

		this.subTiles = null;
		if (this.addedToEnergyNet)
			MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));

	}

	@Override
	protected void updateEntityServer()
	{
		super.updateEntityServer();
		if (this.updateTicker++ % this.getTickRate() == 0)
		{
			if (!this.worldObj.doChunksNearChunkExist(this.xCoord, this.yCoord, this.zCoord, 2))
				this.output = 0.0F;
			else
			{
				if (this.getReactorSize() == 9)
				{
					boolean fluidcoolreactor_new = this.readyforpressurizedreactor();
					if (this.fluidcoolreactor != fluidcoolreactor_new)
					{
						if (fluidcoolreactor_new)
						{
							if (IC2.platform.isSimulating() && this.addedToEnergyNet)
							{
								MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
								this.addedToEnergyNet = false;
							}

							this.movefluidinWorld(false);
						}
						else
						{
							if (IC2.platform.isSimulating() && !this.fluidcoolreactor)
							{
								MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
								this.addedToEnergyNet = true;
							}

							this.movefluidinWorld(true);
						}

						this.fluidcoolreactor = fluidcoolreactor_new;
					}
				}

				this.dropAllUnfittingStuff();
				this.output = 0.0F;
				this.maxHeat = 10000;
				this.hem = 1.0F;
				this.processChambers();
				if (this.fluidcoolreactor)
				{
					this.processfluidsSlots();
					int huOtput = (int) (huOutputModifier * this.EmitHeatbuffer);
					int outputroom = this.outputTank.getCapacity() - this.outputTank.getFluidAmount();
					if (outputroom > 0)
					{
						FluidStack draincoolant;
						if (huOtput < outputroom)
							draincoolant = this.inputTank.drain(huOtput, false);
						else
							draincoolant = this.inputTank.drain(outputroom, false);

						if (draincoolant != null)
						{
							this.EmitHeat = draincoolant.amount;
							huOtput -= this.inputTank.drain(draincoolant.amount, true).amount;
							this.outputTank.fill(new FluidStack(BlocksItems.getFluid(InternalName.fluidHotCoolant), draincoolant.amount), true);
						}
						else
							this.EmitHeat = 0;
					}
					else
						this.EmitHeat = 0;

					this.addHeat(huOtput / 2);
				}

				this.EmitHeatbuffer = 0;
				if (this.calculateHeatEffects())
					return;

				this.setActive(this.heat >= 1000 || this.output > 0.0F);
				this.markDirty();
			}

			IC2.network.get().updateTileEntityField(this, "output");
		}
	}

	public void dropAllUnfittingStuff()
	{
		for (int i = 0; i < this.reactorSlot.size(); ++i)
		{
			ItemStack stack = this.reactorSlot.get(i);
			if (stack != null && !this.isUsefulItem(stack, false))
			{
				this.reactorSlot.put(i, (ItemStack) null);
				this.eject(stack);
			}
		}

		for (int i = this.reactorSlot.size(); i < this.reactorSlot.rawSize(); ++i)
		{
			ItemStack stack = this.reactorSlot.get(i);
			this.reactorSlot.put(i, (ItemStack) null);
			this.eject(stack);
		}

	}

	public boolean isUsefulItem(ItemStack stack, boolean forInsertion)
	{
		Item item = stack.getItem();
		return item == null ? false : forInsertion && this.fluidcoolreactor && item.getClass() == ItemReactorHeatStorage.class && ((ItemReactorHeatStorage) item).getCustomDamage(stack) > 0 ? false : item instanceof IReactorComponent ? true : item == Ic2Items.TritiumCell.getItem() || item == Ic2Items.reactorDepletedUraniumSimple.getItem() || item == Ic2Items.reactorDepletedUraniumDual.getItem() || item == Ic2Items.reactorDepletedUraniumQuad.getItem() || item == Ic2Items.reactorDepletedMOXSimple.getItem() || item == Ic2Items.reactorDepletedMOXDual.getItem() || item == Ic2Items.reactorDepletedMOXQuad.getItem();
	}

	public void eject(ItemStack drop)
	{
		if (IC2.platform.isSimulating() && drop != null)
		{
			float f = 0.7F;
			double d = this.worldObj.rand.nextFloat() * f + (1.0F - f) * 0.5D;
			double d1 = this.worldObj.rand.nextFloat() * f + (1.0F - f) * 0.5D;
			double d2 = this.worldObj.rand.nextFloat() * f + (1.0F - f) * 0.5D;
			EntityItem entityitem = new EntityItem(this.worldObj, this.xCoord + d, this.yCoord + d1, this.zCoord + d2, drop);
			entityitem.delayBeforeCanPickup = 10;
			this.worldObj.spawnEntityInWorld(entityitem);
		}
	}

	public boolean calculateHeatEffects()
	{
		if (this.heat >= 4000 && IC2.platform.isSimulating() && ConfigUtil.getFloat(MainConfig.get(), "protection/reactorExplosionPowerLimit") > 0.0F)
		{
			float power = (float) this.heat / (float) this.maxHeat;
			if (power >= 1.0F)
			{
				this.explode();
				return true;
			}
			else
			{
				if (power >= 0.85F && this.worldObj.rand.nextFloat() <= 0.2F * this.hem)
				{
					int[] coord = this.getRandCoord(2);
					if (coord != null)
					{
						Block block = this.worldObj.getBlock(coord[0], coord[1], coord[2]);
						if (block.isAir(this.worldObj, coord[0], coord[1], coord[2]))
							this.worldObj.setBlock(coord[0], coord[1], coord[2], Blocks.fire, 0, 7);
						else if (block.getBlockHardness(this.worldObj, coord[0], coord[1], coord[2]) >= 0.0F && this.worldObj.getTileEntity(coord[0], coord[1], coord[2]) == null)
						{
							Material mat = block.getMaterial();
							if (mat != Material.rock && mat != Material.iron && mat != Material.lava && mat != Material.ground && mat != Material.clay)
								this.worldObj.setBlock(coord[0], coord[1], coord[2], Blocks.fire, 0, 7);
							else
								this.worldObj.setBlock(coord[0], coord[1], coord[2], Blocks.flowing_lava, 15, 7);
						}
					}
				}

				if (power >= 0.7F)
				{
					List list1 = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, AxisAlignedBB.getBoundingBox(this.xCoord - 3, this.yCoord - 3, this.zCoord - 3, this.xCoord + 4, this.yCoord + 4, this.zCoord + 4));

					for (int l = 0; l < list1.size(); ++l)
					{
						Entity ent = (Entity) list1.get(l);
						ent.attackEntityFrom(IC2DamageSource.radiation, (int) (this.worldObj.rand.nextInt(4) * this.hem));
					}
				}

				if (power >= 0.5F && this.worldObj.rand.nextFloat() <= this.hem)
				{
					int[] coord = this.getRandCoord(2);
					if (coord != null)
					{
						Block block = this.worldObj.getBlock(coord[0], coord[1], coord[2]);
						if (block.getMaterial() == Material.water)
							this.worldObj.setBlockToAir(coord[0], coord[1], coord[2]);
					}
				}

				if (power >= 0.4F && this.worldObj.rand.nextFloat() <= this.hem)
				{
					int[] coord = this.getRandCoord(2);
					if (coord != null && this.worldObj.getTileEntity(coord[0], coord[1], coord[2]) == null)
					{
						Block block = this.worldObj.getBlock(coord[0], coord[1], coord[2]);
						Material mat = block.getMaterial();
						if (mat == Material.wood || mat == Material.leaves || mat == Material.cloth)
							this.worldObj.setBlock(coord[0], coord[1], coord[2], Blocks.fire, 0, 7);
					}
				}

				return false;
			}
		}
		else
			return false;
	}

	public int[] getRandCoord(int radius)
	{
		if (radius <= 0)
			return null;
		else
		{
			int[] c = new int[] { this.xCoord + this.worldObj.rand.nextInt(2 * radius + 1) - radius, this.yCoord + this.worldObj.rand.nextInt(2 * radius + 1) - radius, this.zCoord + this.worldObj.rand.nextInt(2 * radius + 1) - radius };
			return c[0] == this.xCoord && c[1] == this.yCoord && c[2] == this.zCoord ? null : c;
		}
	}

	public void processChambers()
	{
		int size = this.getReactorSize();

		for (int pass = 0; pass < 2; ++pass)
			for (int y = 0; y < 6; ++y)
				for (int x = 0; x < size; ++x)
				{
					ItemStack stack = this.reactorSlot.get(x, y);
					if (stack != null && stack.getItem() instanceof IReactorComponent)
					{
						IReactorComponent comp = (IReactorComponent) stack.getItem();
						comp.processChamber(this, stack, x, y, pass == 0);
					}
				}

	}

	@Override
	public boolean produceEnergy()
	{
		return this.receiveredstone() && ConfigUtil.getFloat(MainConfig.get(), "balance/energy/generator/generator") > 0.0F;
	}

	public boolean receiveredstone()
	{
		return this.worldObj.isBlockIndirectlyGettingPowered(this.xCoord, this.yCoord, this.zCoord) || this.redstone;
	}

	public short getReactorSize()
	{
		if (this.worldObj == null)
			return (short) 9;
		else
		{
			short cols = 3;

			for (Direction direction : Direction.directions)
			{
				TileEntity target = direction.applyToTileEntity(this);
				if (target instanceof TileEntityReactorChamberElectric)
					++cols;
			}

			return cols;
		}
	}

	@Override
	public int getTickRate()
	{
		return 20;
	}

	@Override
	public ContainerBase<TileEntityNuclearReactorElectric> getGuiContainer(EntityPlayer entityPlayer)
	{
		return new ContainerNuclearReactor(entityPlayer, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getGui(EntityPlayer entityPlayer, boolean isAdmin)
	{
		return new GuiNuclearReactor(new ContainerNuclearReactor(entityPlayer, this));
	}

	@Override
	public void onGuiClosed(EntityPlayer entityPlayer)
	{
	}

	@Override
	public void onNetworkUpdate(String field)
	{
		if (field.equals("output"))
		{
			if (this.output > 0.0F)
			{
				if (this.lastOutput <= 0.0F)
				{
					if (this.audioSourceMain == null)
						this.audioSourceMain = IC2.audioManager.createSource(this, PositionSpec.Center, "Generators/NuclearReactor/NuclearReactorLoop.ogg", true, false, IC2.audioManager.getDefaultVolume());

					if (this.audioSourceMain != null)
						this.audioSourceMain.play();
				}

				if (this.output < 40.0F)
				{
					if (this.lastOutput <= 0.0F || this.lastOutput >= 40.0F)
					{
						if (this.audioSourceGeiger != null)
							this.audioSourceGeiger.remove();

						this.audioSourceGeiger = IC2.audioManager.createSource(this, PositionSpec.Center, "Generators/NuclearReactor/GeigerLowEU.ogg", true, false, IC2.audioManager.getDefaultVolume());
						if (this.audioSourceGeiger != null)
							this.audioSourceGeiger.play();
					}
				}
				else if (this.output < 80.0F)
				{
					if (this.lastOutput < 40.0F || this.lastOutput >= 80.0F)
					{
						if (this.audioSourceGeiger != null)
							this.audioSourceGeiger.remove();

						this.audioSourceGeiger = IC2.audioManager.createSource(this, PositionSpec.Center, "Generators/NuclearReactor/GeigerMedEU.ogg", true, false, IC2.audioManager.getDefaultVolume());
						if (this.audioSourceGeiger != null)
							this.audioSourceGeiger.play();
					}
				}
				else if (this.output >= 80.0F && this.lastOutput < 80.0F)
				{
					if (this.audioSourceGeiger != null)
						this.audioSourceGeiger.remove();

					this.audioSourceGeiger = IC2.audioManager.createSource(this, PositionSpec.Center, "Generators/NuclearReactor/GeigerHighEU.ogg", true, false, IC2.audioManager.getDefaultVolume());
					if (this.audioSourceGeiger != null)
						this.audioSourceGeiger.play();
				}
			}
			else if (this.lastOutput > 0.0F)
			{
				if (this.audioSourceMain != null)
					this.audioSourceMain.stop();

				if (this.audioSourceGeiger != null)
					this.audioSourceGeiger.stop();
			}

			this.lastOutput = this.output;
		}

		super.onNetworkUpdate(field);
	}

	@Override
	public float getWrenchDropRate()
	{
		return 0.8F;
	}

	@Override
	public ChunkCoordinates getPosition()
	{
		return new ChunkCoordinates(this.xCoord, this.yCoord, this.zCoord);
	}

	@Override
	public World getWorld()
	{
		return this.worldObj;
	}

	@Override
	public int getHeat()
	{
		return this.heat;
	}

	@Override
	public void setHeat(int heat1)
	{
		this.heat = heat1;
	}

	@Override
	public int addHeat(int amount)
	{
		this.heat += amount;
		return this.heat;
	}

	@Override
	public ItemStack getItemAt(int x, int y)
	{
		return x >= 0 && x < this.getReactorSize() && y >= 0 && y < 6 ? this.reactorSlot.get(x, y) : null;
	}

	@Override
	public void setItemAt(int x, int y, ItemStack item)
	{
		if (x >= 0 && x < this.getReactorSize() && y >= 0 && y < 6)
			this.reactorSlot.put(x, y, item);
	}

	@Override
	public void explode()
	{
		float boomPower = 10.0F;
		float boomMod = 1.0F;

		for (int i = 0; i < this.reactorSlot.size(); ++i)
		{
			ItemStack stack = this.reactorSlot.get(i);
			if (stack != null && stack.getItem() instanceof IReactorComponent)
			{
				float f = ((IReactorComponent) stack.getItem()).influenceExplosion(this, stack);
				if (f > 0.0F && f < 1.0F)
					boomMod *= f;
				else
					boomPower += f;
			}

			this.reactorSlot.put(i, (ItemStack) null);
		}

		boomPower = boomPower * this.hem * boomMod;
		IC2.log.log(LogCategory.PlayerActivity, Level.INFO, "Nuclear Reactor at %s melted (raw explosion power %f)", new Object[] { Util.formatPosition(this), Float.valueOf(boomPower) });
		boomPower = Math.min(boomPower, ConfigUtil.getFloat(MainConfig.get(), "protection/reactorExplosionPowerLimit"));

		for (Direction direction : Direction.directions)
		{
			TileEntity target = direction.applyToTileEntity(this);
			if (target instanceof TileEntityReactorChamberElectric)
				this.worldObj.setBlockToAir(target.xCoord, target.yCoord, target.zCoord);
		}

		this.worldObj.setBlockToAir(this.xCoord, this.yCoord, this.zCoord);
		ExplosionIC2 explosion = new ExplosionIC2(this.worldObj, (Entity) null, this.xCoord, this.yCoord, this.zCoord, boomPower, 0.01F, ExplosionIC2.Type.Nuclear);

		// TODO gamerforEA code start
		explosion.fake.profile = this.fake.profile;
		// TODO gamerforEA code end

		explosion.doExplosion();
	}

	@Override
	public void addEmitHeat(int heat)
	{
		this.EmitHeatbuffer += heat;
	}

	@Override
	public int getMaxHeat()
	{
		return this.maxHeat;
	}

	@Override
	public void setMaxHeat(int newMaxHeat)
	{
		this.maxHeat = newMaxHeat;
	}

	@Override
	public float getHeatEffectModifier()
	{
		return this.hem;
	}

	@Override
	public void setHeatEffectModifier(float newHEM)
	{
		this.hem = newHEM;
	}

	@Override
	public float getReactorEnergyOutput()
	{
		return this.output;
	}

	@Override
	public float addOutput(float energy)
	{
		return this.output += energy;
	}

	private RecipeOutput processInputSlot(boolean simulate)
	{
		if (!this.coolantinputSlot.isEmpty())
		{
			MutableObject<ItemStack> output = new MutableObject();
			if (this.coolantinputSlot.transferToTank(this.inputTank, output, simulate) && (output.getValue() == null || this.coolantoutputSlot.canAdd(output.getValue())))
			{
				if (output.getValue() == null)
					return new RecipeOutput((NBTTagCompound) null, new ItemStack[0]);

				return new RecipeOutput((NBTTagCompound) null, new ItemStack[] { output.getValue() });
			}
		}

		return null;
	}

	private RecipeOutput processOutputSlot(boolean simulate)
	{
		if (!this.hotcoolinputSlot.isEmpty())
		{
			MutableObject<ItemStack> output = new MutableObject();
			if (this.hotcoolinputSlot.transferFromTank(this.outputTank, output, simulate) && (output.getValue() == null || this.hotcoolantoutputSlot.canAdd(output.getValue())))
			{
				if (output.getValue() == null)
					return new RecipeOutput((NBTTagCompound) null, new ItemStack[0]);

				return new RecipeOutput((NBTTagCompound) null, new ItemStack[] { output.getValue() });
			}
		}

		return null;
	}

	@Override
	public boolean isFluidCooled()
	{
		return this.fluidcoolreactor;
	}

	private void movefluidinWorld(boolean out)
	{
		if (out)
		{
			if (this.inputTank.getFluidAmount() < 1000 && this.outputTank.getFluidAmount() < 1000)
			{
				this.inputTank.setFluid((FluidStack) null);
				this.outputTank.setFluid((FluidStack) null);
			}
			else
			{
				for (int yoffset = 1; yoffset < 4; ++yoffset)
					for (int xoffset = 1; xoffset < 4; ++xoffset)
						for (int zoffset = 1; zoffset < 4; ++zoffset)
							if (this.surroundings[xoffset][yoffset][zoffset] instanceof BlockAir)
								if (this.inputTank.getFluidAmount() >= 1000)
								{
									this.worldObj.setBlock(xoffset + this.xCoord - 2, yoffset + this.yCoord - 2, zoffset + this.zCoord - 2, this.inputTank.getFluid().getFluid().getBlock());
									this.inputTank.drain(1000, true);
								}
								else if (this.outputTank.getFluidAmount() >= 1000)
								{
									this.worldObj.setBlock(xoffset + this.xCoord - 2, yoffset + this.yCoord - 2, zoffset + this.zCoord - 2, this.outputTank.getFluid().getFluid().getBlock());
									this.outputTank.drain(1000, true);
								}

				if (this.inputTank.getFluidAmount() < 1000)
					this.inputTank.setFluid((FluidStack) null);

				if (this.outputTank.getFluidAmount() < 1000)
					this.outputTank.setFluid((FluidStack) null);
			}
		}
		else
		{
			Fluid coolantFluid = BlocksItems.getFluid(InternalName.fluidCoolant);
			Block coolantBlock = BlocksItems.getFluidBlock(InternalName.fluidCoolant);
			Fluid hotCoolantFluid = BlocksItems.getFluid(InternalName.fluidHotCoolant);
			Block hotCoolantBlock = BlocksItems.getFluidBlock(InternalName.fluidHotCoolant);

			for (int yoffset = 1; yoffset < 4; ++yoffset)
				for (int xoffset = 1; xoffset < 4; ++xoffset)
					for (int zoffset = 1; zoffset < 4; ++zoffset)
						if (this.surroundings[xoffset][yoffset][zoffset] == coolantBlock)
						{
							this.worldObj.setBlock(xoffset + this.xCoord - 2, yoffset + this.yCoord - 2, zoffset + this.zCoord - 2, Blocks.air);
							this.inputTank.fill(new FluidStack(coolantFluid, 1000), true);
						}
						else if (this.surroundings[xoffset][yoffset][zoffset] == hotCoolantBlock)
						{
							this.worldObj.setBlock(xoffset + this.xCoord - 2, yoffset + this.yCoord - 2, zoffset + this.zCoord - 2, Blocks.air);
							this.outputTank.fill(new FluidStack(hotCoolantFluid, 1000), true);
						}
		}

	}

	private boolean readyforpressurizedreactor()
	{
		Block coolantBlock = BlocksItems.getFluidBlock(InternalName.fluidCoolant);
		Block hotCoolantBlock = BlocksItems.getFluidBlock(InternalName.fluidHotCoolant);

		for (int xoffset = -2; xoffset < 3; ++xoffset)
			for (int yoffset = -2; yoffset < 3; ++yoffset)
				for (int zoffset = -2; zoffset < 3; ++zoffset)
					if (this.worldObj.isAirBlock(xoffset + this.xCoord, yoffset + this.yCoord, zoffset + this.zCoord))
						this.surroundings[xoffset + 2][yoffset + 2][zoffset + 2] = Blocks.air;
					else
					{
						Block block = this.worldObj.getBlock(xoffset + this.xCoord, yoffset + this.yCoord, zoffset + this.zCoord);
						if ((block == coolantBlock || block == hotCoolantBlock) && this.worldObj.getBlockMetadata(xoffset + this.xCoord, yoffset + this.yCoord, zoffset + this.zCoord) != 0)
							this.surroundings[xoffset + 2][yoffset + 2][zoffset + 2] = Blocks.air;
						else
							this.surroundings[xoffset + 2][yoffset + 2][zoffset + 2] = block;
					}

		for (int xoffset = 1; xoffset < 4; ++xoffset)
			for (int yoffset = 1; yoffset < 4; ++yoffset)
				for (int zoffset = 1; zoffset < 4; ++zoffset)
					if (!(this.surroundings[xoffset][yoffset][zoffset] instanceof BlockGenerator) && !(this.surroundings[xoffset][yoffset][zoffset] instanceof BlockReactorChamber) && this.surroundings[xoffset][yoffset][zoffset] != coolantBlock && this.surroundings[xoffset][yoffset][zoffset] != hotCoolantBlock && !(this.surroundings[xoffset][yoffset][zoffset] instanceof BlockAir))
						return false;

		for (int xoffset = 0; xoffset < 5; ++xoffset)
			for (int yoffset = 0; yoffset < 5; ++yoffset)
			{
				if (!(this.surroundings[xoffset][4][yoffset] instanceof BlockReactorVessel) && !(this.surroundings[xoffset][4][yoffset] instanceof BlockReactorAccessHatch) && !(this.surroundings[xoffset][4][yoffset] instanceof BlockReactorRedstonePort) && !(this.surroundings[xoffset][4][yoffset] instanceof BlockReactorFluidPort))
					return false;

				if (!(this.surroundings[xoffset][0][yoffset] instanceof BlockReactorVessel) && !(this.surroundings[xoffset][0][yoffset] instanceof BlockReactorAccessHatch) && !(this.surroundings[xoffset][0][yoffset] instanceof BlockReactorRedstonePort) && !(this.surroundings[xoffset][0][yoffset] instanceof BlockReactorFluidPort))
					return false;

				if (!(this.surroundings[0][xoffset][yoffset] instanceof BlockReactorVessel) && !(this.surroundings[0][xoffset][yoffset] instanceof BlockReactorAccessHatch) && !(this.surroundings[0][xoffset][yoffset] instanceof BlockReactorRedstonePort) && !(this.surroundings[0][xoffset][yoffset] instanceof BlockReactorFluidPort))
					return false;

				if (!(this.surroundings[4][xoffset][yoffset] instanceof BlockReactorVessel) && !(this.surroundings[4][xoffset][yoffset] instanceof BlockReactorAccessHatch) && !(this.surroundings[4][xoffset][yoffset] instanceof BlockReactorRedstonePort) && !(this.surroundings[4][xoffset][yoffset] instanceof BlockReactorFluidPort))
					return false;

				if (!(this.surroundings[yoffset][xoffset][0] instanceof BlockReactorVessel) && !(this.surroundings[yoffset][xoffset][0] instanceof BlockReactorAccessHatch) && !(this.surroundings[yoffset][xoffset][0] instanceof BlockReactorRedstonePort) && !(this.surroundings[yoffset][xoffset][0] instanceof BlockReactorFluidPort))
					return false;

				if (!(this.surroundings[yoffset][xoffset][4] instanceof BlockReactorVessel) && !(this.surroundings[yoffset][xoffset][4] instanceof BlockReactorAccessHatch) && !(this.surroundings[yoffset][xoffset][4] instanceof BlockReactorRedstonePort) && !(this.surroundings[yoffset][xoffset][4] instanceof BlockReactorFluidPort))
					return false;
			}

		return true;
	}

	public int gaugeLiquidScaled(int i, int tank)
	{
		switch (tank)
		{
			case 0:
				if (this.inputTank.getFluidAmount() <= 0)
					return 0;

				return this.inputTank.getFluidAmount() * i / this.inputTank.getCapacity();
			case 1:
				if (this.outputTank.getFluidAmount() <= 0)
					return 0;

				return this.outputTank.getFluidAmount() * i / this.outputTank.getCapacity();
			default:
				return 0;
		}
	}

	public FluidTank getinputtank()
	{
		return this.inputTank;
	}

	public FluidTank getoutputtank()
	{
		return this.outputTank;
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from)
	{
		return new FluidTankInfo[] { this.inputTank.getInfo(), this.outputTank.getInfo() };
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid)
	{
		return !this.fluidcoolreactor ? false : fluid == BlocksItems.getFluid(InternalName.fluidCoolant);
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid)
	{
		if (!this.fluidcoolreactor)
			return false;
		else
		{
			FluidStack fluidStack = this.outputTank.getFluid();
			return fluidStack == null ? false : fluidStack.isFluidEqual(new FluidStack(fluid, 1));
		}
	}

	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill)
	{
		return !this.canFill(from, resource.getFluid()) ? 0 : this.inputTank.fill(resource, doFill);
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
	{
		return resource != null && resource.isFluidEqual(this.outputTank.getFluid()) ? !this.canDrain(from, resource.getFluid()) ? null : this.outputTank.drain(resource.amount, doDrain) : null;
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
	{
		return this.outputTank.drain(maxDrain, doDrain);
	}

	@Override
	public int getInventoryStackLimit()
	{
		return 1;
	}
}
