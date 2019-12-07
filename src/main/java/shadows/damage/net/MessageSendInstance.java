package shadows.damage.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import shadows.damage.DamageInstance;
import shadows.damage.SaveHandler;

public class MessageSendInstance implements IMessage {

	boolean isNull;
	long pos;
	DamageInstance inst;

	public MessageSendInstance(long pos, DamageInstance inst) {
		this.inst = inst;
		this.pos = pos;
		isNull = inst == null;
	}

	public MessageSendInstance() {
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		isNull = buf.readBoolean();
		if (!isNull) inst = DamageInstance.read(buf);
		pos = buf.readLong();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(isNull);
		if (!isNull) inst.write(buf);
		buf.writeLong(pos);
	}

	public static class Handler implements IMessageHandler<MessageSendInstance, IMessage> {

		@Override
		public IMessage onMessage(MessageSendInstance message, MessageContext ctx) {
			Minecraft.getMinecraft().addScheduledTask(() -> message.isNull ? SaveHandler.DMG_MAP.remove(message.pos) : SaveHandler.DMG_MAP.put(message.pos, message.inst));
			return null;
		}

	}

}
