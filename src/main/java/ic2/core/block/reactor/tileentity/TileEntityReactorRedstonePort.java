package ic2.core.block.reactor.tileentity;

import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorChamber;
import ic2.api.tile.IWrenchable;
import ic2.core.Ic2Items;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class TileEntityReactorRedstonePort extends TileEntity implements IWrenchable
{
	public boolean redpowert = false;
	private short ticker = 0;

	@Override
	public void updateEntity()
	{
		super.updateEntity();
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

	public void setRedstoneSignal(boolean redstone)
	{
		TileEntity reactor = this.getReactor();
		if (reactor != null)
			if (reactor instanceof IReactor)
				((IReactor) reactor).setRedstoneSignal(redstone);
			else if (reactor instanceof IReactorChamber)
				((IReactorChamber) reactor).setRedstoneSignal(redstone);
	}

	public TileEntity getReactor()
	{
		for (int xoffset = -1; xoffset < 2; ++xoffset)
		{
			for (int yoffset = -1; yoffset < 2; ++yoffset)
			{
				for (int zoffset = -1; zoffset < 2; ++zoffset)
				{
					TileEntity te = this.worldObj.getTileEntity(this.xCoord + xoffset, this.yCoord + yoffset, this.zCoord + zoffset);
					if (te instanceof IReactorChamber || te instanceof IReactor)
						return te;
				}
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
	public final boolean canUpdate()
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
		return Ic2Items.reactorRedstonePort.copy();
	}
}
