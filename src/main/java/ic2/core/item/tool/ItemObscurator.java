package ic2.core.item.tool;

import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.event.RetextureEvent;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IItemHudInfo;
import ic2.core.IC2;
import ic2.core.init.InternalName;
import ic2.core.item.ItemIC2;
import ic2.core.item.tool.RenderObscurator;
import ic2.core.network.IPlayerItemDataListener;
import ic2.core.network.NetworkManager;
import ic2.core.util.StackUtil;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;

public class ItemObscurator extends ItemIC2 implements IElectricItem, IPlayerItemDataListener, IItemHudInfo {
   private final int scanOperationCost = 20000;
   private final int printOperationCost = 5000;

   public ItemObscurator(InternalName internalName) {
      super(internalName);
      this.setMaxDamage(27);
      this.setMaxStackSize(1);
      this.setNoRepair();
      if(IC2.platform.isRendering()) {
         MinecraftForgeClient.registerItemRenderer(this, new RenderObscurator());
      }

   }

   public List<String> getHudInfo(ItemStack itemStack) {
      List<String> info = new LinkedList();
      info.add(ElectricItem.manager.getToolTip(itemStack));
      return info;
   }

   public boolean onItemUseFirst(ItemStack itemStack, EntityPlayer entityPlayer, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
      if(!entityPlayer.isSneaking() && ElectricItem.manager.canUse(itemStack, 5000.0D)) {
         NBTTagCompound nbtData = StackUtil.getOrCreateNbtData(itemStack);
         String referencedBlockName = nbtData.getString("referencedBlock");
         Block referencedBlock = referencedBlockName.isEmpty()?null:(Block)GameData.getBlockRegistry().getRaw(referencedBlockName);
         if(referencedBlock == null || !isBlockSuitable(referencedBlock)) {
            return false;
         }

         if(IC2.platform.isSimulating()) {
            RetextureEvent event = new RetextureEvent(world, x, y, z, side, referencedBlock, nbtData.getInteger("referencedMeta"), nbtData.getInteger("referencedSide"));
            MinecraftForge.EVENT_BUS.post(event);
            if(event.applied) {
               ElectricItem.manager.use(itemStack, 5000.0D, entityPlayer);
               return true;
            }

            return false;
         }
      } else if(entityPlayer.isSneaking() && IC2.platform.isRendering() && ElectricItem.manager.canUse(itemStack, 20000.0D)) {
         Block block = world.getBlock(x, y, z);
         if(!block.isAir(world, x, y, z) && isBlockSuitable(block)) {
            int meta = world.getBlockMetadata(x, y, z);

            try {
               IIcon texture = block.getIcon(side, meta);
               IIcon textureWorld = block.getIcon(world, x, y, z, side);
               if(texture == null || texture != textureWorld) {
                  return false;
               }
            } catch (Exception var15) {
               return false;
            }

            String referencedBlockName = GameData.getBlockRegistry().getNameForObject(block);
            NBTTagCompound nbtData = StackUtil.getOrCreateNbtData(itemStack);
            if(!nbtData.getString("referencedBlock").equals(referencedBlockName) || nbtData.getInteger("referencedMeta") != meta || nbtData.getInteger("referencedSide") != side) {
               ((NetworkManager)IC2.network.get()).sendPlayerItemData(entityPlayer, entityPlayer.inventory.currentItem, new Object[]{referencedBlockName, Integer.valueOf(meta), Integer.valueOf(side)});
               return true;
            }
         }
      }

      return false;
   }

   public void onPlayerItemNetworkData(EntityPlayer entityPlayer, int slot, Object... data) {
      ItemStack itemStack = entityPlayer.inventory.mainInventory[slot];
      if(ElectricItem.manager.use(itemStack, 20000.0D, entityPlayer)) {
         NBTTagCompound nbtData = StackUtil.getOrCreateNbtData(itemStack);
         nbtData.setString("referencedBlock", (String)data[0]);
         nbtData.setInteger("referencedMeta", ((Integer)data[1]).intValue());
         nbtData.setInteger("referencedSide", ((Integer)data[2]).intValue());
      }

   }

   @SideOnly(Side.CLIENT)
   public void getSubItems(Item item, CreativeTabs tabs, List itemList) {
      ItemStack charged = new ItemStack(this, 1);
      ElectricItem.manager.charge(charged, Double.POSITIVE_INFINITY, Integer.MAX_VALUE, true, false);
      itemList.add(charged);
      itemList.add(new ItemStack(this, 1, this.getMaxDamage()));
   }

   public boolean canProvideEnergy(ItemStack itemStack) {
      return false;
   }

   public Item getChargedItem(ItemStack itemStack) {
      return this;
   }

   public Item getEmptyItem(ItemStack itemStack) {
      return this;
   }

   public double getMaxCharge(ItemStack itemStack) {
      return 100000.0D;
   }

   public int getTier(ItemStack itemStack) {
      return 2;
   }

   public double getTransferLimit(ItemStack itemStack) {
      return 250.0D;
   }

   private static boolean isBlockSuitable(Block block) {
      // TODO synthetic code clear: return block.renderAsNormalBlock();
      return block.renderAsNormalBlock() && block.isOpaqueCube();
   }
}
