package com.gamerforea.ic2;

import com.gamerforea.eventhelper.config.*;
import net.minecraft.util.EnumChatFormatting;

import static net.minecraftforge.common.config.Configuration.CATEGORY_GENERAL;
import static net.minecraftforge.common.config.Configuration.CATEGORY_SPLITTER;

@Config(name = "IC2")
public final class EventConfig
{
	private static final String CATEGORY_EVENTS = "events";
	private static final String CATEGORY_OTHER = "other";
	private static final String CATEGORY_PERFORMANCE = "performance";
	private static final String CATEGORY_ENERGY = "energy";
	private static final String CATEGORY_LASER = "laser";
	private static final String CATEGORY_LASER_ENERGY = CATEGORY_LASER + CATEGORY_SPLITTER + CATEGORY_ENERGY;
	private static final String CATEGORY_WIND_ROTORS = "windRotors";
	private static final String CATEGORY_BLACKLISTS = "blacklists";

	@ConfigItemBlockList(name = "tradeOMat",
						 category = CATEGORY_BLACKLISTS,
						 comment = "Чёрный список предметов для Обменного аппарата",
						 oldName = "tradeOMatBlackList")
	public static final ItemBlockList tradeOMatBlackList = new ItemBlockList(true);

	@ConfigItemBlockList(name = "scanner",
						 category = CATEGORY_BLACKLISTS,
						 comment = "Чёрный список предметов для Сканера",
						 oldName = "scannerBlackList")
	public static final ItemBlockList scannerBlackList = new ItemBlockList(true);

	@ConfigItemBlockList(name = "minerPlace",
						 category = CATEGORY_BLACKLISTS,
						 comment = "Чёрный список устанавливаемых блоков для Буровой установки",
						 oldName = "minerBlackList")
	public static final ItemBlockList minerPlaceBlackList = new ItemBlockList(true);

	@ConfigItemBlockList(name = "minerBreak",
						 category = CATEGORY_BLACKLISTS,
						 comment = "Чёрный список разрушаемых блоков для Буровой установки",
						 oldName = "minerBreakBlackList")
	public static final ItemBlockList minerBreakBlackList = new ItemBlockList(true);

	@ConfigBoolean(category = CATEGORY_EVENTS,
				   comment = "Терраформер (работа только в привате)",
				   oldCategory = CATEGORY_GENERAL)
	public static boolean terraRegionOnly = true;

	@ConfigBoolean(category = CATEGORY_EVENTS,
				   comment = "Терраформер (замена/установка блоков)",
				   oldCategory = CATEGORY_GENERAL)
	public static boolean terraEvent = true;

	@ConfigBoolean(category = CATEGORY_EVENTS, comment = "Помпа (выкачивание жидкости)", oldCategory = CATEGORY_GENERAL)
	public static boolean pumpEvent = true;

	@ConfigBoolean(category = CATEGORY_EVENTS,
				   comment = "Буровая установка (разрушение блоков)",
				   oldCategory = CATEGORY_GENERAL)
	public static boolean minerEvent = true;

	@ConfigBoolean(category = CATEGORY_EVENTS,
				   comment = "Продвинутая буровая установка (разрушение блоков)",
				   oldCategory = CATEGORY_GENERAL)
	public static boolean advminerEvent = true;

	@ConfigBoolean(category = CATEGORY_EVENTS,
				   comment = "Катушка Теслы (урон по мобам)",
				   oldCategory = CATEGORY_GENERAL)
	public static boolean teslaEvent = true;

	@ConfigBoolean(category = CATEGORY_EVENTS,
				   comment = "Пульверизатор (установка блоков)",
				   oldCategory = CATEGORY_GENERAL)
	public static boolean sprayerEvent = true;

	@ConfigBoolean(category = CATEGORY_EVENTS,
				   comment = "Шахтёрский лазер (разрушение блоков и урон по мобам)",
				   oldCategory = CATEGORY_GENERAL)
	public static boolean laserEvent = true;

	@ConfigBoolean(category = CATEGORY_EVENTS, comment = "Плазменная пушка (взрыв)", oldCategory = CATEGORY_GENERAL)
	public static boolean plasmaEvent = true;

	@ConfigBoolean(category = CATEGORY_EVENTS,
				   comment = "Взрывы (разрушение блоков только в привате)",
				   oldCategory = CATEGORY_OTHER)
	public static boolean explosionRegionOnly = false;

	@ConfigBoolean(category = CATEGORY_EVENTS, comment = "Взрывы (разрушение блоков)", oldCategory = CATEGORY_GENERAL)
	public static boolean explosionEvent = true;

	@ConfigBoolean(category = CATEGORY_OTHER, comment = "Плазменная пушка")
	public static boolean plasmaEnabled = false;

	@ConfigBoolean(category = CATEGORY_OTHER, comment = "Радиация")
	public static boolean radiationEnabled = false;

	@ConfigBoolean(category = CATEGORY_OTHER, comment = "Взрывы")
	public static boolean explosionEnabled = false;

	@ConfigBoolean(category = CATEGORY_OTHER, comment = "Комбинирование эффектов от выпивки")
	public static boolean boozeCombineEffectEnabled = false;

	@ConfigBoolean(category = CATEGORY_OTHER, comment = "Дроп предметов из Утильсырья")
	public static boolean scrapboxDropEnabled = true;

	@ConfigBoolean(category = CATEGORY_OTHER, comment = "Установка блоков Буровой установкой")
	public static boolean minerPlacingEnabled = false;

	@ConfigBoolean(category = CATEGORY_OTHER, comment = "Автоматическая привязка Торгового аппарата к владельцу")
	public static boolean autoTradeOMatPrivateEnabled = true;

	@ConfigBoolean(category = CATEGORY_OTHER,
				   comment = "Возможность пользоваться Торговым аппаратом одновременно лишь одним человеком")
	public static boolean tradeOMatOnePlayer = true;

	@ConfigString(category = CATEGORY_OTHER,
				   comment = "Permission для доступа к персональным блокам (сейфам, торговым аппаратам и пр.)")
	public static String safeAccessPermission = "ic2.accesssafe";

	@ConfigBoolean(category = CATEGORY_OTHER,
				   comment = "Проверки корректности выполнения операций при работе с жидкостямии (может вызывать краши)")
	public static boolean liquidChecks = true;

	@ConfigInt(category = CATEGORY_OTHER,
			   comment = "Максимальное количество Ускорителей в механизме (0 - без ограничений)",
			   min = 0)
	public static int maxOverclockerCount = 0;

	@ConfigInt(category = CATEGORY_WIND_ROTORS, comment = "Урон ветровым роторам в тик", min = 0)
	public static int windRotorDamage = 1;

	@ConfigInt(category = CATEGORY_WIND_ROTORS,
			   comment = "Минимальное расстояние в блоках между лопастями роторов",
			   min = 0)
	public static int additionalWindRotorRadius = 0;

	@ConfigFloat(category = CATEGORY_ENERGY,
				 comment = "Множитель энергии Буровой установки",
				 min = 0,
				 oldCategory = "miner")
	public static float minerEnergyMultiplier = 1;

	@ConfigFloat(category = CATEGORY_ENERGY,
				 comment = "Множитель энергии Продвинутой буровой установки",
				 min = 0,
				 oldCategory = "miner")
	public static float advMinerEnergyMultiplier = 1;

	@ConfigFloat(category = CATEGORY_ENERGY, comment = "Множитель энергии Реактора", min = 0, oldCategory = "reactor")
	public static float reactorEnergyMultiplier = 1;

	@ConfigBoolean(category = CATEGORY_PERFORMANCE,
				   comment = "Пропускать тики при обработки энергосетей",
				   oldCategory = CATEGORY_OTHER)
	public static boolean skipTicks = false;

	@ConfigInt(category = CATEGORY_PERFORMANCE,
			   comment = "Количество пропускаемых тиков",
			   oldCategory = CATEGORY_OTHER,
			   min = 1)
	public static int skipTicksAmount = 1;

	@ConfigBoolean(category = CATEGORY_PERFORMANCE,
				   comment = "Оптимизация Ветрогенератора",
				   oldCategory = CATEGORY_OTHER)
	public static boolean optimizeWindGenerator = false;

	@ConfigInt(name = "mining",
			   category = CATEGORY_LASER_ENERGY,
			   comment = "Энергия для режима 'Добыча'",
			   min = 1,
			   oldName = "laserMining",
			   oldCategory = CATEGORY_LASER)
	public static int laserMining = 1250;

	@ConfigInt(name = "focus",
			   category = CATEGORY_LASER_ENERGY,
			   comment = "Энергия для режима 'Короткого фокуса'",
			   min = 1,
			   oldName = "laserLowFocus",
			   oldCategory = CATEGORY_LASER)
	public static int laserLowFocus = 100;

	@ConfigInt(name = "longRange",
			   category = CATEGORY_LASER_ENERGY,
			   comment = "Энергия для режима 'Дальнего действия'",
			   min = 1,
			   oldName = "laserLongRange",
			   oldCategory = CATEGORY_LASER)
	public static int laserLongRange = 5000;

	@ConfigInt(name = "horizontal",
			   category = CATEGORY_LASER_ENERGY,
			   comment = "Энергия для режима 'Горизонтальный'",
			   min = 1,
			   oldName = "laserHorizontal",
			   oldCategory = CATEGORY_LASER)
	public static int laserHorizontal = 3000;

	@ConfigInt(name = "superHeat",
			   category = CATEGORY_LASER_ENERGY,
			   comment = "Энергия для режима 'Перегревающий'",
			   min = 1,
			   oldName = "laserSuperHeat",
			   oldCategory = CATEGORY_LASER)
	public static int laserSuperHeat = 2500;

	@ConfigInt(name = "scatter",
			   category = CATEGORY_LASER_ENERGY,
			   comment = "Энергия для режима 'Разброс'",
			   min = 1,
			   oldName = "laserScatter",
			   oldCategory = CATEGORY_LASER)
	public static int laserScatter = 10000;

	@ConfigInt(name = "explosive",
			   category = CATEGORY_LASER_ENERGY,
			   comment = "Энергия для режима 'Взрывоопасный'",
			   min = 1,
			   oldName = "laserExplosive",
			   oldCategory = CATEGORY_LASER)
	public static int laserExplosive = 5000;

	@ConfigBoolean(name = "breakBlock", category = CATEGORY_LASER, comment = "Разрушение блоков Шахтёрским лазером", oldName = "laserBreakBlock")
	public static boolean laserBreakBlock = true;

	@ConfigInt(name = "maxBreakY",
			   category = CATEGORY_LASER,
			   comment = "Максимальная высота, на которой может работать Шахтёрский лазер",
			   min = 1, oldName = "laserMaxBreakY")
	public static int laserMaxBreakY = 255;

	@ConfigBoolean(name = "scatterEnabled",
				   category = CATEGORY_LASER,
				   comment = "Шахтёрский лазер (режим 'Разброс')",
				   oldName = "laserscatterEnabled")
	public static boolean laserScatterEnabled = true;

	@ConfigString(name = "maxBreakYWarnMsg",
				  category = CATEGORY_LASER,
				  comment = "Предупреждение, отправляемое игроку при попытке использовать Шахтёрский лазер выше максимальной высоты",
				  oldName = "laserMaxBreakYWarnMsg")
	public static String laserMaxBreakYWarnMsg = EnumChatFormatting.RED + "Шахтёрский лазер нельзя использовать на высоте выше %d";

	static
	{
		ConfigUtils.readConfig(EventConfig.class);
	}
}