package me.scmiller.guildarmory.Discord.Commands;

import java.awt.Color;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import me.scmiller.guildarmory.Enums;
import me.scmiller.guildarmory.SingletonConnection;
import me.scmiller.guildarmory.Discord.Commands.SubCommands.SubCommand;
import me.scmiller.guildarmory.Discord.Utilities.Utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;

/**
 * Requests information about a guild character and Inserts/ Updates them in Database
 * 
 * Issues: 
 * 	EventWaiter scope (SOLVED): 
 * 		Unable to pass information to next event waiter and decision tree must rebuild 
 * 		information needed by pulling from message history. This makes error checking 
 * 		and looping much more complex.
 * 		
 * 		SOLVED: Event Waiter Scope solved by utilizing a commandState storage object
 * 				(GuildCommandState), creating unique methods for each commands state,
 * 				and handling these methods recursively with the  waiterSwitch method.  
 * 				waiterSwitch looks for an event, determines how to handle the event 
 * 				with the GuildCommandState passed to it, and is recursively called until 
 * 				GuildCommandState is set to CommandState.CLOSE.
 * TODO:
 *   Get Subclass:
 * 		Final WaiterSwitch Command will be to loop through Spec enum, print all specs of
 * 		chosen class, and wait for users selection.
 *   Re-Implement CommandStates:
 * 		With GuildCommandStates so multiple commands from same user can
 * 		be dealt with.
 *   Refactoring:
 * 		Write ArmorySQLCommands.
 * 		Make Command Methods usable by other Commands if needed.
 * 	 JavaDoc:
 * 		Replace plain references with javadoc links.
 * 		
 * Functionality: 
 * 	Private Messages the user who issues the command.  Requests they paste their character info,
 * 	determines their character's class, creates and new selection response based on all character
 * 	specs that share that class, builds a CharacterInfo object with proper spec and class, updates the 
 * 	database, and sends the user an embeded summary of their updated character.
 * 
 */
public class AddCharacterNew extends Command{
		
	private EventWaiter waiter;
	
	public AddCharacterNew(EventWaiter waiter) {
		super.name = "AddCharacter";
		super.help = "Step by Step add character to guild roster";
		super.aliases = new String[] {"addCharacter, addcharacter"};
		
		this.waiter = waiter;
	}
	
	@Override
	protected void execute(CommandEvent event) {
		
			Message msg = event.getMessage();
			GuildCommandState gcs = new GuildCommandState(event, 4);
			SubCommand[] commands = {new CommandOne(), new CommandTwo(), new CommandThree(), new CommandFour()};
			WaiterSwitch waiterSwitch = new WaiterSwitch();
			
			//Initialize GuildCommandState with the CommandEvent
			//and maximum number of commands command requires
			
			event.getMessage().delete().queue();
			//Delete message from Parent Channel
			
			//Prompt user for first response, update CommandState
			gcs = commands[0].execute(gcs, msg.getContentRaw());
			
			//Begin Recursive calls
			waiterSwitch.execute(waiter, commands, gcs);
		}

	/*
	 * ---------------------------------------------------
	 * Command Methods
	 * ---------------------------------------------------
	 * The individuals steps needed to execute the parent
	 * command in methods.  
	 * 
	 * TODO: Change to lambda expressions of an abstract method?
	 */
	
	/**
	 * Prompts user for their Name.  Nothing that can go wrong.
	 * @param cs last command state
	 * @param msg currest user response
	 * @return Updated commandState
	 */
	public class CommandOne implements SubCommand
	{
		public GuildCommandState execute(GuildCommandState cs, final String msg) {
			String instruction = "**Enter Character's name (include special characters)**";	
			Utils.sendPrivateMessage(cs.EVENT.getAuthor(), new MessageBuilder(instruction).build());
			
			cs.next(msg);
		return cs;
		}
	}
	/**
	 * Handles user response.  Kicks commandstate back to previous commandstate if input
	 * is bad.  Otherwise, build selection list of classes, print.
	 * 
	 * @param cs last command state
	 * @param msg current user response
	 * @return Updated commandState
	 */
	public class CommandTwo implements SubCommand
	{
		public GuildCommandState execute(GuildCommandState cs, final String msg) { 
			StringBuilder builder = new StringBuilder();
			
			builder.append("**Choose Class: **\n");
			builder.append("`01` <:Mage:634827700716175390> Mage \n");
			builder.append("`02` <:Warlock:634827637138784307> Warlock\n");
			builder.append("`03` <:Priest:634827601386536991> Priest\n");
			builder.append("`04` <:Hunter:634827790180548609> Hunter\n");
			builder.append("`05` <:Druid:634827759427911687> Druid\n");
			builder.append("`06` <:Shaman:634827825232347149> Shaman\n");
			builder.append("`07` <:Paladin:634827565084704797> Paladin\n");
			builder.append("`08` <:Warrior:634827672698224693> Warrior\n");
			builder.append("`09` <:Rogue:634827723935842348> Rogue\n");
			
			if(msg.length() <= 12 && msg.indexOf(" ") == -1) {
				try 
				{
					long guild_id = cs.EVENT.getGuild().getIdLong();
					long user_id  = cs.EVENT.getMessage().getAuthor().getIdLong();
					
					Statement stmt = SingletonConnection.getConnection().createStatement();
					ResultSet rs = stmt.executeQuery("SELECT * FROM roster WHERE (guild_id = " 
								+ guild_id + " AND char_name ='" + msg + "');");
					
					if(!rs.next()) {
						stmt.execute("INSERT INTO roster(guild_id, user_id, char_name) VALUES (" 
								+ guild_id + "," + user_id +", '" + msg + "');");
	
						Utils.sendPrivateMessage(cs.EVENT.getAuthor(), new MessageBuilder(builder.toString()).build());
						cs.next(msg);
					}else {
						Utils.sendPrivateMessage(cs.EVENT.getAuthor(), 
								new MessageBuilder("Character Already in Database! Use !UpdateArmory to update.").build());
						cs.close();
					}
				}catch(Exception ex) {
					ex.printStackTrace();
					//TODO: Database offline, try again later.  Close command.
				}
			}else {
				Utils.sendPrivateMessage(cs.EVENT.getAuthor(), new MessageBuilder("Invalid Input! Please Try Again.").build());
				cs.tryAgain();
			}
			
			return cs;
		}
	}
	
	/**
	 * Handle response to request for class.  Kick back if bad.  Otherwise,
	 * add to database, compile spec selection based on selected class, print
	 * selection menu.
	 * 
	 * @param cs last command state
	 * @param msg current user response
	 * @return Updated commandState
	 */
	public class CommandThree implements SubCommand
	{
		public GuildCommandState execute(GuildCommandState cs, final String msg) {
			try {
				Statement stmt = SingletonConnection.getConnection().createStatement();
			
				String lastMsg = cs.getMessageAt(cs.getState() - 1);
				
				String selection2 = "";
				
				//Error-check response
				//	Second character of response should be the index - 1 of where that spec is stored in
				//	possibleSpec2.  If that char isn't a valid digit, or out of range, a error will be thrown
				//TODO If bad response, give opportunity to re-enter.  This will be complex since it breaks
				//		building the character from the previous event.
				if(msg.length() == 2 && msg.charAt(0) == '0' 
						&& msg.charAt(1) >= '1'
						&& Character.isDigit(msg.charAt(1)))
				{
					switch(msg) {
					case "01": 
						selection2 = "MAGE";
						break;
					case "02": 
						selection2 = "WARLOCK";
						break;
					case "03": 
						selection2 = "PRIEST";
						break;
					case "04": 
						selection2 = "HUNTER";
						break;
					case "05": 
						selection2 = "DRUID";
						break;
					case "06": 
						selection2 = "SHAMAN";
						break;
					case "07": 
						selection2 = "PALADIN";
						break;
					case "08": 
						selection2 = "WARRIOR";
						break;
					case "09": 
						selection2 = "ROGUE";
						break;
					}
					//TODO: Separate SQL class for armory 
					stmt.execute("UPDATE roster SET char_class = '" + selection2 + "' WHERE( guild_id = "
							+ cs.EVENT.getGuild().getIdLong() + " AND char_name = '" + lastMsg + "');");
					
					StringBuilder builder = new StringBuilder();
					
					builder.append("**Choose Spec: **\n");
					Enums.SpecType[] specs = Enums.SpecType.values();
					int goodSpecs = 0;
					for(int i = 0; i < specs.length ; i++) {
						if(specs[i].getPlayerClass().equalsIgnoreCase(selection2)) {
							goodSpecs++;
							builder.append("`0" + goodSpecs + "` " + specs[i].getName() + "\n");
						}
					}
					
					Utils.sendPrivateMessage(cs.EVENT.getAuthor(), new MessageBuilder(builder.toString()).build());
					cs.next(msg);
					
				}else {
					System.out.println("failed! Bad input! INPUT = " + msg);
					Utils.sendPrivateMessage(cs.EVENT.getAuthor(), new MessageBuilder("INVALID INPUT! Input must match selections exactly. `01`, `03`, etc. Try Again.").build());
					
					//TODO: Selections should be own methods so they can be used for error codes without redundant code
					StringBuilder builder = new StringBuilder();
					
					builder.append("**Choose Class: **\n");
					builder.append("`01` <:Mage:634827700716175390> Mage \n");
					builder.append("`02` <:Warlock:634827637138784307> Warlock\n");
					builder.append("`03` <:Priest:634827601386536991> Priest\n");
					builder.append("`04` <:Hunter:634827790180548609> Hunter\n");
					builder.append("`05` <:Druid:634827759427911687> Druid\n");
					builder.append("`06` <:Shaman:634827825232347149> Shaman\n");
					builder.append("`07` <:Paladin:634827565084704797> Paladin\n");
					builder.append("`08` <:Warrior:634827672698224693> Warrior\n");
					builder.append("`09` <:Rogue:634827723935842348> Rogue\n");
					
					Utils.sendPrivateMessage(cs.EVENT.getAuthor(), new MessageBuilder(builder.toString()).build());
				}
			}catch(Exception ex) {
				//DATABASE OFFLINE!
				ex.printStackTrace();
			}
			
			return cs;
		}
	}
	
	/**
	 * Handle response to request for spec.  Kick back if bad.  Otherwise,
	 * add to database, compile embedded response based on all responses--now
	 * stored in database--respond with embedded message.
	 * selection menu.
	 * 
	 * @param cs last command state
	 * @param msg current user response
	 * @return Updated commandState
	 */
	
	public class CommandFour implements SubCommand
	{
		public GuildCommandState execute(GuildCommandState cs, final String msg) {
		try {
			Statement stmt = SingletonConnection.getConnection().createStatement();
		
			String char_name = cs.getMessageAt(cs.getState() - 2);
			String char_class = cs.getMessageAt(cs.getState() - 1);
			String selection = "";
			String char_spec = "";
			
			ArrayList<String> classSpecs = new ArrayList<String>();

			Enums.SpecType[] allSpecs = Enums.SpecType.values();
			
			//Error-check response
			//	Second character of response should be the index - 1 of where that spec is stored in
			//	possibleSpec2.  If that char isn't a valid digit, or out of range, a error will be thrown
			//TODO If bad response, give opportunity to re-enter.  This will be complex since it breaks
			//		building the character from the previous event.
			if(msg.length() == 2 && msg.charAt(0) == '0' 
					&& msg.charAt(1) >= '1'
					&& Character.isDigit(msg.charAt(1)))
			{	
				System.out.println("CHAR_CLASS: " + char_class);
				switch(char_class) {
				case "01": 
					selection = "MAGE";
					break;
				case "02": 
					selection = "WARLOCK";
					break;
				case "03": 
					selection = "PRIEST";
					break;
				case "04": 
					selection = "HUNTER";
					break;
				case "05": 
					selection = "DRUID";
					break;
				case "06": 
					selection = "SHAMAN";
					break;
				case "07": 
					selection = "PALADIN";
					break;
				case "08": 
					selection = "WARRIOR";
					break;
				case "09": 
					selection = "ROGUE";
					break;
				}
				
				
				for(int i = 0; i < allSpecs.length ; i++) {
					System.out.println(allSpecs[i].getName());
					if(allSpecs[i].getPlayerClass().equalsIgnoreCase(selection)) {
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
				stmt.execute("UPDATE roster SET char_spec = '" + char_spec + "' WHERE( guild_id = "+ cs.EVENT.getGuild().getIdLong() + " AND char_name = '" + char_name + "');");
				
				Utils.sendPrivateMessage(cs.EVENT.getAuthor(), new MessageBuilder(buildEmbededRosterSpot(cs, char_name)).build());
				
				cs.close();
			}else {
				Utils.sendPrivateMessage(cs.EVENT.getAuthor(), new MessageBuilder("Invalid Input! Please Try Again.").build());
				cs.tryAgain();
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
			return cs;
	}
}
	
	/**
	 * Build embedded response based on new information now stored in database.
	 * Call database using passed character name response and unique guild id stored
	 * in CommandState.
	 * 
	 * @param cs current commandState
	 * @param name character name user initially inputed
	 * @return
	 */
	public static EmbedBuilder buildEmbededRosterSpot(GuildCommandState cs, String name) {
		
		EmbedBuilder eb = new EmbedBuilder();
		
		try {
			Statement stmt = SingletonConnection.getConnection().createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM roster WHERE (guild_id = " + cs.EVENT.getGuild().getIdLong() + " AND char_name ='" + name + "');");
			if(rs.next()) {
				//TODO REENABLE when Google Sheet integration is complete
				//eb.setTitle("Click Here to View Guild Armory", "https://docs.google.com/spreadsheets/d/14mu4lQYVezS5lRRwc1hk1ahQAu3k6JgR8ZWi2Oa-63I/edit#gid=894801606");
				eb.setDescription(rs.getString(3) + " | " + rs.getString(5) + " | " + rs.getString(4));
				eb.setColor(Color.GREEN);
				eb.setAuthor(name + "'s Has been added to the Guild Roster!", null, null);
				eb.setFooter("Guild Armory is in active developement. Report Bugs!");
			}
		}catch(Exception e) {
			e.printStackTrace();
		}

		return eb;
	}
}