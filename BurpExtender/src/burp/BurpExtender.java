package burp;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

public class BurpExtender implements IBurpExtender,ITab, IExtensionStateListener, IContextMenuFactory,
        ActionListener, IMessageEditorController
{
    public PrintWriter stdout;
    public PrintWriter stderr;
    public IBurpExtenderCallbacks callbacks;
    private BurpChat bc;
    private IHttpRequestResponse[] current_reqres;
    public IExtensionHelpers helpers;
    private boolean DEBUG = true;

    //UI stuff
    private JSplitPane splitPane;
    private IMessageEditor requestViewer;
    private IMessageEditor responseViewer;
    private BurpShareItem currentlyDisplayedItem;
    private Table logTable;
    private JPanel jpane;
    private JTextField login;
    private JPasswordField password;
    private JButton loginButton;

    
    public LogTableModel logTableModel;
    public List<LogEntry> log = new ArrayList<LogEntry>();
    
    
    @Override
    public void registerExtenderCallbacks(final IBurpExtenderCallbacks callbacks)
    {
        
        this.callbacks = callbacks;
        
        //setup our extender console output
        stdout = new PrintWriter(callbacks.getStdout(),true);
        stderr = new PrintWriter(callbacks.getStderr(),true);

        
        //setup our extension details
        callbacks.setExtensionName("BurpChat");
        helpers = callbacks.getHelpers();

        
        
        //register a state listener
        callbacks.registerExtensionStateListener(this);
        
        //register our menus
        callbacks.registerContextMenuFactory(this);
        
        
        

        //build our UI
        SwingUtilities.invokeLater(new Runnable(){
           
            @Override
            public void run(){
                

                bc = new BurpChat();
                //setup the output channels for burp extender tab
                bc.setupComms(stdout, stderr,BurpExtender.this);

                //enable debug so we get extended output to the burp console
                bc.setDebug(DEBUG);

                
                SpringLayout flayout = new SpringLayout();
                JLabel uname = new JLabel("Username:");
                JLabel pass = new JLabel("Password:");
                jpane = new JPanel();
                jpane.setLayout(flayout);

                //login controls
                login = new JTextField(20);
                password = new JPasswordField(20);
                loginButton = new JButton("Login");
                loginButton.addActionListener(BurpExtender.this);
                loginButton.setActionCommand("LOGIN");
                

                // table of log entries
                logTableModel = new LogTableModel();
                logTable = new Table(logTableModel);
                
                                
                JScrollPane scrollPane = new JScrollPane(logTable);


                // tabs with request/response viewers
                JTabbedPane tabs = new JTabbedPane();
                requestViewer = callbacks.createMessageEditor(BurpExtender.this, false);
                responseViewer = callbacks.createMessageEditor(BurpExtender.this, false);
                tabs.addTab("Request", requestViewer.getComponent());
                tabs.addTab("Response", responseViewer.getComponent());
              
                jpane.add(uname);
                flayout.putConstraint(SpringLayout.WEST, uname, 25, SpringLayout.WEST, jpane);
                flayout.putConstraint(SpringLayout.NORTH, uname, 30, SpringLayout.NORTH, jpane);
                
                jpane.add(login);
                flayout.putConstraint(SpringLayout.WEST, login, 5, SpringLayout.EAST, uname);
                flayout.putConstraint(SpringLayout.NORTH, login, 25, SpringLayout.NORTH, jpane);
                
                jpane.add(pass);
                flayout.putConstraint(SpringLayout.WEST, pass, 15, SpringLayout.EAST, login);
                flayout.putConstraint(SpringLayout.NORTH, pass, 30, SpringLayout.NORTH, jpane);
                
                jpane.add(password);
                flayout.putConstraint(SpringLayout.WEST, password, 5, SpringLayout.EAST, pass);
                flayout.putConstraint(SpringLayout.NORTH, password, 25, SpringLayout.NORTH, jpane);
                
                jpane.add(loginButton);
                flayout.putConstraint(SpringLayout.WEST, loginButton, 15, SpringLayout.EAST, password);
                flayout.putConstraint(SpringLayout.NORTH, loginButton, 25, SpringLayout.NORTH, jpane);                
                
                jpane.add(scrollPane);
                flayout.putConstraint(SpringLayout.WEST, scrollPane, 20, SpringLayout.WEST, jpane);
                flayout.putConstraint(SpringLayout.EAST, scrollPane, -20, SpringLayout.EAST, jpane);
                flayout.putConstraint(SpringLayout.NORTH, scrollPane, 10, SpringLayout.SOUTH, uname);  
                
                jpane.add(tabs);
                flayout.putConstraint(SpringLayout.WEST, tabs, 0, SpringLayout.WEST, jpane);
                flayout.putConstraint(SpringLayout.EAST, tabs, 0, SpringLayout.EAST, jpane);
                flayout.putConstraint(SpringLayout.NORTH, tabs, 10, SpringLayout.SOUTH, scrollPane);
                flayout.putConstraint(SpringLayout.SOUTH, tabs, -10, SpringLayout.SOUTH, jpane);
                
               // jpane.add(splitPane);
                // customize our UI components
               callbacks.customizeUiComponent(jpane);
               callbacks.customizeUiComponent(logTable);
               callbacks.customizeUiComponent(scrollPane);
               callbacks.customizeUiComponent(tabs);
               
               
               
               //add to the UI
               callbacks.addSuiteTab(BurpExtender.this);
            }
            
            
            
        });
        
        
    }

    @Override
    public void extensionUnloaded()
    {
        stdout.println("Extension was unloaded, Logging out and cleaning up.");
        //log out from chat server
        try{
        bc.logout();
        
        }catch(Exception ex){
            stderr.println("Problem on unload: " + ex.toString());
        }
        
    }

    @Override
    public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
        List<JMenuItem> menus = new ArrayList();
        List<JMenuItem> submenu = new ArrayList();
        
        //
        current_reqres = invocation.getSelectedMessages();
        stdout.println(current_reqres.length);
        
        JMenu extender = new JMenu("Burp Chat - Send To Contact");
        
        //TODO: Change this to get a list of the available users
        //JMenuItem sub = new JMenuItem("evolutionVIII@gmail.com");
        
        for(String buddy: bc.buddylist){
            submenu.add(new JMenuItem(buddy));
        }
        
        for(JMenuItem sub:submenu){
            extender.add(sub);
            sub.addActionListener(this);
        }
        
        extender.addActionListener(this);
        
        menus.add(extender);
        return menus;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        stdout.println(e.getActionCommand());
        stdout.println(e.paramString());
       
        if(e.getActionCommand().equals("LOGIN"))
        {
            stdout.println("loggin in");
                try{
                   //check for active session
                   if(!bc.sessionActive()){
                       //check that the user has entered their login as user@domain
                       String[] user = login.getText().split("@");
                       stdout.println(user.length);
                       if(user.length==2){
                        //create new burpchat instance
                        bc.setupChat(user[1]);
                        bc.login(user[0], new String(password.getPassword()));
                        password.setText(null); 
                        loginButton.setText("Logout");
                       }
                       else
                           stderr.println("Username does not contain the target domain. Example: User@example.com");
                   }
                   else{
                       stdout.println("Already logged in, logging out");
                       bc.logout();
                       loginButton.setText("Login");
                   }
                }catch(Exception ex){
                    
                    stderr.println(ex.getMessage());
                }
        }
        else{
        //send all the selected items to our selected contact
            for(IHttpRequestResponse item:current_reqres){
                BurpShareItem bsi = new BurpShareItem();
                bsi.loadFromRequestResponse(item);
                bc.sendMessage(e.getActionCommand(), bsi);     
            }
        }

         
    }
    
    // implement ITab
    //

    @Override
    public String getTabCaption()
    {
        return "BurpChat";
    }

    @Override
    public Component getUiComponent()
    {
       // return splitPane;
        return jpane;
    }

    // implement IMessageEditorController
    // this allows our request/response viewers to obtain details about the messages being displayed
    //
    
    @Override
    public byte[] getRequest()
    {
        return currentlyDisplayedItem.request;
    }

    @Override
    public byte[] getResponse()
    {
        return currentlyDisplayedItem.response;
    }

    @Override
    public IHttpService getHttpService()
    {   
        
        return helpers.buildHttpService(currentlyDisplayedItem.host, currentlyDisplayedItem.port, currentlyDisplayedItem.protocol);
    }
    
    //
    // extend AbstractTableModel
    //
    
    public class LogTableModel extends DefaultTableModel{
    
     //   @Override
       // public int getRowCount()
      //  {
       //     return 0;
      //  }

        @Override
        public int getColumnCount()
        {
            return 3;
        }

        @Override
        public String getColumnName(int columnIndex)
        {

            switch (columnIndex)
            {
                case 0:
                    return "User";
                case 1:
                    return "URL";
                case 2:
                    return "Comment";
                default:
                    return "";
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            return String.class;
        }

       
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            LogEntry logEntry = log.get(rowIndex);

            switch (columnIndex)
            {
                case 0:
                    return logEntry.user;
                case 1:
                    return logEntry.url.toString();
                case 2:
                    return logEntry.comment;
                default:
                    return "";
            }

        }

    }
    // extend JTable to handle cell selection
    //
    private class Table extends JTable
    {
        public Table(TableModel tableModel)
        {
            super(tableModel);
        }
        
        @Override
        public void changeSelection(int row, int col, boolean toggle, boolean extend)
        {
            // show the log entry for the selected row
           LogEntry logEntry = log.get(row);
           requestViewer.setMessage(logEntry.bitem.request, true);
           if(logEntry.bitem.response!=null)
                responseViewer.setMessage(logEntry.bitem.response, false);
           currentlyDisplayedItem = logEntry.bitem;
            
           super.changeSelection(row, col, toggle, extend);
        }        
    }
    
        //
    // class to hold details of each log entry
    //
    
    public static class LogEntry
    {
        final String user;
        final String comment;
        final BurpShareItem bitem;
        final String url;

        LogEntry(String user, BurpShareItem bitem, String url)
        {
            this.user = user;
            this.bitem = bitem;
            this.url = url;
            this.comment = bitem.comment;
        }
    }
}
