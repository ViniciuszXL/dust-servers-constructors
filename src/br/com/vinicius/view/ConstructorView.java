package br.com.vinicius.view;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.simpleyaml.configuration.file.YamlFile;

public class ConstructorView extends JFrame {

	private static final long serialVersionUID = -2710078388340563961L;
	private JPanel contentPanel = null;

	public ConstructorView() {
		getContentPane().setLayout(null);

		setTitle("Dust Servers Constructors");
		setVisible(true);
		setSize(640, 480);
	}

	public void create() throws Exception {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		contentPanel = new JPanel();
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPanel);

		JScrollPane scroll = new JScrollPane();
		JPanel panel = new JPanel();
		File file = new File("servers.yml");
		if (!file.exists()) {
			panel.add(new JTextField(
					"Ocorreu um erro ao gerar o arquivo pois não foi encontrado o arquivo principal servers.yml!"));
		} else {
			File newFile = new File("docker-compose.yml");
			if (newFile.exists()) {
				newFile.delete();
			}

			newFile.createNewFile();
			if (writeFile(newFile)) {
				panel.add(new JTextField("Arquivo criado com sucesso!"));
			} else {
				panel.add(new JTextField("Arquivo criado com erro!"));
			}
		}

		scroll.setViewportView(panel);
		contentPanel.add(scroll);
		pack();
	}

	private boolean writeFile(File file) throws Exception {
		YamlFile yamlFile = new YamlFile(new File("servers.yml"));
		yamlFile.load();

		ListConstructor realCons = ListConstructor.create();
		ListConstructor cons = ListConstructor.create();

		// Adicionando a versão do docker-compose //
		realCons.add("# Versão do compose").add("version: '3'\n");

		// Adicionando os volumes //
		realCons.add("# Volumes").add("volumes:");

		// Adicionando as networks //
		cons.add("\n# Redes").add("networks:");
		for (Object s : yamlFile.getList("networks")) {
			cons.add("  " + s + ":");
		}

		boolean isBungee = yamlFile.getBoolean("isBungee");
		int networkCompression = isBungee ? -1 : 512;
		
		// Adicionando os serviços //
		cons.add("\n# Serviços").add("services:");
		for (String serverName : yamlFile.getConfigurationSection("servers").getKeys(false)) {
			String path = "servers." + serverName + ".";

			// SERVER CONFIG //
			int maxServers = yamlFile.getInt(path + "server_config.max_servers");
			int initialPort = yamlFile.getInt(path + "server_config.port_initial");
			int maxPlayers = yamlFile.getInt(path + "server_config.max_players");
			String memory = yamlFile.getString(path + "server_config.memory");

			// SERVER VOLUMES //
			boolean saveData = yamlFile.getBoolean(path + "server_volumes.save_data");
			boolean mapBackup = yamlFile.getBoolean(path + "server_volumes.map_backup");
			String worldName = yamlFile.getString(path + "server_volumes.world_name");

			// SERVER NETWORK //
			boolean networkAdd = yamlFile.getBoolean(path + "server_network.toAdd");
			String networkName = yamlFile.getString(path + "server_network.name");
			for (int i = 1; i <= maxServers; i++) {
				String containerName = serverName + '-' + i;
				int port = Integer.valueOf(initialPort + i);

				cons.add("  " + containerName + ":");
				cons.add("    container_name: " + containerName);
				cons.add("    image: itzg/minecraft-server");
				cons.add("    ports:");
				cons.add("      - '" + port + ":" + port + "'");
				cons.add("    environment:");
				cons.add("      EULA: 'TRUE'");
				cons.add("      DIFFICULTY: 'normal'");
				cons.add("      MOTD: 'This is " + containerName + " server running in Docker.'\n");
				cons.add("      # JAR FILE");
				cons.add("      TYPE: 'PAPER'");
				cons.add("      VERSION: '1.8.8'\n");
				cons.add("      # ADMINS");
				cons.add("      OPS: ViniciuszXL\n");
				cons.add("      # WORLD SETTINGS");
				cons.add("      ALLOW_NETHER: 'FALSE'");
				cons.add("      # PLAYER SETTINGS");
				cons.add("      ANNOUNCE_PLAYER_ACHIEVEMENTS: 'FALSE'");
				cons.add("      MAX_PLAYERS: " + maxPlayers + "\n");
				cons.add("      # SERVER CONFIG");
				cons.add("      MEMORY: '" + memory + "'");
				cons.add("      JVM_XX_OPTS: ''");
				cons.add("      SERVER_NAME: '" + containerName + "'");
				cons.add("      SERVER_PORT: " + port);
				cons.add("      NETWORK_COMPRESSION_THRESHOLD: " + networkCompression);
				if (isBungee) {
					cons.add("      ONLINE_MODE: 'FALSE'");
				}

				cons.add("    volumes:");
				if (saveData) {
					cons.add("      # Save data");
					cons.add("      - " + containerName + ":/data\n");
					realCons.add("  " + containerName + ":");
				}

				cons.add("      # Archives");
				cons.add("      - ./plugins:/data/plugins");
				cons.add("      - ./config/bukkit.yml:/data/bukkit.yml");
				cons.add("      - ./config/" + (isBungee ? "spigot-bungee.yml" : "spigot.yml") + ":/data/spigot.yml");
				cons.add("      # World");
				cons.add("      - ./worlds/" + worldName + ":/data/world\n");
				if (mapBackup) {
					cons.add("      # World Backup");
					cons.add("      - ./worlds/" + worldName + ":/data/world_b");
				}

				if (networkAdd) {
					cons.add("    networks:");
					cons.add("      # Network");
					cons.add("      - " + networkName);
				}

				cons.add("    tty: true");
				cons.add("    stdin_open: true");
			}
		}

		Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));

		try {
			cons.build().forEach(x -> realCons.addWithoutLine(x));
			List<String> toAdd = realCons.build();

			for (int i = 0; i < toAdd.size(); i++) {
				String add = toAdd.get(i);
				out.write(add);
			}

			return true;
		} catch (Exception e) {
			return false;
		} finally {
			out.close();
		}
	}

	public static class ListConstructor {

		private final List<String> list;

		public ListConstructor() {
			this.list = new ArrayList<String>();
		}

		public static ListConstructor create() {
			return new ListConstructor();
		}

		public ListConstructor add(String s) {
			this.list.add(s + "\n");
			return this;
		}

		public ListConstructor addWithoutLine(String s) {
			this.list.add(s);
			return this;
		}

		public List<String> build() {
			return list;
		}

	}

}
