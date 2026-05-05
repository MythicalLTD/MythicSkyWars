package systems.mythical.mythicskywars.menus;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.enums.Vote;
import systems.mythical.mythicskywars.events.MythicSkywarsSelectTeamEvent;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.game.PlayerCard;
import systems.mythical.mythicskywars.game.TeamCard;
import systems.mythical.mythicskywars.managers.PlayerStat;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.Party;
import systems.mythical.mythicskywars.utilities.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class TeamSelectionMenu {

    public TeamSelectionMenu(GameMap gMap) {
        String menuName = new Messaging.MessageFormatter().setVariable("mapname", gMap.getDisplayName()).format("menu.teamselection-menu-title");
        if (MythicSkywars.getNMS().getVersion() < 9) {
            if (menuName.length() > 32) {
                menuName = menuName.substring(0, 31);
            }
        }

        List<TeamCard> teamCards = gMap.getTeamCards();
        final int tsize = teamCards.size();
        int msize = 54;
        if (tsize <= 9) {
            msize = 9;
        } else if (tsize <= 18) {
            msize = 18;
        } else if (tsize <= 27) {
            msize = 27;
        } else if (tsize <= 36) {
            msize = 36;
        } else if (tsize <= 45) {
            msize = 45;
        }


        Inventory menu = Bukkit.createInventory(null, msize, menuName);
        ArrayList<Inventory> invs = new ArrayList<>();
        invs.add(menu);

        int finalMsize = msize;
        Runnable update = () -> {
            if (MythicSkywars.getIC().hasViewers(gMap.getName() + "teamselect") || MythicSkywars.getIC().hasViewers(gMap.getName() + "teamspectate")) {
                ArrayList<Inventory> invs1 = MythicSkywars.getIC().getMenu(gMap.getName() + "teamselect").getInventories();
                for (Inventory inv : invs1) {
                    for (int i = 0; i < finalMsize; i++) {
                        inv.setItem(i, new ItemStack(Material.AIR, 1));
                    }
                }
                String mat = MythicSkywars.getCfg().getTeamMaterial();
                List<String> lores = new ArrayList<>();
                for (TeamCard tCard : teamCards) {
                    String name = new Messaging.MessageFormatter()
                            // + 1 for human readable format
                            .setVariable("team", (tCard.getPosition()+1) + "")
                            .   setVariable("playercount", tCard.getPlayersSize() + "")
                            .setVariable("teamsize", tCard.getSize() + "")
                            .setVariable("teamcolor", tCard.getTeamName())
                            .format("menu.team_select_menu.item_title");
                    byte color = MythicSkywars.getCfg().isUseTeamMaterialBytes() ? tCard.getByte() : (byte) MythicSkywars.getCfg().getStandardTeamMaterialByte();
                    ItemStack item;
                    if (MythicSkywars.getNMS().getVersion() >= 13) {
                        Material material = Material.matchMaterial(mat.toUpperCase());
                        if (material == null) material = Material.STONE;
                        item = new ItemStack(material);
                    }
                    else {
                        item = MythicSkywars.getNMS().getColorItem(mat, color);
                    }
                    if (MythicSkywars.getCfg().isUseTeamNumberInMenu()) {
                        // + 1 since item count cannot be 0 and human prefer starting at 1
                        item.setAmount(tCard.getPosition() + 1);
                    }

                    lores.clear();

                    for (String line : MythicSkywars.getMessaging().getFile().getStringList("menu.team_select_menu.lore.general-lore")) {
                        // +1 position for index to be human readable
                        lores.add(ChatColor.translateAlternateColorCodes('&', line.replace("{team}", (tCard.getPosition()+1)+"")
                            .replace("{playercount}", tCard.getPlayersSize() + "")
                            .replace("{teamsize}", tCard.getSize()+"")
                            .replace("{teamcolor}", tCard.getTeamName())));
                    }

                    for (PlayerCard pCard : tCard.getPlayerCards()) {
                        Player p = pCard.getPlayer();
                        if (p != null) {
                            lores.add(new Messaging.MessageFormatter()
                                    // +1 position for index to be human readable
                                    .setVariable("team", (tCard.getPosition()+1) + "")
                                    .setVariable("playercount", tCard.getPlayersSize() + "")
                                    .setVariable("teamsize", tCard.getSize() + "")
                                    .setVariable("teamcolor", tCard.getTeamName())
                                    .setVariable("playername", p.getName())
                                    .setVariable("playerdisplayname", p.getDisplayName())
                                    .format("menu.team_select_menu.lore.player-list-lore-line"));
                        }
                    }
                    invs1.get(0).setItem(tCard.getPosition(), MythicSkywars.getNMS().getItemStack(item, lores, name));
                }
            }
        };

        MythicSkywars.getIC().create(gMap.getName() + "teamselect", invs, event -> {
            Player player = event.getPlayer();

            String name = event.getName();
            if (name.equalsIgnoreCase(MythicSkywars.getNMS().getItemName(MythicSkywars.getIM().getItem("exitMenuItem")))) {
                player.closeInventory();
                return;
            }

            if (gMap.getMatchState() != MatchState.WAITINGSTART && gMap.getMatchState() != MatchState.WAITINGLOBBY) {
                Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getErrorSound(), 1, 1);
                if (!MythicSkywars.getIC().hasViewers("jointeammenu")) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            MythicSkywars.getIC().getMenu("jointeammenu").update();
                        }
                    }.runTaskLater(MythicSkywars.get(), 5);
                }
                MythicSkywars.getIC().show(player, "jointeammenu");
                return;
            }

            TeamCard tCard = null;
            for (TeamCard tc : gMap.getTeamCards()) {
                if (tc.getPosition() == event.getSlot()) {
                    tCard = tc;
                    break;
                }
            }

            if (tCard == null) {
                return;
            }

            boolean joined;
            Party party = Party.getParty(player);
            if (party != null) {
                if (party.getLeader().equals(player.getUniqueId())) {
                    if ((gMap.getMatchState() == MatchState.WAITINGSTART || gMap.getMatchState() == MatchState.WAITINGLOBBY) && gMap.canAddParty(party)) {
                        player.closeInventory();
                        joined = gMap.addPlayers(tCard, party);
                        if (!joined) {
                            player.sendMessage(new Messaging.MessageFormatter().format("error.could-not-join2"));
                        }
                    }
                } else {
                    player.closeInventory();
                    player.sendMessage(new Messaging.MessageFormatter().format("party.onlyleader"));
                }
            } else {
                if ((gMap.getMatchState() == MatchState.WAITINGSTART || gMap.getMatchState() == MatchState.WAITINGLOBBY) && gMap.canAddPlayer(player)) {
                    // joined = gMap.addPlayers(tCard, player);
                    TeamCard tc = gMap.getTeamCard(player);
                    Vote timeVote = null;
                    Vote weatherVote = null;
                    Vote chestVote = null;
                    Vote modifierVote = null;
                    Vote healthVote = null;
                    if (tc != null) {
                        if (tc.getPosition() == event.getSlot()) {
                            // send same team error message
                            player.sendMessage(new Messaging.MessageFormatter().withUniversalPrefixForced().format("menu.teamselection-menu-alredy_in_team"));
                            return;
                        }
                        PlayerCard pc = gMap.getPlayerCard(player);
                        timeVote = pc.getVote("time");
                        weatherVote = pc.getVote("weather");
                        chestVote = pc.getVote("chest");
                        modifierVote = pc.getVote("modifier");
                        healthVote = pc.getVote("health");
                        pc.reset();
                    }

                    tCard.sendReservation(player, PlayerStat.getPlayerStats(player));
                    PlayerCard pc = gMap.getPlayerCard(player);
                    if (timeVote != null) {
                        pc.setGameTime(timeVote);
                    }
                    if (weatherVote != null) {
                        pc.setWeather(weatherVote);
                    }
                    if (chestVote != null) {
                        pc.setChestVote(chestVote);
                    }
                    if (modifierVote != null) {
                        pc.setModifier(modifierVote);
                    }
                    if (healthVote != null) {
                        pc.setHealth(healthVote);
                    }

                    Bukkit.getPluginManager().callEvent(new MythicSkywarsSelectTeamEvent(player, gMap, tCard));
                    // +1 position for index to be human readable
                    int realTeam = tCard.getPosition()+1;
                    player.sendMessage(new Messaging.MessageFormatter().withUniversalPrefixForced().setVariable("team", "" + realTeam).format("menu.teamselection-menu-join"));
                    player.playSound(player.getLocation(), Sound.valueOf(MythicSkywars.getCfg().getTeamJoinSound()), 1, 1);
                    player.closeInventory();
                    MythicSkywars.getIC().getMenu(gMap.getName() + "teamselect").update();

                        /*if (!joined) {
                            player.sendMessage(new Messaging.MessageFormatter().format("error.could-not-join2"));
                        }*/
                }
            }
        });
        MythicSkywars.getIC().getMenu(gMap.getName() + "teamselect").setUpdate(update);
    }

}