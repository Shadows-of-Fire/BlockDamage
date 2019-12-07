package shadows.damage;

import java.util.stream.Collectors;

import com.vicmatskiv.weaponlib.Explosion;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class VicHandler {

	public static void handle(Explosion ex) {
		World world = ex.getWorld();
		if (world.isRemote || ex.getExploder() == null) return;
		for (BlockPos p : ex.getAffectedBlockPositions().stream().map(p -> p.getBlockPos()).collect(Collectors.toList())) {
			long pos = p.toLong();
			IBlockState i = world.getBlockState(p);
			int maxHp = BlockDamage.MAX_HP.getInt(i);
			if (maxHp == 0) maxHp = BlockDamage.WILD_MAX_HP.getInt(i.getBlock());
			if (maxHp > 0 && BlockDamage.canDamage(p)) {
				DamageInstance inst = SaveHandler.DMG_MAP.get(pos);
				if (inst == null) {
					inst = new DamageInstance(i, 0, maxHp, BlockDamage.WILD_MAX_HP.containsKey(i.getBlock()));
					SaveHandler.DMG_MAP.put(pos, inst);
				}
				if ((inst.wildcard && inst.getState().getBlock() != i.getBlock()) || (!inst.wildcard && inst.getState() != i)) {
					inst = new DamageInstance(i, 0, maxHp, BlockDamage.WILD_MAX_HP.containsKey(i.getBlock()));
					SaveHandler.DMG_MAP.put(pos, inst);
				}
				ResourceLocation ent = EntityList.getKey(ex.getExploder());
				int dmg = DamageEvents.getOrLoadDmg(ent);
				inst.damage(p, dmg);
				if (inst.shouldBeDestroyed()) {
					world.destroyBlock(p, true);
					SaveHandler.DMG_MAP.remove(pos);
					SaveHandler.PLAYER_PLACED_BLOCKS.remove(pos);
				}
				BlockDamage.saveData.markDirty();
			}
		}
		ex.clearAffectedBlockPositions();
	}

}
