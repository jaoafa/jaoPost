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
			try {
				ResultSet res = statement.executeQuery("SELECT id FROM jaoinfo");
				while(res.next()){
					String readplayer = res.getString("readplayer");
					int id = res.getInt("id");
					if(!readplayer.contains(player.getName())){
						if(readplayer.equalsIgnoreCase("")){
							statement1.execute("UPDATE jaoinfo SET readplayer = \"" + player.getName() + "\" WHERE id = " + id);
						}else{
							statement1.execute("UPDATE jaoinfo SET readplayer = \"" + readplayer + "," + player.getName() + "\" WHERE id = " + id);
						}
					}
				}
			} catch (SQLException e) {}
		}

		try {
			ResultSet res = statement.executeQuery("SELECT COUNT(id) FROM `jaoinfo` WHERE `readplayer` NOT LIKE '%" + player.getName() + "%'");
			int count = 0;
			if(res.next()){
				count = res.getInt(1);
			}
			if(count != 0){
				player.sendMessage("[jaoPost] " + ChatColor.GREEN + "jaotanからのメッセージが" + count + "件あります！");
				player.sendMessage("[jaoPost] " + ChatColor.GREEN + "/post showで閲覧できます。また、/post allreadですべてを既読にできます。");
			}
		} catch (SQLException e) {}
	}
}
