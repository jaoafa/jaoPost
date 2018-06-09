package com.jaoafa.jaoPost.Event;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.jaoafa.jaoPost.JaoPost;
import com.jaoafa.jaoPost.MySQL;
import com.jaoafa.jaoPost.Command.post;

public class AdminMessageJoin implements Listener {
	JavaPlugin plugin;
	public AdminMessageJoin(JavaPlugin plugin) {
		this.plugin = plugin;
	}
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoinEvent(PlayerJoinEvent event){
		Player player = event.getPlayer();
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
		statement = MySQL.check(statement);
		statement1 = MySQL.check(statement1);

		if(!player.hasPlayedBefore()){
			post.ALLReadInfo(player);
		}

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
				count++;
			}
			c++;
		}

		if(count != 0){
			player.sendMessage("[jaoPost] " + ChatColor.GREEN + "jaotanからのメッセージが" + count + "件あります！");
			player.sendMessage("[jaoPost] " + ChatColor.GREEN + "/post showで閲覧できます。また、/post allreadですべてを既読にできます。");
		}
	}
}
