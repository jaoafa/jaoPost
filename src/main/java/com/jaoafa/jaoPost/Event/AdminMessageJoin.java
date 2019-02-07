package com.jaoafa.jaoPost.Event;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.jaoafa.jaoPost.Task.Task_AdminMsgCheck;

public class AdminMessageJoin implements Listener {
	JavaPlugin plugin;
	public AdminMessageJoin(JavaPlugin plugin) {
		this.plugin = plugin;
	}
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoinEvent(PlayerJoinEvent event){
		new Task_AdminMsgCheck(event.getPlayer()).runTaskAsynchronously(plugin);
	}
}
