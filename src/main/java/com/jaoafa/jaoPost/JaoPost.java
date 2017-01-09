package com.jaoafa.jaoPost;

import java.sql.Connection;
import java.sql.SQLException;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.jaoafa.jaoPost.Command.post;
import com.jaoafa.jaoPost.Event.AdminMessageJoin;
import com.jaoafa.jaoPost.Event.ClickPostChest;
import com.jaoafa.jaoPost.Event.JoinCheck;


public class JaoPost extends JavaPlugin {
	public static String sqluser;
	public static String sqlpassword;
	public static Connection c = null;

	@Override
	public void onEnable() {
		getCommand("post").setExecutor(new post(this));
		getServer().getPluginManager().registerEvents(new ClickPostChest(this), this);
		getServer().getPluginManager().registerEvents(new AdminMessageJoin(this), this);
		getServer().getPluginManager().registerEvents(new JoinCheck(this), this);

		FileConfiguration conf = getConfig();
		if(conf.contains("sqluser") && conf.contains("sqlpassword")){
			JaoPost.sqluser = conf.getString("sqluser");
			JaoPost.sqlpassword = conf.getString("sqlpassword");
		}else{
			getLogger().info("MySQL Connect err. [conf NotFound]");
			getLogger().info("Disable jaoPost...");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		MySQL MySQL = new MySQL("jaoafa.com", "3306", "jaoafa", sqluser, sqlpassword);

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
	}

	@Override
	public void onDisable() {
		getLogger().info("jaoPost Disable.");
	}

	public static void SendMessage(CommandSender sender, Command cmd, String text) {
		sender.sendMessage("[jaoPost] " + ChatColor.AQUA + text);
	}
}
