package shadows.damage;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;

public class DamageInstance {

	public static final String STATE = "state";
	public static final String DMG = "dmg";
	public static final String HP = "hp";
	public static final String WILD = "wildcard";

	protected final IBlockState state;
	protected int dmgTaken;
	protected final int maxHp;
	protected boolean destroyed = false;
	protected final boolean wildcard;

	public DamageInstance(IBlockState state, int dmg, int hp, boolean wild) {
		this.state = state;
		dmgTaken = dmg;
		maxHp = hp;
		wildcard = wild;
	}

	public DamageInstance(NBTTagCompound tag) {
		state = Block.getStateById(tag.getInteger(STATE));
		dmgTaken = tag.getInteger(DMG);
		maxHp = tag.getInteger(HP);
		wildcard = tag.getBoolean(WILD);
	}

	public NBTTagCompound serialize() {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setInteger(STATE, Block.getStateId(state));
		tag.setInteger(DMG, dmgTaken);
		tag.setInteger(HP, maxHp);
		tag.setBoolean(WILD, wildcard);
		return tag;
	}

	boolean shouldBeDestroyed() {
		return dmgTaken >= maxHp;
	}

	void damage(BlockPos pos, int dmg) {
		dmgTaken += dmg;
		MinecraftForge.EVENT_BUS.post(new BlockAttackEvent(this, pos));
	}

	int getDmgTaken() {
		return dmgTaken;
	}

	int getMaxHp() {
		return maxHp;
	}

	IBlockState getState() {
		return state;
	}

	public void markDestroyed() {
		destroyed = true;
	}

	public boolean isDestroyed() {
		return destroyed;
	}

	public void write(ByteBuf buf) {
		buf.writeInt(dmgTaken);
		buf.writeInt(maxHp);
	}

	/**
	 * Client-only
	 * @param buf
	 * @return
	 */
	public static DamageInstance read(ByteBuf buf) {
		int dmgTaken = buf.readInt();
		int maxHp = buf.readInt();
		return new DamageInstance(null, dmgTaken, maxHp, false);
	}
}
