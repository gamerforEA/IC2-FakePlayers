package ic2.core.item.resources;

import com.gamerforea.eventhelper.util.EventUtils;
import ic2.core.IC2;
import ic2.core.Ic2Items;
import ic2.core.init.BlocksItems;
import ic2.core.init.InternalName;
import ic2.core.item.ItemIC2;
import ic2.core.util.LiquidUtil;
import ic2.core.util.StackUtil;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class ItemCell extends ItemIC2
{
	private final Map<Integer, InternalName> names = new HashMap();
	private final Map<Block, ItemStack> cells = new IdentityHashMap();

	public ItemCell(InternalName internalName)
	{
		super(internalName);
		this.setHasSubtypes(true);
		Ic2Items.cell = this.addCell(0, InternalName.itemCellEmpty);
		Ic2Items.waterCell = this.addCell(1, InternalName.itemCellWater, Blocks.water, Blocks.flowing_water);
		Ic2Items.lavaCell = this.addCell(2, InternalName.itemCellLava, Blocks.lava, Blocks.flowing_lava);
		Ic2Items.uuMatterCell = this.addRegisterCell(3, InternalName.itemCellUuMatter, InternalName.fluidUuMatter);
		Ic2Items.CFCell = this.addRegisterCell(4, InternalName.itemCellCF, InternalName.fluidConstructionFoam);
		Ic2Items.airCell = this.addCell(5, InternalName.itemCellAir);
		Ic2Items.biomassCell = this.addRegisterCell(6, InternalName.itemCellBiomass, InternalName.fluidBiomass);
		Ic2Items.biogasCell = this.addRegisterCell(7, InternalName.itemCellBiogas, InternalName.fluidBiogas);
		Ic2Items.electrolyzedWaterCell = this.addCell(8, InternalName.itemCellWaterElectro);
		Ic2Items.coolantCell = this.addRegisterCell(9, InternalName.itemCellCoolant, InternalName.fluidCoolant);
		Ic2Items.hotcoolantCell = this.addRegisterCell(10, InternalName.itemCellHotCoolant, InternalName.fluidHotCoolant);
		Ic2Items.pahoehoelavaCell = this.addRegisterCell(11, InternalName.itemCellPahoehoelava, InternalName.fluidPahoehoeLava);
		Ic2Items.distilledwaterCell = this.addRegisterCell(12, InternalName.itemCellDistilledWater, InternalName.fluidDistilledWater);
		Ic2Items.superheatedsteamCell = this.addRegisterCell(13, InternalName.itemCellSuperheatedSteam, InternalName.fluidSuperheatedSteam);
		Ic2Items.steamCell = this.addRegisterCell(14, InternalName.itemCellSteam, InternalName.fluidSteam);
		FluidContainerRegistry.registerFluidContainer(FluidRegistry.WATER, Ic2Items.waterCell.copy(), Ic2Items.cell.copy());
		FluidContainerRegistry.registerFluidContainer(FluidRegistry.LAVA, Ic2Items.lavaCell.copy(), Ic2Items.cell.copy());
	}

	@Override
	public String getTextureFolder()
	{
		return "cell";
	}

	@Override
	public String getUnlocalizedName(ItemStack stack)
	{
		InternalName ret = this.names.get(stack.getItemDamage());
		return ret == null ? null : "ic2." + ret.name();
	}

	@Override
	public void getSubItems(Item item, CreativeTabs tabs, List itemList)
	{
		for (int meta = 0; meta < 32767; ++meta)
		{
			ItemStack stack = new ItemStack(this, 1, meta);
			if (this.getUnlocalizedName(stack) == null)
				break;

			itemList.add(stack);
		}

	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float xOffset, float yOffset, float zOffset)
	{
		// TODO gamerforEA code start
		if (player instanceof FakePlayer)
			return false;
		// TODO gamerforEA code end

		if (!IC2.platform.isSimulating())
			return false;
		else
		{
			MovingObjectPosition position = this.getMovingObjectPositionFromPlayer(world, player, true);
			if (position == null)
				return false;
			else
			{
				if (position.typeOfHit == MovingObjectType.BLOCK)
				{
					x = position.blockX;
					y = position.blockY;
					z = position.blockZ;
					if (!world.canMineBlock(player, x, y, z))
						return false;

					if (!player.canPlayerEdit(x, y, z, position.sideHit, stack))
						return false;

					// TODO gamerforEA code start
					if (EventUtils.cantBreak(player, x, y, z))
						return false;
					// TODO gamerforEA code end

					if (stack.getItemDamage() == 0)
					{
						if (world.getBlockMetadata(x, y, z) == 0)
						{
							ItemStack filledCell = this.cells.get(world.getBlock(x, y, z));
							if (filledCell != null && StackUtil.storeInventoryItem(filledCell.copy(), player, false))
							{
								world.setBlockToAir(x, y, z);
								--stack.stackSize;
								return true;
							}
						}
					}
					else
					{
						ForgeDirection dir = ForgeDirection.VALID_DIRECTIONS[position.sideHit];

						// TODO gamerforEA code start
						if (EventUtils.cantBreak(player, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ))
							return false;
						// TODO gamerforEA code end

						FluidStack fs = FluidContainerRegistry.getFluidForFilledItem(stack);

						if (fs != null && LiquidUtil.placeFluid(fs, world, x, y, z) || player.canPlayerEdit(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ, position.sideHit, stack) && LiquidUtil.placeFluid(fs, world, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ))
						{
							if (!player.capabilities.isCreativeMode)
								--stack.stackSize;

							return true;
						}
					}
				}

				return false;
			}
		}
	}

	private ItemStack addCell(int meta, InternalName name, Block... blocks)
	{
		this.names.put(meta, name);
		ItemStack ret = new ItemStack(this, 1, meta);

		for (Block block : blocks)
		{
			this.cells.put(block, ret);
		}

		return ret;
	}

	private ItemStack addRegisterCell(int meta, InternalName name, InternalName blockName)
	{
		ItemStack ret = this.addCell(meta, name, BlocksItems.getFluidBlock(blockName));
		FluidContainerRegistry.registerFluidContainer(BlocksItems.getFluid(blockName), ret.copy(), Ic2Items.cell.copy());
		return ret;
	}
}
