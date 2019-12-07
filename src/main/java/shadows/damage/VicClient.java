package shadows.damage;

import com.vicmatskiv.weaponlib.KeyBindings;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class VicClient {

	public static void load() {
		KeyBindings.proningSwitchKey = new KeyBinding("eey", 0, "unused") {
			@Override
			public boolean isPressed() {
				return false;
			}
		};
		ClientRegistry.registerKeyBinding(KeyBindings.proningSwitchKey);
	}

}
