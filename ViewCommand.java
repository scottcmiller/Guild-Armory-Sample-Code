package me.scmiller.guildarmory.Discord.Commands;

import java.awt.Color;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import me.scmiller.guildarmory.CharacterInfo;
import me.scmiller.guildarmory.Enums;
import me.scmiller.guildarmory.Enums.ClassType;
import me.scmiller.guildarmory.Enums.PlayerRace;
import me.scmiller.guildarmory.Enums.SpecType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import me.scmiller.guildarmory.Item;
import me.scmiller.guildarmory.Items;
import me.scmiller.guildarmory.PlayerItems;
import me.scmiller.guildarmory.SingletonConnection;

/**
 * !View allows all users on a discord server to view their fellow member's 
 * character information.  Returns an embedded response with the Character's
 * name, class, spec and all Items.
 * 
 * TODO: Link Guild's Armory Google Sheet in Title when fully integrated.
 * 
 * @author &#064scottc_miller
 *
 */
public class ViewCommand extends Command{
	public ViewCommand() {
		super.name = "View";
		super.help = "Returns armory info on a character";
		super.arguments = "[character's name]";
		super.aliases = new String[] {"view"};
		
	}

	@Override
	protected void execute(CommandEvent event) {
		String response = event.getMessage().getContentRaw();
		
		System.out.println(response);
		
		//Remove command from response
		if(response.indexOf(' ') >= 0) {
			response = response.substring(response.indexOf(' ') + 1, response.length());
			System.out.println(response);
			Statement stmt;
			//THIS SQL QUEREY CAN BE RESUABLE CODE!!!!!!
			//TODO ADD GUILD ID
			String command = ("SELECT * FROM armory WHERE (UPPER(char_name) LIKE UPPER('" + response + "') AND guild_id = " + event.getGuild().getIdLong() + ");");
			System.out.println(command);
			//System.out.println(command); 
			ResultSet rs;
			try {
					Connection conn = SingletonConnection.getConnection();
					System.out.print("RETRIEVING [" + response + "] FROM DATABASE...\n");
					stmt = conn.createStatement();
					rs = stmt.executeQuery(command);
					if(rs.next()) {
						CharacterInfo c = new CharacterInfo();
						PlayerItems i = new PlayerItems();;
						
						//Get non-item info
						c.setName(rs.getString(2));
						c.setRace(Enums.valueOfIgnoreCase(PlayerRace.class, Enums.convertName(rs.getString("char_race"))));
						c.setPlayerClass(Enums.valueOfIgnoreCase(ClassType.class, Enums.convertName(rs.getString("char_class"))));
						c.setSpec(Enums.valueOfIgnoreCase(SpecType.class, Enums.convertName(rs.getString("char_spec"))));
						
						//Get all items
						i.setHead(Items.getItem(rs.getString("helm"), c.getPlayerClass().getName()));
						i.setNeck(Items.getItem(rs.getString("neck"), c.getPlayerClass().getName()));
						i.setShoulders(Items.getItem(rs.getString("shoulders"), c.getPlayerClass().getName()));
						i.setBack(Items.getItem(rs.getString("back"), c.getPlayerClass().getName()));
						i.setChest(Items.getItem(rs.getString("chest"), c.getPlayerClass().getName()));
						i.setWrists(Items.getItem(rs.getString("wrists"), c.getPlayerClass().getName()));
						i.setGloves(Items.getItem(rs.getString("gloves"), c.getPlayerClass().getName()));
						i.setWaist(Items.getItem(rs.getString("waist"), c.getPlayerClass().getName()));
						i.setLegs(Items.getItem(rs.getString("leggings"), c.getPlayerClass().getName()));
						i.setBoots(Items.getItem(rs.getString("boots"), c.getPlayerClass().getName()));
						i.setRing_0(Items.getItem(rs.getString("ring_0"), c.getPlayerClass().getName()));
						i.setRing_1(Items.getItem(rs.getString("ring_1"), c.getPlayerClass().getName()));
						i.setTrinket_0(Items.getItem(rs.getString("trinket_0"), c.getPlayerClass().getName()));
						i.setTrinket_1(Items.getItem(rs.getString("trinket_1"), c.getPlayerClass().getName()));
						i.setMainHand(Items.getItem(rs.getString("mainHand"), c.getPlayerClass().getName()));
						i.setOffHand(Items.getItem(rs.getString("offHand"), c.getPlayerClass().getName()));
						i.setRanged(Items.getItem(rs.getString("ranged"), c.getPlayerClass().getName()));
						
						//Add items to CharacterInfo object
						c.setItems(i);
						
						event.reply(buildEmbededArmory(c).build());
					}else {
						event.reply(response + " has not been added to the Armory!");
					}
				}catch(Exception e) {
					e.printStackTrace();
				}
		}else {
			event.reply("Invalid Arguments! Type !View <Character's Name>.");
		}
		
		event.getMessage().delete().queue();
	}
	
	public static EmbedBuilder buildEmbededArmory(CharacterInfo c) {
		ArrayList<PlayerItems.SlottedItem> ar = c.getItems().getSlottedItems();
		EmbedBuilder eb = new EmbedBuilder();
		
		//TODO REENABLE when Google Sheet integration is complete
		//eb.setTitle("Click Here to View Guild Armory", "https://docs.google.com/spreadsheets/d/14mu4lQYVezS5lRRwc1hk1ahQAu3k6JgR8ZWi2Oa-63I/edit#gid=894801606");
		eb.setDescription(c.getRace().toUpperCase() + " | " + c.getPlayerClass().getName().toUpperCase() + " | " + c.getSpecString().toUpperCase());
		eb.setColor(Color.GREEN);
		eb.setAuthor(c.getName() + "'s Armory!", null, null);
		eb.setFooter("Guild Armory is in active developement. Report Bugs!");
		
		for(PlayerItems.SlottedItem s: ar)
		{	
			if(s.item.getId() > 0) {
				Field f = new Field(s.slot, "[["+ s.item.getName() + "](" + s.item.getLink() + ")]", true);
				eb.addField(f);
			}
		}

		return eb;
	}

}
