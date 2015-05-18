package ic2.core.block.machine.tileentity;

import ic2.api.item.ElectricItem;
import ic2.api.network.INetworkClientTileEntityEventListener;
import ic2.api.recipe.IRecipeInput;
import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.Ic2Items;
import ic2.core.Ic2Player;
import ic2.core.block.IUpgradableBlock;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableId;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.block.machine.container.ContainerAdvMiner;
import ic2.core.block.machine.gui.GuiAdvMiner;
import ic2.core.init.MainConfig;
import ic2.core.item.IUpgradeItem;
import ic2.core.item.tool.ItemScanner;
import ic2.core.util.ConfigUtil;
import ic2.core.util.StackUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.IFluidBlock;

import com.gamerforea.ic2.EventConfig;
import com.gamerforea.ic2.FakePlayerUtils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityAdvMiner extends TileEntityElectricMachine implements IHasGui, INetworkClientTileEntityEventListener, IUpgradableBlock
{
	private final List<ItemStack> itemstack = new ArrayList();
	private int currectblockscanncount;
	private int blockscanncount;
	public final int defaultTier;
	public final int workTick;
	public boolean blacklist = true;
	public boolean silktouch = false;
	public boolean redstonePowered = false;
	public int energyConsume;
	public int xcounter = 99;
	public int zcounter = 99;
	private int minelayer = -1;
	private int minetargetX = -1;
	private int minetargetZ = -1;
	private short ticker = 0;
	public final InvSlotConsumableId scannerSlot;
	public final InvSlotUpgrade upgradeSlot;
	public final InvSlot ListSlot;

	public TileEntityAdvMiner()
	{
		super(4000000, 3, 0);
		this.scannerSlot = new InvSlotConsumableId(this, "scanner", 1, InvSlot.Access.IO, 1, InvSlot.InvSide.BOTTOM, new Item[] { Ic2Items.odScanner.getItem(), Ic2Items.ovScanner.getItem() });
		this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 3, 4);
		this.ListSlot = new InvSlot(this, "list", 8, (InvSlot.Access) null, 15);
		this.energyConsume = 512;
		this.defaultTier = 3;
		this.workTick = 20;
	}

	public void onLoaded()
	{
		super.onLoaded();
		if (IC2.platform.isSimulating())
		{
			if (this.minelayer < 0)
			{
				this.minelayer = this.yCoord - 1;
			}

			this.setUpgradestat();
		}
	}

	private void chargeTool()
	{
		if (!this.scannerSlot.isEmpty())
		{
			this.energy -= ElectricItem.manager.charge(this.scannerSlot.get(), this.energy, 2, false, false);
		}
	}

	public void updateEntity()
	{
		super.updateEntity();
		this.chargeTool();
		this.setUpgradestat();
		if (this.work())
		{
			this.markDirty();
			if (!this.getActive())
			{
				this.setActive(true);
			}
		}
		else if (this.getActive())
		{
			this.setActive(false);
		}
	}

	private boolean work()
	{
		if (this.energy < (double) this.energyConsume)
		{
			return false;
		}
		else if (!this.isRedstonePowered())
		{
			return false;
		}
		else if (this.minelayer == 0)
		{
			return false;
		}
		else if (this.scannerSlot.isEmpty())
		{
			return false;
		}
		else if (this.scannerSlot.get().getItem() instanceof ItemScanner && !((ItemScanner) this.scannerSlot.get().getItem()).haveChargeforScan(this.scannerSlot.get()))
		{
			return false;
		}
		else
		{
			byte range = 0;
			if (this.scannerSlot.get().getItem() == Ic2Items.odScanner.getItem())
			{
				range = 16;
			}

			if (this.scannerSlot.get().getItem() == Ic2Items.ovScanner.getItem())
			{
				range = 32;
			}

			if (this.ticker == this.workTick)
			{
				this.currectblockscanncount = this.blockscanncount;

				while (this.minelayer > 0 && this.currectblockscanncount > 0)
				{
					if (this.xcounter == 99)
					{
						this.xcounter = 0 - range / 2;
					}

					if (this.zcounter == 99)
					{
						this.zcounter = 0 - range / 2;
					}

					if (this.xcounter <= range / 2)
					{
						if (this.zcounter < range / 2)
						{
							this.minetargetX = this.xCoord - this.xcounter;
							this.minetargetZ = this.zCoord - this.zcounter;
							++this.zcounter;
						}
						else
						{
							this.minetargetX = this.xCoord - this.xcounter;
							this.minetargetZ = this.zCoord - this.zcounter;
							++this.xcounter;
							this.zcounter = 0 - range / 2;
						}

						if (this.scannerSlot.get().getItem() instanceof ItemScanner)
						{
							((ItemScanner) this.scannerSlot.get().getItem()).discharge(this.scannerSlot.get(), 64);
						}

						Block block = this.worldObj.getBlock(this.minetargetX, this.minelayer, this.minetargetZ);
						if (!block.isAir(this.worldObj, this.minetargetX, this.minelayer, this.minetargetZ) && this.canMine(this.minetargetX, this.minelayer, this.minetargetZ, block))
						{
							this.doMine(block);
							break;
						}

						--this.currectblockscanncount;
					}
					else
					{
						--this.minelayer;
						this.xcounter = 0 - range / 2;
						this.zcounter = 0 - range / 2;
					}
				}

				this.ticker = 0;
			}
			else
			{
				++this.ticker;
			}

			return true;
		}
	}

	public void doMine(Block block)
	{
		// TODO gamerforEA code start
		if (EventConfig.advminerEvent && FakePlayerUtils.cantBreak(this.minetargetX, this.minelayer, this.minetargetZ, this.getOwnerFake())) return;
		// TODO gamerforEA code end

		if (this.silktouch && block.canSilkHarvest(this.worldObj, new Ic2Player(this.worldObj), this.minetargetX, this.minelayer, this.minetargetZ, this.worldObj.getBlockMetadata(this.minetargetX, this.minelayer, this.minetargetZ)))
		{
			if (Item.getItemFromBlock(block) != null && StackUtil.check(new ItemStack(Item.getItemFromBlock(block), 1, this.worldObj.getBlockMetadata(this.minetargetX, this.minelayer, this.minetargetZ))))
			{
				StackUtil.distribute(this, new ItemStack(Item.getItemFromBlock(block), 1, this.worldObj.getBlockMetadata(this.minetargetX, this.minelayer, this.minetargetZ)), false);
			}
		}
		else
		{
			StackUtil.distributeDrop(this, block.getDrops(this.worldObj, this.minetargetX, this.minelayer, this.minetargetZ, this.worldObj.getBlockMetadata(this.minetargetX, this.minelayer, this.minetargetZ), 0));
		}

		this.worldObj.setBlockToAir(this.minetargetX, this.minelayer, this.minetargetZ);
		this.energy -= (double) this.energyConsume;
	}

	public boolean canMine(int x, int y, int z, Block block)
	{
		this.itemstack.clear();
		if (block.hasTileEntity(this.worldObj.getBlockMetadata(this.minetargetX, this.minelayer, this.minetargetZ)))
		{
			ItemStack i = new ItemStack(block, 1, this.worldObj.getBlockMetadata(this.minetargetX, this.minelayer, this.minetargetZ));
			int max = 0;
			Iterator i$ = IC2.valuableOres.entrySet().iterator();

			while (i$.hasNext())
			{
				Entry entry = (Entry) i$.next();
				if (((IRecipeInput) entry.getKey()).matches(i))
				{
					++max;
				}
			}

			if (max == 0)
			{
				return false;
			}
		}

		if (!(block instanceof IFluidBlock) && !(block instanceof BlockFluidClassic) && !(block instanceof BlockStaticLiquid) && !(block instanceof BlockDynamicLiquid))
		{
			if (block.getBlockHardness(this.worldObj, x, y, z) < 0.0F)
			{
				return false;
			}
			else
			{
				if (this.silktouch && block.canSilkHarvest(this.worldObj, new Ic2Player(this.worldObj), this.minetargetX, this.minelayer, this.minetargetZ, this.worldObj.getBlockMetadata(this.minetargetX, this.minelayer, this.minetargetZ)))
				{
					if (Item.getItemFromBlock(block) == null)
					{
						return false;
					}

					if (!StackUtil.check(new ItemStack(Item.getItemFromBlock(block), 1, this.worldObj.getBlockMetadata(this.minetargetX, this.minelayer, this.minetargetZ))))
					{
						return false;
					}

					this.itemstack.add(new ItemStack(Item.getItemFromBlock(block), 1, this.worldObj.getBlockMetadata(this.minetargetX, this.minelayer, this.minetargetZ)));
				}
				else
				{
					this.itemstack.addAll(block.getDrops(this.worldObj, this.minetargetX, this.minelayer, this.minetargetZ, this.worldObj.getBlockMetadata(this.minetargetX, this.minelayer, this.minetargetZ), 0));
				}

				if (!this.itemstack.isEmpty())
				{
					int var9;
					if (this.blacklist)
					{
						for (var9 = 0; var9 < this.ListSlot.size(); ++var9)
						{
							if (this.ListSlot.get(var9) != null && StackUtil.isStackEqual((ItemStack) this.itemstack.get(0), this.ListSlot.get(var9)))
							{
								return false;
							}
						}

						return true;
					}
					else
					{
						for (var9 = 0; var9 < this.ListSlot.size(); ++var9)
						{
							if (this.ListSlot.get(var9) != null && StackUtil.isStackEqual((ItemStack) this.itemstack.get(0), this.ListSlot.get(var9)))
							{
								return true;
							}
						}

						return false;
					}
				}
				else
				{
					return false;
				}
			}
		}
		else
		{
			return false;
		}
	}

	public void readFromNBT(NBTTagCompound nbtTagCompound)
	{
		super.readFromNBT(nbtTagCompound);
		this.minelayer = nbtTagCompound.getInteger("minelayer");
		this.xcounter = nbtTagCompound.getInteger("xcounter");
		this.zcounter = nbtTagCompound.getInteger("zcounter");
		this.blacklist = nbtTagCompound.getBoolean("blacklist");
		this.silktouch = nbtTagCompound.getBoolean("silktouch");
	}

	public void writeToNBT(NBTTagCompound nbtTagCompound)
	{
		super.writeToNBT(nbtTagCompound);
		nbtTagCompound.setInteger("minelayer", this.minelayer);
		nbtTagCompound.setInteger("xcounter", this.xcounter);
		nbtTagCompound.setInteger("zcounter", this.zcounter);
		nbtTagCompound.setBoolean("blacklist", this.blacklist);
		nbtTagCompound.setBoolean("silktouch", this.silktouch);
	}

	public void onNetworkEvent(EntityPlayer player, int event)
	{
		switch (event)
		{
			case 0:
				if (!this.getActive())
				{
					this.minelayer = this.yCoord - 1;
					this.xcounter = 99;
					this.zcounter = 99;
				}
				break;
			case 1:
				if (!this.getActive())
				{
					this.blacklist = !this.blacklist;
				}
				break;
			case 2:
				if (!this.getActive())
				{
					this.silktouch = !this.silktouch;
				}
		}
	}

	public void setUpgradestat()
	{
		int extraTier = 0;
		int scannMultiplier = 1;
		this.redstonePowered = false;

		for (int i = 0; i < this.upgradeSlot.size(); ++i)
		{
			ItemStack stack = this.upgradeSlot.get(i);
			if (stack != null && stack.getItem() instanceof IUpgradeItem)
			{
				IUpgradeItem upgrade = (IUpgradeItem) stack.getItem();
				extraTier += upgrade.getExtraTier(stack, this) * stack.stackSize;
				if (stack.getItemDamage() == 0)
				{
					scannMultiplier += stack.stackSize;
				}

				if (((IUpgradeItem) stack.getItem()).useRedstoneinverter(stack, this))
				{
					this.redstonePowered = true;
				}
			}
		}

		this.setTier(applyModifier(this.defaultTier, extraTier, 1.0D));
		this.blockscanncount = 5 * scannMultiplier;
		this.setRedstonePowered(this.redstonePowered);
	}

	private static int applyModifier(int base, int extra, double multiplier)
	{
		double ret = (double) Math.round(((double) base + (double) extra) * multiplier);
		return ret > 2.147483647E9D ? Integer.MAX_VALUE : (int) ret;
	}

	public ContainerBase<TileEntityAdvMiner> getGuiContainer(EntityPlayer entityPlayer)
	{
		return new ContainerAdvMiner(entityPlayer, this);
	}

	@SideOnly(Side.CLIENT)
	public GuiScreen getGui(EntityPlayer entityPlayer, boolean isAdmin)
	{
		return new GuiAdvMiner(new ContainerAdvMiner(entityPlayer, this));
	}

	public void onGuiClosed(EntityPlayer entityPlayer)
	{
	}

	public boolean isRedstonePowered()
	{
		return this.redstonePowered ? !super.isRedstonePowered() : super.isRedstonePowered();
	}

	public void setRedstonePowered(boolean redstone)
	{
		if (this.redstonePowered != redstone)
		{
			this.redstonePowered = redstone;
		}
	}

	public double getEnergy()
	{
		return this.energy;
	}

	public boolean useEnergy(double amount)
	{
		if (this.energy >= amount)
		{
			this.energy -= amount;
			return true;
		}
		else
		{
			return false;
		}
	}

	public int getOutputSize()
	{
		return 0;
	}

	public ItemStack getOutput(int index)
	{
		return null;
	}

	public void setOutput(int index, ItemStack stack)
	{
	}

	public String getInventoryName()
	{
		return "AdvMiner";
	}

	public int getminelayer()
	{
		return this.minelayer;
	}

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

	public List<ItemStack> getCompatibleUpgradeList()
	{
		ArrayList itemstack = new ArrayList();
		itemstack.add(Ic2Items.overclockerUpgrade);
		itemstack.add(Ic2Items.redstoneinvUpgrade);
		return itemstack;
	}
}