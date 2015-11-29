package ic2.core.item.tool;

import java.util.List;

import com.gamerforea.eventhelper.util.EventUtils;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ic2.api.event.PaintEvent;
import ic2.api.item.IBoxable;
import ic2.core.IC2;
import ic2.core.Ic2Items;
import ic2.core.audio.PositionSpec;
import ic2.core.init.InternalName;
import ic2.core.item.ItemIC2;
import ic2.core.util.StackUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.BlockStainedGlassPane;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.oredict.OreDictionary;

public class ItemToolPainter extends ItemIC2 implements IBoxable
{
	private static final String[] dyes = new String[] { "dyeBlack", "dyeRed", "dyeGreen", "dyeBrown", "dyeBlue", "dyePurple", "dyeCyan", "dyeLightGray", "dyeGray", "dyePink", "dyeLime", "dyeYellow", "dyeLightBlue", "dyeMagenta", "dyeOrange", "dyeWhite" };
	public final int color;

	public ItemToolPainter(InternalName internalName, int col)
	{
		super(internalName);
		this.setMaxDamage(32);
		this.setMaxStackSize(1);
		this.color = col;
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Override
	public boolean onItemUseFirst(ItemStack itemstack, EntityPlayer entityplayer, World world, int i, int j, int k, int side, float a, float b, float c)
	{
		PaintEvent event = new PaintEvent(world, i, j, k, side, this.color);
		MinecraftForge.EVENT_BUS.post(event);
		if (event.painted)
		{
			if (IC2.platform.isSimulating())
				this.damagePainter(entityplayer);

			if (IC2.platform.isRendering())
				IC2.audioManager.playOnce(entityplayer, PositionSpec.Hand, "Tools/Painter.ogg", true, IC2.audioManager.getDefaultVolume());

			return IC2.platform.isSimulating();
		}
		else
		{
			// TODO gamerforEA code start
			if (EventUtils.cantBreak(entityplayer, i, j, k))
				return false;
			// TODO gamerforEA code end

			Block block = world.getBlock(i, j, k);
			int targetMeta = BlockColored.func_150031_c(this.color);
			if (!block.recolourBlock(world, i, j, k, ForgeDirection.VALID_DIRECTIONS[side], targetMeta) && !this.colorBlock(world, i, j, k, block, targetMeta))
				return false;
			else
			{
				this.damagePainter(entityplayer);
				if (IC2.platform.isRendering())
					IC2.audioManager.playOnce(entityplayer, PositionSpec.Hand, "Tools/Painter.ogg", true, IC2.audioManager.getDefaultVolume());

				return IC2.platform.isSimulating();
			}
		}
	}

	private boolean colorBlock(World world, int x, int y, int z, Block block, int targetMeta)
	{
		if (!(block instanceof BlockColored) && !(block instanceof BlockStainedGlass) && !(block instanceof BlockStainedGlassPane))
		{
			if (block == Blocks.hardened_clay)
			{
				world.setBlock(x, y, z, Blocks.stained_hardened_clay, targetMeta, 3);
				return true;
			}
			else if (block == Blocks.glass)
			{
				world.setBlock(x, y, z, Blocks.stained_glass, targetMeta, 3);
				return true;
			}
			else if (block == Blocks.glass_pane)
			{
				world.setBlock(x, y, z, Blocks.stained_glass_pane, targetMeta, 3);
				return true;
			}
			else
				return false;
		}
		else
		{
			int meta = world.getBlockMetadata(x, y, z);
			if (meta == targetMeta)
				return false;
			else
			{
				world.setBlockMetadataWithNotify(x, y, z, targetMeta, 3);
				return true;
			}
		}
	}

	@SubscribeEvent
	public boolean onEntityInteract(EntityInteractEvent event)
	{
		EntityPlayer player = event.entityPlayer;
		Entity entity = event.entity;
		if (!entity.worldObj.isRemote && player.getCurrentEquippedItem() != null && player.getCurrentEquippedItem().getItem() == this)
		{
			boolean ret = true;
			if (entity instanceof EntitySheep)
			{
				EntitySheep sheep = (EntitySheep) entity;
				int clr = BlockColored.func_150031_c(this.color);
				if (sheep.getFleeceColor() != clr)
				{
					ret = false;
					((EntitySheep) entity).setFleeceColor(clr);
					this.damagePainter(player);
				}
			}

			return ret;
		}
		else
			return true;
	}

	@Override
	public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer entityplayer)
	{
		if (IC2.platform.isSimulating() && IC2.keyboard.isModeSwitchKeyDown(entityplayer))
		{
			NBTTagCompound nbtData = StackUtil.getOrCreateNbtData(itemstack);
			boolean newValue = !nbtData.getBoolean("autoRefill");
			nbtData.setBoolean("autoRefill", newValue);
			if (newValue)
				IC2.platform.messagePlayer(entityplayer, "Painter automatic refill mode enabled", new Object[0]);
			else
				IC2.platform.messagePlayer(entityplayer, "Painter automatic refill mode disabled", new Object[0]);
		}

		return itemstack;
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List info, boolean debugTooltips)
	{
		info.add(StatCollector.translateToLocal(Items.dye.getUnlocalizedName(new ItemStack(Items.dye, 1, this.color)) + ".name"));
	}

	private void damagePainter(EntityPlayer player)
	{
		if (player.inventory.mainInventory[player.inventory.currentItem].getItemDamage() >= player.inventory.mainInventory[player.inventory.currentItem].getMaxDamage() - 1)
		{
			int dyeIS = -1;
			NBTTagCompound nbtData = StackUtil.getOrCreateNbtData(player.inventory.mainInventory[player.inventory.currentItem]);
			if (nbtData.getBoolean("autoRefill"))
				for (int l = 0; l < player.inventory.mainInventory.length; ++l)
					if (player.inventory.mainInventory[l] != null)
						for (ItemStack ore : OreDictionary.getOres(dyes[this.color]))
							if (ore.isItemEqual(player.inventory.mainInventory[l]))
							{
								dyeIS = l;
								break;
							}

			if (dyeIS == -1)
				player.inventory.mainInventory[player.inventory.currentItem] = Ic2Items.painter.copy();
			else
			{
				if (--player.inventory.mainInventory[dyeIS].stackSize <= 0)
					player.inventory.mainInventory[dyeIS] = null;

				player.inventory.mainInventory[player.inventory.currentItem].setItemDamage(0);
			}
		}
		else
			player.inventory.mainInventory[player.inventory.currentItem].damageItem(1, player);

		player.openContainer.detectAndSendChanges();
	}

	@Override
	public boolean canBeStoredInToolbox(ItemStack itemstack)
	{
		return true;
	}
}
