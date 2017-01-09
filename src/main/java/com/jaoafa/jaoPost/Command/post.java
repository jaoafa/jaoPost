package com.jaoafa.jaoPost.Command;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.jaoafa.jaoPost.JaoPost;
import com.jaoafa.jaoPost.MySQL;
import com.jaoafa.jaoPost.Event.ClickPostChest;

public class post implements CommandExecutor {
	JavaPlugin plugin;
	public post(JavaPlugin plugin) {
		this.plugin = plugin;
	}
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		if (!(sender instanceof Player)) {
			JaoPost.SendMessage(sender, cmd, "このコマンドはゲーム内から実行してください。");
			Bukkit.getLogger().info("ERROR! コマンドがゲーム内から実行されませんでした。");
			return true;
		}
		Player player = (Player) sender;
		if(player.getItemInHand().getType() == Material.AIR){
			JaoPost.SendMessage(sender, cmd, "アイテムを持っていません。");
			return true;
		}
		if(args.length == 1){
			if(args[0].equalsIgnoreCase("show")){
				return onCommand_Show(sender, cmd, commandLabel, args, player);
			}
		}else if(args.length == 2){
			if(args[0].equalsIgnoreCase("send")){
				return onCommand_Send(sender, cmd, commandLabel, args, player);
			}
		}
		JaoPost.SendMessage(sender, cmd, "--- jaoPost Help ---");
		JaoPost.SendMessage(sender, cmd, "/post: このコマンドのヘルプを表示する。");
		JaoPost.SendMessage(sender, cmd, "/post show: ポストを見る");
		JaoPost.SendMessage(sender, cmd, "/post send <Player>: Playerに対して手に持っている本を送信する。");
		return true;
	}
	private boolean onCommand_Send(CommandSender sender, Command cmd, String commandLabel, String[] args, Player player){
		String to = args[1];
		Material handtype = player.getItemInHand().getType();
		if(handtype != Material.WRITTEN_BOOK){
			JaoPost.SendMessage(sender, cmd, "このコマンドを使用するには、送る本を手に持ってください。");
			return true;
		}
		BookMeta book = (BookMeta) player.getItemInHand().getItemMeta();

		String title = "";
		List<String> pages = null;
		if(book.hasTitle()){
			title = book.getTitle();
		}else{
			title = book.getDisplayName();
		}
		if(book.hasPages()){
			pages = book.getPages();
		}
		if(pages == null){
			JaoPost.SendMessage(sender, cmd, "エラーが発生しました。詳しくはプラグイン制作者にお問い合わせください。Debug: Pages null");
			return true;
		}
		String message = pages.get(0);
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
				return true;
			}
		} catch (SQLException e) {
			player.sendMessage("[jaoPost] " + ChatColor.GREEN + "送信にエラーが発生しました。再度お試しください。");
			e.printStackTrace();
			return true;
		}
		statement = MySQL.check(statement);
		try {

			statement.execute("INSERT INTO jaopost (fromplayer, toplayer, title, message, readed, date) VALUES (\"" + player.getName() + "\", \"" + to + "\", \"" + title + "\", \"" + message + "\", false, \"" + date + "\")");
			player.sendMessage("[jaoPost] " + ChatColor.GREEN + "送信が完了しました。");
			return true;
		} catch (SQLException e) {
			player.sendMessage("[jaoPost] " + ChatColor.GREEN + "送信にエラーが発生しました。再度お試しください。");
			e.printStackTrace();
			return true;
		}
	}

	private boolean onCommand_Show(CommandSender sender, Command cmd, String commandLabel, String[] args, Player player){
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
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return true;
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
				if(ClickPostChest.post.containsKey(player.getName())){
					Map<Integer, Integer> postdata = ClickPostChest.post.get(player.getName());
					postdata.put(25, -1);
					ClickPostChest.post.put(player.getName(), postdata);
				}else{
					Map<Integer, Integer> postdata = new HashMap<Integer, Integer>();
					postdata.put(25, -1);
					ClickPostChest.post.put(player.getName(), postdata);
				}

				ItemStack itemread = new ItemStack(Material.BUCKET);
				ItemMeta itemmetaread = itemread.getItemMeta();
				itemmetaread.setDisplayName("既読済みを見る");
				itemread.setItemMeta(itemmetaread);
				inv.setItem(26, itemread);
				if(ClickPostChest.post.containsKey(player.getName())){
					Map<Integer, Integer> postdata = ClickPostChest.post.get(player.getName());
					postdata.put(26, 0);
					ClickPostChest.post.put(player.getName(), postdata);
				}else{
					Map<Integer, Integer> postdata = new HashMap<Integer, Integer>();
					postdata.put(26, 0);
					ClickPostChest.post.put(player.getName(), postdata);
				}

				player.openInventory(inv);
				return true;
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
				ClickPostChest.IntoBook(inv, title, message, from, id, c, date);
				c++;
			}
			ItemStack item = new ItemStack(Material.ENDER_CHEST);
			ItemMeta itemmeta = item.getItemMeta();
			itemmeta.setDisplayName("お知らせ(jaotanからのメッセージ)を見る");
			item.setItemMeta(itemmeta);
			inv.setItem((i * 9)-2, item);
			if(ClickPostChest.post.containsKey(player.getName())){
				Map<Integer, Integer> postdata = ClickPostChest.post.get(player.getName());
				postdata.put((i * 9)-2, -1);
				ClickPostChest.post.put(player.getName(), postdata);
			}else{
				Map<Integer, Integer> postdata = new HashMap<Integer, Integer>();
				postdata.put((i * 9)-2, -1);
				ClickPostChest.post.put(player.getName(), postdata);
			}

			item = new ItemStack(Material.BUCKET);
			itemmeta = item.getItemMeta();
			itemmeta.setDisplayName("既読済みを見る");
			item.setItemMeta(itemmeta);
			inv.setItem((i * 9)-1, item);
			if(ClickPostChest.post.containsKey(player.getName())){
				Map<Integer, Integer> postdata = ClickPostChest.post.get(player.getName());
				postdata.put((i * 9)-1, 0);
				ClickPostChest.post.put(player.getName(), postdata);
			}else{
				Map<Integer, Integer> postdata = new HashMap<Integer, Integer>();
				postdata.put((i * 9)-1, 0);
				ClickPostChest.post.put(player.getName(), postdata);
			}

			player.openInventory(inv);
			return true;
		} catch (SQLException e) {
			player.sendMessage("[jaoPost] " + ChatColor.GREEN + "受信箱のチェックに失敗しました。再度お試しください。");
			return true;
		}
	}
}
