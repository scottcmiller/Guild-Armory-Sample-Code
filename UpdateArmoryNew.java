package me.scmiller.guildarmory.Discord.Commands;

import java.awt.Color;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import me.scmiller.guildarmory.ArmoryIngest;
import me.scmiller.guildarmory.CharacterInfo;
import me.scmiller.guildarmory.Enums;
import me.scmiller.guildarmory.PlayerItems;
import me.scmiller.guildarmory.SingletonConnection;
import me.scmiller.guildarmory.Discord.DiscordBot;
import me.scmiller.guildarmory.Discord.Commands.SubCommands.SubCommand;
import me.scmiller.guildarmory.Discord.Utilities.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;

/**
 * Allows armory information on a character to be recorded in database
 * 
 * This command utilizes the string export from the Classic Armory addon.
 * The user will paste and send it as a response, and parsed by ArmoryIngest.
 * 
 * If the character already exists in the guild roster, their armory table
 * will be auto-updated.  Otherwise, the character class will be pulled from
 * the Classic Armory String, a second response subcommand will be triggered
 * asking for the user's selection, and then the character is added to both
 * the roster and armory.   
 * 
 * The user's discord guild and user id's are stored with character info so
 * other program functions can compile user and guild specific information.
 */
public class UpdateArmoryNew extends Command{
		
	private EventWaiter waiter;
	
	public UpdateArmoryNew(EventWaiter waiter) {
		super.name = "UpdateArmory";
		super.help = "Step by Step character upload / update.  Bot will PM you.";
		super.aliases = new String[] {"updatearmory, updateArmory"};
		
		this.waiter = waiter;
	}
	
	public void execute(CommandEvent event) {
		Message msg = event.getMessage();
		
		SubCommand[] commands = new SubCommand[] {new UACommandOne(), new UACommandTwo(), new UACommandThree()};
		GuildCommandState gcs = new GuildCommandState(event, 3);
		//Initialize GuildCommandState with the CommandEvent
		//and maximum number of commands command requires
		WaiterSwitch waiterSwitch = new WaiterSwitch();
		
		//Initialize GuildCommandState with the CommandEvent
		//and maximum number of commands command requires
		
		//Prompt user for first response, update CommandState
		gcs = commands[0].execute(gcs, msg.getContentRaw());
		
		//Begin Recursive calls
		waiterSwitch.execute(waiter, commands, gcs);
	}
	
	/**
	 * Prompts User to sumbit their Classic Armory string
	 * @author &#064scottc_miller
	 *
	 */
	public class UACommandOne implements SubCommand
	{
		public GuildCommandState execute(GuildCommandState cs, String msg) {
			//Instruction to be sent to user
			//These types of Instructions could be loaded from a config file for easier management
			String instruction = "**Using the Classic Armory Addon, Open your character window, press upload, copy all text, and paste here**";
			
			Utils.sendPrivateMessage(cs.EVENT.getAuthor(), new MessageBuilder(instruction).build());
			
			//Delete message from guild channel to reduce clutter
			cs.EVENT.getMessage().delete().queue();
	
			cs.next(msg);
			return cs;
		}
	}
	
	/**
	 * Takes user's response and parses the String using {@link me.scmiller.guildarmory.ArmoryIngest#ingestNewEntry(String)}
	 * 
	 * Requests User's subclass if they are not already in the guild roster.  Then submits their info to both the roster as a
	 * new entry and the armory table.
	 * 
	 * @author &#064scottc_miller
	 *
	 */
	public class UACommandTwo implements SubCommand
	{
		public GuildCommandState execute(GuildCommandState cs, String msg) {
			
			StringBuilder builder = new StringBuilder(); //To build secondary response
			CharacterInfo c       = new CharacterInfo(); //Create a CharacterInfo from user response to find class
			
			//TODO - Create function to only return class
			// IF waiter-scope issue cannot be resolved
			
			try 
			{
				c = ArmoryIngest.ingestNewEntry(msg);

				String statement = "SELECT * FROM roster WHERE (guild_id = ? AND char_name = ?);";
				PreparedStatement p = SingletonConnection.getConnection().prepareStatement(statement);
				
				p.setLong(1, cs.EVENT.getGuild().getIdLong());
				p.setString(2, c.getName());
				
				ResultSet rs = p.executeQuery();
				
				
				if(rs.next()) {
					
					System.out.println(c.getName());
					c.setSpec(Enums.SpecType.valueOf(Enums.convertName(rs.getString("char_spec")).toUpperCase()));
					ArmoryIngest.addCharacterToDB(c, cs.EVENT.getGuild().getIdLong());
					Utils.sendPrivateMessage(cs.EVENT.getAuthor(), new MessageBuilder(buildEmbeddedArmory(c).build()).build());
					cs.close();
				}else {
					statement = "REPLACE into roster (guild_id, user_id, char_name, char_spec, char_class) VALUES (?,?,?,?,?);";
					p = SingletonConnection.getConnection().prepareStatement(statement);

					p.setLong(1, cs.EVENT.getGuild().getIdLong());
					p.setLong(2, cs.EVENT.getAuthor().getIdLong());
					p.setString(3, c.getName());
					p.setString(4, "");
					p.setString(5, c.getPlayerClass().getName());
					p.executeUpdate();
					
					builder = new StringBuilder();
					builder.append("**Choose Spec: **\n");
					Enums.SpecType[] specs = Enums.SpecType.values();
					int goodSpecs = 0;
					for(int i = 0; i < specs.length ; i++) {
						if(specs[i].getPlayerClass().equalsIgnoreCase(c.getPlayerClass().getName())) {
							goodSpecs++;
							builder.append("`0" + goodSpecs + "` " + specs[i].getName() + "\n");
						}
					}
					
					Utils.sendPrivateMessage(cs.EVENT.getAuthor(), new MessageBuilder(builder.toString()).build());
					cs.next(msg);
				}
				
			}catch(ArmoryIngest.BadArmoryException ae) {
				ae.printStackTrace();
				Utils.sendPrivateMessage(cs.EVENT.getAuthor(), new MessageBuilder("Invalid Input! Please Try Again.").build());
				cs.tryAgain();
				
				return cs;
			}catch(Exception se) {
				se.printStackTrace();
				Utils.sendPrivateMessage(cs.EVENT.getAuthor(), new MessageBuilder("Database Offline.  Please Try again later").build());
				cs.close();
				
				return cs;
			}
	
			return cs;
		}
	}
	
	public class UACommandThree implements SubCommand
	{
		public GuildCommandState execute(GuildCommandState cs, String msg) {
			try 
			{
				if(msg.length() == 2 && msg.charAt(0) == '0' 
						&& msg.charAt(1) >= '1'
						&& Character.isDigit(msg.charAt(1)))
				{	
					Statement stmt = SingletonConnection.getConnection().createStatement();
					
					String char_spec = "";
					CharacterInfo c       = new CharacterInfo(); //Create a CharacterInfo from user response to find class
					
					c = ArmoryIngest.ingestNewEntry(cs.getMessageAt(cs.getState() - 1));
					
					ArrayList<String> classSpecs = new ArrayList<String>();
			
					Enums.SpecType[] allSpecs = Enums.SpecType.values();
					
					for(int i = 0; i < allSpecs.length ; i++) {
						System.out.println(allSpecs[i].getName());
						
						System.out.println();
						if(allSpecs[i].getPlayerClass().equalsIgnoreCase(c.getPlayerClass().getName())) {
							classSpecs.add(allSpecs[i].getName());
						}
					}
					
					int index = new Integer(msg.substring(1));
					System.out.println("Possble Specs:\n");
					for(String s : classSpecs) {
						System.out.println(s);
					}
					System.out.println("USER INPUT AS INDEX: " + (index - 1));
					
					char_spec = classSpecs.get(index - 1);
					c.setSpec(Enums.SpecType.valueOf(Enums.convertName(char_spec).toUpperCase()));
					
					stmt.execute("UPDATE roster SET char_spec = '" + char_spec + "' WHERE( guild_id = "+ cs.EVENT.getGuild().getIdLong() + " AND char_name = '" + c.getName() + "');");
					stmt.close();
					
					ArmoryIngest.addCharacterToDB(c, cs.EVENT.getGuild().getIdLong());
					Utils.sendPrivateMessage(cs.EVENT.getAuthor(), new MessageBuilder(buildEmbeddedArmory(c).build()).build());
					cs.close();
					
				}else {
					Utils.sendPrivateMessage(cs.EVENT.getAuthor(), new MessageBuilder("Invalid Input! Please Try Again.").build());
					cs.tryAgain();
				}
				
			}catch(ArmoryIngest.BadArmoryException ae) {
				ae.printStackTrace();
				
				Utils.sendPrivateMessage(cs.EVENT.getAuthor(), new MessageBuilder("Invalid Input! Please Try Again.").build());
				cs.tryAgain();
				
				return cs;
			}catch(Exception se) {
				se.printStackTrace();
				Utils.sendPrivateMessage(cs.EVENT.getAuthor(), new MessageBuilder("Database Offline.  Please Try again later").build());
				cs.close();
				
				return cs;
			}
			return cs;
		}
	}
	
	public static EmbedBuilder buildEmbeddedArmory(CharacterInfo c) {
		ArrayList<PlayerItems.SlottedItem> ar = c.getItems().getSlottedItems();
		EmbedBuilder eb = new EmbedBuilder();
		
		//TODO REENABLE when Google Sheet integration is complete
		//eb.setTitle("Click Here to View Guild Armory", "https://docs.google.com/spreadsheets/d/14mu4lQYVezS5lRRwc1hk1ahQAu3k6JgR8ZWi2Oa-63I/edit#gid=894801606");
		eb.setDescription(c.getRace().toUpperCase() + " | " + c.getPlayerClass().getName().toUpperCase() + " | " + c.getSpecString().toUpperCase());
		eb.setColor(Color.GREEN);
		eb.setAuthor(c.getName() + " has been Updated!", null, null);
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
