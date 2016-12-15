package ic2.core.block.reactor.tileentity;

import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorChamber;
import ic2.api.tile.IWrenchable;
import ic2.core.Ic2Items;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class TileEntityReactorAccessHatch extends TileEntity implements IWrenchable, ISidedInventory
{
	@Override
	public final boolean canUpdate()
	{
		return false;
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
		return Ic2Items.reactorAccessHatch.copy();
	}

	@Override
	public int getSizeInventory()
	{
		IInventory reactor = this.getReactor();
		return reactor == null ? 0 : reactor.getSizeInventory();
	}

	@Override
	public ItemStack getStackInSlot(int i)
	{
		IInventory reactor = this.getReactor();
		return reactor == null ? null : reactor.getStackInSlot(i);
	}

	@Override
	public ItemStack decrStackSize(int i, int j)
	{
		IInventory reactor = this.getReactor();
		return reactor == null ? null : reactor.decrStackSize(i, j);
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack itemstack)
	{
		IInventory reactor = this.getReactor();
		if (reactor != null)
			reactor.setInventorySlotContents(i, itemstack);
	}

	@Override
	public String getInventoryName()
	{
		IInventory reactor = this.getReactor();
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
		IInventory reactor = this.getReactor();
		return reactor == null ? 64 : reactor.getInventoryStackLimit();
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer entityplayer)
	{
		IInventory reactor = this.getReactor();
		return reactor == null ? false : reactor.isUseableByPlayer(entityplayer);
	}

	@Override
	public void openInventory()
	{
		IInventory reactor = this.getReactor();
		if (reactor != null)
			reactor.openInventory();
	}

	@Override
	public void closeInventory()
	{
		IInventory reactor = this.getReactor();
		if (reactor != null)
			reactor.closeInventory();
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int var1)
	{
		IInventory reactor = this.getReactor();
		return reactor == null ? null : reactor.getStackInSlotOnClosing(var1);
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemstack)
	{
		IInventory reactor = this.getReactor();
		return reactor == null ? false : reactor.isItemValidForSlot(i, itemstack);
	}

	public IInventory getReactor()
	{
		for (int xoffset = -1; xoffset < 2; ++xoffset)
			for (int yoffset = -1; yoffset < 2; ++yoffset)
				for (int zoffset = -1; zoffset < 2; ++zoffset)
				{
					TileEntity te = this.worldObj.getTileEntity(this.xCoord + xoffset, this.yCoord + yoffset, this.zCoord + zoffset);
					if (te instanceof IReactorChamber || te instanceof IReactor)
						return (IInventory) te;
				}

		// TODO gamerforEA code replace, old code: Block blk = this.getBlockType();
		Block blk = this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord);
		// TODO gamerforEA code end

		if (blk != null)
			blk.onNeighborBlockChange(this.worldObj, this.xCoord, this.yCoord, this.zCoord, blk);

		return null;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int p_94128_1_)
	{
		IInventory inv = this.getReactor();
		if (inv instanceof ISidedInventory)
			return ((ISidedInventory) inv).getAccessibleSlotsFromSide(p_94128_1_);
		else
		{
			int[] accessibleSlots = new int[this.getSizeInventory()];

			for (int i = 0; i < accessibleSlots.length; accessibleSlots[i] = i++)
				;

			return accessibleSlots;
		}
	}

	@Override
	public boolean canInsertItem(int p_102007_1_, ItemStack p_102007_2_, int p_102007_3_)
	{
		IInventory reactor = this.getReactor();
		return reactor instanceof ISidedInventory ? ((ISidedInventory) reactor).canInsertItem(p_102007_1_, p_102007_2_, p_102007_3_) : reactor.isItemValidForSlot(p_102007_1_, p_102007_2_);
	}

	@Override
	public boolean canExtractItem(int p_102008_1_, ItemStack p_102008_2_, int p_102008_3_)
	{
		IInventory reactor = this.getReactor();
		return reactor instanceof ISidedInventory ? ((ISidedInventory) reactor).canExtractItem(p_102008_1_, p_102008_2_, p_102008_3_) : true;
	}
}
