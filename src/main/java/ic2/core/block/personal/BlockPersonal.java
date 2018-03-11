package ic2.core.block.personal;

import com.gamerforea.ic2.EventConfig;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.core.IC2;
import ic2.core.Ic2Items;
import ic2.core.block.BlockMultiID;
import ic2.core.block.TileEntityBlock;
import ic2.core.init.InternalName;
import ic2.core.item.block.ItemPersonalBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.ArrayList;

public class BlockPersonal extends BlockMultiID
{
	public BlockPersonal(InternalName internalName1)
	{
		super(internalName1, Material.iron, ItemPersonalBlock.class);
		this.setBlockUnbreakable();
		this.setResistance(6000000.0F);
		this.setStepSound(soundTypeMetal);
		this.canBlockGrass = false;
		Ic2Items.personalSafe = new ItemStack(this, 1, 0);
		Ic2Items.tradeOMat = new ItemStack(this, 1, 1);
		Ic2Items.energyOMat = new ItemStack(this, 1, 2);
		GameRegistry.registerTileEntity(TileEntityPersonalChest.class, "Personal Safe");
		GameRegistry.registerTileEntity(TileEntityTradeOMat.class, "Trade-O-Mat");
		GameRegistry.registerTileEntity(TileEntityEnergyOMat.class, "Energy-O-Mat");
	}

	@Override
	public String getTextureFolder(int id)
	{
		return "personal";
	}

	@Override
	public int damageDropped(int meta)
	{
		return meta;
	}

	@Override
	public boolean renderAsNormalBlock()
	{
		return false;
	}

	@Override
	public int getRenderType()
	{
		return IC2.platform.getRenderId("personal");
	}

	@Override
	public boolean isOpaqueCube()
	{
		return false;
	}

	@Override
	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune)
	{
		if (IC2.platform.isSimulating() && IC2.platform.isRendering())
			return super.getDrops(world, x, y, z, metadata, fortune);
		else
		{
			ArrayList<ItemStack> ret = new ArrayList();
			ret.add(new ItemStack(this, 1, metadata));
			return ret;
		}
	}

	@Override
	public Class<? extends TileEntity> getTeClass(int meta, MutableObject<Class<?>[]> ctorArgTypes, MutableObject<Object[]> ctorArgs)
	{
		try
		{
			switch (meta)
			{
				case 0:
					return TileEntityPersonalChest.class;
				case 1:
					return TileEntityTradeOMat.class;
				case 2:
					return TileEntityEnergyOMat.class;
				default:
					return null;
			}
		}
		catch (Exception var5)
		{
			throw new RuntimeException(var5);
		}
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float xOffset, float yOffset, float zOffset)
	{
		if (player.isSneaking())
			return false;
		else
		{
			int meta = world.getBlockMetadata(x, y, z);
			TileEntityBlock te = (TileEntityBlock) this.getOwnTe(world, x, y, z);
			if (te == null)
				return false;
			else if (IC2.platform.isSimulating() && meta != 1 && meta != 2 && te instanceof IPersonalBlock && !((IPersonalBlock) te).permitsAccess(player.getGameProfile()))
			{
				IC2.platform.messagePlayer(player, "This safe is owned by " + ((IPersonalBlock) te).getOwner().getName());
				return false;
			}
			else
			{
				// TODO gamerforEA code start
				if (EventConfig.tradeOMatOnePlayer && te instanceof TileEntityTradeOMat && ((TileEntityTradeOMat) te).isOpened)
				{
					player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Данный торговый аппарат уже кем-то открыт"));
					return false;
				}
				// TODO gamerforEA code end

				return super.onBlockActivated(world, x, y, z, player, side, xOffset, yOffset, zOffset);
			}
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public EnumRarity getRarity(ItemStack stack)
	{
		return stack.getItemDamage() == 0 ? EnumRarity.uncommon : EnumRarity.common;
	}

	@Override
	public boolean canEntityDestroy(IBlockAccess world, int x, int y, int z, Entity entity)
	{
		return false;
	}
}
