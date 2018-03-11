package ic2.core.block.personal;

import com.gamerforea.ic2.EventConfig;
import com.gamerforea.ic2.ModUtils;
import com.mojang.authlib.GameProfile;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlot;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.Vector;

public class TileEntityPersonalChest extends TileEntityInventory implements IPersonalBlock, IHasGui
{
	private int ticksSinceSync;
	private int numUsingPlayers;
	public float lidAngle;
	public float prevLidAngle;
	private GameProfile owner = null;
	public final InvSlot contentSlot = new InvSlot(this, "content", 0, InvSlot.Access.NONE, 54);

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		if (nbt.hasKey("ownerGameProfile"))
			this.owner = NBTUtil.func_152459_a(nbt.getCompoundTag("ownerGameProfile"));

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

	}

	@Override
	public String getInventoryName()
	{
		return "Personal Safe";
	}

	@Override
	protected void updateEntityServer()
	{
		super.updateEntityServer();
		if (++this.ticksSinceSync % 20 * 4 == 0 && IC2.platform.isSimulating())
			this.syncNumUsingPlayers();

	}

	@Override
	protected void updateEntityClient()
	{
		super.updateEntityClient();
		this.prevLidAngle = this.lidAngle;
		float var1 = 0.1F;
		if (this.numUsingPlayers > 0 && this.lidAngle == 0.0F)
		{
			double var2 = this.xCoord + 0.5D;
			double var4 = this.zCoord + 0.5D;
			this.worldObj.playSoundEffect(var2, this.yCoord + 0.5D, var4, "random.chestopen", 0.5F, this.worldObj.rand.nextFloat() * 0.1F + 0.9F);
		}

		if (this.numUsingPlayers == 0 && this.lidAngle > 0.0F || this.numUsingPlayers > 0 && this.lidAngle < 1.0F)
		{
			float var8 = this.lidAngle;
			if (this.numUsingPlayers > 0)
				this.lidAngle += var1;
			else
				this.lidAngle -= var1;

			if (this.lidAngle > 1.0F)
				this.lidAngle = 1.0F;

			float var3 = 0.5F;
			if (this.lidAngle < var3 && var8 >= var3)
			{
				double var4 = this.xCoord + 0.5D;
				double var6 = this.zCoord + 0.5D;
				this.worldObj.playSoundEffect(var4, this.yCoord + 0.5D, var6, "random.chestclosed", 0.5F, this.worldObj.rand.nextFloat() * 0.1F + 0.9F);
			}

			if (this.lidAngle < 0.0F)
				this.lidAngle = 0.0F;
		}

	}

	@Override
	public void openInventory()
	{
		++this.numUsingPlayers;
		this.syncNumUsingPlayers();
	}

	@Override
	public void closeInventory()
	{
		--this.numUsingPlayers;
		this.syncNumUsingPlayers();
	}

	@Override
	public boolean receiveClientEvent(int event, int data)
	{
		if (event == 1)
		{
			this.numUsingPlayers = data;
			return true;
		}
		else
			return false;
	}

	private void syncNumUsingPlayers()
	{
		this.worldObj.addBlockEvent(this.xCoord, this.yCoord, this.zCoord, this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord), 1, this.numUsingPlayers);
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
	public boolean wrenchCanRemove(EntityPlayer player)
	{
		if (!this.permitsAccess(player.getGameProfile()))
		{
			IC2.platform.messagePlayer(player, "This safe is owned by " + this.owner.getName());
			return false;
		}
		else if (!this.contentSlot.isEmpty())
		{
			IC2.platform.messagePlayer(player, "Can\'t wrench non-empty safe");
			return false;
		}
		else
			return true;
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

				// TODO gamerforEA code start
				if (ModUtils.hasPermission(profile.getId(), EventConfig.safeAccessPermission))
					return true;
				// TODO gamerforEA code end
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
	public ContainerBase<TileEntityPersonalChest> getGuiContainer(EntityPlayer entityPlayer)
	{
		return new ContainerPersonalChest(entityPlayer, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getGui(EntityPlayer entityPlayer, boolean isAdmin)
	{
		return new GuiPersonalChest(new ContainerPersonalChest(entityPlayer, this));
	}

	@Override
	public void onGuiClosed(EntityPlayer entityPlayer)
	{
	}

	@Override
	public boolean shouldRenderInPass(int pass)
	{
		return pass == 0;
	}
}
