package com.github.cloudflareddns;

import org.json.simple.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

/**
 * @author Austin Lautissier
 *
 */
public class Gui {
    private static JFrame frame = new JFrame("CloudFlareDDNS v2.0");
    private static final Image icon = Toolkit.getDefaultToolkit().getImage(Gui.class.getClassLoader().getResource("icon.png"));
    private static boolean autoRefresh = false;
    private static CfAutoRefreshTask autoRefreshTask;

    private static boolean toTray = false;
    private static TrayIcon trayIcon;

    private static Configuration configuration;

    public static void main(String[] args) {
        configuration = new Configuration("ddns-config.yml");
        toTray = configuration.isToTray();

        for (String arg : args) {
            //force to tray
            if (arg.equalsIgnoreCase("--to-tray")) {
                toTray = true;
                break;
            }
        }

        frame.setLocation(200, 200);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setIconImage(icon);

        addToSystemTray();
        loginGui();
        //auto login
        if (!configuration.getKey().equals("your_api_key")) {
            new CfLoginTask(null, configuration.getKey()).execute();
            loadingGui("Connecting to server...");
        }
    }

    private static void loginGui() {
        frame.getContentPane().removeAll();
        frame.setLayout(new GridBagLayout());

        //JLabel emailLabel = new JLabel("Email");
        //JTextField emailTf = new JTextField(30);
        JLabel keyLabel = new JLabel("Api key");
        JTextField keyTf = new JTextField(30);
        JButton loginBut = new JButton("Log In");
        loginBut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new CfLoginTask(null, keyTf.getText()).execute();
                loadingGui("Connecting to server...");
            }
        });

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(10, 10, 10, 10);

        //frame.add(emailLabel, gc);

        //gc.gridx = 1;
        //gc.gridy = 0;
        //frame.add(emailTf, gc);

        gc.gridx = 0;
        gc.gridy = 1;
        frame.add(keyLabel ,gc);

        gc.gridx = 1;
        gc.gridy = 1;
        frame.add(keyTf ,gc);

        gc.gridx = 2;
        gc.gridy = 2;
        frame.add(loginBut ,gc);

        frame.pack();
    }

    private static void loadingGui(String message) {
        frame.getContentPane().removeAll();

        JPanel loadingPanel = new JPanel(new FlowLayout());
        frame.add(loadingPanel);

        JLabel loadingLabel = new JLabel(message);
        loadingPanel.add(loadingLabel);

        frame.getContentPane().revalidate();
        frame.repaint();
    }

    private static void showMessageBox(String message) {
        JOptionPane.showMessageDialog(null, message);
    }

    private static void serverSelectGui(CfConfig config) {
        frame.getContentPane().removeAll();
        frame.setLayout(new BorderLayout());
        // frame.setResizable(false); ???

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowIconified(WindowEvent evt) {
                frame.setVisible(false);
            }
        });

        //Schedule refresh task if enabled
        scheduleAutoRefreshTask(config, configuration.getInterval());

        String ar;
        if (autoRefresh) {
            ar = "ON";
        } else {
            ar = "OFF";
        }

        JLabel snLabel = new JLabel("Server name: " + config.getServerName() + "     WAN IP: " + config.getWanIp() + "     Auto refresh: " + ar);
        frame.add(snLabel, BorderLayout.PAGE_START);

        ArrayList<JSONObject> dnsList = config.getDnsList();
        String[] colNames = {"Name", "Type", "Content"};
        Object[][] data = new Object[dnsList.size()][colNames.length];

        for (int i = 0; i < dnsList.size(); i++) {
            JSONObject dnsJson = dnsList.get(i);

            data[i][0] = dnsJson.get("name");
            data[i][1] = dnsJson.get("type");
            data[i][2] = dnsJson.get("content");
        }

        JTable table = new JTable(data, colNames);
        JScrollPane sp = new JScrollPane(table);
        table.setFillsViewportHeight(true);
        table.setEnabled(false); // disable edit
        frame.add(sp, BorderLayout.CENTER);


        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));

        JButton refreshBut = new JButton("Refresh");
        refreshBut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                (new CfRefreshTask(config)).execute();
                loadingGui("Refreshing DNS records...");
            }
        });
        buttonPanel.add(refreshBut);

        JButton autoRefreshBut = new JButton("Auto refresh");
        autoRefreshBut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                autoRefreshGui(config);
            }
        });
        buttonPanel.add(autoRefreshBut);

        frame.add(buttonPanel, BorderLayout.PAGE_END);

        frame.pack();

        if (toTray) {
            frame.setVisible(false);
        }
    }

    private static void autoRefreshGui(CfConfig config) {
        frame.getContentPane().removeAll();
        frame.setLayout(new GridBagLayout());

        JLabel timeLabel = new JLabel("Minutes between updates: ");

        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(configuration.getInterval(), 1, 1440, 1);
        JSpinner spinner = new JSpinner(spinnerModel);

        JLabel autoRefreshLabel = new JLabel("Toggle auto refresh on/off:");
        JCheckBox arCheckBox = new JCheckBox();
        arCheckBox.setSelected(configuration.isAutoRefresh());

        JButton okBut = new JButton("OK");
        okBut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                autoRefresh = arCheckBox.isSelected();

                //Save changes
                configuration.setInterval((int) spinner.getValue()); // Save interval
                configuration.setAutoRefresh(arCheckBox.isSelected()); // Save whether auto-refresh is enabled
                configuration.saveChanges(); // Save changes to config file

                serverSelectGui(config);
            }
        });
        JButton cancelBut = new JButton("Cancel");
        cancelBut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                serverSelectGui(config);
            }
        });

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(10, 10, 10, 10);

        frame.add(timeLabel, gc);

        gc.gridx = 1;
        gc.gridy = 0;
        frame.add(spinner, gc);

        gc.gridx = 0;
        gc.gridy = 1;
        frame.add(autoRefreshLabel, gc);

        gc.gridx = 1;
        gc.gridy = 1;
        frame.add(arCheckBox, gc);

        gc.gridx = 1;
        gc.gridy = 2;
        frame.add(okBut, gc);

        gc.gridx = 2;
        gc.gridy = 2;
        frame.add(cancelBut, gc);

        frame.revalidate();
        frame.repaint();
    }

    private static void scheduleAutoRefreshTask(CfConfig config, int mins) {
        //FIXME: Clicking 'cancel' in GUI reschedules task like 'OK' button
        if (autoRefreshTask != null) {
            System.out.println("Cancelling previous task");
            autoRefreshTask.cancel(true);
            autoRefreshTask = null;
        }

        if (configuration.isAutoRefresh()) {
            autoRefresh = true;
            autoRefreshTask = new CfAutoRefreshTask(config, mins);
            autoRefreshTask.execute();
        }
    }

    private static class CfRefreshTask extends SwingWorker<Void, Void> {
        CfConfig config;

        CfRefreshTask(CfConfig config) {
            this.config = config;
        }

        @Override
        public Void doInBackground() {
            try {
                config.refreshDnsRecords(configuration.getRecord());
                showMessageBox("Refreshed records");
            } catch (Exception e) {
                showMessageBox("Failed to refresh records. Please check internet connectivity");
            }
            return null;
        }

        @Override
        public void done() {
            serverSelectGui(config);
        }
    }

    private static class CfAutoRefreshTask extends SwingWorker<Void, Void> {
        CfConfig config;
        int mins;

        public CfAutoRefreshTask(CfConfig config, int mins) {
            this.config = config;
            this.mins = mins;
        }

        @Override
        protected Void doInBackground() throws Exception {
            while (autoRefresh) {
                Thread.sleep(mins * 60000);
                loadingGui("Refreshing DNS records...");
                config.refreshDnsRecords(configuration.getRecord());
                serverSelectGui(config);
            }
            return null;
        }
    }

    private static class CfLoginTask extends SwingWorker<Void, Void>{
        String email, key;
        boolean success = false;
        CfConfig config;

        CfLoginTask(String email, String key){
            this.email = email;
            this.key = key;
        }

        @Override
        public Void doInBackground() {
            try {
                config = new CfConfig(email, key, configuration.getRecord());
                success = true;
            } catch (Exception e) {
                e.printStackTrace();
                showMessageBox("Invalid email or key");
            }
            return null;
        }

        @Override
        public void done(){
            if (success) {
                serverSelectGui(config);
            } else {
                loginGui();
            }
        }

    }

    private static void addToSystemTray() {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();

            // Dodawanie ikony do System Tray
            trayIcon = new TrayIcon(icon, "CloudFlare DDNS");
            trayIcon.setImageAutoSize(true);

            // Dodawanie menu do ikony
            PopupMenu popupMenu = new PopupMenu();
            MenuItem showItem = new MenuItem("Show");
            showItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.setVisible(true);
                    //pokazywanie okienka "na wierzchu"
                    if (!toTray) {
                        frame.setState(Frame.ICONIFIED);
                        frame.setState(Frame.NORMAL);
                    }
                }
            });

            MenuItem exitItem = new MenuItem("Close");
            exitItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });
            popupMenu.add(showItem);
            popupMenu.add(exitItem);
            trayIcon.setPopupMenu(popupMenu);

            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                e.printStackTrace();
            }
        }
    }

    static void displayTrayNotification(String oldIp, String newIp) {
        if (SystemTray.isSupported() && trayIcon != null) {
            trayIcon.displayMessage("IP changed", "Updated DNS record to new IP \n" + oldIp + " -> " + newIp, TrayIcon.MessageType.INFO);
        }
    }

}
