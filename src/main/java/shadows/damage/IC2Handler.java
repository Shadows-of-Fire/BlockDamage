package shadows.damage;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import ic2.core.ExplosionIC2;
import ic2.core.block.TileEntityBlock;
import ic2.core.item.tool.EntityMiningLaser;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class IC2Handler {

	public static boolean handle(Object block, Object world, Object pos, Object ex) {
		handle2((World) world, (BlockPos) pos, (Explosion) ex);
		return false;
	}

	private static void handle2(World world, BlockPos p, Explosion ex) {
		if (world.isRemote || ex.exploder == null) return;
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
			ResourceLocation ent = BlockDamage.findName(ex);
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

	static MethodHandle m;
	static {
		try {
			Method met = TileEntityBlock.class.getDeclaredMethod("getFaceShape", EnumFacing.class);
			met.setAccessible(true);
			m = MethodHandles.lookup().unreflect(met);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static BlockFaceShape getBlockFaceShape(Object w, Object s, Object p, Object f) throws Throwable {
		IBlockAccess world = (IBlockAccess) w;
		IBlockState state = (IBlockState) s;
		BlockPos pos = (BlockPos) p;
		EnumFacing face = (EnumFacing) f;
		if (world == null || state == null || pos == null || face == null) return BlockFaceShape.SOLID;
		TileEntity te = world.getTileEntity(pos);
		if (!(te instanceof TileEntityBlock)) return BlockFaceShape.SOLID;
		return (BlockFaceShape) m.invoke(te, face);
	}

	public static boolean doDamage(ExplosionIC2 ex, boolean was) {
		if (((Explosion) ex).exploder instanceof EntityMiningLaser) return false;
		return was;
	}

}
