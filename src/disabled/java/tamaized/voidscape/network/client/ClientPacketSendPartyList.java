package tamaized.voidscape.network.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import tamaized.voidscape.Voidscape;
import tamaized.voidscape.network.NetworkMessages;
import tamaized.voidscape.party.ClientPartyInfo;
import tamaized.voidscape.party.Party;
import tamaized.voidscape.party.PartyManager;
import tamaized.voidscape.turmoil.Duties;
import tamaized.voidscape.world.Instance;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ClientPacketSendPartyList implements NetworkMessages.IMessage<ClientPacketSendPartyList> {

	private final List<ClientPartyInfo.Party> data = new ArrayList<>();

	@Override
	public void handle(@Nullable Player player) {
		if (player == null || player.level == null || !player.level.isClientSide()) {
			Voidscape.LOGGER.fatal("Warning, client attempted to send malicious packet! ({})", player == null ? "NULL PLAYER" : player.getDisplayName());
			return;
		}
		ClientPartyInfo.PARTIES.clear();
		data.forEach(party -> {
			if (player instanceof LocalPlayer) {
				ClientPacketListener network = ((LocalPlayer) player).connection;
				PlayerInfo remote = network.getPlayerInfo(party.network_host);
				if (remote != null) {
					party.host = remote.getProfile();
					party.network_host = null;
					ClientPartyInfo.PARTIES.add(party);
				}
			}
		});
		data.clear();
	}

	@Override
	public void toBytes(FriendlyByteBuf packet) {
		List<Party> list = PartyManager.parties();
		packet.writeInt(list.size());
		list.forEach(party -> {
			packet.writeVarInt(Duties.getID(party.duty()));
			packet.writeBoolean(party.hasPassword());
			packet.writeVarInt(party.size());
			packet.writeUUID(party.host().getUUID());
		});
	}

	@Override
	public ClientPacketSendPartyList fromBytes(FriendlyByteBuf packet) {
		data.clear();
		int len = packet.readInt();
		for (int i = 0; i < len; i++) {
			ClientPartyInfo.Party party = new ClientPartyInfo.Party();
			party.duty = Duties.fromID(packet.readVarInt());
			party.password = packet.readBoolean();
			party.members = packet.readVarInt();
			party.network_host = packet.readUUID();
			data.add(party);
		}
		return this;
	}

}
