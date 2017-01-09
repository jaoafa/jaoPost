package com.jaoafa.jaoPost.Event;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.jaoafa.jaoPost.JaoPost;
import com.jaoafa.jaoPost.MySQL;

public class ClickPostChest implements Listener {
	JavaPlugin plugin;
	public ClickPostChest(JavaPlugin plugin) {
		this.plugin = plugin;
	}
	public static Map<String,Map<Integer,Integer>> post = new HashMap<String,Map<Integer,Integer>>();
	public static Map<String,Map<Integer,Integer>> postjao = new HashMap<String,Map<Integer,Integer>>();
	public static Map<String,Map<Integer,String>> jao = new HashMap<String,Map<Integer,String>>();
	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent event){
		if(event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		Block block = event.getClickedBlock();
		Material material = block.getType();
		Player player = event.getPlayer();
		if (material == Material.CHEST) {
			Sign sign = getBlockStickSign(block.getLocation());
			if(sign == null){
				return;
			}
			if(!sign.getLine(1).equalsIgnoreCase("[post]")){
				return;
			}
			event.setCancelled(true);
			Statement statement;
			try {
				statement = JaoPost.c.createStatement();
			} catch (NullPointerException e) {
				MySQL MySQL = new MySQL("jaoafa.com", "3306", "jaoafa", JaoPost.sqluser, JaoPost.sqlpassword);
				try {
					JaoPost.c = MySQL.openConnection();
					statement = JaoPost.c.createStatement();
				} catch (ClassNotFoundException | SQLException e1) {
					e1.printStackTrace();
					return;
				}
			} catch (SQLException e) {
				e.printStackTrace();
				return;
			}
			statement = MySQL.check(statement);
			try {
				ResultSet res = statement.executeQuery("SELECT COUNT(id) FROM jaopost WHERE toplayer = '" + player.getName() + "' AND readed = false;;");
				double count = 0;
				if(res.next()){
					count = res.getInt(1);
				}
				count = count / 9;
				count = Math.ceil(count);
				int i = (int) count;
				if(i == 0){
					Inventory inv = Bukkit.getServer().createInventory(player, 3 * 9, "jaoPost - 受信箱");
					ItemStack item = new ItemStack(Material.BARRIER);
					ItemMeta itemmeta = item.getItemMeta();
					itemmeta.setDisplayName("受信箱は空です。");
					item.setItemMeta(itemmeta);
					inv.setItem(13, item);


					item = new ItemStack(Material.ENDER_CHEST);
					itemmeta = item.getItemMeta();
					itemmeta.setDisplayName("お知らせ(jaotanからのメッセージ)を見る");
					item.setItemMeta(itemmeta);
					inv.setItem(25, item);
					if(post.containsKey(player.getName())){
						Map<Integer, Integer> postdata = post.get(player.getName());
						postdata.put(25, -1);
						post.put(player.getName(), postdata);
					}else{
						Map<Integer, Integer> postdata = new HashMap<Integer, Integer>();
						postdata.put(25, -1);
						post.put(player.getName(), postdata);
					}

					ItemStack itemread = new ItemStack(Material.BUCKET);
					ItemMeta itemmetaread = itemread.getItemMeta();
					itemmetaread.setDisplayName("既読済みを見る");
					itemread.setItemMeta(itemmetaread);
					inv.setItem(26, itemread);
					if(post.containsKey(player.getName())){
						Map<Integer, Integer> postdata = post.get(player.getName());
						postdata.put(26, 0);
						post.put(player.getName(), postdata);
					}else{
						Map<Integer, Integer> postdata = new HashMap<Integer, Integer>();
						postdata.put(26, 0);
						post.put(player.getName(), postdata);
					}

					player.openInventory(inv);
					return;
				}
				i += 1;
				Inventory inv = Bukkit.getServer().createInventory(player, i * 9, "jaoPost - 受信箱");
				res = statement.executeQuery("SELECT * FROM jaopost WHERE toplayer = '" + player.getName() + "' AND readed = false;");
				int c = 0;
				while(res.next()){
					if(res.getBoolean("readed")){
						continue;
					}
					int id = res.getInt("id");
					String title = res.getString("title");
					String message = res.getString("message");
					String from = res.getString("fromplayer");
					String date = res.getString("date");
					IntoBook(inv, title, message, from, id, c, date);
					c++;
				}
				ItemStack item = new ItemStack(Material.ENDER_CHEST);
				ItemMeta itemmeta = item.getItemMeta();
				itemmeta.setDisplayName("お知らせ(jaotanからのメッセージ)を見る");
				item.setItemMeta(itemmeta);
				inv.setItem((i * 9)-2, item);
				if(post.containsKey(player.getName())){
					Map<Integer, Integer> postdata = post.get(player.getName());
					postdata.put((i * 9)-2, -1);
					post.put(player.getName(), postdata);
				}else{
					Map<Integer, Integer> postdata = new HashMap<Integer, Integer>();
					postdata.put((i * 9)-2, -1);
					post.put(player.getName(), postdata);
				}

				item = new ItemStack(Material.BUCKET);
				itemmeta = item.getItemMeta();
				itemmeta.setDisplayName("既読済みを見る");
				item.setItemMeta(itemmeta);
				inv.setItem((i * 9)-1, item);
				if(post.containsKey(player.getName())){
					Map<Integer, Integer> postdata = post.get(player.getName());
					postdata.put((i * 9)-1, 0);
					post.put(player.getName(), postdata);
				}else{
					Map<Integer, Integer> postdata = new HashMap<Integer, Integer>();
					postdata.put((i * 9)-1, 0);
					post.put(player.getName(), postdata);
				}

				player.openInventory(inv);
			} catch (SQLException e) {
				player.sendMessage("[jaoPost] " + ChatColor.GREEN + "受信箱のチェックに失敗しました。再度お試しください。");
				event.setCancelled(true);
			}
		}
	}
	@EventHandler(ignoreCancelled = true)
	public void onPostClick(InventoryClickEvent event) {
		if(event.getWhoClicked().getType() != EntityType.PLAYER) return;
		if(event.getClickedInventory() == null) return;
		if(!event.getClickedInventory().getName().equals("jaoPost - 受信箱")) return;
		Player player = (Player) event.getWhoClicked();

		if(post.containsKey(player.getName())){
			Map<Integer, Integer> postdata = post.get(player.getName());
			if(postdata.containsKey(event.getSlot())){
				int id = postdata.get(event.getSlot());
				if(id == 0){
					player.closeInventory();
					openReadedMessagesInv(player);
					return;
				}else if(id == -1){
					player.closeInventory();
					//openJaotanMessagesInv(player);
					new openJaotanMessagesInv(player).runTaskLater(plugin, 1);
					return;
				}
				Statement statement;
				try {
					statement = JaoPost.c.createStatement();
				} catch (NullPointerException e) {
					MySQL MySQL = new MySQL("jaoafa.com", "3306", "jaoafa", JaoPost.sqluser, JaoPost.sqlpassword);
					try {
						JaoPost.c = MySQL.openConnection();
						statement = JaoPost.c.createStatement();
					} catch (ClassNotFoundException | SQLException e1) {
						e1.printStackTrace();
						return;
					}
				} catch (SQLException e) {
					e.printStackTrace();
					return;
				}
				statement = MySQL.check(statement);
				try {
					statement.execute("UPDATE jaopost SET readed = true WHERE id = " + id);
				} catch (SQLException e) {
					player.sendMessage("[jaoPost] " + ChatColor.GREEN + "未既読の変更に失敗しました。再度お試しください。");
					event.setCancelled(true);
				}
			}
		}
	}
	private class openJaotanMessagesInv extends BukkitRunnable{
		Player player;
		public openJaotanMessagesInv(Player player) {
			this.player = player;
		}
		@Override
		public void run() {
			openJaotanMessagesInv(player);
		}
	}

	@EventHandler
	public void onJaotanClick(InventoryClickEvent event) {
		if(event.getWhoClicked().getType() != EntityType.PLAYER) return;
		if(event.getClickedInventory() == null) return;
		if(!event.getClickedInventory().getName().equals("jaoPost - お知らせ")) return;
		Player player = (Player) event.getWhoClicked();
		if(postjao.containsKey(player.getName())){
			Map<Integer, Integer> postdata = postjao.get(player.getName());
			if(postdata.containsKey(event.getSlot())){
				int id = postdata.get(event.getSlot());
				Statement statement1;
				try {
					statement1 = JaoPost.c.createStatement();
				} catch (NullPointerException e) {
					MySQL MySQL = new MySQL("jaoafa.com", "3306", "jaoafa", JaoPost.sqluser, JaoPost.sqlpassword);
					try {
						JaoPost.c = MySQL.openConnection();
						statement1 = JaoPost.c.createStatement();
					} catch (ClassNotFoundException | SQLException e1) {
						e1.printStackTrace();
						return;
					}
				} catch (SQLException e) {
					e.printStackTrace();
					return;
				}
				statement1 = MySQL.check(statement1);
				try {
					ResultSet res = statement1.executeQuery("SELECT * FROM jaoinfo WHERE id = " + id);
					if(!res.next()){
						player.sendMessage("[jaoPost] " + ChatColor.GREEN + "未既読の変更に失敗しました。再度お試しください。");
						player.closeInventory();
						event.setCancelled(true);
					}
					String readplayer = res.getString("readplayer");
					//String readplayer = "";
					if(!readplayer.contains(player.getName())){
						if(readplayer.equalsIgnoreCase("")){
							statement1.execute("UPDATE jaoinfo SET readplayer = \"" + player.getName() + "\" WHERE id = " + id);
						}else{
							statement1.execute("UPDATE jaoinfo SET readplayer = \"" + readplayer + "," + player.getName() + "\" WHERE id = " + id);
						}
					}
					CraftPlayer cp = (CraftPlayer) player;
					event.setCancelled(true);

					ItemStack is = event.getClickedInventory().getItem(event.getSlot());
					BookMeta isbm = (BookMeta) is.getItemMeta();

					ItemStack item = new ItemStack(Material.WRITTEN_BOOK, 1);
					BookMeta bm = (BookMeta) item.getItemMeta();
					bm.setAuthor(isbm.getAuthor());
					bm.setDisplayName(isbm.getDisplayName());
					List<String> pages_read = new ArrayList<String>();
					List<String> pages = isbm.getPages();
					for(int i=0; i < pages.size(); i++){
						BufferedReader br = new BufferedReader(new StringReader(pages.get(i)));
						String text = br.readLine();
						while(text != null){
							pages_read.add(text);
							text = br.readLine();
						}
					}
					bm.setPages(pages_read);
					bm.setLore(isbm.getLore());
					item.setItemMeta(bm);

					//ItemStack is = event.getClickedInventory().getItem(event.getSlot());
					player.getInventory().setItemInHand(item);
					cp.getHandle().openBook(CraftItemStack.asNMSCopy(item));

				} catch (SQLException | IOException e) {
					e.printStackTrace();
					player.sendMessage("[jaoPost] " + ChatColor.GREEN + "未既読の変更に失敗しました。再度お試しください。");
					player.closeInventory();
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPostClose(InventoryCloseEvent event) {
		if(event.getInventory() == null) return;
		if(event.getInventory().getName().equals("jaoPost - 受信箱")){
			post.remove(event.getPlayer().getName());
		}
		if(event.getInventory().getName().equals("jaoPost - お知らせ")){
			postjao.remove(event.getPlayer().getName());
		}
	}

	private void openReadedMessagesInv(Player player){
		Statement statement;
		try {
			statement = JaoPost.c.createStatement();
		} catch (NullPointerException e) {
			MySQL MySQL = new MySQL("jaoafa.com", "3306", "jaoafa", JaoPost.sqluser, JaoPost.sqlpassword);
			try {
				JaoPost.c = MySQL.openConnection();
				statement = JaoPost.c.createStatement();
			} catch (ClassNotFoundException | SQLException e1) {
				e1.printStackTrace();
				return;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		}
		statement = MySQL.check(statement);
		try {
			ResultSet res = statement.executeQuery("SELECT COUNT(id) FROM jaopost WHERE toplayer = '" + player.getName() + "' AND readed = true;");
			double count = 0;
			if(res.next()){
				count = res.getInt(1);
			}
			count = count / 9;
			count = Math.ceil(count);
			int i = (int) count;
			if(i == 0){
				Inventory inv = Bukkit.getServer().createInventory(player, 3 * 9, "jaoPost - 既読済箱");
				ItemStack item = new ItemStack(Material.BARRIER);
				ItemMeta itemmeta = item.getItemMeta();
				itemmeta.setDisplayName("既読済箱は空です。");
				item.setItemMeta(itemmeta);
				inv.setItem(13, item);
				player.openInventory(inv);
				return;
			}
			i += 1;
			Inventory inv = Bukkit.getServer().createInventory(player, i * 9, "jaoPost - 既読済箱");
			res = statement.executeQuery("SELECT * FROM jaopost WHERE toplayer = '" + player.getName() + "';");
			while(res.next()){
				if(!res.getBoolean("readed")){
					continue;
				}
				String title = res.getString("title");
				String message = res.getString("message");
				String from = res.getString("fromplayer");
				IntoBook_Readed(inv, title, message, from);
			}

			player.openInventory(inv);
		} catch (SQLException e) {
			player.sendMessage("[jaoPost] " + ChatColor.GREEN + "既読済箱のチェックに失敗しました。再度お試しください。");
		}
	}

	private void openJaotanMessagesInv(Player player){
		Statement statement;
		try {
			statement = JaoPost.c.createStatement();
		} catch (NullPointerException e) {
			MySQL MySQL = new MySQL("jaoafa.com", "3306", "jaoafa", JaoPost.sqluser, JaoPost.sqlpassword);
			try {
				JaoPost.c = MySQL.openConnection();
				statement = JaoPost.c.createStatement();
			} catch (ClassNotFoundException | SQLException e1) {
				e1.printStackTrace();
				return;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		}
		statement = MySQL.check(statement);
		try {
			ResultSet res = statement.executeQuery("SELECT * FROM `jaoinfo` ORDER BY `id` DESC");
			Inventory inv = Bukkit.getServer().createInventory(player, 5 * 9, "jaoPost - お知らせ");
			int c = 0;
			while(res.next()){
				String readed = res.getString("readplayer");
				String title;
				if(!readed.contains(player.getName())){
					title = res.getString("date") + "に投稿されたメッセージ" + ChatColor.RED + "(NEW!!)";
				}else{
					title = res.getString("date") + "に投稿されたメッセージ";
				}

				String message = res.getString("message");
				int id = res.getInt("id");

				IntoBook_Jaotan(inv, title, message, id, c);
				c++;
			}

			player.openInventory(inv);
		} catch (SQLException e) {
			player.sendMessage("[jaoPost] " + ChatColor.GREEN + "お知らせのチェックに失敗しました。再度お試しください。");
		}
	}

	private Sign getBlockStickSign(Location loc){
		Location locsign = loc.add(1, 0, 0);
		if(locsign.getBlock().getType() == Material.WALL_SIGN){
			Sign sign = (Sign) locsign.getBlock().getState();
			return sign;
		}
		locsign = loc.add(-2, 0, 0);
		if(locsign.getBlock().getType() == Material.WALL_SIGN){
			Sign sign = (Sign) locsign.getBlock().getState();
			return sign;
		}
		locsign = loc.add(1, 0, 1);
		if(locsign.getBlock().getType() == Material.WALL_SIGN){
			Sign sign = (Sign) locsign.getBlock().getState();
			return sign;
		}
		locsign = loc.add(0, 0, -2);
		if(locsign.getBlock().getType() == Material.WALL_SIGN){
			Sign sign = (Sign) locsign.getBlock().getState();
			return sign;
		}
		return null;
	}
	public static void IntoBook(Inventory inv, String title, String message, String from, int id, int slot, String date) {
		ItemStack item = new ItemStack(Material.WRITTEN_BOOK, 1);
		BookMeta bm = (BookMeta) item.getItemMeta();
		bm.setAuthor(from);
		bm.setDisplayName(title);
		ArrayList<String> pages = new ArrayList<String>();
		pages.add(message);
		bm.setPages(pages);
		List<String> lore = new ArrayList<String>();
		lore.add("受信日時: " + date);
		bm.setLore(lore);
		item.setItemMeta(bm);
		inv.addItem(item);

		if(inv.getHolder() != null && inv.getHolder() instanceof Player) {
			Player player = (Player) inv.getHolder();
			if(post.containsKey(player.getName())){
				Map<Integer, Integer> postdata = post.get(player.getName());
				postdata.put(slot, id);
				post.put(player.getName(), postdata);
			}else{
				Map<Integer, Integer> postdata = new HashMap<Integer, Integer>();
				postdata.put(slot, id);
				post.put(player.getName(), postdata);
			}
		}
	}

	private static void IntoBook_Jaotan(Inventory inv, String title, String message, int id, int slot) {
		ItemStack item = new ItemStack(Material.WRITTEN_BOOK, 1);
		BookMeta bm = (BookMeta) item.getItemMeta();
		bm.setAuthor("jaotan");
		bm.setDisplayName(title);
		ArrayList<String> pages = new ArrayList<String>();
		pages.add(message);
		bm.setPages(pages);
		item.setItemMeta(bm);
		inv.addItem(item);

		if(inv.getHolder() != null && inv.getHolder() instanceof Player) {
			Player player = (Player) inv.getHolder();
			if(postjao.containsKey(player.getName())){
				Map<Integer, Integer> postdata = postjao.get(player.getName());
				postdata.put(slot, id);
				postjao.put(player.getName(), postdata);
			}else{
				Map<Integer, Integer> postdata = new HashMap<Integer, Integer>();
				postdata.put(slot, id);
				postjao.put(player.getName(), postdata);
			}
		}
	}

	private static void IntoBook_Readed(Inventory inv, String title, String message, String from) {
		ItemStack item = new ItemStack(Material.WRITTEN_BOOK, 1);
		BookMeta bm = (BookMeta) item.getItemMeta();
		bm.setAuthor(from);
		bm.setDisplayName(title);
		ArrayList<String> pages = new ArrayList<String>();
		pages.add(message);
		bm.setPages(pages);
		item.setItemMeta(bm);
		inv.addItem(item);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onSignChange(SignChangeEvent event){
		if(!event.getBlock().getType().equals(Material.WALL_SIGN) && !event.getBlock().getType().equals(Material.SIGN_POST)){
			return;
		}
		Sign sign = (Sign) event.getBlock().getState();
		org.bukkit.material.Sign signdata = (org.bukkit.material.Sign) sign.getData();
		Block block = event.getBlock().getRelative(signdata.getAttachedFace());
		if(block.getType() == Material.CHEST){
			event.setLine(1, "[post]");
		}
	}
}
