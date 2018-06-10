package me.dordsor21.AdvSwearBlock.util;

import me.dordsor21.AdvSwearBlock.Main;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SwearList {

    private List<String> badWords;

    private Main plugin;

    public SwearList(Main plugin) {
        this.plugin = plugin;
        File f = fileExists();

        badWords = new ArrayList<>();

        try {
            FileInputStream fis = new FileInputStream(f);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = br.readLine()) != null)
                badWords.add(line.toLowerCase());
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public List<String> getList() {
        return badWords;
    }

    private File fileExists() {
        File f = new File(plugin.getDataFolder(), "swearlist.txt");
        if (!f.exists()) {
            try {
                InputStream is = getClass().getResourceAsStream("/swearlist.txt");
                Reader r = new InputStreamReader(is, "utf-8");
                Writer w = new FileWriter(f);
                int read;
                while ((read = r.read()) != -1)
                    w.write(read);
                is.close();
                w.flush();
                w.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return f;
    }

    public void add(CommandSender sender, String[] args) throws IOException {
        File f = fileExists();
        StringBuilder successes = new StringBuilder();
        StringBuilder failures = new StringBuilder();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, true))) {
            for (String arg : args) {
                if (!badWords.contains(arg)) {
                    badWords.add(arg);
                    bw.newLine();
                    bw.write(arg);
                    successes.append(arg).append(", ");
                    continue;
                }
                failures.append(arg).append(", ");
            }
        }
        if (!successes.toString().isEmpty())
            sender.sendMessage(plugin.messages.get("badWordAddSuccess").replace("{{words}}", successes.substring(0, successes.length() - 2)));
        if (!failures.toString().isEmpty())
            sender.sendMessage(plugin.messages.get("badWordAddFailure").replace("{{words}}", failures.substring(0, failures.length() - 2)));
    }

    public void remove(CommandSender sender, String[] args) throws IOException {
        File f = fileExists();
        List<String> list = Arrays.asList(args);
        StringBuilder successes = new StringBuilder();
        StringBuilder failures = new StringBuilder();
        List<String> failureList = list;//VERY IMPORTANT DO NOT DELETE EVEN IF INTELLIJ TELLS YOU NOT TO ELSE YOU WILL BREAK THIS
        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            String line;
            StringBuilder str = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                String finalLine = line;
                if (list.stream().anyMatch(s -> s.equalsIgnoreCase(finalLine))) {
                    badWords.remove(finalLine.toLowerCase());
                    line = "";
                    successes.append(finalLine).append(", ");
                    failureList.remove(finalLine);
                }
                str.append(line).append('\n');
            }
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(str.toString().replaceAll("(?m)^\\s+$", "").getBytes());
            fos.close();
        }
        for(String failure: failureList)
            failures.append(failure).append(", ");
        if (!successes.toString().isEmpty())
            sender.sendMessage(plugin.messages.get("badWordRemoveSuccess").replace("{{words}}", successes.substring(0, failures.length() - 2)));
        if (!failures.toString().isEmpty())
            sender.sendMessage(plugin.messages.get("badWordRemoveFailure").replace("{{words}}", failures.substring(0, failures.length() - 2)));
    }

    public void list(CommandSender sender, int page) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(badWords.toString());
            return;
        }
        int pageSize = plugin.getConfig().getInt("swearing.listPageSize");
        Player p = (Player) sender;
        int pages = (int) Math.ceil(badWords.size() / pageSize);
        p.sendMessage(plugin.messages.get("listBadWordsTop").replace("{{count}}", String.valueOf(pageSize)).replace("{{total}}", String.valueOf(badWords.size())));
        for (String word : badWords.subList((page * pageSize) - pageSize, page * pageSize - 1))
            p.sendMessage("   " + word);
        p.sendMessage(plugin.messages.get("listBadWordsBottom").replace("{{page}}", String.valueOf(page)).replace("{{pagecount}}", String.valueOf(pages)));
    }

}
