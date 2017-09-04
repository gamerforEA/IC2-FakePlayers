package com.gamerforea.ic2;

import com.gamerforea.eventhelper.util.FastUtils;
import com.google.common.collect.Sets;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.common.config.Configuration;

import java.util.Set;

import static net.minecraftforge.common.config.Configuration.CATEGORY_GENERAL;

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
	public static boolean explosionRegionOnly = false;
	public static boolean laserscatterEnabled = true;
	public static boolean boozeCombineEffectEnabled = false;
	public static boolean scrapboxDropEnabled = true;

	public static String safeAccessPermission = "ic2.accesssafe";
	public static boolean skipTicks = false;
	public static int skipTicksAmount = 1;
	public static boolean tradeOMatOnePlayer = true;

	public static int laserMining = 1250;
	public static int laserLowFocus = 100;
	public static int laserLongRange = 5000;
	public static int laserHorizontal = 3000;
	public static int laserSuperHeat = 2500;
	public static int laserScatter = 10000;
	public static int laserExplosive = 5000;
	public static boolean laserBreakBlock = true;

	public static float minerEnergyMultiplier = 1;
	public static float advMinerEnergyMultiplier = 1;

	public static int windRotorDamage = 1;
	public static int additionalWindRotorRadius = 0;

	static
	{
		init();
	}

	public static void init()
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
			explosionRegionOnly = cfg.getBoolean("explosionRegionOnly", "other", explosionRegionOnly, "Взрывы только в приватах");
			laserscatterEnabled = cfg.getBoolean("laserscatterEnabled", "other", laserscatterEnabled, "Шахтёрский лазер (режим \"Разброс\")");
			boozeCombineEffectEnabled = cfg.getBoolean("boozeCombineEffectEnabled", "other", boozeCombineEffectEnabled, "Комбинирование эффектов от выпивки");
			scrapboxDropEnabled = cfg.getBoolean("scrapbosDropEnabled", "other", scrapboxDropEnabled, "Дроп предметов из Утильсырья");

			safeAccessPermission = cfg.getString("safeAccessPermission", "other", safeAccessPermission, "Permission для доступа к персональным блокам (сейфам, торговым аппаратам и пр.)");
			skipTicks = cfg.getBoolean("skipTicks", "other", skipTicks, "Пропускать тики при обработки энергосетей");
			skipTicksAmount = cfg.getInt("skipTicksAmount", "other", skipTicksAmount, 1, Integer.MAX_VALUE, "Количество пропускаемых тиков");
			tradeOMatOnePlayer = cfg.getBoolean("tradeOMatOnePlayer", "other", tradeOMatOnePlayer, "Возможность пользоваться Торговым аппаратом одновременно лишь одним человеком");

			laserMining = cfg.getInt("laserMining", "laser", laserMining, 1, Integer.MAX_VALUE, "Энергия для режима Короткого фокуса");
			laserLowFocus = cfg.getInt("laserLowFocus", "laser", laserLowFocus, 1, Integer.MAX_VALUE, "Энергия для режима Короткого фокуса");
			laserLongRange = cfg.getInt("laserLongRange", "laser", laserLongRange, 1, Integer.MAX_VALUE, "Энергия для режима Дальнего действия");
			laserHorizontal = cfg.getInt("laserHorizontal", "laser", laserHorizontal, 1, Integer.MAX_VALUE, "Энергия для режима Горизонтальный");
			laserSuperHeat = cfg.getInt("laserSuperHeat", "laser", laserSuperHeat, 1, Integer.MAX_VALUE, "Энергия для режима Перегревающий");
			laserScatter = cfg.getInt("laserScatter", "laser", laserScatter, 1, Integer.MAX_VALUE, "Энергия для режима Разброс");
			laserExplosive = cfg.getInt("laserExplosive", "laser", laserExplosive, 1, Integer.MAX_VALUE, "Энергия для режима Взрывоопасный");
			laserBreakBlock = cfg.getBoolean("laserBreakBlock", "laser", laserBreakBlock, "Разрушение блоков Шахтёрским лазером");

			minerEnergyMultiplier = cfg.getFloat("minerEnergyMultiplier", "miner", minerEnergyMultiplier, 0, Float.MAX_VALUE, "Множитель энергии Буровой установки");
			advMinerEnergyMultiplier = cfg.getFloat("advMinerEnergyMultiplier", "miner", advMinerEnergyMultiplier, 0, Float.MAX_VALUE, "Множитель энергии Продвинутой буровой установки");

			windRotorDamage = cfg.getInt("windRotorDamage", "windRotors", windRotorDamage, 0, Integer.MAX_VALUE, "Урон ветровым роторам в тик");
			additionalWindRotorRadius = cfg.getInt("additionalWindRotorRadius", "windRotors", additionalWindRotorRadius, 0, Integer.MAX_VALUE, "Минимальное расстояние в блоках между лопастями роторов");

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

	public static final boolean inList(Set<String> list, Item item, int meta)
	{
		if (item instanceof ItemBlock)
			return inList(list, ((ItemBlock) item).field_150939_a, meta);

		return inList(list, getId(item), meta);
	}

	public static final boolean inList(Set<String> list, Block block, int meta)
	{
		return inList(list, getId(block), meta);
	}

	private static final boolean inList(Set<String> list, String id, int meta)
	{
		return id != null && (list.contains(id) || list.contains(id + ':' + meta));
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

	private static final String getId(Item item)
	{
		return GameData.getItemRegistry().getNameForObject(item);
	}

	private static final String getId(Block block)
	{
		return GameData.getBlockRegistry().getNameForObject(block);
	}
}