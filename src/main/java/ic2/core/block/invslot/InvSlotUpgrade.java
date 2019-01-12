package ic2.core.block.invslot;

import com.gamerforea.ic2.EventConfig;
import ic2.core.Ic2Items;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Redstone;
import ic2.core.block.comp.TileEntityComponent;
import ic2.core.upgrade.IUpgradableBlock;
import ic2.core.upgrade.IUpgradeItem;
import ic2.core.util.StackUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class InvSlotUpgrade extends InvSlot
{
	public int augmentation;
	public int extraProcessTime;
	public double processTimeMultiplier;
	public int extraEnergyDemand;
	public double energyDemandMultiplier;
	public int extraEnergyStorage;
	public double energyStorageMultiplier;
	public int extraTier;

	public InvSlotUpgrade(TileEntityInventory base1, String name1, int oldStartIndex1, int count)
	{
		super(base1, name1, oldStartIndex1, InvSlot.Access.NONE, count);
		if (!(base1 instanceof IUpgradableBlock))
			throw new IllegalArgumentException("Base needs to be an IUpgradableBlock.");
		this.resetRates();
	}

	@Override
	public boolean accepts(ItemStack stack)
	{
		Item rawItem = stack.getItem();
		if (!(rawItem instanceof IUpgradeItem))
			return false;
		IUpgradeItem item = (IUpgradeItem) rawItem;

		// TODO gamerforEA code start
		int maxOverclockerCount = EventConfig.maxOverclockerCount;
		if (maxOverclockerCount > 0 && stack.isItemEqual(Ic2Items.overclockerUpgrade))
		{
			for (int slot = 0, count = 0; slot < this.size(); slot++)
			{
				ItemStack stackInSlot = this.get(slot);
				if (stackInSlot != null && stackInSlot.stackSize > 0 && stackInSlot.isItemEqual(Ic2Items.overclockerUpgrade))
				{
					count += stackInSlot.stackSize;
					if (count >= maxOverclockerCount)
						return false;
				}
			}
		}
		// TODO gamerforEA code end

		return item.isSuitableFor(stack, ((IUpgradableBlock) this.base).getUpgradableProperties());
	}

	// TODO gamerforEA code start
	private final ThreadLocal<Boolean> onChangedLock = new ThreadLocal<>();
	// TODO gamerforEA code end

	@Override
	public void onChanged()
	{
		// TODO gamerforEA code start
		int maxOverclockerCount = EventConfig.maxOverclockerCount;
		if (maxOverclockerCount > 0 && Boolean.TRUE.equals(this.onChangedLock.get()))
			return;
		// TODO gamerforEA code end

		this.resetRates();

		// TODO gamerforEA code start
		if (maxOverclockerCount > 0)
		{
			this.onChangedLock.set(true);
			try
			{
				for (int slot = 0, count = 0; slot < this.size(); slot++)
				{
					ItemStack stackInSlot = this.get(slot);
					if (stackInSlot != null && stackInSlot.stackSize > 0 && stackInSlot.isItemEqual(Ic2Items.overclockerUpgrade))
					{
						if (count >= maxOverclockerCount)
						{
							StackUtil.dropAsEntity(this.base.getWorldObj(), this.base.xCoord, this.base.yCoord, this.base.zCoord, stackInSlot.copy());
							stackInSlot.stackSize = 0;
							this.put(slot, null);
						}
						else
						{
							int newCount = count + stackInSlot.stackSize;
							if (newCount > maxOverclockerCount)
							{
								int dropCount = newCount - maxOverclockerCount;
								newCount = maxOverclockerCount;
								ItemStack stackToDrop = StackUtil.copyWithSize(stackInSlot, dropCount);
								stackInSlot.stackSize -= dropCount;
								if (stackInSlot.stackSize <= 0)
									this.put(slot, null);
								StackUtil.dropAsEntity(this.base.getWorldObj(), this.base.xCoord, this.base.yCoord, this.base.zCoord, stackToDrop);
							}
							count = newCount;
						}
					}
				}
			}
			finally
			{
				this.onChangedLock.set(false);
			}
		}
		// TODO gamerforEA code end

		final IUpgradableBlock block = (IUpgradableBlock) this.base;
		List<Redstone.IRedstoneModifier> redstoneModifiers = null;

		for (int i = 0; i < this.size(); ++i)
		{
			final ItemStack stack = this.get(i);

			// TODO gamerforEA add condition [2]
			if (stack != null && stack.stackSize > 0 && this.accepts(stack))
			{
				final IUpgradeItem upgrade = (IUpgradeItem) stack.getItem();
				this.augmentation += upgrade.getAugmentation(stack, block) * stack.stackSize;
				this.extraProcessTime += upgrade.getExtraProcessTime(stack, block) * stack.stackSize;
				this.processTimeMultiplier *= Math.pow(upgrade.getProcessTimeMultiplier(stack, block), (double) stack.stackSize);
				this.extraEnergyDemand += upgrade.getExtraEnergyDemand(stack, block) * stack.stackSize;
				this.energyDemandMultiplier *= Math.pow(upgrade.getEnergyDemandMultiplier(stack, block), (double) stack.stackSize);
				this.extraEnergyStorage += upgrade.getExtraEnergyStorage(stack, block) * stack.stackSize;
				this.energyStorageMultiplier *= Math.pow(upgrade.getEnergyStorageMultiplier(stack, block), (double) stack.stackSize);
				this.extraTier += upgrade.getExtraTier(stack, block) * stack.stackSize;
				if (upgrade.modifiesRedstoneInput(stack, block))
				{
					if (redstoneModifiers == null)
						redstoneModifiers = new ArrayList<>(this.size());
					redstoneModifiers.add(redstoneInput -> upgrade.getRedstoneInput(stack, block, redstoneInput));
				}
			}
		}

		for (TileEntityComponent component : this.base.getComponents())
		{
			if (component instanceof Redstone)
				((Redstone) component).setRedstoneModifier(redstoneModifiers);
		}
	}

	private void resetRates()
	{
		this.augmentation = 0;
		this.extraProcessTime = 0;
		this.processTimeMultiplier = 1.0D;
		this.extraEnergyDemand = 0;
		this.energyDemandMultiplier = 1.0D;
		this.extraEnergyStorage = 0;
		this.energyStorageMultiplier = 1.0D;
		this.extraTier = 0;
	}
}
