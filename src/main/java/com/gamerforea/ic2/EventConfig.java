package com.gamerforea.ic2;

import static net.minecraftforge.common.config.Configuration.CATEGORY_GENERAL;

import java.io.File;

import net.minecraftforge.common.config.Configuration;
import cpw.mods.fml.common.FMLCommonHandler;

public class EventConfig
{
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

	static
	{
		File mainDirectory = FMLCommonHandler.instance().getMinecraftServerInstance().getFile(".");
		Configuration config = new Configuration(new File(mainDirectory, "config/Events/IC2.cfg"));
		config.load();
		pumpEvent = config.getBoolean("pumpEvent", CATEGORY_GENERAL, pumpEvent, "Помпа (выкачивание жидкости)");
		minerEvent = config.getBoolean("minerEvent", CATEGORY_GENERAL, minerEvent, "Буровая установка (разрушение блоков)");
		advminerEvent = config.getBoolean("advMinerEvent", CATEGORY_GENERAL, advminerEvent, "Продвинутая буровая установка (разрушение блоков)");
		teslaEvent = config.getBoolean("teslaEvent", CATEGORY_GENERAL, teslaEvent, "Катушка Теслы (урон по мобам)");
		sprayerEvent = config.getBoolean("sprayerEvent", CATEGORY_GENERAL, sprayerEvent, "Пульверизатор (установка блоков)");
		laserEvent = config.getBoolean("laserEvent", CATEGORY_GENERAL, laserEvent, "Шахтёрский лазер (разрушение блоков и урон по мобам)");
		plasmaEvent = config.getBoolean("plasmaEvent", CATEGORY_GENERAL, plasmaEvent, "Плазменная пушка (взрыв)");
		explosionEvent = config.getBoolean("explosionEvent", CATEGORY_GENERAL, explosionEvent, "Взрывы (разрушение блоков)");

		plasmaEnabled = config.getBoolean("plasmaEnabled", "other", plasmaEnabled, "Плазменная пушка");
		radiationEnabled = config.getBoolean("radiationEnabled", "other", radiationEnabled, "Радиация");
		explosionEnabled = config.getBoolean("explosionEnabled", "other", explosionEnabled, "Взрывы");
		config.save();
	}
}