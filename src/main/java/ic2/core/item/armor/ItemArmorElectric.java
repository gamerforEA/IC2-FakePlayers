package ic2.core.item.armor;

import ic2.api.item.ElectricItem;
import ic2.api.item.ICustomDamageItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IItemHudInfo;
import ic2.core.IC2;
import ic2.core.init.InternalName;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.ISpecialArmor;

import java.util.LinkedList;
import java.util.List;

public abstract class ItemArmorElectric extends ItemArmorIC2
		implements ISpecialArmor, IElectricItem, IItemHudInfo, ICustomDamageItem
{
	protected final double maxCharge;
	protected final double transferLimit;
	protected final int tier;
	private final ThreadLocal<Boolean> allowDamaging = new ThreadLocal();

	public ItemArmorElectric(InternalName internalName, InternalName armorName, int armorType1, double maxCharge1, double transferLimit1, int tier1)
	{
		super(internalName, ArmorMaterial.DIAMOND, armorName, armorType1, null);
		this.maxCharge = maxCharge1;
		this.tier = tier1;
		this.transferLimit = transferLimit1;
		this.setMaxDamage(27);
		this.setMaxStackSize(1);
		this.setNoRepair();
	}

	@Override
	public int getItemEnchantability()
	{
		return 0;
	}

	@Override
	public boolean isBookEnchantable(ItemStack stack, ItemStack book)
	{
		return false;
	}

	@Override
	public List<String> getHudInfo(ItemStack itemStack)
	{
		List<String> info = new LinkedList();
		info.add(ElectricItem.manager.getToolTip(itemStack));
		info.add(StatCollector.translateToLocal("ic2.item.tooltip.PowerTier") + " " + this.tier);
		return info;
	}

	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer player, List info, boolean b)
	{
		info.add(StatCollector.translateToLocal("ic2.item.tooltip.PowerTier") + " " + this.tier);
	}

	@Override
	public void getSubItems(Item item, CreativeTabs tabs, List itemList)
	{
		ItemStack charged = new ItemStack(this, 1);
		ElectricItem.manager.charge(charged, Double.POSITIVE_INFINITY, Integer.MAX_VALUE, true, false);
		itemList.add(charged);
		itemList.add(new ItemStack(this, 1, this.getMaxDamage()));
	}

	@Override
	public ArmorProperties getProperties(EntityLivingBase player, ItemStack armor, DamageSource source, double damage, int slot)
	{
		if (source.isUnblockable())
			return new ArmorProperties(0, 0.0D, 0);
		else
		{
			double absorptionRatio = this.getBaseAbsorptionRatio() * this.getDamageAbsorptionRatio();
			int energyPerDamage = this.getEnergyPerDamage();
			int damageLimit = Integer.MAX_VALUE;
			if (energyPerDamage > 0)
				damageLimit = (int) Math.min(damageLimit, 25.0D * ElectricItem.manager.getCharge(armor) / energyPerDamage);

			return new ArmorProperties(0, absorptionRatio, damageLimit);
		}
	}

	@Override
	public int getArmorDisplay(EntityPlayer player, ItemStack armor, int slot)
	{
		return ElectricItem.manager.getCharge(armor) >= this.getEnergyPerDamage() ? (int) Math.round(20.0D * this.getBaseAbsorptionRatio() * this.getDamageAbsorptionRatio()) : 0;
	}

	@Override
	public void damageArmor(EntityLivingBase entity, ItemStack stack, DamageSource source, int damage, int slot)
	{
		ElectricItem.manager.discharge(stack, damage * this.getEnergyPerDamage(), Integer.MAX_VALUE, true, false, false);
	}

	@Override
	public boolean canProvideEnergy(ItemStack itemStack)
	{
		return false;
	}

	@Override
	public Item getChargedItem(ItemStack itemStack)
	{
		return this;
	}

	@Override
	public Item getEmptyItem(ItemStack itemStack)
	{
		return this;
	}

	@Override
	public double getMaxCharge(ItemStack itemStack)
	{
		return this.maxCharge;
	}

	@Override
	public int getTier(ItemStack itemStack)
	{
		return this.tier;
	}

	@Override
	public double getTransferLimit(ItemStack itemStack)
	{
		return this.transferLimit;
	}

	@Override
	public boolean getIsRepairable(ItemStack par1ItemStack, ItemStack par2ItemStack)
	{
		return false;
	}

	@Override
	public void setDamage(ItemStack stack, int damage)
	{
		if (damage != stack.getItemDamage())
		{
			Boolean allow = this.allowDamaging.get();
			if (allow != null && allow)
				super.setDamage(stack, damage);

			/* TODO gamerforEA code clear:
			else
				IC2.log.warn(LogCategory.Item, new Throwable(), "Detected invalid armor damage application:"); */
		}
	}

	@Override
	public int getCustomDamage(ItemStack stack)
	{
		return stack.getItemDamage();
	}

	@Override
	public int getMaxCustomDamage(ItemStack stack)
	{
		return stack.getMaxDamage();
	}

	@Override
	public void setCustomDamage(ItemStack stack, int damage)
	{
		this.allowDamaging.set(Boolean.TRUE);
		stack.setItemDamage(damage);
		this.allowDamaging.set(Boolean.FALSE);
	}

	@Override
	public boolean applyCustomDamage(ItemStack stack, int damage, EntityLivingBase src)
	{
		if (src != null)
		{
			stack.damageItem(damage, src);
			return true;
		}
		else
			return stack.attemptDamageItem(damage, IC2.random);
	}

	public abstract double getDamageAbsorptionRatio();

	public abstract int getEnergyPerDamage();

	private double getBaseAbsorptionRatio()
	{
		switch (this.armorType)
		{
			case 0:
				return 0.15D;
			case 1:
				return 0.4D;
			case 2:
				return 0.3D;
			case 3:
				return 0.15D;
			default:
				return 0.0D;
		}
	}
}
