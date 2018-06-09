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
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.jaoafa.jaoPost.JaoPost;
import com.jaoafa.jaoPost.MySQL;
import com.jaoafa.jaoPost.Event.ClickPostChest;

public class post implements CommandExecutor {
	JavaPlugin plugin;
	public post(JavaPlugin plugin) {
		this.plugin = plugin;
	}
	public static Map<String,Map<String,String>> itemdata = new HashMap<String,Map<String,String>>();
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		if(args.length == 1){
			if(args[0].equalsIgnoreCase("show")){
				if (!(sender instanceof Player)) {
					JaoPost.SendMessage(sender, cmd, "このコマンドはゲーム内から実行してください。");
					Bukkit.getLogger().info("ERROR! コマンドがゲーム内から実行されませんでした。");
					return true;
				}
				Player player = (Player) sender;
				return onCommand_Show(sender, cmd, commandLabel, args, player);
			}else if(args[0].equalsIgnoreCase("allread")){
				// 全既読
				if (!(sender instanceof Player)) {
					JaoPost.SendMessage(sender, cmd, "このコマンドはゲーム内から実行してください。");
					Bukkit.getLogger().info("ERROR! コマンドがゲーム内から実行されませんでした。");
					return true;
				}
				Player player = (Player) sender;
				ALLReadInfo(player);
				return true;
			}
		}else if(args.length == 2){
			if(args[0].equalsIgnoreCase("send")){
				if (!(sender instanceof Player)) {
					JaoPost.SendMessage(sender, cmd, "このコマンドはゲーム内から実行してください。");
					Bukkit.getLogger().info("ERROR! コマンドがゲーム内から実行されませんでした。");
					return true;
				}
				Player player = (Player) sender;
				return onCommand_Send(sender, cmd, commandLabel, args, player);
			}
		}else if(args.length == 4){
			if(args[0].equalsIgnoreCase("cmdbsend")){
				if (!(sender instanceof BlockCommandSender)) {
					JaoPost.SendMessage(sender, cmd, "このコマンドはコマンドブロックから実行してください。");
					return true;
				}
				return onCommand_SendByCmdb(sender, cmd, commandLabel, args);
			}
		}
		JaoPost.SendMessage(sender, cmd, "--- jaoPost Help ---");
		JaoPost.SendMessage(sender, cmd, "/post: このコマンドのヘルプを表示する。");
		JaoPost.SendMessage(sender, cmd, "/post show: ポストを見る");
		JaoPost.SendMessage(sender, cmd, "/post allread: jaotanからのメッセージ(お知らせ)をすべて既読にする");
		JaoPost.SendMessage(sender, cmd, "/post send <Player>: Playerに対して手に持っている本を送信する。");
		JaoPost.SendMessage(sender, cmd, "/post cmdbsend <Player> <Title> <Text>: Playerに対してTitleのTextを送信する。(コマンドブロックのみ使用可能)");
		JaoPost.SendMessage(sender, cmd, "Tips: /post cmdbsendではコマンドブロックの名前が送信者として登録されます。");
		return true;
	}
	private boolean onCommand_Send(CommandSender sender, Command cmd, String commandLabel, String[] args, Player player){
		String to = args[1];
		if(player.getItemInHand().getType() == Material.AIR){
			JaoPost.SendMessage(sender, cmd, "アイテムを持っていません。");
			return true;
		}
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
		String message = implode(pages, "§j");

		Inventory inv = Bukkit.getServer().createInventory(player, 4 * 9, "jaoPost - アイテム送信有無選択");

		ItemStack item = new ItemStack(Material.BOOK);
		ItemMeta itemmeta = item.getItemMeta();
		itemmeta.setDisplayName("送信する本にアイテムを添付しますか？");
		item.setItemMeta(itemmeta);
		inv.setItem(4, item);

		ItemStack item_green = new ItemStack(Material.WOOL);
		item_green.setDurability((short) 5); // 黄緑
		ItemMeta itemmeta_green = item_green.getItemMeta();
		itemmeta_green.setDisplayName("添付する");
		item_green.setItemMeta(itemmeta_green);
		inv.setItem(20, item_green);

		ItemStack item_red = new ItemStack(Material.WOOL);
		item_red.setDurability((short) 14); // 赤
		ItemMeta itemmeta_red = item_red.getItemMeta();
		itemmeta_red.setDisplayName("添付しない");
		item_red.setItemMeta(itemmeta_red);
		inv.setItem(24, item_red);

		ItemStack item_white = new ItemStack(Material.WOOL);
		ItemMeta itemmeta_white = item_white.getItemMeta();
		itemmeta_white.setDisplayName("送信をやめる");
		item_white.setItemMeta(itemmeta_white);
		inv.setItem(35, item_white);

		player.openInventory(inv);
		Map<String,String> data = new HashMap<String,String>();

		data.put("to", to);
		data.put("title", title);
		data.put("message", message);

		itemdata.put(player.getName(), data);
		return true;
	}

	private boolean onCommand_SendByCmdb(CommandSender sender, Command cmd, String commandLabel, String[] args){
		String to = args[1];
		String title = args[2];
		String message = args[3];
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		String date = sdf.format(new Date());

		Statement statement;
		try {
			statement = JaoPost.c.createStatement();
		} catch (NullPointerException e) {
			MySQL MySQL = new MySQL(JaoPost.sqlserver, "3306", "jaoafa", JaoPost.sqluser, JaoPost.sqlpassword);
			try {
				JaoPost.c = MySQL.openConnection();
				statement = JaoPost.c.createStatement();
			} catch (ClassNotFoundException | SQLException e1) {
				sender.sendMessage("[jaoPost] " + ChatColor.GREEN + "送信にエラーが発生しました。再度お試しください。");
				e1.printStackTrace();
				return true;
			}
		} catch (SQLException e) {
			sender.sendMessage("[jaoPost] " + ChatColor.GREEN + "送信にエラーが発生しました。再度お試しください。");
			e.printStackTrace();
			return true;
		}
		statement = MySQL.check(statement);
		try {
			statement.execute("INSERT INTO jaopost (fromplayer, toplayer, title, message, readed, date) VALUES (\"" + sender.getName() + " (Cmdb)\", \"" + to + "\", \"" + title + "\", \"" + message + "\", false, \"" + date + "\")");
			sender.sendMessage("[jaoPost] " + ChatColor.GREEN + "送信が完了しました。");
			return true;
		} catch (SQLException e) {
			sender.sendMessage("[jaoPost] " + ChatColor.GREEN + "送信にエラーが発生しました。再度お試しください。");
			e.printStackTrace();
			return true;
		}
	}

	public static void ALLReadInfo(Player player){
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("Authorization", "Bot " + JaoPost.discordtoken);
		headers.put("User-Agent", "DiscordBot (https://jaoafa.com, v0.0.1)");

		JSONArray MessageList = ClickPostChest.getHttpsArrayJson("https://discordapp.com/api/channels/245526046303059969/messages?limit=100", headers);

		int count = 0;
		int c = 0;
		for(int i = 0; i < MessageList.size(); i++){
			if(c >= 5 * 9){
				break;
			}
			JSONObject message = (JSONObject) MessageList.get(i);
			String id = (String) message.get("id");
			Long type = (Long) message.get("type");
			if(type != 0) continue;
			if(!ClickPostChest.isMessageRead(id, player.getUniqueId())){
				ClickPostChest.addMessageRead(id, player.getUniqueId());
				count++;
			}
			c++;
		}

		player.sendMessage("[jaoPost] " + ChatColor.GREEN + "すべてのメッセージの既読処理を終了しました。(処理件数: " + count + " / " + c + "件)");
	}

	private boolean onCommand_Show(CommandSender sender, Command cmd, String commandLabel, String[] args, Player player){
		Statement statement;
		try {
			statement = JaoPost.c.createStatement();
		} catch (NullPointerException e) {
			MySQL MySQL = new MySQL(JaoPost.sqlserver, "3306", "jaoafa", JaoPost.sqluser, JaoPost.sqlpassword);
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
				String item = res.getString("item");
				if(item.equals("")){
					ClickPostChest.IntoBook(inv, title, message, from, id, c, date);
				}else{
					ClickPostChest.IntoBook_WithItem(inv, title, message, from, id, c, item, date);
				}
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
	public static <T> String implode(List<T> list, String glue) {
	    StringBuilder sb = new StringBuilder();
	    for (T e : list) {
	        sb.append(glue).append(e);
	    }
	    return sb.substring(glue.length());
	}
}
