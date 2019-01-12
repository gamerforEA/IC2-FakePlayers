package ic2.core.slot;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

public class SlotArmor extends Slot
{
	private final int armorType;

	public SlotArmor(InventoryPlayer inventory1, int armorType1, int xDisplayPosition1, int yDisplayPosition1)
	{
		super(inventory1, 36 + 3 - armorType1, xDisplayPosition1, yDisplayPosition1);
		this.armorType = armorType1;
	}

	@Override
	public boolean isItemValid(ItemStack itemStack)
	{
		// TODO gamerforEA code start
		if (itemStack == null)
			return false;
		// TODO gamerforEA code end

		Item item = itemStack.getItem();
		return item != null && item.isValidArmor(itemStack, this.armorType, ((InventoryPlayer) this.inventory).player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getBackgroundIconIndex()
	{
		return ItemArmor.func_94602_b(this.armorType);
	}
}
