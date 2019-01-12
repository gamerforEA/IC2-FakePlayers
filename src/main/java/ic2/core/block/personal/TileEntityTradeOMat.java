package ic2.core.block.personal;

import com.gamerforea.ic2.EventConfig;
import com.mojang.authlib.GameProfile;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.network.INetworkClientTileEntityEventListener;
import ic2.api.network.INetworkTileEntityEventListener;
import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.audio.PositionSpec;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLinked;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.util.LogCategory;
import ic2.core.util.StackUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.Vector;

public class TileEntityTradeOMat extends TileEntityInventory
		implements IPersonalBlock, IHasGui, INetworkTileEntityEventListener, INetworkClientTileEntityEventListener
{
	private int ticker = IC2.random.nextInt(64);
	private GameProfile owner = null;
	public int totalTradeCount = 0;
	public int stock = 0;
	public boolean infinite = false;
	private static final int stockUpdateRate = 64;
	private static final int EventTrade = 0;

	// TODO gamerforEA move initializer to constructor, old code: new InvSlot(this, "demand", 0, InvSlot.Access.NONE, 1)
	public final InvSlot demandSlot;

	// TODO gamerforEA move initializer to constructor, old code: new InvSlot(this, "offer", 1, InvSlot.Access.NONE, 1)
	public final InvSlot offerSlot;

	public final InvSlotConsumableLinked inputSlot;
	public final InvSlotOutput outputSlot;

	// TODO gamerforEA code start
	public boolean isOpened;
	// TODO gamerforEA code end

	public TileEntityTradeOMat()
	{
		/* TODO gamerforEA code replace, old code:
		this.inputSlot = new InvSlotConsumableLinked(this, "input", 2, 1, this.demandSlot); */
		this.demandSlot = new InvSlot(this, "demand", 0, InvSlot.Access.NONE, 1)
		{
			@Override
			public boolean accepts(ItemStack stack)
			{
				return !EventConfig.tradeOMatBlackList.contains(stack) && super.accepts(stack);
			}
		};

		this.offerSlot = new InvSlot(this, "offer", 1, InvSlot.Access.NONE, 1)
		{
			@Override
			public boolean accepts(ItemStack stack)
			{
				return !EventConfig.tradeOMatBlackList.contains(stack) && super.accepts(stack);
			}
		};

		this.inputSlot = new InvSlotConsumableLinked(this, "input", 2, 1, this.demandSlot)
		{
			@Override
			public boolean accepts(ItemStack stack)
			{
				return !EventConfig.tradeOMatBlackList.contains(stack) && super.accepts(stack);
			}
		};
		// TODO gamerforEA code end

		this.outputSlot = new InvSlotOutput(this, "output", 3, 1);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		if (nbt.hasKey("ownerGameProfile"))
			this.owner = NBTUtil.func_152459_a(nbt.getCompoundTag("ownerGameProfile"));

		this.totalTradeCount = nbt.getInteger("totalTradeCount");
		if (nbt.hasKey("infinite"))
			this.infinite = nbt.getBoolean("infinite");

	}

	@Override
	public void writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		if (this.owner != null)
		{
			NBTTagCompound ownerNbt = new NBTTagCompound();
			NBTUtil.func_152460_a(ownerNbt, this.owner);
			nbt.setTag("ownerGameProfile", ownerNbt);
		}

		nbt.setInteger("totalTradeCount", this.totalTradeCount);
		if (this.infinite)
			nbt.setBoolean("infinite", this.infinite);

	}

	@Override
	public List<String> getNetworkedFields()
	{
		List<String> ret = new Vector(1);
		ret.add("owner");
		ret.addAll(super.getNetworkedFields());
		return ret;
	}

	@Override
	protected void updateEntityServer()
	{
		super.updateEntityServer();
		this.trade();
		if (this.infinite)
			this.stock = -1;
		else if (this.ticker++ % 64 == 0)
			this.updateStock();

	}

	private void trade()
	{
		ItemStack tradedIn = this.inputSlot.consumeLinked(true);
		if (tradedIn != null && tradedIn.stackSize > 0)
		{
			ItemStack offer = this.offerSlot.get();
			if (offer != null && offer.stackSize > 0)
				if (this.outputSlot.canAdd(offer))
				{
					if (this.infinite)
					{
						this.inputSlot.consumeLinked(false);
						this.outputSlot.add(offer);
					}
					else
					{
						ItemStack transferredIn = StackUtil.fetch(this, offer, true);
						if (transferredIn == null || transferredIn.stackSize != offer.stackSize)
							return;

						int transferredOut = StackUtil.distribute(this, tradedIn, true);
						if (transferredOut != tradedIn.stackSize)
							return;

						transferredIn = StackUtil.fetch(this, offer, false);
						if (transferredIn == null)
							return;

						if (transferredIn.stackSize != offer.stackSize)
						{
							IC2.log.warn(LogCategory.Block, "The Trade-O-Mat at dim %d, %d/%d/%d received an inconsistent result from an adjacent trade supply inventory, the item stack %s will be lost.", Integer.valueOf(this.getWorldObj().provider.dimensionId), Integer.valueOf(this.xCoord), Integer.valueOf(this.yCoord), Integer.valueOf(this.zCoord), transferredIn);
							return;
						}

						StackUtil.distribute(this, this.inputSlot.consumeLinked(false), false);
						this.outputSlot.add(transferredIn);
						this.stock -= offer.stackSize;
					}

					++this.totalTradeCount;
					IC2.network.get().initiateTileEntityEvent(this, 0, true);
					this.markDirty();
				}
		}
	}

	@Override
	public void onLoaded()
	{
		super.onLoaded();
		if (IC2.platform.isSimulating())
			this.updateStock();

	}

	public void updateStock()
	{
		this.stock = 0;
		ItemStack offer = this.offerSlot.get();
		if (offer != null)
		{
			ItemStack available = StackUtil.fetch(this, StackUtil.copyWithSize(offer, Integer.MAX_VALUE), true);
			if (available == null)
				this.stock = 0;
			else
				this.stock = available.stackSize;
		}

	}

	@Override
	public boolean wrenchCanRemove(EntityPlayer entityPlayer)
	{
		return this.permitsAccess(entityPlayer.getGameProfile());
	}

	@Override
	public boolean permitsAccess(GameProfile profile)
	{
		if (profile == null)
			return this.owner == null;
		else
		{
			if (IC2.platform.isSimulating())
			{
				if (this.owner == null)
				{
					this.owner = profile;
					IC2.network.get().updateTileEntityField(this, "owner");
					return true;
				}

				if (MinecraftServer.getServer().getConfigurationManager().func_152596_g(profile))
					return true;
			}
			else if (this.owner == null)
				return true;

			return this.owner.equals(profile);
		}
	}

	@Override
	public GameProfile getOwner()
	{
		return this.owner;
	}

	@Override
	public String getInventoryName()
	{
		return "Trade-O-Mat";
	}

	@Override
	public ContainerBase<TileEntityTradeOMat> getGuiContainer(EntityPlayer entityPlayer)
	{
		return this.permitsAccess(entityPlayer.getGameProfile()) ? new ContainerTradeOMatOpen(entityPlayer, this) : new ContainerTradeOMatClosed(entityPlayer, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getGui(EntityPlayer entityPlayer, boolean isAdmin)
	{
		return !isAdmin && !this.permitsAccess(entityPlayer.getGameProfile()) ? new GuiTradeOMatClosed(new ContainerTradeOMatClosed(entityPlayer, this)) : new GuiTradeOMatOpen(new ContainerTradeOMatOpen(entityPlayer, this), isAdmin);
	}

	@Override
	public void onGuiClosed(EntityPlayer entityPlayer)
	{
	}

	@Override
	public void onNetworkEvent(int event)
	{
		switch (event)
		{
			case 0:
				IC2.audioManager.playOnce(this, PositionSpec.Center, "Machines/o-mat.ogg", true, IC2.audioManager.getDefaultVolume());
				break;
			default:
				IC2.platform.displayError("An unknown event type was received over multiplayer.\nThis could happen due to corrupted data or a bug.\n\n(Technical information: event ID " + event + ", tile entity below)\n" + "T: " + this + " (" + this.xCoord + ", " + this.yCoord + ", " + this.zCoord + ")");
		}

	}

	@Override
	public void onNetworkEvent(EntityPlayer player, int event)
	{
		if (event == 0)
		{
			MinecraftServer server = MinecraftServer.getServer();
			if (server.getConfigurationManager().func_152596_g(player.getGameProfile()))
			{
				this.infinite = !this.infinite;
				if (!this.infinite)
					this.updateStock();
			}
		}

	}
}
