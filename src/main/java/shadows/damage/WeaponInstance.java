package shadows.damage;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class WeaponInstance {

	protected Item weapon;
	protected int duraPerHit;
	protected int dmgPerHit;

	public WeaponInstance(Item weapon, int dura, int dmg) {
		duraPerHit = dura;
		dmgPerHit = dmg;
		this.weapon = weapon;
	}

	public boolean matches(ItemStack stack) {
		return stack.getItem() == weapon;
	}

	public int getDura() {
		return duraPerHit;
	}

	public int getDmg() {
		return dmgPerHit;
	}

}
