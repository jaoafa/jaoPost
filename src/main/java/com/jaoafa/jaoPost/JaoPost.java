package com.jaoafa.jaoPost;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.jaoafa.jaoPost.Command.post;
import com.jaoafa.jaoPost.Event.AdminMessageJoin;
import com.jaoafa.jaoPost.Event.BookItemCheck;
import com.jaoafa.jaoPost.Event.ClickPostChest;
import com.jaoafa.jaoPost.Event.JoinCheck;


public class JaoPost extends JavaPlugin {
	public static String sqluser;
	public static String sqlpassword;
	public static String sqlserver = "jaoafa.com";
	public static String discordtoken = null;
	public static Connection c = null;
	public static long ConnectionCreate = 0;

	@Override
	public void onEnable() {
		getCommand("post").setExecutor(new post(this));
		getServer().getPluginManager().registerEvents(new ClickPostChest(this), this);
		getServer().getPluginManager().registerEvents(new AdminMessageJoin(this), this);
		getServer().getPluginManager().registerEvents(new JoinCheck(this), this);
		getServer().getPluginManager().registerEvents(new BookItemCheck(this), this);
		new Check().runTaskTimer(this, 24000, 24000);

		FileConfiguration conf = getConfig();

		if(conf.contains("discordtoken")){
			discordtoken = (String) conf.get("discordtoken");
		}

		if(conf.contains("sqluser") && conf.contains("sqlpassword")){
			JaoPost.sqluser = conf.getString("sqluser");
			JaoPost.sqlpassword = conf.getString("sqlpassword");
		}else{
			getLogger().info("MySQL Connect err. [conf NotFound]");
			getLogger().info("Disable jaoPost...");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		if(conf.contains("sqlserver")){
			sqlserver = (String) conf.get("sqlserver");
		}
		MySQL MySQL = new MySQL(sqlserver, "3306", "jaoafa", sqluser, sqlpassword);

		try {
			c = MySQL.openConnection();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			getLogger().info("MySQL Connect err. [ClassNotFoundException]");
			getLogger().info("Disable jaoPost...");
			getServer().getPluginManager().disablePlugin(this);
			return;
		} catch (SQLException e) {
			e.printStackTrace();
			getLogger().info("MySQL Connect err. [SQLException: " + e.getSQLState() + "]");
			getLogger().info("Disable jaoPost...");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		getLogger().info("MySQL Connect successful.");

		instance = this;
	}

	@Override
	public void onDisable() {
		getLogger().info("jaoPost Disable.");
	}

	public static void SendMessage(CommandSender sender, Command cmd, String text) {
		sender.sendMessage("[jaoPost] " + ChatColor.AQUA + text);
	}

	private class Check extends BukkitRunnable{
		@Override
		public void run() {
			for(Player player: Bukkit.getServer().getOnlinePlayers()) {
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
	}
	private static JavaPlugin instance;
	public static JavaPlugin getJavaPlugin(){
		return instance;
	}
}
