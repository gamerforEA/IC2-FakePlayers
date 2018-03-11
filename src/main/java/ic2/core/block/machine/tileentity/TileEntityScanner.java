package ic2.core.block.machine.tileentity;

import com.gamerforea.ic2.EventConfig;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.Direction;
import ic2.api.network.INetworkClientTileEntityEventListener;
import ic2.api.recipe.IPatternStorage;
import ic2.core.ContainerBase;
import ic2.core.IHasGui;
import ic2.core.Ic2Items;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumable;
import ic2.core.block.invslot.InvSlotConsumableId;
import ic2.core.block.invslot.InvSlotScannable;
import ic2.core.block.machine.container.ContainerScanner;
import ic2.core.block.machine.gui.GuiScanner;
import ic2.core.item.ItemCrystalMemory;
import ic2.core.util.StackUtil;
import ic2.core.uu.UuGraph;
import ic2.core.uu.UuIndex;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class TileEntityScanner extends TileEntityElectricMachine
		implements IHasGui, INetworkClientTileEntityEventListener
{
	private ItemStack currentStack = null;
	private ItemStack pattern = null;
	private final int energyusecycle = 256;
	public int progress = 0;
	public final int duration = 3300;
	public final InvSlotConsumable inputSlot = new InvSlotScannable(this, "input", 0, 1);
	public final InvSlot diskSlot = new InvSlotConsumableId(this, "disk", 0, InvSlot.Access.IO, 1, InvSlot.InvSide.ANY, Ic2Items.crystalmemory.getItem());
	private TileEntityScanner.State state = TileEntityScanner.State.IDLE;
	public double patternUu;
	public double patternEu;

	public TileEntityScanner()
	{
		super(512000, 3, 0);
	}

	@Override
	protected void updateEntityServer()
	{
		super.updateEntityServer();
		boolean newActive = false;
		if (this.progress < 3300)
		{
			if (!this.inputSlot.isEmpty() && (this.currentStack == null || StackUtil.isStackEqual(this.currentStack, this.inputSlot.get())))
			{
				if (this.getPatternStorage() == null && this.diskSlot.isEmpty())
				{
					this.state = TileEntityScanner.State.NO_STORAGE;
					this.reset();
				}
				else if (this.energy >= 256.0D)
				{
					if (this.currentStack == null)
						this.currentStack = StackUtil.copyWithSize(this.inputSlot.get(), 1);

					this.pattern = UuGraph.find(this.currentStack);

					// TODO gameroforEA add condition [2, 3]
					if (this.pattern == null || this.currentStack != null && EventConfig.inList(EventConfig.scannerBlackList, this.currentStack.getItem(), this.currentStack.getItemDamage()))
						this.state = TileEntityScanner.State.FAILED;
					else if (this.isPatternRecorded(this.pattern))
					{
						this.state = TileEntityScanner.State.ALREADY_RECORDED;
						this.reset();
					}
					else
					{
						newActive = true;
						this.state = TileEntityScanner.State.SCANNING;
						this.energy -= 256.0D;
						++this.progress;
						if (this.progress >= 3300)
						{
							this.refreshInfo();
							if (this.patternUu != Double.POSITIVE_INFINITY)
							{
								this.state = TileEntityScanner.State.COMPLETED;
								this.inputSlot.consume(1, false, true);
								this.markDirty();
							}
							else
								this.state = TileEntityScanner.State.FAILED;
						}
					}
				}
				else
					this.state = TileEntityScanner.State.NO_ENERGY;
			}
			else
			{
				this.state = TileEntityScanner.State.IDLE;
				this.reset();
			}
		}
		else if (this.pattern == null)
		{
			this.state = TileEntityScanner.State.IDLE;
			this.progress = 0;
		}

		this.setActive(newActive);
	}

	public void reset()
	{
		this.progress = 0;
		this.currentStack = null;
		this.pattern = null;
	}

	private boolean isPatternRecorded(ItemStack stack)
	{
		if (!this.diskSlot.isEmpty() && this.diskSlot.get().getItem() instanceof ItemCrystalMemory)
		{
			ItemStack crystalMemory = this.diskSlot.get();
			if (StackUtil.isStackEqual(((ItemCrystalMemory) crystalMemory.getItem()).readItemStack(crystalMemory), stack))
				return true;
		}

		IPatternStorage storage = this.getPatternStorage();
		if (storage == null)
			return false;
		else
		{
			for (ItemStack stored : storage.getPatterns())
			{
				if (StackUtil.isStackEqual(stored, stack))
					return true;
			}

			return false;
		}
	}

	private void record()
	{
		if (this.pattern != null && this.patternUu != Double.POSITIVE_INFINITY)
		{
			if (!this.savetoDisk(this.pattern))
			{
				IPatternStorage storage = this.getPatternStorage();
				if (storage == null)
				{
					this.state = TileEntityScanner.State.TRANSFER_ERROR;
					return;
				}

				if (!storage.addPattern(this.pattern))
				{
					this.state = TileEntityScanner.State.TRANSFER_ERROR;
					return;
				}
			}

			this.reset();
		}
		else
			this.reset();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound)
	{
		super.readFromNBT(nbttagcompound);
		this.progress = nbttagcompound.getInteger("progress");
		NBTTagCompound contentTag = nbttagcompound.getCompoundTag("currentStack");
		this.currentStack = ItemStack.loadItemStackFromNBT(contentTag);
		contentTag = nbttagcompound.getCompoundTag("pattern");
		this.pattern = ItemStack.loadItemStackFromNBT(contentTag);
		int stateIdx = nbttagcompound.getInteger("state");
		this.state = stateIdx < TileEntityScanner.State.values().length ? TileEntityScanner.State.values()[stateIdx] : TileEntityScanner.State.IDLE;
		this.refreshInfo();
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound)
	{
		super.writeToNBT(nbttagcompound);
		nbttagcompound.setInteger("progress", this.progress);
		if (this.currentStack != null)
		{
			NBTTagCompound contentTag = new NBTTagCompound();
			this.currentStack.writeToNBT(contentTag);
			nbttagcompound.setTag("currentStack", contentTag);
		}

		if (this.pattern != null)
		{
			NBTTagCompound contentTag = new NBTTagCompound();
			this.pattern.writeToNBT(contentTag);
			nbttagcompound.setTag("pattern", contentTag);
		}

		nbttagcompound.setInteger("state", this.state.ordinal());
	}

	@Override
	public ContainerBase<TileEntityScanner> getGuiContainer(EntityPlayer entityPlayer)
	{
		return new ContainerScanner(entityPlayer, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getGui(EntityPlayer entityPlayer, boolean isAdmin)
	{
		return new GuiScanner(new ContainerScanner(entityPlayer, this));
	}

	@Override
	public void onGuiClosed(EntityPlayer entityPlayer)
	{
	}

	@Override
	public String getInventoryName()
	{
		return "scanner";
	}

	public IPatternStorage getPatternStorage()
	{
		for (Direction direction : Direction.directions)
		{
			TileEntity target = direction.applyToTileEntity(this);
			if (target instanceof IPatternStorage)
				return (IPatternStorage) target;
		}

		return null;
	}

	public boolean savetoDisk(ItemStack stack)
	{
		if (!this.diskSlot.isEmpty() && stack != null)
		{
			if (this.diskSlot.get().getItem() instanceof ItemCrystalMemory)
			{
				ItemStack crystalMemory = this.diskSlot.get();
				((ItemCrystalMemory) crystalMemory.getItem()).writecontentsTag(crystalMemory, stack);
				return true;
			}
			else
				return false;
		}
		else
			return false;
	}

	@Override
	public void onNetworkEvent(EntityPlayer player, int event)
	{
		switch (event)
		{
			case 0:
				this.reset();
				break;
			case 1:
				if (this.progress >= 3300)
					this.record();
		}

	}

	private void refreshInfo()
	{
		if (this.pattern != null)
			this.patternUu = UuIndex.instance.getInBuckets(this.pattern);

	}

	public int getPercentageDone()
	{
		return 100 * this.progress / 3300;
	}

	public int getSubPercentageDoneScaled(int width)
	{
		return width * (100 * this.progress % 3300) / 3300;
	}

	public boolean isDone()
	{
		return this.progress >= 3300;
	}

	public TileEntityScanner.State getState()
	{
		return this.state;
	}

	public enum State
	{
		IDLE,
		SCANNING,
		COMPLETED,
		FAILED,
		NO_STORAGE,
		NO_ENERGY,
		TRANSFER_ERROR,
		ALREADY_RECORDED
	}
}
