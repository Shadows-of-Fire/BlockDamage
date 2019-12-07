package shadows.damage;

import net.minecraft.client.util.RecipeItemHelper;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@EventBusSubscriber(value = Side.CLIENT, modid = BlockDamage.MODID)
public class ClientHandler {

	@SubscribeEvent
	public static void tooltips(ItemTooltipEvent e) {
		int hp = BlockDamage.STACK_HP.get(RecipeItemHelper.pack(e.getItemStack()));
		if (hp > 0) {
			e.getToolTip().add(String.format("Max HP: %s", hp));
		}
	}

}
