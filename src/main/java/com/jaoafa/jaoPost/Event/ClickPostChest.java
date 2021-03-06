package com.jaoafa.jaoPost.Event;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.jaoafa.jaoPost.JaoPost;
import com.jaoafa.jaoPost.MySQL;

public class ClickPostChest implements Listener {
	JavaPlugin plugin;
	public ClickPostChest(JavaPlugin plugin) {
		this.plugin = plugin;
	}
	public static Map<String,Map<Integer,Integer>> post = new HashMap<String,Map<Integer,Integer>>();
	public static Map<String, Map<Integer, String>> postjao = new HashMap<String,Map<Integer,String>>();
	public static Map<String,Map<Integer,String>> withitempost = new HashMap<String,Map<Integer,String>>();
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
				MySQL MySQL = new MySQL(JaoPost.sqlserver, "3306", "jaoafa", JaoPost.sqluser, JaoPost.sqlpassword);
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
				ResultSet res = statement.executeQuery("SELECT COUNT(id) FROM jaopost WHERE toplayer = '" + player.getName() + "' AND readed = false;");
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
					String item = res.getString("item");
					if(item.equals("")){
						IntoBook(inv, title, message, from, id, c, date);
					}else{
						IntoBook_WithItem(inv, title, message, from, id, c, item, date);
					}
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
					MySQL MySQL = new MySQL(JaoPost.sqlserver, "3306", "jaoafa", JaoPost.sqluser, JaoPost.sqlpassword);
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
				if(withitempost.containsKey(player.getName())){
					if(withitempost.get(player.getName()).containsKey(event.getSlot())){
						String item = withitempost.get(player.getName()).get(event.getSlot());
						ItemStack is = BookItemCheck.jaoPostItemDataLoad(item);
						if(is != null){
							if(player.getInventory().firstEmpty() == -1){
								player.getLocation().getWorld().dropItem(player.getLocation(), is);
								player.sendMessage("[jaoPost] " + ChatColor.GREEN + "添付されていたアイテムをインベントリに追加しようとしましたが、インベントリが一杯だったのであなたの足元にドロップしました。");
							}else{
								player.getInventory().addItem(is);
								player.sendMessage("[jaoPost] " + ChatColor.GREEN + "添付されていたアイテムをインベントリに追加しました。");
							}
						}else{
							player.sendMessage("[jaoPost] " + ChatColor.GREEN + "添付されていたアイテム情報を取得できませんでした。");
						}
					}
					try {
						statement.execute("UPDATE jaopost SET readed = true WHERE id = " + id);
					} catch (SQLException e) {
						player.sendMessage("[jaoPost] " + ChatColor.GREEN + "未既読の変更に失敗しました。再度お試しください。");
						event.setCancelled(true);
					}
				}
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
	public void onJaotanClick(InventoryClickEvent event) throws IOException {
		if(event.getWhoClicked().getType() != EntityType.PLAYER) return;
		if(event.getClickedInventory() == null) return;
		if(!event.getClickedInventory().getName().equals("jaoPost - お知らせ")) return;

		event.setCancelled(true);

		Player player = (Player) event.getWhoClicked();
		if(postjao.containsKey(player.getName())){
			if(event.getSlot() == 53){
				// 全部既読
				player.closeInventory();
				com.jaoafa.jaoPost.Command.post.ALLReadInfo(player);
				return;
			}

			Map<Integer, String> postdata = postjao.get(player.getName());
			if(postdata.containsKey(event.getSlot())){
				String id = postdata.get(event.getSlot());
				Statement statement1;
				try {
					statement1 = JaoPost.c.createStatement();
				} catch (NullPointerException e) {
					MySQL MySQL = new MySQL(JaoPost.sqlserver, "3306", "jaoafa", JaoPost.sqluser, JaoPost.sqlpassword);
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
					ResultSet res = statement1.executeQuery("SELECT * FROM jaoinfo WHERE msgid = \"" + id + "\"");
					String readplayer = "";
					if(res.next()){
						readplayer = res.getString("readplayer");
					}

					//String readplayer = "";
					if(!readplayer.contains(player.getName())){
						addMessageRead(id, player.getUniqueId());
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

					if(pages.contains(0)){

					}
					String str = pages.get(0);

					pages_read.add(str);
					pages_read.add("このメッセージについて詳しくはDiscord#infoにて。\n"
							+ "https://jaoafa.com/community/discord");

					bm.setPages(pages_read);
					bm.setLore(isbm.getLore());
					item.setItemMeta(bm);

					//ItemStack is = event.getClickedInventory().getItem(event.getSlot());
					player.getInventory().setItemInMainHand(item);
					//cp.getHandle().openBook(CraftItemStack.asNMSCopy(item));
					player.sendMessage("[jaoPost] " + ChatColor.GREEN + "右クリックして本を閲覧ください。");
					player.closeInventory();
				} catch (SQLException e) {
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
			MySQL MySQL = new MySQL(JaoPost.sqlserver, "3306", "jaoafa", JaoPost.sqluser, JaoPost.sqlpassword);
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
		if(JaoPost.discordtoken == null){
			player.sendMessage("[jaoPost] " + ChatColor.GREEN + "お知らせのチェックに失敗しました。開発部にお問い合わせください。");
			return;
		}
		player.sendMessage("[jaoPost] " + ChatColor.GREEN + "しばらくお待ちください…。");
		Inventory inv = Bukkit.getServer().createInventory(player, 6 * 9, "jaoPost - お知らせ");
		/*int c = 0;
		while(res.next()){
			if(c >= 5 * 9){
				break;
			}
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
		}*/


		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("Authorization", "Bot " + JaoPost.discordtoken);
		headers.put("User-Agent", "DiscordBot (https://jaoafa.com, v0.0.1)");

		JSONArray MessageList = getHttpsArrayJson("https://discordapp.com/api/channels/245526046303059969/messages?limit=100", headers);

		int c = 0;
		for(int i = 0; i < MessageList.size(); i++){
			if(c >= 5 * 9){
				break;
			}
			JSONObject message = (JSONObject) MessageList.get(i);
			String id = (String) message.get("id");
			String content = (String) message.get("content");
			Long type = (Long) message.get("type");
			if(type != 0) continue;


			// Contentフォーマット
			JSONArray mentions = (JSONArray) message.get("mentions");
			for(int m = 0; m < mentions.size(); m++){
				JSONObject mention = (JSONObject) mentions.get(m);
				String mention_userid = (String) mention.get("id");
				String username = (String) mention.get("username");
				String discriminator = (String) mention.get("discriminator");
				content = content.replace("<@" + mention_userid + ">", "@" + username + "#" + discriminator)
						.replace("<@!" + mention_userid + ">", "@" + username + "#" + discriminator);
			}
			/*
 			正常動作しないので削除
			JSONArray mention_roles = (JSONArray) message.get("mention_roles");
			for(int m = 0; m < mention_roles.size(); m++){
				JSONObject mention_role = (JSONObject) mention_roles.get(m);
				String mention_roleid = (String) mention_role.get("id");
				String name = (String) mention_role.get("name");
				content = content.replace("<@&" + mention_roleid + ">", "#" + name);
			}
			*/
			// チャンネルも置き換えたいけどまた今度

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			long unixtime = (Long.parseLong(id) >> 22) + 1420070400000L;
			Date date = new Date(unixtime);

			String title = sdf.format(date) + "に投稿されたメッセージ";
			if(!isMessageRead(id, player.getUniqueId())){
				title += ChatColor.RED + "(NEW!!)";
			}

			IntoBook_Jaotan(inv, title, content, id, c);
			c++;
		}

		ItemStack item = new ItemStack(Material.BOOKSHELF);
		ItemMeta itemmeta = item.getItemMeta();
		itemmeta.setDisplayName("すべてのお知らせを既読にする。");
		item.setItemMeta(itemmeta);
		inv.setItem(53, item);

		player.openInventory(inv);
	}

	public static boolean isMessageRead(String msgid, UUID uuid){ // 53メッセージのみ
		try {
			PreparedStatement statement = MySQL.getNewPreparedStatement("SELECT * FROM jaoinfo WHERE msgid = ?");
			statement.setString(1, msgid);
			ResultSet res = statement.executeQuery();
			if(!res.next()){
				return false;
			}
			String readplayer = res.getString("readplayer");
			if(readplayer.contains(uuid.toString())){
				return true;
			}else{
				return false;
			}
		} catch (SQLException | ClassNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			return false;
		}
	}

	public static void addMessageRead(String msgid, UUID uuid){
		try {
			PreparedStatement statement = MySQL.getNewPreparedStatement("SELECT * FROM jaoinfo WHERE msgid = ?");
			statement.setString(1, msgid);
			ResultSet res = statement.executeQuery();
			if(!res.next()){
				PreparedStatement insent_statement = MySQL.getNewPreparedStatement("INSERT INTO jaoinfo (msgid, readplayer) VALUES (?, ?)");
				insent_statement.setString(1, msgid);
				insent_statement.setString(2, uuid.toString());
				insent_statement.execute();
				return;
			}
			String readplayer = res.getString("readplayer");
			if(readplayer.contains(uuid.toString())){
				return;
			}
			int id = res.getInt("id");
			PreparedStatement add_statement = MySQL.getNewPreparedStatement("UPDATE jaoinfo SET readplayer = ? WHERE id = ?");
			if(readplayer.equalsIgnoreCase("")){
				add_statement.setString(1, uuid.toString());
			}else{
				add_statement.setString(1, readplayer + "," + uuid.toString());
			}
			add_statement.setInt(2, id);
			add_statement.execute();
		} catch (SQLException | ClassNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

	}

	public static JSONArray getHttpsArrayJson(String address, Map<String, String> headers){
		StringBuilder builder = new StringBuilder();
		try{
			URL url = new URL(address);

			HttpURLConnection connect = (HttpURLConnection) url.openConnection();
			connect.setRequestMethod("GET");
			if(headers != null){
				for(Map.Entry<String, String> header : headers.entrySet()) {
					connect.setRequestProperty(header.getKey(), header.getValue());
				}
			}

			connect.connect();

			if(connect.getResponseCode() != HttpURLConnection.HTTP_OK){
				InputStream in = connect.getErrorStream();

				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
				in.close();
				connect.disconnect();

				System.out.println("DiscordWARN: " + connect.getResponseMessage());
				new IOException(builder.toString()).printStackTrace();
				return null;
			}

			InputStream in = connect.getInputStream();

			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			in.close();
			connect.disconnect();
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(builder.toString());
			JSONArray json = (JSONArray) obj;
			return json;
		}catch(Exception e){
			e.printStackTrace();
			return null;
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
		String[] messages = message.split("§j");
		for(String msg : messages){
			pages.add(msg);
		}
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
	public static void IntoBook_WithItem(Inventory inv, String title, String message, String from, int id, int slot, String ItemID, String date) {
		ItemStack item = new ItemStack(Material.WRITTEN_BOOK, 1);
		BookMeta bm = (BookMeta) item.getItemMeta();
		bm.setAuthor(from);
		bm.setDisplayName(title);
		ArrayList<String> pages = new ArrayList<String>();
		String[] messages = message.split("§j");
		for(String msg : messages){
			pages.add(msg);
		}
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

				if(withitempost.containsKey(player.getName())){
					withitempost.get(player.getName()).put(slot, ItemID);
				}else{
					Map<Integer,String> itempost = new HashMap<Integer,String>();
					itempost.put(slot, ItemID);
					withitempost.put(player.getName(), itempost);
				}
			}else{
				Map<Integer, Integer> postdata = new HashMap<Integer, Integer>();
				postdata.put(slot, id);
				post.put(player.getName(), postdata);

				if(withitempost.containsKey(player.getName())){
					withitempost.get(player.getName()).put(slot, ItemID);
				}else{
					Map<Integer,String> itempost = new HashMap<Integer,String>();
					itempost.put(slot, ItemID);
					withitempost.put(player.getName(), itempost);
				}
			}
		}
	}

	private static void IntoBook_Jaotan(Inventory inv, String title, String message, String id, int slot) {
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
				Map<Integer, String> postdata = postjao.get(player.getName());
				postdata.put(slot, id);
				postjao.put(player.getName(), postdata);
			}else{
				Map<Integer, String> postdata = new HashMap<Integer, String>();
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
/*
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
*/
	/**
	 * 指定された文字列が、半角英数字(記号含む)か否かを返します。
	 *
	 * @param value 処理対象となる文字列
	 * @return true:半角英数字である(もしくは対象文字がない), false:半角英数字でない
	 * @see http://www.saka-en.com/java/java-ishalfwidthalphanumeric-string/
	 */
	public static Boolean isHalfWidthAlphanumeric(String value) {
		if ( value == null || value.length() == 0 )
			return true;
		int len = value.length();
		byte[] bytes = value.getBytes();
		if ( len != bytes.length )
			return false;
		return true;
	}
}
