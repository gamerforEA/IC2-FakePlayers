package ic2.core.block.machine;

import ic2.core.IC2;
import ic2.core.Ic2Items;
import ic2.core.block.BlockMultiID;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.machine.tileentity.TileEntityCanner;
import ic2.core.block.machine.tileentity.TileEntityCompressor;
import ic2.core.block.machine.tileentity.TileEntityElectricFurnace;
import ic2.core.block.machine.tileentity.TileEntityElectrolyzer;
import ic2.core.block.machine.tileentity.TileEntityExtractor;
import ic2.core.block.machine.tileentity.TileEntityInduction;
import ic2.core.block.machine.tileentity.TileEntityIronFurnace;
import ic2.core.block.machine.tileentity.TileEntityMacerator;
import ic2.core.block.machine.tileentity.TileEntityMagnetizer;
import ic2.core.block.machine.tileentity.TileEntityMatter;
import ic2.core.block.machine.tileentity.TileEntityMiner;
import ic2.core.block.machine.tileentity.TileEntityPump;
import ic2.core.block.machine.tileentity.TileEntityRecycler;
import ic2.core.block.machine.tileentity.TileEntityStandardMachine;
import ic2.core.block.machine.tileentity.TileEntityTerra;
import ic2.core.init.InternalName;
import ic2.core.item.block.ItemMachine;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.apache.commons.lang3.mutable.MutableObject;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockMachine extends BlockMultiID
{
	public BlockMachine(InternalName internalName1)
	{
		super(internalName1, Material.iron, ItemMachine.class);
		this.setHardness(2.0F);
		this.setStepSound(soundTypeMetal);
		Ic2Items.machine = new ItemStack(this, 1, 0);
		Ic2Items.advancedMachine = new ItemStack(this, 1, 12);
		Ic2Items.ironFurnace = new ItemStack(this, 1, 1);
		Ic2Items.electroFurnace = new ItemStack(this, 1, 2);
		Ic2Items.macerator = new ItemStack(this, 1, 3);
		Ic2Items.extractor = new ItemStack(this, 1, 4);
		Ic2Items.compressor = new ItemStack(this, 1, 5);
		Ic2Items.canner = new ItemStack(this, 1, 6);
		Ic2Items.miner = new ItemStack(this, 1, 7);
		Ic2Items.pump = new ItemStack(this, 1, 8);
		Ic2Items.magnetizer = new ItemStack(this, 1, 9);
		Ic2Items.electrolyzer = new ItemStack(this, 1, 10);
		Ic2Items.recycler = new ItemStack(this, 1, 11);
		Ic2Items.inductionFurnace = new ItemStack(this, 1, 13);
		Ic2Items.massFabricator = new ItemStack(this, 1, 14);
		Ic2Items.terraformer = new ItemStack(this, 1, 15);
		GameRegistry.registerTileEntity(TileEntityIronFurnace.class, "Iron Furnace");
		GameRegistry.registerTileEntity(TileEntityElectricFurnace.class, "Electric Furnace");
		GameRegistry.registerTileEntity(TileEntityMacerator.class, "Macerator");
		GameRegistry.registerTileEntity(TileEntityExtractor.class, "Extractor");
		GameRegistry.registerTileEntity(TileEntityCompressor.class, "Compressor");
		GameRegistry.registerTileEntity(TileEntityCanner.class, "Canning Machine");
		GameRegistry.registerTileEntity(TileEntityMiner.class, "Miner");
		GameRegistry.registerTileEntity(TileEntityPump.class, "Pump");
		GameRegistry.registerTileEntity(TileEntityMagnetizer.class, "Magnetizer");
		GameRegistry.registerTileEntity(TileEntityElectrolyzer.class, "Electrolyzer");
		GameRegistry.registerTileEntity(TileEntityRecycler.class, "Recycler");
		GameRegistry.registerTileEntity(TileEntityInduction.class, "Induction Furnace");
		GameRegistry.registerTileEntity(TileEntityMatter.class, "Mass Fabricator");
		GameRegistry.registerTileEntity(TileEntityTerra.class, "Terraformer");
	}

	// TODO gamerforEA code start
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entity, ItemStack stack)
	{
		super.onBlockPlacedBy(world, x, y, z, entity, stack);
		if (IC2.platform.isSimulating())
		{
			TileEntity te = this.getOwnTe(world, x, y, z);
			if (te != null && entity != null && entity instanceof EntityPlayer)
			{
				GameProfile profile = ((EntityPlayer) entity).getGameProfile();
				if (te instanceof TileEntityMiner)
				{
					TileEntityMiner miner = (TileEntityMiner) te;
					miner.ownerUUID = profile.getId();
					miner.ownerName = profile.getName();
				}
				else if (te instanceof TileEntityPump)
				{
					TileEntityPump pump = (TileEntityPump) te;
					pump.ownerUUID = profile.getId();
					pump.ownerName = profile.getName();
				}
			}
		}
	}
	// TODO gamerforEA code end

	@Override
	public String getTextureFolder(int id)
	{
		return "machine";
	}

	@Override
	public int damageDropped(int meta)
	{
		switch (meta)
		{
			case 1:
				return meta;
			case 2:
				return meta;
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
			case 8:
			case 10:
			case 11:
			default:
				return 0;
			case 9:
				return meta;
			case 12:
				return 12;
			case 13:
				return 12;
			case 14:
				return 12;
			case 15:
				return 12;
		}
	}

	@Override
	public Class<? extends TileEntity> getTeClass(int meta, MutableObject<Class<?>[]> ctorArgTypes, MutableObject<Object[]> ctorArgs)
	{
		switch (meta)
		{
			case 1:
				return TileEntityIronFurnace.class;
			case 2:
				return TileEntityElectricFurnace.class;
			case 3:
				return TileEntityMacerator.class;
			case 4:
				return TileEntityExtractor.class;
			case 5:
				return TileEntityCompressor.class;
			case 6:
				return TileEntityCanner.class;
			case 7:
				return TileEntityMiner.class;
			case 8:
				return TileEntityPump.class;
			case 9:
				return TileEntityMagnetizer.class;
			case 10:
				return TileEntityElectrolyzer.class;
			case 11:
				return TileEntityRecycler.class;
			case 12:
			default:
				return null;
			case 13:
				return TileEntityInduction.class;
			case 14:
				return TileEntityMatter.class;
			case 15:
				return TileEntityTerra.class;
		}
	}

	@Override
	public void randomDisplayTick(World world, int x, int y, int z, Random random)
	{
		if (IC2.platform.isRendering())
		{
			int meta = world.getBlockMetadata(x, y, z);
			float f2;
			float fmod;
			float f1mod;
			float f2mod;
			if (meta == 1 && this.isActive(world, x, y, z))
			{
				TileEntityBlock tile = (TileEntityBlock) this.getOwnTe(world, x, y, z);
				if (tile == null)
				{
					return;
				}

				short var15 = tile.getFacing();
				f2 = (float) x + 0.5F;
				float var16 = (float) y + 0.0F + random.nextFloat() * 6.0F / 16.0F;
				fmod = (float) z + 0.5F;
				f1mod = 0.52F;
				f2mod = random.nextFloat() * 0.6F - 0.3F;
				switch (var15)
				{
					case 2:
						world.spawnParticle("smoke", (double) (f2 + f2mod), (double) var16, (double) (fmod - f1mod), 0.0D, 0.0D, 0.0D);
						world.spawnParticle("flame", (double) (f2 + f2mod), (double) var16, (double) (fmod - f1mod), 0.0D, 0.0D, 0.0D);
						break;
					case 3:
						world.spawnParticle("smoke", (double) (f2 + f2mod), (double) var16, (double) (fmod + f1mod), 0.0D, 0.0D, 0.0D);
						world.spawnParticle("flame", (double) (f2 + f2mod), (double) var16, (double) (fmod + f1mod), 0.0D, 0.0D, 0.0D);
						break;
					case 4:
						world.spawnParticle("smoke", (double) (f2 - f1mod), (double) var16, (double) (fmod + f2mod), 0.0D, 0.0D, 0.0D);
						world.spawnParticle("flame", (double) (f2 - f1mod), (double) var16, (double) (fmod + f2mod), 0.0D, 0.0D, 0.0D);
						break;
					case 5:
						world.spawnParticle("smoke", (double) (f2 + f1mod), (double) var16, (double) (fmod + f2mod), 0.0D, 0.0D, 0.0D);
						world.spawnParticle("flame", (double) (f2 + f1mod), (double) var16, (double) (fmod + f2mod), 0.0D, 0.0D, 0.0D);
				}
			}
			else if (meta == 3 && this.isActive(world, x, y, z))
			{
				float f = (float) x + 1.0F;
				float f1 = (float) y + 1.0F;
				f2 = (float) z + 1.0F;

				for (int i = 0; i < 4; ++i)
				{
					fmod = -0.2F - random.nextFloat() * 0.6F;
					f1mod = -0.1F + random.nextFloat() * 0.2F;
					f2mod = -0.2F - random.nextFloat() * 0.6F;
					world.spawnParticle("smoke", (double) (f + fmod), (double) (f1 + f1mod), (double) (f2 + f2mod), 0.0D, 0.0D, 0.0D);
				}
			}
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public EnumRarity getRarity(ItemStack stack)
	{
		return stack.getItemDamage() == 14 ? EnumRarity.rare : (stack.getItemDamage() != 15 && stack.getItemDamage() != 13 && stack.getItemDamage() != 12 ? EnumRarity.common : EnumRarity.uncommon);
	}

	@Override
	public boolean hasComparatorInputOverride()
	{
		return true;
	}

	@Override
	public int getComparatorInputOverride(World world, int x, int y, int z, int side)
	{
		TileEntityBlock te = (TileEntityBlock) this.getOwnTe(world, x, y, z);
		if (te == null)
		{
			return 0;
		}
		else if (te instanceof TileEntityInduction)
		{
			TileEntityInduction tem1 = (TileEntityInduction) te;
			return (int) Math.floor((double) ((float) tem1.heat / (float) TileEntityInduction.maxHeat * 15.0F));
		}
		else if (te instanceof TileEntityMatter)
		{
			return (int) Math.floor(((TileEntityMatter) te).energy / 1000000.0D * 15.0D);
		}
		else if (te instanceof TileEntityElectrolyzer)
		{
			return (int) Math.floor((double) ((float) ((TileEntityElectrolyzer) te).energy / 20000.0F * 15.0F));
		}
		else if (te instanceof TileEntityStandardMachine)
		{
			TileEntityStandardMachine tem = (TileEntityStandardMachine) te;
			return (int) Math.floor((double) (tem.getProgress() * 15.0F));
		}
		else
		{
			return 0;
		}
	}
}