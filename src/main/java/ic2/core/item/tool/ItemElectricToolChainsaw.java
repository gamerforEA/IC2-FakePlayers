package ic2.core.item.tool;

import java.util.EnumSet;

import com.gamerforea.eventhelper.util.EventUtils;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ic2.api.item.ElectricItem;
import ic2.core.IC2;
import ic2.core.IHitSoundOverride;
import ic2.core.audio.AudioSource;
import ic2.core.audio.PositionSpec;
import ic2.core.init.InternalName;
import ic2.core.util.StackUtil;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.EntityInteractEvent;

public class ItemElectricToolChainsaw extends ItemElectricTool implements IHitSoundOverride
{
	public static boolean wasEquipped = false;
	public static AudioSource audioSource;

	public ItemElectricToolChainsaw(InternalName internalName)
	{
		this(internalName, 100, ItemElectricTool.HarvestLevel.Iron);
	}

	public ItemElectricToolChainsaw(InternalName internalName, int operationEnergyCost, ItemElectricTool.HarvestLevel harvestLevel)
	{
		super(internalName, operationEnergyCost, harvestLevel, EnumSet.of(ItemElectricTool.ToolClass.Axe, ItemElectricTool.ToolClass.Sword, ItemElectricTool.ToolClass.Shears));
		this.maxCharge = 30000;
		this.transferLimit = 100;
		this.tier = 1;
		this.efficiencyOnProperMaterial = 12.0F;
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)
	{
		if (!IC2.platform.isSimulating())
			return super.onItemRightClick(stack, world, player);
		else
		{
			if (IC2.keyboard.isModeSwitchKeyDown(player))
			{
				NBTTagCompound compoundTag = StackUtil.getOrCreateNbtData(stack);
				if (compoundTag.getBoolean("disableShear"))
				{
					compoundTag.setBoolean("disableShear", false);
					IC2.platform.messagePlayer(player, "ic2.tooltip.mode", new Object[] { "ic2.tooltip.mode.normal" });
				}
				else
				{
					compoundTag.setBoolean("disableShear", true);
					IC2.platform.messagePlayer(player, "ic2.tooltip.mode", new Object[] { "ic2.tooltip.mode.noShear" });
				}
			}

			return super.onItemRightClick(stack, world, player);
		}
	}

	@Override
	public Multimap getAttributeModifiers(ItemStack stack)
	{
		Multimap<String, AttributeModifier> ret;
		if (ElectricItem.manager.canUse(stack, this.operationEnergyCost))
		{
			ret = HashMultimap.create();
			ret.put(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(), new AttributeModifier(field_111210_e, "Tool modifier", 9.0D, 0));
		}
		else
			ret = super.getAttributeModifiers(stack);

		return ret;
	}

	@Override
	public boolean hitEntity(ItemStack itemstack, EntityLivingBase entityliving, EntityLivingBase attacker)
	{
		ElectricItem.manager.use(itemstack, this.operationEnergyCost, attacker);
		if (attacker instanceof EntityPlayer && entityliving instanceof EntityCreeper && entityliving.getHealth() <= 0.0F)
			IC2.achievements.issueAchievement((EntityPlayer) attacker, "killCreeperChainsaw");

		return true;
	}

	@SubscribeEvent
	public void onEntityInteract(EntityInteractEvent event)
	{
		if (IC2.platform.isSimulating())
		{
			Entity entity = event.target;
			EntityPlayer player = event.entityPlayer;
			ItemStack itemstack = player.inventory.getStackInSlot(player.inventory.currentItem);
			if (itemstack != null && itemstack.getItem() == this && entity instanceof IShearable && !StackUtil.getOrCreateNbtData(itemstack).getBoolean("disableShear") && ElectricItem.manager.use(itemstack, this.operationEnergyCost, player))
			{
				IShearable target = (IShearable) entity;
				if (target.isShearable(itemstack, entity.worldObj, (int) entity.posX, (int) entity.posY, (int) entity.posZ))
				{
					// TODO gamerforEA code start
					if (EventUtils.cantDamage(player, entity))
						return;
					// TODO gamerforEA code end

					for (ItemStack stack : target.onSheared(itemstack, entity.worldObj, (int) entity.posX, (int) entity.posY, (int) entity.posZ, EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, itemstack)))
					{
						EntityItem ent = entity.entityDropItem(stack, 1.0F);
						ent.motionY += itemRand.nextFloat() * 0.05F;
						ent.motionX += (itemRand.nextFloat() - itemRand.nextFloat()) * 0.1F;
						ent.motionZ += (itemRand.nextFloat() - itemRand.nextFloat()) * 0.1F;
					}
				}
			}
		}
	}

	@Override
	public boolean onBlockStartBreak(ItemStack itemstack, int x, int y, int z, EntityPlayer player)
	{
		if (!IC2.platform.isSimulating())
			return false;
		else if (StackUtil.getOrCreateNbtData(itemstack).getBoolean("disableShear"))
			return false;
		else
		{
			Block block = player.worldObj.getBlock(x, y, z);
			if (block instanceof IShearable)
			{
				IShearable target = (IShearable) block;
				if (target.isShearable(itemstack, player.worldObj, x, y, z) && ElectricItem.manager.use(itemstack, this.operationEnergyCost, player))
				{
					for (ItemStack stack : target.onSheared(itemstack, player.worldObj, x, y, z, EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, itemstack)))
					{
						float f = 0.7F;
						double d = itemRand.nextFloat() * f + (1.0F - f) * 0.5D;
						double d1 = itemRand.nextFloat() * f + (1.0F - f) * 0.5D;
						double d2 = itemRand.nextFloat() * f + (1.0F - f) * 0.5D;
						EntityItem entityitem = new EntityItem(player.worldObj, x + d, y + d1, z + d2, stack);
						entityitem.delayBeforeCanPickup = 10;
						player.worldObj.spawnEntityInWorld(entityitem);
					}

					player.addStat(StatList.mineBlockStatArray[Block.getIdFromBlock(block)], 1);
				}
			}

			return false;
		}
	}

	@Override
	public void onUpdate(ItemStack itemstack, World world, Entity entity, int i, boolean flag)
	{
		boolean isEquipped = flag && entity instanceof EntityLivingBase;
		if (IC2.platform.isRendering())
		{
			if (isEquipped && !wasEquipped)
			{
				if (audioSource == null)
					audioSource = IC2.audioManager.createSource(entity, PositionSpec.Hand, "Tools/Chainsaw/ChainsawIdle.ogg", true, false, IC2.audioManager.getDefaultVolume());

				if (audioSource != null)
					audioSource.play();
			}
			else if (!isEquipped && audioSource != null)
			{
				audioSource.stop();
				audioSource.remove();
				audioSource = null;
				if (entity instanceof EntityLivingBase)
					IC2.audioManager.playOnce(entity, PositionSpec.Hand, "Tools/Chainsaw/ChainsawStop.ogg", true, IC2.audioManager.getDefaultVolume());
			}
			else if (audioSource != null)
				audioSource.updatePosition();

			wasEquipped = isEquipped;
		}

	}

	@Override
	public boolean onDroppedByPlayer(ItemStack item, EntityPlayer player)
	{
		if (audioSource != null)
		{
			audioSource.stop();
			audioSource.remove();
			audioSource = null;
		}

		return true;
	}

	@Override
	public String getHitSoundForBlock(int x, int y, int z)
	{
		String[] soundEffects = new String[] { "Tools/Chainsaw/ChainsawUseOne.ogg", "Tools/Chainsaw/ChainsawUseTwo.ogg" };
		return soundEffects[itemRand.nextInt(soundEffects.length)];
	}
}
