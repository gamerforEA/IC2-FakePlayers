package ic2.core.util;

import ic2.api.Direction;
import ic2.core.IC2;
import ic2.core.block.personal.IPersonalBlock;
import ic2.core.block.personal.TileEntityTradeOMat;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;

import java.util.*;

public final class StackUtil
{
	private static final int[] emptySlotArray = new int[0];

	public static StackUtil.AdjacentInv getAdjacentInventory(TileEntity source, Direction direction)
	{
		TileEntity target = direction.applyToTileEntity(source);
		if (!(target instanceof IInventory))
			return null;
		else
		{
			// TODO gamerforEA code start
			if (source instanceof TileEntityTradeOMat && target instanceof TileEntityTradeOMat && source != target)
				return null;
			// TODO gamerforEA code end

			IInventory inventory = (IInventory) target;
			if (target instanceof TileEntityChest)
				for (Direction direction2 : Direction.directions)
				{
					if (direction2 != Direction.YN && direction2 != Direction.YP)
					{
						TileEntity target2 = direction2.applyToTileEntity(target);
						if (target2 instanceof TileEntityChest)
						{
							inventory = new InventoryLargeChest("", inventory, (IInventory) target2);
							break;
						}
					}
				}

			if (target instanceof IPersonalBlock)
			{
				if (!(source instanceof IPersonalBlock))
					return null;

				if (!((IPersonalBlock) target).permitsAccess(((IPersonalBlock) source).getOwner()))
					return null;
			}

			return new StackUtil.AdjacentInv(inventory, direction);
		}
	}

	public static List<StackUtil.AdjacentInv> getAdjacentInventories(TileEntity source)
	{
		List<StackUtil.AdjacentInv> inventories = new ArrayList();

		for (Direction direction : Direction.directions)
		{
			StackUtil.AdjacentInv inventory = getAdjacentInventory(source, direction);
			if (inventory != null)
				inventories.add(inventory);
		}

		Collections.sort(inventories, new Comparator<StackUtil.AdjacentInv>()
		{
			@Override
			public int compare(StackUtil.AdjacentInv a, StackUtil.AdjacentInv b)
			{
				return !(a.inv instanceof IPersonalBlock) && b.inv instanceof IPersonalBlock ? !(b.inv instanceof IPersonalBlock) && a.inv instanceof IPersonalBlock ? b.inv.getSizeInventory() - a.inv.getSizeInventory() : 1 : -1;
			}
		});
		return inventories;
	}

	public static int distribute(TileEntity source, ItemStack itemStack, boolean simulate)
	{
		int transferred = 0;

		for (StackUtil.AdjacentInv inventory : getAdjacentInventories(source))
		{
			int amount = putInInventory(inventory.inv, inventory.dir.getInverse(), itemStack, simulate);
			transferred += amount;
			itemStack.stackSize -= amount;
			if (itemStack.stackSize == 0)
				break;
		}

		itemStack.stackSize += transferred;
		return transferred;
	}

	public static ItemStack fetch(TileEntity source, ItemStack itemStack, boolean simulate)
	{
		ItemStack ret = null;
		int oldStackSize = itemStack.stackSize;

		for (StackUtil.AdjacentInv inventory : getAdjacentInventories(source))
		{
			ItemStack transferred = getFromInventory(inventory.inv, inventory.dir.getInverse(), itemStack, itemStack.stackSize, true, simulate);
			if (transferred != null)
			{
				if (ret == null)
					ret = transferred;
				else
				{
					ret.stackSize += transferred.stackSize;
					itemStack.stackSize -= transferred.stackSize;
				}

				if (itemStack.stackSize <= 0)
					break;
			}
		}

		itemStack.stackSize = oldStackSize;
		return ret;
	}

	public static int transfer(IInventory src, IInventory dst, Direction dir, int amount)
	{
		int[] srcSlots = getInventorySlots(src, dir, false, true);
		int[] dstSlots = getInventorySlots(dst, dir.getInverse(), true, false);
		ISidedInventory dstSided = dst instanceof ISidedInventory ? (ISidedInventory) dst : null;
		int dstVanillaSide = dir.getInverse().toSideValue();

		label69:
		for (int srcSlot : srcSlots)
		{
			ItemStack srcStack = src.getStackInSlot(srcSlot);
			if (srcStack != null)
			{
				int srcTransfer = Math.min(amount, srcStack.stackSize);

				assert srcTransfer > 0;

				for (int pass = 0; pass < 2; ++pass)
				{
					for (int i = 0; i < dstSlots.length; ++i)
					{
						int dstSlot = dstSlots[i];
						if (dstSlot >= 0)
						{
							ItemStack dstStack = dst.getStackInSlot(dstSlot);
							if ((pass != 0 || dstStack != null && isStackEqualStrict(srcStack, dstStack)) && (pass != 1 || dstStack == null) && dst.isItemValidForSlot(dstSlot, srcStack) && (dstSided == null || dstSided.canInsertItem(dstSlot, srcStack, dstVanillaSide)))
							{
								assert srcTransfer > 0;

								int transfer;
								if (dstStack == null)
								{
									transfer = Math.min(srcTransfer, dst.getInventoryStackLimit());
									dst.setInventorySlotContents(dstSlot, copyWithSize(srcStack, transfer));
								}
								else
								{
									transfer = Math.min(srcTransfer, Math.min(dstStack.getMaxStackSize(), dst.getInventoryStackLimit()) - dstStack.stackSize);
									if (transfer <= 0)
									{
										dstSlots[i] = -1;
										continue;
									}

									dstStack.stackSize += transfer;
								}

								assert transfer > 0;

								srcStack.stackSize -= transfer;
								amount -= transfer;
								srcTransfer -= transfer;
								if (srcTransfer <= 0)
								{
									if (srcStack.stackSize <= 0)
										src.setInventorySlotContents(srcSlot, null);

									if (amount <= 0)
										break label69;
									continue label69;
								}

								assert srcStack.stackSize > 0;

								assert amount > 0;
							}
						}
					}
				}
			}
		}

		amount = amount - amount;

		assert amount >= 0;

		if (amount > 0)
		{
			src.markDirty();
			dst.markDirty();
		}

		return amount;
	}

	public static void distributeDrop(TileEntity source, List<ItemStack> itemStacks)
	{
		Iterator<ItemStack> it = itemStacks.iterator();

		while (it.hasNext())
		{
			ItemStack itemStack = it.next();
			int amount = distribute(source, itemStack, false);
			if (amount == itemStack.stackSize)
				it.remove();
			else
				itemStack.stackSize -= amount;
		}

		for (ItemStack itemStack : itemStacks)
		{
			dropAsEntity(source.getWorldObj(), source.xCoord, source.yCoord, source.zCoord, itemStack);
		}

		itemStacks.clear();
	}

	public static ItemStack getFromInventory(IInventory inv, Direction side, ItemStack itemStackDestination, int max, boolean ignoreMaxStackSize, boolean simulate)
	{
		if (itemStackDestination != null && !ignoreMaxStackSize)
			max = Math.min(max, itemStackDestination.getMaxStackSize() - itemStackDestination.stackSize);

		ItemStack ret = null;

		for (int i : getInventorySlots(inv, side, false, true))
		{
			if (max <= 0)
				break;

			ItemStack stack = inv.getStackInSlot(i);

			assert stack != null;

			if (itemStackDestination == null || isStackEqualStrict(stack, itemStackDestination))
			{
				if (ret == null)
				{
					ret = copyWithSize(stack, 0);
					if (itemStackDestination == null)
					{
						if (!ignoreMaxStackSize)
							max = Math.min(max, ret.getMaxStackSize());

						itemStackDestination = ret;
					}
				}

				int transfer = Math.min(max, stack.stackSize);
				if (!simulate)
				{
					stack.stackSize -= transfer;
					if (stack.stackSize == 0)
						inv.setInventorySlotContents(i, null);
				}

				max -= transfer;
				ret.stackSize += transfer;
			}
		}

		if (!simulate && ret != null)
			inv.markDirty();

		return ret;
	}

	public static int putInInventory(IInventory inv, Direction side, ItemStack itemStackSource, boolean simulate)
	{
		if (itemStackSource == null)
			return 0;
		else
		{
			int toTransfer = itemStackSource.stackSize;
			int vanillaSide = side.toSideValue();
			int[] slots = getInventorySlots(inv, side, true, false);

			for (int i : slots)
			{
				if (toTransfer <= 0)
					break;

				if (inv.isItemValidForSlot(i, itemStackSource) && (!(inv instanceof ISidedInventory) || ((ISidedInventory) inv).canInsertItem(i, itemStackSource, vanillaSide)))
				{
					ItemStack itemStack = inv.getStackInSlot(i);
					if (itemStack != null && isStackEqualStrict(itemStack, itemStackSource))
					{
						int transfer = Math.min(toTransfer, Math.min(inv.getInventoryStackLimit(), itemStack.getMaxStackSize()) - itemStack.stackSize);
						if (!simulate)
							itemStack.stackSize += transfer;

						toTransfer -= transfer;
					}
				}
			}

			for (int i : slots)
			{
				if (toTransfer <= 0)
					break;

				if (inv.isItemValidForSlot(i, itemStackSource) && (!(inv instanceof ISidedInventory) || ((ISidedInventory) inv).canInsertItem(i, itemStackSource, vanillaSide)))
				{
					ItemStack itemStack = inv.getStackInSlot(i);
					if (itemStack == null)
					{
						int transfer = Math.min(toTransfer, Math.min(inv.getInventoryStackLimit(), itemStackSource.getMaxStackSize()));
						if (!simulate)
						{
							ItemStack dest = copyWithSize(itemStackSource, transfer);
							inv.setInventorySlotContents(i, dest);
						}

						toTransfer -= transfer;
					}
				}
			}

			if (!simulate && toTransfer != itemStackSource.stackSize)
				inv.markDirty();

			return itemStackSource.stackSize - toTransfer;
		}
	}

	public static int[] getInventorySlots(IInventory inv, Direction side, boolean checkInsert, boolean checkExtract)
	{
		if (inv.getInventoryStackLimit() <= 0)
			return emptySlotArray;
		else
		{
			ISidedInventory sidedInv;
			int[] ret;
			if (inv instanceof ISidedInventory)
			{
				sidedInv = (ISidedInventory) inv;
				ret = sidedInv.getAccessibleSlotsFromSide(side.toSideValue());

				// TODO gamerforEA code start
				// Fixed by synthetic65535
				if (ret == null)
					return emptySlotArray;
				// TODO gamerforEA code end

				if (ret.length == 0)
					return emptySlotArray;

				ret = Arrays.copyOf(ret, ret.length);
			}
			else
			{
				int size = inv.getSizeInventory();
				if (size <= 0)
					return emptySlotArray;

				sidedInv = null;
				ret = new int[size];

				for (int i = 0; i < ret.length; ret[i] = i++)
				{
				}
			}

			if (checkInsert || checkExtract)
			{
				int writeIdx = 0;
				int vanillaSide = side.toSideValue();

				for (int readIdx = 0; readIdx < ret.length; ++readIdx)
				{
					int slot = ret[readIdx];
					ItemStack stack = inv.getStackInSlot(slot);
					if ((!checkExtract || stack != null && stack.stackSize > 0 && (sidedInv == null || sidedInv.canExtractItem(slot, stack, vanillaSide))) && (!checkInsert || stack == null || stack.stackSize < stack.getMaxStackSize() && stack.stackSize < inv.getInventoryStackLimit() && (sidedInv == null || sidedInv.canInsertItem(slot, stack, vanillaSide))))
					{
						ret[writeIdx] = slot;
						++writeIdx;
					}
				}

				if (writeIdx != ret.length)
					ret = Arrays.copyOf(ret, writeIdx);
			}

			return ret;
		}
	}

	public static void dropAsEntity(World world, int x, int y, int z, ItemStack itemStack)
	{
		if (itemStack != null)
		{
			double f = 0.7D;
			double dx = world.rand.nextFloat() * f + (1.0D - f) * 0.5D;
			double dy = world.rand.nextFloat() * f + (1.0D - f) * 0.5D;
			double dz = world.rand.nextFloat() * f + (1.0D - f) * 0.5D;
			EntityItem entityItem = new EntityItem(world, x + dx, y + dy, z + dz, itemStack.copy());
			entityItem.delayBeforeCanPickup = 10;
			world.spawnEntityInWorld(entityItem);
		}
	}

	public static ItemStack copyWithSize(ItemStack itemStack, int newSize)
	{
		ItemStack ret = itemStack.copy();
		ret.stackSize = newSize;
		return ret;
	}

	public static ItemStack copyWithWildCard(ItemStack itemStack)
	{
		ItemStack ret = itemStack.copy();
		Items.dye.setDamage(ret, 32767);
		return ret;
	}

	public static NBTTagCompound getOrCreateNbtData(ItemStack itemStack)
	{
		NBTTagCompound ret = itemStack.getTagCompound();
		if (ret == null)
		{
			ret = new NBTTagCompound();
			itemStack.setTagCompound(ret);
		}

		return ret;
	}

	public static boolean isStackEqual(ItemStack stack1, ItemStack stack2)
	{
		return stack1 == null && stack2 == null || stack1 != null && stack2 != null && stack1.getItem() == stack2.getItem() && (!stack1.getHasSubtypes() && !stack1.isItemStackDamageable() || stack1.getItemDamage() == stack2.getItemDamage());
	}

	public static boolean isStackEqualStrict(ItemStack stack1, ItemStack stack2)
	{
		return isStackEqual(stack1, stack2) && ItemStack.areItemStackTagsEqual(stack1, stack2);
	}

	public static boolean isTagEqual(ItemStack a, ItemStack b)
	{
		boolean aEmpty = !a.hasTagCompound() || a.getTagCompound().hasNoTags();
		boolean bEmpty = !b.hasTagCompound() || b.getTagCompound().hasNoTags();
		return aEmpty == bEmpty && (aEmpty || a.getTagCompound().equals(b.getTagCompound()));
	}

	public static Block getBlock(ItemStack stack)
	{
		Item item = stack.getItem();
		return item instanceof ItemBlock ? ((ItemBlock) item).field_150939_a : null;
	}

	public static boolean equals(Block block, ItemStack stack)
	{
		return block == getBlock(stack);
	}

	public static boolean damageItemStack(ItemStack itemStack, int amount)
	{
		if (itemStack.attemptDamageItem(amount, IC2.random))
		{
			--itemStack.stackSize;
			itemStack.setItemDamage(0);
			return itemStack.stackSize <= 0;
		}
		else
			return false;
	}

	public static boolean check2(Iterable<List<ItemStack>> list)
	{
		for (List<ItemStack> list2 : list)
		{
			if (!check(list2))
				return false;
		}

		return true;
	}

	public static boolean check(ItemStack[] array)
	{
		return check(Arrays.asList(array));
	}

	public static boolean check(Iterable<ItemStack> list)
	{
		for (ItemStack stack : list)
		{
			if (!check(stack))
				return false;
		}

		return true;
	}

	public static boolean check(ItemStack stack)
	{
		return stack.getItem() != null;
	}

	public static String toStringSafe2(Iterable<List<ItemStack>> list)
	{
		String ret = "[";

		for (List<ItemStack> list2 : list)
		{
			if (ret.length() > 1)
				ret = ret + ", ";

			ret = ret + toStringSafe(list2);
		}

		ret = ret + "]";
		return ret;
	}

	public static String toStringSafe(ItemStack[] array)
	{
		return toStringSafe(Arrays.asList(array));
	}

	public static String toStringSafe(Iterable<ItemStack> list)
	{
		String ret = "[";

		for (ItemStack stack : list)
		{
			if (ret.length() > 1)
				ret = ret + ", ";

			ret = ret + toStringSafe(stack);
		}

		ret = ret + "]";
		return ret;
	}

	public static String toStringSafe(ItemStack stack)
	{
		return stack.getItem() == null ? stack.stackSize + "x(null)@(unknown)" : stack.toString();
	}

	public static void consumeInventoryItem(EntityPlayer player, ItemStack itemStack)
	{
		for (int i = 0; i < player.inventory.mainInventory.length; ++i)
		{
			if (player.inventory.mainInventory[i] != null && player.inventory.mainInventory[i].isItemEqual(itemStack))
			{
				player.inventory.decrStackSize(i, 1);
				return;
			}
		}

	}

	public static boolean storeInventoryItem(ItemStack stack, EntityPlayer player, boolean simulate)
	{
		if (simulate)
			for (int i = 0; i < player.inventory.mainInventory.length; ++i)
			{
				ItemStack invStack = player.inventory.mainInventory[i];
				if (invStack == null || isStackEqualStrict(stack, invStack) && invStack.stackSize + stack.stackSize <= Math.min(player.inventory.getInventoryStackLimit(), invStack.getMaxStackSize()))
					return true;
			}
		else if (player.inventory.addItemStackToInventory(stack))
		{
			if (!IC2.platform.isRendering())
				player.openContainer.detectAndSendChanges();

			return true;
		}

		return false;
	}

	public static class AdjacentInv
	{
		public final IInventory inv;
		public final Direction dir;

		private AdjacentInv(IInventory inv, Direction dir)
		{
			this.inv = inv;
			this.dir = dir;
		}
	}
}
