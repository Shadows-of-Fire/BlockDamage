package shadows.damage.net;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import shadows.damage.BlockDamage;

@EventBusSubscriber(modid = BlockDamage.MODID, value = Side.CLIENT)
public class ClientTickHandler {

	static long x = 0;

	@SubscribeEvent
	public static void clientTick(ClientTickEvent e) {
		if (Minecraft.getMinecraft().isIntegratedServerRunning()) return;
		if (e.phase == Phase.START) {
			x++;
			if (x % 10 == 0) {
				RayTraceResult r = Minecraft.getMinecraft().objectMouseOver;
				if (r != null && r.typeOfHit == Type.BLOCK) BlockDamage.NETWORK.sendToServer(new MessageRequestInstance(r.getBlockPos().toLong()));
			}
		}
	}

}
