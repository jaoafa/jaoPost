package com.jaoafa.jaoPost.Event;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.jaoafa.jaoPost.JaoPost;
import com.jaoafa.jaoPost.MySQL;
import com.jaoafa.jaoPost.Command.post;

public class BookItemCheck implements Listener {
	JavaPlugin plugin;
	public BookItemCheck(JavaPlugin plugin) {
		this.plugin = plugin;
	}
	@EventHandler(ignoreCancelled = true)
	public void onBookItemOKorNGClick(InventoryClickEvent event) {
		if(event.getWhoClicked().getType() != EntityType.PLAYER) return;
		if(event.getClickedInventory() == null) return;
		if(!event.getClickedInventory().getName().equals("jaoPost - アイテム送信有無選択")) return;
		Player player = (Player) event.getWhoClicked();

		ItemStack is = event.getCurrentItem();
		if(is.getType() == Material.AIR){
			return;
		}
		if(!post.itemdata.containsKey(player.getName())){
			return;
		}
		Map<String, String> data = post.itemdata.get(player.getName());

		String to = data.get("to");
		String title = data.get("title");
		String message = data.get("message");

		ItemMeta itemmeta = is.getItemMeta();
		if(itemmeta.getDisplayName().equalsIgnoreCase("添付する")){
			is.setType(Material.AIR);
			new openBookItemSelectInv(player).runTaskLater(plugin, 1);
		}else if(itemmeta.getDisplayName().equalsIgnoreCase("添付しない")){
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			String date = sdf.format(new Date());
			Statement statement;
			try {
				statement = JaoPost.c.createStatement();
			} catch (NullPointerException e) {
				MySQL MySQL = new MySQL("jaoafa.com", "3306", "jaoafa", JaoPost.sqluser, JaoPost.sqlpassword);
				try {
					JaoPost.c = MySQL.openConnection();
					statement = JaoPost.c.createStatement();
				} catch (ClassNotFoundException | SQLException e1) {
					player.sendMessage("[jaoPost] " + ChatColor.GREEN + "送信にエラーが発生しました。再度お試しください。");
					e1.printStackTrace();
					return;
				}
			} catch (SQLException e) {
				player.sendMessage("[jaoPost] " + ChatColor.GREEN + "送信にエラーが発生しました。再度お試しください。");
				e.printStackTrace();
				return;
			}
			statement = MySQL.check(statement);
			try {
				statement.execute("INSERT INTO jaopost (fromplayer, toplayer, title, message, readed, date) VALUES (\"" + player.getName() + "\", \"" + to + "\", \"" + title + "\", \"" + message + "\", false, \"" + date + "\")");
				player.sendMessage("[jaoPost] " + ChatColor.GREEN + "送信が完了しました。");
				player.getInventory().clear(player.getInventory().getHeldItemSlot());
				if(!post.itemdata.containsKey(player.getName())){
					post.itemdata.remove(player.getName());
				}
				player.closeInventory();
				return;
			} catch (SQLException e) {
				player.sendMessage("[jaoPost] " + ChatColor.GREEN + "送信にエラーが発生しました。再度お試しください。");
				e.printStackTrace();
				return;
			}

		}else if(itemmeta.getDisplayName().equalsIgnoreCase("送信をやめる")){
			if(!post.itemdata.containsKey(player.getName())){
				post.itemdata.remove(player.getName());
			}
			player.closeInventory();
		}else{
			return;
		}

	}
	private class openBookItemSelectInv extends BukkitRunnable{
		Player player;
		public openBookItemSelectInv(Player player) {
			this.player = player;
		}
		@Override
		public void run() {
			PlayerInventory inv = player.getInventory();
			Inventory inventory = Bukkit.getServer().createInventory(null, InventoryType.PLAYER, "jaoPost - 送信アイテム選択");
			inventory.setItem(27, inv.getItem(0));
			inventory.setItem(28, inv.getItem(1));
			inventory.setItem(29, inv.getItem(2));
			inventory.setItem(30, inv.getItem(3));
			inventory.setItem(31, inv.getItem(4));
			inventory.setItem(32, inv.getItem(5));
			inventory.setItem(33, inv.getItem(6));
			inventory.setItem(34, inv.getItem(7));
			inventory.setItem(35, inv.getItem(8));
			ItemStack[] invdata = inv.getContents();
			for(int n=0; n != invdata.length; n++)
			{
				if(n <= 8) continue;
				inventory.setItem(n-9, inv.getItem(n));
			}
			player.openInventory(inventory);
		}
	}
	@EventHandler(ignoreCancelled = true)
	public void onBookItemSelectClick(InventoryClickEvent event) {
		if(event.getWhoClicked().getType() != EntityType.PLAYER) return;
		if(event.getClickedInventory() == null) return;
		if(!event.getClickedInventory().getName().equals("jaoPost - 送信アイテム選択")) return;
		Player player = (Player) event.getWhoClicked();

		ItemStack is = event.getCurrentItem();
		if(is.getType() == Material.AIR){
			return;
		}
		if(!post.itemdata.containsKey(player.getName())){
			return;
		}
		Map<String, String> data = post.itemdata.get(player.getName());

		String to = data.get("to");
		String title = data.get("title");
		String message = data.get("message");

		String id = CreateItemID();
		if(!jaoPostItemDataSave(id, is)){
			player.sendMessage("[jaoPost] " + ChatColor.GREEN + "アイテムの保存に失敗したため送信できませんでした。");
			player.closeInventory();
			return;
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		String date = sdf.format(new Date());
		Statement statement;
		try {
			statement = JaoPost.c.createStatement();
		} catch (NullPointerException e) {
			MySQL MySQL = new MySQL("jaoafa.com", "3306", "jaoafa", JaoPost.sqluser, JaoPost.sqlpassword);
			try {
				JaoPost.c = MySQL.openConnection();
				statement = JaoPost.c.createStatement();
			} catch (ClassNotFoundException | SQLException e1) {
				player.sendMessage("[jaoPost] " + ChatColor.GREEN + "送信にエラーが発生しました。再度お試しください。");
				e1.printStackTrace();
				return;
			}
		} catch (SQLException e) {
			player.sendMessage("[jaoPost] " + ChatColor.GREEN + "送信にエラーが発生しました。再度お試しください。");
			e.printStackTrace();
			return;
		}
		statement = MySQL.check(statement);
		try {
			statement.execute("INSERT INTO jaopost (fromplayer, toplayer, title, message, item, readed, date) VALUES (\"" + player.getName() + "\", \"" + to + "\", \"" + title + "\", \"" + message + "\", \"" + id + "\", false, \"" + date + "\")");
			player.sendMessage("[jaoPost] " + ChatColor.GREEN + "送信が完了しました。");
			player.getInventory().clear(player.getInventory().getHeldItemSlot());
			if(!post.itemdata.containsKey(player.getName())){
				post.itemdata.remove(player.getName());
			}
			player.closeInventory();
			return;
		} catch (SQLException e) {
			player.sendMessage("[jaoPost] " + ChatColor.GREEN + "送信にエラーが発生しました。再度お試しください。");
			e.printStackTrace();
			return;
		}

	}

	private boolean jaoPostItemDataSave(String id, ItemStack is){
		File file = new File(plugin.getDataFolder(), "postitem.yml");
		FileConfiguration data = YamlConfiguration.loadConfiguration(file);
		data.set(id, is);
		try {
			data.save(file);
			return true;
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			return false;
		}
	}
	public static ItemStack jaoPostItemDataLoad(String id){
		File file = new File(JaoPost.getJavaPlugin().getDataFolder(), "postitem.yml");
		FileConfiguration data = YamlConfiguration.loadConfiguration(file);
		if(data.contains(id)){
			ItemStack is = data.getItemStack(id);
			data.set(id, null);
			try {
				data.save(file);
				return is;
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
				return null;
			}
		}else{
			return null;
		}
	}
	private String CreateItemID(){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssS");
		String date = sdf.format(new Date());
		return date;
	}
}
