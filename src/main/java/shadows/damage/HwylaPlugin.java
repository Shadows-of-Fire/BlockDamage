package shadows.damage;

import java.util.List;

import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.IWailaRegistrar;
import mcp.mobius.waila.api.WailaPlugin;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

@WailaPlugin
public class HwylaPlugin implements IWailaPlugin, IWailaDataProvider {

	@Override
	public void register(IWailaRegistrar registrar) {
		registrar.registerBodyProvider(this, Block.class);
	}

	@Override
	public List<String> getWailaBody(ItemStack itemStack, List<String> tooltip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
		DamageInstance dmg = SaveHandler.DMG_MAP.get(accessor.getPosition().toLong());
		if (dmg != null) {
			int hp = dmg.getMaxHp();
			int hpLeft = hp - dmg.getDmgTaken();
			tooltip.add(String.format("HP: %s / %s", hpLeft, hp));
		}
		return tooltip;
	}

}
