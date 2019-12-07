package shadows.damage.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import shadows.damage.DamageInstance;
import shadows.damage.SaveHandler;

public class MessageRequestInstance implements IMessage {

	long pos;

	public MessageRequestInstance(BlockPos pos) {
		this.pos = pos.toLong();
	}

	public MessageRequestInstance(long pos) {
		this.pos = pos;
	}

	public MessageRequestInstance() {
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		pos = buf.readLong();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeLong(pos);
	}

	public static class Handler implements IMessageHandler<MessageRequestInstance, MessageSendInstance> {

		@Override
		public MessageSendInstance onMessage(MessageRequestInstance message, MessageContext ctx) {
			DamageInstance inst = SaveHandler.DMG_MAP.get(message.pos);
			return new MessageSendInstance(message.pos, inst);
		}

	}

}
