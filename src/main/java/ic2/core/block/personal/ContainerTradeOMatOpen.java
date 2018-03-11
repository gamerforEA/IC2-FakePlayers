package ic2.core.block.personal;

import ic2.core.ContainerFullInv;
import ic2.core.slot.SlotInvSlot;
import net.minecraft.entity.player.EntityPlayer;

import java.util.List;

public class ContainerTradeOMatOpen extends ContainerFullInv<TileEntityTradeOMat>
{
	public ContainerTradeOMatOpen(EntityPlayer entityPlayer, TileEntityTradeOMat tileEntity1)
	{
		super(entityPlayer, tileEntity1, 166);
		this.addSlotToContainer(new SlotInvSlot(tileEntity1.demandSlot, 0, 50, 19));
		this.addSlotToContainer(new SlotInvSlot(tileEntity1.offerSlot, 0, 50, 53));
		this.addSlotToContainer(new SlotInvSlot(tileEntity1.inputSlot, 0, 80, 19));
		this.addSlotToContainer(new SlotInvSlot(tileEntity1.outputSlot, 0, 80, 53));

		// TODO gamerforEA code start
		tileEntity1.isOpened = true;
		// TODO gamerforEA code end
	}

	// TODO gamerforEA code start
	@Override
	public void onContainerClosed(EntityPlayer player)
	{
		this.base.isOpened = false;
		super.onContainerClosed(player);
	}
	// TODO gamerforEA code end

	@Override
	public List<String> getNetworkedFields()
	{
		List<String> ret = super.getNetworkedFields();
		ret.add("stock");
		ret.add("totalTradeCount");
		return ret;
	}
}
