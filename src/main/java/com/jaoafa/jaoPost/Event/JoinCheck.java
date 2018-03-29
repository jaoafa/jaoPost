package com.jaoafa.jaoPost.Event;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.jaoafa.jaoPost.JaoPost;
import com.jaoafa.jaoPost.MySQL;

public class JoinCheck implements Listener {
	JavaPlugin plugin;
	public JoinCheck(JavaPlugin plugin) {
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
		statement = MySQL.check(statement);
		try {
			ResultSet res = statement.executeQuery("SELECT COUNT(id) FROM jaopost WHERE toplayer = '" + player.getName() + "' AND readed = false;;");
			double count = 0;
			if(res.next()){
				count = res.getInt(1);
			}
			if(count != 0){
				player.sendMessage("[jaoPost] " + ChatColor.GREEN + "受信箱に" + ((int)count) + "件のメッセージがあります！");
				player.sendMessage("[jaoPost] " + ChatColor.GREEN + "/post showで閲覧できます！");
			}
		} catch (SQLException e) {}
	}
}
