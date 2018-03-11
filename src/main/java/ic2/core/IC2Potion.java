package ic2.core;

import com.gamerforea.ic2.EventConfig;
import com.google.common.base.Throwables;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class IC2Potion extends Potion
{
	public static IC2Potion radiation;
	private final List<ItemStack> curativeItems;

	public static void init()
	{
		radiation.setPotionName("ic2.potion.radiation");
		radiation.setIconIndex(6, 0);
		radiation.setEffectiveness(0.25D);
	}

	public static IC2Potion registerPotion(int id, boolean badEffect, int liquidColor, ItemStack... curativeItems1)
	{
		if (id >= Potion.potionTypes.length)
		{
			Potion[] potionNew = new Potion[Math.max(256, id)];
			System.arraycopy(Potion.potionTypes, 0, potionNew, 0, Potion.potionTypes.length);
			Field f = ReflectionHelper.findField(Potion.class, "field_76425_a", "potionTypes");
			f.setAccessible(true);

			try
			{
				Field modfield = Field.class.getDeclaredField("modifiers");
				modfield.setAccessible(true);
				modfield.setInt(f, f.getModifiers() & -17);
				f.set(null, potionNew);
			}
			catch (Exception var7)
			{
				Throwables.propagate(var7);
			}
		}

		return new IC2Potion(id, badEffect, liquidColor, curativeItems1);
	}

	private IC2Potion(int id1, boolean badEffect, int liquidColor, ItemStack... curativeItems1)
	{
		super(id1, badEffect, liquidColor);
		this.curativeItems = Arrays.asList(curativeItems1);
	}

	@Override
	public void performEffect(EntityLivingBase entity, int amplifier)
	{
		if (this.id == radiation.id)
		{
			// TODO gameroforEA code start
			if (!EventConfig.radiationEnabled)
				return;
			// TODO gamerforEA code end

			entity.attackEntityFrom(IC2DamageSource.radiation, amplifier / 100 + 0.5F);
		}
	}

	@Override
	public boolean isReady(int duration, int amplifier)
	{
		if (this.id == radiation.id)
		{
			int rate = 25 >> amplifier;
			return rate <= 0 || duration % rate == 0;
		}
		else
			return false;
	}

	public void applyTo(EntityLivingBase entity, int duration, int amplifier)
	{
		// TODO gameroforEA code start
		if (!EventConfig.radiationEnabled)
			return;
		// TODO gamerforEA code end

		PotionEffect effect = new PotionEffect(this.id, duration, amplifier);
		effect.setCurativeItems(this.curativeItems);
		entity.addPotionEffect(effect);
	}
}
