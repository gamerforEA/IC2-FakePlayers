package ic2.core.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.tile.IWrenchable;
import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.Ic2Items;
import ic2.core.init.InternalName;
import ic2.core.item.block.ItemBlockIC2;
import ic2.core.util.LogCategory;
import ic2.core.util.Util;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.ForgeDirection;
import org.apache.commons.lang3.mutable.MutableObject;

import java.lang.reflect.Constructor;
import java.util.*;

public abstract class BlockMultiID extends BlockBase
{
	public int renderMask = 63;
	private static final Class<?>[] emptyClassArray = new Class[0];
	private static final Object[] emptyObjArray = new Object[0];
	private static final int tesBeforeBreakLimit = 8;
	private static ArrayDeque<TileEntity> tesBeforeBreak = new ArrayDeque(8);

	public BlockMultiID(InternalName internalName1, Material material)
	{
		super(internalName1, material);
	}

	public BlockMultiID(InternalName internalName1, Material material, Class<? extends ItemBlockIC2> itemClass)
	{
		super(internalName1, material, itemClass);
	}

	@Override
	public int getFacing(IBlockAccess iBlockAccess, int x, int y, int z)
	{
		TileEntity te = this.getOwnTe(iBlockAccess, x, y, z);
		if (te instanceof TileEntityBlock)
			return ((TileEntityBlock) te).getFacing();
		else
		{
			int meta = iBlockAccess.getBlockMetadata(x, y, z);
			return this.getFacing(meta);
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister iconRegister)
	{
		int metaCount = this.getMetaCount();
		this.textures = new IIcon[metaCount][12];

		for (int index = 0; index < metaCount; ++index)
		{
			String textureFolder = this.getTextureFolder(index);
			textureFolder = textureFolder == null ? "" : textureFolder + "/";
			String name = IC2.textureDomain + ":" + textureFolder + this.getTextureName(index);

			for (int active = 0; active < 2; ++active)
			{
				for (int side = 0; side < 6; ++side)
				{
					int subIndex = active * 6 + side;
					String subName = name + ":" + subIndex;
					TextureAtlasSprite texture = new BlockTextureStitched(subName, subIndex);
					this.textures[index][subIndex] = texture;
					((TextureMap) iconRegister).setTextureEntry(subName, texture);
				}
			}
		}

	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(IBlockAccess iBlockAccess, int x, int y, int z, int side)
	{
		int facing = this.getFacing(iBlockAccess, x, y, z);
		boolean active = this.isActive(iBlockAccess, x, y, z);
		int meta = iBlockAccess.getBlockMetadata(x, y, z);
		int index = this.getTextureIndex(meta);
		if (index >= this.textures.length)
			return null;
		else
		{
			int subIndex = getTextureSubIndex(facing, side);
			if (active)
				subIndex += 6;

			try
			{
				return this.textures[index][subIndex];
			}
			catch (Exception var12)
			{
				IC2.platform.displayError(var12, "Coordinates: %d/%d/%d\nSide: %d\nBlock: %s\nMeta: %d\nFacing: %d\nActive: %s\nIndex: %d\nSubIndex: %d", x, y, z, side, this, meta, facing, active, index, subIndex);
				return null;
			}
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta)
	{
		int facing = this.getFacing(meta);
		int index = this.getTextureIndex(meta);
		int subIndex = getTextureSubIndex(facing, side);
		if (index >= this.textures.length)
			return null;
		else
			try
			{
				return this.textures[index][subIndex];
			}
			catch (Exception var7)
			{
				IC2.platform.displayError(var7, "Side: " + side + "\n" + "Block: " + this + "\n" + "Meta: " + meta + "\n" + "Facing: " + facing + "\n" + "Index: " + index + "\n" + "SubIndex: " + subIndex);
				return null;
			}
	}

	@Override
	public int getRenderType()
	{
		return IC2.platform.getRenderId("default");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldSideBeRendered(IBlockAccess blockAccess, int x, int y, int z, int side)
	{
		return (this.renderMask & 1 << side) != 0 && super.shouldSideBeRendered(blockAccess, x, y, z, side);
	}

	@SideOnly(Side.CLIENT)
	public void onRender(IBlockAccess blockAccess, int x, int y, int z)
	{
		TileEntity te = this.getOwnTe(blockAccess, x, y, z);
		if (te instanceof TileEntityBlock)
			((TileEntityBlock) te).onRender();

	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer entityPlayer, int side, float a, float b, float c)
	{
		if (entityPlayer.isSneaking())
			return false;
		else
		{
			TileEntity te = this.getOwnTe(world, x, y, z);
			return te instanceof IHasGui && (!IC2.platform.isSimulating() || IC2.platform.launchGui(entityPlayer, (IHasGui) te));
		}
	}

	@Override
	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune)
	{
		ArrayList<ItemStack> ret = super.getDrops(world, x, y, z, metadata, fortune);
		TileEntity te = this.getOwnTe(world, x, y, z);
		if (te == null)
		{
			Iterator<TileEntity> it = tesBeforeBreak.descendingIterator();

			while (it.hasNext())
			{
				TileEntity te2 = it.next();
				if (te2.getWorldObj() == world && te2.xCoord == x && te2.yCoord == y && te2.zCoord == z)
				{
					te = te2;
					it.remove();
					break;
				}
			}
		}

		if (te instanceof IInventory)
		{
			IInventory inv = (IInventory) te;

			for (int i = 0; i < inv.getSizeInventory(); ++i)
			{
				ItemStack itemStack = inv.getStackInSlot(i);
				if (itemStack != null)
					ret.add(itemStack);
			}
		}

		return ret;
	}

	@Override
	public void onBlockPreDestroy(World world, int x, int y, int z, int meta)
	{
		TileEntity te = this.getOwnTe(world, x, y, z);
		if (te instanceof TileEntityBlock)
		{
			TileEntityBlock teb = (TileEntityBlock) te;
			teb.onBlockBreak(this, meta);
			teb.onUnloaded();
		}

		if (te != null)
		{
			if (te instanceof IHasGui)
				for (Object obj : world.playerEntities)
				{
					if (obj instanceof EntityPlayerMP)
					{
						EntityPlayerMP player = (EntityPlayerMP) obj;
						if (player.openContainer instanceof ContainerBase)
						{
							ContainerBase<?> container = (ContainerBase) player.openContainer;
							if (container.base == te)
								player.closeScreen();
						}
					}
				}

			if (tesBeforeBreak.size() >= 8)
				tesBeforeBreak.pop();

			tesBeforeBreak.push(te);
		}

		if (Ic2Items.copperOre != null && this.getUnlocalizedName().equals(Ic2Items.copperOre.getUnlocalizedName()))
			this.dropXpOnBlockBreak(world, x, y, z, 1);

		if (Ic2Items.tinOre != null && this.getUnlocalizedName().equals(Ic2Items.tinOre.getUnlocalizedName()))
			this.dropXpOnBlockBreak(world, x, y, z, 1);

		if (Ic2Items.uraniumOre != null && this.getUnlocalizedName().equals(Ic2Items.uraniumOre.getUnlocalizedName()))
			this.dropXpOnBlockBreak(world, x, y, z, 2);

		if (Ic2Items.leadOre != null && this.getUnlocalizedName().equals(Ic2Items.leadOre.getUnlocalizedName()))
			this.dropXpOnBlockBreak(world, x, y, z, 1);

	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z)
	{
		Iterator<TileEntity> it = tesBeforeBreak.descendingIterator();

		while (it.hasNext())
		{
			TileEntity te = it.next();
			if (te.getWorldObj() == world && te.xCoord == x && te.yCoord == y && te.zCoord == z)
			{
				it.remove();
				break;
			}
		}

	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entityliving, ItemStack itemStack)
	{
		if (IC2.platform.isSimulating())
		{
			TileEntity tileEntity = this.getOwnTe(world, x, y, z);
			if (tileEntity instanceof IWrenchable)
			{
				IWrenchable te = (IWrenchable) tileEntity;
				if (entityliving == null)
					te.setFacing((short) 2);
				else
				{
					// TODO gamerforEA code start
					if (te instanceof TileEntityBlock && entityliving instanceof EntityPlayer)
						((TileEntityBlock) te).fake.setProfile(((EntityPlayer) entityliving).getGameProfile());
					// TODO gamerforEA code end

					int l = MathHelper.floor_double(entityliving.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
					switch (l)
					{
						case 0:
							te.setFacing((short) 2);
							break;
						case 1:
							te.setFacing((short) 5);
							break;
						case 2:
							te.setFacing((short) 3);
							break;
						case 3:
							te.setFacing((short) 4);
					}
				}
			}
		}
	}

	@Override
	public final boolean hasTileEntity(int metadata)
	{
		return true;
	}

	@Override
	public boolean canCreatureSpawn(EnumCreatureType type, IBlockAccess world, int x, int y, int z)
	{
		return false;
	}

	@Override
	public final TileEntity createTileEntity(World world, int metadata)
	{
		MutableObject<Class<?>[]> ctorArgTypes = new MutableObject(emptyClassArray);
		MutableObject<Object[]> ctorArgs = new MutableObject(emptyObjArray);
		Class<? extends TileEntity> teClass = this.getTeClass(metadata, ctorArgTypes, ctorArgs);
		if (teClass == null)
			return null;
		else
			try
			{
				Constructor<? extends TileEntity> ctor = teClass.getConstructor(ctorArgTypes.getValue());
				return ctor.newInstance(ctorArgs.getValue());
			}
			catch (Throwable var7)
			{
				throw new RuntimeException("Error constructing " + teClass + " with " + Arrays.asList((Object[]) ctorArgTypes.getValue()) + ", " + Arrays.asList(ctorArgs.getValue()) + ".", var7);
			}
	}

	public abstract Class<? extends TileEntity> getTeClass(int var1, MutableObject<Class<?>[]> var2, MutableObject<Object[]> var3);

	public TileEntity getOwnTe(IBlockAccess blockAccess, int x, int y, int z)
	{
		Block block;
		int meta;
		TileEntity te;
		if (blockAccess instanceof World)
		{
			Chunk chunk = Util.getLoadedChunk((World) blockAccess, x >> 4, z >> 4);
			if (chunk == null)
				return null;

			block = chunk.getBlock(x & 15, y, z & 15);
			meta = chunk.getBlockMetadata(x & 15, y, z & 15);
			te = blockAccess.getTileEntity(x, y, z);
		}
		else
		{
			block = blockAccess.getBlock(x, y, z);
			meta = blockAccess.getBlockMetadata(x, y, z);
			te = blockAccess.getTileEntity(x, y, z);
		}

		Class<? extends TileEntity> expectedClass = this.getTeClass(meta, null, null);
		Class<? extends TileEntity> actualClass = te != null ? te.getClass() : null;
		if (actualClass != expectedClass)
		{
			if (block != this)
			{
				if (Util.inDev())
				{
					StackTraceElement[] st = new Throwable().getStackTrace();
					IC2.log.warn(LogCategory.Block, "Own tile entity query from %s to foreign block %s instead of %s at %s.", st.length > 1 ? st[1] : "?", block != null ? block.getClass() : null, this.getClass(), Util.formatPosition(blockAccess, x, y, z));
				}

				return null;
			}

			IC2.log.warn(LogCategory.Block, "Mismatched tile entity at %s, got %s, expected %s.", Util.formatPosition(blockAccess, x, y, z), actualClass, expectedClass);
			if (!(blockAccess instanceof World))
				return null;

			World world = (World) blockAccess;
			te = this.createTileEntity(world, meta);
			world.setTileEntity(x, y, z, te);
		}

		return te;
	}

	public final boolean isActive(IBlockAccess blockAccess, int x, int y, int z)
	{
		TileEntity te = this.getOwnTe(blockAccess, x, y, z);
		return te instanceof TileEntityBlock && ((TileEntityBlock) te).getActive();
	}

	@Override
	public void getSubBlocks(Item j, CreativeTabs tabs, List itemList)
	{
		Item item = Item.getItemFromBlock(this);
		if (!item.getHasSubtypes())
			itemList.add(new ItemStack(this));
		else
			for (int i = 0; i < 16; ++i)
			{
				ItemStack is = new ItemStack(this, 1, i);
				if (is.getItem().getUnlocalizedName(is) == null)
					break;

				itemList.add(is);
			}

	}

	@Override
	public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z)
	{
		return new ItemStack(this, 1, world.getBlockMetadata(x, y, z));
	}

	@Override
	public boolean rotateBlock(World worldObj, int x, int y, int z, ForgeDirection axis)
	{
		if (axis == ForgeDirection.UNKNOWN)
			return false;
		else
		{
			TileEntity tileEntity = this.getOwnTe(worldObj, x, y, z);
			if (tileEntity instanceof IWrenchable)
			{
				IWrenchable te = (IWrenchable) tileEntity;
				int newFacing = ForgeDirection.getOrientation(te.getFacing()).getRotation(axis).ordinal();
				if (te.wrenchCanSetFacing(null, newFacing))
					te.setFacing((short) newFacing);
			}

			return false;
		}
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block srcBlock)
	{
		TileEntity te = this.getOwnTe(world, x, y, z);
		if (te instanceof TileEntityBlock)
			((TileEntityBlock) te).onNeighborUpdate(srcBlock);

	}
}
