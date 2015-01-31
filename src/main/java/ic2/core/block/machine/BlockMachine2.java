package ic2.core.block.machine;

import ic2.core.IC2;
import ic2.core.Ic2Items;
import ic2.core.block.BlockMultiID;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.machine.tileentity.TileEntityAdvMiner;
import ic2.core.block.machine.tileentity.TileEntityCentrifuge;
import ic2.core.block.machine.tileentity.TileEntityCondenser;
import ic2.core.block.machine.tileentity.TileEntityCropmatron;
import ic2.core.block.machine.tileentity.TileEntityFermenter;
import ic2.core.block.machine.tileentity.TileEntityFluidBottler;
import ic2.core.block.machine.tileentity.TileEntityFluidRegulator;
import ic2.core.block.machine.tileentity.TileEntityLiquidHeatExchanger;
import ic2.core.block.machine.tileentity.TileEntityMetalFormer;
import ic2.core.block.machine.tileentity.TileEntityOreWashing;
import ic2.core.block.machine.tileentity.TileEntityPatternStorage;
import ic2.core.block.machine.tileentity.TileEntityReplicator;
import ic2.core.block.machine.tileentity.TileEntityScanner;
import ic2.core.block.machine.tileentity.TileEntitySolidCanner;
import ic2.core.block.machine.tileentity.TileEntityTeleporter;
import ic2.core.block.machine.tileentity.TileEntityTesla;
import ic2.core.init.InternalName;
import ic2.core.item.block.ItemMachine2;
import ic2.core.util.StackUtil;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.apache.commons.lang3.mutable.MutableObject;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockMachine2 extends BlockMultiID
{
	public BlockMachine2(InternalName internalName1)
	{
		super(internalName1, Material.iron, ItemMachine2.class);
		this.setHardness(2.0F);
		this.setStepSound(soundTypeMetal);
		Ic2Items.teleporter = new ItemStack(this, 1, 0);
		Ic2Items.teslaCoil = new ItemStack(this, 1, 1);
		Ic2Items.cropmatron = new ItemStack(this, 1, 2);
		Ic2Items.centrifuge = new ItemStack(this, 1, 3);
		Ic2Items.metalformer = new ItemStack(this, 1, 4);
		Ic2Items.orewashingplant = new ItemStack(this, 1, 5);
		Ic2Items.patternstorage = new ItemStack(this, 1, 6);
		Ic2Items.scanner = new ItemStack(this, 1, 7);
		Ic2Items.replicator = new ItemStack(this, 1, 8);
		Ic2Items.solidcanner = new ItemStack(this, 1, 9);
		Ic2Items.fluidbottler = new ItemStack(this, 1, 10);
		Ic2Items.advminer = new ItemStack(this, 1, 11);
		Ic2Items.liquidheatexchanger = new ItemStack(this, 1, 12);
		Ic2Items.fermenter = new ItemStack(this, 1, 13);
		Ic2Items.fluidregulator = new ItemStack(this, 1, 14);
		Ic2Items.condenser = new ItemStack(this, 1, 15);
		GameRegistry.registerTileEntity(TileEntityTeleporter.class, "Teleporter");
		GameRegistry.registerTileEntity(TileEntityTesla.class, "Tesla Coil");
		GameRegistry.registerTileEntity(TileEntityCropmatron.class, "Crop-Matron");
		GameRegistry.registerTileEntity(TileEntityCentrifuge.class, "Thermal Centrifuge");
		GameRegistry.registerTileEntity(TileEntityMetalFormer.class, "Metal Former");
		GameRegistry.registerTileEntity(TileEntityOreWashing.class, "Ore Washing Plant");
		GameRegistry.registerTileEntity(TileEntityPatternStorage.class, "Pattern Storage");
		GameRegistry.registerTileEntity(TileEntityScanner.class, "Scanner");
		GameRegistry.registerTileEntity(TileEntityReplicator.class, "Replicator");
		GameRegistry.registerTileEntity(TileEntitySolidCanner.class, "Solid Canner Maschine");
		GameRegistry.registerTileEntity(TileEntityFluidBottler.class, "Fluid Bottler Maschine");
		GameRegistry.registerTileEntity(TileEntityAdvMiner.class, "Advanced Miner");
		GameRegistry.registerTileEntity(TileEntityLiquidHeatExchanger.class, "Liquid Heat Exchanger");
		GameRegistry.registerTileEntity(TileEntityFermenter.class, "Fermenter");
		GameRegistry.registerTileEntity(TileEntityFluidRegulator.class, "Fluid Regulator");
		GameRegistry.registerTileEntity(TileEntityCondenser.class, "Condenser");
	}

	@Override
	public String getTextureFolder(int id)
	{
		return "machine";
	}

	@Override
	public Item getItemDropped(int meta, Random random, int fortune)
	{
		switch (meta)
		{
			case 0:
			case 6:
			case 7:
			case 8:
			case 11:
				return Ic2Items.advancedMachine.getItem();
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 9:
			case 10:
			default:
				return Ic2Items.machine.getItem();
		}
	}

	@Override
	public int damageDropped(int meta)
	{
		switch (meta)
		{
			case 0:
			case 6:
			case 7:
			case 8:
			case 11:
				return Ic2Items.advancedMachine.getItemDamage();
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 9:
			case 10:
			default:
				return Ic2Items.machine.getItemDamage();
		}
	}

	@Override
	public Class<? extends TileEntity> getTeClass(int meta, MutableObject<Class<?>[]> ctorArgTypes, MutableObject<Object[]> ctorArgs)
	{
		switch (meta)
		{
			case 0:
				return TileEntityTeleporter.class;
			case 1:
				return TileEntityTesla.class;
			case 2:
				return TileEntityCropmatron.class;
			case 3:
				return TileEntityCentrifuge.class;
			case 4:
				return TileEntityMetalFormer.class;
			case 5:
				return TileEntityOreWashing.class;
			case 6:
				return TileEntityPatternStorage.class;
			case 7:
				return TileEntityScanner.class;
			case 8:
				return TileEntityReplicator.class;
			case 9:
				return TileEntitySolidCanner.class;
			case 10:
				return TileEntityFluidBottler.class;
			case 11:
				return TileEntityAdvMiner.class;
			case 12:
				return TileEntityLiquidHeatExchanger.class;
			case 13:
				return TileEntityFermenter.class;
			case 14:
				return TileEntityFluidRegulator.class;
			case 15:
				return TileEntityCondenser.class;
			default:
				return null;
		}
	}

	@Override
	public void randomDisplayTick(World world, int i, int j, int k, Random random)
	{
		world.getBlockMetadata(i, j, k);
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entity, ItemStack stack)
	{
		super.onBlockPlacedBy(world, x, y, z, entity, stack);
		if (IC2.platform.isSimulating())
		{
			TileEntityBlock te = (TileEntityBlock) this.getOwnTe(world, x, y, z);
			if (te != null)
			{
				NBTTagCompound nbttagcompound;
				if (te instanceof TileEntityAdvMiner)
				{
					nbttagcompound = StackUtil.getOrCreateNbtData(stack);
					((TileEntityAdvMiner) te).energy = nbttagcompound.getDouble("energy");
					// TODO gamerforEA code start
					if (entity != null && entity instanceof EntityPlayer)
					{
						GameProfile profile = ((EntityPlayer) entity).getGameProfile();
						TileEntityAdvMiner miner = (TileEntityAdvMiner) te;
						miner.ownerUUID = profile.getId();
						miner.ownerName = profile.getName();
					}
					// TODO gamerforEA code end
				}

				if (te instanceof TileEntityPatternStorage)
				{
					nbttagcompound = StackUtil.getOrCreateNbtData(stack);
					((TileEntityPatternStorage) te).readContents(nbttagcompound);
				}
			}
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public EnumRarity getRarity(ItemStack stack)
	{
		return stack.getItemDamage() == 0 ? EnumRarity.rare : EnumRarity.common;
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
		return te == null ? 0 : (te instanceof TileEntityTeleporter ? (((TileEntityTeleporter) te).targetSet ? 15 : 0) : 0);
	}
}