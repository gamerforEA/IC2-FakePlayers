package ic2.core.init;

import com.gamerforea.ic2.EventConfig;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent.MissingMapping;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry.Type;
import ic2.api.info.Info;
import ic2.core.IC2;
import ic2.core.IC2Potion;
import ic2.core.Ic2Fluid;
import ic2.core.Ic2Items;
import ic2.core.block.*;
import ic2.core.block.generator.block.BlockGenerator;
import ic2.core.block.heatgenerator.block.BlockHeatGenerator;
import ic2.core.block.kineticgenerator.block.BlockKineticGenerator;
import ic2.core.block.machine.*;
import ic2.core.block.personal.BlockPersonal;
import ic2.core.block.reactor.block.*;
import ic2.core.block.wiring.BlockCable;
import ic2.core.block.wiring.BlockChargepad;
import ic2.core.block.wiring.BlockElectric;
import ic2.core.block.wiring.BlockLuminator;
import ic2.core.crop.BlockCrop;
import ic2.core.item.*;
import ic2.core.item.armor.*;
import ic2.core.item.block.ItemBarrel;
import ic2.core.item.block.ItemCable;
import ic2.core.item.block.ItemDynamite;
import ic2.core.item.block.ItemIC2Door;
import ic2.core.item.reactor.*;
import ic2.core.item.resources.*;
import ic2.core.item.tfbp.*;
import ic2.core.item.tool.*;
import ic2.core.util.ConfigUtil;
import ic2.core.util.StackUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLiquid;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import java.util.*;

public class BlocksItems
{
	private static Map<InternalName, Fluid> fluids = new EnumMap(InternalName.class);
	private static Map<InternalName, Block> fluidBlocks = new EnumMap(InternalName.class);
	private static Map<String, InternalName> renames = new HashMap();
	private static Set<String> dropped = new HashSet();

	public static void init()
	{
		initPotions();
		initBlocks();
		initFluids();
		initItems();
		initMigration();
	}

	private static void initPotions()
	{
		Info.POTION_RADIATION = IC2Potion.radiation = IC2Potion.registerPotion(ConfigUtil.getInt(MainConfig.get(), "misc/radiationPotionID"), true, 5149489);
	}

	private static void initBlocks()
	{
		Ic2Items.copperOre = new ItemStack(new BlockMetaData(InternalName.blockOreCopper, Material.rock).setHardness(3.0F).setResistance(5.0F));
		Ic2Items.tinOre = new ItemStack(new BlockMetaData(InternalName.blockOreTin, Material.rock).setHardness(3.0F).setResistance(5.0F));
		Ic2Items.uraniumOre = new ItemStack(new BlockMetaData(InternalName.blockOreUran, Material.rock).setHardness(4.0F).setResistance(6.0F));
		Ic2Items.leadOre = new ItemStack(new BlockMetaData(InternalName.blockOreLead, Material.rock).setHardness(2.0F).setResistance(4.0F));
		new BlockRubWood(InternalName.blockRubWood);
		new BlockRubLeaves(InternalName.blockRubLeaves);
		new BlockRubSapling(InternalName.blockRubSapling);
		new BlockResin(InternalName.blockHarz);
		new BlockRubberSheet(InternalName.blockRubber);
		new BlockPoleFence(InternalName.blockFenceIron);
		Ic2Items.reinforcedStone = new ItemStack(new BlockMetaData(InternalName.blockAlloy, Material.iron).setHardness(80.0F).setResistance(180.0F).setStepSound(Block.soundTypeMetal));
		Ic2Items.basaltBlock = new ItemStack(new BlockMetaData(InternalName.blockBasalt, Material.rock).setHardness(20.0F).setResistance(45.0F).setStepSound(Block.soundTypeStone));
		Ic2Items.reinforcedGlass = new ItemStack(new BlockTexGlass(InternalName.blockAlloyGlass));
		Ic2Items.reinforcedDoorBlock = new ItemStack(new BlockIC2Door(InternalName.blockDoorAlloy));
		new BlockReinforcedFoam(InternalName.blockReinforcedFoam);
		new BlockFoam(InternalName.blockFoam);
		new BlockWall(InternalName.blockWall);
		new BlockScaffold(InternalName.blockScaffold);
		new BlockScaffold(InternalName.blockIronScaffold);
		new BlockMetal(InternalName.blockMetal);
		new BlockCable(InternalName.blockCable);
		new BlockKineticGenerator(InternalName.blockKineticGenerator);
		new BlockHeatGenerator(InternalName.blockHeatGenerator);
		new BlockGenerator(InternalName.blockGenerator);
		new BlockReactorChamber(InternalName.blockReactorChamber);
		new BlockReactorFluidPort(InternalName.blockReactorFluidPort);
		new BlockReactorAccessHatch(InternalName.blockReactorAccessHatch);
		new BlockReactorRedstonePort(InternalName.blockReactorRedstonePort);
		new BlockReactorVessel(InternalName.blockreactorvessel);
		new BlockElectric(InternalName.blockElectric);
		new BlockChargepad(InternalName.blockChargepad);
		new BlockMachine(InternalName.blockMachine);
		new BlockMachine2(InternalName.blockMachine2);
		new BlockMachine3(InternalName.blockMachine3);
		Ic2Items.luminator = new ItemStack(new BlockLuminator(InternalName.blockLuminatorDark));
		Ic2Items.activeLuminator = new ItemStack(new BlockLuminator(InternalName.blockLuminator));
		new BlockMiningPipe(InternalName.blockMiningPipe);
		new BlockMiningTip(InternalName.blockMiningTip);
		new BlockPersonal(InternalName.blockPersonal);
		Ic2Items.industrialTnt = new ItemStack(new BlockITNT(InternalName.blockITNT));
		Ic2Items.nuke = new ItemStack(new BlockITNT(InternalName.blockNuke));
		Ic2Items.dynamiteStick = new ItemStack(new BlockDynamite(InternalName.blockDynamite));
		Ic2Items.dynamiteStickWithRemote = new ItemStack(new BlockDynamite(InternalName.blockDynamiteRemote));
		new BlockCrop(InternalName.blockCrop);
		new BlockBarrel(InternalName.blockBarrel);
	}

	private static void initFluids()
	{
		Material steam = new MaterialLiquid(MapColor.silverColor);
		registerIC2fluid(InternalName.fluidUuMatter, Material.water, 3867955, 3000, 3000, 0, 300, false);
		registerIC2fluid(InternalName.fluidConstructionFoam, Material.water, 2105376, 10000, '썐', 0, 300, false);
		registerIC2fluid(InternalName.fluidCoolant, Material.water, 1333866, 1000, 3000, 0, 300, false);
		registerIC2fluid(InternalName.fluidHotCoolant, Material.lava, 11872308, 1000, 3000, 0, 1200, false);
		registerIC2fluid(InternalName.fluidPahoehoeLava, Material.lava, 8090732, '썐', 250000, 10, 1200, false);
		registerIC2fluid(InternalName.fluidBiomass, Material.water, 3632933, 1000, 3000, 0, 300, false);
		registerIC2fluid(InternalName.fluidBiogas, Material.water, 10983500, 1000, 3000, 0, 300, true);
		registerIC2fluid(InternalName.fluidDistilledWater, Material.water, 4413173, 1000, 1000, 0, 300, false);
		registerIC2fluid(InternalName.fluidSuperheatedSteam, steam, 13291985, -3000, 100, 0, 600, true);
		registerIC2fluid(InternalName.fluidSteam, steam, 12369084, -800, 300, 0, 420, true);
		registerIC2fluid(InternalName.fluidHotWater, Material.water, 4644607, 1000, 1000, 0, 350, false);
	}

	private static void initItems()
	{
		EnumHelper.addToolMaterial("IC2_BRONZE", 2, 350, 6.0F, 2.0F, 13);
		ArmorMaterial bronzeArmorMaterial = EnumHelper.addArmorMaterial("IC2_BRONZE", 15, new int[] { 2, 6, 5, 2 }, 9);
		ArmorMaterial alloyArmorMaterial = EnumHelper.addArmorMaterial("IC2_ALLOY", 50, new int[] { 4, 9, 7, 4 }, 12);
		Ic2Items.resin = new ItemStack(new ItemResin(InternalName.itemHarz));
		Ic2Items.rubber = new ItemStack(new ItemIC2(InternalName.itemRubber));
		Ic2Items.FluidCell = new ItemStack(new ItemFluidCell(InternalName.itemFluidCell));
		new ItemUpgradeKit(InternalName.itemupgradekit);
		new ItemRecipePart(InternalName.itemRecipePart);
		new ItemCasing(InternalName.itemCasing);
		new ItemCrushedOre(InternalName.itemCrushedOre);
		new ItemPurifiedCrushedOre(InternalName.itemPurifiedCrushedOre);
		new ItemPlate(InternalName.itemPlates);
		new ItemDensePlate(InternalName.itemDensePlates);
		Ic2Items.turningBlankIron = new ItemStack(new ItemLatheDefault(ItemLatheDefault.LatheMaterial.IRON));
		Ic2Items.turningBlankWood = new ItemStack(new ItemLatheDefault(ItemLatheDefault.LatheMaterial.WOOD));
		new ItemsmallDust(InternalName.itemDustSmall);
		new ItemDust(InternalName.itemDust);
		new ItemDust2(InternalName.itemDust2);
		new ItemIngot(InternalName.itemIngot);
		Ic2Items.reactorLithiumCell = new ItemStack(new ItemReactorLithiumCell(InternalName.reactorLithiumCell));
		Ic2Items.TritiumCell = new ItemStack(new ItemIC2(InternalName.itemTritiumCell));
		Ic2Items.UranFuel = new ItemStack(new ItemRadioactive(InternalName.itemUran, 60, 100));
		Ic2Items.MOXFuel = new ItemStack(new ItemRadioactive(InternalName.itemMOX, 300, 100));
		Ic2Items.Plutonium = new ItemStack(new ItemRadioactive(InternalName.itemPlutonium, 150, 100));
		Ic2Items.smallPlutonium = new ItemStack(new ItemRadioactive(InternalName.itemPlutoniumSmall, 150, 100));
		Ic2Items.Uran235 = new ItemStack(new ItemRadioactive(InternalName.itemUran235, 150, 100));
		Ic2Items.smallUran235 = new ItemStack(new ItemRadioactive(InternalName.itemUran235small, 150, 100));
		Ic2Items.Uran238 = new ItemStack(new ItemRadioactive(InternalName.itemUran238, 10, 90));
		Ic2Items.fuelRod = new ItemStack(new ItemIC2(InternalName.itemFuelRod));
		Ic2Items.RTGPellets = new ItemStack(new ItemRadioactive(InternalName.itemRTGPellet, 2, 90).setMaxStackSize(1));
		Ic2Items.electronicCircuit = new ItemStack(new ItemIC2(InternalName.itemPartCircuit));
		Ic2Items.advancedCircuit = new ItemStack(new ItemIC2(InternalName.itemPartCircuitAdv).setRarity(1).setUnlocalizedName("itemPartCircuitAdv").setCreativeTab(IC2.tabIC2));
		Ic2Items.advancedAlloy = new ItemStack(new ItemIC2(InternalName.itemPartAlloy));
		Ic2Items.carbonFiber = new ItemStack(new ItemIC2(InternalName.itemPartCarbonFibre));
		Ic2Items.carbonMesh = new ItemStack(new ItemIC2(InternalName.itemPartCarbonMesh));
		Ic2Items.carbonPlate = new ItemStack(new ItemIC2(InternalName.itemPartCarbonPlate));
		Ic2Items.iridiumOre = new ItemStack(new ItemIC2(InternalName.itemOreIridium).setRarity(2).setUnlocalizedName("itemOreIridium").setCreativeTab(IC2.tabIC2));
		Ic2Items.iridiumPlate = new ItemStack(new ItemIC2(InternalName.itemPartIridium).setRarity(2).setUnlocalizedName("itemPartIridium").setCreativeTab(IC2.tabIC2));
		Ic2Items.iridiumShard = new ItemStack(new ItemIC2(InternalName.itemShardIridium).setRarity(2).setUnlocalizedName("itemShardIridium").setCreativeTab(IC2.tabIC2));
		Ic2Items.treetap = new ItemStack(new ItemTreetap(InternalName.itemTreetap));
		Ic2Items.bronzePickaxe = new ItemStack(new ItemIC2Pickaxe(InternalName.itemToolBronzePickaxe, ToolMaterial.IRON, 5.0F, "ingotBronze"));
		Ic2Items.bronzeAxe = new ItemStack(new ItemIC2Axe(InternalName.itemToolBronzeAxe, ToolMaterial.IRON, 5.0F, "ingotBronze"));
		Ic2Items.bronzeSword = new ItemStack(new ItemIC2Sword(InternalName.itemToolBronzeSword, ToolMaterial.IRON, 7, "ingotBronze"));
		Ic2Items.bronzeShovel = new ItemStack(new ItemIC2Spade(InternalName.itemToolBronzeSpade, ToolMaterial.IRON, 5.0F, "ingotBronze"));
		Ic2Items.bronzeHoe = new ItemStack(new ItemIC2Hoe(InternalName.itemToolBronzeHoe, ToolMaterial.IRON, "ingotBronze"));
		Ic2Items.wrench = new ItemStack(new ItemToolWrench(InternalName.itemToolWrench));
		Ic2Items.cutter = new ItemStack(new ItemToolCutter(InternalName.itemToolCutter));
		Ic2Items.constructionFoamSprayer = new ItemStack(new ItemSprayer(InternalName.itemFoamSprayer));
		Ic2Items.toolbox = new ItemStack(new ItemToolbox(InternalName.itemToolbox));
		Ic2Items.containmentbox = new ItemStack(new ItemContainmentbox(InternalName.itemContainmentbox));
		Ic2Items.ForgeHammer = new ItemStack(new ItemToolHammer(InternalName.itemToolForgeHammer));
		Ic2Items.LathingTool = new ItemStack(new ItemLathingTool(InternalName.itemLathingTool));
		Ic2Items.crystalmemory = new ItemStack(new ItemCrystalMemory(InternalName.itemcrystalmemory));
		new ItemDrillStandard(InternalName.itemToolDrill);
		new ItemDrillDiamond(InternalName.itemToolDDrill);
		new ItemDrillIridium(InternalName.itemToolIridiumDrill);
		Ic2Items.chainsaw = new ItemStack(new ItemElectricToolChainsaw(InternalName.itemToolChainsaw));
		Ic2Items.electricWrench = new ItemStack(new ItemToolWrenchElectric(InternalName.itemToolWrenchElectric));
		Ic2Items.electricTreetap = new ItemStack(new ItemTreetapElectric(InternalName.itemTreetapElectric));
		Ic2Items.miningLaser = new ItemStack(new ItemToolMiningLaser(InternalName.itemToolMiningLaser));
		Ic2Items.ecMeter = new ItemStack(new ItemToolMeter(InternalName.itemToolMEter));
		Ic2Items.odScanner = new ItemStack(new ItemScanner(InternalName.itemScanner));
		Ic2Items.ovScanner = new ItemStack(new ItemScannerAdv(InternalName.itemScannerAdv));
		Ic2Items.obscurator = new ItemStack(new ItemObscurator(InternalName.obscurator));
		Ic2Items.frequencyTransmitter = new ItemStack(new ItemFrequencyTransmitter(InternalName.itemFreq));
		Ic2Items.nanoSaber = new ItemStack(new ItemNanoSaber(InternalName.itemNanoSaber));
		Ic2Items.plasmaLauncher = new ItemStack(new PlasmaLauncher(InternalName.plasmaLauncher));
		Ic2Items.windmeter = new ItemStack(new ItemWindmeter(InternalName.windmeter));
		Ic2Items.hazmatHelmet = new ItemStack(new ItemArmorHazmat(InternalName.itemArmorHazmatHelmet, 0));
		Ic2Items.hazmatChestplate = new ItemStack(new ItemArmorHazmat(InternalName.itemArmorHazmatChestplate, 1));
		Ic2Items.hazmatLeggings = new ItemStack(new ItemArmorHazmat(InternalName.itemArmorHazmatLeggings, 2));
		Ic2Items.hazmatBoots = new ItemStack(new ItemArmorHazmat(InternalName.itemArmorRubBoots, 3));
		Ic2Items.bronzeHelmet = new ItemStack(new ItemArmorIC2(InternalName.itemArmorBronzeHelmet, bronzeArmorMaterial, InternalName.bronze, 0, "ingotBronze"));
		Ic2Items.bronzeChestplate = new ItemStack(new ItemArmorIC2(InternalName.itemArmorBronzeChestplate, bronzeArmorMaterial, InternalName.bronze, 1, "ingotBronze"));
		Ic2Items.bronzeLeggings = new ItemStack(new ItemArmorIC2(InternalName.itemArmorBronzeLegs, bronzeArmorMaterial, InternalName.bronze, 2, "ingotBronze"));
		Ic2Items.bronzeBoots = new ItemStack(new ItemArmorIC2(InternalName.itemArmorBronzeBoots, bronzeArmorMaterial, InternalName.bronze, 3, "ingotBronze"));
		Ic2Items.compositeArmor = new ItemStack(new ItemArmorIC2(InternalName.itemArmorAlloyChestplate, alloyArmorMaterial, InternalName.alloy, 1, Ic2Items.advancedAlloy));
		Ic2Items.nanoHelmet = new ItemStack(new ItemArmorNanoSuit(InternalName.itemArmorNanoHelmet, 0));
		Ic2Items.nanoBodyarmor = new ItemStack(new ItemArmorNanoSuit(InternalName.itemArmorNanoChestplate, 1));
		Ic2Items.nanoLeggings = new ItemStack(new ItemArmorNanoSuit(InternalName.itemArmorNanoLegs, 2));
		Ic2Items.nanoBoots = new ItemStack(new ItemArmorNanoSuit(InternalName.itemArmorNanoBoots, 3));
		Ic2Items.quantumHelmet = new ItemStack(new ItemArmorQuantumSuit(InternalName.itemArmorQuantumHelmet, 0));
		Ic2Items.quantumBodyarmor = new ItemStack(new ItemArmorQuantumSuit(InternalName.itemArmorQuantumChestplate, 1));
		Ic2Items.quantumLeggings = new ItemStack(new ItemArmorQuantumSuit(InternalName.itemArmorQuantumLegs, 2));
		Ic2Items.quantumBoots = new ItemStack(new ItemArmorQuantumSuit(InternalName.itemArmorQuantumBoots, 3));
		Ic2Items.jetpack = new ItemStack(new ItemArmorJetpack(InternalName.itemArmorJetpack));
		Ic2Items.electricJetpack = new ItemStack(new ItemArmorJetpackElectric(InternalName.itemArmorJetpackElectric));
		Ic2Items.batPack = new ItemStack(new ItemArmorBatpack(InternalName.itemArmorBatpack));
		Ic2Items.advbatPack = new ItemStack(new ItemArmorAdvBatpack(InternalName.itemArmorAdvBatpack));
		Ic2Items.energyPack = new ItemStack(new ItemArmorEnergypack(InternalName.itemArmorEnergypack));
		Ic2Items.lapPack = Ic2Items.energyPack;
		Ic2Items.cfPack = new ItemStack(new ItemArmorCFPack(InternalName.itemArmorCFPack));
		Ic2Items.solarHelmet = new ItemStack(new ItemArmorSolarHelmet(InternalName.itemSolarHelmet));
		Ic2Items.staticBoots = new ItemStack(new ItemArmorStaticBoots(InternalName.itemStaticBoots));
		Ic2Items.nightvisionGoggles = new ItemStack(new ItemArmorNightvisionGoggles(InternalName.itemNightvisionGoggles));
		Ic2Items.reBattery = new ItemStack(new ItemBatteryDischarged(InternalName.itemBatREDischarged, 10000, 100, 1));
		Ic2Items.chargedReBattery = new ItemStack(new ItemBattery(InternalName.itemBatRE, 10000.0D, 32.0D, 1));
		Ic2Items.advBattery = new ItemStack(new ItemBattery(InternalName.itemAdvBat, 100000.0D, 256.0D, 2));
		Ic2Items.energyCrystal = new ItemStack(new ItemBattery(InternalName.itemBatCrystal, 1000000.0D, 2048.0D, 3));
		Ic2Items.lapotronCrystal = new ItemStack(new ItemBattery(InternalName.itemBatLamaCrystal, 1.0E7D, 8092.0D, 4).setRarity(1));
		Ic2Items.suBattery = new ItemStack(new ItemBatterySU(InternalName.itemBatSU, 1200, 1));
		Ic2Items.chargingREBattery = new ItemStack(new ItemBatteryChargeHotbar(InternalName.itemBatChargeRE, 40000.0D, 128.0D, 1));
		Ic2Items.chargingAdvBattery = new ItemStack(new ItemBatteryChargeHotbar(InternalName.itemBatChargeAdv, 400000.0D, 1024.0D, 2));
		Ic2Items.chargingEnergyCrystal = new ItemStack(new ItemBatteryChargeHotbar(InternalName.itemBatChargeCrystal, 4000000.0D, 8192.0D, 3));
		Ic2Items.chargingLapotronCrystal = new ItemStack(new ItemBatteryChargeHotbar(InternalName.itemBatChargeLamaCrystal, 4.0E7D, 32768.0D, 4).setRarity(1));
		new ItemCable(InternalName.itemCable);
		Ic2Items.cell = new ItemStack(new ItemCell(InternalName.itemCellEmpty));
		Ic2Items.tinCan = new ItemStack(new ItemIC2(InternalName.itemTinCan));
		Ic2Items.filledTinCan = new ItemStack(new ItemTinCan(InternalName.itemTinCanFilled));
		Ic2Items.reactorMOXSimple = new ItemStack(new ItemReactorMOX(InternalName.reactorMOXSimple, 1));
		Ic2Items.reactorMOXDual = new ItemStack(new ItemReactorMOX(InternalName.reactorMOXDual, 2));
		Ic2Items.reactorMOXQuad = new ItemStack(new ItemReactorMOX(InternalName.reactorMOXQuad, 4));
		Ic2Items.reactorUraniumSimple = new ItemStack(new ItemReactorUranium(InternalName.reactorUraniumSimple, 1));
		Ic2Items.reactorUraniumDual = new ItemStack(new ItemReactorUranium(InternalName.reactorUraniumDual, 2));
		Ic2Items.reactorUraniumQuad = new ItemStack(new ItemReactorUranium(InternalName.reactorUraniumQuad, 4));
		Ic2Items.reactorDepletedMOXSimple = new ItemStack(new ItemRadioactive(InternalName.reactorMOXSimpledepleted, 10, 100));
		Ic2Items.reactorDepletedMOXDual = new ItemStack(new ItemRadioactive(InternalName.reactorMOXDualdepleted, 10, 100));
		Ic2Items.reactorDepletedMOXQuad = new ItemStack(new ItemRadioactive(InternalName.reactorMOXQuaddepleted, 10, 100));
		Ic2Items.reactorDepletedUraniumSimple = new ItemStack(new ItemRadioactive(InternalName.reactorUraniumSimpledepleted, 10, 100));
		Ic2Items.reactorDepletedUraniumDual = new ItemStack(new ItemRadioactive(InternalName.reactorUraniumDualdepleted, 10, 100));
		Ic2Items.reactorDepletedUraniumQuad = new ItemStack(new ItemRadioactive(InternalName.reactorUraniumQuaddepleted, 10, 100));
		Ic2Items.reactorCoolantSimple = new ItemStack(new ItemReactorHeatStorage(InternalName.reactorCoolantSimple, 10000));
		Ic2Items.reactorCoolantTriple = new ItemStack(new ItemReactorHeatStorage(InternalName.reactorCoolantTriple, 30000));
		Ic2Items.reactorCoolantSix = new ItemStack(new ItemReactorHeatStorage(InternalName.reactorCoolantSix, '\uea60'));
		Ic2Items.reactorPlating = new ItemStack(new ItemReactorPlating(InternalName.reactorPlating, 1000, 0.95F));
		Ic2Items.reactorPlatingHeat = new ItemStack(new ItemReactorPlating(InternalName.reactorPlatingHeat, 2000, 0.99F));
		Ic2Items.reactorPlatingExplosive = new ItemStack(new ItemReactorPlating(InternalName.reactorPlatingExplosive, 500, 0.9F));
		Ic2Items.reactorHeatSwitch = new ItemStack(new ItemReactorHeatSwitch(InternalName.reactorHeatSwitch, 2500, 12, 4));
		Ic2Items.reactorHeatSwitchCore = new ItemStack(new ItemReactorHeatSwitch(InternalName.reactorHeatSwitchCore, 5000, 0, 72));
		Ic2Items.reactorHeatSwitchSpread = new ItemStack(new ItemReactorHeatSwitch(InternalName.reactorHeatSwitchSpread, 5000, 36, 0));
		Ic2Items.reactorHeatSwitchDiamond = new ItemStack(new ItemReactorHeatSwitch(InternalName.reactorHeatSwitchDiamond, 10000, 24, 8));
		Ic2Items.reactorVent = new ItemStack(new ItemReactorVent(InternalName.reactorVent, 1000, 6, 0));
		Ic2Items.reactorVentCore = new ItemStack(new ItemReactorVent(InternalName.reactorVentCore, 1000, 5, 5));
		Ic2Items.reactorVentGold = new ItemStack(new ItemReactorVent(InternalName.reactorVentGold, 1000, 20, 36));
		Ic2Items.reactorVentSpread = new ItemStack(new ItemReactorVentSpread(InternalName.reactorVentSpread, 4));
		Ic2Items.reactorVentDiamond = new ItemStack(new ItemReactorVent(InternalName.reactorVentDiamond, 1000, 12, 0));
		Ic2Items.reactorReflector = new ItemStack(new ItemReactorReflector(InternalName.reactorReflector, 10000));
		Ic2Items.reactorReflectorThick = new ItemStack(new ItemReactorReflector(InternalName.reactorReflectorThick, '鱀'));
		Ic2Items.reactorCondensator = new ItemStack(new ItemReactorCondensator(InternalName.reactorCondensator, 20000));
		Ic2Items.reactorCondensatorLap = new ItemStack(new ItemReactorCondensator(InternalName.reactorCondensatorLap, 100000));
		Ic2Items.terraformerBlueprint = new ItemStack(new ItemIC2(InternalName.itemTFBP));
		Ic2Items.cultivationTerraformerBlueprint = new ItemStack(new ItemTFBPCultivation(InternalName.itemTFBPCultivation));
		Ic2Items.irrigationTerraformerBlueprint = new ItemStack(new ItemTFBPIrrigation(InternalName.itemTFBPIrrigation));
		Ic2Items.chillingTerraformerBlueprint = new ItemStack(new ItemTFBPChilling(InternalName.itemTFBPChilling));
		Ic2Items.desertificationTerraformerBlueprint = new ItemStack(new ItemTFBPDesertification(InternalName.itemTFBPDesertification));
		Ic2Items.flatificatorTerraformerBlueprint = new ItemStack(new ItemTFBPFlatification(InternalName.itemTFBPFlatification));
		Ic2Items.mushroomTerraformerBlueprint = new ItemStack(new ItemTFBPMushroom(InternalName.itemTFBPMushroom));
		Ic2Items.coalBall = new ItemStack(new ItemIC2(InternalName.itemPartCoalBall));
		Ic2Items.compressedCoalBall = new ItemStack(new ItemIC2(InternalName.itemPartCoalBlock));
		Ic2Items.coalChunk = new ItemStack(new ItemIC2(InternalName.itemPartCoalChunk));
		Ic2Items.industrialDiamond = new ItemStack(new ItemIC2(InternalName.itemPartIndustrialDiamond).setUnlocalizedName("itemPartIndustrialDiamond"));
		Ic2Items.slag = new ItemStack(new ItemIC2(InternalName.itemSlag));
		Ic2Items.scrap = new ItemStack(new ItemIC2(InternalName.itemScrap));
		Ic2Items.scrapBox = new ItemStack(new ItemScrapbox(InternalName.itemScrapbox));
		Ic2Items.plantBall = new ItemStack(new ItemIC2(InternalName.itemFuelPlantBall));
		Ic2Items.biochaff = new ItemStack(new ItemIC2(InternalName.itemBiochaff));
		Ic2Items.painter = new ItemStack(new ItemIC2(InternalName.itemToolPainter));
		Ic2Items.blackPainter = new ItemStack(new ItemToolPainter(InternalName.itemToolPainterBlack, 0));
		Ic2Items.redPainter = new ItemStack(new ItemToolPainter(InternalName.itemToolPainterRed, 1));
		Ic2Items.greenPainter = new ItemStack(new ItemToolPainter(InternalName.itemToolPainterGreen, 2));
		Ic2Items.brownPainter = new ItemStack(new ItemToolPainter(InternalName.itemToolPainterBrown, 3));
		Ic2Items.bluePainter = new ItemStack(new ItemToolPainter(InternalName.itemToolPainterBlue, 4));
		Ic2Items.purplePainter = new ItemStack(new ItemToolPainter(InternalName.itemToolPainterPurple, 5));
		Ic2Items.cyanPainter = new ItemStack(new ItemToolPainter(InternalName.itemToolPainterCyan, 6));
		Ic2Items.lightGreyPainter = new ItemStack(new ItemToolPainter(InternalName.itemToolPainterLightGrey, 7));
		Ic2Items.darkGreyPainter = new ItemStack(new ItemToolPainter(InternalName.itemToolPainterDarkGrey, 8));
		Ic2Items.pinkPainter = new ItemStack(new ItemToolPainter(InternalName.itemToolPainterPink, 9));
		Ic2Items.limePainter = new ItemStack(new ItemToolPainter(InternalName.itemToolPainterLime, 10));
		Ic2Items.yellowPainter = new ItemStack(new ItemToolPainter(InternalName.itemToolPainterYellow, 11));
		Ic2Items.cloudPainter = new ItemStack(new ItemToolPainter(InternalName.itemToolPainterCloud, 12));
		Ic2Items.magentaPainter = new ItemStack(new ItemToolPainter(InternalName.itemToolPainterMagenta, 13));
		Ic2Items.orangePainter = new ItemStack(new ItemToolPainter(InternalName.itemToolPainterOrange, 14));
		Ic2Items.whitePainter = new ItemStack(new ItemToolPainter(InternalName.itemToolPainterWhite, 15));
		Ic2Items.dynamite = new ItemStack(new ItemDynamite(InternalName.itemDynamite, false));
		Ic2Items.stickyDynamite = new ItemStack(new ItemDynamite(InternalName.itemDynamiteSticky, true));
		Ic2Items.remote = new ItemStack(new ItemRemote(InternalName.itemRemote));
		new ItemUpgradeModule(InternalName.upgradeModule);
		Ic2Items.coin = new ItemStack(new ItemIC2(InternalName.itemCoin));
		Ic2Items.reinforcedDoor = new ItemStack(new ItemIC2Door(InternalName.itemDoorAlloy, StackUtil.getBlock(Ic2Items.reinforcedDoorBlock)));
		Ic2Items.constructionFoamPowder = new ItemStack(new ItemFoamPowder(InternalName.itemPartCFPowder));
		Ic2Items.grinPowder = new ItemStack(new ItemIC2(InternalName.itemGrinPowder));
		Ic2Items.debug = new ItemStack(new ItemDebug(InternalName.itemDebug));
		new ItemIC2Boat(InternalName.itemBoat);
		Ic2Items.weedingTrowel = new ItemStack(new ItemWeedingTrowel(InternalName.itemWeedingTrowel));
		Ic2Items.weed = new ItemStack(new ItemIC2(InternalName.itemWeed));
		Ic2Items.cropSeed = new ItemStack(new ItemCropSeed(InternalName.itemCropSeed));
		Ic2Items.cropnalyzer = new ItemStack(new ItemCropnalyzer(InternalName.itemCropnalyzer));
		Ic2Items.fertilizer = new ItemStack(new ItemFertilizer(InternalName.itemFertilizer));
		Ic2Items.hydratingCell = new ItemStack(new ItemGradual(InternalName.itemCellHydrant));
		Ic2Items.electricHoe = new ItemStack(new ItemElectricToolHoe(InternalName.itemToolHoe));
		Ic2Items.terraWart = new ItemStack(new ItemTerraWart(InternalName.itemTerraWart));
		Ic2Items.weedEx = new ItemStack(new ItemIC2(InternalName.itemWeedEx).setMaxStackSize(1).setMaxDamage(64));
		Ic2Items.mugEmpty = new ItemStack(new ItemMug(InternalName.itemMugEmpty));
		Ic2Items.coffeeBeans = new ItemStack(new ItemIC2(InternalName.itemCofeeBeans));
		Ic2Items.coffeePowder = new ItemStack(new ItemIC2(InternalName.itemCofeePowder));
		Ic2Items.mugCoffee = new ItemStack(new ItemMugCoffee(InternalName.itemMugCoffee));
		Ic2Items.hops = new ItemStack(new ItemIC2(InternalName.itemHops));
		Ic2Items.barrel = new ItemStack(new ItemBarrel(InternalName.itemBarrel));
		Ic2Items.mugBooze = new ItemStack(new ItemBooze(InternalName.itemMugBooze));

		/* TODO gamerforEA code replace, old code
		Ic2Items.woodrotor = new ItemStack(new ItemWindRotor(InternalName.itemwoodrotor, 5, 10800, 0.25F, 10, 60, new ResourceLocation(IC2.textureDomain, "textures/items/rotors/rotorWoodmodel.png")));
		Ic2Items.ironrotor = new ItemStack(new ItemWindRotor(InternalName.itemironrotor, 7, 86400, 0.5F, 14, 75, new ResourceLocation(IC2.textureDomain, "textures/items/rotors/rotorIronmodel.png")));
		Ic2Items.steelrotor = new ItemStack(new ItemWindRotor(InternalName.itemsteelrotor, 9, 172800, 0.75F, 17, 90, new ResourceLocation(IC2.textureDomain, "textures/items/rotors/rotorSteelmodel.png")));
		Ic2Items.carbonrotor = new ItemStack(new ItemWindRotor(InternalName.itemwcarbonrotor, 11, 604800, 1.0F, 20, 110, new ResourceLocation(IC2.textureDomain, "textures/items/rotors/rotorCarbonmodel.png"))); */
		int add = EventConfig.additionalWindRotorRadius;
		Ic2Items.woodrotor = new ItemStack(new ItemWindRotor(InternalName.itemwoodrotor, 5 + add, 10800, 0.25F, 10, 60, new ResourceLocation(IC2.textureDomain, "textures/items/rotors/rotorWoodmodel.png")));
		Ic2Items.ironrotor = new ItemStack(new ItemWindRotor(InternalName.itemironrotor, 7 + add, 86400, 0.5F, 14, 75, new ResourceLocation(IC2.textureDomain, "textures/items/rotors/rotorIronmodel.png")));
		Ic2Items.steelrotor = new ItemStack(new ItemWindRotor(InternalName.itemsteelrotor, 9 + add, 172800, 0.75F, 17, 90, new ResourceLocation(IC2.textureDomain, "textures/items/rotors/rotorSteelmodel.png")));
		Ic2Items.carbonrotor = new ItemStack(new ItemWindRotor(InternalName.itemwcarbonrotor, 11 + add, 604800, 1.0F, 20, 110, new ResourceLocation(IC2.textureDomain, "textures/items/rotors/rotorCarbonmodel.png")));
		// TODO gamerforEA code end

		Ic2Items.steamturbine = new ItemStack(new ItemGradualInt(InternalName.itemSteamTurbine, ConfigUtil.getInt(MainConfig.get(), "balance/SteamKineticGenerator/rotorlivetime")));
		Ic2Items.steamturbineblade = new ItemStack(new ItemIC2(InternalName.itemSteamTurbineBlade));
		Ic2Items.ironblockcuttingblade = new ItemStack(new ItemBlockCuttingBlade(InternalName.itemIronBlockCuttingBlade, 3));
		Ic2Items.advironblockcuttingblade = new ItemStack(new ItemBlockCuttingBlade(InternalName.itemAdvIronBlockCuttingBlade, 6));
		Ic2Items.diamondblockcuttingblade = new ItemStack(new ItemBlockCuttingBlade(InternalName.itemDiamondBlockCuttingBlade, 9));
		((BlockIC2Door) StackUtil.getBlock(Ic2Items.reinforcedDoorBlock)).setItemDropped(Ic2Items.reinforcedDoor.getItem());
	}

	private static void initMigration()
	{
		renames.put("blockfluidUuMatter", InternalName.fluidUuMatter);
		renames.put("blockfluidCf", InternalName.fluidConstructionFoam);
		renames.put("blockFluidcoolant", InternalName.fluidCoolant);
		renames.put("blockFluidhotcoolant", InternalName.fluidHotCoolant);
		renames.put("blockFluidpahoehoelava", InternalName.fluidPahoehoeLava);
		renames.put("blockbiomass", InternalName.fluidBiomass);
		renames.put("blockbiogas", InternalName.fluidBiogas);
		renames.put("blockdistilledwater", InternalName.fluidDistilledWater);
		renames.put("blocksuperheatedsteam", InternalName.fluidSuperheatedSteam);
		renames.put("blocksteam", InternalName.fluidSteam);
		dropped.add("itemArmorLappack");
		dropped.add("itemLithium");
		dropped.add("itemNanoSaberOff");
		dropped.add("itemDustIronSmall");
		dropped.add("itemDustBronze");
		dropped.add("itemDustClay");
		dropped.add("itemDustCoal");
		dropped.add("itemDustCopper");
		dropped.add("itemDustGold");
		dropped.add("itemDustIron");
		dropped.add("itemDustSilver");
		dropped.add("itemDustTin");
		dropped.add("itemIngotAdvIron");
		dropped.add("itemIngotAlloy");
		dropped.add("itemIngotBronze");
		dropped.add("itemIngotCopper");
		dropped.add("itemIngotTin");
		dropped.add("itemCellLava");
		dropped.add("itemCellWater");
		dropped.add("itemCellAir");
		dropped.add("itemCellWaterElectro");
		dropped.add("itemDustIronSmall");
		dropped.add("itemDustBronze");
		dropped.add("itemDustClay");
		dropped.add("itemDustCoal");
		dropped.add("itemDustCopper");
		dropped.add("itemDustGold");
		dropped.add("itemDustIron");
		dropped.add("itemDustSilver");
		dropped.add("itemDustTin");
		dropped.add("itemIngotAdvIron");
		dropped.add("itemIngotAlloy");
		dropped.add("itemIngotBronze");
		dropped.add("itemIngotCopper");
		dropped.add("itemIngotTin");
		dropped.add("itemCellCoal");
		dropped.add("itemFuelCoalCmpr");
		dropped.add("itemFuelCan");
		dropped.add("itemMatter");
		dropped.add("itemFuelPlantCmpr");
		dropped.add("itemCellBioRef");
		dropped.add("itemFuelCanEmpty");
		dropped.add("itemCellCoalRef");
		dropped.add("itemCellBio");
	}

	private static void registerIC2fluid(InternalName internalName, Material material, int color, int density, int viscosity, int luminosity, int temperature, boolean isGaseous)
	{
		if (!internalName.name().startsWith("fluid"))
		{
			throw new IllegalArgumentException("Invalid fluid block name: " + internalName);
		}
		else
		{
			String fluidName = "ic2" + internalName.name().substring("fluid".length()).toLowerCase(Locale.ENGLISH);
			Fluid fluid = new Ic2Fluid(fluidName).setDensity(density).setViscosity(viscosity).setLuminosity(luminosity).setTemperature(temperature).setGaseous(isGaseous);
			if (!FluidRegistry.registerFluid(fluid))
			{
				fluid = FluidRegistry.getFluid(fluidName);
			}

			Block block;
			if (!fluid.canBePlacedInWorld())
			{
				block = new BlockIC2Fluid(internalName, fluid, material, color);
				fluid.setBlock(block);
				fluid.setUnlocalizedName(block.getUnlocalizedName());
			}
			else
			{
				block = fluid.getBlock();
			}

			fluids.put(internalName, fluid);
			fluidBlocks.put(internalName, block);
		}
	}

	public static void onMissingMappings(FMLMissingMappingsEvent event)
	{
		for (MissingMapping mapping : event.get())
		{
			if (mapping.name.startsWith("IC2:"))
			{
				String subName = mapping.name.substring("IC2".length() + 1);
				InternalName newName = renames.get(subName);
				if (newName != null)
				{
					String name = "IC2:" + newName.name();
					if (mapping.type == Type.BLOCK)
					{
						mapping.remap(GameData.getBlockRegistry().getRaw(name));
					}
					else
					{
						mapping.remap(GameData.getItemRegistry().getRaw(name));
					}
				}
				else if (dropped.contains(subName))
				{
					mapping.ignore();
				}
			}
		}

	}

	public static Fluid getFluid(InternalName name)
	{
		return fluids.get(name);
	}

	public static Block getFluidBlock(InternalName name)
	{
		return fluidBlocks.get(name);
	}

	public static Collection<InternalName> getIc2FluidNames()
	{
		return fluids.keySet();
	}
}
