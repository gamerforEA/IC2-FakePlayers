package com.gamerforea.ic2;

import static net.minecraftforge.common.config.Configuration.CATEGORY_GENERAL;

import java.util.Set;

import com.gamerforea.eventhelper.util.FastUtils;
import com.google.common.collect.Sets;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.common.config.Configuration;

public final class EventConfig
{
	public static final Set<String> tradeOMatBlackList = Sets.newHashSet("minecraft:stone", "IC2:blockMachine:5");
	public static final Set<String> scannerBlackList = Sets.newHashSet("minecraft:stone", "IC2:blockMachine:5");

	public static boolean terraEvent = true;
	public static boolean pumpEvent = true;
	public static boolean minerEvent = true;
	public static boolean advminerEvent = true;
	public static boolean teslaEvent = true;
	public static boolean sprayerEvent = true;
	public static boolean laserEvent = true;
	public static boolean plasmaEvent = true;
	public static boolean explosionEvent = true;

	public static boolean plasmaEnabled = false;
	public static boolean radiationEnabled = false;
	public static boolean explosionEnabled = false;
	public static boolean laserscatterEnabled = true;
	public static boolean boozeCombineEffectEnabled = false;
	public static boolean scrapboxDropEnabled = true;

	public static String safeAccessPermission = "ic2.accesssafe";
	public static boolean skipTicks = false;

	static
	{
		try
		{
			Configuration cfg = FastUtils.getConfig("IC2");

			terraEvent = cfg.getBoolean("terraEvent", CATEGORY_GENERAL, terraEvent, "Терраформер (замена/установка блоков)");
			pumpEvent = cfg.getBoolean("pumpEvent", CATEGORY_GENERAL, pumpEvent, "Помпа (выкачивание жидкости)");
			minerEvent = cfg.getBoolean("minerEvent", CATEGORY_GENERAL, minerEvent, "Буровая установка (разрушение блоков)");
			advminerEvent = cfg.getBoolean("advMinerEvent", CATEGORY_GENERAL, advminerEvent, "Продвинутая буровая установка (разрушение блоков)");
			teslaEvent = cfg.getBoolean("teslaEvent", CATEGORY_GENERAL, teslaEvent, "Катушка Теслы (урон по мобам)");
			sprayerEvent = cfg.getBoolean("sprayerEvent", CATEGORY_GENERAL, sprayerEvent, "Пульверизатор (установка блоков)");
			laserEvent = cfg.getBoolean("laserEvent", CATEGORY_GENERAL, laserEvent, "Шахтёрский лазер (разрушение блоков и урон по мобам)");
			plasmaEvent = cfg.getBoolean("plasmaEvent", CATEGORY_GENERAL, plasmaEvent, "Плазменная пушка (взрыв)");
			explosionEvent = cfg.getBoolean("explosionEvent", CATEGORY_GENERAL, explosionEvent, "Взрывы (разрушение блоков)");

			plasmaEnabled = cfg.getBoolean("plasmaEnabled", "other", plasmaEnabled, "Плазменная пушка");
			radiationEnabled = cfg.getBoolean("radiationEnabled", "other", radiationEnabled, "Радиация");
			explosionEnabled = cfg.getBoolean("explosionEnabled", "other", explosionEnabled, "Взрывы");
			laserscatterEnabled = cfg.getBoolean("laserscatterEnabled", "other", laserscatterEnabled, "Шахтёрский лазер (режим \"Разброс\")");
			boozeCombineEffectEnabled = cfg.getBoolean("boozeCombineEffectEnabled", "other", boozeCombineEffectEnabled, "Комбинирование эффектов от выпивки");
			scrapboxDropEnabled = cfg.getBoolean("scrapbosDropEnabled", "other", scrapboxDropEnabled, "Дроп предметов из Утильсырья");

			safeAccessPermission = cfg.getString("safeAccessPermission", "other", safeAccessPermission, "Permission для доступа к персональным блокам (сейфам, торговым аппаратам и пр.)");
			skipTicks = cfg.getBoolean("skipTicks", "other", skipTicks, "Пропускать каждый второй тик обработки энергосетей");

			readStringSet(cfg, "tradeOMatBlackList", "blacklists", "Чёрный список предметов для Обменного аппарата", tradeOMatBlackList);
			readStringSet(cfg, "scannerBlackList", "blacklists", "Чёрный список предметов для Сканера", scannerBlackList);

			cfg.save();
		}
		catch (Throwable throwable)
		{
			System.err.println("Failed load config. Use default values.");
			throwable.printStackTrace();
		}
	}

	public static final boolean inBlackList(Set<String> blackList, Item item, int meta)
	{
		if (item instanceof ItemBlock)
			if (inBlackList(blackList, ((ItemBlock) item).field_150939_a, meta))
				return true;
		return inBlackList(blackList, GameRegistry.findUniqueIdentifierFor(item), meta);
	}

	public static final boolean inBlackList(Set<String> blackList, Block block, int meta)
	{
		return inBlackList(blackList, GameRegistry.findUniqueIdentifierFor(block), meta);
	}

	private static final boolean inBlackList(Set<String> blackList, UniqueIdentifier id, int meta)
	{
		if (id != null)
		{
			String name = id.modId + ":" + id.name;
			if (blackList.contains(name) || blackList.contains(name + ":" + meta))
				return true;
		}

		return false;
	}

	private static final void readStringSet(Configuration cfg, String name, String category, String comment, Set<String> def)
	{
		Set<String> temp = getStringSet(cfg, name, category, comment, def);
		def.clear();
		def.addAll(temp);
	}

	private static final Set<String> getStringSet(Configuration cfg, String name, String category, String comment, Set<String> def)
	{
		return getStringSet(cfg, name, category, comment, def.toArray(new String[def.size()]));
	}

	private static final Set<String> getStringSet(Configuration cfg, String name, String category, String comment, String... def)
	{
		return Sets.newHashSet(cfg.getStringList(name, category, def, comment));
	}
}