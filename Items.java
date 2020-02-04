package me.scmiller.guildarmory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import me.scmiller.guildarmory.Wowhead.ItemDoesNotExistException;
import me.scmiller.guildarmory.utilities.ItemSqlCommands;

/**
 * The itemDB table is quite large and will be called far more than anything else.
 * Items speeds up reading from our database of items and loading into memory.
 * 
 * Items are stored in a static ArrayList of Items, with 2 custom Comparators
 * for searching and sorting.
 * 
 * If an item is fetched, but not found in ArrayList, Items will ping Wowhead to
 * see if it does exist, and add it to the ArrayList and SQL database if it does.
 * 
 * Items are loaded into memory from SQL database when load() is called.
 * Load() checks to make sure the item hasn't already been added to the ArrayList
 * before inserting items.  Protects against duplicated being added on secondary load()
 * calls.
 * 
 * @author &#064scottc_miller
 *
 */
public class Items{
	private static ArrayList<Item> items_by_id = new ArrayList<Item>();
	private static ArrayList<Item> items_by_name = new ArrayList<Item>();
	
	/**
	 * Comparing Items by JUST their name will have erroneous results
	 * dude to Item Names not being a unique identifier. Items such as the 
	 * [Royal Seal of Eldre'Thalas] shares a single name between 8 items 
	 * that are class specific.<br></br>  
	 * 
	 * So, when these items are fetched, the Comparator will determine they 
	 * are these special edge cases by checking if their isUnique_name() returns false.
	 * If it does, the Comparator will check to see if both items have their 
	 * requiredClass values set properly.<br></br>
	 * 
	 * In cases where both items DO have these values set, the comparator will compare
	 * them.<br></br>  
	 * 
	 * In cases where they don't (getItem(name) called instead of 
	 * getItem(name, req_class), Comparator will throw a ItemNeedsClassException letting
	 * the method invoking the Comparator that it needs to re-invoke it with items that
	 * have their req_class values set.<br></br>
	 */
	private static Comparator<Item> c_name_verbose = new Comparator<Item>() {
		public int compare(Item i1, Item i2) 
		{
			
			int strCompare = i1.getName().compareToIgnoreCase(i2.getName());
			
			if(strCompare != 0) 
			{
				return strCompare;
			}
			else if(strCompare == 0 && !i1.isUnique_name()) 
			{
				if(i2.getReqclass().isEmpty() || i2.getReqclass().isEmpty()) 
				{
					throw new ItemNeedsClassException();
				}else 
				{
					return i1.getReqclass().compareToIgnoreCase(i2.getReqclass());
				}	
			}else 
			{
				return strCompare;
			}
			//If strings are equal, check id's
			//Probably should be throwing error to make sure search w/ reqclass is being used
		}
	};
	
	private static Comparator<Item> c_name = new Comparator<Item>() {
		public int compare(Item i1, Item i2) 
		{
			
			int strCompare = i1.getName().compareToIgnoreCase(i2.getName());
			
			if(strCompare != 0) 
			{
				return strCompare;
			}else if(strCompare == 0 && !i1.isUnique_name()) 
			{
				if(i2.getReqclass().isEmpty() || i2.getReqclass().isEmpty()) 
				{
					throw new ItemNeedsClassException();
				}else 
				{
					return i1.getReqclass().compareToIgnoreCase(i2.getReqclass());
				}	
			}else {
				return strCompare;
			}
			//If strings are equal, check id's
			//Probably should be throwing error to make sure search w/ reqclass is being used
		}
	};
	
	private static Comparator<Item> c_id = new Comparator<Item>() {		
		public int compare(Item i1, Item i2) {
			return (i1.getId() - i2.getId());
		}
	};
	
	//Load Items from DB into ArrayLists
	static {
		load();
	}
	
	public static Item getItem(String name) throws ItemNeedsClassException
	{
		Item item = new Item();
		ItemInfo info = new ItemInfo();
		
		if(!name.equals("")) {
			int index = -1;
			boolean needsClass = false;
			
			try {
				index = search(name);
			}catch(ItemNeedsClassException e) {
				needsClass = true;
				throw e;
			}
			
			if(!needsClass) {
				item = findNameByIndex(index);
				
				if(item == null) {
					Wowhead w = new Wowhead();
					try {
						info = w.getInfo(name);
						
						item = new Item(info);
						updateDatabase(item);
						add(item);
					}
					catch(ItemDoesNotExistException e) {
						System.out.println("[" + name + "] does not exist in local or Wowhead Database!");
					}
				}
			}
		}
		
		return item;
	}
	
	public static Item getItem(String name, String char_class) 
	{
		
		Item item = new Item();
		ItemInfo info = new ItemInfo();
		
		if(!name.equals("")) {
			int index = search(name, char_class, false);
			
			//Handle reqclass being thrown
			item = findNameByIndex(index);
			
			if(item == null) {
				Wowhead w = new Wowhead();
				try {
					info = w.getInfo(name);
					
					item = new Item(info);
					updateDatabase(item);
					add(item);
				}
				catch(ItemDoesNotExistException e) {
					System.out.println("[" + name + "] does not exist in local or Wowhead Database!");
				}
			}
		}
		return item;
	}
	
	public static Item getItem(int id) 
	{
		int index = search(id);
		
		return findIDByIndex(index);
	}
	
	protected static void updateDatabase(Item i) {

		i.setUnique_name(search(i.getName()) >= 0 ? false : true);
		
		try {
			ItemSqlCommands.insertItem(i);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected static Item findNameByIndex(int index) {
		if(index >= 0) {
			return items_by_name.get(index);
		}
		else {
			return null;
		}
	}
	
	protected static Item findIDByIndex(int index) {
		if(index >= 0) {
			return items_by_id.get(index);
		}
		else {
			return null;
		}
	}
	
	public static void add(Item item) {
		insertUniqueItem(item);
	}
	
	protected final static void load() {
		System.out.println("Loading Items from Database....");
		Stopwatch stopwatch = Stopwatch.createStarted();

		try {
			Statement stmt = SingletonConnection.getConnection().createStatement();
			
			ResultSet rs = stmt.executeQuery("SELECT * FROM itemDB");
			
			while(rs.next()) {
				insertUniqueItemOnLoad(new Item(rs.getString(1), rs.getInt(2), rs.getString(3), rs.getString(4), 
								rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), 
								rs.getInt(9) > 0 ? true : false));
				items_by_name.sort(c_name);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		stopwatch.stop();
		System.out.println("..." + items_by_id.size() + " items loaded in: " + 
				   stopwatch.elapsed(TimeUnit.SECONDS) + " SECONDS\n");
	}
	
	private static void insertUniqueItem(Item item) {
		int index = search(item.getId());
		
	    if(index < 0) {
	        items_by_id.add(-(index) - 1, item);   
	        items_by_name.add(item);
	        
	        items_by_name.sort(c_name);
	    }
	    else {
	    	System.out.println(item.getName() + " IS A DUPLICATE");
	    }
	}
	
	private static void insertUniqueItemOnLoad(Item item) {
		int index = search(item.getId());
		
	    if(index < 0) {
	        items_by_id.add(-(index) - 1, item);   
	        items_by_name.add(item);
	    }else {
	    	System.out.println(item.getName() + " IS A DUPLICATE");
	    }
	}
	
	private static int search(int id) {
		return Collections.binarySearch(items_by_id, new Item(id), c_id);
	}
	
	private static int search(String name) throws ItemNeedsClassException{
		int index = -1;
		try {
			index = Collections.binarySearch(items_by_name, new Item(name), c_name);
		}catch(ItemNeedsClassException e) {
			throw e;
		}
		
		return index;
	}
	
	private static int search(String name, String char_class, boolean verbose) {
		int index;
		
		if(!verbose) index = Collections.binarySearch(items_by_name, new Item(name), c_name);
		else index = Collections.binarySearch(items_by_name, new Item(name), c_name_verbose);
		
		return index;
	}
	
	public static void print() {
		for(Item i : items_by_id) {
			System.out.println(i.toString());
		}
	}
	
	public static void printNameAr() {
		for(int i = 2270; i < 2280; i++) {
			System.out.println(items_by_name.get(i));
		}
	}
	
	@SuppressWarnings("serial")
	public static class ItemNeedsClassException extends RuntimeException{
		public ItemNeedsClassException() {
			super();
		}
	}
}
