package shadows.damage;

import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.Event;

public class BlockAttackEvent extends Event {

	protected final DamageInstance inst;
	protected final BlockPos pos;

	public BlockAttackEvent(DamageInstance inst, BlockPos pos) {
		this.inst = inst;
		this.pos = pos;
	}

	public DamageInstance getInstance() {
		return inst;
	}

	public BlockPos getPos() {
		return pos;
	}

}
