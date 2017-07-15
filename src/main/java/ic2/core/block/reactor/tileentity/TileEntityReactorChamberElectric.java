package ic2.core.block.reactor.tileentity;

import java.lang.ref.WeakReference;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.Direction;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.reactor.IReactorChamber;
import ic2.api.tile.IWrenchable;
import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.ITickCallback;
import ic2.core.Ic2Items;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

public class TileEntityReactorChamberElectric extends TileEntity implements IHasGui, IWrenchable, IInventory, IReactorChamber, IEnergyEmitter, IFluidHandler
{
	public boolean redpowert = false;
	private short ticker = 0;
	private boolean loaded = false;

	// TODO gamerforEA code start
	private WeakReference<TileEntityNuclearReactorElectric> cachedReactor;
	// TODO gamerforEA code end

	@Override
	public void validate()
	{
		super.validate();
		IC2.tickHandler.addSingleTickCallback(this.worldObj, new ITickCallback()
		{
			@Override
			public void tickCallback(World world)
			{
				if (!TileEntityReactorChamberElectric.this.isInvalid() && world.blockExists(TileEntityReactorChamberElectric.this.xCoord, TileEntityReactorChamberElectric.this.yCoord, TileEntityReactorChamberElectric.this.zCoord))
				{
					TileEntityReactorChamberElectric.this.onLoaded();
					if (TileEntityReactorChamberElectric.this.enableUpdateEntity())
						world.loadedTileEntityList.add(TileEntityReactorChamberElectric.this);

				}
			}
		});
	}

	public void onLoaded()
	{
		if (IC2.platform.isSimulating())
		{
			TileEntityNuclearReactorElectric te = this.getReactor();
			if (te != null)
				te.refreshChambers();
		}

		this.loaded = true;
	}

	public void onUnloaded()
	{
		if (IC2.platform.isSimulating() && this.worldObj.blockExists(this.xCoord, this.yCoord, this.zCoord))
		{
			TileEntityNuclearReactorElectric te = this.getReactor();
			if (te != null)
				te.refreshChambers();
		}

		this.loaded = false;
	}

	@Override
	public boolean emitsEnergyTo(TileEntity receiver, ForgeDirection direction)
	{
		return true;
	}

	@Override
	public void invalidate()
	{
		super.invalidate();
		if (this.loaded)
			this.onUnloaded();

		// TODO gamerforEA code start
		this.cachedReactor = null;
		// TODO gamerforEA code end
	}

	@Override
	public void onChunkUnload()
	{
		super.onChunkUnload();
		if (this.loaded)
			this.onUnloaded();

	}

	@Override
	public void updateEntity()
	{
		super.updateEntity();

		// TODO gamerforEA code start
		this.cachedReactor = null;
		// TODO gamerforEA code end

		if (this.ticker == 19)
		{
			if (this.worldObj.isBlockIndirectlyGettingPowered(this.xCoord, this.yCoord, this.zCoord))
			{
				if (!this.redpowert)
				{
					this.redpowert = true;
					this.setRedstoneSignal(this.redpowert);
				}
			}
			else if (this.redpowert)
			{
				this.redpowert = false;
				this.setRedstoneSignal(this.redpowert);
			}

			this.ticker = 0;
		}

		++this.ticker;
	}

	@Override
	public final boolean canUpdate()
	{
		return true;
	}

	public boolean enableUpdateEntity()
	{
		return true;
	}

	@Override
	public boolean wrenchCanSetFacing(EntityPlayer entityPlayer, int side)
	{
		return false;
	}

	@Override
	public short getFacing()
	{
		return (short) 0;
	}

	@Override
	public void setFacing(short facing)
	{
	}

	@Override
	public boolean wrenchCanRemove(EntityPlayer entityPlayer)
	{
		return true;
	}

	@Override
	public float getWrenchDropRate()
	{
		return 0.8F;
	}

	@Override
	public ItemStack getWrenchDrop(EntityPlayer entityPlayer)
	{
		return Ic2Items.reactorChamber.copy();
	}

	@Override
	public int getSizeInventory()
	{
		TileEntityNuclearReactorElectric reactor = this.getReactor();
		return reactor == null ? 0 : reactor.getSizeInventory();
	}

	@Override
	public ItemStack getStackInSlot(int i)
	{
		TileEntityNuclearReactorElectric reactor = this.getReactor();
		return reactor == null ? null : reactor.getStackInSlot(i);
	}

	@Override
	public ItemStack decrStackSize(int i, int j)
	{
		TileEntityNuclearReactorElectric reactor = this.getReactor();
		return reactor == null ? null : reactor.decrStackSize(i, j);
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack itemstack)
	{
		TileEntityNuclearReactorElectric reactor = this.getReactor();
		if (reactor != null)
			reactor.setInventorySlotContents(i, itemstack);
	}

	@Override
	public String getInventoryName()
	{
		TileEntityNuclearReactorElectric reactor = this.getReactor();
		return reactor == null ? "Nuclear Reactor" : reactor.getInventoryName();
	}

	@Override
	public boolean hasCustomInventoryName()
	{
		return false;
	}

	@Override
	public int getInventoryStackLimit()
	{
		TileEntityNuclearReactorElectric reactor = this.getReactor();
		return reactor == null ? 64 : reactor.getInventoryStackLimit();
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer entityplayer)
	{
		TileEntityNuclearReactorElectric reactor = this.getReactor();
		return reactor == null ? false : reactor.isUseableByPlayer(entityplayer);
	}

	@Override
	public void openInventory()
	{
		TileEntityNuclearReactorElectric reactor = this.getReactor();
		if (reactor != null)
			reactor.openInventory();
	}

	@Override
	public void closeInventory()
	{
		TileEntityNuclearReactorElectric reactor = this.getReactor();
		if (reactor != null)
			reactor.closeInventory();
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int var1)
	{
		TileEntityNuclearReactorElectric reactor = this.getReactor();
		return reactor == null ? null : reactor.getStackInSlotOnClosing(var1);
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemstack)
	{
		TileEntityNuclearReactorElectric reactor = this.getReactor();
		return reactor == null ? false : reactor.isItemValidForSlot(i, itemstack);
	}

	@Override
	public TileEntityNuclearReactorElectric getReactor()
	{
		// TODO gamerforEA code start
		WeakReference<TileEntityNuclearReactorElectric> ref = this.cachedReactor;
		if (ref != null)
		{
			TileEntityNuclearReactorElectric tile = ref.get();
			if (tile == null)
				this.cachedReactor = null;
			else
				return tile;
		}
		// TODO gamerforEA code end

		for (Direction value : Direction.directions)
		{
			TileEntity te = value.applyToTileEntity(this);
			if (te instanceof TileEntityNuclearReactorElectric)
			{
				TileEntityNuclearReactorElectric reactor = (TileEntityNuclearReactorElectric) te;

				// TODO gamerforEA code start
				this.cachedReactor = new WeakReference<TileEntityNuclearReactorElectric>(reactor);
				// TODO gamerforEA code end

				return reactor;
			}
		}

		// TODO gamerforEA code replace, old code: Block blk = this.getBlockType();
		Block blk = this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord);
		// TODO gamerforEA code end

		if (blk != null)
			blk.onNeighborBlockChange(this.worldObj, this.xCoord, this.yCoord, this.zCoord, blk);

		return null;
	}

	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill)
	{
		TileEntityNuclearReactorElectric reactor = this.getReactor();
		return reactor == null ? 0 : reactor.fill(from, resource, doFill);
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
	{
		TileEntityNuclearReactorElectric reactor = this.getReactor();
		return reactor == null ? null : reactor.drain(from, resource, doDrain);
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
	{
		TileEntityNuclearReactorElectric reactor = this.getReactor();
		return reactor == null ? null : reactor.drain(from, maxDrain, doDrain);
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid)
	{
		TileEntityNuclearReactorElectric reactor = this.getReactor();
		return reactor == null ? false : reactor.canFill(from, fluid);
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid)
	{
		TileEntityNuclearReactorElectric reactor = this.getReactor();
		return reactor == null ? false : reactor.canDrain(from, fluid);
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from)
	{
		TileEntityNuclearReactorElectric reactor = this.getReactor();
		return reactor == null ? null : reactor.getTankInfo(from);
	}

	@Override
	public ContainerBase<?> getGuiContainer(EntityPlayer entityPlayer)
	{
		TileEntityNuclearReactorElectric reactor = this.getReactor();
		return reactor == null ? null : reactor.getGuiContainer(entityPlayer);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getGui(EntityPlayer entityPlayer, boolean isAdmin)
	{
		TileEntityNuclearReactorElectric reactor = this.getReactor();
		return reactor == null ? null : reactor.getGui(entityPlayer, isAdmin);
	}

	@Override
	public void onGuiClosed(EntityPlayer entityPlayer)
	{
		TileEntityNuclearReactorElectric reactor = this.getReactor();
		if (reactor != null)
			reactor.onGuiClosed(entityPlayer);
	}

	@Override
	public void setRedstoneSignal(boolean redstone)
	{
		TileEntityNuclearReactorElectric reactor = this.getReactor();
		if (reactor != null)
			reactor.setRedstoneSignal(redstone);
	}
}
