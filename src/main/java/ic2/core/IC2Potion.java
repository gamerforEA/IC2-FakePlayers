package ic2.core;

import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;

public class IC2Potion extends Potion
{
	public static final IC2Potion radiation = new IC2Potion(24, true, 5149489, new ItemStack[0]);
	private final List<ItemStack> curativeItems;

	public static void init()
	{
		radiation.setPotionName("ic2.potion.radiation");
		radiation.setIconIndex(6, 0);
		radiation.setEffectiveness(0.25D);
	}

	public IC2Potion(int id, boolean badEffect, int liquidColor, ItemStack... curativeItems)
	{
		super(id, badEffect, liquidColor);
		this.curativeItems = Arrays.asList(curativeItems);
	}

	public void performEffect(EntityLivingBase entity, int amplifier)
	{
		if (this.id == radiation.id)
		{
			// TODO gamerforEA code replace: entity.attackEntityFrom(IC2DamageSource.radiation, (float) (amplifier / 100) + 0.5F);
			entity.removePotionEffect(radiation.id);
			// TODO gamerforEA code end
		}
	}

	public boolean isReady(int duration, int amplifier)
	{
		if (this.id == radiation.id)
		{
			int rate = 25 >> amplifier;
			return rate > 0 ? duration % rate == 0 : true;
		}
		else
		{
			return false;
		}
	}

	public void applyTo(EntityLivingBase entity, int duration, int amplifier)
	{
		/* TODO gamerforEA code clear:
		PotionEffect effect = new PotionEffect(this.id, duration, amplifier);
		effect.setCurativeItems(this.curativeItems);
		entity.addPotionEffect(effect); */
	}
}