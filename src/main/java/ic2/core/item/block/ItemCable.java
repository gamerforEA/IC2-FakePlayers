package ic2.core.item.block;

import ic2.api.item.IBoxable;
import ic2.core.Ic2Items;
import ic2.core.block.wiring.BlockCable;
import ic2.core.block.wiring.TileEntityCable;
import ic2.core.init.InternalName;
import ic2.core.item.ItemIC2;
import ic2.core.util.StackUtil;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.List;

public class ItemCable extends ItemIC2 implements IBoxable
{
	public ItemCable(InternalName internalName)
	{
		super(internalName);
		this.setHasSubtypes(true);
		Ic2Items.copperCableItem = new ItemStack(this, 1, 1);
		Ic2Items.insulatedCopperCableItem = new ItemStack(this, 1, 0);
		Ic2Items.goldCableItem = new ItemStack(this, 1, 2);
		Ic2Items.insulatedGoldCableItem = new ItemStack(this, 1, 3);
		Ic2Items.doubleInsulatedGoldCableItem = new ItemStack(this, 1, 4);
		Ic2Items.ironCableItem = new ItemStack(this, 1, 5);
		Ic2Items.insulatedIronCableItem = new ItemStack(this, 1, 6);
		Ic2Items.doubleInsulatedIronCableItem = new ItemStack(this, 1, 7);
		Ic2Items.trippleInsulatedIronCableItem = new ItemStack(this, 1, 8);
		Ic2Items.glassFiberCableItem = new ItemStack(this, 1, 9);
		Ic2Items.tinCableItem = new ItemStack(this, 1, 10);
		Ic2Items.detectorCableItem = new ItemStack(this, 1, 11);
		Ic2Items.splitterCableItem = new ItemStack(this, 1, 12);
		Ic2Items.insulatedTinCableItem = new ItemStack(this, 1, 13);
	}

	@Override
	public String getUnlocalizedName(ItemStack itemstack)
	{
		int meta = itemstack.getItemDamage();
		InternalName ret;
		switch (meta)
		{
			case 0:
				ret = InternalName.itemCable;
				break;
			case 1:
				ret = InternalName.itemCableO;
				break;
			case 2:
				ret = InternalName.itemGoldCable;
				break;
			case 3:
				ret = InternalName.itemGoldCableI;
				break;
			case 4:
				ret = InternalName.itemGoldCableII;
				break;
			case 5:
				ret = InternalName.itemIronCable;
				break;
			case 6:
				ret = InternalName.itemIronCableI;
				break;
			case 7:
				ret = InternalName.itemIronCableII;
				break;
			case 8:
				ret = InternalName.itemIronCableIIII;
				break;
			case 9:
				ret = InternalName.itemGlassCable;
				break;
			case 10:
				ret = InternalName.itemTinCable;
				break;
			case 11:
				ret = InternalName.itemDetectorCable;
				break;
			case 12:
				ret = InternalName.itemSplitterCable;
				break;
			case 13:
				ret = InternalName.itemTinCableI;
				break;
			default:
				return null;
		}

		return "ic2." + ret.name();
	}

	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer player, List info, boolean b)
	{
		int capacity = TileEntityCable.getMaxCapacity(itemStack.getItemDamage());
		info.add(capacity + " EU/t");
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float a, float b, float c)
	{
		Block oldBlock = world.getBlock(x, y, z);
		if (!oldBlock.isAir(world, x, y, z))
			if (oldBlock == Blocks.snow_layer)
				side = 1;
			else if (oldBlock != Blocks.vine && oldBlock != Blocks.tallgrass && oldBlock != Blocks.deadbush && !oldBlock.isReplaceable(world, x, y, z))
				switch (side)
				{
					case 0:
						--y;
						break;
					case 1:
						++y;
						break;
					case 2:
						--z;
						break;
					case 3:
						++z;
						break;
					case 4:
						--x;
						break;
					case 5:
						++x;
				}

		// TODO gamerforEA code start
		if (!world.isAirBlock(x, y, z))
		{
			Block bl = world.getBlock(x, y, z);
			if (bl != Blocks.vine && bl != Blocks.tallgrass && bl != Blocks.deadbush && !bl.isReplaceable(world, x, y, z))
				return false;
		}
		// TODO gamerforEA code end

		BlockCable block = (BlockCable) StackUtil.getBlock(Ic2Items.insulatedCopperCableBlock);

		// TODO gamerforEA replace Block.isAir to World.isAirBlock [1]
		if ((world.isAirBlock(x, y, z) || world.canPlaceEntityOnSide(StackUtil.getBlock(Ic2Items.insulatedCopperCableBlock), x, y, z, false, side, player, stack)) && world.checkNoEntityCollision(block.getCollisionBoundingBoxFromPool(world, x, y, z, stack.getItemDamage())) && world.setBlock(x, y, z, block, stack.getItemDamage(), 3))
		{
			block.onPostBlockPlaced(world, x, y, z, side);
			block.onBlockPlacedBy(world, x, y, z, player, stack);
			if (!player.capabilities.isCreativeMode)
				--stack.stackSize;

			return true;
		}
		else
			return false;
	}

	@Override
	public void getSubItems(Item item, CreativeTabs tabs, List itemList)
	{
		for (int meta = 0; meta < 32767; ++meta)
		{
			if (meta != 4 && meta != 7 && meta != 8)
			{
				ItemStack stack = new ItemStack(this, 1, meta);
				if (this.getUnlocalizedName(stack) != null)
					itemList.add(stack);
			}
		}

	}

	@Override
	public boolean canBeStoredInToolbox(ItemStack itemstack)
	{
		return true;
	}
}
