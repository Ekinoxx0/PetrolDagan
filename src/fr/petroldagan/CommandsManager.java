package fr.petroldagan;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.reaction.ReactionEmoji.Unicode;
import fr.petroldagan.PlayersContainer.PlayerData;

public class CommandsManager {
	
	private BotInstance inst;
	private int verificationCount = 0;
	
	public CommandsManager(BotInstance inst) {
		this.inst = inst;
	}

	public void msg(String text) {
		inst.client.getChannelById(inst.salaireId)
			.cast(TextChannel.class)
			.flatMap(channel -> channel.createMessage(text))
			.block();
	}
	
	public void msg(String title, String text) {
		inst.client.getChannelById(inst.salaireId)
			.cast(TextChannel.class)
			.flatMap(channel -> channel.createMessage(mcs -> mcs.setEmbed(ecs -> {
				ecs.setColor(Color.GRAY);
				ecs.setTitle(title);
				ecs.setDescription(text);
			})))
			.block();
	}
	
	public void process(Message msg, Member author) {
		if(!msg.getContent().isPresent()) return;
		
		String rawText = msg.getContent().get();
		
		if(rawText.startsWith("!") 
				|| rawText.startsWith("*") 
				|| rawText.startsWith("\\") 
				|| rawText.startsWith(".") 
				|| rawText.startsWith("/") 
				|| rawText.startsWith("_") 
				|| rawText.startsWith("#")) {
			rawText = rawText.substring(1);
		} else {
			return;
		}
		System.out.println(author.getUsername() + ": " + rawText);
		String[] splitted = rawText.split(" ");
		String cmd = splitted[0].toLowerCase();
		String[] args = Arrays.copyOfRange(splitted, 1, splitted.length);

		String name = "";
		int amount = 0;
		PlayerData data = null;
		switch(cmd) {

		case "aide":
		case "help":
			msg(	":zap: **Aide :** :zap:",
					""
					+ "!pay <PSEUDO/NOM/PRENOM/ID> <MONTANT>\n"
					+ "!info <PSEUDO/NOM/PRENOM/ID>\n"
					+ "!search <PSEUDO/NOM/PRENOM/ID>\n"
					+ "!list\n"
					+ "!verify\n"
					+ "!input <PSEUDO/NOM/PRENOM/ID> <NBR_RUN>");
			break;
			
		case "verify":
			if(verificationCount > 0) {
				msg(":x: **Verification déjà en cours !** :x:");
				return;
			}
			
			msg(":timer: Vérification des msgs... :timer:");
			System.out.println("Verifying all precedent messages");
			inst.client.getChannelById(inst.tournéeId)
				.cast(TextChannel.class)
				.flatMapMany(tc -> tc.getMessagesBefore(tc.getLastMessage().block().getId()))
				.concatWithValues(inst.client.getChannelById(inst.tournéeId).cast(TextChannel.class).flatMap(TextChannel::getLastMessage).block())
				.filter(p -> p.getReactors(Unicode.unicode("✅")).all(user -> !user.getId().equals(inst.selfId)).block())
				.doOnComplete(new Runnable() {
					
					@Override
					public void run() {
						System.out.println("Verification ended with " + verificationCount + "msg");
						msg(":white_check_mark: Vérification des tournées terminée (Total de " + verificationCount + " messages) :white_check_mark:");
						if(verificationCount > 0) {
							inst.toChannel(inst.tournéeId, ":white_check_mark: Comptabilisation des dernières tournées effectuée ! :white_check_mark:");
						}
					}
				})
				.subscribe(verifiedMsg -> {
					verificationCount += 1;
					inst.tournéeMsg(verifiedMsg, verifiedMsg.getAuthorAsMember().block());
				});
			verificationCount = 0;
			break;
			
		case "total":
			int total = 0;
			for(PlayerData dat : inst.saver.getAllData()) {
				total += dat.toPay();
			}

			msg("**TOTAL A PAYER AUX EMPLOYES**", "  " + total + "$");
			break;
			
		case "list":
			for(PlayerData dat : inst.saver.getAllData()) {
				dat.postPay();
			}
			break;
			
		case "infp":
		case "info":
			String search1 = rawText.replaceFirst(cmd + " ", "");
			
			List<PlayerData> datas1 = inst.saver.searchPlayers(search1);
			if(datas1.size() != 1) {
				msg(":x: **Joueur inconnu ou plusieurs joueurs du même nom (utiliser !search <NOM>) [" + datas1.size() + "]** :x:");
				return;
			}
			try {
				datas1.get(0).postPay();
			}catch(Exception ex) {
				msg(":x: **Une erreur est survenue** :x:");
			}
			break;

		case "cherche":
		case "recherche":
		case "research":
		case "search":
			String search = rawText.replaceFirst(cmd + " ", "");
			
			List<PlayerData> datas = inst.saver.searchPlayers(search);
			
			String searchResult = "\n";
			
			for(PlayerData uData : datas) {
				searchResult += 
							"**" + uData.displayName + "**\n"
						+ 	"  ID:" + uData.id + "\n"
						+   "  $: " + uData.toPay() + "\n"
						+   "  Pseudo: " + uData.username + "\n"
						+ 	"\n";
			}
			
			if(datas.isEmpty()) {
				searchResult += "*Aucun résultat...*";
			}
			
			msg("**Résultat de recherche**", searchResult);
			break;

		case "paye":
		case "pai":
		case "paie":
		case "pay":
			if(args.length == 2) {
				name = args[0];
				try {
					System.out.println(args[1]);
					amount = Integer.parseInt(args[1]);
				}catch(NumberFormatException e) {
					msg(":x: **Montant inconnu..** :x:");
					return;
				}
			} else if(args.length == 3) {
				name = args[0] + " " + args[1];
				try {
					System.out.println(args[2]);
					amount = Integer.parseInt(args[2]);
				}catch(NumberFormatException e) {
					msg(":x: **Montant inconnu...** :x:");
					return;
				}
			} else {
				msg(":x: **Mauvais argument pour la commande pay (utiliser !help)** :x:");
				return;
			}
			
			data = inst.saver.searchPlayer(name);
			if(data == null) {
				msg(":x: **Joueur inconnu ou plusieurs joueurs du même nom (utiliser !search <NOM>)** :x:");
				return;
			}
			
			if(amount < 0) {
				msg(":x: **Montant absurde...** :x:");
				return;
			}
			
			if(amount > 10000) {
				msg(":x: **Montant impossible !** :x:\n"
				+ 	"Les primes ne peuvent pas être plus élevé que 10k par jour !");
				return;
			}
			
			data.payToPlayer(amount);
			msg(":white_check_mark: Prime de " + amount + "$ délivré à **" + data.displayName + "** :white_check_mark:");
			inst.saver.save();
			
			break;
			
		case "input":

			if(args.length == 2) {
				name = args[0];
				try {
					System.out.println(args[1]);
					amount = Integer.parseInt(args[1]);
				}catch(NumberFormatException e) {
					msg(":x: **Montant inconnu..** :x:");
					return;
				}
			} else if(args.length == 3) {
				name = args[0] + " " + args[1];
				try {
					System.out.println(args[2]);
					amount = Integer.parseInt(args[2]);
				}catch(NumberFormatException e) {
					msg(":x: **Montant inconnu...** :x:");
					return;
				}
			} else {
				msg(":x: **Mauvais argument pour la commande pay (utiliser !help)** :x:");
				return;
			}
			
			data = inst.saver.searchPlayer(name);
			if(data == null) {
				msg(":x: **Joueur inconnu ou plusieurs joueurs du même nom (utiliser !search <NOM>)** :x:");
				return;
			}
			
			if(amount < 0) {
				msg(":x: **Montant absurde...** :x:");
				return;
			}
			
			if(amount > 20) {
				msg(":x: **Impossible d'ajouter plus de 20 run à la fois...** :x:");
			}
			
			data.addRun(amount);
			inst.saver.save();
			msg(":white_check_mark: Ajout manuel de " + amount + "RUN à **" + data.displayName + "** :white_check_mark:");
			data.postPay();
			break;
		
		}
	}

}
