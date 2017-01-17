/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package burp;

import burp.BurpExtender.LogEntry;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import rocks.xmpp.core.Jid;
import rocks.xmpp.core.XmppException;
import rocks.xmpp.core.roster.RosterEvent;
import rocks.xmpp.core.roster.RosterListener;
import rocks.xmpp.core.roster.RosterManager;
import rocks.xmpp.core.roster.model.Contact;
import rocks.xmpp.core.session.SessionStatusEvent;
import rocks.xmpp.core.session.SessionStatusListener;
import rocks.xmpp.core.session.XmppSession;
import rocks.xmpp.core.session.XmppSession.Status;
import rocks.xmpp.core.stanza.MessageEvent;
import rocks.xmpp.core.stanza.MessageListener;
import rocks.xmpp.core.stanza.PresenceEvent;
import rocks.xmpp.core.stanza.PresenceListener;
import rocks.xmpp.core.stanza.model.client.Message;
import rocks.xmpp.core.stanza.model.client.Presence;


/**
 *
 * 
 */
public class BurpChat {
    
    private XmppSession xmppSession;
    private PrintWriter stdout;
    private PrintWriter stderr;
    private String user;
    private boolean debug = false;
    public List<String> buddylist;
    private BurpExtender be;
    
    public BurpChat(){
        
    }
    
    //initialize a new XMPP session to our target host
    public void setupChat(String host){
        xmppSession = new XmppSession(host);
        buddylist = new ArrayList();
        //setup our callback handlers
        // Listen for presence changes
            xmppSession.addInboundPresenceListener(new PresenceListener() {
                @Override
                public void handlePresence(PresenceEvent e) {
                    // Handle inbound presence.
                    //if the contact has become available, check that they dont already exist and add them
                    if(e.getPresence().isAvailable()){
                        if(!buddylist.contains(e.getPresence().getFrom().asBareJid().toString()))
                            if(debug){
                                stdout.println("Contact added as available: " + e.getPresence().getFrom().asBareJid().toString());
                                stdout.println("Resource: "+e.getPresence().getFrom().getResource());
                             }
                            //only add buddys who are using burp chat
                            if(e.getPresence().getFrom().getResource().startsWith("burpChat")){
                                stdout.println("Available Burp Chat Buddy: " + e.getPresence().getFrom().toString());
                                if(!buddylist.contains(e.getPresence().getFrom().toString()))
                                    buddylist.add(e.getPresence().getFrom().toString());
                            }
                    }
                    
                    //if the contact becomes unavailable, remove them from the list
                    else if(!e.getPresence().isAvailable()){
                        if(debug)
                            stdout.println("Contact became unavailable, removing: " + e.getPresence().getFrom().asBareJid().toString());
                        buddylist.remove(e.getPresence().getFrom().toString());
                    }
                    
                    
                    if(e.getPresence().getId()!=null){
                        if(debug){
                            stdout.println(e.getPresence().getId());
                        }
                    }
                }
            });
            
           
            //Listen for session status events
            xmppSession.addSessionStatusListener(new SessionStatusListener(){
                
               @Override
               public void sessionStatusChanged(SessionStatusEvent e){
                   //handle a session status change
                   if(e.getStatus().equals(Status.CONNECTING))
                       stdout.println("Trying to connect to the server");
                   else if(e.getStatus().equals(Status.CONNECTED))
                       stdout.println("Connected to the server");
                   else if(e.getStatus().equals(Status.AUTHENTICATING))
                       stdout.println("Trying to authenticate");
                   else if(e.getStatus().equals(Status.AUTHENTICATED))
                       stdout.println("Authenticated successfully");
                   else if(e.getStatus().equals(Status.CLOSING))
                       stdout.println("Closing the connection");

               }
            });

            xmppSession.addInboundMessageListener(new MessageListener() {
                @Override
                public void handleMessage(MessageEvent e) {
                    // Handle inbound message
                    if(e.getMessage().getBody()!=null && e.getMessage().getFrom().getResource().startsWith("burpChat")){
                        processMessage(e.getMessage());
                    }
                    
                }
            });
            // Listen for roster pushes
            xmppSession.getManager(RosterManager.class).addRosterListener(new RosterListener() {
                @Override
                public void rosterChanged(RosterEvent e) {
                    if(debug)
                        stdout.println("roster event:");
                    for(Contact i:e.getAddedContacts()){
                        if(debug){
                            stdout.println("Contact Added: "+i.getJid().toString());
                        }
                      //add to buddy list
                    //  buddylist.add(i.getJid().toString());
                    }
                    
                }
            });
   
    }
    
    public void setupComms(PrintWriter out, PrintWriter err, BurpExtender be){
        
        stdout = out;
        stderr = err;
        this.be = be;
        
    }
    
    public void setDebug(boolean dbg){
        debug = dbg;
    }
    
    
    public void setUser(String user){
        this.user = user;
    }
    
    public String getUser(){
        return user;
    }
    
    //connect to the server and login using the provided credentials
    public void login(String user, String password) throws XmppException{
        
        xmppSession.connect();
        xmppSession.login(user, password, "burpChat");
        xmppSession.send(new Presence());
        
    }
    
    public void logout() throws XmppException{
        xmppSession.close();
        
        if(debug)
            stdout.println("Logged Out!");
        
    }
    
    public boolean sessionActive(){
        try{
            return xmppSession.isConnected();
        }catch(Exception ex){
            return false;
        }
        
    }
    
    //send a message to the provided contact
    public void sendMessage(String contact, String message){
        xmppSession.send(new Message(Jid.valueOf(contact), Message.Type.CHAT, message));
    }

    public void sendMessage(String contact, BurpShareItem message){
        try{
        if(debug)    
            stdout.println(message.toJson());
        
        xmppSession.send(new Message(Jid.valueOf(contact), Message.Type.CHAT, message.toJson()));
        
        }catch(Exception ex){
            stderr.println("Error sending message: " + ex.toString());
        }
    }
    
    
    private void processMessage(Message e){
        
 
        try{
            BurpShareItem bitem = new BurpShareItem();
            bitem.loadJson(e.getBody());
            if(bitem.request!=null){


                int row = be.log.size();
                be.log.add(new LogEntry(e.getFrom().asBareJid().toString(),bitem,bitem.protocol+"://"+bitem.host));
                stdout.println(be.log.size());
                be.logTableModel.addRow(new Object[]{});
                be.logTableModel.fireTableRowsInserted(row, row);
                
                 if(debug){
                     stdout.println(bitem.toString());
                 }
            }
            else
                stderr.println("Received a malformed message, request body was empty.");
        }catch(Exception ex){
            stderr.println("Problem with recieved message:" + ex.toString());
        }
        
    }
    
    
}
