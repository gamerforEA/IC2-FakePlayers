package ic2.core.block;

import ic2.api.network.INetworkDataProvider;
import ic2.api.network.INetworkUpdateListener;
import ic2.api.tile.IWrenchable;
import ic2.core.IC2;
import ic2.core.ITickCallback;
import ic2.core.Ic2Items;
import ic2.core.migration.BlockMigrate;
import ic2.core.network.NetworkManager;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.Vector;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.gamerforea.ic2.FakePlayerUtils;
import com.google.common.base.Strings;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityBlock extends TileEntity implements INetworkDataProvider, INetworkUpdateListener, IWrenchable
{
	public int tileEntityId;
	private boolean active = false;
	private short facing = 0;
	public boolean prevActive = false;
	public short prevFacing = 0;
	public boolean loaded = false;
	@SideOnly(Side.CLIENT)
	private IIcon[] lastRenderIcons;
	private int tesrMask;
	public int tesrTtl;
	private static final int defaultTesrTtl = 500;

	// TODO gamerforEA code start
	public GameProfile ownerProfile;
	private FakePlayer ownerFake;

	public FakePlayer getOwnerFake()
	{
		if (this.ownerFake != null) return this.ownerFake;
		else if (this.ownerProfile != null) return this.ownerFake = FakePlayerUtils.create(this.worldObj, this.ownerProfile);
		else return FakePlayerUtils.getModFake(this.worldObj);
	}
	// TODO gamerforEA code end

	public void validate()
	{
		super.validate();
		IC2.tickHandler.addSingleTickCallback(this.worldObj, new ITickCallback()
		{
			public void tickCallback(World world)
			{
				if (!TileEntityBlock.this.isInvalid() && world.blockExists(TileEntityBlock.this.xCoord, TileEntityBlock.this.yCoord, TileEntityBlock.this.zCoord))
				{
					TileEntityBlock.this.onLoaded();
					if (!TileEntityBlock.this.isInvalid() && TileEntityBlock.this.enableUpdateEntity())
					{
						world.loadedTileEntityList.add(TileEntityBlock.this);
					}
				}
			}
		});
	}

	public void invalidate()
	{
		if (this.loaded)
		{
			this.onUnloaded();
		}

		super.invalidate();
	}

	public void onChunkUnload()
	{
		if (this.loaded)
		{
			this.onUnloaded();
		}

		super.onChunkUnload();
	}

	public void onLoaded()
	{
		this.loaded = true;
		Block block = this.getBlockType();
		if (block instanceof BlockMigrate)
		{
			((BlockMigrate) block).migrate(this.worldObj, this.xCoord, this.yCoord, this.zCoord);
		}
	}

	public void onUnloaded()
	{
		this.loaded = false;
	}

	public void readFromNBT(NBTTagCompound nbttagcompound)
	{
		super.readFromNBT(nbttagcompound);
		this.tileEntityId = nbttagcompound.getInteger("tileEntityId");
		this.prevFacing = this.facing = nbttagcompound.getShort("facing");
		this.prevActive = this.active = nbttagcompound.getBoolean("active");
		// TODO gamerforEA code start
		String uuid = nbttagcompound.getString("ownerUUID");
		if (!Strings.isNullOrEmpty(uuid))
		{
			String name = nbttagcompound.getString("ownerName");
			if (!Strings.isNullOrEmpty(name)) this.ownerProfile = new GameProfile(UUID.fromString(uuid), name);
		}
		// TODO gamerforEA code end
	}

	public void writeToNBT(NBTTagCompound nbttagcompound)
	{
		super.writeToNBT(nbttagcompound);
		nbttagcompound.setInteger("tileEntityId", this.tileEntityId);
		nbttagcompound.setShort("facing", this.facing);
		nbttagcompound.setBoolean("active", this.active);
		// TODO gamerforEA code start
		if (this.ownerProfile != null)
		{
			nbttagcompound.setString("ownerUUID", this.ownerProfile.getId().toString());
			nbttagcompound.setString("ownerName", this.ownerProfile.getName());
		}
		// TODO gamerforEA code end
	}

	public final boolean canUpdate()
	{
		return false;
	}

	public boolean enableUpdateEntity()
	{
		return false;
	}

	@SideOnly(Side.CLIENT)
	public void onRender()
	{
		Block block = this.getBlockType();
		if (this.lastRenderIcons == null)
		{
			this.lastRenderIcons = new IIcon[6];
		}

		for (int side = 0; side < 6; ++side)
		{
			this.lastRenderIcons[side] = block.getIcon(this.worldObj, this.xCoord, this.yCoord, this.zCoord, side);
		}

		this.tesrMask = 0;
	}

	public boolean shouldRefresh(Block oldBlock, Block newBlock, int oldMeta, int newMeta, World world, int x, int y, int z)
	{
		return !(oldBlock instanceof BlockMigrate) || !(newBlock instanceof BlockTileEntity);
	}

	public boolean getActive()
	{
		return this.active;
	}

	public void setActive(boolean active1)
	{
		this.active = active1;
		if (this.prevActive != active1)
		{
			((NetworkManager) IC2.network.get()).updateTileEntityField(this, "active");
		}

		this.prevActive = active1;
	}

	public short getFacing()
	{
		return this.facing;
	}

	public List<String> getNetworkedFields()
	{
		Vector ret = new Vector(2);
		ret.add("tileEntityId");
		ret.add("active");
		ret.add("facing");
		return ret;
	}

	public void onNetworkUpdate(String field)
	{
		if (field.equals("active") && this.prevActive != this.active || field.equals("facing") && this.prevFacing != this.facing)
		{
			int reRenderMask = 0;
			Block block = this.getBlockType();
			if (this.lastRenderIcons == null)
			{
				reRenderMask = -1;
			}
			else
			{
				for (int side = 0; side < 6; ++side)
				{
					IIcon oldIcon = this.lastRenderIcons[side];
					if (oldIcon instanceof BlockTextureStitched)
					{
						oldIcon = ((BlockTextureStitched) oldIcon).getRealTexture();
					}

					IIcon newIcon = block.getIcon(this.worldObj, this.xCoord, this.yCoord, this.zCoord, side);
					if (newIcon instanceof BlockTextureStitched)
					{
						newIcon = ((BlockTextureStitched) newIcon).getRealTexture();
					}

					if (oldIcon != newIcon)
					{
						reRenderMask |= 1 << side;
					}
				}
			}

			if (reRenderMask != 0)
			{
				if (reRenderMask >= 0 && this.prevFacing == this.facing && block.getRenderType() == IC2.platform.getRenderId("default"))
				{
					this.tesrMask = reRenderMask;
					this.tesrTtl = 500;
				}
				else
				{
					this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
				}
			}

			this.prevActive = this.active;
			this.prevFacing = this.facing;
		}

	}

	public boolean wrenchCanSetFacing(EntityPlayer entityPlayer, int side)
	{
		return false;
	}

	public void setFacing(short facing1)
	{
		this.facing = facing1;
		if (this.prevFacing != facing1)
		{
			((NetworkManager) IC2.network.get()).updateTileEntityField(this, "facing");
		}

		this.prevFacing = facing1;
	}

	public boolean wrenchCanRemove(EntityPlayer entityPlayer)
	{
		return true;
	}

	public float getWrenchDropRate()
	{
		return 1.0F;
	}

	public ItemStack getWrenchDrop(EntityPlayer entityPlayer)
	{
		return new ItemStack(this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord), 1, this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord));
	}

	public boolean shouldRenderInPass(int pass)
	{
		return this.tesrMask != 0 && pass == 0;
	}

	public final int getTesrMask()
	{
		return this.tesrMask;
	}

	public void onBlockBreak(Block block, int meta)
	{
	}

	public String getTextureFolder()
	{
		return null;
	}

	public boolean onBlockActivated(EntityPlayer player, float xOffset, float yOffset, float zOffset, MutableBoolean result)
	{
		return false;
	}

	public void randomDisplayTick(Random random)
	{
	}

	public void adjustDrops(List<ItemStack> drops, int fortune)
	{
		drops.set(0, new ItemStack(Ic2Items.teBlock.getItem(), 1, this.tileEntityId));
	}
}