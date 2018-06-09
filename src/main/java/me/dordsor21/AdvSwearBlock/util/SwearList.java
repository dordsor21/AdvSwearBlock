package me.dordsor21.AdvSwearBlock.util;

import me.dordsor21.AdvSwearBlock.Main;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SwearList {

    private List<String> badWords;

    public SwearList(Main plugin) {
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

        badWords = new ArrayList<>();

        try {
            FileInputStream fis = new FileInputStream(f);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = br.readLine()) != null)
                badWords.add(line);
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public List<String> getList() {
        return badWords;
    }

}
