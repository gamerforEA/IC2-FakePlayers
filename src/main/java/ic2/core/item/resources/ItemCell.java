package ic2.core.item.resources;

import ic2.core.IC2;
import ic2.core.Ic2Items;
import ic2.core.init.BlocksItems;
import ic2.core.init.InternalName;
import ic2.core.item.ItemIC2;
import ic2.core.util.LiquidUtil;
import ic2.core.util.StackUtil;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gamerforea.ic2.FakePlayerUtils;

public class ItemCell extends ItemIC2
{
	private final Map<Integer, InternalName> names = new HashMap();
	private final Map<Block, ItemStack> cells = new IdentityHashMap();

	public ItemCell(InternalName internalName)
	{
		super(internalName);
		this.setHasSubtypes(true);
		Ic2Items.cell = this.addCell(0, InternalName.itemCellEmpty, new Block[0]);
		Ic2Items.waterCell = this.addCell(1, InternalName.itemCellWater, new Block[] { Blocks.water, Blocks.flowing_water });
		Ic2Items.lavaCell = this.addCell(2, InternalName.itemCellLava, new Block[] { Blocks.lava, Blocks.flowing_lava });
		Ic2Items.uuMatterCell = this.addRegisterCell(3, InternalName.itemCellUuMatter, InternalName.fluidUuMatter);
		Ic2Items.CFCell = this.addRegisterCell(4, InternalName.itemCellCF, InternalName.fluidConstructionFoam);
		Ic2Items.airCell = this.addCell(5, InternalName.itemCellAir, new Block[0]);
		Ic2Items.biomassCell = this.addRegisterCell(6, InternalName.itemCellBiomass, InternalName.fluidBiomass);
		Ic2Items.biogasCell = this.addRegisterCell(7, InternalName.itemCellBiogas, InternalName.fluidBiogas);
		Ic2Items.electrolyzedWaterCell = this.addCell(8, InternalName.itemCellWaterElectro, new Block[0]);
		Ic2Items.coolantCell = this.addRegisterCell(9, InternalName.itemCellCoolant, InternalName.fluidCoolant);
		Ic2Items.hotcoolantCell = this.addRegisterCell(10, InternalName.itemCellHotCoolant, InternalName.fluidHotCoolant);
		Ic2Items.pahoehoelavaCell = this.addRegisterCell(11, InternalName.itemCellPahoehoelava, InternalName.fluidPahoehoeLava);
		Ic2Items.distilledwaterCell = this.addRegisterCell(12, InternalName.itemCellDistilledWater, InternalName.fluidDistilledWater);
		Ic2Items.superheatedsteamCell = this.addRegisterCell(13, InternalName.itemCellSuperheatedSteam, InternalName.fluidSuperheatedSteam);
		Ic2Items.steamCell = this.addRegisterCell(14, InternalName.itemCellSteam, InternalName.fluidSteam);
		FluidContainerRegistry.registerFluidContainer(FluidRegistry.WATER, Ic2Items.waterCell.copy(), Ic2Items.cell.copy());
		FluidContainerRegistry.registerFluidContainer(FluidRegistry.LAVA, Ic2Items.lavaCell.copy(), Ic2Items.cell.copy());
	}

	public String getTextureFolder()
	{
		return "cell";
	}

	public String getUnlocalizedName(ItemStack stack)
	{
		InternalName ret = (InternalName) this.names.get(Integer.valueOf(stack.getItemDamage()));
		return ret == null ? null : "ic2." + ret.name();
	}

	public void getSubItems(Item item, CreativeTabs tabs, List itemList)
	{
		for (int meta = 0; meta < 32767; ++meta)
		{
			ItemStack stack = new ItemStack(this, 1, meta);
			if (this.getUnlocalizedName(stack) == null)
			{
				break;
			}

			itemList.add(stack);
		}

	}

	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float xOffset, float yOffset, float zOffset)
	{
		if (!IC2.platform.isSimulating())
		{
			return false;
		}
		else
		{
			MovingObjectPosition mop = this.getMovingObjectPositionFromPlayer(world, player, true);
			if (mop == null)
			{
				return false;
			}
			else
			{
				if (mop.typeOfHit == MovingObjectType.BLOCK)
				{
					x = mop.blockX;
					y = mop.blockY;
					z = mop.blockZ;
					if (!world.canMineBlock(player, x, y, z))
					{
						return false;
					}

					if (!player.canPlayerEdit(x, y, z, mop.sideHit, stack))
					{
						return false;
					}
					// TODO gamerforEA code start
					if (FakePlayerUtils.cantBreak(x, y, z, player)) return false;
					// TODO gamerforEA code end
					if (stack.getItemDamage() == 0)
					{
						if (world.getBlockMetadata(x, y, z) == 0)
						{
							ItemStack fs = (ItemStack) this.cells.get(world.getBlock(x, y, z));
							if (fs != null && StackUtil.storeInventoryItem(fs.copy(), player, false))
							{
								world.setBlockToAir(x, y, z);
								--stack.stackSize;
								return true;
							}
						}
					}
					else
					{
						FluidStack fluid = FluidContainerRegistry.getFluidForFilledItem(stack);
						ForgeDirection dir = ForgeDirection.VALID_DIRECTIONS[mop.sideHit];
						if (fluid != null && LiquidUtil.placeFluid(fluid, world, x, y, z) || player.canPlayerEdit(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ, mop.sideHit, stack) && LiquidUtil.placeFluid(fluid, world, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ))
						{
							if (!player.capabilities.isCreativeMode)
							{
								--stack.stackSize;
							}

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
		this.names.put(Integer.valueOf(meta), name);
		ItemStack ret = new ItemStack(this, 1, meta);
		Block[] arr$ = blocks;
		int len$ = blocks.length;

		for (int i$ = 0; i$ < len$; ++i$)
		{
			Block block = arr$[i$];
			this.cells.put(block, ret);
		}

		return ret;
	}

	private ItemStack addRegisterCell(int meta, InternalName name, InternalName blockName)
	{
		ItemStack ret = this.addCell(meta, name, new Block[] { BlocksItems.getFluidBlock(blockName) });
		FluidContainerRegistry.registerFluidContainer(BlocksItems.getFluid(blockName), ret.copy(), Ic2Items.cell.copy());
		return ret;
	}
}