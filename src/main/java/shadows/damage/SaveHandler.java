package shadows.damage;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

public class SaveHandler extends WorldSavedData {

	public static final String KEY = "bd_data";
	public static final String PLAYER_KEY = "bd_ppb";
	public static final String DMG_KEY = "bd_dmg";
	public static final LongSet PLAYER_PLACED_BLOCKS = new LongOpenHashSet();
	public static final Long2ObjectMap<DamageInstance> DMG_MAP = new Long2ObjectOpenHashMap<>();

	public SaveHandler() {
		super(KEY);
	}

	public SaveHandler(String s) {
		super(s);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		DMG_MAP.clear();
		PLAYER_PLACED_BLOCKS.clear();
		NBTTagCompound tag = compound.getCompoundTag(DMG_KEY);
		for (String s : tag.getKeySet()) {
			long key = Long.parseLong(s, 10);
			DamageInstance inst = new DamageInstance(tag.getCompoundTag(s));
			DMG_MAP.put(key, inst);
		}
		NBTBase nbt = compound.getTag(PLAYER_KEY);
		if (nbt instanceof NBTTagLongArray) {
			for (long l : ((NBTTagLongArray) nbt).data)
				PLAYER_PLACED_BLOCKS.add(l);
		}

	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		NBTTagCompound tag = new NBTTagCompound();
		for (Entry<DamageInstance> e : DMG_MAP.long2ObjectEntrySet()) {
			tag.setTag(Long.toString(e.getLongKey()), e.getValue().serialize());
		}
		compound.setTag(DMG_KEY, tag);
		compound.setTag(PLAYER_KEY, new NBTTagLongArray(PLAYER_PLACED_BLOCKS.toLongArray()));
		return compound;
	}

	public static SaveHandler get(World world) {
		MapStorage storage = world.getMapStorage();
		SaveHandler instance = (SaveHandler) storage.getOrLoadData(SaveHandler.class, KEY);

		if (instance == null) {
			instance = new SaveHandler();
			storage.setData(KEY, instance);
		}
		return instance;
	}

}
