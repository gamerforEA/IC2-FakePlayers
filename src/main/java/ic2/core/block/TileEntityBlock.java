package ic2.core.block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.gamerforea.eventhelper.fake.FakePlayerContainer;
import com.gamerforea.eventhelper.fake.FakePlayerContainerTileEntity;
import com.gamerforea.ic2.ModUtils;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.network.INetworkDataProvider;
import ic2.api.network.INetworkUpdateListener;
import ic2.api.tile.IWrenchable;
import ic2.core.IC2;
import ic2.core.ITickCallback;
import ic2.core.block.comp.TileEntityComponent;
import ic2.core.util.LogCategory;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

public abstract class TileEntityBlock extends TileEntity implements INetworkDataProvider, INetworkUpdateListener, IWrenchable
{
	private static final List<TileEntityComponent> emptyComponents = Arrays.asList();
	private static final List<Entry<String, TileEntityComponent>> emptyNamedComponents = Arrays.asList();
	private Map<String, TileEntityComponent> components;
	private List<TileEntityComponent> updatableComponents;
	private boolean active = false;
	private short facing = 0;
	public boolean prevActive = false;
	public short prevFacing = 0;
	private boolean loaded = false;
	private boolean enableWorldTick;
	private static final Map<Class<?>, TileEntityBlock.TickSubscription> tickSubscriptions = new HashMap();
	@SideOnly(Side.CLIENT)
	private IIcon[] lastRenderIcons;
	private int tesrMask;
	public int tesrTtl;
	private static final int defaultTesrTtl = 500;

	// TODO gamerforEA code start
	public final FakePlayerContainer fake = new FakePlayerContainerTileEntity(ModUtils.profile, this);
	// TODO gamerforEA code end

	@Override
	public final void validate()
	{
		super.validate();
		IC2.tickHandler.addSingleTickCallback(this.worldObj, new ITickCallback()
		{
			@Override
			public void tickCallback(World world)
			{
				if (!TileEntityBlock.this.isInvalid() && world.blockExists(TileEntityBlock.this.xCoord, TileEntityBlock.this.yCoord, TileEntityBlock.this.zCoord))
				{
					TileEntityBlock.this.onLoaded();
					if (!TileEntityBlock.this.isInvalid() && (TileEntityBlock.this.enableWorldTick || TileEntityBlock.this.updatableComponents != null))
						world.loadedTileEntityList.add(TileEntityBlock.this);

				}
			}
		});
	}

	@Override
	public final void invalidate()
	{
		this.onUnloaded();
		super.invalidate();
	}

	@Override
	public final void onChunkUnload()
	{
		this.onUnloaded();
		super.onChunkUnload();
	}

	public void onLoaded()
	{
		this.loaded = true;
		this.enableWorldTick = this.requiresWorldTick();
		if (this.components != null)
			for (TileEntityComponent component : this.components.values())
			{
				component.onLoaded();
				if (component.enableWorldTick())
				{
					if (this.updatableComponents == null)
						this.updatableComponents = new ArrayList(4);

					this.updatableComponents.add(component);
				}
			}

	}

	public void onUnloaded()
	{
		if (this.loaded)
		{
			this.loaded = false;
			if (this.components != null)
				for (TileEntityComponent component : this.components.values())
					component.onUnloaded();

		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		this.prevFacing = this.facing = nbt.getShort("facing");
		this.prevActive = this.active = nbt.getBoolean("active");
		if (this.components != null && nbt.hasKey("components", 10))
		{
			NBTTagCompound componentsNbt = nbt.getCompoundTag("components");

			for (String name : (Iterable<String>) componentsNbt.func_150296_c())
			{
				NBTTagCompound componentNbt = componentsNbt.getCompoundTag(name);
				TileEntityComponent component = this.components.get(name);
				if (component == null)
					IC2.log.warn(LogCategory.Block, "Can\'t find component {} while loading {}.", new Object[] { name, this });
				else
					component.readFromNbt(componentNbt);
			}
		}

		// TODO gamerforEA code start
		this.fake.readFromNBT(nbt);
		// TODO gamerforEA code end
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		nbt.setShort("facing", this.facing);
		nbt.setBoolean("active", this.active);
		NBTTagCompound componentsNbt = null;
		if (this.components != null)
			for (Entry<String, TileEntityComponent> entry : this.components.entrySet())
			{
				NBTTagCompound componentNbt = entry.getValue().writeToNbt();
				if (componentNbt != null)
				{
					if (componentsNbt == null)
					{
						componentsNbt = new NBTTagCompound();
						nbt.setTag("components", componentsNbt);
					}

					componentsNbt.setTag(entry.getKey(), componentNbt);
				}
			}

		// TODO gamerforEA code start
		this.fake.writeToNBT(nbt);
		// TODO gamerforEA code end
	}

	@Override
	public final boolean canUpdate()
	{
		return false;
	}

	@Override
	public final void updateEntity()
	{
		if (this.updatableComponents != null)
			for (TileEntityComponent component : this.updatableComponents)
				component.onWorldTick();

		if (this.enableWorldTick)
			if (this.worldObj.isRemote)
				this.updateEntityClient();
			else
				this.updateEntityServer();

	}

	protected void updateEntityClient()
	{
	}

	protected void updateEntityServer()
	{
	}

	@SideOnly(Side.CLIENT)
	public void onRender()
	{
		Block block = this.getBlockType();
		if (this.lastRenderIcons == null)
			this.lastRenderIcons = new IIcon[6];

		for (int side = 0; side < 6; ++side)
			this.lastRenderIcons[side] = block.getIcon(this.worldObj, this.xCoord, this.yCoord, this.zCoord, side);

		this.tesrMask = 0;
	}

	public boolean getActive()
	{
		return this.active;
	}

	public void setActive(boolean active1)
	{
		this.active = active1;
		if (this.prevActive != active1)
			IC2.network.get().updateTileEntityField(this, "active");

		this.prevActive = active1;
	}

	@Override
	public short getFacing()
	{
		return this.facing;
	}

	@Override
	public List<String> getNetworkedFields()
	{
		List<String> ret = new Vector(2);
		ret.add("active");
		ret.add("facing");
		return ret;
	}

	@Override
	public void onNetworkUpdate(String field)
	{
		if (field.equals("active") && this.prevActive != this.active || field.equals("facing") && this.prevFacing != this.facing)
		{
			int reRenderMask = 0;
			Block block = this.getBlockType();
			if (this.lastRenderIcons == null)
				reRenderMask = -1;
			else
				for (int side = 0; side < 6; ++side)
				{
					IIcon oldIcon = this.lastRenderIcons[side];
					if (oldIcon instanceof BlockTextureStitched)
						oldIcon = ((BlockTextureStitched) oldIcon).getRealTexture();

					IIcon newIcon = block.getIcon(this.worldObj, this.xCoord, this.yCoord, this.zCoord, side);
					if (newIcon instanceof BlockTextureStitched)
						newIcon = ((BlockTextureStitched) newIcon).getRealTexture();

					if (oldIcon != newIcon)
						reRenderMask |= 1 << side;
				}

			if (reRenderMask != 0)
				if (reRenderMask >= 0 && this.prevFacing == this.facing && block.getRenderType() == IC2.platform.getRenderId("default"))
				{
					this.tesrMask = reRenderMask;
					this.tesrTtl = 500;
				}
				else
					this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);

			this.prevActive = this.active;
			this.prevFacing = this.facing;
		}

	}

	@Override
	public boolean wrenchCanSetFacing(EntityPlayer entityPlayer, int side)
	{
		return false;
	}

	@Override
	public void setFacing(short facing1)
	{
		this.facing = facing1;
		if (this.prevFacing != facing1)
			IC2.network.get().updateTileEntityField(this, "facing");

		this.prevFacing = facing1;
	}

	@Override
	public boolean wrenchCanRemove(EntityPlayer entityPlayer)
	{
		return true;
	}

	@Override
	public float getWrenchDropRate()
	{
		return 1.0F;
	}

	@Override
	public ItemStack getWrenchDrop(EntityPlayer entityPlayer)
	{
		return new ItemStack(this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord), 1, this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord));
	}

	@Override
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
	}

	protected final <T extends TileEntityComponent> T addComponent(T component)
	{
		return this.addComponent(component.getDefaultName(), component);
	}

	protected final <T extends TileEntityComponent> T addComponent(String name, T component)
	{
		if (this.components == null)
			this.components = new HashMap(4);

		TileEntityComponent prev = this.components.put(name, component);
		if (prev != null)
			throw new RuntimeException("ambiguous component name " + name + " when adding " + component + ", already used by " + prev + ".");
		else
			return component;
	}

	public TileEntityComponent getComponent(String name)
	{
		return this.components == null ? null : (TileEntityComponent) this.components.get(name);
	}

	public final Iterable<TileEntityComponent> getComponents()
	{
		return this.components == null ? emptyComponents : this.components.values();
	}

	public final Iterable<Entry<String, TileEntityComponent>> getNamedComponents()
	{
		return this.components == null ? emptyNamedComponents : this.components.entrySet();
	}

	public void onNeighborUpdate(Block srcBlock)
	{
		if (this.components != null)
			for (TileEntityComponent component : this.components.values())
				component.onNeighborUpdate(srcBlock);

	}

	private final boolean requiresWorldTick()
	{
		Class<?> cls = this.getClass();
		TileEntityBlock.TickSubscription subscription = tickSubscriptions.get(cls);
		if (subscription == null)
		{
			boolean hasUpdateClient = false;
			boolean hasUpdateServer = false;

			for (boolean isClient = FMLCommonHandler.instance().getSide().isClient(); cls != TileEntityBlock.class && (!hasUpdateClient && isClient || !hasUpdateServer); cls = cls.getSuperclass())
			{
				if (!hasUpdateClient && isClient)
				{
					boolean found = true;

					try
					{
						cls.getDeclaredMethod("updateEntityClient", new Class[0]);
					}
					catch (NoSuchMethodException var9)
					{
						found = false;
					}

					if (found)
						hasUpdateClient = true;
				}

				if (!hasUpdateServer)
				{
					boolean found = true;

					try
					{
						cls.getDeclaredMethod("updateEntityServer", new Class[0]);
					}
					catch (NoSuchMethodException var8)
					{
						found = false;
					}

					if (found)
						hasUpdateServer = true;
				}
			}

			if (hasUpdateClient)
			{
				if (hasUpdateServer)
					subscription = TileEntityBlock.TickSubscription.Both;
				else
					subscription = TileEntityBlock.TickSubscription.Client;
			}
			else if (hasUpdateServer)
				subscription = TileEntityBlock.TickSubscription.Server;
			else
				subscription = TileEntityBlock.TickSubscription.None;

			tickSubscriptions.put(this.getClass(), subscription);
		}

		return this.worldObj.isRemote ? subscription == TileEntityBlock.TickSubscription.Both || subscription == TileEntityBlock.TickSubscription.Client : subscription == TileEntityBlock.TickSubscription.Both || subscription == TileEntityBlock.TickSubscription.Server;
	}

	private static enum TickSubscription
	{
		None,
		Client,
		Server,
		Both;
	}
}
