package fr.petroldagan;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.reaction.ReactionEmoji.Unicode;
import discord4j.core.object.util.Snowflake;
import fr.petroldagan.PlayersContainer.PlayerData;

public class BotInstance {

	public DiscordClient client;
	public PlayersContainer saver = new PlayersContainer(this);
	public CommandsManager commands = new CommandsManager(this);
	
	protected Snowflake selfId = Snowflake.of(608766391016751158L);
	protected Snowflake employéRoleId = Snowflake.of(595424617099821102L);
	protected Snowflake tournéeId = Snowflake.of(609137346381873153L);
	protected Snowflake salaireId = Snowflake.of(604077451596922912L);
	protected Snowflake logsId = Snowflake.of(604110367278235667L);

	public BotInstance() {
		this.client = new DiscordClientBuilder("NjA4NzY2MzkxMDE2NzUxMTU4.XUs89g.47TgHox0gu0Ucb8PN22e3lnqCJg").build();

		client.getEventDispatcher().on(ReadyEvent.class).subscribe(this::onReady);
		client.getEventDispatcher().on(MessageCreateEvent.class).filter(msg -> msg.getMember().isPresent()).subscribe(this::onMessage);
		client.getEventDispatcher().on(MessageUpdateEvent.class).subscribe(this::onMessageUpdate);

		client.login().block();
	}
	
	private void onReady(ReadyEvent ready) {
		System.out.println("Logged in as " + ready.getSelf().getUsername());
        client.updatePresence(Presence.online(Activity.watching("le pétrole couler"))).block();
	}
	
	private void onMessage(MessageCreateEvent eventMsg) {
		tournéeMsg(eventMsg.getMessage(), eventMsg.getMember().get());
	}
	
	private void onMessageUpdate(MessageUpdateEvent eventMsg) {
		eventMsg.getMessage()
			.filter(msg -> eventMsg.isEmbedsChanged())
			.filter(msg -> msg.getEmbeds().isEmpty())
			.filter(msg -> msg.getAuthor().get().getId().equals(selfId))
			.flatMap(Message::delete)
			.block();
		
		
	}
	
	//
	
	public void tournéeMsg(Message msg, Member author) {
		if(author.isBot()) return;
		
		if(msg.getChannelId().equals(tournéeId)) {
			System.out.println("Detected a msg in Tournée !");
			if(author.getRoleIds().contains(employéRoleId)) {
				if(msg.getEmbeds().size() != 0) return;
				if(msg.getAttachments().size() == 0) {
					toChannel(tournéeId, "Aucun screen dans ce message, merci de ne pas utiliser ce canal pour discuter.");
					return;
				} else if(msg.getAttachments().size() > 1) {
					toChannel(tournéeId, "Impossible d'ajouter plus d'un screen à la fois !");
					return;
				}
				
				PlayerData data = saver.createPlayer(author.getUsername(), author.getDisplayName(), author.getId().asLong());
				data.addRun(1);
				saver.save();
				System.out.println("Employé :");
				System.out.println("          " + data.displayName);
				System.out.println("          " + data.stillActive());
				System.out.println("          " + data.toPay() + "$");
				//toChannel(tournéeId, ":zap: **Capture d'écran prise en compte** :zap: *(nécessite la vérification d'un chef)*");
				msg.addReaction(Unicode.unicode("✅")).block();
				msg.addReaction(Unicode.unicode("❌")).block();
				data.postPay();
			} else {
				System.out.println("Not an employee");
				toChannel(tournéeId, "Vous n'êtes pas un employé...");
			}
		}else if(msg.getChannelId().equals(salaireId)) {
			commands.process(msg, author);
		}
	}
	
	public void toChannel(Snowflake flake, String text) {
		client.getChannelById(flake).cast(TextChannel.class).flatMap(channel -> channel.createMessage(text)).block();
	}

}
