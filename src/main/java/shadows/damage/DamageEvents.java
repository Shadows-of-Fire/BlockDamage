package shadows.damage;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;

public class DamageEvents {

	@SubscribeEvent
	public void handleExplosion(ExplosionEvent.Detonate e) {
		World world = e.getWorld();
		e.getExplosion().damagesTerrain = false;
		if (world.isRemote | e.getExplosion().exploder == null) return;
		for (BlockPos p : e.getAffectedBlocks()) {
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
				ResourceLocation ent = BlockDamage.findName(e.getExplosion());
				int dmg = getOrLoadDmg(ent);
				inst.damage(p, dmg);
				if (inst.shouldBeDestroyed()) {
					world.destroyBlock(p, true);
					SaveHandler.DMG_MAP.remove(pos);
					SaveHandler.PLAYER_PLACED_BLOCKS.remove(pos);
				}
				BlockDamage.saveData.markDirty();
			}
		}
		e.getExplosion().clearAffectedBlockPositions();
	}

	@SubscribeEvent
	public void handlePlace(BlockEvent.PlaceEvent e) {
		if (e.getWorld().isRemote) return;
		SaveHandler.PLAYER_PLACED_BLOCKS.add(e.getPos().toLong());
		BlockDamage.saveData.markDirty();
	}

	@SubscribeEvent
	public void handleBreak(BlockEvent.BreakEvent e) {
		long pos = e.getPos().toLong();
		SaveHandler.PLAYER_PLACED_BLOCKS.remove(pos);
		SaveHandler.DMG_MAP.remove(pos);
		if (!e.getWorld().isRemote) BlockDamage.saveData.markDirty();
	}

	@SubscribeEvent(receiveCanceled = true, priority = EventPriority.LOWEST)
	public void handleClick(PlayerInteractEvent.LeftClickBlock e) {
		if (e.isCanceled()) {
			ItemStack s = e.getEntityPlayer().getHeldItemMainhand();
			for (WeaponInstance i : BlockDamage.WEAPONS) {
				if (i.matches(s)) {
					BlockPos p = e.getPos();
					long pos = p.toLong();
					IBlockState state = e.getWorld().getBlockState(p);
					int maxHp = BlockDamage.MAX_HP.getInt(state);
					if (maxHp == 0) maxHp = BlockDamage.WILD_MAX_HP.getInt(state.getBlock());
					if (maxHp > 0 && BlockDamage.canDamage(p)) {
						DamageInstance inst = SaveHandler.DMG_MAP.get(pos);
						if (inst == null) {
							inst = new DamageInstance(state, 0, maxHp, BlockDamage.WILD_MAX_HP.containsKey(state.getBlock()));
							SaveHandler.DMG_MAP.put(pos, inst);
						}
						if ((inst.wildcard && inst.getState().getBlock() != state.getBlock()) || (!inst.wildcard && inst.getState() != state)) {
							inst = new DamageInstance(state, 0, maxHp, BlockDamage.WILD_MAX_HP.containsKey(state.getBlock()));
							SaveHandler.DMG_MAP.put(pos, inst);
						}
						if (inst.isDestroyed()) return;
						inst.damage(p, i.getDmg());
						if (inst.shouldBeDestroyed()) {
							DELETION.add(Pair.of(p, inst));
							inst.markDestroyed();
						}
						BlockDamage.saveData.markDirty();
						s.damageItem(i.getDura(), e.getEntityPlayer());
						return;
					}
				}
			}
		}

	}

	private static final List<Pair<BlockPos, DamageInstance>> DELETION = new ArrayList<>();

	@SubscribeEvent
	public void serverTick(ServerTickEvent e) {
		if (e.phase == Phase.START) {
			for (int i = 0; i < DELETION.size(); i++) {
				Pair<BlockPos, DamageInstance> p = DELETION.get(i);
				BlockPos pos = p.getLeft();
				FMLCommonHandler.instance().getMinecraftServerInstance().worlds[0].destroyBlock(pos, true);
				SaveHandler.DMG_MAP.remove(pos.toLong());
				SaveHandler.PLAYER_PLACED_BLOCKS.remove(pos.toLong());
				DELETION.remove(i--);
			}
		}
	}

	static int getOrLoadDmg(ResourceLocation ent) {
		int dmg = BlockDamage.EXPLOSION_DMG.getOrDefault(ent, -1);
		if (dmg != -1) return dmg;
		dmg = BlockDamage.explosionConfig.getInt(ent.toString(), "Explosions", 5, 0, Integer.MAX_VALUE, "");
		if (BlockDamage.explosionConfig.hasChanged()) BlockDamage.explosionConfig.save();
		BlockDamage.EXPLOSION_DMG.put(ent, dmg);
		return dmg;
	}

	/*
	@SubscribeEvent
	public void exStart(ExplosionEvent.Start e) {
		Explosion ex = e.getExplosion();
		if (ex instanceof BlastRot || ex instanceof BlastMutation) e.setCanceled(true);
	}
	*/
}
