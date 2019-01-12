package ic2.core.block.reactor.block;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.Direction;
import ic2.core.IC2;
import ic2.core.Ic2Items;
import ic2.core.block.BlockMultiID;
import ic2.core.block.reactor.tileentity.TileEntityNuclearReactorElectric;
import ic2.core.block.reactor.tileentity.TileEntityReactorChamberElectric;
import ic2.core.init.InternalName;
import ic2.core.item.block.ItemBlockIC2;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.ArrayList;
import java.util.Random;

public class BlockReactorChamber extends BlockMultiID
{
	TileEntityNuclearReactorElectric reactor;

	public BlockReactorChamber(InternalName internalName1)
	{
		super(internalName1, Material.iron, ItemBlockIC2.class);
		this.setHardness(2.0F);
		this.setStepSound(soundTypeMetal);
		Ic2Items.reactorChamber = new ItemStack(this, 1, 0);
		GameRegistry.registerTileEntity(TileEntityReactorChamberElectric.class, "Reactor Chamber");
	}

	@Override
	public String getTextureFolder(int id)
	{
		return "reactor";
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor)
	{
		if (world.checkChunksExist(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1) && !this.canPlaceBlockAt(world, x, y, z))
		{
			// TODO gamerforEA code start
			if (world.getBlock(x, y, z) != this)
				return;
			// TODO gamerforEA code end

			world.setBlockToAir(x, y, z);
			this.dropBlockAsItem(world, x, y, z, Ic2Items.reactorChamber.copy());
		}

	}

	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z)
	{
		int count = 0;

		for (Direction dir : Direction.directions)
		{
			if (dir.applyTo(world, x, y, z) instanceof TileEntityNuclearReactorElectric)
				++count;
		}

		return count == 1;
	}

	@Override
	public void randomDisplayTick(World world, int i, int j, int k, Random random)
	{
		this.reactor = this.getReactorEntity(world, i, j, k);
		if (this.reactor == null)
			this.onNeighborBlockChange(world, i, j, k, this);
		else
		{
			int puffs = this.reactor.heat / 1000;
			if (puffs > 0)
			{
				puffs = world.rand.nextInt(puffs);

				for (int n = 0; n < puffs; ++n)
				{
					world.spawnParticle("smoke", i + random.nextFloat(), j + 0.95F, k + random.nextFloat(), 0.0D, 0.0D, 0.0D);
				}

				puffs = puffs - (world.rand.nextInt(4) + 3);

				for (int n = 0; n < puffs; ++n)
				{
					world.spawnParticle("flame", i + random.nextFloat(), j + 1.0F, k + random.nextFloat(), 0.0D, 0.0D, 0.0D);
				}

			}
		}
	}

	public TileEntityNuclearReactorElectric getReactorEntity(World world, int x, int y, int z)
	{
		for (Direction dir : Direction.directions)
		{
			TileEntity te = dir.applyTo(world, x, y, z);
			if (te instanceof TileEntityNuclearReactorElectric)
				return (TileEntityNuclearReactorElectric) te;
		}

		this.onNeighborBlockChange(world, x, y, z, world.getBlock(x, y, z));
		return null;
	}

	@Override
	public boolean onBlockActivated(World world, int i, int j, int k, EntityPlayer entityplayer, int side, float a, float b, float c)
	{
		if (entityplayer.isSneaking())
			return false;

		TileEntityNuclearReactorElectric reactor = this.getReactorEntity(world, i, j, k);
		if (reactor == null)
		{
			this.onNeighborBlockChange(world, i, j, k, this);
			return false;
		}
		return !IC2.platform.isSimulating() || IC2.platform.launchGui(entityplayer, reactor);
	}

	@Override
	public Class<? extends TileEntity> getTeClass(int meta, MutableObject<Class<?>[]> ctorArgTypes, MutableObject<Object[]> ctorArgs)
	{
		try
		{
			return TileEntityReactorChamberElectric.class;
		}
		catch (Throwable var5)
		{
			throw new RuntimeException(var5);
		}
	}

	@Override
	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune)
	{
		ArrayList<ItemStack> ret = new ArrayList();
		ret.add(Ic2Items.machine.copy());
		return ret;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public EnumRarity getRarity(ItemStack stack)
	{
		return EnumRarity.uncommon;
	}
}
