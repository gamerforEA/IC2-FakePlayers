package ic2.core.block;

import com.google.common.base.Preconditions;
import ic2.core.block.invslot.InvSlot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class TileEntityInventory extends TileEntityBlock implements ISidedInventory
{
	public final List<InvSlot> invSlots = new ArrayList<>();

	@Override
	public void readFromNBT(NBTTagCompound nbtTagCompound)
	{
		super.readFromNBT(nbtTagCompound);
		if (nbtTagCompound.hasKey("Items"))
		{
			NBTTagList nbtTagList = nbtTagCompound.getTagList("Items", 10);

			for (int i = 0; i < nbtTagList.tagCount(); ++i)
			{
				NBTTagCompound nbtTagCompoundSlot = nbtTagList.getCompoundTagAt(i);
				byte slot = nbtTagCompoundSlot.getByte("Slot");
				int maxOldStartIndex = -1;
				InvSlot maxSlot = null;

				for (InvSlot invSlot : this.invSlots)
				{
					if (invSlot.oldStartIndex <= slot && invSlot.oldStartIndex > maxOldStartIndex)
					{
						maxOldStartIndex = invSlot.oldStartIndex;
						maxSlot = invSlot;
					}
				}

				if (maxSlot != null)
				{
					int index = Math.min(slot - maxOldStartIndex, maxSlot.size() - 1);
					maxSlot.put(index, ItemStack.loadItemStackFromNBT(nbtTagCompoundSlot));
				}
			}
		}

		NBTTagCompound invSlotsTag = nbtTagCompound.getCompoundTag("InvSlots");

		for (InvSlot invSlot : this.invSlots)
		{
			invSlot.readFromNbt(invSlotsTag.getCompoundTag(invSlot.name));
		}

	}

	@Override
	public void writeToNBT(NBTTagCompound nbtTagCompound)
	{
		super.writeToNBT(nbtTagCompound);
		NBTTagCompound invSlotsTag = new NBTTagCompound();

		for (InvSlot invSlot : this.invSlots)
		{
			NBTTagCompound invSlotTag = new NBTTagCompound();
			invSlot.writeToNbt(invSlotTag);
			invSlotsTag.setTag(invSlot.name, invSlotTag);
		}

		nbtTagCompound.setTag("InvSlots", invSlotsTag);
	}

	@Override
	public int getSizeInventory()
	{
		int ret = 0;

		for (InvSlot invSlot : this.invSlots)
		{
			ret += invSlot.size();
		}

		return ret;
	}

	@Override
	public ItemStack getStackInSlot(int index)
	{
		for (InvSlot invSlot : this.invSlots)
		{
			if (index < invSlot.size())
				return invSlot.get(index);

			index -= invSlot.size();
		}

		return null;
	}

	@Override
	public ItemStack decrStackSize(int index, int amount)
	{
		ItemStack stack = this.getStackInSlot(index);
		if (stack == null)
			return null;

		if (amount >= stack.stackSize)
		{
			this.setInventorySlotContents(index, null);
			return stack;
		}

		if (amount != 0)
		{
			// TODO gamerforEA code start
			InvSlot targetSlot = Preconditions.checkNotNull(this.getInvSlot(index));
			// TODO gamerforEA code end

			if (amount < 0)
			{
				// TODO gamerforEA code replace, old code:
				// int space = Math.min(this.getInvSlot(index).getStackSizeLimit(), stack.getMaxStackSize()) - stack.stackSize;
				int space = Math.min(targetSlot.getStackSizeLimit(), stack.getMaxStackSize()) - stack.stackSize;
				// TODO gamerforEA code end

				amount = Math.max(amount, -space);
			}

			stack.stackSize -= amount;

			// TODO gamerforEA code replace, old code:
			// this.getInvSlot(index).onChanged();
			targetSlot.onChanged();
			// TODO gamerforEA code end
		}

		ItemStack ret = stack.copy();
		ret.stackSize = amount;
		return ret;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int index)
	{
		ItemStack ret = this.getStackInSlot(index);
		if (ret != null)
			this.setInventorySlotContents(index, null);

		return ret;
	}

	@Override
	public void setInventorySlotContents(int index, ItemStack itemStack)
	{
		for (InvSlot invSlot : this.invSlots)
		{
			if (index < invSlot.size())
			{
				invSlot.put(index, itemStack);
				break;
			}

			index -= invSlot.size();
		}

	}

	@Override
	public void markDirty()
	{
		super.markDirty();

		for (InvSlot invSlot : this.invSlots)
		{
			invSlot.onChanged();
		}

	}

	@Override
	public abstract String getInventoryName();

	@Override
	public boolean hasCustomInventoryName()
	{
		return false;
	}

	@Override
	public int getInventoryStackLimit()
	{
		int max = 0;

		for (InvSlot slot : this.invSlots)
		{
			max = Math.max(max, slot.getStackSizeLimit());
		}

		return max;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer entityPlayer)
	{
		return !this.isInvalid() && entityPlayer.getDistance((double) this.xCoord + 0.5D, (double) this.yCoord + 0.5D, (double) this.zCoord + 0.5D) <= 64.0D;
	}

	@Override
	public void openInventory()
	{
	}

	@Override
	public void closeInventory()
	{
	}

	@Override
	public boolean isItemValidForSlot(int index, ItemStack itemStack)
	{
		InvSlot invSlot = this.getInvSlot(index);
		return invSlot != null && invSlot.canInput() && invSlot.accepts(itemStack);
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int var1)
	{
		int[] ret = new int[this.getSizeInventory()];

		for (int i = 0; i < ret.length; )
		{
			ret[i] = i++;
		}

		return ret;
	}

	@Override
	public boolean canInsertItem(int index, ItemStack itemStack, int side)
	{
		InvSlot targetSlot = this.getInvSlot(index);
		if (targetSlot == null)
			return false;
		if (targetSlot.canInput() && targetSlot.accepts(itemStack))
		{
			if (targetSlot.preferredSide != InvSlot.InvSide.ANY && targetSlot.preferredSide.matches(side))
				return true;
			for (InvSlot invSlot : this.invSlots)
			{
				if (invSlot != targetSlot && invSlot.preferredSide != InvSlot.InvSide.ANY && invSlot.preferredSide.matches(side) && invSlot.canInput() && invSlot.accepts(itemStack))
					return false;
			}

			return true;
		}
		return false;
	}

	@Override
	public boolean canExtractItem(int index, ItemStack itemStack, int side)
	{
		InvSlot targetSlot = this.getInvSlot(index);
		if (targetSlot == null)
			return false;
		if (!targetSlot.canOutput())
			return false;
		boolean correctSide = targetSlot.preferredSide.matches(side);
		if (targetSlot.preferredSide != InvSlot.InvSide.ANY && correctSide)
			return true;
		Iterator var6 = this.invSlots.iterator();

		while (true)
		{
			if (!var6.hasNext())
				return true;

			InvSlot invSlot = (InvSlot) var6.next();
			if (invSlot != targetSlot && (invSlot.preferredSide != InvSlot.InvSide.ANY || !correctSide) && invSlot.preferredSide.matches(side) && invSlot.canOutput())
				break;
		}

		return false;
	}

	public void addInvSlot(InvSlot invSlot)
	{
		this.invSlots.add(invSlot);
	}

	private InvSlot getInvSlot(int index)
	{
		for (InvSlot invSlot : this.invSlots)
		{
			if (index < invSlot.size())
				return invSlot;

			index -= invSlot.size();
		}

		return null;
	}
}
