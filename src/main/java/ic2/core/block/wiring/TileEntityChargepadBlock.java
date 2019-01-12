package ic2.core.block.wiring;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.tile.IEnergyStorage;
import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.Ic2Items;
import ic2.core.init.MainConfig;
import ic2.core.util.ConfigUtil;
import ic2.core.util.EntityIC2FX;
import ic2.core.util.StackUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Random;

public abstract class TileEntityChargepadBlock extends TileEntityElectricBlock
		implements IEnergySink, IEnergySource, IHasGui, IEnergyStorage
{
	private int updateTicker = IC2.random.nextInt(this.getTickRate());
	private EntityPlayer player = null;
	private boolean isEmittingRedstone = false;
	public boolean addedToEnergyNet = false;
	public static byte redstoneModes = 2;

	public TileEntityChargepadBlock(int tier1, int output1, int maxStorage1)
	{
		super(tier1, output1, maxStorage1);
	}

	public void playerstandsat(EntityPlayer entity)
	{
		if (this.player == null)
			this.player = entity;
		else if (this.player.getUniqueID() != entity.getUniqueID())
			this.player = entity;

	}

	protected int getTickRate()
	{
		return 2;
	}

	@Override
	public boolean wrenchCanSetFacing(EntityPlayer entityPlayer, int side)
	{
		return side != 1 && this.getFacing() != side;
	}

	@Override
	protected void updateEntityServer()
	{
		super.updateEntityServer();
		boolean needsInvUpdate = false;
		if (this.updateTicker++ % this.getTickRate() == 0)
		{
			if (this.player != null && this.energy >= 1.0D)
			{
				if (!this.getActive())
					this.setActive(true);

				this.getItems(this.player);
				this.player = null;
				needsInvUpdate = true;
			}
			else if (this.getActive())
			{
				this.setActive(false);
				needsInvUpdate = true;
			}

			/* TODO gamerforEA code replace, old code:
			if ((this.redstoneMode != 0 || !this.getActive()) && (this.redstoneMode != 1 || this.getActive()))
			{
				this.isEmittingRedstone = false;
				this.worldObj.notifyBlocksOfNeighborChange(this.xCoord, this.yCoord, this.zCoord, this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord));
			}
			else
			{
				this.isEmittingRedstone = true;
				this.worldObj.notifyBlocksOfNeighborChange(this.xCoord, this.yCoord, this.zCoord, this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord));
			} */
			boolean isEmittingRedstone = !((this.redstoneMode != 0 || !this.getActive()) && (this.redstoneMode != 1 || this.getActive()));
			if (this.isEmittingRedstone != isEmittingRedstone)
			{
				this.isEmittingRedstone = isEmittingRedstone;
				this.worldObj.notifyBlocksOfNeighborChange(this.xCoord, this.yCoord, this.zCoord, this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord));
			}
			// TODO gamerforEA code end

			if (needsInvUpdate)
				this.markDirty();

		}
	}

	protected abstract void getItems(EntityPlayer var1);

	@Override
	public boolean acceptsEnergyFrom(TileEntity emitter, ForgeDirection direction)
	{
		return direction != ForgeDirection.UP && !this.facingMatchesDirection(direction);
	}

	@Override
	public ContainerBase<TileEntityChargepadBlock> getGuiContainer(EntityPlayer entityPlayer)
	{
		return new ContainerChargepadBlock(entityPlayer, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getGui(EntityPlayer entityPlayer, boolean isAdmin)
	{
		return new GuiChargepadBlock(new ContainerChargepadBlock(entityPlayer, this));
	}

	@Override
	public void onGuiClosed(EntityPlayer entityPlayer)
	{
	}

	@Override
	public boolean isEmittingRedstone()
	{
		return this.isEmittingRedstone;
	}

	@SideOnly(Side.CLIENT)
	public void spawnParticles(World world, int blockX, int blockY, int blockZ, Random rand)
	{
		if (this.getActive())
		{
			EffectRenderer effect = FMLClientHandler.instance().getClient().effectRenderer;

			for (int particles = 20; particles > 0; --particles)
			{
				double x = (double) ((float) blockX + 0.0F + rand.nextFloat());
				double y = (double) ((float) blockY + 0.9F + rand.nextFloat());
				double z = (double) ((float) blockZ + 0.0F + rand.nextFloat());
				effect.addEffect(new EntityIC2FX(world, x, y, z, 60, new double[] { 0.0D, 0.1D, 0.0D }, new float[] { 0.2F, 0.2F, 1.0F }));
			}
		}

	}

	@Override
	public ItemStack getWrenchDrop(EntityPlayer entityPlayer)
	{
		ItemStack ret = super.getWrenchDrop(entityPlayer);
		float energyRetainedInStorageBlockDrops = ConfigUtil.getFloat(MainConfig.get(), "balance/energyRetainedInStorageBlockDrops");
		if (energyRetainedInStorageBlockDrops > 0.0F)
		{
			NBTTagCompound nbttagcompound = StackUtil.getOrCreateNbtData(ret);
			nbttagcompound.setDouble("energy", this.energy * (double) energyRetainedInStorageBlockDrops);
		}

		return ret;
	}

	@Override
	public void onNetworkEvent(EntityPlayer player, int event)
	{
		++this.redstoneMode;
		if (this.redstoneMode >= redstoneModes)
			this.redstoneMode = 0;

		IC2.platform.messagePlayer(player, this.getredstoneMode());
	}

	@Override
	public String getredstoneMode()
	{
		return this.redstoneMode <= 1 && this.redstoneMode >= 0 ? StatCollector.translateToLocal("ic2.blockChargepad.gui.mod.redstone" + this.redstoneMode) : "";
	}

	protected void chargeitems(ItemStack itemstack, int chargefactor)
	{
		if (itemstack.getItem() instanceof IElectricItem)
			if (itemstack.getItem() != Ic2Items.debug.getItem())
			{
				double freeamount = ElectricItem.manager.charge(itemstack, Double.POSITIVE_INFINITY, this.tier, true, true);
				double charge = 0.0D;
				if (freeamount >= 0.0D)
				{
					if (freeamount >= (double) (chargefactor * this.getTickRate()))
						charge = (double) (chargefactor * this.getTickRate());
					else
						charge = freeamount;

					if (this.energy < charge)
						charge = this.energy;

					this.energy -= ElectricItem.manager.charge(itemstack, charge, this.tier, true, false);
				}

			}
	}
}
