package ic2.core.block.invslot;

import ic2.core.block.TileEntityInventory;
import ic2.core.item.DamageHandler;
import ic2.core.util.StackUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public abstract class InvSlotConsumable extends InvSlot
{
	public InvSlotConsumable(TileEntityInventory base1, String name1, int oldStartIndex1, int count)
	{
		super(base1, name1, oldStartIndex1, InvSlot.Access.I, count, InvSlot.InvSide.TOP);
	}

	public InvSlotConsumable(TileEntityInventory base1, String name1, int oldStartIndex1, InvSlot.Access access1, int count, InvSlot.InvSide preferredSide1)
	{
		super(base1, name1, oldStartIndex1, access1, count, preferredSide1);
	}

	@Override
	public abstract boolean accepts(ItemStack var1);

	@Override
	public boolean canOutput()
	{
		return super.canOutput() || this.access != InvSlot.Access.NONE && this.get() != null && !this.accepts(this.get());
	}

	public ItemStack consume(int amount)
	{
		return this.consume(amount, false, false);
	}

	public ItemStack consume(int amount, boolean simulate, boolean consumeContainers)
	{
		ItemStack ret = null;

		for (int i = 0; i < this.size(); ++i)
		{
			ItemStack stack = this.get(i);
			if (stack != null && stack.stackSize >= 1 && this.accepts(stack) && (ret == null || StackUtil.isStackEqualStrict(stack, ret)) && (stack.stackSize == 1 || consumeContainers || !stack.getItem().hasContainerItem(stack)))
			{
				int currentAmount = Math.min(amount, stack.stackSize);
				amount -= currentAmount;
				if (!simulate)
					if (stack.stackSize == currentAmount)
					{
						if (!consumeContainers && stack.getItem().hasContainerItem(stack))
							this.put(i, stack.getItem().getContainerItem(stack));
						else
							this.put(i, (ItemStack) null);
					}
					else
						stack.stackSize -= currentAmount;

				if (ret == null)
					ret = StackUtil.copyWithSize(stack, currentAmount);
				else
					ret.stackSize += currentAmount;

				if (amount == 0)
					break;
			}
		}

		return ret;
	}

	public ItemStack damage(int amount, boolean simulate)
	{
		return this.damage(amount, simulate, (EntityLivingBase) null);
	}

	public ItemStack damage(int amount, boolean simulate, EntityLivingBase src)
	{
		ItemStack ret = null;
		int damageApplied = 0;

		for (int i = 0; i < this.size() && amount > 0; ++i)
		{
			ItemStack stack = this.get(i);

			// TODO gamerforEA code start
			if (stack == null)
				continue;
			// TODO gamerforEA code end

			Item item = stack.getItem();

			// TODO gamerforEA code start
			if (item == null)
				continue;
			// TODO gamerforEA code end

			if (stack != null && this.accepts(stack) && item.isDamageable() && (ret == null || stack.getItem() == ret.getItem() && ItemStack.areItemStackTagsEqual(stack, ret)))
			{
				if (simulate)
					stack = stack.copy();

				int maxDamage = DamageHandler.getMaxDamage(stack);

				while (amount > 0 && stack.stackSize > 0)
				{
					int currentAmount = Math.min(amount, maxDamage - DamageHandler.getDamage(stack));
					DamageHandler.damage(stack, currentAmount, src);
					damageApplied += currentAmount;
					amount -= currentAmount;
					if (DamageHandler.getDamage(stack) >= maxDamage)
					{
						--stack.stackSize;
						DamageHandler.setDamage(stack, 0);
					}

					if (ret == null)
						ret = stack.copy();
				}

				if (stack.stackSize == 0 && !simulate)
					this.put(i, (ItemStack) null);
			}
		}

		if (ret != null)
		{
			int max = DamageHandler.getMaxDamage(ret);
			ret.stackSize = damageApplied / max;
			DamageHandler.setDamage(ret, damageApplied % max);
		}

		return ret;
	}
}
