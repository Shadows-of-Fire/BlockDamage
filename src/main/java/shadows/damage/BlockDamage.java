package shadows.damage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.RecipeItemHelper;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.oredict.OreDictionary;
import shadows.claim.ClaimSystem;
import shadows.claim.WorldClaims;
import shadows.claim.data.ChunkPosSerializer;
import shadows.damage.net.MessageRequestInstance;
import shadows.damage.net.MessageSendInstance;

@Mod(modid = BlockDamage.MODID, name = BlockDamage.MODNAME, version = BlockDamage.VERSION, dependencies = "required:worldclaims;after:mw")
public class BlockDamage {

	public static final String MODID = "blockdamage";
	public static final String MODNAME = "Block Damage";
	public static final String VERSION = "1.2.0";

	public static final Logger LOG = LogManager.getLogger(MODID);

	public static final Object2IntMap<IBlockState> MAX_HP = new Object2IntOpenHashMap<>();
	public static final Object2IntMap<Block> WILD_MAX_HP = new Object2IntOpenHashMap<>();
	public static final Object2IntMap<ResourceLocation> EXPLOSION_DMG = new Object2IntOpenHashMap<>();
	public static final Int2IntMap STACK_HP = new Int2IntOpenHashMap();
	public static final List<WeaponInstance> WEAPONS = new ArrayList<>();
	public static final SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);

	static File cfgDir;
	public static Configuration explosionConfig;
	public static SaveHandler saveData;
	public static AxisAlignedBB excludedZone;

	@EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		cfgDir = e.getModConfigurationDirectory();
		MinecraftForge.EVENT_BUS.register(new DamageEvents());
		MinecraftForge.EVENT_BUS.register(this);
		NETWORK.registerMessage(MessageRequestInstance.Handler.class, MessageRequestInstance.class, 0, Side.SERVER);
		NETWORK.registerMessage(MessageSendInstance.Handler.class, MessageSendInstance.class, 1, Side.CLIENT);
		Configuration mainCfg = new Configuration(e.getSuggestedConfigurationFile());
		int[] vals = new int[6];
		String[] comments = { "x1", "y1", "z1", "x2", "y2", "z2" };
		for (int i = 0; i < 6; i++) {
			vals[i] = mainCfg.getInt("Exclusion Point " + i, "general", 0, -30000000, 30000000, "The " + comments[i] + " coordinate.");
		}
		excludedZone = new AxisAlignedBB(vals[0], vals[1], vals[2], vals[3], vals[4], vals[5]);
		if (mainCfg.hasChanged()) mainCfg.save();
	}

	@EventHandler
	public void init(FMLInitializationEvent e) throws IOException {
		readHPConfigs();
		readAttackConfigs();
		preprocessTooltips();
		readHPTooltipConfigs();
		explosionConfig = new Configuration(new File(cfgDir, "explosion_damage.cfg"));
		if (FMLCommonHandler.instance().getSide() == Side.CLIENT) VicClient.load();
	}

	@SuppressWarnings("deprecation")
	void readHPConfigs() throws IOException {
		File hpData = new File(cfgDir, "block_damage_hp.cfg");
		BufferedReader in = null;
		try {
			if (hpData.exists()) {
				in = new BufferedReader(new FileReader(hpData));
				String str = null;
				while ((str = in.readLine()) != null) {
					str = str.trim();
					String[] split = str.split("\\s+");
					String[] info = split[0].split(":");
					Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(info[0], info[1]));
					if (b == Blocks.AIR || b == null) {
						LOG.error(String.format("Failed to parse %s into a block!", info[0] + ":" + info[1]));
						continue;
					}
					try {
						int meta = Integer.parseInt(info[2]);
						if (meta != OreDictionary.WILDCARD_VALUE) {
							IBlockState bs = b.getStateFromMeta(meta);
							MAX_HP.put(bs, Integer.parseInt(split[1]));
						} else WILD_MAX_HP.put(b, Integer.parseInt(split[1]));
					} catch (ArrayIndexOutOfBoundsException | NumberFormatException ex) {
						LOG.error("Failed to read block HP value from the line {}", str);
						ex.printStackTrace();
					}
				}
			} else {
				hpData.createNewFile();
			}
		} catch (IOException ex) {
			LOG.error("Error when reading HP Data configuration");
			ex.printStackTrace();
		} finally {
			if (in != null) in.close();
		}
	}

	void readHPTooltipConfigs() throws IOException {
		File hpData = new File(cfgDir, "block_damage_tooltips.cfg");
		BufferedReader in = null;
		try {
			if (hpData.exists()) {
				in = new BufferedReader(new FileReader(hpData));
				String str = null;
				while ((str = in.readLine()) != null) {
					str = str.trim();
					String[] split = str.split("\\s+");
					String[] info = split[0].split(":");
					Item i = ForgeRegistries.ITEMS.getValue(new ResourceLocation(info[0], info[1]));
					if (i == Items.AIR || i == null) {
						LOG.error(String.format("Failed to parse %s into an item!", info[0] + ":" + info[1]));
						continue;
					}
					try {
						ItemStack stack = new ItemStack(i, 1, Integer.parseInt(info[2]));
						STACK_HP.put(RecipeItemHelper.pack(stack), Integer.parseInt(split[1]));
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			} else {
				hpData.createNewFile();
			}
		} catch (IOException ex) {
			LOG.error("Error when reading HP Tooltip configuration");
			ex.printStackTrace();
		} finally {
			if (in != null) in.close();
		}
	}

	void preprocessTooltips() {
		for (Map.Entry<IBlockState, Integer> e : MAX_HP.entrySet()) {
			Item i = Item.getItemFromBlock(e.getKey().getBlock());
			if (i == Items.AIR) i = ForgeRegistries.ITEMS.getValue(e.getKey().getBlock().getRegistryName());
			if (i == Items.AIR || i == null) {
				LOG.error(String.format("Failed to automatically parse %s into an item!", e.getKey().getBlock().getRegistryName()));
				continue;
			}
			try {
				ItemStack stack = new ItemStack(i, 1, e.getKey().getBlock().getMetaFromState(e.getKey()));
				STACK_HP.put(RecipeItemHelper.pack(stack), (int) e.getValue());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	void readAttackConfigs() throws IOException {
		File wepData = new File(cfgDir, "block_damage_weapons.cfg");
		BufferedReader in = null;
		try {
			if (wepData.exists()) {
				in = new BufferedReader(new FileReader(wepData));
				String str = null;
				while ((str = in.readLine()) != null) {
					str = str.trim();
					String[] split = str.split("\\s+");
					String[] info = split[0].split(":");
					Item i = ForgeRegistries.ITEMS.getValue(new ResourceLocation(info[0], info[1]));
					if (i == Items.AIR || i == null) {
						LOG.error(String.format("Failed to parse %s into an itemstack!", info[0] + ":" + info[1]));
						continue;
					} else if (!i.isDamageable()) {
						LOG.error(String.format("Weapons: The item %s cannot take damage, it will be ignored.", info[0] + ":" + info[1]));
						continue;
					}
					try {
						WeaponInstance inst = new WeaponInstance(i, Integer.valueOf(info[2]), Integer.valueOf(split[1]));
						WEAPONS.add(inst);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			} else {
				wepData.createNewFile();
			}
		} catch (IOException ex) {
			LOG.error("Error when reading Weapon Damage configuration");
			ex.printStackTrace();
		} finally {
			if (in != null) in.close();
		}
	}

	@EventHandler
	public void serverStart(FMLServerStartingEvent e) {
		saveData = SaveHandler.get(e.getServer().getWorld(0));
	}

	@EventHandler
	public void serverStop(FMLServerStoppedEvent e) {
		SaveHandler.DMG_MAP.clear();
		SaveHandler.PLAYER_PLACED_BLOCKS.clear();
	}

	@SubscribeEvent
	public void disconnect(ClientDisconnectionFromServerEvent e) {
		SaveHandler.DMG_MAP.clear();
		SaveHandler.PLAYER_PLACED_BLOCKS.clear();
	}

	public static boolean canDamage(BlockPos pos) {
		return !WorldClaims.isSafe(ChunkPosSerializer.toLong(new ChunkPos(pos))) && !excludedZone.contains(new Vec3d(pos)) && (SaveHandler.PLAYER_PLACED_BLOCKS.contains(pos.toLong()) || ClaimSystem.getOwningPlayer(new ChunkPos(pos)) != null);
	}

	public static Team getTeam(EntityPlayer player) {
		Scoreboard sb = player.getWorldScoreboard();
		if (sb == null || player.getName() == null) return null;
		return sb.getPlayersTeam(player.getName());
	}

	public static ResourceLocation findName(Explosion ex) {
		ResourceLocation ent = null;
		//if (ex instanceof Blast) ent = new ResourceLocation("icbm", ex.getClass().getSimpleName());
		//else if (ex.exploder instanceof EntityMissile) ent = new ResourceLocation("icbm", ((EntityMissile) ex.exploder).explosiveID.getName());
		//else if (ex.exploder instanceof EntityGrenade) ent = new ResourceLocation("icbm", ((EntityGrenade) ex.exploder).explosiveID.getName());
		return ent == null ? EntityList.getKey(ex.exploder) : ent;
	}

}
